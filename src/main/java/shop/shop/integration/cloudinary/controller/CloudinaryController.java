package shop.shop.integration.cloudinary.controller;
// package shop.shop.integration.cloudinary;

// import java.util.List;

// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RequestPart;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.multipart.MultipartFile;

// import lombok.AccessLevel;
// import lombok.RequiredArgsConstructor;
// import lombok.experimental.FieldDefaults;
// import shop.shop.common.dto.response.ApiResponse;

// @RestController
// @RequestMapping("/api/cloudinary")
// @RequiredArgsConstructor
// @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
// public class CloudinaryController {

//     CloudinaryService cloudinaryService;

//     @PostMapping(value = "/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//     public ResponseEntity<ApiResponse<List<CloudinaryImage>>> uploadImages(
//             @RequestPart("files") List<MultipartFile> files,
//             @RequestParam("folder") String folder) {
//         List<CloudinaryImage> images = cloudinaryService.uploadImages(files, folder);
//         return ResponseEntity.ok(ApiResponse.success("Upload anh thanh cong", images));
//     }
// }
