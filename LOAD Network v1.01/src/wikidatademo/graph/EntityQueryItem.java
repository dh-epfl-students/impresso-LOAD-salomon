package wikidatademo.graph;

/**
 * Item used to pass lists of query entities to the graph
 * 
 * Originally published November 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class EntityQueryItem {
	
	public int node_id;			// internal ID of he node in the graph (unique in its set by type)
	public String type;			// type of the node (L, O, A, D, T)
	
	public EntityQueryItem(int node_id, String type) {
		this.node_id = node_id;
		this.type = type;
	}
	
	public String toString() {
		String retval = type + " " + node_id;
		return retval;
	}
}
