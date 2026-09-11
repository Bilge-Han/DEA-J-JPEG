package jpegdeadatabits.entropy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dynamic Bit-Level Encoding Algorithm (DEA).
 *
 * Erdal & Önal (2025) tarafından önerilen koşullu (conditional) Huffman tabanlı
 * entropi kodlama yönteminin JPEG akışına uyarlanmış versiyonudur.
 *
 * KLASİK HUFFMAN'DAN FARKI:
 *   Klasik Huffman, her sembolün koşulsuz (marjinal) frekansını kullanır.
 *   DEA, "bir önceki sembol verildiğinde mevcut sembolün koşullu frekans
 *   dağılımı" üzerinden kodlama yapar. Yani her olası "prev" değeri için
 *   ayrı bir Huffman ağacı kurulur.
 *
 * TEMEL FİKİR:
 *   Doğal görüntülerde komşu pikseller (ve dolayısıyla komşu DCT katsayıları,
 *   komşu RLE pair'leri) birbirine benzer değerler alma eğilimindedir. DEA
 *   bu komşuluk ilişkisini doğrudan istatistiksel modele dahil eder. Bilgi
 *   kuramsal temeli: H(X|Y) ≤ H(X), yani koşullu entropi marjinal entropiden
 *   küçük veya eşittir.
 *
 * BU SÜRÜMÜN AMACI:
 *   Yalnızca veri bitlerini saymak. Conditional ağaçların yan bilgi maliyeti
 *   bu hesaba dahil değildir. Bu, DEA'nın saf istatistiksel model gücünü
 *   Huffman'a karşı doğrudan karşılaştırma olanağı sağlar.
 *
 * JPEG İÇİN UYARLAMA:
 *   Orijinal DEA piksel verileri (0-255) için tasarlanmıştır. Bu sürümde
 *   DEA'nın iç mantığı değiştirilmemiş; sembol uzayı tip-bağımsız (generic)
 *   yapılarak JPEG RLE çıktısı olan Pair (run, value) çiftleri ve DC fark
 *   değerleri ile çalışabilir hale getirilmiştir.
 */
public class DEAEncoder {

    /**
     * Sembol dizisini DEA (conditional Huffman) ile kodlar, yalnızca veri
     * biti sayısını döner.
     *
     * @param symbols kodlanacak sembol dizisi
     * @return sıkıştırılmış verinin bit sayısı (ağaç maliyeti dahil DEĞİL)
     */
    public static <T> long encodeDataBits(List<T> symbols) {
        if (symbols.size() < 2) {
            // İki sembolden az ise conditional model anlamsız;
            // sembol başına sabit 16 bit varsay
            return symbols.size() * 16L;
        }

        // 1. Koşullu frekanslar oluştur: prev_sym → {next_sym → count}
        Map<T, Map<T, Integer>> conditional = new HashMap<>();
        for (int i = 1; i < symbols.size(); i++) {
            T prev = symbols.get(i - 1);
            T curr = symbols.get(i);
            conditional
                    .computeIfAbsent(prev, k -> new HashMap<>())
                    .merge(curr, 1, Integer::sum);
        }

        // 2. Her prev için ayrı Huffman kod sözlüğü oluştur
        Map<T, Map<T, String>> codeMaps = new HashMap<>();
        for (var entry : conditional.entrySet()) {
            codeMaps.put(entry.getKey(), HuffmanNode.buildCodes(entry.getValue()));
        }

        // 3. İlk sembol için raw bit sayısı (alfabe büyüklüğüne göre)
        Set<T> allSymbols = new HashSet<>(symbols);
        int rawBits = Math.max(8,
                (int) Math.ceil(Math.log(Math.max(2, allSymbols.size())) / Math.log(2)));

        // 4. İlk sembol raw, sonrakiler conditional ağaçtan
        long totalBits = rawBits;
        for (int i = 1; i < symbols.size(); i++) {
            T prev = symbols.get(i - 1);
            T curr = symbols.get(i);
            Map<T, String> codes = codeMaps.get(prev);
            String code = (codes != null) ? codes.get(curr) : null;
            // codes null olamaz çünkü prev'i mutlaka conditional'a eklemiştik
            // ama emniyet için fallback olarak rawBits kullan
            totalBits += (code != null) ? code.length() : rawBits;
        }

        return totalBits;
    }
}
