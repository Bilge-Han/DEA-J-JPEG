package jpegdeadatabits.entropy;
import java.util.*;

/** Ablation ölçümleri: bağlamsız Huffman, bağlam sayısı, model boyutu. */
public final class AblationEncoder {
    public static <T> long unconditionalHuffmanBits(List<T> symbols) {
        if (symbols.isEmpty()) return 0;
        Map<T,Integer> f=new HashMap<>();
        for (T s: symbols) f.merge(s,1,Integer::sum);
        Map<T,String> c=HuffmanNode.buildCodes(f);
        long b=0;
        for (T s: symbols){ String x=c.get(s); b += (x!=null)?x.length():8; }
        return b;
    }
    public static <T> long unconditionalModelBits(List<T> symbols){
        if(symbols.isEmpty()) return 0;
        return canonicalHeaderBits(new HashSet<>(symbols).size());
    }
    public static <T> long conditionalModelBits(List<T> symbols){
        if(symbols.size()<2) return 0;
        Map<T,Set<T>> a=new HashMap<>();
        for(int i=1;i<symbols.size();i++) a.computeIfAbsent(symbols.get(i-1),k->new HashSet<>()).add(symbols.get(i));
        long t=0; for(Set<T> s:a.values()) t+=canonicalHeaderBits(s.size());
        return t;
    }
    public static <T> int contextCount(List<T> symbols){
        if(symbols.size()<2) return 0;
        Set<T> c=new HashSet<>();
        for(int i=1;i<symbols.size();i++) c.add(symbols.get(i-1));
        return c.size();
    }
    private static long canonicalHeaderBits(int k){ return k<=1?0:16L*8+(long)k*8; }
    private AblationEncoder(){}
}
