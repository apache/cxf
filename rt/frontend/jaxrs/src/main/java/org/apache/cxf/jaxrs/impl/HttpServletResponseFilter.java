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
package org.apache.cxf.jaxrs.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.cxf.message.Message;

public class HttpServletResponseFilter extends HttpServletResponseWrapper {

    private Message m;
    public HttpServletResponseFilter(HttpServletResponse response, Message message) {
        super(response);
        m = message;
    }

    @Override
    public void setStatus(int sc) {
        super.setStatus(sc);
        m.getExchange().put(Message.RESPONSE_CODE, sc);
    }
    
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStreamFilter(super.getOutputStream(), m);
    }
    
    @Override
    public void setHeader(String name, String value) {
        setHeaderInternal(name, value, false);
    }
    
    @Override
    public void addHeader(String name, String value) {
        setHeaderInternal(name, value, true);
    }
    
    @SuppressWarnings("unchecked")
    private void setHeaderInternal(String name, String value, boolean add) {
        Map<String, List<String>> headers = 
            (Map<String, List<String>>)m.get(Message.PROTOCOL_HEADERS);
        List<String> values = headers.get(name);
        if (values == null) {
            values = new ArrayList<String>();
            headers.put(name, values);
        }
        if (!add) {
            values.clear();
        }
        values.add(value);
    }
}

