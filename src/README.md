# JPEG-DEA Data Bits Comparison

JPEG sıkıştırma akışında standart Huffman entropi kodlayıcısı yerine
**Dynamic Bit-Level Encoding Algorithm (DEA)** kullanıldığında elde edilen
veri biti kazancını ölçmek için tasarlanmış Java projesi.

## Amaç

Erdal & Önal (2025) tarafından önerilen DEA algoritmasının JPEG'in son entropi
kodlama adımında Huffman yerine kullanılabilirliğini incelemek. Bu projede
yalnızca **veri bitleri** karşılaştırılır; Huffman ağaçlarının saklama maliyeti
(yan bilgi) hesaplamaya dahil edilmemiştir.

Bu yaklaşımın gerekçesi: DEA'nın saf istatistiksel model gücünü Huffman'a karşı
doğrudan ölçmek. Yan bilgi maliyetinin azaltılması (canonical Huffman, frekans
grubu birleştirme vb.) ileride ayrı bir araştırma konusu olarak ele alınabilir.

## Ölçülen Metrikler

Program her görüntü ve her kalite (Q) seviyesi için üç grup metrik üretir:

**1. Bit sayıları ve DEA kazancı**
- Huffman ve DEA veri bitleri (DC + AC ayrı)
- DC / AC / TOPLAM kazanç yüzdesi

**2. Sıkıştırma metrikleri (Huffman vs DEA — AYRI)**
- CR (Compression Ratio) = orijinal_bit / sıkıştırılmış_bit
- BPP (Bits Per Pixel) = sıkıştırılmış_bit / piksel_sayısı
- Bit sayıları farklı olduğu için DEA ve Huffman farklı CR/BPP verir.

**3. Kalite metrikleri (PSNR / MSE / SSIM — ORTAK)**
- Orijinal görüntü ile geri-çatılmış (reconstruct) görüntü arasında hesaplanır.
- **Önemli:** Bu metrikler yalnızca kuantizasyona (Q) bağlıdır. Entropi kodlama
  (Huffman/DEA) kayıpsız olduğundan ikisi de aynı kuantize katsayıları kodlar;
  bu nedenle PSNR/MSE/SSIM **Huffman ve DEA için aynıdır**. Fark yalnızca bit
  sayısında (CR/BPP) ortaya çıkar.
- **Temel bulgu:** DEA, aynı görsel kaliteyi (aynı PSNR/SSIM) daha az bitle sağlar.

Reconstruction akışı: kuantize bloklar → ters kuantizasyon → IDCT → +128 →
chroma upsample → YCbCr→RGB → orijinal ile karşılaştırma.

## JPEG Akışı

```
RGB → YCbCr (BT.601) → 4:2:0 chroma subsampling → 8×8 blok →
    DCT → Kuantizasyon (JPEG matrisleri + QF scaling) →
        Zigzag → RLE → [Standart Huffman | DEA]
```

JPEG'in tam akışı korunmuş, yalnızca son entropi kodlama adımında iki yöntem
karşılaştırılır.

## Proje Yapısı

```
jpegdea-databits/
└── src/jpegdeadatabits/
    ├── Main.java                          ← Entry point + batch test runner
    ├── images/                            ← Test görüntüleri (PNG/JPG)
    ├── preprocessing/
    │   ├── ImageReader.java               ← Görüntü dosyası okuma
    │   ├── ColorSpaceConverter.java       ← RGB ↔ YCbCr (BT.601)
    │   └── ChromaSubsampler.java          ← 4:2:0 subsample
    ├── transform/
    │   ├── DCT8x8.java                    ← Forward 8×8 DCT
    │   ├── Quantization.java              ← JPEG Q matrisleri + QF scaling
    │   ├── ZigzagScanner.java             ← 8×8 ↔ 64-elemanlı tarama
    │   ├── RLE.java                       ← (run, value) + DC DPCM
    │   └── ChannelProcessor.java          ← Tek kanal tam JPEG akışı
    ├── entropy/
    │   ├── Pair.java                      ← (run, value) record
    │   ├── HuffmanNode.java               ← Huffman ağacı (generic)
    │   ├── HuffmanEncoder.java            ← Standart Huffman (referans)
    │   └── DEAEncoder.java                ← DEA (conditional Huffman)
    └── report/
        ├── TestResult.java                ← Test sonucu record
        └── ResultPrinter.java             ← Konsol çıktısı formatı
```

## Kullanım

### NetBeans ile

1. NetBeans'te yeni "Java with Ant > Java Application" projesi oluştur, adı `jpegdea-databits`.
2. Proje açıldıktan sonra `src/` klasörünün içeriğini bu projenin `src/` ile değiştir.
3. Test görüntülerini `src/jpegdeadatabits/images/` klasörüne koy:
   - `baboon.png`, `lena.png`, `peppers.png`, `natural.png`
4. `Main.java` üzerinde sağ-tık → **Run File**

### Komut Satırı ile

```bash
cd jpegdea-databits/src
javac jpegdeadatabits/Main.java
java jpegdeadatabits.Main
```

## Beklenen Çıktı

Her görüntü için her kalite seviyesinde detaylı tablo + en sonda genel özet:

```
GÖRÜNTÜ: baboon  (512 × 512)
  Kalite   Huffman (bit)       DEA (bit)     DC Kazancı     AC Kazancı  TOPLAM Kazanç
  Q=25           235,907         223,118         5.75%         5.38%         5.42%
  Q=50           389,272         362,083         9.70%         6.75%         6.98%
  Q=75           600,690         548,936        17.61%         8.03%         8.62%
  Q=90         1,029,113         911,025        35.31%        10.37%        11.47%

GENEL ÖZET — DEA'nın Huffman'a Göre Sıkıştırma Kazancı (%)
  Görüntü               Q=25       Q=50       Q=75       Q=90
  baboon               5.42%      6.98%      8.62%     11.47%
  lena                 8.39%     10.47%     11.76%     14.26%
  peppers              8.98%     10.83%     12.19%     14.87%
  natural             14.32%     18.11%     18.52%     21.17%
  ORTALAMA             9.28%     11.60%     12.77%     15.44%

→ DEA, tüm testlerde ortalama %12.27 daha fazla sıkıştırma sağlamıştır.
```

Pozitif değerler DEA'nın Huffman'dan o oranda daha fazla sıkıştırdığını gösterir.

## Test Sonuçları (16/16 testte DEA daha fazla sıkıştırdı)

| Görüntü | Q=25 | Q=50 | Q=75 | Q=90 |
|---------|------|------|------|------|
| Baboon  | %5.42 | %6.98 | %8.62 | %11.47 |
| Lena    | %8.39 | %10.47 | %11.76 | %14.26 |
| Peppers | %8.98 | %10.83 | %12.19 | %14.87 |
| Natural | %14.32 | %18.11 | %18.52 | %21.17 |
| **Ortalama** | **%9.28** | **%11.60** | **%12.77** | **%15.44** |

**Genel ortalama: DEA, Huffman'dan %12.27 daha fazla sıkıştırma sağlamıştır.**

## Gereksinimler

- Java 17+ (record desteği için)
- Standart `javax.imageio` paketi (JDK ile gelir, ek bağımlılık yok)

## Referanslar

- Erdal, E., & Önal, Y. (2025). *Enhanced framework for lossless image
  compression using image segmentation and a novel dynamic bit-level encoding
  algorithm*. Applied Sciences, 15(6), 2964.
- Wallace, G. K. (1992). *The JPEG still picture compression standard*. IEEE
  Transactions on Consumer Electronics, 38(1), xviii-xxxiv.
- ITU-R Recommendation BT.601 — Studio encoding parameters of digital television.

---
**Bilgehan ACAR** — Haziran 2026
*Kırıkkale Üniversitesi, Bilgisayar Mühendisliği Yüksek Lisans*
*Danışman: Dr. Erdal ERDAL*

## GÜNCELLEME: Genişletilmiş Metrik Seti (CR, BPP, MSE, PSNR, SSIM)

Artık her görüntü-kalite kombinasyonu için üç tablo üretilir:

1. **Bit sayıları + DEA kazancı** (DC/AC/toplam) — Huffman vs DEA
2. **Sıkıştırma metrikleri: CR ve BPP** — Huffman ve DEA için AYRI (bit sayısına bağlı)
3. **Kalite metrikleri: PSNR, MSE, SSIM** — Huffman ve DEA için ORTAK

### Neden kalite metrikleri ortak?
Entropi kodlama (Huffman/DEA) **kayıpsızdır**; ikisi de aynı kuantize DCT
katsayılarını kodlar. Bu yüzden geri-çatılmış görüntü ve dolayısıyla PSNR/MSE/SSIM
her ikisinde de aynıdır. Kalite yalnızca kuantizasyona (Q faktörü) bağlıdır.

**Çalışmanın temel bulgusu:** DEA, aynı görsel kaliteyi (aynı PSNR/SSIM) Huffman'dan
daha az bitle sağlar → daha yüksek CR, daha düşük BPP.

### Otomatik görsel tarama
`images/` klasörüne konulan tüm `.png/.jpg/.jpeg/.bmp` dosyaları otomatik
taranır. Sabit dosya listesi yoktur; istediğiniz kadar görsel ekleyebilirsiniz.

### Reconstruction akışı (kalite metrikleri için)
```
Kuantize bloklar → dequantize → IDCT → +128 → (chroma upsample) → YCbCr→RGB
→ orijinal ile karşılaştır → MSE/PSNR/SSIM
```

### Yeni/güncellenen sınıflar
- `report/Metrics.java` — MSE, PSNR, SSIM (Wang 2004, 8×8), CR, BPP
- `transform/ChannelProcessor.reconstruct()` — dequant→IDCT geri-çatım
- `report/TestResult.java` — CR/BPP/PSNR/MSE/SSIM alanları
- `report/ResultPrinter.java` — 3 tablolu çıktı

## TEST GÖRÜNTÜLERİ — Kullanım

### Desteklenen formatlar
`.png .jpg .jpeg .bmp .tif .tiff` — hepsi otomatik taranır. Java 21 ImageIO
TIFF'i yerleşik okur, ek kütüphane gerekmez.

### Gri ve renkli görüntüler
- **Renkli** görüntüler doğrudan RGB→YCbCr→4:2:0 akışıyla işlenir.
- **Gri (grayscale)** görüntüler otomatik olarak 3 kanallı RGB'ye çevrilir
  (R=G=B). Renk kanalları nötr (Cb=Cr=128) olur; luma kanalı gerçek gri veridir.
  Bu, gri görüntülerde de akışın sorunsuz çalışmasını sağlar.

### Farklı boyutlar
256×256, 512×512 veya herhangi bir boyut çalışır (8'in katı değilse kenar
tekrarı ile padding uygulanır). Kod boyutu dinamik okur.

### imageprocessingplace.com "Standard" test set
Lena, peppers, cameraman, mandril, lake, jetplane, house vb. görüntüler
(gri + renkli, 256 + 512, TIFF) doğrudan `images/` klasörüne atılıp
çalıştırılabilir. SCI makalesi için literatürde en sık atıf verilen settir.
