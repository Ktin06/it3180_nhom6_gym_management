package vn.edu.hust.nhom6.gym.feature.invoice.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoicePaymentDto;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.Invoice;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceItem;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.PaymentMethod;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.PaymentStatus;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.InvoiceDocumentRepository.BranchSnapshot;
import vn.edu.hust.nhom6.gym.feature.invoice.repository.InvoiceRepository.MemberSnapshot;

/**
 * Kết xuất hóa đơn thanh toán sang PDF bằng OpenPDF (SRS F5 UC-F5.4 bước 5, BR-F5-09, BR-F5-10).
 * Nhúng font miễn phí DejaVu Sans ({@code resources/fonts/DejaVuSans*.ttf}, giấy phép Bitstream Vera / DejaVu)
 * để hiển thị đầy đủ dấu tiếng Việt trên mọi nền tảng và đọc lại được văn bản bằng {@code PdfReader}.
 */
@Component
public class InvoicePdfRenderer {
    private static final String REGULAR_FONT_PATH = "fonts/DejaVuSans.ttf";
    private static final String BOLD_FONT_PATH = "fonts/DejaVuSans-Bold.ttf";
    private static final DateTimeFormatter VN_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color HEADER_BG = new Color(240, 244, 248);
    private static final Color BORDER_COLOR = new Color(210, 216, 222);

    private final byte[] regularFontBytes;
    private final byte[] boldFontBytes;

    public InvoicePdfRenderer() {
        this.regularFontBytes = loadFontBytes(REGULAR_FONT_PATH);
        this.boldFontBytes = loadFontBytes(BOLD_FONT_PATH);
    }

    public byte[] render(Invoice invoice, BranchSnapshot branch, MemberSnapshot member,
            List<InvoicePaymentDto> payments) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BaseFont regularBase = BaseFont.createFont(
                "DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, regularFontBytes, null);
            BaseFont boldBase = BaseFont.createFont(
                "DejaVuSans-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, boldFontBytes, null);

            Font titleFont = new Font(boldBase, 15);
            Font sectionFont = new Font(boldBase, 11);
            Font boldFont = new Font(boldBase, 9.5f);
            Font regularFont = new Font(regularBase, 9.5f);
            Font smallFont = new Font(regularBase, 8.5f, Font.NORMAL, Color.DARK_GRAY);

            Document doc = new Document(PageSize.A4, 40, 40, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addBranchHeader(doc, branch, sectionFont, regularFont);
            addTitle(doc, titleFont);
            addInvoiceAndCustomerInfo(doc, invoice, member, boldFont, regularFont);
            addItemsTable(doc, invoice.getItems(), boldFont, regularFont);
            addTotalsBlock(doc, invoice, boldFont, regularFont);
            addPaymentsSection(doc, payments, sectionFont, boldFont, regularFont);
            addFooter(doc, smallFont);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("Không thể kết xuất hóa đơn PDF cho " + invoice.getCode(), ex);
        }
    }

    private void addBranchHeader(Document doc, BranchSnapshot branch, Font sectionFont, Font regularFont)
            throws DocumentException {
        Paragraph name = new Paragraph(branch.name() + " (" + branch.code() + ")", sectionFont);
        name.setSpacingAfter(2f);
        doc.add(name);

        if (branch.address() != null && !branch.address().isBlank()) {
            Paragraph address = new Paragraph("Địa chỉ: " + branch.address(), regularFont);
            address.setSpacingAfter(2f);
            doc.add(address);
        }
        if (branch.phone() != null && !branch.phone().isBlank()) {
            Paragraph phone = new Paragraph("Hotline: " + branch.phone(), regularFont);
            phone.setSpacingAfter(10f);
            doc.add(phone);
        }
    }

    private void addTitle(Document doc, Font titleFont) throws DocumentException {
        Paragraph title = new Paragraph("HÓA ĐƠN THANH TOÁN", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(4f);
        title.setSpacingAfter(14f);
        doc.add(title);
    }

    private void addInvoiceAndCustomerInfo(Document doc, Invoice invoice, MemberSnapshot member,
            Font boldFont, Font regularFont) throws DocumentException {
        PdfPTable meta = new PdfPTable(new float[]{1.2f, 2.3f, 1.2f, 2.3f});
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(12f);

        addMetaCell(meta, "Mã hóa đơn:", boldFont);
        addMetaCell(meta, invoice.getCode(), regularFont);
        addMetaCell(meta, "Trạng thái:", boldFont);
        addMetaCell(meta, invoiceStatusLabel(invoice.getStatus()), regularFont);

        addMetaCell(meta, "Ngày thanh toán:", boldFont);
        addMetaCell(meta, formatVnTime(invoice.getPaidAt() != null ? invoice.getPaidAt() : invoice.getCreatedAt()),
            regularFont);
        addMetaCell(meta, "Khách hàng:", boldFont);
        addMetaCell(meta, member == null ? "Khách lẻ" : member.fullName(), regularFont);

        if (member != null) {
            addMetaCell(meta, "Mã hội viên:", boldFont);
            addMetaCell(meta, member.memberCode(), regularFont);
            addMetaCell(meta, "Số điện thoại:", boldFont);
            addMetaCell(meta, member.phone() == null ? "—" : member.phone(), regularFont);
        }

        if (invoice.getStatus() == InvoiceStatus.REFUNDED) {
            addMetaCell(meta, "Ngày hoàn tiền:", boldFont);
            addMetaCell(meta, formatVnTime(invoice.getRefundedAt()), regularFont);
            addMetaCell(meta, "Lý do hoàn tiền:", boldFont);
            addMetaCell(meta, invoice.getRefundReason() == null ? "—" : invoice.getRefundReason(), regularFont);
        }

        doc.add(meta);
    }

    private void addItemsTable(Document doc, List<InvoiceItem> items, Font boldFont, Font regularFont)
            throws DocumentException {
        // BR-F5-09: tên dịch vụ/gói tập và đơn giá luôn lấy từ invoice_items (chụp tại thời điểm bán).
        PdfPTable table = new PdfPTable(new float[]{0.6f, 3.6f, 0.7f, 1.5f, 1.6f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(10f);

        addHeaderCell(table, "STT", boldFont, Element.ALIGN_CENTER);
        addHeaderCell(table, "Dịch vụ / Gói tập", boldFont, Element.ALIGN_LEFT);
        addHeaderCell(table, "SL", boldFont, Element.ALIGN_CENTER);
        addHeaderCell(table, "Đơn giá", boldFont, Element.ALIGN_RIGHT);
        addHeaderCell(table, "Thành tiền", boldFont, Element.ALIGN_RIGHT);

        int index = 1;
        for (InvoiceItem item : items) {
            addDataCell(table, String.valueOf(index++), regularFont, Element.ALIGN_CENTER);
            addDataCell(table, item.getDescription(), regularFont, Element.ALIGN_LEFT);
            addDataCell(table, String.valueOf(item.getQuantity()), regularFont, Element.ALIGN_CENTER);
            addDataCell(table, formatVnd(item.getUnitPrice()), regularFont, Element.ALIGN_RIGHT);
            addDataCell(table, formatVnd(item.getAmount()), regularFont, Element.ALIGN_RIGHT);
        }
        doc.add(table);
    }

    private void addTotalsBlock(Document doc, Invoice invoice, Font boldFont, Font regularFont)
            throws DocumentException {
        PdfPTable totals = new PdfPTable(new float[]{4.5f, 1.8f, 1.7f});
        totals.setWidthPercentage(100);
        totals.setSpacingAfter(14f);

        addTotalRow(totals, "Tạm tính:", formatVnd(invoice.getSubtotal()), regularFont);
        addTotalRow(totals, "Giảm giá:", formatVnd(invoice.getDiscount()), regularFont);
        addTotalRow(totals, "Tổng thanh toán:", formatVnd(invoice.getTotal()), boldFont);

        doc.add(totals);
    }

    private void addPaymentsSection(Document doc, List<InvoicePaymentDto> payments,
            Font sectionFont, Font boldFont, Font regularFont) throws DocumentException {
        if (payments.isEmpty()) {
            return;
        }
        Paragraph heading = new Paragraph("Thông tin thanh toán", sectionFont);
        heading.setSpacingAfter(6f);
        doc.add(heading);

        PdfPTable table = new PdfPTable(new float[]{1.8f, 1.6f, 1.8f, 1.6f, 1.2f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(12f);

        addHeaderCell(table, "Phương thức", boldFont, Element.ALIGN_LEFT);
        addHeaderCell(table, "Mã giao dịch", boldFont, Element.ALIGN_LEFT);
        addHeaderCell(table, "Người thu", boldFont, Element.ALIGN_LEFT);
        addHeaderCell(table, "Thời gian", boldFont, Element.ALIGN_LEFT);
        addHeaderCell(table, "Trạng thái", boldFont, Element.ALIGN_LEFT);

        for (InvoicePaymentDto p : payments) {
            addDataCell(table, paymentMethodLabel(p.method()), regularFont, Element.ALIGN_LEFT);
            addDataCell(table, p.transactionRef() == null || p.transactionRef().isBlank() ? "—" : p.transactionRef(),
                regularFont, Element.ALIGN_LEFT);
            addDataCell(table, p.receivedByName() == null ? "—" : p.receivedByName(), regularFont, Element.ALIGN_LEFT);
            addDataCell(table, formatVnTime(p.paidAt()), regularFont, Element.ALIGN_LEFT);
            addDataCell(table, paymentStatusLabel(p.status()), regularFont, Element.ALIGN_LEFT);
        }
        doc.add(table);
    }

    private void addFooter(Document doc, Font smallFont) throws DocumentException {
        Paragraph note = new Paragraph("Cảm ơn quý khách đã sử dụng dịch vụ! Hóa đơn này dùng cho mục đích xác nhận "
            + "thanh toán tại phòng tập.", smallFont);
        note.setAlignment(Element.ALIGN_CENTER);
        note.setSpacingBefore(12f);
        doc.add(note);
    }

    private static void addMetaCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(3f);
        cell.setPaddingBottom(3f);
        table.addCell(cell);
    }

    private static void addHeaderCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorderColor(BORDER_COLOR);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private static void addDataCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(BORDER_COLOR);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private static void addTotalRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell spacer = new PdfPCell(new Phrase(""));
        spacer.setBorder(Rectangle.NO_BORDER);
        table.addCell(spacer);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPaddingTop(3f);
        labelCell.setPaddingBottom(3f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPaddingTop(3f);
        valueCell.setPaddingBottom(3f);
        table.addCell(valueCell);
    }

    private static String invoiceStatusLabel(InvoiceStatus status) {
        return switch (status) {
            case PAID -> "Đã thanh toán";
            case REFUNDED -> "Đã hoàn tiền";
            case PENDING -> "Chờ thanh toán";
            case CANCELLED -> "Đã hủy";
        };
    }

    private static String paymentMethodLabel(PaymentMethod method) {
        return switch (method) {
            case CASH -> "Tiền mặt";
            case BANK_TRANSFER -> "Chuyển khoản ngân hàng";
        };
    }

    private static String paymentStatusLabel(PaymentStatus status) {
        return switch (status) {
            case CONFIRMED -> "Đã xác nhận";
            case CANCELLED -> "Đã hủy";
        };
    }

    private static String formatVnTime(OffsetDateTime instant) {
        if (instant == null) {
            return "—";
        }
        return instant.atZoneSameInstant(VnTime.ZONE).format(VN_DATE_TIME);
    }

    private static String formatVnd(Integer amount) {
        int value = amount == null ? 0 : amount;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,###", symbols).format(value) + " đ";
    }

    private static byte[] loadFontBytes(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể nạp file font PDF: " + classpathLocation, ex);
        }
    }
}
