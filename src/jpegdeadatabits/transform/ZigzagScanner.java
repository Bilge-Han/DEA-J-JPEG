package jpegdeadatabits.transform;

/**
 * Zigzag tarama — 8×8 DCT katsayı matrisini 64 elemanlı 1D diziye dönüştürür.
 *
 * Tarama düzeni (sıra numaraları):
 *    0  1  5  6 14 15 27 28
 *    2  4  7 13 16 26 29 42
 *    3  8 12 17 25 30 41 43
 *    9 11 18 24 31 40 44 53
 *   10 19 23 32 39 45 52 54
 *   20 22 33 38 46 51 55 60
 *   21 34 37 47 50 56 59 61
 *   35 36 48 49 57 58 62 63
 *
 * Sol üstten (DC) sağ alta (en yüksek frekans AC) doğru ilerler. DCT, doğal
 * görüntülerin enerjisini sol üstte topladığı için sıfır olmayan katsayılar
 * dizinin başında, sıfırlar ise sonunda arka arkaya gelir. Bu yapı,
 * Run-Length Encoding için ideal koşulları sağlar.
 */
public class ZigzagScanner {

    /** ZIGZAG_ORDER[k] = matriste 0..63 lineer indeks olarak k. zigzag pozisyonu */
    public static final int[] ZIGZAG_ORDER = {
         0,  1,  8, 16,  9,  2,  3, 10,
        17, 24, 32, 25, 18, 11,  4,  5,
        12, 19, 26, 33, 40, 48, 41, 34,
        27, 20, 13,  6,  7, 14, 21, 28,
        35, 42, 49, 56, 57, 50, 43, 36,
        29, 22, 15, 23, 30, 37, 44, 51,
        58, 59, 52, 45, 38, 31, 39, 46,
        53, 60, 61, 54, 47, 55, 62, 63
    };

    /** 8×8 matrisi → 64 elemanlı zigzag dizisi */
    public static int[] toZigzag(int[][] matrix) {
        int[] out = new int[64];
        for (int k = 0; k < 64; k++) {
            int pos = ZIGZAG_ORDER[k];
            out[k] = matrix[pos / 8][pos % 8];
        }
        return out;
    }

    /** 64 elemanlı zigzag dizisi → 8×8 matris (reconstruction için) */
    public static int[][] fromZigzag(int[] zigzag) {
        int[][] out = new int[8][8];
        for (int k = 0; k < 64; k++) {
            int pos = ZIGZAG_ORDER[k];
            out[pos / 8][pos % 8] = zigzag[k];
        }
        return out;
    }
}
