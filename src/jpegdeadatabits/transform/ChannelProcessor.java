package jpegdeadatabits.transform;

import jpegdeadatabits.entropy.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Tek bir kanal (Y, Cb veya Cr) için JPEG akışını uygular.
 *
 * Her 8×8 blok için sırasıyla:
 *   1. Level shift (-128) — DCT'yi simetrik [-128, 127] aralığına taşır
 *   2. Forward DCT (8×8)
 *   3. Kuantizasyon (Q matrisine göre)
 *   4. Zigzag tarama
 *   5. RLE (DC ayrı, AC pair listesi)
 *
 * Sonuç: tüm bloklardan elde edilen DC katsayıları listesi ve AC pair'leri.
 * Bu liste daha sonra entropi kodlayıcıya (Huffman veya DEA) verilir.
 *
 * Görüntü boyutu 8'in katı değilse edge replication ile padding uygulanır.
 */
public class ChannelProcessor {

    public static final int BLOCK = 8;

    /**
     * Sonuç: DC katsayıları, AC pair'leri (entropi kodlama için) ve
     * quantize edilmiş bloklar (reconstruction / PSNR-SSIM için).
     * quantBlocks[blockIndex] = o bloğun 8×8 kuantize katsayı matrisi.
     */
    public record Result(List<Integer> dcList, List<Pair> acList,
                         List<int[][]> quantBlocks, int padH, int padW) {}

    public static Result process(double[][] channel, int[][] qMatrix) {
        int H = channel.length;
        int W = channel[0].length;
        int ph = (BLOCK - H % BLOCK) % BLOCK;
        int pw = (BLOCK - W % BLOCK) % BLOCK;
        int padH = H + ph;
        int padW = W + pw;

        // Edge replication padding (8'in katı olmayan kenarlar için)
        double[][] padded = new double[padH][padW];
        for (int y = 0; y < padH; y++) {
            for (int x = 0; x < padW; x++) {
                padded[y][x] = channel[Math.min(y, H - 1)][Math.min(x, W - 1)];
            }
        }

        List<Integer> dcList = new ArrayList<>();
        List<Pair> acList = new ArrayList<>();
        List<int[][]> quantBlocks = new ArrayList<>();

        for (int by = 0; by < padH; by += BLOCK) {
            for (int bx = 0; bx < padW; bx += BLOCK) {
                // 1. Bloğu çıkar ve level shift uygula
                double[][] block = new double[BLOCK][BLOCK];
                for (int i = 0; i < BLOCK; i++) {
                    for (int j = 0; j < BLOCK; j++) {
                        block[i][j] = padded[by + i][bx + j] - 128.0;
                    }
                }

                // 2. Forward DCT
                double[][] coef = DCT8x8.forward(block);

                // 3. Kuantize
                int[][] quant = Quantization.quantize(coef, qMatrix);
                quantBlocks.add(quant);

                // 4. Zigzag tarama
                int[] zigzag = ZigzagScanner.toZigzag(quant);

                // 5. RLE
                dcList.add(RLE.extractDc(zigzag));
                acList.addAll(RLE.encodeAc(zigzag));
            }
        }

        return new Result(dcList, acList, quantBlocks, padH, padW);
    }

    /**
     * Kuantize edilmiş bloklardan kanalı geri oluşturur (dequant → IDCT → +128).
     * PSNR/SSIM hesabı için kullanılır. Çıktı orijinal H×W boyutuna kırpılır.
     */
    public static double[][] reconstruct(List<int[][]> quantBlocks, int[][] qMatrix,
                                         int padH, int padW, int origH, int origW) {
        double[][] padded = new double[padH][padW];
        int blocksPerRow = padW / BLOCK;

        for (int idx = 0; idx < quantBlocks.size(); idx++) {
            int by = (idx / blocksPerRow) * BLOCK;
            int bx = (idx % blocksPerRow) * BLOCK;

            double[][] deq = Quantization.dequantize(quantBlocks.get(idx), qMatrix);
            double[][] spatial = DCT8x8.inverse(deq);

            for (int i = 0; i < BLOCK; i++) {
                for (int j = 0; j < BLOCK; j++) {
                    padded[by + i][bx + j] = spatial[i][j] + 128.0;
                }
            }
        }

        // Orijinal boyuta kırp
        double[][] out = new double[origH][origW];
        for (int y = 0; y < origH; y++) {
            System.arraycopy(padded[y], 0, out[y], 0, origW);
        }
        return out;
    }
}
