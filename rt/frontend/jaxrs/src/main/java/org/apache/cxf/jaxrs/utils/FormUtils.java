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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

public final class FormUtils {
    
    private static final String FORM_DATA_TYPE = "form-data";  
        
    private FormUtils() {
        
    }
    
    public static void addPropertyToForm(MultivaluedMap<String, Object> map, String name, Object value) {
        if (!"".equals(name)) {
            map.add(name, value);
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
    
    public static String readBody(InputStream is) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copy(is, bos, 1024);
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }
    
    public static void populateMapFromString(MultivaluedMap<String, String> params, 
                                             String postBody, boolean decode,
                                             HttpServletRequest request) {
        if (!StringUtils.isEmpty(postBody)) {
            List<String> parts = Arrays.asList(postBody.split("&"));
            for (String part : parts) {
                String[] keyValue = part.split("=");
                // Change to add blank string if key but not value is specified
                if (keyValue.length == 2) {
                    if (decode) {
                        params.add(keyValue[0], 
                            HttpUtils.urlDecode(keyValue[1]));
                    } else {
                        params.add(keyValue[0], keyValue[1]);
                    }
                } else {
                    params.add(keyValue[0], "");
                }
            }
        } else if (request != null) {
            for (Enumeration en = request.getParameterNames(); en.hasMoreElements();) {
                String paramName = en.nextElement().toString();
                String[] values = request.getParameterValues(paramName);
                params.put(paramName, Arrays.asList(values));
            }
        }
    }
    
    public static void populateMapFromMultipart(MultivaluedMap<String, String> params,
                                                MultipartBody body, 
                                                boolean decode) {
        List<Attachment> atts = body.getAllAttachments();
        for (Attachment a : atts) {
            ContentDisposition cd = a.getContentDisposition();
            if (cd == null || !FORM_DATA_TYPE.equalsIgnoreCase(cd.getType())
                || cd.getParameter("name") == null) {
                throw new WebApplicationException(415);
            }
            String name = cd.getParameter("name").replace("\"", "").replace("'", "");
            try {
                String value = IOUtils.toString(a.getDataHandler().getInputStream());
                params.add(name, decode ? HttpUtils.urlDecode(value) : value);
            } catch (IOException ex) {
                throw new WebApplicationException(415);
            }
        }
    }
}
