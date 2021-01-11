package construction;

/**
 * LOAD Graph edge class
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class Edge implements Comparable<Edge> {
    public char sourceType;
    public char targetType;
    public int sourceId;
    public int targetId;
    public float weight;

    
    public Edge(char sType, char tType, int sId, int tId) {
        sourceType = sType;
        targetType = tType;
        sourceId = sId;
        targetId = tId;
    }

    @Override
    public boolean equals(Object obj) {
        Edge e = (Edge)obj;
        if (sourceType == e.sourceType && targetType == e.targetType && sourceId == e.sourceId && targetId == e.targetId) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public int compareTo(Edge e) {
        int rv = sourceType - e.sourceType;
        if (rv != 0) {
            return rv;
        } else {
            rv = sourceId - e.sourceId;
            if (rv != 0) {
                return rv;
            } else {
                rv = targetType - e.targetType;
                if (rv != 0) {
                    return rv;
                } else {
                    return targetId - e.targetId;
                }
            }
        }
    }
    
    @Override
    public int hashCode() {
        String s =" " + sourceType + targetType + sourceId + targetId;
        return(s.hashCode());
    }
    
}
