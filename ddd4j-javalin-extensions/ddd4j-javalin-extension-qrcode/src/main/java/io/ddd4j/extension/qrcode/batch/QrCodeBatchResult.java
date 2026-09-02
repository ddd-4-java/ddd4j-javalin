package io.ddd4j.extension.qrcode.batch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/** Ordered, non-atomic batch result. */
@Getter
public final class QrCodeBatchResult {

    private final List<QrCodeBatchItemResult> items;

    public QrCodeBatchResult(List<QrCodeBatchItemResult> items) {
        this.items = Collections.unmodifiableList(new ArrayList<QrCodeBatchItemResult>(items));
    }

    public long successCount() {
        return items.stream().filter(QrCodeBatchItemResult::isSuccess).count();
    }

    public long failureCount() {
        return items.size() - successCount();
    }
}
