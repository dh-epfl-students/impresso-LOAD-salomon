package controller.structure;
import wikidatademo.graph.*;

import java.util.ArrayList;

/**
 * Created by Satya Almasian on 17/12/16.
 * Datatype for sending the results and the messages back to webpage
 * in each data sending stage not all of the fields are used
 */
public class ResultSet {
    private ArrayList<EntityInfoItem> result;// the result as entityInfoItem could also be the node list for the graph
    private ArrayList<PageInfoItem> resultPAG;
    private ArrayList<SentenceInfoItem> resultSEN;
    private String messages;//any messages for the browser could be error or others
    private ArrayList<EntityQueryItem> queryItems;//for passing the actual query items
    private ArrayList<GraphLink> links;//links for the graph
    private ArrayList<GraphNode> nodes;//nodes for the graph
    private ArrayList<EntityInfoItem> cachedItems;//items to be passed back to browser to fill the tagsinput

    public ResultSet()
    {
        messages="";
        cachedItems=new ArrayList<>();
    }
    public ArrayList<EntityInfoItem> getResult() {
        return result;
    }

    public void setResult(ArrayList<EntityInfoItem> result) {
        this.result = result;
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }
    public ArrayList<EntityQueryItem> getQueryItems() {
        return queryItems;
    }
    public boolean isMessageEmpty(){return this.messages.isEmpty();}

    public void setQueryItems(ArrayList<EntityQueryItem> queryItems) {
        this.queryItems = queryItems;
    }

    public ArrayList<GraphLink> getLinks() {
        return links;
    }

    public void setLinks(ArrayList<GraphLink> links) {
        this.links = links;
    }

    public ArrayList<EntityInfoItem> getCachedItems() {
        return cachedItems;
    }

    public void setCachedItems(ArrayList<EntityInfoItem> cachedItems) {
        this.cachedItems = cachedItems;
    }
    public boolean isCacheEmpty(){
        return this.cachedItems.size()<1;
    }
    public ArrayList<PageInfoItem> getResultPAG() {
        return resultPAG;
    }

    public void setResultPAG(ArrayList<PageInfoItem> resultPAG) {
        this.resultPAG = resultPAG;
    }

    public ArrayList<SentenceInfoItem> getResultSEN() {
        return resultSEN;
    }

    public void setResultSEN(ArrayList<SentenceInfoItem> resultSEN) {
        this.resultSEN = resultSEN;
    }

    public ArrayList<GraphNode> getNodes() {
        return nodes;
    }

    public void setNodes(ArrayList<GraphNode> nodes) {
        this.nodes = nodes;
    }



}
