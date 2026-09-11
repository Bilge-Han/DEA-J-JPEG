package jpegdeadatabits.transform;

/**
 * 8×8 Discrete Cosine Transform (DCT).
 *
 * Görüntüdeki uzamsal piksel bloklarını frekans katsayılarına dönüştürür.
 * Doğal görüntülerin enerjisi düşük frekanslarda yoğunlaşır; bu sayede
 * yüksek frekans katsayıları daha agresif kuantize edilebilir.
 *
 * Forward DCT formülü:
 *   F(u,v) = (1/4) C(u) C(v) Σ Σ f(x,y) cos[(2x+1)uπ/16] cos[(2y+1)vπ/16]
 *
 * Burada C(0) = 1/√2, C(k) = 1 (k > 0).
 *
 * Matris çarpımı versiyonu: F = M · f · Mᵀ
 * Bu hem hesaplama hızını arttırır hem de kodu sadeleştirir.
 */
public class DCT8x8 {

    public static final int N = 8;

    /** DCT temel matrisi (8×8) — sınıf yüklenirken bir kez hesaplanır */
    private static final double[][] M = makeDctMatrix();

    private static double[][] makeDctMatrix() {
        double[][] m = new double[N][N];
        for (int k = 0; k < N; k++) {
            double scale = (k == 0) ? 1.0 / Math.sqrt(2) : 1.0;
            for (int n = 0; n < N; n++) {
                m[k][n] = scale * Math.cos((2 * n + 1) * k * Math.PI / 16);
            }
        }
        // sqrt(2/N) normalizasyonu
        double s = Math.sqrt(2.0 / N);
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                m[i][j] *= s;
            }
        }
        return m;
    }

    /**
     * Forward DCT: F = M · block · Mᵀ
     * Girdi: 8×8 piksel bloğu (genellikle level-shifted, [-128, 127])
     * Çıktı: 8×8 frekans katsayısı bloğu
     */
    public static double[][] forward(double[][] block) {
        double[][] tmp = new double[N][N];
        double[][] out = new double[N][N];

        // tmp = M · block
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                double sum = 0;
                for (int k = 0; k < N; k++) sum += M[i][k] * block[k][j];
                tmp[i][j] = sum;
            }
        }
        // out = tmp · Mᵀ
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                double sum = 0;
                for (int k = 0; k < N; k++) sum += tmp[i][k] * M[j][k];
                out[i][j] = sum;
            }
        }
        return out;
    }

    /**
     * Inverse DCT: block = Mᵀ · F · M
     * Girdi: 8×8 frekans katsayısı bloğu (dequantize edilmiş)
     * Çıktı: 8×8 piksel bloğu (hâlâ level-shifted, +128 ekleme çağıran tarafta yapılır)
     *
     * Forward F = M·block·Mᵀ olduğundan, M ortonormal (M⁻¹ = Mᵀ) için
     * ters dönüşüm block = Mᵀ·F·M biçimindedir.
     */
    public static double[][] inverse(double[][] coef) {
        double[][] tmp = new double[N][N];
        double[][] out = new double[N][N];

        // tmp = Mᵀ · coef
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                double sum = 0;
                for (int k = 0; k < N; k++) sum += M[k][i] * coef[k][j];
                tmp[i][j] = sum;
            }
        }
        // out = tmp · M
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                double sum = 0;
                for (int k = 0; k < N; k++) sum += tmp[i][k] * M[k][j];
                out[i][j] = sum;
            }
        }
        return out;
    }
}
