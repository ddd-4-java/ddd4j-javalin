package io.ddd4j.extension.validation;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

/**
 * 根据文件内容识别出的真实文件类型。
 *
 * 组件 extension：真实扩展名，不含点号
 * 组件 mimeType：真实 MIME 类型
 */
public final class DetectedFileType {

    private static final long serialVersionUID = 0L;

    private final String extension;

    private final String mimeType;

    @JsonCreator()
    public DetectedFileType(@JsonProperty("extension") String extension, @JsonProperty("mimeType") String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    @JsonProperty("extension")
    public String extension() {
        return extension;
    }

    @JsonProperty("mimeType")
    public String mimeType() {
        return mimeType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (Objects.isNull(obj) || getClass() != obj.getClass()) {
            return false;
        }
        DetectedFileType other = (DetectedFileType) obj;
        return Objects.equals(this.extension, other.extension) && Objects.equals(this.mimeType, other.mimeType);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(extension);
        result = 31 * result + Objects.hashCode(mimeType);
        return result;
    }

    @Override
    public String toString() {
        return "DetectedFileType[extension=" + extension + ", mimeType=" + mimeType + "]";
    }
}
