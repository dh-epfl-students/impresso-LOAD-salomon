package graphQuery;

/**
 * Query output format that can be used for ranking query results.
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class RankItem implements Comparable<RankItem> {
    public GraphNode source;
    public GraphNode target;
    public double weight;
    
    public RankItem(GraphNode s, GraphNode t, double w) {
        source = s;
        target = t;
        weight = w;
    }
    
    @Override
    public int compareTo(RankItem r) {
        if (r.weight < weight) return -1;
        if (r.weight > weight) return 1;
        return 0;
    }
    
    @Override
    public boolean equals(Object o) {
        RankItem r = (RankItem) o;
        //return (source == r.source && target == r.target);
        return (target == r.target);
    }
    
    @Override
    public int hashCode() {
        //return source.hashCode() + target.hashCode();
        return target.hashCode();
    }
    
}
