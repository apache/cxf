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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class HttpServletRequestFilter extends HttpServletRequestWrapper {

    private Message m;
    private boolean isPostFormRequest;
    private MultivaluedMap<String, String> formParams;
    public HttpServletRequestFilter(HttpServletRequest request, Message message) {
        super(request);
        m = message;
        isPostFormRequest = FormUtils.isFormPostRequest(m);
    }
    @Override
    public ServletInputStream getInputStream() throws IOException {
        InputStream is = m.getContent(InputStream.class);
        if (is instanceof DelegatingInputStream) {
            is = ((DelegatingInputStream)is).getInputStream();
        }
        if (is instanceof ServletInputStream) {
            return (ServletInputStream)is;
        }
        return super.getInputStream();
    }
    @Override
    public String getParameter(String name) {
        String[] values = this.getParameterValues(name);
        return values == null || values.length == 0 ? null : values[0];
    }
    @Override
    public String[] getParameterValues(String name) {
        String[] value = super.getParameterValues(name);
        if (value == null && isPostFormRequest) {
            readFromParamsIfNeeded();
            List<String> values = formParams.get(name);
            if (values != null) {
                value = values.toArray(new String[]{});
            }
        }
        return value;
    }
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> map1 = super.getParameterMap();
        if (isPostFormRequest) {
            readFromParamsIfNeeded();
            Map<String, String[]> map2 = new LinkedHashMap<>();
            map2.putAll(map1);
            for (Map.Entry<String, List<String>> e : formParams.entrySet()) {
                map2.put(e.getKey(), e.getValue().toArray(new String[]{}));
            }
            return Collections.unmodifiableMap(map2);
        }
        return map1;
    }
    @Override
    public Enumeration<String> getParameterNames() {
        Map<String, String[]> map = this.getParameterMap();
        final Iterator<String> it = map.keySet().iterator();
        return new Enumeration<String>() {

            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }

            @Override
            public String nextElement() {
                return it.next();
            }

        };
    }

    @SuppressWarnings("unchecked")
    private void readFromParamsIfNeeded() {
        if (formParams == null) {
            if (m.containsKey(FormUtils.FORM_PARAM_MAP)) {
                formParams = (MultivaluedMap<String, String>)m.get(FormUtils.FORM_PARAM_MAP);
            } else {
                formParams = new MetadataMap<>();
                MediaType mt = JAXRSUtils.toMediaType((String)m.get(Message.CONTENT_TYPE));
                String enc = HttpUtils.getEncoding(mt, StandardCharsets.UTF_8.name());
                String body = FormUtils.readBody(m.getContent(InputStream.class), enc);
                FormUtils.populateMapFromString(formParams, m, body, enc, true);
            }
        }

    }
}

