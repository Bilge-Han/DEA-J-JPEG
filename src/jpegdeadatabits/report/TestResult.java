package jpegdeadatabits.report;

/**
 * Tek bir görüntü-kalite test sonucunu temsil eder.
 *
 * BİT ALANLARI:
 *   huffmanDcBits / deaDcBits : DC katsayıları için bit sayıları
 *   huffmanAcBits / deaAcBits : AC katsayıları için bit sayıları
 *
 * KALİTE METRİKLERİ (mse, psnr, ssim):
 *   Yalnızca kuantizasyona (Q faktörüne) bağlıdır. Entropi kodlayıcı kayıpsız
 *   olduğu için Huffman ve DEA aynı değerleri verir — bu yüzden tek bir set
 *   tutulur. Orijinal görüntü ile geri-çatılmış görüntü arasında hesaplanır.
 *
 * SIKIŞTIRMA METRİKLERİ (CR, BPP):
 *   Bit sayısına bağlı olduğu için Huffman ve DEA için AYRI hesaplanır.
 */
public record TestResult(
        String imageName,
        int width,
        int height,
        int quality,
        long huffmanDcBits,
        long deaDcBits,
        long huffmanAcBits,
        long deaAcBits,
        double mse,
        double psnr,
        double ssim
) {

    public long huffmanTotalBits() {
        return huffmanDcBits + huffmanAcBits;
    }

    public long deaTotalBits() {
        return deaDcBits + deaAcBits;
    }

    // ==================== KAZANÇ (%) ====================

    public double overallGain() {
        long h = huffmanTotalBits();
        if (h == 0) return 0;
        return (h - deaTotalBits()) * 100.0 / h;
    }

    public double dcGain() {
        if (huffmanDcBits == 0) return 0;
        return (huffmanDcBits - deaDcBits) * 100.0 / huffmanDcBits;
    }

    public double acGain() {
        if (huffmanAcBits == 0) return 0;
        return (huffmanAcBits - deaAcBits) * 100.0 / huffmanAcBits;
    }

    // ==================== SIKIŞTIRMA METRİKLERİ ====================

    public double huffmanCR() {
        return Metrics.compressionRatio(width, height, huffmanTotalBits());
    }

    public double deaCR() {
        return Metrics.compressionRatio(width, height, deaTotalBits());
    }

    public double huffmanBPP() {
        return Metrics.bpp(width, height, huffmanTotalBits());
    }

    public double deaBPP() {
        return Metrics.bpp(width, height, deaTotalBits());
    }
}
