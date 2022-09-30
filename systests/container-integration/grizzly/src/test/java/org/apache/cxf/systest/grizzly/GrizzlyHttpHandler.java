package org.apache.cxf.systest.grizzly;

import jakarta.xml.ws.spi.http.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

public class GrizzlyHttpHandler extends org.glassfish.grizzly.http.server.HttpHandler {

    private final jakarta.xml.ws.spi.http.HttpHandler spihandler;
    private final GrizzlyHttpContext context;

    public GrizzlyHttpHandler(HttpHandler handler, GrizzlyHttpContext context) {
        spihandler = handler;
        this.context = context;
    }

    @Override
    public void service(Request request, Response response) throws Exception {
        spihandler.handle(new GrizzlyHttpExchange(context, request, response));
    }

}