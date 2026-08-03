package io.ddd4j.javalin.validation;

import io.ddd4j.extension.validation.FileValidationPolicy;
import io.ddd4j.extension.validation.FileValidationService;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.UploadedFile;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JavalinFileValidatorTest {

    private final JavalinFileValidator validator = new JavalinFileValidator(new FileValidationService());

    @Test
    void shouldAcceptRealPdf() throws Exception {
        byte[] content = "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
        UploadedFile uploadedFile = uploadedFile("report.pdf", "application/pdf", content);

        assertThat(validator.validate(uploadedFile, pdfPolicy())).isSameAs(uploadedFile);
    }

    @Test
    void shouldReturnBadRequestForDisguisedExecutable() throws Exception {
        UploadedFile uploadedFile = uploadedFile("report.pdf", "application/pdf",
                new byte[]{0x4D, 0x5A, 0x00, 0x00});

        assertThatThrownBy(() -> validator.validate(uploadedFile, pdfPolicy()))
                .isInstanceOf(BadRequestResponse.class)
                .satisfies(exception -> assertThat(((BadRequestResponse) exception).getDetails())
                        .containsEntry("code", "TYPE_UNDETECTABLE"));
    }

    private FileValidationPolicy pdfPolicy() {
        return FileValidationPolicy.builder()
                .allowedExtensions("pdf")
                .allowedMimeTypes("application/pdf")
                .maxSizeBytes(1024)
                .strict(true)
                .build();
    }

    private UploadedFile uploadedFile(String fileName, String contentType, byte[] content) throws Exception {
        Part part = mock(Part.class);
        when(part.getSubmittedFileName()).thenReturn(fileName);
        when(part.getContentType()).thenReturn(contentType);
        when(part.getSize()).thenReturn((long) content.length);
        when(part.getInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(content));
        return new UploadedFile(part);
    }
}
