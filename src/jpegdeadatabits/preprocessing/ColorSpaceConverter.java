package jpegdeadatabits.preprocessing;

import java.awt.image.BufferedImage;

/**
 * RGB ↔ YCbCr renk uzayı dönüşümü (ITU-R BT.601 standardı).
 *
 * JPEG'in standart ön-işleme adımıdır. İnsan görsel sistemi parlaklığa (luma)
 * renge (chroma) göre çok daha duyarlıdır; bu yüzden chroma kanalları daha
 * agresif şekilde işlenebilir hale gelir.
 *
 * Forward (RGB → YCbCr):
 *   Y  =  0.299 R + 0.587 G + 0.114 B
 *   Cb = -0.169 R - 0.331 G + 0.500 B + 128
 *   Cr =  0.500 R - 0.419 G - 0.081 B + 128
 *
 * Inverse (YCbCr → RGB):
 *   R = Y + 1.402   (Cr - 128)
 *   G = Y - 0.344136(Cb - 128) - 0.714136(Cr - 128)
 *   B = Y + 1.772   (Cb - 128)
 */
public class ColorSpaceConverter {

    /** RGB görüntüyü 3 YCbCr kanalına ayırır. Sonuç: [Y, Cb, Cr] */
    public static double[][][] rgbToYCbCr(BufferedImage img) {
        int W = img.getWidth(), H = img.getHeight();
        double[][] Y  = new double[H][W];
        double[][] Cb = new double[H][W];
        double[][] Cr = new double[H][W];

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >>  8) & 0xff;
                int b =  rgb        & 0xff;
                Y [y][x] = clamp( 0.299  * r + 0.587  * g + 0.114  * b);
                Cb[y][x] = clamp(-0.169  * r - 0.331  * g + 0.500  * b + 128);
                Cr[y][x] = clamp( 0.500  * r - 0.419  * g - 0.081  * b + 128);
            }
        }
        return new double[][][] {Y, Cb, Cr};
    }

    /**
     * YCbCr kanallarını RGB BufferedImage'e geri dönüştürür (reconstruction için).
     * Girdi: tam çözünürlüklü Y, Cb, Cr kanalları (Cb/Cr önceden upsample edilmiş olmalı).
     */
    public static BufferedImage yCbCrToRgb(double[][] Y, double[][] Cb, double[][] Cr) {
        int H = Y.length, W = Y[0].length;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                double yy = Y[y][x];
                double cb = Cb[y][x] - 128.0;
                double cr = Cr[y][x] - 128.0;
                int r = (int) Math.round(yy + 1.402   * cr);
                int g = (int) Math.round(yy - 0.344136 * cb - 0.714136 * cr);
                int b = (int) Math.round(yy + 1.772   * cb);
                r = clampInt(r); g = clampInt(g); b = clampInt(b);
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(255, v));
    }

    private static int clampInt(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
