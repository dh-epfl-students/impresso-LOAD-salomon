package wikidatademo.graph;

import org.bson.Document;

import static settings.WebInterfaceSettings.*;


/**
 * Item used to pass entity node information to the interface
 * 
 * Originally published November 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class EntityInfoItem implements Comparable<EntityInfoItem> {
	
	public int node_id;			  // internal ID of he node in the graph (unique in its set by type)
	public String entity_id;
	public String type;			  // type of the node (L, O, A, D, T, S, P)
	public String wikidata_id;    // Wikidata ID of the entity (Q....)
	public String label;		  // label / name of the entity
	public String description;	  // one-sentence Wikidata description of the entity
	public int sentence_degree;   // number of sentences this entity occurs in
	public double score;		  // text ranking score
	public boolean isQueryEntity; // true if this was also a query entity
	
	public EntityInfoItem(Document doc) {
		node_id = doc.getInteger(ci_Entity_node_id);
		entity_id = doc.getString(ci_Entity_id);
		type = doc.getString(ci_Entity_type);
		wikidata_id = doc.getString(ci_Entity_description); //NOTE: to change
		label = doc.getString(ci_Entity_label);
		description = doc.getString(ci_Entity_description); //NOTE: to change
		sentence_degree = doc.getInteger(ci_Entity_senDegree);
		if (doc.containsKey(ci_Entity_score)) {
			score = doc.getDouble(ci_Entity_score);
		}
		isQueryEntity = false;
	}
	
	public EntityInfoItem(int node_id, String type, double score, String label, String description) {
		this.node_id = node_id;
		this.type = type;
		this.score = score;
		this.label = label;
		this.description = description;
	}
	
	public String toString() {
		String retval = type + " " + entity_id + " [" + wikidata_id + "] " + label + " (" + description + ") <" + score + "|" + sentence_degree + ">";
		return retval;
	}
	
	public String toStringFormatSEN() {
		String retval = type + " " + entity_id + ": <" + score + "> " + label;
		return retval;
	}
	
	public String toStringFormatPAG() {
		String retval = type + " " + node_id + ": <" + score + "> " + label + " [" + description + "]";
		return retval;
	}
	
	// return a unique string nodeID (IDs are not unique without the type information)
	public String getStringNodeID() {
		return type + node_id;
	}
	
	@Override
	public int compareTo(EntityInfoItem eii) {
		if (eii.score < score) return -1;
        if (eii.score > score) return 1;
        return 0;
	}
	
	@Override
	public boolean equals(Object o) {
		EntityInfoItem eii = (EntityInfoItem) o;
		return (node_id == eii.node_id && type.equals(eii.type));
	}
	
	@Override
	public int hashCode() {
		return node_id;			// can cause at most 7 collision due to 7 types
	}
}
