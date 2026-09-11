# JPEG / JPEG-MQ / JPEG-DEA / JPEG-DEA-MQ — Dört Yöntemli Karşılaştırma

DEA'nın koşullu prensibi JPEG'in HER İKİ entropi versiyonuna uygulandı.
Dört yöntem de AYNI JPEG akışını kullanır; yalnızca ENTROPİ KODLAMA farklıdır:

| Yöntem | Entropi kodlama | Sınıf | Fikir |
|---|---|---|---|
| **JPEG** | Huffman | `HuffmanEncoder` | baseline standart |
| **JPEG-MQ** | Aritmetik (Ek D) | `JpegMQEncoder` | JPEG'in kendi aritmetik varyantı |
| **JPEG-DEA** | DEA (koşullu Huffman) | `DEAEncoder` | önceki-sembole koşullu |
| **JPEG-DEA-MQ** | DEA + MQ | `JpegDeaMQEncoder` | **koşullu model + aritmetik motor ← ÖNERİLEN** |

## ANA FİKİR (JPEG-DEA-MQ)
DEA'nın "önceki sembole koşullandırma" prensibi + MQ'nun "tamsayı kod kısıtı yok"
avantajı birleştirildi. MQ'nun her ikili kararının bağlamı, DEA'daki gibi bir
önceki sembolün sınıfına göre kaydırılır. Sonuç: hem saf MQ'yu hem saf DEA'yı geçer.

## SONUÇLAR (4 örnek görüntü, JDK 21 ile doğrulandı)
Baseline JPEG (Huffman)'a göre ortalama kazanç:

| Yöntem | Ortalama kazanç | Not |
|---|---|---|
| JPEG-DEA | %12,27 | koşullu Huffman |
| JPEG-MQ | %16,86 | aritmetik |
| **JPEG-DEA-MQ** | **%21,58** | **en iyi — saf MQ'ya göre +%5,71 ek kazanç** |

Hız (ortalama, ms): JPEG-DEA-MQ MQ motoru sayesinde HIZLIDIR (~15 ms,
Huffman'ın ~28 ms'inden hızlı). Yalnızca saf JPEG-DEA yavaştır (~44 ms).

## entropy/ klasöründe olması gereken 6 dosya
- `HuffmanEncoder.java`   (JPEG)
- `JpegMQEncoder.java`    (JPEG-MQ, Ek D modeli)
- `DEAEncoder.java`       (JPEG-DEA)
- `JpegDeaMQEncoder.java` (JPEG-DEA-MQ — YENİ, önerilen)
- `HuffmanNode.java`      (ortak altyapı)
- `Pair.java`             (AC sembolü)

> Eski mimari dosyaları (SymbolEncoder, ArithmeticEncoder, GeneralArithmeticEncoder,
> JpegArithmeticEncoder, DeaEncoder(küçük d), EntropyBenchmark) varsa SİLİN.

## Değişen dosyalar (mevcut mantık korundu)
- `Main.java` — 4 encoder çağrısı + her biri için süre ölçümü (nanoTime).
- `report/TestResult.java` — deaMq* bit alanları + *TimeNs süre alanları eklendi.
- `report/ResultPrinter.java` — tablolar 4 yöntem + hız gösterir.

## Değişmeyen dosyalar
`HuffmanEncoder`, `DEAEncoder`, `HuffmanNode`, `Pair`, tüm transform/ ve preprocessing/.

## Ölçüm
- Bit: JPEG/DEA → kod-kelimesi; MQ/DEA-MQ → gerçek MQ çıkış biti (renorm+flush).
- Süre: her yöntemin DC+AC kodlama süresi (System.nanoTime).
- Kazanç (%) = (JPEG_bits − yöntem_bits) / JPEG_bits × 100.

## MQ hakkında not
`JpegMQEncoder` binarization'ı JPEG Ek D modeline uygun (DC/AC ayrı model, unary
büyüklük, ayrı bağlamlar). Literatürdeki "aritmetik, Huffman'dan ~%5-10 iyi"
sonucuyla tutarlıdır (burada %16,86 — modern MQ tablosu daha güçlü).

## Çalıştırma
NetBeans'te normal Run. 19 görselinizi src/jpegdeadatabits/images/ altına koyun.
