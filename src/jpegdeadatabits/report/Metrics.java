package jpegdeadatabits.report;

import java.awt.image.BufferedImage;

/**
 * Görüntü kalite ve sıkıştırma metrikleri.
 *
 * KALİTE METRİKLERİ (orijinal ile geri-çatılmış görüntü arasında):
 *   - MSE  : Ortalama Kare Hata (düşük = iyi)
 *   - PSNR : Tepe Sinyal-Gürültü Oranı, dB (yüksek = iyi)
 *   - SSIM : Yapısal Benzerlik İndeksi, [0,1] (1'e yakın = iyi)
 *
 * SIKIŞTIRMA METRİKLERİ:
 *   - CR   : Sıkıştırma Oranı = orijinal_bit / sıkıştırılmış_bit (yüksek = iyi)
 *   - BPP  : Piksel Başına Bit = sıkıştırılmış_bit / piksel_sayısı (düşük = iyi)
 *
 * ÖNEMLİ NOT: Kalite metrikleri (MSE/PSNR/SSIM) yalnızca kuantizasyona
 * (Q faktörüne) bağlıdır. Entropi kodlayıcı (Huffman veya DEA) KAYIPSIZDIR;
 * ikisi de aynı kuantize katsayıları kodlar. Bu nedenle Huffman ve DEA için
 * kalite metrikleri AYNIDIR. Fark yalnızca bit sayısında (dolayısıyla CR/BPP'de)
 * ortaya çıkar. Bu, çalışmanın temel bulgusudur: DEA aynı görsel kaliteyi daha
 * az bitle sağlar.
 */
public class Metrics {

    /** RGB görüntüler arası MSE (3 kanal ortalaması) */
    public static double mse(BufferedImage a, BufferedImage b) {
        int W = a.getWidth(), H = a.getHeight();
        double sum = 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int p1 = a.getRGB(x, y), p2 = b.getRGB(x, y);
                int r1 = (p1 >> 16) & 0xff, g1 = (p1 >> 8) & 0xff, b1 = p1 & 0xff;
                int r2 = (p2 >> 16) & 0xff, g2 = (p2 >> 8) & 0xff, b2 = p2 & 0xff;
                sum += (r1 - r2) * (r1 - r2);
                sum += (g1 - g2) * (g1 - g2);
                sum += (b1 - b2) * (b1 - b2);
            }
        }
        return sum / (W * H * 3.0);
    }

    /** PSNR (dB) — MSE'den türetilir. MSE=0 ise +sonsuz (mükemmel) döner. */
    public static double psnr(double mse) {
        if (mse <= 0) return Double.POSITIVE_INFINITY;
        return 10.0 * Math.log10(255.0 * 255.0 / mse);
    }

    /**
     * SSIM — luma (Y) kanalı üzerinden, 8×8 kayan pencere ile hesaplanır.
     * Wang ve diğerleri (2004) formülü. Sabitler: C1=(0.01·255)², C2=(0.03·255)².
     * Görüntü luma'ya çevrilir, pencereler arası ortalama SSIM döndürülür.
     */
    public static double ssim(BufferedImage a, BufferedImage b) {
        int W = a.getWidth(), H = a.getHeight();
        double[][] ya = toLuma(a), yb = toLuma(b);

        final int win = 8;
        final double C1 = (0.01 * 255) * (0.01 * 255);
        final double C2 = (0.03 * 255) * (0.03 * 255);

        double total = 0;
        int count = 0;

        for (int by = 0; by + win <= H; by += win) {
            for (int bx = 0; bx + win <= W; bx += win) {
                double muA = 0, muB = 0;
                for (int i = 0; i < win; i++)
                    for (int j = 0; j < win; j++) {
                        muA += ya[by + i][bx + j];
                        muB += yb[by + i][bx + j];
                    }
                int n = win * win;
                muA /= n; muB /= n;

                double varA = 0, varB = 0, cov = 0;
                for (int i = 0; i < win; i++)
                    for (int j = 0; j < win; j++) {
                        double da = ya[by + i][bx + j] - muA;
                        double db = yb[by + i][bx + j] - muB;
                        varA += da * da;
                        varB += db * db;
                        cov  += da * db;
                    }
                varA /= (n - 1); varB /= (n - 1); cov /= (n - 1);

                double s = ((2 * muA * muB + C1) * (2 * cov + C2))
                         / ((muA * muA + muB * muB + C1) * (varA + varB + C2));
                total += s;
                count++;
            }
        }
        return count == 0 ? 1.0 : total / count;
    }

    /** RGB → luma (Y, BT.601) */
    private static double[][] toLuma(BufferedImage img) {
        int W = img.getWidth(), H = img.getHeight();
        double[][] y = new double[H][W];
        for (int j = 0; j < H; j++) {
            for (int i = 0; i < W; i++) {
                int rgb = img.getRGB(i, j);
                int r = (rgb >> 16) & 0xff, g = (rgb >> 8) & 0xff, b = rgb & 0xff;
                y[j][i] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
        }
        return y;
    }

    /**
     * Sıkıştırma oranı: CR = orijinal_bit / sıkıştırılmış_bit.
     * Orijinal boyut, sıkıştırılmamış RGB kabul edilir: W·H·3·8 bit.
     */
    public static double compressionRatio(int width, int height, long compressedBits) {
        long originalBits = (long) width * height * 3 * 8;
        return (double) originalBits / compressedBits;
    }

    /** Piksel başına bit: BPP = sıkıştırılmış_bit / (W·H) */
    public static double bpp(int width, int height, long compressedBits) {
        return (double) compressedBits / ((long) width * height);
    }
}
