================================================================
  JPEG-DEA (DEA-J) — VERSİYON 1: Huffman vs DEA-J
================================================================

Bu versiyon, SCI makalesinin 1. AŞAMASINA karşılık gelir:
Standart JPEG (Huffman) ile önerilen JPEG-DEA (DEA-J, koşullu
entropi kodlama) karşılaştırması.

KARŞILAŞTIRILAN YÖNTEMLER:
  JPEG      → Huffman  (baseline standart entropi kodlayıcı)
  JPEG-DEA  → DEA-J    (koşullu / bağlam tabanlı entropi kodlayıcı)

JPEG akışı (RGB→YCbCr→4:2:0→DCT→Kuantizasyon→Zigzag→DPCM→RLE)
her iki yöntemde de AYNIDIR; yalnızca son adım olan entropi
kodlama farklıdır.

ÖLÇÜLEN:
  - Veri bitleri (model/ağaç saklama maliyeti HARİÇ)
  - Genel kazanç + DC kazanç + AC kazanç (ayrı ayrı)
  - Sıkıştırma oranı (CR), piksel başına bit (BPP)
  - Kalite metrikleri: PSNR, MSE, SSIM (iki yöntemde de aynı,
    çünkü entropi kodlama kayıpsızdır)
  - Kodlama süresi (ms)
  - Sonda CSV çıktısı (Excel/analiz için)

----------------------------------------------------------------
DERLEME VE ÇALIŞTIRMA (JDK 17+ gerekir):

  1) Test görüntülerini şuraya koyun:
       src/jpegdeadatabits/images/    (.png .jpg .jpeg .bmp)

  2) Derleyin:
       javac -d out $(find src -name "*.java")

  3) Çalıştırın (UTF-8 terminal önerilir):
       java -Dfile.encoding=UTF-8 -cp out jpegdeadatabits.Main

----------------------------------------------------------------
KLASÖR YAPISI:
  src/jpegdeadatabits/
    Main.java                       → ana test programı
    entropy/
      HuffmanEncoder.java           → standart JPEG Huffman
      DEAEncoder.java               → DEA-J (koşullu) kodlayıcı
      HuffmanNode.java, Pair.java   → yardımcı yapılar
    preprocessing/                  → renk dönüşümü, chroma, okuma
    transform/                      → DCT, kuantizasyon, zigzag, RLE
    report/                         → TestResult, ResultPrinter, Metrics
    images/                         → test görüntüleri (buraya koyun)
================================================================
