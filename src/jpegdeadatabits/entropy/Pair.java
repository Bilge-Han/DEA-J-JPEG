package jpegdeadatabits.entropy;

/**
 * JPEG'in (run, value) sembol çifti.
 *
 * AC katsayıları RLE çıktısında bu yapıda temsil edilir:
 *   run   : bu katsayıdan önce kaç ardışık sıfır var (0-15)
 *   value : sıfır olmayan katsayı değeri
 *
 * Özel değerler:
 *   (15, 0) → ZRL: 16 ardışık sıfır
 *   (0,  0) → EOB: bloğun sonu, kalan tüm katsayılar sıfır
 *
 * Java record olarak tanımlanmıştır → equals/hashCode otomatik gelir,
 * bu HashMap anahtarı olarak doğrudan kullanılabilir hale getirir.
 */
public record Pair(int run, int value) {}
