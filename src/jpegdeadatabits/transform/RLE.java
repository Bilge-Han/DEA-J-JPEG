package jpegdeadatabits.transform;

import jpegdeadatabits.entropy.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Run-Length Encoding (JPEG standardı tarzı).
 *
 * JPEG'in zigzag çıktısındaki sıfırları "run" sayacı ile kodlama yöntemi:
 *
 *   - DC katsayısı (zigzag[0]) ayrı tutulur; DPCM (delta) ile kodlanır
 *   - AC katsayıları (zigzag[1..63]) için (run, value) çiftleri üretilir:
 *       run   = bu değerden önce kaç sıfır var (0-15 arası)
 *       value = sıfır olmayan katsayı
 *   - Çok uzun sıfır dizileri için ZRL (Zero-Run-Length): (15, 0) → 16 sıfır
 *   - Bloğun kalan sıfırları için EOB (End-Of-Block): (0, 0) → blok bitti
 *
 * Bu yapı, ardından gelen entropi kodlayıcı (Huffman veya DEA) için verimli
 * bir sembol dizisi üretir.
 */
public class RLE {

    /** Zigzag dizisinin ilk elemanını (DC katsayısı) döner */
    public static int extractDc(int[] zigzag) {
        return zigzag[0];
    }

    /** AC katsayılarını (run, value) çiftlerine kodlar, sonda EOB ile bitirir */
    public static List<Pair> encodeAc(int[] zigzag) {
        List<Pair> out = new ArrayList<>();
        int run = 0;
        for (int i = 1; i < 64; i++) {
            if (zigzag[i] == 0) {
                run++;
            } else {
                // 16+ ardışık sıfır varsa ZRL bayrağı ile parçala
                while (run >= 16) {
                    out.add(new Pair(15, 0));
                    run -= 16;
                }
                out.add(new Pair(run, zigzag[i]));
                run = 0;
            }
        }
        out.add(new Pair(0, 0)); // EOB
        return out;
    }

    /** DC katsayıları için DPCM (delta) kodlaması: ilk değer aynı, sonrakiler farkları */
    public static List<Integer> dcDpcm(List<Integer> dcValues) {
        List<Integer> out = new ArrayList<>(dcValues.size());
        if (dcValues.isEmpty()) return out;
        out.add(dcValues.get(0));
        for (int i = 1; i < dcValues.size(); i++) {
            out.add(dcValues.get(i) - dcValues.get(i - 1));
        }
        return out;
    }
}
