package graphQuery;

import static settings.LOADmodelSettings.*;

/**
 * LOAD graph node
 * 
 * Originally published July 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class GraphNode {
    
    public String value;       // label of the node, i.e. the entity name
    public int set;            // type of entity
    public int id;             // internal ID, type-specific
    
    // adjacency information
    public int[][] adjacency;
    
    // edge weight information (same indices as adjacency lists)
    public float[][] weights;
    
    // degree information: dates, locations, actors, organizations, terms and pages
    public int[] degrees;
    
    public GraphNode(int set, int id, String value, int[] degs) {
        this.set = set;
        this.id = id;
        this.value = value;
        degrees = new int[nANNOTATIONS];
        adjacency = new int[nANNOTATIONS][];
        weights = new float[nANNOTATIONS][];
        
        for (int i=0; i<nANNOTATIONS; i++) {
            degrees[i] = degs[i];
            adjacency[i] = new int[degs[i]];
            weights[i] = new float[degs[i]];
        }
    }
}
