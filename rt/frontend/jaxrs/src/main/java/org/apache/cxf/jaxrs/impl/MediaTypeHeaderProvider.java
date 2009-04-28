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
import java.util.Map;
import java.util.StringTokenizer;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

public class MediaTypeHeaderProvider implements HeaderDelegate<MediaType> {

    public MediaType fromString(String mType) {
        
        if (mType == null) {
            throw new IllegalArgumentException("Media type value can not be null");
        }
        
        if (mType.equals(MediaType.MEDIA_TYPE_WILDCARD) || mType.startsWith("*;")) {
            return new MediaType("*", "*");
        }
        
        int i = mType.indexOf('/');
        if (i == -1) {
            throw new IllegalArgumentException("Media type separator is missing");
        }
        
        int paramsStart = mType.indexOf(';', i + 1);
        int end = paramsStart == -1  ? mType.length() : paramsStart;
        
        String type = mType.substring(0, i); 
        String subtype = mType.substring(i + 1, end);
        
        Map<String, String> parameters = Collections.emptyMap();
        if (paramsStart != -1) {
            // Using Pattern.compile might be marginally faster ?
            // though it's rare when more than one parameter is provided
            parameters = new LinkedHashMap<String, String>();
            StringTokenizer st = 
                new StringTokenizer(mType.substring(paramsStart + 1), ";");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                int equalSign = token.indexOf('=');
                if (equalSign == -1) {
                    throw new IllegalArgumentException("Wrong media type  parameter, seperator is missing");
                }
                parameters.put(token.substring(0, equalSign).trim().toLowerCase(), 
                               token.substring(equalSign + 1).trim().toLowerCase());
            }
            
        }
        
        return new MediaType(type.trim().toLowerCase(), 
                             subtype.trim().toLowerCase(), 
                             parameters);
    }

    public String toString(MediaType type) {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getType()).append('/').append(type.getSubtype());
        
        Map<String, String> params = type.getParameters();
        if (params != null) {
            for (Iterator<Map.Entry<String, String>> iter = params.entrySet().iterator();
                iter.hasNext();) {
                Map.Entry<String, String> entry = iter.next();
                sb.append(';').append(entry.getKey()).append('=').append(entry.getValue());
            }
        }
        
        return sb.toString();
    }

    
}
