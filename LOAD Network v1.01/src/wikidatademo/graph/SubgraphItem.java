package wikidatademo.graph;

import java.util.ArrayList;

/**
 * Item used to pass subgraph information to the interface
 * 
 * Originally published December 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class SubgraphItem {
	
	public ArrayList<EntityInfoItem> nodes;     // entities in the subgraph
	public ArrayList<EdgeItem> edges;			// edges of the subgraph.
	
	public SubgraphItem() {
		nodes = new ArrayList<EntityInfoItem>();
		edges = new ArrayList<EdgeItem>();
	}
	
	public void printGraph() {
		System.out.println("Graph nodes");
		for (EntityInfoItem eii : nodes) {
			System.out.println(" " + eii.toString());
		}
		System.out.println("Graph edges");
		for (EdgeItem ei : edges) {
			System.out.println(" " + ei.toString());
		}
	}
	
}
