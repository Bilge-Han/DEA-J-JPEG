package jpegdeadatabits.preprocessing;

/**
 * 4:2:0 Chroma Subsampling.
 *
 * JPEG'in renkli görüntülerde uyguladığı standart subsample adımıdır.
 * Luma kanalı (Y) tam çözünürlükte tutulur; chroma kanalları (Cb, Cr) 2:1
 * oranında her iki eksende küçültülür. Bu, toplam veri miktarını %50 oranında
 * azaltır:
 *
 *   Orijinal: 3 · W · H pixel
 *   4:2:0   : 1 · W · H + 2 · (W/2) · (H/2) = 1.5 · W · H pixel
 *
 * Görsel kalite üzerinde minimal etkisi vardır çünkü insan görsel sistemi
 * renk değişimlerine parlaklık değişimlerinden çok daha az duyarlıdır.
 */
public class ChromaSubsampler {

    /** Chroma kanalını 2x2 ortalama ile downsample eder */
    public static double[][] downsample420(double[][] chroma) {
        int H = chroma.length;
        int W = chroma[0].length;
        int h2 = (H + 1) / 2;
        int w2 = (W + 1) / 2;

        double[][] out = new double[h2][w2];
        for (int y = 0; y < h2; y++) {
            for (int x = 0; x < w2; x++) {
                int y0 = 2 * y, x0 = 2 * x;
                int y1 = Math.min(y0 + 1, H - 1);
                int x1 = Math.min(x0 + 1, W - 1);
                out[y][x] = (chroma[y0][x0] + chroma[y0][x1]
                           + chroma[y1][x0] + chroma[y1][x1]) / 4.0;
            }
        }
        return out;
    }

    /**
     * Downsample edilmiş chroma kanalını orijinal (targetH × targetW) boyutuna
     * geri büyütür. En yakın komşu (nearest-neighbor) yöntemiyle her örnek 2×2
     * piksele kopyalanır — JPEG çözücüsünün tipik yaklaşımı.
     */
    public static double[][] upsample420(double[][] small, int targetH, int targetW) {
        double[][] out = new double[targetH][targetW];
        for (int y = 0; y < targetH; y++) {
            for (int x = 0; x < targetW; x++) {
                out[y][x] = small[y / 2][x / 2];
            }
        }
        return out;
    }
}
