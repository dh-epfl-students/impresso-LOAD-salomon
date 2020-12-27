package wikidatademo.graph;

/**
 * Item used to pass page node information to the interface
 *
 * Originally published November 2017 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2017 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class PageInfoItem {

    public int node_id;			  // internal ID of the page node in the graph
    public String type;			  // type of node. Here: PAG
    public String title;		  // title of the page
    public String url;	          // url of the page
    public double score;		  // ranking score

    public PageInfoItem(int node_id, String title, String url, double score) {
        this.node_id = node_id;
        this.type = "PAG";
        this.score = score;
        this.title = title;
        this.url = url;
    }

    @Override
    public String toString() {
        String retval = type + " " + node_id + ": <" + score + "> " + title + " [" + url + "]";
        return retval;
    }

//	// return a unique string nodeID (IDs are not unique without the type information)
//	public String getStringNodeID() {
//		return type + node_id;
//	}
//
//	@Override
//	public int compareTo(Item_PageInfo eii) {
//		if (eii.score < score) return -1;
//        if (eii.score > score) return 1;
//        return 0;
//	}
//
//	@Override
//	public boolean equals(Object o) {
//		Item_PageInfo eii = (Item_PageInfo) o;
//		return (node_id == eii.node_id && type.equals(eii.type));
//	}

    @Override
    public int hashCode() {
        return node_id;
    }
}

