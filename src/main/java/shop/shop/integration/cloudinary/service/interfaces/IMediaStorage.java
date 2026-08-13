package shop.shop.integration.cloudinary.service.interfaces;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import shop.shop.integration.cloudinary.DTO.CloudinaryImage;

public interface IMediaStorage {
    List<CloudinaryImage> uploadImages(List<MultipartFile> files, String folder);

    void deleteImage(List<String> publicIds);
}
