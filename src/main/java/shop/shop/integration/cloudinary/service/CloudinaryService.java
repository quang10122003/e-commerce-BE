package shop.shop.integration.cloudinary.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import shop.shop.common.error.ApiError;
import shop.shop.common.error.ErrorCode;
import shop.shop.integration.cloudinary.DTO.CloudinaryImage;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CloudinaryService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "jfif", "png", "gif", "webp", "bmp", "svg", "ico");

    Cloudinary cloudinary;

    public List<CloudinaryImage> uploadImages(List<MultipartFile> files, String folder) {

        // Kiem tra file hop le.
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                validateImageFile(file);
            }
        }

        return files.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> {
                    try {

                        Map uploadResult = cloudinary.uploader().upload(
                                file.getBytes(),
                                ObjectUtils.asMap(
                                        "folder", folder,
                                        "resource_type", "image"));

                        return new CloudinaryImage(
                                uploadResult.get("secure_url").toString(),
                                uploadResult.get("public_id").toString());

                    } catch (Exception e) {

                        Throwable root = e;
                        while (root.getCause() != null) {
                            root = root.getCause();
                        }

                        throw new RuntimeException("Cloudinary upload failed", e);
                    }
                })
                .toList();
    }

    private void validateImageFile(MultipartFile file) {

        String contentType = file.getContentType();
        String extension = getFileExtension(file.getOriginalFilename());

        boolean validContentType = contentType != null && contentType.startsWith("image/");

        boolean validExtension = extension != null &&
                ALLOWED_IMAGE_EXTENSIONS.contains(extension);

        if (!validContentType && !validExtension) {
            throw new ApiError(
                    ErrorCode.BAD_REQUEST,
                    "File không hợp lệ");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return null;
        }

        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    public void deleteImage(List<String> publicIds) {
        publicIds.parallelStream().filter((publicId) -> publicId != null && !publicId.isBlank())
                .forEach((publicId) -> {
                    try {
                        Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                                "resource_type", "image",
                                "invalidate", true));
                        String status = result.get("result").toString();
                        if (!"ok".equals(status) && !"not found".equals(status)) {
                            throw new ApiError(ErrorCode.CLOUDINARY_DELETE_FAILED);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Delete image cloudinary  failed: " + publicId, e);
                    }
                });
    }
}