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

package org.apache.cxf.transport.http.netty.server.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Locale;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.AsciiString;

import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION;


public class NettyServletResponse implements HttpServletResponse {

    private HttpResponse originalResponse;

    private NettyServletOutputStream outputStream;

    private PrintWriter writer;

    private boolean responseCommited;

    public NettyServletResponse(HttpResponse response) {
        this.originalResponse = response;
        this.outputStream = new NettyServletOutputStream((HttpContent)response);
        this.writer = new PrintWriter(this.outputStream);
    }

    public HttpResponse getOriginalResponse() {
        return originalResponse;
    }

    public void addDateHeader(String name, long date) {
        this.originalResponse.headers().set(name, date);
    }

    public void addHeader(String name, String value) {
        this.originalResponse.headers().set(name, value);
    }

    public void addIntHeader(String name, int value) {
        this.originalResponse.headers().set(name, value);
    }

    @Override
    public void addCookie(jakarta.servlet.http.Cookie cookie) {
        //TODO Do we need to implement it ?
    }

    public boolean containsHeader(String name) {
        return this.originalResponse.headers().contains(name);
    }

    public void sendError(int sc) throws IOException {
        this.originalResponse.setStatus(HttpResponseStatus.valueOf(sc));
    }

    public void sendError(int sc, String msg) throws IOException {
        this.originalResponse.setStatus(new HttpResponseStatus(sc, msg));

    }

    public void sendRedirect(String location) throws IOException {
        setStatus(302);
        setHeader(LOCATION, location);
    }

    public void setDateHeader(String name, long date) {
        this.originalResponse.headers().set(name, date);
    }

    public void setHeader(AsciiString name, String value) {
        this.originalResponse.headers().set(name, value);
    }

    public void setHeader(String name, String value) {
        this.originalResponse.headers().set(name, value);
    }

    public void setIntHeader(String name, int value) {
        this.originalResponse.headers().set(name, value);

    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return this.outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return this.writer;
    }

    public void setStatus(int sc) {
        this.originalResponse.setStatus(HttpResponseStatus.valueOf(sc));
    }

    public void setStatus(int sc, String sm) {
        this.originalResponse.setStatus(new HttpResponseStatus(sc, sm));
    }

    @Override
    public void setContentType(String type) {
        this.originalResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, type);
    }

    @Override
    public void setContentLength(int len) {
        HttpUtil.setContentLength(this.originalResponse, len);
    }

    @Override
    public boolean isCommitted() {
        return this.responseCommited;
    }

    @Override
    public void reset() {
        if (isCommitted()) {
            throw new IllegalStateException("Response already commited!");
        }

        this.originalResponse.headers().clear();
        this.resetBuffer();
    }

    @Override
    public void resetBuffer() {
        if (isCommitted()) {
            throw new IllegalStateException("Response already commited!");
        }
        this.outputStream.resetBuffer();
    }

    @Override
    public void flushBuffer() throws IOException {
        this.getWriter().flush();
        this.responseCommited = true;
    }

    @Override
    public int getBufferSize() {
        return this.outputStream.getBufferSize();
    }

    @Override
    public void setBufferSize(int size) {
        // we using always dynamic buffer for now
    }

    public String encodeRedirectURL(String url) {
        return this.encodeURL(url);
    }

    public String encodeRedirectUrl(String url) {
        return this.encodeURL(url);
    }

    public String encodeURL(String url) {
        try {
            return URLEncoder.encode(url, getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error encoding url!", e);
        }
    }

    public String encodeUrl(String url) {
        return this.encodeRedirectURL(url);
    }

    @Override
    public String getCharacterEncoding() {
        throw new IllegalStateException(
                "Method 'getCharacterEncoding' not yet implemented!");
    }

    @Override
    public String getContentType() {
        throw new IllegalStateException(
                "Method 'getContentType' not yet implemented!");
    }

    @Override
    public Locale getLocale() {
        throw new IllegalStateException(
                "Method 'getLocale' not yet implemented!");
    }

    @Override
    public void setCharacterEncoding(String charset) {
        throw new IllegalStateException(
                "Method 'setCharacterEncoding' not yet implemented!");

    }

    @Override
    public void setLocale(Locale loc) {
        throw new IllegalStateException(
                "Method 'setLocale' not yet implemented!");

    }

    @Override
    public void setContentLengthLong(long len) {
        throw new IllegalStateException("Method 'setContentLengthLong' not yet implemented!");
    }

    @Override
    public int getStatus() {
        throw new IllegalStateException("Method 'getStatus' not yet implemented!");
    }

    @Override
    public String getHeader(String name) {
        throw new IllegalStateException("Method 'getHeader' not yet implemented!");
    }

    @Override
    public Collection<String> getHeaders(String name) {
        throw new IllegalStateException("Method 'getHeaders' not yet implemented!");
    }

    @Override
    public Collection<String> getHeaderNames() {
        throw new IllegalStateException("Method 'getHeaderNames' not yet implemented!");
    }

    @Override
    public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
        setStatus(302);
        setHeader(LOCATION, location);
        if (clearBuffer) {
            resetBuffer();
        }
    }
}
