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

package org.apache.cxf.transport.websocket.undertow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.websocket.WebSocketConstants;
import org.apache.cxf.transport.websocket.WebSocketUtils;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

/**
 *
 */
public class WebSocketUndertowServletResponse implements HttpServletResponse {
    private static final Logger LOG = LogUtils.getL7dLogger(WebSocketUndertowServletResponse.class);
    private WebSocketChannel channel;
    private Map<String, String> responseHeaders;
    private ServletOutputStream outputStream;

    public WebSocketUndertowServletResponse(WebSocketChannel channel) {
        this.channel = channel;
        this.responseHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.outputStream = createOutputStream();
    }

    @Override
    public void flushBuffer() throws IOException {
        LOG.log(Level.FINE, "flushBuffer()");
        outputStream.flush();
    }

    @Override
    public int getBufferSize() {
        LOG.log(Level.FINE, "getBufferSize()");
        return 0;
    }

    @Override
    public String getCharacterEncoding() {
        LOG.log(Level.FINE, "getCharacterEncoding()");
        return null;
    }

    @Override
    public String getContentType() {
        LOG.log(Level.FINE, "getContentType()");
        return responseHeaders.get("Content-Type");
    }

    @Override
    public Locale getLocale() {
        LOG.log(Level.FINE, "getLocale");
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        LOG.log(Level.FINE, "getWriter()");
        return new PrintWriter(getOutputStream());
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
        LOG.log(Level.FINE, "resetBuffer()");
    }

    @Override
    public void setBufferSize(int size) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "setBufferSize({0})", size);
        }
    }

    @Override
    public void setCharacterEncoding(String charset) {
        // TODO
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "setCharacterEncoding({0})", charset);
        }
    }

    @Override
    public void setContentLength(int len) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "setContentLength({0})", len);
        }
        responseHeaders.put("Content-Length", Integer.toString(len));
    }

    @Override
    public void setContentType(String type) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "setContentType({0})", type);
        }
        responseHeaders.put("Content-Type", type);
    }

    @Override
    public void setLocale(Locale loc) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "setLocale({0})", loc);
        }
    }

    @Override
    public void addCookie(Cookie cookie) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "addCookie({0})", cookie);
        }
    }

    @Override
    public void addDateHeader(String name, long date) {
        // TODO
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "addDateHeader({0}, {1})", new Object[]{name, date});
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "addHeader({0}, {1})", new Object[]{name, value});
        }
        responseHeaders.put(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "addIntHeader({0}, {1})", new Object[]{name, value});
        }
        responseHeaders.put(name, Integer.toString(value));
    }

    @Override
    public boolean containsHeader(String name) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "containsHeader({0})", name);
        }
        return responseHeaders.containsKey(name);
    }

    

    @Override
    public String getHeader(String name) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "getHeader({0})", name);
        }
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        LOG.log(Level.FINE, "getHeaderNames()");
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "getHeaders({0})", name);
        }
        return null;
    }

    @Override
    public int getStatus() {
        LOG.log(Level.FINE, "getStatus()");
        String v = responseHeaders.get(WebSocketUtils.SC_KEY);
        return v == null ? 200 : Integer.parseInt(v);
    }

    @Override
    public void sendError(int sc) throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "sendError{0}", sc);
        }
        responseHeaders.put(WebSocketUtils.SC_KEY, Integer.toString(sc));
        byte[] data = WebSocketUtils.buildResponse(responseHeaders, null, 0, 0);
        WebSockets.sendText(ByteBuffer.wrap(data), channel, null);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "sendError({0}, {1})", new Object[]{sc, msg});
        }
        responseHeaders.put(WebSocketUtils.SC_KEY, Integer.toString(sc));
        byte[] data = WebSocketUtils.buildResponse(responseHeaders, null, 0, 0);
        WebSockets.sendText(ByteBuffer.wrap(data), channel, null);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "sendRedirect({0})", location);
        }
    }

    @Override
    public void setDateHeader(String name, long date) {
        // ignore
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "setDateHeader({0}, {1})", new Object[]{name, date});
        }
    }

    @Override
    public void setHeader(String name, String value) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "setHeader({0}, {1})", new Object[]{name, value});
        }
        responseHeaders.put(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "setIntHeader({0}, {1})", new Object[]{name, value});
        }
    }

    @Override
    public void setStatus(int sc) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "setStatus({0})", sc);
        }
        responseHeaders.put(WebSocketUtils.SC_KEY, Integer.toString(sc));
    }

    private ServletOutputStream createOutputStream() {
        //REVISIT
        // This output buffering is needed as the server side websocket does
        // not support the fragment transmission mode when sending back a large data.
        // And this buffering is only used for the response for the initial service innovation.
        // For the subsequently pushed data to the socket are sent back
        // unbuffered as individual websocket messages.
        // the things to consider :
        // - provide a size limit if we are use this buffering
        // - add a chunking mode in the cxf websocket's binding.
        //CHECKSTYLE:OFF
        return new ServletOutputStream() {
            private InternalByteArrayOutputStream buffer = new InternalByteArrayOutputStream();

            @Override
            public void write(int b) throws IOException {
                byte[] data = new byte[1];
                data[0] = (byte)b;
                write(data, 0, 1);
            }

            @Override
            public void write(byte[] data) throws IOException {
                write(data, 0, data.length);
            }

            @Override
            public void write(byte[] data, int offset, int length) throws IOException {
                if (responseHeaders.get(WebSocketUtils.FLUSHED_KEY) == null) {
                    // buffer the data until it gets flushed
                    buffer.write(data, offset, length);
                } else {
                    // unbuffered write to the socket
                    String respid = responseHeaders.get(WebSocketConstants.DEFAULT_RESPONSE_ID_KEY);
                    byte[] headers = respid != null
                        ? WebSocketUtils.buildHeaderLine(WebSocketConstants.DEFAULT_RESPONSE_ID_KEY, respid) : null;
                    data = WebSocketUtils.buildResponse(headers, data, offset, length);
                    WebSockets.sendText(ByteBuffer.wrap(data), channel, null);
                }
            }
            public void close() throws IOException {
                if (responseHeaders.get(WebSocketUtils.FLUSHED_KEY) == null) {
                    byte[] data = WebSocketUtils.buildResponse(responseHeaders, buffer.getBytes(), 0, buffer.size());
                    WebSockets.sendText(ByteBuffer.wrap(data), channel, null);
                    responseHeaders.put(WebSocketUtils.FLUSHED_KEY, "true");
                }
                super.close();
            }

            @Override
            public boolean isReady() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setWriteListener(WriteListener arg0) {
                throw new UnsupportedOperationException();
            }
        };
        //CHECKSTYLE:ON
    }

    private static final class InternalByteArrayOutputStream extends ByteArrayOutputStream {
        public byte[] getBytes() {
            return buf;
        }
    }

    @Override
    public void setContentLengthLong(long arg0) {
        throw new UnsupportedOperationException();

    }

    @Override
    public String encodeURL(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    @Override
    public String encodeRedirectURL(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }


    public void setStatus(int sc, String sm) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "setStatus({0})", sc);
        }
        responseHeaders.put(WebSocketUtils.SC_KEY, Integer.toString(sc));
    }
    
    @Override
    public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "sendRedirect({0}, {1}, {2})", new Object[]{location, sc, clearBuffer});
        }
    }
}
