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
package org.apache.cxf.transport.http_jaxws_spi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.ws.spi.http.HttpExchange;

/**
 * This class provides a HttpServletResponse instance using information
 * coming from the HttpExchange instance provided
 * by the underlying container.
 * Note: many methods' implementation still TODO.
 *
 */
class HttpServletResponseAdapter implements HttpServletResponse {

    private HttpExchange exchange;
    private String characterEncoding;
    private Locale locale;
    private boolean committed;
    private ServletOutputStreamAdapter servletOutputStream;
    private PrintWriter writer;
    private int status;

    HttpServletResponseAdapter(HttpExchange exchange) {
        this.exchange = exchange;
    }

    public void flushBuffer() throws IOException {
        exchange.getResponseBody().flush();
        committed = true;
    }

    public int getBufferSize() {
        throw new UnsupportedOperationException();
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public String getContentType() {
        return this.getHeader("Content-Type");
    }

    public Locale getLocale() {
        return locale;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        if (servletOutputStream == null) {
            servletOutputStream = new ServletOutputStreamAdapter(exchange.getResponseBody());
        }
        return servletOutputStream;
    }

    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            if (characterEncoding != null) {
                writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(exchange.getResponseBody(),
                                                                                   characterEncoding)));
            } else {
                writer = new PrintWriter(exchange.getResponseBody());
            }
        }
        return writer;
    }

    public boolean isCommitted() {
        return committed;
    }

    public void reset() {
        throw new UnsupportedOperationException();
    }

    public void resetBuffer() {
        throw new UnsupportedOperationException();
    }

    public void setBufferSize(int size) {
        throw new UnsupportedOperationException();
    }

    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }

    public void setContentLength(int len) {
        if (!committed) {
            exchange.getResponseHeaders().put("Content-Length",
                Collections.singletonList(String.valueOf(len)));
        }
    }

    public void setContentType(String type) {
        if (!committed) {
            exchange.getResponseHeaders().put("Content-Type", Collections.singletonList(type));
        }
    }

    public void setLocale(Locale loc) {
        this.locale = loc;
    }

    public void addCookie(Cookie cookie) {
        throw new UnsupportedOperationException();
    }

    public void addDateHeader(String name, long date) {
        this.addHeader(name, String.valueOf(date));
    }

    public void addHeader(String name, String value) {
        exchange.addResponseHeader(name, value);
    }

    public void addIntHeader(String name, int value) {
        this.addHeader(name, String.valueOf(value));
    }

    public boolean containsHeader(String name) {
        return exchange.getResponseHeaders().containsKey(name);
    }

    public String encodeURL(String url) {
        throw new UnsupportedOperationException();
    }

    public String encodeRedirectURL(String url) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public String encodeUrl(String url) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public String encodeRedirectUrl(String url) {
        throw new UnsupportedOperationException();
    }

    public String getHeader(String name) {
        List<String> headers = exchange.getResponseHeaders().get(name);
        return (headers != null && !headers.isEmpty()) ? headers.get(0) : null;
    }

    public Collection<String> getHeaderNames() {
        return exchange.getResponseHeaders().keySet();
    }

    public Collection<String> getHeaders(String headerName) {
        return exchange.getResponseHeaders().get(headerName);
    }

    public int getStatus() {
        return status;
    }

    public void sendError(int sc) throws IOException {
        this.setStatus(sc);
        this.committed = true;
    }

    public void sendError(int sc, String msg) throws IOException {
        this.sendError(sc);
    }
    
    @Override
    public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void setDateHeader(String name, long date) {
        this.setHeader(name, String.valueOf(date));
    }

    public void setHeader(String name, String value) {
        List<String> list = new LinkedList<>();
        list.add(value);
        exchange.getResponseHeaders().put(name, list);
    }

    public void setIntHeader(String name, int value) {
        this.setHeader(name, String.valueOf(value));
    }

    public void setStatus(int sc) {
        this.status = sc;
        this.exchange.setStatus(sc);
    }

    @Deprecated
    public void setStatus(int sc, String sm) {
        this.setStatus(sc);
    }

    private static class ServletOutputStreamAdapter extends ServletOutputStream {

        private OutputStream delegate;

        ServletOutputStreamAdapter(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public boolean isReady() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setWriteListener(WriteListener arg0) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void setContentLengthLong(long arg0) {
        throw new UnsupportedOperationException();
    }
}
