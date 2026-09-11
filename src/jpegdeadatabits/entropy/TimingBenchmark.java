package jpegdeadatabits.entropy;
import java.util.*;

/** Kodlama ve kod çözme sürelerini ölçer (ns). */
public final class TimingBenchmark {
    private static final class Trie {
        int[] left,right,sym; int size=1;
        Trie(int cap){ left=new int[cap];right=new int[cap];sym=new int[cap];
            Arrays.fill(left,-1);Arrays.fill(right,-1);Arrays.fill(sym,-1); }
        void insert(String code,int si){ int n=0;
            for(int i=0;i<code.length();i++){ boolean one=code.charAt(i)=='1';
                int nx=one?right[n]:left[n];
                if(nx==-1){ nx=size++; if(one) right[n]=nx; else left[n]=nx; }
                n=nx; }
            sym[n]=si; }
    }
    private static <T> Trie buildTrie(Map<T,String> codes, Map<T,Integer> si){
        int cap=4; for(String c:codes.values()) cap+=c.length()+1;
        Trie t=new Trie(cap);
        for(Map.Entry<T,String> e:codes.entrySet()) t.insert(e.getValue(), si.get(e.getKey()));
        return t;
    }
    public record Timing(long encodeNs, long decodeNs) {}

    public static <T> Timing timeUnconditional(List<T> symbols){
        if(symbols.size()<2) return new Timing(0,0);
        long t0=System.nanoTime();
        Map<T,Integer> f=new HashMap<>();
        for(T s:symbols) f.merge(s,1,Integer::sum);
        Map<T,String> codes=HuffmanNode.buildCodes(f);
        BitSet bits=new BitSet(); int bi=0;
        for(T s:symbols){ String c=codes.get(s);
            for(int i=0;i<c.length();i++){ if(c.charAt(i)=='1') bits.set(bi); bi++; } }
        long enc=System.nanoTime()-t0;
        Map<T,Integer> si=new HashMap<>(); List<T> idx=new ArrayList<>();
        for(T s:codes.keySet()){ si.put(s,idx.size()); idx.add(s); }
        Trie tr=buildTrie(codes,si); final int total=bi;
        long t1=System.nanoTime(); int n=0,prod=0;
        for(int i=0;i<total;i++){ n=bits.get(i)?tr.right[n]:tr.left[n];
            if(n==-1) break; if(tr.sym[n]!=-1){ prod++; n=0; } }
        long dec=System.nanoTime()-t1;
        if(prod<0) System.err.print("");
        return new Timing(enc,dec);
    }

    public static <T> Timing timeConditional(List<T> symbols){
        if(symbols.size()<2) return new Timing(0,0);
        long t0=System.nanoTime();
        Map<T,Map<T,Integer>> cond=new HashMap<>();
        for(int i=1;i<symbols.size();i++)
            cond.computeIfAbsent(symbols.get(i-1),k->new HashMap<>()).merge(symbols.get(i),1,Integer::sum);
        Map<T,Map<T,String>> cm=new HashMap<>();
        for(Map.Entry<T,Map<T,Integer>> e:cond.entrySet()) cm.put(e.getKey(),HuffmanNode.buildCodes(e.getValue()));
        BitSet bits=new BitSet(); int bi=0;
        for(int i=1;i<symbols.size();i++){
            String c=cm.get(symbols.get(i-1)).get(symbols.get(i));
            if(c==null) continue;
            for(int k=0;k<c.length();k++){ if(c.charAt(k)=='1') bits.set(bi); bi++; } }
        long enc=System.nanoTime()-t0;
        Map<T,Trie> tries=new HashMap<>(); Map<T,List<T>> cs=new HashMap<>();
        for(Map.Entry<T,Map<T,String>> e:cm.entrySet()){
            Map<T,Integer> si=new HashMap<>(); List<T> l=new ArrayList<>();
            for(T s:e.getValue().keySet()){ si.put(s,l.size()); l.add(s); }
            tries.put(e.getKey(),buildTrie(e.getValue(),si)); cs.put(e.getKey(),l); }
        final int total=bi;
        long t1=System.nanoTime();
        T prev=symbols.get(0); Trie tr=tries.get(prev); int n=0,prod=0;
        for(int i=0;i<total&&tr!=null;i++){
            n=bits.get(i)?tr.right[n]:tr.left[n];
            if(n==-1) break;
            if(tr.sym[n]!=-1){ T dec2=cs.get(prev).get(tr.sym[n]); prod++;
                prev=dec2; tr=tries.get(prev); n=0; if(tr==null) break; } }
        long dec=System.nanoTime()-t1;
        if(prod<0) System.err.print("");
        return new Timing(enc,dec);
    }

    /** JIT ısınması. */
    public static void warmup(){
        Random rnd=new Random(42);
        List<Integer> s=new ArrayList<>(200000); int p=0;
        for(int i=0;i<200000;i++){ p=(p*3+rnd.nextInt(40))%256; s.add(p); }
        for(int r=0;r<3;r++){ timeUnconditional(s); timeConditional(s); }
    }
    private TimingBenchmark(){}
}
