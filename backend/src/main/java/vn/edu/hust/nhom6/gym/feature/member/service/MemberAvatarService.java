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
import javax.imageio.ImageReadParam;
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

/** Ảnh hội viên theo ADR 0011; dùng chung quyền hồ sơ, lưu ảnh sau xử lý trong PostgreSQL. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAvatarService {
    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    private static final int MAX_INPUT_EDGE = 8000;
    private static final int MAX_OUTPUT_EDGE = 256;
    private static final int MAX_STORED_BYTES = 102400;

    private final MemberService members;
    private final MemberAvatarRepository avatars;

    public byte[] getAvatar(CurrentUser actor, Long memberId) {
        members.requireReadable(actor, memberId);
        return avatars.findByMemberId(memberId).orElseThrow(
            () -> new NotFoundException("Hội viên chưa có ảnh đại diện.")).getContent();
    }

    @Transactional
    public void uploadAvatar(CurrentUser actor, Long memberId, MultipartFile file) {
        // Khóa dòng cha cả khi chưa có ảnh: hai lần upload đầu tiên không tạo hai dòng (NF6).
        members.requireEditableForUpdate(actor, memberId);
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Vui lòng chọn file ảnh đại diện không rỗng.");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_UPLOAD_BYTES);
        }
        byte[] input;
        try {
            input = file.getBytes();
        } catch (IOException ex) {
            throw new InvalidRequestException("Không thể đọc file ảnh tải lên.");
        }
        if (input.length > MAX_UPLOAD_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_UPLOAD_BYTES);
        }
        byte[] jpeg = compress(readAndResize(input));
        // Chỉ sửa entity sau khi toàn bộ kiểm tra và xử lý ảnh thành công.
        MemberAvatar avatar = avatars.findByMemberId(memberId).orElseGet(MemberAvatar::new);
        avatar.setMemberId(memberId);
        avatar.setContent(jpeg);
        avatar.setContentType("image/jpeg");
        avatar.setSizeBytes(jpeg.length);
        avatar.setUploadedBy(actor.userId());
        avatars.saveAndFlush(avatar);
    }

    private static BufferedImage readAndResize(byte[] input) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!"JPEG".equalsIgnoreCase(format) && !"PNG".equalsIgnoreCase(format)) {
                    throw invalidImage();
                }
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_INPUT_EDGE || height > MAX_INPUT_EDGE) {
                    throw new InvalidRequestException("Mỗi cạnh ảnh phải từ 1 đến 8.000 điểm ảnh.");
                }
                double scale = Math.min(1.0, (double) MAX_OUTPUT_EDGE / Math.max(width, height));
                int targetWidth = Math.max(1, (int) Math.round(width * scale));
                int targetHeight = Math.max(1, (int) Math.round(height * scale));
                // Đọc header trước; lấy mẫu khi giải mã để không giữ bitmap 8000x8000 trong heap.
                ImageReadParam param = reader.getDefaultReadParam();
                int sample = Math.max(1, Math.max(width, height) / MAX_OUTPUT_EDGE);
                param.setSourceSubsampling(sample, sample, 0, 0);
                BufferedImage decoded = reader.read(0, param);
                if (decoded == null) {
                    throw invalidImage();
                }
                return resizeToRgb(decoded, targetWidth, targetHeight);
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw invalidImage();
        }
    }

    private static BufferedImage resizeToRgb(BufferedImage input, int width, int height) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(input, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
            input.flush();
        }
        return output;
    }

    private static byte[] compress(BufferedImage image) {
        try {
            for (int quality = 8; quality >= 1; quality--) {
                byte[] jpeg = encodeJpeg(image, quality / 10.0f);
                if (jpeg.length > 0 && jpeg.length <= MAX_STORED_BYTES) {
                    return jpeg;
                }
            }
            throw new InvalidRequestException("Ảnh sau khi nén vượt quá 100 KiB.");
        } finally {
            image.flush();
        }
    }

    private static byte[] encodeJpeg(BufferedImage image, float quality) {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("JPEG").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), param);
            output.flush();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new InvalidRequestException("Không thể nén ảnh JPEG.");
        } finally {
            writer.dispose();
        }
    }

    private static InvalidRequestException invalidImage() {
        return new InvalidRequestException("File tải lên phải là ảnh JPEG hoặc PNG hợp lệ.");
    }
}
