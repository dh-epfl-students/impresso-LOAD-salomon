package controller;
import controller.structure.GraphLink;
import controller.structure.GraphNode;
import controller.structure.ResultSet;
import controller.structure.Setting;
import wikidatademo.graph.*;
import wikidatademo.logger.QueryException;
import settings.WebInterfaceSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiFunction;

import static settings.LOADmodelSettings.*;
import static settings.SystemSettings.VERBOSE;

/**
 * Created by Satya Almasian on 12/6/2016 AD.
 * Parses queries with the methods from LOAD package and return the response to the Controller
 */
public class Service {
    private static LOADGraph g ;//object to connect to the webservice
    private  LOADGraph g_articles ;//object to connect to the webservice for the artilces
    private  LOADGraph g_wiki ;//object to connect to the webservice for the wikipedia

    public Service(Setting setting)
    {
        WebInterfaceSettings.Builder builder= new WebInterfaceSettings.Builder();
        builder.logfileName(setting.getlogfileName());
        builder.MongoAdress(setting.getDBHost());
        builder.Mongousername(setting.getMongoDBUsername());
        builder.Mongopassword(setting.getMongoDBPassword());
        builder.auth_db(setting.getMongoAuthenticationDB());
        builder.verbose(setting.getVerbose());
        builder.maxQueryDuration (setting.getmaxQueryDuration());
        builder.maxQueryCount (setting.getmaxQueryCount());
        builder.cohesion (setting.getcohesion());
        builder.sentenceTermLimit (setting.getsentenceTermLimit());
        builder.graphNeighbourLimit(setting.getgraphNeighbourLimit());
        builder.subgraphEdgeWeightLimit(setting.getsubgraphEdgeWeightLimit());
        builder.maxSentenceLength (setting.getmaxSentenceLength());
        builder.useSSHtunnel (setting.getuseSSHtunnel());
        builder.SSHHost(setting.getSSHHost());
        builder.LDAPUsername(setting.getLDAPUsername());
        builder.LDAPPassword(setting.getLDAPPassword());
        builder.SSHport(22);
        builder.MongoDBName(setting.getWikiDBName());
        g_wiki = new LOADGraph(new WebInterfaceSettings(builder.build()));
        g_articles=new LOADGraph(new WebInterfaceSettings(builder.build()));
        g=g_wiki;
    }
    /**
     * changes the active database for the queries
     * @param databaseName name of the database
     * @return the top 10 entities matching the query
     */
    public void changeDatabase(String databaseName)
    {
        if (databaseName.equals( "Wiki"))
        {
            g=g_wiki;
        }
        else if (databaseName.equals("Article"))
        {
            g=g_articles;
        }
    }
    /**
     * take the query from ajax function and find the top entities matching and return them
     * @param query the query to search for
     * @param user fingerprint of the user
     * @return the top 10 entities matching the query
     */
    public static ResultSet getTagInput(String query,String user)
    {
        ResultSet resultSet=new ResultSet();
        ArrayList<EntityInfoItem> result= new ArrayList<>();
        try {
            result = g.getEntitiesByLabel(query, 20,user);
            resultSet.setResult(result);
        } catch (QueryException e) {
            resultSet.setMessages(e.getMessage());//send errors to browser
        }
        return resultSet;

    }
    /**
     * take the query and find the top entities matching and return them
     * @param query the query to search for
     * @param target what the result should be
     * @param numResults number of results
     * @param user fingerprint of the user
     * @return the top numResults entities matching the query with messages for the webpage
     */
    public static ResultSet search(String query,String target,String user,int numResults) {
        ResultSet resultSet=processQuery(query,user);
        ArrayList<EntityQueryItem> queryItems=resultSet.getQueryItems();
        ArrayList<EntityInfoItem> result=null;
        ArrayList<SentenceInfoItem> resultSen=null;
        ArrayList<PageInfoItem> resultPag=null;

        try {

            //if it is a page
            if (target.equals(page)) {
                resultPag = g.pageQuery(queryItems, numResults,user);
                resultSet.setResultPAG(resultPag);

            } else if (target.equals(sentence)) {//if the target is a sentence
                resultSen = g.sentenceQuery(queryItems, numResults,user);
                resultSet.setResultSEN(resultSen);

            } else {//otherwise check the the other types

                //serach for resutls
                result = g.entityQuery(queryItems, target, numResults,user);
            }
        } catch (QueryException e) {//catch any exception on the LOAD side and send messages
            if(resultSet.isMessageEmpty())
            {
                resultSet.setMessages(e.getMessage());
            }
            else
            {
                resultSet.setMessages(resultSet.getMessages()+"\n"+e.getMessage());
            }
        }

        resultSet.setResult(result);
        return resultSet;

    }

    /**
     * take the query and create a graph and return it
     * @param query the query to search for
     * @param numNeighbors number of neighbors for the graph
     * @param user fingerprint of the user
     * @return the top numResults entities matching the query with messages for the webpage
     */
    public static ResultSet createGraph(String query,int numNeighbors,String user) {
            ResultSet resultSet=processQuery(query,user);
            HashMap<String,Integer> uniquekeys=new HashMap<>();//to give a unique number starting from zero to all the nodes
            //we need the node numbers for the graph visualization
            ArrayList<GraphLink> links=new ArrayList<>();//edge list
            ArrayList<GraphNode> nodes=new ArrayList<>();//node list

        GraphLink link;
        GraphNode nodeg;
            int count=0;
            ArrayList<EntityQueryItem> queryItems=resultSet.getQueryItems();

            //create graph
            SubgraphItem result = null;
            try {
                result = g.subgraphQuery(queryItems,user);
            } catch (QueryException e) {//catch any exception on the LOAD side and send messages
                if(resultSet.isMessageEmpty())
                {
                    resultSet.setMessages(e.getMessage());
                }
                else
                {
                    resultSet.setMessages(resultSet.getMessages()+"\n"+e.getMessage());
                }
            }

            //-------- Create the custom edge list for the D3 visualization based on integer values for node ids
            try {
                //give a unique key to each node

                for (EntityInfoItem node : result.nodes) {
                    uniquekeys.put(node.type + node.node_id, count);
//                    node.node_id=count;
                    nodeg = new GraphNode(count,node);
                    nodes.add(nodeg);
                    count++;
                }
                //re-create the edge list based on the new node numbers
                for (EdgeItem edge : result.edges) {
                    link = new GraphLink(uniquekeys.get(edge.sourceID), uniquekeys.get(edge.targetID),edge.weight);
                    links.add(link);
                }
            }
            catch (Exception e)
            {
                if(resultSet.isMessageEmpty())
                {
                    resultSet.setMessages( "Error while creating the custom edge list :" +e.getMessage());
                }
                else
                {
                    resultSet.setMessages(resultSet.getMessages()+"\n"+"Error while creating the custom edge list :" +e.getMessage());
                }

            }
            try {
                //add to the result set
                resultSet.setResult(result.nodes);
                resultSet.setLinks(links);
                resultSet.setNodes(nodes);
            }
            catch (Exception e)
            {
                if(resultSet.isMessageEmpty())
                {
                    resultSet.setMessages( "Result is empty :" +e.getMessage());
                }
                else
                {
                    resultSet.setMessages(resultSet.getMessages()+"\n"+"Result is empty  :" +e.getMessage());
                }
            }

            return resultSet;

    }
    /**
     * take the query and find the top entities matching to that specific node inn the graph
     * @param query the query to search for
     * @param target what the result should be
     * @param numResults number of results
     * @param user fingerprint of the user
     * @return the top numResults entities matching the query with messages for the webpage
     */
    public static ResultSet findRevelent(String query,String target,String user,int numResults) {
        ResultSet resultSet=processQuery(query,user);
        ArrayList<EntityQueryItem> queryItems=resultSet.getQueryItems();
        ArrayList<EntityInfoItem> result=null;
        try {
                //serach for resutls
                System.out.println("LINE 231");
                result = g.entityQuery(queryItems, target, numResults,user);

        } catch (QueryException e) {//catch any exception on the LOAD side and send messages
            if(resultSet.isMessageEmpty())
            {
                resultSet.setMessages(e.getMessage());
            }
            else
            {
                resultSet.setMessages(resultSet.getMessages()+"\n"+e.getMessage());
            }
        }

        resultSet.setResult(result);
        return resultSet;

    }
    /**
     * Proccess the query and check if all the term exists also create messages for the webpage
     * @param query the query to search for
     * @param user fingerprint of the user
     * @return the top numResults entities matching the query with messages for the webpage
     */
    public static ResultSet processQuery(String query,String user){
        String[] tagsTypes = query.split("\\$");//first split for tags and types together
        ArrayList<EntityQueryItem> queryItems = new ArrayList<EntityQueryItem>();
        ResultSet resultSet=new ResultSet();
        ArrayList<String> messages=new ArrayList<>();//for sending error messages
        ArrayList<EntityInfoItem> cachedItems=new ArrayList<>();
        try {
            for (String tagType : tagsTypes)//split for tag and type separatly
            {//each tag is passed by three information part tag(if not term it is node id else it is the term itself)!!type!!label
                String tag = tagType.split("!!")[0];
                String type = tagType.split("!!")[1];
                String label = tagType.split("!!")[2];

                if (!type.equals(term)) {//only if it is term try to find a matching term in the system if not remove it
                    queryItems.add(new EntityQueryItem(Integer.parseInt(tag.replace("/","")), type.replace("/","")));
                    cachedItems.add(new EntityInfoItem(Integer.parseInt(tag.replace("/","")), type.replace("/",""), 0, label, ""));
                } else {
                    //check if the term exists then add it to the list of query
                    EntityInfoItem item = null;
                    try {
                        item = g.getTermByLabel(label,user);
                    } catch (QueryException e) {
                        if(resultSet.isMessageEmpty())
                        {
                            resultSet.setMessages("Error while pre-proccesing the query : "+e.getMessage());
                        }
                        else
                        {
                            resultSet.setMessages(resultSet.getMessages()+"\n"+"Error while pre-proccesing the query : "+e.getMessage());
                        }
                    }

                    if (item != null) {
                        queryItems.add(new EntityQueryItem(item.node_id, type));//query items are made witht the node id and the type
                        cachedItems.add(item);

                    } else
                        messages.add(label);//to report the label as not found
                }
            }
            resultSet.setQueryItems(queryItems);//send the query items
            resultSet.setCachedItems(cachedItems);//send cache

            // constructs the message if available
            if(messages.size()>0){
                String m="Requested Query :";
                for(String i:messages)
                {
                    m+=" <"+i+" > ";
                }
                m+=" does not exist in our Database. ";
                resultSet.setMessages(m);
            }
        } catch (Exception e)
        {
            if(resultSet.isMessageEmpty())
        {
            resultSet.setMessages("Error while pre-proccesing the query : "+e.getMessage());
        }
        else
        {
            resultSet.setMessages(resultSet.getMessages()+"\n"+"Error while pre-proccesing the query : "+e.getMessage());
        }
        }
        return resultSet;
    }
}
