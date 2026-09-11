package jpegdeadatabits.entropy;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Huffman ağacı düğümü ve kod sözlüğü inşası.
 *
 * Tip-bağımsız (generic) — sembol tipi T, Integer (DC farkları için) veya
 * Pair (AC katsayıları için) olabilir. Bu sayede aynı kod hem Huffman hem
 * DEA tarafından farklı sembol uzayları için yeniden kullanılabilir.
 *
 * Klasik Huffman algoritması:
 *   1. Her sembolü düğüm olarak min-heap'e ekle (frekansa göre sıralı)
 *   2. En küçük iki düğümü çıkar, birleştirip yeni ağırlık ile geri koy
 *   3. Tek düğüm kalana kadar tekrarla — bu kök düğümdür
 *   4. Sol kenara "0", sağ kenara "1" atayarak prefix kodlarını çıkar
 */
public class HuffmanNode<T> implements Comparable<HuffmanNode<T>> {

    public int freq;
    public T sym;
    public HuffmanNode<T> left, right;

    public HuffmanNode(int freq, T sym) {
        this.freq = freq;
        this.sym = sym;
    }

    @Override
    public int compareTo(HuffmanNode<T> other) {
        return Integer.compare(this.freq, other.freq);
    }

    /**
     * Frekans tablosundan Huffman ağacı inşa eder ve kod sözlüğü döner.
     *
     * @param freqs sembol → frekans haritası
     * @return sembol → bit dizisi (örn "1010") kod sözlüğü
     */
    public static <T> Map<T, String> buildCodes(Map<T, Integer> freqs) {
        Map<T, String> codes = new HashMap<>();
        if (freqs.isEmpty()) return codes;

        // Tek sembol özel durumu — ona "0" kodu ata
        if (freqs.size() == 1) {
            codes.put(freqs.keySet().iterator().next(), "0");
            return codes;
        }

        PriorityQueue<HuffmanNode<T>> pq = new PriorityQueue<>();
        for (var e : freqs.entrySet()) {
            pq.add(new HuffmanNode<>(e.getValue(), e.getKey()));
        }

        while (pq.size() > 1) {
            HuffmanNode<T> a = pq.poll();
            HuffmanNode<T> b = pq.poll();
            HuffmanNode<T> merged = new HuffmanNode<>(a.freq + b.freq, null);
            merged.left = a;
            merged.right = b;
            pq.add(merged);
        }

        assignCodes(pq.poll(), "", codes);
        return codes;
    }

    /** Ağaçta yürüyerek her yaprağa prefix kodu atar */
    private static <T> void assignCodes(HuffmanNode<T> node, String prefix,
                                         Map<T, String> codes) {
        if (node == null) return;
        if (node.sym != null) {
            codes.put(node.sym, prefix.isEmpty() ? "0" : prefix);
            return;
        }
        assignCodes(node.left,  prefix + "0", codes);
        assignCodes(node.right, prefix + "1", codes);
    }
}
