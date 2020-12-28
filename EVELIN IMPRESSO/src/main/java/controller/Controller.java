package controller;

import controller.structure.ResultSet;
import controller.structure.Setting;
import wikidatademo.graph.*;
import spark.ModelAndView;
import spark.Spark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static controller.JsonUtil.json;
import static settings.LOADmodelSettings.*;
import static spark.Spark.*;

/**
 * Created by Satya Almasian on 12/6/2016 AD.
 * Main class that is responsible for reciving the client side information and passing it to appropiate methods on server side
 * Routes and any properites for the webserver is defined here .
 */
public class Controller {
    public static Setting settings;

    public Controller(final Service service) {
        staticFileLocation("files");//set where the static files like css and js are stored

        //set the port
        Spark.setPort(settings.getRunningPort());//choose the port to run the program on


        Map map = new HashMap();
        map.put("maxLimit",settings.getmaxNumberOfQueryEntities());
        //open the main page
        get("/",
                (rq, rs) ->{return new ModelAndView(map, "index.mustache");}, new MustacheTemplateEngine());
        get("/change_database",
                (rq, rs) ->
                {
                    String database_name=rq.queryParams("database");
                    service.changeDatabase(database_name);
                    return new ModelAndView(map, "index.mustache");

                }, new MustacheTemplateEngine());

        //take the query from ajax function and find the top entities matching and return them
        post("/search", (req, res) -> Service.getTagInput(
        req.queryParams("search"),req.queryParams("user")
        ), json());

        //search button is pressed the tabular info is returned
        get("/findmatch", (req, res) ->{
            ResultSet set=Service.processQuery(req.queryParams("query"),req.queryParams("user"));
            Map allResults=new HashMap();
            try {
                allResults.put("query", req.queryParams("query"));
                allResults.put("target", req.queryParams("target"));
                allResults.put("user", req.queryParams("user"));
                //check for cache
                if(!set.isCacheEmpty())
                {
                    allResults.put("cache",set.getCachedItems());
                }
                allResults.put("maxLimit",settings.getmaxNumberOfQueryEntities());
            }
            catch (Exception e)
            {
                allResults.put("message","Wrong Query : "+ e.getMessage());
            }

            return new ModelAndView(allResults, "result.mustache");}, new MustacheTemplateEngine());
        //the info for the result is acquired and returned
        get("/query",(req,res) ->{
            Map allResults=new HashMap();
            try {
                ResultSet set=Service.search(req.queryParams("query"),req.queryParams("target"), req.queryParams("user"),100);
                ArrayList<EntityInfoItem> result;
                if (req.queryParams("target").equals(sentence)) {
                    allResults.put("sentence", true);
                    ArrayList<SentenceInfoItem> resultSEN= set.getResultSEN();//get all the results
                    allResults.put("results", resultSEN);
                } else if (req.queryParams("target").equals(page)) {
                    allResults.put("page", true);
                    ArrayList<PageInfoItem> resultPAG= set.getResultPAG();//get all the results
                    allResults.put("results", resultPAG);
                } else if (req.queryParams("target").equals(dateEnt)) {
                    allResults.put("date", true);
                    result= set.getResult();//get all the results
                    allResults.put("results", result);
                } else if (req.queryParams("target").equals(term)) {
                    allResults.put("term", true);
                    result= set.getResult();//get all the results
                    allResults.put("results", result);
                } else {
                    allResults.put("other", true);
                    if(req.queryParams("target").equals(actEnt))
                        allResults.put("act", true);
                    else if(req.queryParams("target").equals(locEnt))
                        allResults.put("loc", true);
                    else if(req.queryParams("target").equals(orgEnt))
                        allResults.put("org", true);
                    result= set.getResult();//get all the results
                    allResults.put("results", result);
                }
                //check for messages
                if (!set.isMessageEmpty()) {
                    allResults.put("message", set.getMessages());
                }
                //check for cache
                if(!set.isCacheEmpty())
                {
                    allResults.put("cache",set.getCachedItems());
                }
                allResults.put("maxLimit",settings.getmaxNumberOfQueryEntities());

            }
            catch (Exception e)
            {
                e.printStackTrace();
                allResults.put("message","Wrong Query : "+ e.getMessage());
            }

            return allResults;},json());

        //show the template for graph
        get("/findgraph", (req, res) ->{
            ResultSet set=Service.processQuery(req.queryParams("query"),req.queryParams("user"));
            Map allResults=new HashMap();
            try {
                allResults.put("query", req.queryParams("query"));
                allResults.put("target", req.queryParams("target"));
                //check for cache
                if(!set.isCacheEmpty())
                {
                    allResults.put("cache",set.getCachedItems());
                }
                allResults.put("maxLimit",settings.getmaxNumberOfQueryEntities());

            }
            catch (Exception e)
            {
                allResults.put("message","Wrong Query : "+ e.getMessage());
            }
            return new ModelAndView(allResults, "graphV2.mustache");}, new MustacheTemplateEngine());

        //create and return the graph
        get("/graph", (req, res) ->{
            Map allResults=new HashMap();
            try {
                ResultSet set=Service.createGraph(req.queryParams("query"),3,req.queryParams("user"));
                allResults.put("nodes", set.getNodes());
                allResults.put("edges", set.getLinks());

                //check for messages
                if (!set.isMessageEmpty()) {
                    allResults.put("message", set.getMessages());
                }
                //check for cache
                if(!set.isCacheEmpty())
                {
                    allResults.put("cache",set.getCachedItems());
                }
                allResults.put("maxLimit",settings.getmaxNumberOfQueryEntities());
            }
            catch (Exception e)
            {
                allResults.put("message","Wrong Query: "+ e.getMessage());
            }
            return allResults;}, json());


        //------------Topic extention
        //create and return the graph for topics
        get("/topic", (req, res) ->{
            Map allResults=new HashMap();
            try {
                ResultSet set=Service.createGraph(req.queryParams("query"),3,req.queryParams("user"));
                allResults.put("nodes", set.getNodes());
                allResults.put("edges", set.getLinks());

                //check for messages
                if (!set.isMessageEmpty()) {
                    allResults.put("message", set.getMessages());
                }
                //check for cache
                if(!set.isCacheEmpty())
                {
                    allResults.put("cache",set.getCachedItems());
                }
                allResults.put("maxLimit",settings.getmaxNumberOfQueryEntities());
            }
            catch (Exception e)
            {
                allResults.put("message","Wrong Query: "+ e.getMessage());
            }
            return allResults;}, json());

        //show the template for graph
        get("/findtopic", (req, res) ->{
            ResultSet set=Service.processQuery(req.queryParams("query"),req.queryParams("user"));
            Map allResults=new HashMap();
            try {
                allResults.put("query", req.queryParams("query"));
                allResults.put("target", req.queryParams("target"));
                //check for cache
                if(!set.isCacheEmpty())
                {
                    allResults.put("cache",set.getCachedItems());
                }
                allResults.put("maxLimit",settings.getmaxNumberOfQueryEntities());

            }
            catch (Exception e)
            {
                allResults.put("message","Wrong Query : "+ e.getMessage());
            }
            return new ModelAndView(allResults, "topics.mustache");}, new MustacheTemplateEngine());

        //search for similiar nodes
        get("/find_relevent",(req,res) ->{
            Map allResults=new HashMap();
            try {
                ResultSet set=Service.findRevelent(req.queryParams("query"),req.queryParams("target"), req.queryParams("user"),5);
                ArrayList<EntityInfoItem> result;

                    if(req.queryParams("target").equals(actEnt))
                        allResults.put("act", true);
                    else if(req.queryParams("target").equals(locEnt))
                        allResults.put("loc", true);
                    else if(req.queryParams("target").equals(orgEnt))
                        allResults.put("org", true);
                    result= set.getResult();//get all the results
                    allResults.put("results", result);

                //check for messages
                if (!set.isMessageEmpty()) {
                    allResults.put("message", set.getMessages());
                }
                //check for cache
                if(!set.isCacheEmpty())
                {
                    allResults.put("cache",set.getCachedItems());
                }
                allResults.put("maxLimit",settings.getmaxNumberOfQueryEntities());

            }
            catch (Exception e)
            {
                e.printStackTrace();
                allResults.put("message","Wrong Query : "+ e.getMessage());
            }

            return allResults;},json());
        //getting started
        get("/getting_started", (rq, rs) -> new ModelAndView(map, "getting_started.mustache"), new MustacheTemplateEngine());

        //faq
        get("/faq", (rq, rs) -> new ModelAndView(map, "faq.mustache"), new MustacheTemplateEngine());

        //faq
        get("/test", (rq, rs) -> new ModelAndView(map, "test2.mustache"), new MustacheTemplateEngine());

    }

    public static void main(String[] args) {
        //read the setting files
        settings=new Setting(args[0]);
        new Controller(new Service(settings));
    }
    
}
