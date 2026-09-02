package io.ddd4j.extension.qrcode;

import java.util.List;

import io.ddd4j.extension.qrcode.batch.QrCodeBatchItem;
import io.ddd4j.extension.qrcode.batch.QrCodeBatchResult;
import io.ddd4j.extension.qrcode.command.DecodeQrCodeCommand;
import io.ddd4j.extension.qrcode.command.GenerateQrCodeCommand;
import io.ddd4j.extension.qrcode.result.QrCodeArtifact;
import io.ddd4j.extension.qrcode.result.QrCodeScanResult;

/** Framework-neutral QR code application service. */
public interface QrCodeService {

    QrCodeArtifact generate(GenerateQrCodeCommand command);

    QrCodeScanResult decode(DecodeQrCodeCommand command);

    QrCodeBatchResult generateBatch(List<QrCodeBatchItem> items);
}
