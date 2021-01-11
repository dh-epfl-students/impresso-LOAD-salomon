package controller;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.eclipse.jetty.io.RuntimeIOException;
import spark.ModelAndView;
import spark.TemplateEngine;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Defaults to the 'templates' directory under the resource path.
 */
public class MustacheTemplateEngine extends TemplateEngine {

    //Mustache template engine is used to pass the objects from server side to the client sie
    //to personlize the properties of the class we extend a template Engine from it and use our own
    private MustacheFactory mf;

    public MustacheTemplateEngine() {
        mf = new DefaultMustacheFactory("templates");//so the template is the root path for the html pages

    }

    public MustacheTemplateEngine(String root) {
        mf = new DefaultMustacheFactory(root);
    }

    public MustacheTemplateEngine(MustacheFactory mf) {
        this.mf = mf;
    }

    @Override
    public String render(ModelAndView modelAndView) {
        String viewName = modelAndView.getViewName();
        Mustache mustache = mf.compile(viewName);
        StringWriter sw = new StringWriter();
        try {
            mustache.execute(sw, modelAndView.getModel()).close();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        return sw.toString();
    }
}
