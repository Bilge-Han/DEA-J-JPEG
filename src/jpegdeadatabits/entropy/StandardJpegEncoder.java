package jpegdeadatabits.entropy;
import java.util.*;

/**
 * Standart JPEG sembol gösterimi + GÖRÜNTÜYE ÖZEL OPTİMİZE Huffman tabloları.
 *   DC: DPCM farkı -> kategori + ek bitler
 *   AC: (koşu,değer) -> (RRRR|SSSS) sembolü + ek bitler
 */
public final class StandardJpegEncoder {
    private static int category(int v){ int a=Math.abs(v),c=0; while(a>0){c++;a>>=1;} return c; }

    public static long dcBits(List<Integer> d){
        if(d.isEmpty()) return 0;
        Map<Integer,Integer> f=new HashMap<>(); long extra=0;
        for(int v:d){ int c=category(v); f.merge(c,1,Integer::sum); extra+=c; }
        Map<Integer,String> codes=HuffmanNode.buildCodes(f);
        long h=0; for(int v:d){ String c=codes.get(category(v)); h+=(c!=null)?c.length():8; }
        return h+extra;
    }
    public static long acBits(List<Pair> pairs){
        if(pairs.isEmpty()) return 0;
        List<Integer> syms=new ArrayList<>(); long extra=0;
        for(Pair p:pairs){
            int run=p.run(), val=p.value();
            if(run==0&&val==0){ syms.add(0x00); continue; }
            while(run>15){ syms.add(0xF0); run-=16; }
            int c=category(val); syms.add((run<<4)|c); extra+=c;
        }
        Map<Integer,Integer> f=new HashMap<>();
        for(int s:syms) f.merge(s,1,Integer::sum);
        Map<Integer,String> codes=HuffmanNode.buildCodes(f);
        long h=0; for(int s:syms){ String c=codes.get(s); h+=(c!=null)?c.length():8; }
        return h+extra;
    }
    private StandardJpegEncoder(){}
}
