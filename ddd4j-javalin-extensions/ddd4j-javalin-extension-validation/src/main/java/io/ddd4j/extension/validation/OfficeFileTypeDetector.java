package io.ddd4j.extension.validation;

import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 识别 PDF、Microsoft Office OLE2 与 OOXML 文件的内容检测器。
 */
public final class OfficeFileTypeDetector implements FileTypeDetector {

    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] OLE_SIGNATURE = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final int MAX_ZIP_ENTRIES_TO_INSPECT = 512;

    @Override
    public Optional<DetectedFileType> detect(ValidatableFile file) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        byte[] signature = readSignature(file, OLE_SIGNATURE.length);
        if (startsWith(signature, PDF_SIGNATURE)) {
            return Optional.of(new DetectedFileType("pdf", "application/pdf"));
        }
        if (isZip(signature)) {
            return detectOoxml(file);
        }
        if (startsWith(signature, OLE_SIGNATURE)) {
            return detectOle(file);
        }
        return Optional.empty();
    }

    private byte[] readSignature(ValidatableFile file, int size) throws IOException {
        try (InputStream inputStream = new BufferedInputStream(file.openStream())) {
            return inputStream.readNBytes(size);
        }
    }

    private Optional<DetectedFileType> detectOoxml(ValidatableFile file) throws IOException {
        boolean hasContentTypes = false;
        boolean hasWordDocument = false;
        boolean hasExcelWorkbook = false;
        int inspectedEntries = 0;

        try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(file.openStream()))) {
            ZipEntry entry;
            while (Objects.nonNull(entry = zipInputStream.getNextEntry())
                    && inspectedEntries++ < MAX_ZIP_ENTRIES_TO_INSPECT) {
                String name = entry.getName().replace('\\', '/').toLowerCase(Locale.ROOT);
                hasContentTypes |= "[content_types].xml".equals(name);
                hasWordDocument |= "word/document.xml".equals(name);
                hasExcelWorkbook |= "xl/workbook.xml".equals(name);
            }
        }

        if (hasContentTypes && hasWordDocument) {
            return Optional.of(new DetectedFileType("docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        }
        if (hasContentTypes && hasExcelWorkbook) {
            return Optional.of(new DetectedFileType("xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }
        return Optional.empty();
    }

    private Optional<DetectedFileType> detectOle(ValidatableFile file) throws IOException {
        try (InputStream inputStream = new BufferedInputStream(file.openStream());
                POIFSFileSystem fileSystem = new POIFSFileSystem(inputStream)) {
            DirectoryNode root = fileSystem.getRoot();
            if (root.hasEntry("WordDocument")) {
                return Optional.of(new DetectedFileType("doc", "application/msword"));
            }
            if (root.hasEntry("Workbook") || root.hasEntry("Book")) {
                return Optional.of(new DetectedFileType("xls", "application/vnd.ms-excel"));
            }
            return Optional.empty();
        }
    }

    private boolean startsWith(byte[] source, byte[] prefix) {
        if (source.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (source[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isZip(byte[] signature) {
        return signature.length >= 4
                && signature[0] == 'P'
                && signature[1] == 'K'
                && ((signature[2] == 3 && signature[3] == 4)
                || (signature[2] == 5 && signature[3] == 6)
                || (signature[2] == 7 && signature[3] == 8));
    }
}
