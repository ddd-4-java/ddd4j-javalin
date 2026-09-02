package io.ddd4j.extension.qrcode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import io.ddd4j.extension.qrcode.batch.QrCodeBatchItem;
import io.ddd4j.extension.qrcode.batch.QrCodeBatchItemResult;
import io.ddd4j.extension.qrcode.batch.QrCodeBatchResult;
import io.ddd4j.extension.qrcode.command.DecodeQrCodeCommand;
import io.ddd4j.extension.qrcode.command.GenerateQrCodeCommand;
import io.ddd4j.extension.qrcode.model.QrCodeDecodeResult;
import io.ddd4j.extension.qrcode.model.QrCodeOutput;
import io.ddd4j.extension.qrcode.result.QrCodeArtifact;
import io.ddd4j.extension.qrcode.result.QrCodeScanResult;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

/** Default QR code service with bounded, order-preserving batch execution. */
public class DefaultQrCodeService implements QrCodeService, AutoCloseable {

    public static final int DEFAULT_MAX_BATCH_SIZE = 100;

    private final ExecutorService executorService;
    private final int maxBatchSize;

    public DefaultQrCodeService() {
        this(Math.min(Runtime.getRuntime().availableProcessors(), 8), DEFAULT_MAX_BATCH_SIZE);
    }

    public DefaultQrCodeService(int concurrency, int maxBatchSize) {
        if (concurrency <= 0 || maxBatchSize <= 0) {
            throw new IllegalArgumentException("concurrency and maxBatchSize must be positive");
        }
        this.executorService = Executors.newFixedThreadPool(concurrency, daemonThreadFactory());
        this.maxBatchSize = maxBatchSize;
    }

    @Override
    public QrCodeArtifact generate(GenerateQrCodeCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.getRequest(), "command.request must not be null");
        return QrCodeArtifact.builder()
                .correlationId(command.getCorrelationId())
                .templateId(command.getTemplateId())
                .output(encode(command.getRequest()))
                .build();
    }

    @Override
    public QrCodeScanResult decode(DecodeQrCodeCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.getRequest(), "command.request must not be null");
        return new QrCodeScanResult(command.getCorrelationId(), decode(command.getRequest().getBytes()));
    }

    @Override
    public QrCodeBatchResult generateBatch(List<QrCodeBatchItem> items) {
        Objects.requireNonNull(items, "items must not be null");
        if (items.size() > maxBatchSize) {
            throw new IllegalArgumentException("batch size must not exceed " + maxBatchSize);
        }
        List<Future<QrCodeBatchItemResult>> futures = new ArrayList<Future<QrCodeBatchItemResult>>(items.size());
        for (QrCodeBatchItem item : items) {
            futures.add(executorService.submit(createBatchTask(item)));
        }
        List<QrCodeBatchItemResult> results = new ArrayList<QrCodeBatchItemResult>(items.size());
        for (int index = 0; index < futures.size(); index++) {
            try {
                results.add(futures.get(index).get());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                results.add(failure(items.get(index), "QRCODE_RENDER_FAILED",
                        "Batch execution was interrupted"));
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause();
                results.add(failure(items.get(index), "QRCODE_RENDER_FAILED", cause.getMessage()));
            }
        }
        return new QrCodeBatchResult(results);
    }

    private Callable<QrCodeBatchItemResult> createBatchTask(final QrCodeBatchItem item) {
        return () -> {
            Objects.requireNonNull(item, "batch item must not be null");
            if (StringUtils.isBlank(item.getItemId())) {
                throw new IllegalArgumentException("batch itemId must not be blank");
            }
            return QrCodeBatchItemResult.builder()
                    .itemId(item.getItemId())
                    .success(true)
                    .artifact(generate(item.getCommand()))
                    .build();
        };
    }

    private QrCodeBatchItemResult failure(QrCodeBatchItem item, String errorCode, String errorMessage) {
        return QrCodeBatchItemResult.builder()
                .itemId(Objects.nonNull(item) ? item.getItemId() : null)
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }

    private QrCodeOutput encode(io.ddd4j.extension.qrcode.model.QrCodeRequest request) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BitMatrix matrix = new QRCodeWriter().encode(request.getContent(), BarcodeFormat.QR_CODE,
                    request.getWidth(), request.getHeight());
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return new QrCodeOutput(output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("QR code encoding failed", exception);
        }
    }

    private List<QrCodeDecodeResult> decode(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (Objects.isNull(image)) {
                throw new IllegalArgumentException("bytes do not contain an image");
            }
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            Result result = new MultiFormatReader().decode(bitmap);
            return List.of(new QrCodeDecodeResult(result.getText()));
        } catch (Exception exception) {
            throw new IllegalStateException("QR code decoding failed", exception);
        }
    }

    private ThreadFactory daemonThreadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "ddd4j-qrcode-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }
}
