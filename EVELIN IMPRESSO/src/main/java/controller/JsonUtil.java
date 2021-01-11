package controller;

import com.google.gson.Gson;
import spark.ResponseTransformer;

/**
 * Created by Satya Almasian on 21/12/16.
 * parse objects to json
 */
public class JsonUtil {
    //class is used for passing json data to the browser
    public static String toJson(Object object) {
        return new Gson().toJson(object);
    }

    public static ResponseTransformer json() {
        return JsonUtil::toJson;
    }
}

