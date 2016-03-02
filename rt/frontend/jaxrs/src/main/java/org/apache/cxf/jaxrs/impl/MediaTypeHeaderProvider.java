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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;

public class MediaTypeHeaderProvider implements HeaderDelegate<MediaType> {
    private static final Logger LOG = LogUtils.getL7dLogger(MediaTypeHeaderProvider.class);
    private static final String STRICT_MEDIA_TYPE_CHECK = 
        "org.apache.cxf.jaxrs.mediaTypeCheck.strict";
    private static final Pattern COMPLEX_PARAMETERS = 
        Pattern.compile("(([\\w-]+=\"[^\"]*\")|([\\w-]+=[\\w-/\\+]+))");
    
    public MediaType fromString(String mType) {
        
        return valueOf(mType);
    }

    public static MediaType valueOf(String mType) {
        
        if (mType == null) {
            throw new IllegalArgumentException("Media type value can not be null");
        }
        
        int i = mType.indexOf('/');
        if (i == -1) {
            return handleMediaTypeWithoutSubtype(mType.trim());
        }
        
        int paramsStart = mType.indexOf(';', i + 1);
        int end = paramsStart == -1  ? mType.length() : paramsStart;
        
        String[] parts = mType.substring(0, end).split("/");
        if (parts.length != 2 || StringUtils.isEmpty(parts[0]) || StringUtils.isEmpty(parts[1])) {
            throw new IllegalArgumentException("Can not parse media type string: " + mType);
        }
        
        String type = mType.substring(0, i); 
        String subtype = mType.substring(i + 1, end);
        
        Map<String, String> parameters = Collections.emptyMap();
        if (paramsStart != -1) {

            parameters = new LinkedHashMap<String, String>();
            
            String paramString = mType.substring(paramsStart + 1);
            if (paramString.contains("\"")) {
                Matcher m = COMPLEX_PARAMETERS.matcher(paramString);
                while (m.find()) {
                    String val = m.group().trim();
                    addParameter(parameters, val);
                }
            } else {
                StringTokenizer st = new StringTokenizer(paramString, ";");
                while (st.hasMoreTokens()) {
                    addParameter(parameters, st.nextToken());
                }
            }
        }
        
        return new MediaType(type.trim().toLowerCase(), 
                             subtype.trim().toLowerCase(), 
                             parameters);
    }
    
    private static void addParameter(Map<String, String> parameters, String token) {
        int equalSign = token.indexOf('=');
        if (equalSign == -1) {
            throw new IllegalArgumentException("Wrong media type parameter, separator is missing");
        }
        parameters.put(token.substring(0, equalSign).trim().toLowerCase(), 
                       token.substring(equalSign + 1).trim());
    }
    
    public String toString(MediaType type) {
        return typeToString(type);
    }
    public static String typeToString(MediaType type) {
        return typeToString(type, null);
    }
    // Max number of parameters that may be ignored is 3, at least as known 
    // to the implementation
    public static String typeToString(MediaType type, List<String> ignoreParams) {
        if (type == null) {
            throw new IllegalArgumentException("MediaType parameter is null");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(type.getType()).append('/').append(type.getSubtype());
        
        Map<String, String> params = type.getParameters();
        if (params != null) {
            for (Iterator<Map.Entry<String, String>> iter = params.entrySet().iterator();
                iter.hasNext();) {
                Map.Entry<String, String> entry = iter.next();
                if (ignoreParams != null && ignoreParams.contains(entry.getKey())) {
                    continue;
                }
                sb.append(';').append(entry.getKey()).append('=').append(entry.getValue());
            }
        }
        
        return sb.toString();
    }

    private static MediaType handleMediaTypeWithoutSubtype(String mType) {
        if (mType.startsWith(MediaType.MEDIA_TYPE_WILDCARD)) {
            String mTypeNext = mType.length() == 1 ? "" : mType.substring(1).trim();
            boolean mTypeNextEmpty = StringUtils.isEmpty(mTypeNext);
            if (mTypeNextEmpty || mTypeNext.startsWith(";")) {
                if (!mTypeNextEmpty) {
                    Map<String, String> parameters = new LinkedHashMap<String, String>();
                    StringTokenizer st = new StringTokenizer(mType.substring(2).trim(), ";");
                    while (st.hasMoreTokens()) {
                        addParameter(parameters, st.nextToken());
                    }
                    return new MediaType(MediaType.MEDIA_TYPE_WILDCARD,
                                         MediaType.MEDIA_TYPE_WILDCARD,
                                         parameters);
                } else { 
                    return MediaType.WILDCARD_TYPE;
                }
                
            }
        }
        Message message = PhaseInterceptorChain.getCurrentMessage();
        if (message != null 
            && !MessageUtils.isTrue(message.getContextualProperty(STRICT_MEDIA_TYPE_CHECK))) {
            MediaType mt = null;
            if (mType.equals(MediaType.TEXT_PLAIN_TYPE.getType())) {
                mt = MediaType.TEXT_PLAIN_TYPE;
            } else if (mType.equals(MediaType.APPLICATION_XML_TYPE.getSubtype())) {
                mt = MediaType.APPLICATION_XML_TYPE;
            } else {
                mt = MediaType.WILDCARD_TYPE;
            }
            LOG.fine("Converting a malformed media type '" + mType + "' to '" + typeToString(mt) + "'");
            return mt;
        } else {
            throw new IllegalArgumentException("Media type separator is missing");
        }
    }
}
