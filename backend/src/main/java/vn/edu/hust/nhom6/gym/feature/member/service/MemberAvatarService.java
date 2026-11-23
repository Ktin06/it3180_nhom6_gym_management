package vn.edu.hust.nhom6.gym.feature.member.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberAvatar;
import vn.edu.hust.nhom6.gym.feature.member.repository.MemberAvatarRepository;

/**
 * Nghiệp vụ ảnh đại diện hội viên lưu trong PostgreSQL (F2, ADR 0011).
 *
 * <p>Dùng lại hàm kiểm tra quyền của {@link MemberService} để quyền xem và đổi ảnh luôn đồng nhất
 * với quyền xem và sửa hồ sơ hội viên. Kiểm tra định dạng thực từ nội dung nhị phân, đọc kích thước
 * ảnh bằng {@link ImageReader} trước khi giải nén bitmap, thu nhỏ cạnh dài nhất về tối đa 256 px
 * và nén JPEG chất lượng 0,8 (tối đa 100 KB).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAvatarService {

    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    private static final int MAX_DIMENSION_PX = 8000;
    private static final int TARGET_MAX_EDGE_PX = 256;
    private static final int MAX_STORED_BYTES = 102400;
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";

    private final MemberService members;
    private final MemberAvatarRepository avatars;

    public byte[] getAvatar(CurrentUser actor, Long memberId) {
        members.requireReadable(actor, memberId);
        MemberAvatar avatar = avatars.findByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException("Hội viên chưa có ảnh đại diện."));
        return avatar.getContent();
    }

    @Transactional
    public void uploadAvatar(CurrentUser actor, Long memberId, MultipartFile file) {
        members.requireEditableForUpdate(actor, memberId);
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Vui lòng chọn file ảnh đại diện.");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_UPLOAD_BYTES);
        }

        byte[] rawBytes;
        try {
            rawBytes = file.getBytes();
        } catch (IOException ex) {
            throw new InvalidRequestException("Không thể đọc dữ liệu file tải lên.");
        }
        if (rawBytes.length > MAX_UPLOAD_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_UPLOAD_BYTES);
        }

        byte[] compressedJpeg = processAndCompress(rawBytes);

        MemberAvatar avatar = avatars.findByMemberId(memberId).orElseGet(MemberAvatar::new);
        avatar.setMemberId(memberId);
        avatar.setContent(compressedJpeg);
        avatar.setContentType(JPEG_CONTENT_TYPE);
        avatar.setSizeBytes(compressedJpeg.length);
        avatar.setUploadedBy(actor.userId());
        avatars.saveAndFlush(avatar);
    }

    private static byte[] processAndCompress(byte[] rawBytes) {
        BufferedImage source = readAndValidateImage(rawBytes);
        BufferedImage resized = resizeToRgb(source);
        return compressJpegUnderLimit(resized);
    }

    private static BufferedImage readAndValidateImage(byte[] rawBytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(rawBytes))) {
            if (input == null) {
                throw new InvalidRequestException("File tải lên phải là ảnh JPEG hoặc PNG hợp lệ.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidRequestException("File tải lên phải là ảnh JPEG hoặc PNG hợp lệ.");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!isSupportedFormat(format)) {
                    throw new InvalidRequestException("Chỉ chấp nhận ảnh định dạng JPEG hoặc PNG.");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION_PX || height > MAX_DIMENSION_PX) {
                    throw new InvalidRequestException("Kích thước ảnh không được vượt quá 8000x8000 điểm ảnh.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new InvalidRequestException("Không thể giải mã nội dung ảnh.");
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (InvalidRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidRequestException("File tải lên không phải là ảnh JPEG hoặc PNG hợp lệ.");
        }
    }

    private static boolean isSupportedFormat(String format) {
        if (format == null) {
            return false;
        }
        return "JPEG".equalsIgnoreCase(format)
            || "JPG".equalsIgnoreCase(format)
            || "PNG".equalsIgnoreCase(format);
    }

    private static BufferedImage resizeToRgb(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int targetWidth = width;
        int targetHeight = height;

        int maxEdge = Math.max(width, height);
        if (maxEdge > TARGET_MAX_EDGE_PX) {
            if (width >= height) {
                targetWidth = TARGET_MAX_EDGE_PX;
                targetHeight = Math.max(1, (int) Math.round((double) height * TARGET_MAX_EDGE_PX / width));
            } else {
                targetHeight = TARGET_MAX_EDGE_PX;
                targetWidth = Math.max(1, (int) Math.round((double) width * TARGET_MAX_EDGE_PX / height));
            }
        }

        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetWidth, targetHeight);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static byte[] compressJpegUnderLimit(BufferedImage image) {
        float quality = 0.8f;
        while (quality >= 0.1f) {
            byte[] encoded = encodeJpeg(image, quality);
            if (encoded.length > 0 && encoded.length <= MAX_STORED_BYTES) {
                return encoded;
            }
            quality -= 0.1f;
        }
        throw new InvalidRequestException("Ảnh sau khi nén vẫn vượt quá giới hạn 100 KB.");
    }

    private static byte[] encodeJpeg(BufferedImage image, float quality) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("Không tìm thấy bộ mã hóa JPEG.");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(Math.clamp(quality, 0.1f, 1.0f));
            }
            writer.write(null, new IIOImage(image, null, null), param);
            ios.flush();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new InvalidRequestException("Không thể nén ảnh đại diện.");
        } finally {
            writer.dispose();
        }
    }
}
