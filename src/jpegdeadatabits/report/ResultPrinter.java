package jpegdeadatabits.report;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test sonuçlarını konsola okunabilir formatta yazdırır.
 *
 * Sonuçlar "DEA kazancı (%)" olarak rapor edilir:
 *   Pozitif değer → DEA Huffman'dan daha fazla sıkıştırdı (kazanç)
 *   Negatif değer → Huffman daha fazla sıkıştırdı
 *
 * Formül: kazanç = (Huffman_bits - DEA_bits) / Huffman_bits × 100
 */
public class ResultPrinter {

    private static final String LINE      = "================================================================";
    private static final String SUBLINE   = "──────────────────────────────────────────────────────────────";

    public static void printHeader() {
        System.out.println(LINE);
        System.out.println("  JPEG-DEA — Veri Biti Karşılaştırması (Ağaç Maliyeti HARİÇ)");
        System.out.println(LINE);
        System.out.println("  JPEG akışı: RGB → YCbCr → 4:2:0 → 8×8 DCT → Quant → Zigzag → RLE → [H | DEA]");
        System.out.println("  Ölçülen   : yalnızca veri bitleri (Huffman ağaç maliyeti dahil değil)");
        System.out.println("  Formül    : DEA Kazancı (%) = (Huffman_bits − DEA_bits) / Huffman_bits × 100");
        System.out.println("  Yorum     : pozitif değer = DEA, Huffman'dan o oranda daha fazla sıkıştırdı");
        System.out.println(LINE);
    }

    /** Tek görüntü için detaylı tablo */
    public static void printImageDetail(String imageName, int w, int h, List<TestResult> results) {
        System.out.println();
        System.out.println(SUBLINE);
        System.out.printf ("  GÖRÜNTÜ: %s  (%d × %d)%n", imageName, w, h);
        System.out.println(SUBLINE);

        // --- Tablo 1: Bit sayıları ve kazanç ---
        System.out.println("  [1] BİT SAYILARI VE DEA KAZANCI");
        System.out.printf ("  %-6s %15s %15s %12s %12s %12s%n",
                "Kalite", "Huffman (bit)", "DEA (bit)",
                "DC Kaz.", "AC Kaz.", "TOPLAM Kaz.");
        for (TestResult r : results) {
            System.out.printf ("  Q=%-4d %,15d %,15d %10.2f%% %10.2f%% %10.2f%%%n",
                    r.quality(),
                    r.huffmanTotalBits(), r.deaTotalBits(),
                    r.dcGain(), r.acGain(), r.overallGain());
        }

        // --- Tablo 2: Sıkıştırma metrikleri (CR, BPP) — Huffman vs DEA ---
        System.out.println();
        System.out.println("  [2] SIKIŞTIRMA METRİKLERİ (Huffman vs DEA)");
        System.out.printf ("  %-6s %11s %11s %11s %11s%n",
                "Kalite", "CR (Huff)", "CR (DEA)", "BPP (Huff)", "BPP (DEA)");
        for (TestResult r : results) {
            System.out.printf ("  Q=%-4d %11.2f %11.2f %11.3f %11.3f%n",
                    r.quality(),
                    r.huffmanCR(), r.deaCR(), r.huffmanBPP(), r.deaBPP());
        }

        // --- Tablo 3: Kalite metrikleri (PSNR, MSE, SSIM) — Q'ya bağlı (ortak) ---
        System.out.println();
        System.out.println("  [3] KALİTE METRİKLERİ (Huffman=DEA — entropi kodlama kayıpsız)");
        System.out.printf ("  %-6s %12s %12s %12s%n",
                "Kalite", "PSNR (dB)", "MSE", "SSIM");
        for (TestResult r : results) {
            System.out.printf ("  Q=%-4d %12.2f %12.2f %12.4f%n",
                    r.quality(), r.psnr(), r.mse(), r.ssim());
        }
    }

    /** Tüm görüntüler için genel özet tablosu */
    public static void printSummary(List<TestResult> allResults) {
        // İsim → (kalite → toplam kazanç) eşlemesini oluştur
        Map<String, Map<Integer, Double>> byName = new LinkedHashMap<>();
        for (TestResult r : allResults) {
            byName.computeIfAbsent(r.imageName(), k -> new LinkedHashMap<>())
                  .put(r.quality(), r.overallGain());
        }

        System.out.println();
        System.out.println(LINE);
        System.out.println("  GENEL ÖZET — DEA'nın Huffman'a Göre Sıkıştırma Kazancı (%)");
        System.out.println(LINE);
        System.out.printf ("  %-15s %10s %10s %10s %10s%n",
                "Görüntü", "Q=25", "Q=50", "Q=75", "Q=90");
        System.out.println("  " + "─".repeat(60));

        double avgQ25 = 0, avgQ50 = 0, avgQ75 = 0, avgQ90 = 0;
        int count = 0;

        for (var entry : byName.entrySet()) {
            Map<Integer, Double> q = entry.getValue();
            double q25 = q.getOrDefault(25, 0.0);
            double q50 = q.getOrDefault(50, 0.0);
            double q75 = q.getOrDefault(75, 0.0);
            double q90 = q.getOrDefault(90, 0.0);

            System.out.printf ("  %-15s  %8.2f%%  %8.2f%%  %8.2f%%  %8.2f%%%n",
                    entry.getKey(), q25, q50, q75, q90);

            avgQ25 += q25; avgQ50 += q50; avgQ75 += q75; avgQ90 += q90;
            count++;
        }

        if (count > 0) {
            System.out.println("  " + "─".repeat(60));
            System.out.printf ("  %-15s  %8.2f%%  %8.2f%%  %8.2f%%  %8.2f%%%n",
                    "ORTALAMA",
                    avgQ25/count, avgQ50/count, avgQ75/count, avgQ90/count);
        }

        System.out.println();
        System.out.println(LINE);
        System.out.println("  YORUM:");
        System.out.println("    • Pozitif değer = DEA, Huffman'dan o oranda daha fazla sıkıştırdı ✓");
        System.out.println("    • Negatif değer = Huffman daha fazla sıkıştırdı");
        System.out.println();
        if (count > 0) {
            double overallAvg = (avgQ25 + avgQ50 + avgQ75 + avgQ90) / (count * 4);
            System.out.printf ("  → DEA, tüm testlerde ortalama %%%.2f daha fazla sıkıştırma sağlamıştır.%n",
                    overallAvg);
        }
        System.out.println(LINE);
    }
}
