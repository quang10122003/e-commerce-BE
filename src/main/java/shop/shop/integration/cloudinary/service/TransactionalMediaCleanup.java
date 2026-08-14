package shop.shop.integration.cloudinary.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import shop.shop.common.until.ValidationUtils;
import shop.shop.integration.cloudinary.service.interfaces.IMediaStorage;

@Component
public class TransactionalMediaCleanup {
    IMediaStorage mediaStorage;
    ValidationUtils validationUtils;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    // Thêm publicId<id để xóa ảnh>  hợp lệ vào danh sách, bỏ qua giá trị null hoặc chuỗi rỗng.
    public void addPublicId(List<String> publicIds, String publicId) {
        if (validationUtils.hasText(publicId)) {
            publicIds.add(publicId);
        }
    }

    // Xóa ảnh ngay lập tức, không liên quan transaction (dùng khi lỗi xảy ra trước khi có thao tác DB nào).
    public void deleteNow(List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return;
        }
        mediaStorage.deleteImage(publicIds);
    }
    
    // Chỉ xóa ảnh sau khi transaction DB commit thành công, tránh mất ảnh nếu
    // rollback. Nếu không có transaction nào đang active thì xóa ngay.
    public void deleteAfterCommit(List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return;
        }

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            logger.info("Không có transaction đang chạy, xóa ảnh Cloudinary ngay: {}", publicIds);
            mediaStorage.deleteImage(publicIds);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                logger.info("Transaction commit thành công, xóa ảnh Cloudinary: {}", publicIds);
                mediaStorage.deleteImage(publicIds);
            }
        });
    }

    // Chỉ xóa ảnh khi transaction DB rollback (dùng cho ảnh vừa upload mới,cần dọn nếu DB ghi thất bại).
    public void deleteOnRollback(List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    logger.error("Transaction rollback, xóa ảnh Cloudinary vừa upload: {}", publicIds);
                    mediaStorage.deleteImage(publicIds);
                }
            }
        });
    }
}
