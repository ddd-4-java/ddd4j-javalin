package io.ddd4j.extension.qrcode.resource;

import java.awt.image.BufferedImage;
import java.io.IOException;

/** Framework-neutral port for resolving logos and frame images. */
public interface QrCodeResourceResolver {

    BufferedImage resolve(String location) throws IOException;
}
