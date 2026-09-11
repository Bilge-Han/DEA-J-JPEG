package jpegdeadatabits;

import jpegdeadatabits.entropy.*;
import jpegdeadatabits.preprocessing.ChromaSubsampler;
import jpegdeadatabits.preprocessing.ColorSpaceConverter;
import jpegdeadatabits.preprocessing.ImageReader;
import jpegdeadatabits.transform.ChannelProcessor;
import jpegdeadatabits.transform.Quantization;
import jpegdeadatabits.transform.RLE;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ABLATION ANA PROGRAM (v4)
 *
 * Danışmanın ablation tablosunun BEŞ satırını da üretir:
 *
 *   jpeg_default_bits -> "JPEG-default"   : standart gösterim + Ek K sabit tabloları
 *   jpeg_opt_bits     -> "JPEG-optimized" : standart gösterim + optimize tablolar
 *   uncond_bits       -> "DEA-J-global"   : (koşu,değer) gösterimi, bağlam yok
 *   deaj_data_bits    -> "DEA-J"          : (koşu,değer) + önceki sembol bağlamı
 *   deaj_model_bits   -> "DEA-J-net" için model yükü
 *
 * JPEG-default ve JPEG-optimized, standart JPEG uygulamasına uygun biçimde
 * KANAL BAZLI hesaplanır: luma (Y) için luma tabloları, krominans (Cb+Cr)
 * için ortak krominans tabloları. Böylece iki satır birebir karşılaştırılabilir.
 *
 * Ayrıca kodlama / kod çözme süreleri (ms) ölçülür.
 */
public class AblationMain {
    private static final int[] QUALITIES = {25, 50, 75, 90};
    private static final String IMAGES_DIR = "src/jpegdeadatabits/images";

    public static void main(String[] args) {
        File dir = new File(IMAGES_DIR);
        File[] found = dir.listFiles((d, n) -> {
            String ln = n.toLowerCase();
            return ln.endsWith(".png") || ln.endsWith(".jpg") || ln.endsWith(".jpeg")
                || ln.endsWith(".bmp") || ln.endsWith(".tif") || ln.endsWith(".tiff");
        });
        if (found == null || found.length == 0) {
            System.err.println("GÖRÜNTÜ YOK: " + IMAGES_DIR);
            return;
        }
        java.util.Arrays.sort(found, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        System.err.println("JIT isinma turu calisiyor...");
        TimingBenchmark.warmup();
        System.err.println("Isinma tamam, olcum basliyor.");

        System.out.println("CSV_START");
        System.out.println("img,w,h,q,jpeg_default_bits,jpeg_opt_bits,uncond_bits,deaj_data_bits,"
            + "deaj_model_bits,uncond_model_bits,n_contexts,uncond_enc_ms,uncond_dec_ms,deaj_enc_ms,deaj_dec_ms");

        for (File file : found) {
            try {
                BufferedImage img = ImageReader.read(file.getPath());
                String name = file.getName().replaceAll("(?i)\\.(png|jpg|jpeg|bmp|tif|tiff)$", "");
                int W = img.getWidth(), H = img.getHeight();

                for (int q : QUALITIES) {
                    double[][][] ycc = ColorSpaceConverter.rgbToYCbCr(img);
                    double[][] Cb = ChromaSubsampler.downsample420(ycc[1]);
                    double[][] Cr = ChromaSubsampler.downsample420(ycc[2]);
                    int[][] qY = Quantization.scaledMatrix(Quantization.Q_LUMA, q);
                    int[][] qC = Quantization.scaledMatrix(Quantization.Q_CHROMA, q);
                    ChannelProcessor.Result rY  = ChannelProcessor.process(ycc[0], qY);
                    ChannelProcessor.Result rCb = ChannelProcessor.process(Cb, qC);
                    ChannelProcessor.Result rCr = ChannelProcessor.process(Cr, qC);

                    // --- kanal bazlı listeler (standart JPEG uygulaması) ---
                    List<Integer> dcY = RLE.dcDpcm(rY.dcList());
                    List<Integer> dcC = new ArrayList<>();
                    dcC.addAll(RLE.dcDpcm(rCb.dcList()));
                    dcC.addAll(RLE.dcDpcm(rCr.dcList()));
                    List<Pair> acY = new ArrayList<>(rY.acList());
                    List<Pair> acC = new ArrayList<>();
                    acC.addAll(rCb.acList());
                    acC.addAll(rCr.acList());

                    // --- birleşik listeler (DEA-J akışı) ---
                    List<Integer> allDc = new ArrayList<>(dcY); allDc.addAll(dcC);
                    List<Pair> allAc = new ArrayList<>(acY);   allAc.addAll(acC);

                    // (A) JPEG-default : Ek K sabit tabloları, kanal bazlı
                    long jpegDefault = AnnexKEncoder.dcBits(dcY, true)
                                     + AnnexKEncoder.acBits(acY, true)
                                     + AnnexKEncoder.dcBits(dcC, false)
                                     + AnnexKEncoder.acBits(acC, false);

                    // (B) JPEG-optimized : optimize tablolar, kanal bazlı
                    long jpegOpt = StandardJpegEncoder.dcBits(dcY)
                                 + StandardJpegEncoder.acBits(acY)
                                 + StandardJpegEncoder.dcBits(dcC)
                                 + StandardJpegEncoder.acBits(acC);

                    // (C) DEA-J-global : (koşu,değer) gösterimi, bağlamsız
                    long uncond = AblationEncoder.unconditionalHuffmanBits(allDc)
                                + AblationEncoder.unconditionalHuffmanBits(allAc);

                    // (D) DEA-J : bağlamlı
                    long deajData = DEAEncoder.encodeDataBits(allDc)
                                  + DEAEncoder.encodeDataBits(allAc);

                    long deajModel = AblationEncoder.conditionalModelBits(allDc)
                                   + AblationEncoder.conditionalModelBits(allAc);
                    long uncondModel = AblationEncoder.unconditionalModelBits(allDc)
                                     + AblationEncoder.unconditionalModelBits(allAc);
                    int nCtx = AblationEncoder.contextCount(allDc)
                             + AblationEncoder.contextCount(allAc);

                    // (E) süreler
                    TimingBenchmark.Timing uDc = TimingBenchmark.timeUnconditional(allDc);
                    TimingBenchmark.Timing uAc = TimingBenchmark.timeUnconditional(allAc);
                    TimingBenchmark.Timing dDc = TimingBenchmark.timeConditional(allDc);
                    TimingBenchmark.Timing dAc = TimingBenchmark.timeConditional(allAc);
                    double uEnc = (uDc.encodeNs() + uAc.encodeNs()) / 1e6;
                    double uDec = (uDc.decodeNs() + uAc.decodeNs()) / 1e6;
                    double dEnc = (dDc.encodeNs() + dAc.encodeNs()) / 1e6;
                    double dDec = (dDc.decodeNs() + dAc.decodeNs()) / 1e6;

                    System.out.printf(java.util.Locale.US,
                        "%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.3f,%.3f,%.3f,%.3f%n",
                        name, W, H, q, jpegDefault, jpegOpt, uncond, deajData,
                        deajModel, uncondModel, nCtx, uEnc, uDec, dEnc, dDec);
                }
                System.err.println("[OK] " + name);
            } catch (Exception e) {
                System.err.println("[HATA] " + file.getName() + ": " + e.getMessage());
            }
        }
        System.out.println("CSV_END");
    }
}
