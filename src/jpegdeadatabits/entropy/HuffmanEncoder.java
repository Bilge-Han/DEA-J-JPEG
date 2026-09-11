package jpegdeadatabits.entropy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standart Huffman entropi kodlayıcısı — JPEG referansı.
 *
 * BU SÜRÜMÜN AMACI: Yalnızca sıkıştırılmış veri bitlerini saymaktır.
 * Huffman ağacının saklama maliyeti (yan bilgi) bu hesaba dahil DEĞİLDİR.
 * Bu, danışman hocanın talebi doğrultusunda DEA'nın saf istatistiksel model
 * gücünü Huffman'ın saf istatistiksel modeline karşı karşılaştırma yapmak
 * içindir.
 *
 * Yöntem:
 *   1. Sembol dizisindeki her sembolün koşulsuz (marjinal) frekansını say
 *   2. Bu frekanslara göre Huffman ağacı kur, kod sözlüğü çıkar
 *   3. Her sembolü kendi koduyla değiştir, toplam bit sayısını döndür
 *
 * Bu, JPEG'in kullandığı standart Huffman'a istatistiksel olarak denktir
 * (JPEG'in DHT tabloları da tek bir global frekansa dayanır).
 */
public class HuffmanEncoder {

    /**
     * Sembol dizisini Huffman ile kodlar, yalnızca veri biti sayısını döner.
     *
     * @param symbols kodlanacak sembol dizisi
     * @return sıkıştırılmış verinin bit sayısı (ağaç maliyeti dahil DEĞİL)
     */
    public static <T> long encodeDataBits(List<T> symbols) {
        if (symbols.isEmpty()) return 0;

        // 1. Frekans say
        Map<T, Integer> freqs = new HashMap<>();
        for (T s : symbols) {
            freqs.merge(s, 1, Integer::sum);
        }

        // 2. Huffman kod sözlüğü oluştur
        Map<T, String> codes = HuffmanNode.buildCodes(freqs);

        // 3. Her sembolün kod uzunluğunu topla
        long totalBits = 0;
        for (T s : symbols) {
            totalBits += codes.get(s).length();
        }

        return totalBits;
    }
}
