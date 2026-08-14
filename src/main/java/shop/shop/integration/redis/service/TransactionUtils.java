// class chạy hành động cụ thể xóa cache v.v sau khi commit transaction or chạy ngay néu k trong transaction
package shop.shop.integration.redis.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TransactionUtils {

    private TransactionUtils() {
    }

    // Chạy action ngay nếu không có transaction, hoặc sau khi commit thành công nếu
    // đang trong transaction dùng đề xóa cache khi có thao tác thay dổi dữ liệu
    public static void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}