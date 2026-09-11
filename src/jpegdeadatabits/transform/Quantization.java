package jpegdeadatabits.transform;

/**
 * JPEG standart kuantizasyon matrisleri (Annex K, ISO/IEC 10918-1).
 *
 * Bu matrisler insan görsel sisteminin frekans duyarlılığına göre
 * ayarlanmıştır:
 *   - Düşük frekans (sol üst)  → küçük değer  → az kayıp
 *   - Yüksek frekans (sağ alt) → büyük değer → çok kayıp
 *
 * Quality Factor (QF) 1-100 aralığında değer alır:
 *   QF = 50  → standart matris (baz)
 *   QF < 50  → scale = 5000/QF, agresif kuantizasyon (düşük kalite)
 *   QF > 50  → scale = 200 - 2*QF, hafif kuantizasyon (yüksek kalite)
 *
 * Kuantizasyon (kayıplı):   F'(u,v) = round( F(u,v) / Q(u,v) )
 */
public class Quantization {

    /** JPEG standart Y (luma) kuantizasyon matrisi - QF=50 referansı */
    public static final int[][] Q_LUMA = {
        {16, 11, 10, 16, 24,  40,  51,  61},
        {12, 12, 14, 19, 26,  58,  60,  55},
        {14, 13, 16, 24, 40,  57,  69,  56},
        {14, 17, 22, 29, 51,  87,  80,  62},
        {18, 22, 37, 56, 68,  109, 103, 77},
        {24, 35, 55, 64, 81,  104, 113, 92},
        {49, 64, 78, 87, 103, 121, 120, 101},
        {72, 92, 95, 98, 112, 100, 103, 99}
    };

    /** JPEG standart Cb/Cr (chroma) kuantizasyon matrisi - QF=50 referansı */
    public static final int[][] Q_CHROMA = {
        {17, 18, 24, 47, 99, 99, 99, 99},
        {18, 21, 26, 66, 99, 99, 99, 99},
        {24, 26, 56, 99, 99, 99, 99, 99},
        {47, 66, 99, 99, 99, 99, 99, 99},
        {99, 99, 99, 99, 99, 99, 99, 99},
        {99, 99, 99, 99, 99, 99, 99, 99},
        {99, 99, 99, 99, 99, 99, 99, 99},
        {99, 99, 99, 99, 99, 99, 99, 99}
    };

    /** Quality factor (1-100) parametresine göre ölçeklenmiş matris döner */
    public static int[][] scaledMatrix(int[][] base, int quality) {
        double s = (quality < 50) ? 5000.0 / quality : 200.0 - 2.0 * quality;
        int[][] out = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int v = (int) Math.floor((base[i][j] * s + 50) / 100);
                out[i][j] = Math.max(1, Math.min(255, v));
            }
        }
        return out;
    }

    /** DCT katsayılarını kuantize eder: F'(u,v) = round( F(u,v) / Q(u,v) ) */
    public static int[][] quantize(double[][] coef, int[][] qMatrix) {
        int[][] out = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                out[i][j] = (int) Math.round(coef[i][j] / qMatrix[i][j]);
            }
        }
        return out;
    }

    /** Ters kuantizasyon: F(u,v) = F'(u,v) · Q(u,v) — reconstruction için */
    public static double[][] dequantize(int[][] quant, int[][] qMatrix) {
        double[][] out = new double[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                out[i][j] = (double) quant[i][j] * qMatrix[i][j];
            }
        }
        return out;
    }
}
