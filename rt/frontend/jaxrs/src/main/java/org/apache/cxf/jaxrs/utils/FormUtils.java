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

package org.apache.cxf.jaxrs.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.phase.PhaseInterceptorChain;

public final class FormUtils {
    public static final String FORM_PARAM_MAP = "org.apache.cxf.form_data";
    
    private static final Logger LOG = LogUtils.getL7dLogger(FormUtils.class);
    private static final String MULTIPART_FORM_DATA_TYPE = "form-data";  
        
    private FormUtils() {
        
    }
    
    public static void addPropertyToForm(MultivaluedMap<String, String> map, String name, Object value) {
        if (!"".equals(name)) {
            map.add(name, value.toString());
        } else {
            MultivaluedMap<String, Object> values = 
                InjectionUtils.extractValuesFromBean(value, "");
            for (Map.Entry<String, List<Object>> entry : values.entrySet()) {
                for (Object v : entry.getValue()) {
                    map.add(entry.getKey(), v.toString());
                }
            }
        }
    }
    
    public static String readBody(InputStream is, String encoding) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copy(is, bos, 1024);
            return new String(bos.toByteArray(), encoding);
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }
    
    public static void populateMapFromString(MultivaluedMap<String, String> params, 
                                             String postBody, 
                                             String enc,
                                             boolean decode,
                                             HttpServletRequest request) {
        if (!StringUtils.isEmpty(postBody)) {
            List<String> parts = Arrays.asList(postBody.split("&"));
            for (String part : parts) {
                String[] keyValue = new String[2];
                int index = part.indexOf("=");
                if (index != -1) {
                    keyValue[0] = part.substring(0, index);
                    keyValue[1] = index + 1 < part.length() ? part.substring(index + 1) : "";
                } else {
                    keyValue[0] = part;
                    keyValue[1] = "";
                }
                String name = HttpUtils.urlDecode(keyValue[0], enc);
                if (decode) {
                    params.add(name, HttpUtils.urlDecode(keyValue[1], enc));
                } else {
                    params.add(name, keyValue[1]);
                }
            }
        } else if (request != null) {
            for (Enumeration en = request.getParameterNames(); en.hasMoreElements();) {
                String paramName = en.nextElement().toString();
                String[] values = request.getParameterValues(paramName);
                params.put(HttpUtils.urlDecode(paramName), Arrays.asList(values));
            }
            logRequestParametersIfNeeded(params, enc);
        }
    }
    
    public static void logRequestParametersIfNeeded(Map<String, List<String>> params, String enc) {
        String chain = PhaseInterceptorChain.getCurrentMessage().getInterceptorChain().toString();
        if (chain.contains(LoggingInInterceptor.class.getSimpleName())) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                writeMapToOutputStream(params, bos, enc, false);
                LOG.info(bos.toString(enc));
            } catch (IOException ex) {
                // ignore
            }
        }
    }
    
    public static void writeMapToOutputStream(Map<String, List<String>> map, 
                                              OutputStream os,
                                              String enc,
                                              boolean encoded) throws IOException {
        for (Iterator<Map.Entry<String, List<String>>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, List<String>> entry = it.next();
            for (Iterator<String> entryIterator = entry.getValue().iterator(); entryIterator.hasNext();) {
                String value = entryIterator.next();
                os.write(entry.getKey().getBytes(enc));
                os.write('=');
                String data = encoded ? value : HttpUtils.urlEncode(value);
                os.write(data.getBytes(enc));
                if (entryIterator.hasNext() || it.hasNext()) {
                    os.write('&');
                }
            }

        }
    }
    
    public static void populateMapFromMultipart(MultivaluedMap<String, String> params,
                                                MultipartBody body, 
                                                boolean decode) {
        List<Attachment> atts = body.getAllAttachments();
        for (Attachment a : atts) {
            ContentDisposition cd = a.getContentDisposition();
            if (cd != null && !MULTIPART_FORM_DATA_TYPE.equalsIgnoreCase(cd.getType())) {
                continue;
            }
            String cdName = cd == null ? null : cd.getParameter("name");
            String contentId = a.getContentId();
            String name = StringUtils.isEmpty(cdName) ? contentId : cdName.replace("\"", "").replace("'", "");
            if (StringUtils.isEmpty(name)) { 
                throw new WebApplicationException(400);
            }
            try {
                String value = IOUtils.toString(a.getDataHandler().getInputStream());
                params.add(HttpUtils.urlDecode(name),
                           decode ? HttpUtils.urlDecode(value) : value);
            } catch (IOException ex) {
                throw new WebApplicationException(415);
            }
        }
    }
}
