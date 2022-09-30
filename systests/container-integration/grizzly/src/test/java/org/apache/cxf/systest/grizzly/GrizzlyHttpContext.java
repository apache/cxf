package org.apache.cxf.systest.grizzly;

import jakarta.xml.ws.spi.http.HttpContext;
import java.util.Map;
import java.util.Set;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpHandlerRegistration;
import org.glassfish.grizzly.http.server.HttpServer;

public class GrizzlyHttpContext extends HttpContext {
    private final String contextPath;
    private final String path;
    private final HttpServer server;

    public GrizzlyHttpContext(HttpServer server, String contextPath, String path) {
        this.server = server;
        this.contextPath = contextPath;
        this.path = path;
    }

    @Override
    public void setHandler(jakarta.xml.ws.spi.http.HttpHandler handler) {
        HttpHandlerRegistration httpHandlerRegistration = new HttpHandlerRegistration.Builder().
                contextPath(this.contextPath).urlPattern(path).build();
        for (Map.Entry<HttpHandler, HttpHandlerRegistration[]> httpHandlerMapping :
                server.getServerConfiguration().getHttpHandlersWithMapping().entrySet()) {
            for(HttpHandlerRegistration registration : httpHandlerMapping.getValue()) {
                if (registration.equals(httpHandlerRegistration)) {
                    server.getServerConfiguration().removeHttpHandler(httpHandlerMapping.getKey());
                }
            }
        }
        server.getServerConfiguration().addHttpHandler(new GrizzlyHttpHandler(handler, this), httpHandlerRegistration);
    }

    @Override
    public String getPath() {
        return path;
    }

    String getContextPath() {
        return contextPath;
    }

    jakarta.xml.ws.spi.http.HttpHandler getHandler() {
        return handler;
    }

    //There is no grizzly server configuration can be provided for jaxws service
    @Override
    public Object getAttribute(String s) {
        return null;
    }

    //There is no grizzly server configuration can be provided for jaxws service
    @Override
    public Set<String> getAttributeNames() {
        return null;
    }
}
