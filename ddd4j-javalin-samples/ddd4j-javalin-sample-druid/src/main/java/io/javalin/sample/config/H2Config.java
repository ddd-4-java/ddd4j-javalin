package io.javalin.sample.config;

import lombok.Data;

@Data
public class H2Config {
    private Integer webPort;
    private Integer tcpPort;
    private String extraNames;
    private String baseDir;
}
