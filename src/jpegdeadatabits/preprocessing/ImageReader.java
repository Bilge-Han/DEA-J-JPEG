package jpegdeadatabits.preprocessing;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Görüntü dosyası okuyucu.
 *
 * BMP, PNG, JPG, TIFF gibi yaygın formatları okur. Görüntü RGB veya RGBA olarak
 * gelirse RGB formatına dönüştürür (alpha kanalı varsa atılır). Bu, downstream
 * işlemcilerin (renk uzayı dönüşümü, blok işleme) her durumda 3 kanallı RGB
 * verisiyle çalışmasını garanti eder.
 */
public class ImageReader {

    public static BufferedImage read(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new IOException("Görüntü dosyası bulunamadı: " + path);
        }

        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IOException("Görüntü okunamadı (desteklenmeyen format?): " + path);
        }

        // RGB olmayan formatları (RGBA, grayscale, indexed) RGB'ye dönüştür
        if (img.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage rgb = new BufferedImage(
                    img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            rgb.getGraphics().drawImage(img, 0, 0, null);
            img = rgb;
        }

        return img;
    }
}
