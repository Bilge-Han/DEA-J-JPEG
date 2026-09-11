package jpegdeadatabits;

import jpegdeadatabits.entropy.DEAEncoder;
import jpegdeadatabits.entropy.HuffmanEncoder;
import jpegdeadatabits.entropy.Pair;
import jpegdeadatabits.preprocessing.ChromaSubsampler;
import jpegdeadatabits.preprocessing.ColorSpaceConverter;
import jpegdeadatabits.preprocessing.ImageReader;
import jpegdeadatabits.report.Metrics;
import jpegdeadatabits.report.ResultPrinter;
import jpegdeadatabits.report.TestResult;
import jpegdeadatabits.transform.ChannelProcessor;
import jpegdeadatabits.transform.Quantization;
import jpegdeadatabits.transform.RLE;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JPEG-DEA Data Bits Comparison — Ana Test Programı
 * ==================================================
 *
 * AMAÇ:
 *   Standart JPEG akışında Huffman yerine DEA kullanıldığında üretilen veri
 *   bit sayılarını karşılaştırmak. Yan bilgi (Huffman ağacı saklama maliyeti)
 *   bu karşılaştırmaya dahil değildir. Bu yaklaşım, DEA'nın saf istatistiksel
 *   model gücünü değerlendirmeye olanak tanır.
 *
 * KULLANIM:
 *   1. src/jpegdeadatabits/images/ klasörüne test görüntülerini koy
 *      (baboon.png, lena.png, peppers.png, natural.png gibi)
 *   2. Main.java'yı çalıştır
 *   3. Konsol çıktısında her görüntü ve kalite seviyesi için detaylı sonuçlar
 *      + genel özet tablosu görüntülenir
 *
 * AKIŞ:
 *   Her görüntü için her kalite seviyesinde:
 *     1. Görüntüyü oku
 *     2. RGB → YCbCr dönüşümü
 *     3. Chroma kanallarına 4:2:0 subsample
 *     4. Her kanal için: 8x8 blok → DCT → Quant → Zigzag → RLE
 *     5. DC için DPCM, sonra DC ve AC sembollerini birleştir
 *     6. Aynı sembol listeleri üzerinde Huffman ve DEA çalıştır
 *     7. Sonuçları topla, en sonda özet tabloyu yazdır
 */
public class Main {

    // ==================== TEST PARAMETRELERİ ====================

    /** Test edilecek görüntü dosyaları (src/jpegdeadatabits/images/ altında) */
    private static final String[] IMAGE_FILES = {
            "baboon.png",
            "lena.png",
            "peppers.png",
            "natural.png",
            "highfreq.png",
            "smooth.png",
            "blocks.png"
    };

    /** Test edilecek JPEG kalite faktörleri */
    private static final int[] QUALITIES = {25, 50, 75, 90};

    /** Görüntülerin bulunduğu klasör */
    private static final String IMAGES_DIR = "src/jpegdeadatabits/images";

    // ============================================================

    public static void main(String[] args) {
        ResultPrinter.printHeader();

        List<TestResult> allResults = new ArrayList<>();

        // images/ klasöründeki tüm görselleri otomatik tara (.png .jpg .jpeg .bmp)
        File imagesDir = new File(IMAGES_DIR);
        File[] found = imagesDir.listFiles((d, n) -> {
            String ln = n.toLowerCase();
            return ln.endsWith(".png") || ln.endsWith(".jpg")
                || ln.endsWith(".jpeg") || ln.endsWith(".bmp")
                || ln.endsWith(".tif") || ln.endsWith(".tiff");
        });
        if (found == null || found.length == 0) {
            System.out.println();
            System.out.println("HİÇBİR GÖRÜNTÜ BULUNAMADI!");
            System.out.println("Lütfen şu klasöre görsel koyun: " + IMAGES_DIR);
            return;
        }
        java.util.Arrays.sort(found, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        for (File file : found) {
            String fileName = file.getName();
            String path = file.getPath();

            if (!file.exists()) {
                System.out.println();
                System.out.println("[ATLA] Görüntü bulunamadı: " + path);
                continue;
            }

            try {
                BufferedImage img = ImageReader.read(path);
                String name = fileName.replaceAll("(?i)\\.(png|jpg|jpeg|bmp|tif|tiff)$", "");

                // Her kalite seviyesi için test yap
                List<TestResult> imageResults = new ArrayList<>();
                for (int q : QUALITIES) {
                    TestResult result = runTest(img, name, q);
                    imageResults.add(result);
                    allResults.add(result);
                }

                ResultPrinter.printImageDetail(name, img.getWidth(), img.getHeight(), imageResults);

            } catch (IOException e) {
                System.err.println("[HATA] " + path + " okunamadı: " + e.getMessage());
            }
        }

        // Tüm sonuçların özet tablosu
        if (!allResults.isEmpty()) {
            ResultPrinter.printSummary(allResults);
        } else {
            System.out.println();
            System.out.println("HİÇBİR GÖRÜNTÜ OKUNAMADI!");
            System.out.println("Lütfen src/jpegdeadatabits/images/ klasörüne test görüntüleri koyun:");
            for (String f : IMAGE_FILES) {
                System.out.println("  - " + f);
            }
        }
    }

    /**
     * Tek bir görüntü-kalite kombinasyonu için Huffman ve DEA bit sayılarını
     * hesaplar.
     */
    private static TestResult runTest(BufferedImage img, String name, int quality) {
        // 1. RGB → YCbCr
        double[][][] ycc = ColorSpaceConverter.rgbToYCbCr(img);
        double[][] Y  = ycc[0];
        double[][] Cb = ycc[1];
        double[][] Cr = ycc[2];

        // 2. 4:2:0 chroma subsampling
        double[][] CbDown = ChromaSubsampler.downsample420(Cb);
        double[][] CrDown = ChromaSubsampler.downsample420(Cr);

        // 3. JPEG kuantizasyon matrislerini ölçekle
        int[][] qY = Quantization.scaledMatrix(Quantization.Q_LUMA,   quality);
        int[][] qC = Quantization.scaledMatrix(Quantization.Q_CHROMA, quality);

        // 4. Her kanal için JPEG akışı (DCT → Quant → Zigzag → RLE)
        ChannelProcessor.Result resY  = ChannelProcessor.process(Y,      qY);
        ChannelProcessor.Result resCb = ChannelProcessor.process(CbDown, qC);
        ChannelProcessor.Result resCr = ChannelProcessor.process(CrDown, qC);

        // 5. DC katsayıları için DPCM uygula ve tüm kanalları birleştir
        List<Integer> allDc = new ArrayList<>();
        allDc.addAll(RLE.dcDpcm(resY.dcList()));
        allDc.addAll(RLE.dcDpcm(resCb.dcList()));
        allDc.addAll(RLE.dcDpcm(resCr.dcList()));

        // 6. AC pair'lerini birleştir
        List<Pair> allAc = new ArrayList<>();
        allAc.addAll(resY.acList());
        allAc.addAll(resCb.acList());
        allAc.addAll(resCr.acList());

        // 7. İki kodlayıcıyı çalıştır (yalnızca veri bitleri)
        long huffmanDcBits = HuffmanEncoder.encodeDataBits(allDc);
        long deaDcBits     = DEAEncoder.encodeDataBits(allDc);
        long huffmanAcBits = HuffmanEncoder.encodeDataBits(allAc);
        long deaAcBits     = DEAEncoder.encodeDataBits(allAc);

        // 8. RECONSTRUCTION — geri-çatım (kalite metrikleri için)
        //    Kuantize bloklardan: dequant → IDCT → +128 ile her kanalı geri kur.
        int H = img.getHeight(), W = img.getWidth();
        double[][] recY = ChannelProcessor.reconstruct(
                resY.quantBlocks(), qY, resY.padH(), resY.padW(), H, W);

        // Chroma downsample edilmiş boyutta geri-çatılır, sonra upsample edilir
        int chH = CbDown.length, chW = CbDown[0].length;
        double[][] recCbSmall = ChannelProcessor.reconstruct(
                resCb.quantBlocks(), qC, resCb.padH(), resCb.padW(), chH, chW);
        double[][] recCrSmall = ChannelProcessor.reconstruct(
                resCr.quantBlocks(), qC, resCr.padH(), resCr.padW(), chH, chW);
        double[][] recCb = ChromaSubsampler.upsample420(recCbSmall, H, W);
        double[][] recCr = ChromaSubsampler.upsample420(recCrSmall, H, W);

        // 9. YCbCr → RGB ile geri-çatılmış görüntü
        BufferedImage recon = ColorSpaceConverter.yCbCrToRgb(recY, recCb, recCr);

        // 10. Kalite metrikleri (orijinal ile geri-çatılmış arasında)
        //     NOT: Huffman=DEA olduğu için tek set (entropi kodlama kayıpsız).
        double mse  = Metrics.mse(img, recon);
        double psnr = Metrics.psnr(mse);
        double ssim = Metrics.ssim(img, recon);

        return new TestResult(name, W, H, quality,
                huffmanDcBits, deaDcBits, huffmanAcBits, deaAcBits,
                mse, psnr, ssim);
    }
}
