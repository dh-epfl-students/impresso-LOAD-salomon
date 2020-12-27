package wikidatademo.graph;

import org.bson.Document;

import static settings.LOADmodelSettings.*;
import static settings.WebInterfaceStaticSettings.*;


/**
 * Item used to pass entity node information to the interface
 * 
 * Originally published November 2016 at
 * http://dbs.ifi.uni-heidelberg.de/?id=load
 * (c) 2016 Andreas Spitz (spitz@informatik.uni-heidelberg.de)
 */
public class EntityInfoItem implements Comparable<EntityInfoItem> {

	// attributes that are always set
	public int node_id;			  // internal ID of he node in the graph (unique in its set by type)
	public String type;			  // type of the node (L, O, A, D, T)
	public String label;		  // label / name of the entity
	public int sentence_degree;   // number of sentences this entity occurs in
	public double score;		  // text ranking score
	public boolean isQueryEntity; // true if this was also a query entity

	// attributes that CAN be null:
	public String entityId;    // Wikidata ID of the entity (Q....). This is "null" for dates D and terms T!
	public String description;	  // one-sentence Wikidata description of the entity. This is "null" if no description exists 

	// constructor from an entity LOOKUP collection document as input
	public EntityInfoItem(Document doc) {
		node_id = doc.getInteger(ci_Lookup_id);
		type = doc.getString(ci_Lookup_type);
		if (type.equals(setNames[DAT])) {
			label = doc.getString(ci_Lookup_label);
		} else {
			label = doc.getString(ci_Lookup_fullLabel);
		}
		entityId = "NOT AVAILABLE IN LOOKUP";
		description = doc.getString(ci_Lookup_description);
		sentence_degree = doc.getInteger(ci_Lookup_senDegree);
		score = doc.getDouble(ci_Lookup_score);
		isQueryEntity = false;
	}

	// constructor from an entity collection document as input with custom score and query entity flag
	public EntityInfoItem(Document doc, double score, boolean isQueryEntity) {
		node_id = doc.getInteger(ci_Entity_node_id);
		type = doc.getString(ci_Entity_type);
		entityId = doc.getString(ci_Entity_id);
		label = doc.getString(ci_Entity_label);
		description = doc.getString(ci_Entity_description);
		sentence_degree = doc.getInteger(ci_Entity_senDegree);
		this.score = score;
		this.isQueryEntity = isQueryEntity;
	}

	// constructor from a term collection document as input 
	public EntityInfoItem(Document doc, String desc) {
		node_id = doc.getInteger(ci_Terms_id);
		type = setNames[TER];
		label = doc.getString(ci_Terms_label);
		sentence_degree = doc.getInteger(ci_Terms_senDegree);
		score = 0; // terms are matched precisely and have no score
		description = desc;
		isQueryEntity = false;
	}

	public EntityInfoItem(int node_id, String type, double score, String label, String description) {
		this.node_id = node_id;
		this.type = type;
		this.score = score;
		this.label = label;
		this.description = description;
	}

	// print to string for debugging
	@Override
	public String toString() {
		String retval = type + " " + node_id + " [" + entityId + "] " + label + " (" + description + ") <" + score + "|" + sentence_degree + ">";
		return retval;
	}

	// return a unique string nodeID (IDs are not unique without the type information)
	public String getStringNodeID() {
		return type + node_id;
	}

	@Override
	public int compareTo(EntityInfoItem e) {
		if (e.score < score) return -1;
		if (e.score > score) return 1;
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		EntityInfoItem e = (EntityInfoItem) o;
		return (node_id == e.node_id && type.equals(e.type));
	}

	@Override
	public int hashCode() {
		return node_id;			// can cause at most 5 collision due to 5 types LOADT
	}
}
