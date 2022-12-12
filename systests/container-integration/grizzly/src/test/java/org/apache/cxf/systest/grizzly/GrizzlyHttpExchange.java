package org.apache.cxf.systest.grizzly;

import jakarta.xml.ws.spi.http.HttpContext;
import jakarta.xml.ws.spi.http.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

public class GrizzlyHttpExchange extends HttpExchange {
    private final Request request;
    private final Response response;
    private Map<String, List<String>> requestHeaders;
    private Map<String, List<String>> responseHeaders;
    private final GrizzlyHttpContext context;

    public GrizzlyHttpExchange(GrizzlyHttpContext context, Request request, Response response) {
        this.context = context;
        this.request = request;
        this.response = response;
    }

    @Override
    public Map<String, List<String>> getRequestHeaders() {
        if (requestHeaders == null) {

            requestHeaders = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
            for (String key : request.getHeaderNames()) {
                List<String> values = new ArrayList<String>();
                for (String value : request.getHeaders(key)) {
                    values.add(value);
                }
                requestHeaders.put(key, values);
            }
        }
        return requestHeaders;
    }

    @Override
    public String getRequestHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        if (responseHeaders == null) {
            responseHeaders = new ResponseHeaders(response);
            for (String key : response.getHeaderNames()) {
                List<String> values = new ArrayList<String>();
                for (String value : response.getHeaderValues(key)) {
                    values.add(value);
                }
                responseHeaders.put(key, values);
            }

        }
        return responseHeaders;
    }

    @Override
    public void addResponseHeader(String name, String value) {
        response.addHeader(name, value);
    }

    @Override
    public String getRequestURI() {
        return request.getRequestURI() + "?" + request.getQueryString();
    }

    @Override
    public String getContextPath() {
        return request.getContextPath();
    }

    @Override
    public String getRequestMethod() {
        return request.getMethod().getMethodString();
    }

    @Override
    public HttpContext getHttpContext() {

        return context;
    }

    @Override
    public void close() throws IOException {
        response.finish();
    }

    @Override
    public InputStream getRequestBody() throws IOException {
        return request.getInputStream();
    }

    @Override
    public OutputStream getResponseBody() throws IOException {
        return response.getOutputStream();
    }

    @Override
    public void setStatus(int status) {
        response.setStatus(status);

    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return InetSocketAddress.createUnresolved(request.getRemoteHost(), request.getRemotePort());
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return InetSocketAddress.createUnresolved(request.getLocalAddr(), request.getLocalPort());
    }

    @Override
    public String getProtocol() {
        return request.getProtocol().getProtocolString();
    }

    @Override
    public String getScheme() {
        return request.getScheme();
    }

    @Override
    public String getPathInfo() {
        return request.getRequestURI();
    }

    @Override
    public String getQueryString() {
        return request.getQueryString();
    }

    @Override
    public Object getAttribute(String name) {
        return request.getAttribute(name);
    }

    @Override
    public Set<String> getAttributeNames() {
        return request.getAttributeNames();
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    public class ResponseHeaders extends TreeMap<String, List<String>> {
        private final Response response;
        public ResponseHeaders(Response response) {
            super(String.CASE_INSENSITIVE_ORDER);
            this.response = response;
        }
        @Override
        public List<String> put(String key, List<String> value) {
            for (String val : value) {
                response.addHeader(key, val);
            }
            return super.put(key, value);
        }
    }
}