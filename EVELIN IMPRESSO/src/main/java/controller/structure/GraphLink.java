package controller.structure;

/**
 * Created by Satya Almasian on 21/12/16.
 * Class has two properties , target and souce that are interger ids for the edges
 * an array list of GraphLinks presents the edge list of the graph
 */
public class GraphLink {
    public GraphLink(int source,int target,double weight)
    {
        this.source=source;//source node number
        this.target=target;//target node number
        this.weight=weight;//weight of the edge
    }
    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }


    private  int source;
    private int target;
    private double weight;
}