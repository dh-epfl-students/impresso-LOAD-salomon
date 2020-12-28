package controller.structure;

import  wikidatademo.graph.EntityInfoItem;

public class GraphNode {
    public GraphNode(int uniqueKey, EntityInfoItem node)
    {
       this.uniqueKey=uniqueKey;
       this.node_id=node.node_id;
       this.type=node.type;
       this.label=node.label;
       this.sentence_degree=node.sentence_degree;
       this.score=node.score;
       this.isQueryEntity=node.isQueryEntity;
       this.wikidata_id=node.description;
       this.description=node.description;
    }



    public int getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(int uniqueKey) {
        this.uniqueKey = uniqueKey;
    }
    public int getNode_id() {
        return node_id;
    }

    public void setNode_id(int node_id) {
        this.node_id = node_id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getSentence_degree() {
        return sentence_degree;
    }

    public void setSentence_degree(int sentence_degree) {
        this.sentence_degree = sentence_degree;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public boolean isQueryEntity() {
        return isQueryEntity;
    }

    public void setQueryEntity(boolean queryEntity) {
        isQueryEntity = queryEntity;
    }

    public String getWikidata_id() {
        return wikidata_id;
    }

    public void setWikidata_id(String wikidata_id) {
        this.wikidata_id = wikidata_id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    private int uniqueKey;
    public int node_id;
    public String type;
    public String label;
    public int sentence_degree;
    public double score;
    public boolean isQueryEntity;
    public String wikidata_id;
    public String description;
}
