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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;

public class RequestPreprocessor {
    
    private Map<Object, Object> languageMappings;
    private Map<Object, Object> extensionMappings;
    
    @SuppressWarnings("unchecked")
    public RequestPreprocessor(Map<Object, Object> languageMappings,
                           Map<Object, Object> extensionMappings) {
        this.languageMappings = 
            languageMappings == null ? Collections.EMPTY_MAP : languageMappings;
        this.extensionMappings = 
            extensionMappings == null ? Collections.EMPTY_MAP : extensionMappings;
    }

    public String preprocess(Message m, UriInfo u) {
        handleExtensionMappings(m, u);
        handleLanguageMappings(m, u);
        return new UriInfoImpl(m, null).getPath();
    }
    
    private void handleLanguageMappings(Message m, UriInfo uriInfo) {
        String path = uriInfo.getPath(false);
        for (Map.Entry<?, ?> entry : languageMappings.entrySet()) {
            if (path.endsWith("." + entry.getKey())) {
                updateAcceptLanguageHeader(m, entry.getValue().toString());
                updatePath(m, path, entry.getKey().toString());
                break;
            }    
        }
    }
    
    private void handleExtensionMappings(Message m, UriInfo uriInfo) {
        String path = uriInfo.getPath(false);
        for (Map.Entry<?, ?> entry : extensionMappings.entrySet()) {
            if (path.endsWith("." + entry.getKey().toString())) {
                updateAcceptTypeHeader(m, entry.getValue().toString());
                updatePath(m, path, entry.getKey().toString());
                break;
            }
        }
        
    }
    
    private void updateAcceptTypeHeader(Message m, String anotherValue) {
        m.put(Message.ACCEPT_CONTENT_TYPE, anotherValue);
    }
    
    @SuppressWarnings("unchecked")
    private void updateAcceptLanguageHeader(Message m, String anotherValue) {
        List<String> acceptLanguage =
            ((Map<String, List<String>>)m.get(Message.PROTOCOL_HEADERS)).get("Accept-Language");
        if (acceptLanguage == null) {
            acceptLanguage = new ArrayList<String>(); 
        }
        
        acceptLanguage.add(anotherValue);
        ((Map<String, List<String>>)m.get(Message.PROTOCOL_HEADERS))
            .put("Accept-Language", acceptLanguage);
    }
    
    private void updatePath(Message m, String path, String suffix) {
        String newPath = path.substring(0, path.length() - (suffix.length() + 1));
        HttpUtils.updatePath(m, newPath);
        //m.put(Message.REQUEST_URI, newPath);
    }
    
}
