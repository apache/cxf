/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.transport.websocket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.common.logging.LogUtils;

/**
 * 
 */
public class WebSocketVirtualServletResponse implements HttpServletResponse {
    private static final Logger LOG = LogUtils.getL7dLogger(WebSocketVirtualServletResponse.class);
    private WebSocketServletHolder webSocketHolder;
    private Map<String, String> responseHeaders;
    private boolean flushed;

    public WebSocketVirtualServletResponse(WebSocketServletHolder websocket) {
        this.webSocketHolder = websocket;
        this.responseHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    public void flushBuffer() throws IOException {
        LOG.log(Level.INFO, "flushBuffer()");
        if (!flushed) {
            //REVISIT this mechanism to determine if the headers have been flushed
            if (responseHeaders.get(WebSocketUtils.FLUSHED_KEY) == null) {
                byte[] data = WebSocketUtils.buildResponse(responseHeaders, null, 0, 0);
                webSocketHolder.write(data, 0, data.length);
                responseHeaders.put(WebSocketUtils.FLUSHED_KEY, "true");
            }
            flushed = true;
        }
    }

    @Override
    public int getBufferSize() {
        LOG.log(Level.INFO, "getBufferSize()");
        return 0;
    }

    @Override
    public String getCharacterEncoding() {
        // TODO Auto-generated method stub
        LOG.log(Level.INFO, "getCharacterEncoding()");
        return null;
    }

    @Override
    public String getContentType() {
        LOG.log(Level.INFO, "getContentType()");
        return responseHeaders.get("Content-Type");
    }

    @Override
    public Locale getLocale() {
        // TODO Auto-generated method stub
        LOG.log(Level.INFO, "getLocale");
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                byte[] data = new byte[1];
                data[0] = (byte)b;
                write(data, 0, 1);
            }

            @Override
            public void write(byte[] data, int offset, int length) throws IOException {
                if (responseHeaders.get(WebSocketUtils.FLUSHED_KEY) == null) {
                    data = WebSocketUtils.buildResponse(responseHeaders, data, offset, length);
                    responseHeaders.put(WebSocketUtils.FLUSHED_KEY, "true");
                } else {
                    data = WebSocketUtils.buildResponse(data, offset, length);
                }
                webSocketHolder.write(data, 0, data.length);
            }

            @Override
            public void close() throws IOException {
                if (responseHeaders.get(WebSocketUtils.FLUSHED_KEY) == null) {
                    byte[] data = WebSocketUtils.buildResponse(responseHeaders, null, 0, 0);
                    webSocketHolder.write(data, 0, data.length);
                    responseHeaders.put(WebSocketUtils.FLUSHED_KEY, "true");
                }
                super.close();
            }
            
        };
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        LOG.log(Level.INFO, "getWriter()");
        return new PrintWriter(new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                byte[] data = new byte[1];
                data[0] = (byte)b;
                write(data, 0, 1);
            }
            
            @Override
            public void write(byte[] data, int offset, int length) throws IOException {
                if (responseHeaders.get(WebSocketUtils.FLUSHED_KEY) == null) {
                    data = WebSocketUtils.buildResponse(responseHeaders, data, offset, length);
                    responseHeaders.put(WebSocketUtils.FLUSHED_KEY, "true");
                } else {
                    data = WebSocketUtils.buildResponse(data, offset, length);
                }
                webSocketHolder.write(data, 0, data.length);
            }

            @Override
            public void close() throws IOException {
                if (responseHeaders.get(WebSocketUtils.FLUSHED_KEY) == null) {
                    byte[] data = WebSocketUtils.buildResponse(responseHeaders, null, 0, 0);
                    webSocketHolder.write(data, 0, data.length);
                    responseHeaders.put(WebSocketUtils.FLUSHED_KEY, "true");
                }                
                super.close();
            }
        });
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetBuffer() {
        LOG.log(Level.INFO, "resetBuffer()");
    }

    @Override
    public void setBufferSize(int size) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "setBufferSize({0})", size);
        }
    }

    @Override
    public void setCharacterEncoding(String charset) {
        // TODO 
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "setCharacterEncoding({0})", charset);
        }
    }

    @Override
    public void setContentLength(int len) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "setContentLength({0})", len);
        }
        responseHeaders.put("Content-Length", Integer.toString(len));
    }

    @Override
    public void setContentType(String type) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "setContentType({0})", type);
        }
        responseHeaders.put("Content-Type", type);
    }

    @Override
    public void setLocale(Locale loc) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "setLocale({0})", loc);
        }
    }

    @Override
    public void addCookie(Cookie cookie) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "addCookie({0})", cookie);
        }
    }

    @Override
    public void addDateHeader(String name, long date) {
        // TODO
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "addDateHeader({0}, {1})", new Object[]{name, date});
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "addHeader({0}, {1})", new Object[]{name, value});
        }
        responseHeaders.put(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "addIntHeader({0}, {1})", new Object[]{name, value});
        }
        responseHeaders.put(name, Integer.toString(value));
    }

    @Override
    public boolean containsHeader(String name) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "containsHeader({0})", name);
        }
        return responseHeaders.containsKey(name);
    }

    @Override
    public String encodeRedirectURL(String url) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "encodeRedirectURL({0})", url);
        }
        return null;
    }

    @Override
    public String encodeRedirectUrl(String url) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "encodeRedirectUrl({0})", url);
        }
        return null;
    }

    @Override
    public String encodeURL(String url) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "encodeURL({0})", url);
        }
        return null;
    }

    @Override
    public String encodeUrl(String url) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "encodeUrl({0})", url);
        }
        return null;
    }

    @Override
    public String getHeader(String name) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "getHeader({0})", name);
        }
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        LOG.log(Level.INFO, "getHeaderNames()");
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "getHeaders({0})", name);
        }
        return null;
    }

    @Override
    public int getStatus() {
        LOG.log(Level.INFO, "getStatus()");
        String v = responseHeaders.get(WebSocketUtils.SC_KEY);
        return v == null ? 200 : Integer.parseInt(v);
    }

    @Override
    public void sendError(int sc) throws IOException {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "sendError{0}", sc);
        }
        responseHeaders.put(WebSocketUtils.SC_KEY, Integer.toString(sc));
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "sendError({0}, {1})", new Object[]{sc, msg});
        }
        responseHeaders.put(WebSocketUtils.SC_KEY, Integer.toString(sc));
        responseHeaders.put(WebSocketUtils.SM_KEY, msg);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        // TODO Auto-generated method stub
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "sendRedirect({0})", location);
        }
    }

    @Override
    public void setDateHeader(String name, long date) {
        // ignore
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "setDateHeader({0}, {1})", new Object[]{name, date});
        }
    }

    @Override
    public void setHeader(String name, String value) {
        // TODO Auto-generated method stub
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "setHeader({0}, {1})", new Object[]{name, value});
        }
    }

    @Override
    public void setIntHeader(String name, int value) {
        // TODO Auto-generated method stub
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "setIntHeader({0}, {1})", new Object[]{name, value});
        }
    }

    @Override
    public void setStatus(int sc) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "setStatus({0})", sc);
        }
        responseHeaders.put(WebSocketUtils.SC_KEY, Integer.toString(sc));
    }

    @Override
    public void setStatus(int sc, String sm) {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "setStatus({0}, {1})", new Object[]{sc, sm});
        }
        responseHeaders.put(WebSocketUtils.SC_KEY, Integer.toString(sc));
        responseHeaders.put(WebSocketUtils.SM_KEY, sm);
    }
}
