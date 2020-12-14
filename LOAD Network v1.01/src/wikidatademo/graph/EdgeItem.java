package wikidatademo.graph;

/**
 * Item used to pass edge information in subgraphs to the interface
 * 
 * Originally published December 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class EdgeItem {
	
	public String sourceID;	// concatenation of node type and node id. E.g. the LOC node with id 123 has id LOC123
	public String targetID;
	public double weight;
	
	public EdgeItem(String source, String target, double weight) {
		if (source.compareTo(target) < 0) {
			sourceID = source;
			targetID = target;
		} else {
			sourceID = target;
			targetID = source;
		}
		this.weight = weight;
	}
	
	@Override
	public boolean equals(Object o) {
		EdgeItem ei = (EdgeItem) o;
		return (sourceID.equals(ei.sourceID) && targetID.equals(ei.targetID));
	}
	
	@Override
	public int hashCode() {
		String combined = sourceID + targetID;
		return combined.hashCode();
	}
	
	@Override
	public String toString() {
		return sourceID + " -- " + targetID + " (" + weight + ")";
	}
	
}
