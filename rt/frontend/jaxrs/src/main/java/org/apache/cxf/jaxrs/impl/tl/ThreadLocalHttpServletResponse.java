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

package org.apache.cxf.jaxrs.impl.tl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class ThreadLocalHttpServletResponse extends AbstractThreadLocalProxy<HttpServletResponse> 
    implements HttpServletResponse {

    public void addCookie(Cookie cookie) {
        get().addCookie(cookie);
        
    }

    public void addDateHeader(String name, long date) {
        get().addDateHeader(name, date);
    }

    public void addHeader(String name, String value) {
        get().addHeader(name, value);
        
    }

    public void addIntHeader(String name, int value) {
        get().addIntHeader(name, value);
        
    }

    public boolean containsHeader(String name) {
        return get().containsHeader(name);
    }

    public String encodeRedirectURL(String url) {
        return get().encodeRedirectURL(url);
    }

    @SuppressWarnings("deprecation")
    public String encodeRedirectUrl(String url) {
        return get().encodeRedirectUrl(url);
    }

    public String encodeURL(String url) {
        return get().encodeURL(url);
    }

    @SuppressWarnings("deprecation")
    public String encodeUrl(String url) {
        return get().encodeUrl(url);
    }

    public void sendError(int sc) throws IOException {
        get().sendError(sc);
    }

    public void sendError(int sc, String msg) throws IOException {
        get().sendError(sc, msg);
    }

    public void sendRedirect(String location) throws IOException {
        get().sendRedirect(location);
    }

    public void setDateHeader(String name, long date) {
        get().setDateHeader(name, date);
    }

    public void setHeader(String name, String value) {
        get().setHeader(name, value);
        
    }

    public void setIntHeader(String name, int value) {
        get().setIntHeader(name, value);
    }

    public void setStatus(int sc) {
        get().setStatus(sc);
        
    }

    @SuppressWarnings("deprecation")
    public void setStatus(int sc, String sm) {
        get().setStatus(sc, sm);
    }

    public void flushBuffer() throws IOException {
        get().flushBuffer();
        
    }

    public int getBufferSize() {
        return get().getBufferSize();
    }

    public String getCharacterEncoding() {
        return get().getCharacterEncoding();
    }

    public String getContentType() {
        return get().getContentType();
    }

    public Locale getLocale() {
        return get().getLocale();
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return get().getOutputStream();
    }

    public PrintWriter getWriter() throws IOException {
        return get().getWriter();
    }

    public boolean isCommitted() {
        return get().isCommitted();
    }

    public void reset() {
        get().reset();
        
    }

    public void resetBuffer() {
        get().resetBuffer();
        
    }

    public void setBufferSize(int size) {
        get().setBufferSize(size);
        
    }

    public void setCharacterEncoding(String charset) {
        get().setCharacterEncoding(charset);
        
    }

    public void setContentLength(int len) {
        get().setContentLength(len);
        
    }

    public void setContentType(String type) {
        get().setContentType(type);
        
    }

    public void setLocale(Locale loc) {
        get().setLocale(loc);
        
    }

    

}
