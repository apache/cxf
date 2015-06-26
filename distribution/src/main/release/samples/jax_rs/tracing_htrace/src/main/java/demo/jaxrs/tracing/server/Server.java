package demo.jaxrs.tracing.server;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.apache.cxf.tracing.htrace.jaxrs.HTraceFeature;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Server {
    protected Server() throws Exception {
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(9000);

        // Register and map the dispatcher servlet
        final ServletHolder servletHolder = new ServletHolder(new CXFNonSpringJaxrsServlet());
        final ServletContextHandler context = new ServletContextHandler();      
        context.setContextPath("/");
        context.addServlet(servletHolder, "/*");
        
        servletHolder.setInitParameter("jaxrs.serviceClasses", Catalog.class.getName());
        servletHolder.setInitParameter("jaxrs.features", HTraceFeature.class.getName());
        servletHolder.setInitParameter("jaxrs.providers", StringUtils.join(
            new String[] { 
                JsrJsonpProvider.class.getName()
            }, ",") 
        );                
                
        server.setHandler(context);
        server.start();
        server.join();
    }

    public static void main(String args[]) throws Exception {
        new Server();
        System.out.println("Server ready...");

        Thread.sleep(5 * 6000 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }
}
