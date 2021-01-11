import spark.ModelAndView;
import controller.MustacheTemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;

public class TestServer {
  public static void main(String[] args) {
    Map map = new HashMap();
    map.put("name", "Sam");
    get("/hello", (rq, rs) -> new ModelAndView(map, "hello.mustache"), new MustacheTemplateEngine());
  }
}
