package com.tariki.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;

final class DocumentImages {
    private DocumentImages() { }

    static byte[] decode(String dataUrl, boolean signature) {
        try {
            if (dataUrl == null || dataUrl.length() > (signature ? 400000 : 1500000)) throw new IOException();
            String prefix = dataUrl.startsWith("data:image/png;base64,") ? "data:image/png;base64,"
                    : !signature && dataUrl.startsWith("data:image/jpeg;base64,") ? "data:image/jpeg;base64," : null;
            if (prefix == null) throw new IOException();
            byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(prefix.length()));
            BufferedImage image;
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IOException();
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    String format = reader.getFormatName();
                    if (!"png".equalsIgnoreCase(format) && (signature || !"jpeg".equalsIgnoreCase(format))) throw new IOException();
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 16 || height < 16 || width > 2000 || height > 1200 || (long) width * height > 2000000) throw new IOException();
                    image = reader.read(0);
                } finally { reader.dispose(); }
            }
            if (signature) {
                int ink = 0, minX = image.getWidth(), maxX = 0, minY = image.getHeight(), maxY = 0;
                for (int y=0; y<image.getHeight(); y++) for (int x=0; x<image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y);
                    if ((rgb >>> 24) > 100 && (((rgb >> 16) & 255) + ((rgb >> 8) & 255) + (rgb & 255)) < 550) {
                        ink++; minX = Math.min(minX,x); maxX = Math.max(maxX,x); minY = Math.min(minY,y); maxY = Math.max(maxY,y);
                    }
                }
                if (ink < 30 || maxX-minX < 20 || maxY-minY < 5 || ink > image.getWidth()*image.getHeight()*0.6) throw new IOException();
            }
            var output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            if (output.size() > (signature ? 1000000 : 2000000)) throw new IOException();
            return output.toByteArray();
        } catch (IOException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, signature
                    ? "Signature PNG invalide ou vide (2000 x 1200 maximum)" : "Logo PNG/JPEG invalide (1 Mo, 2000 x 1200 maximum)");
        }
    }
}
