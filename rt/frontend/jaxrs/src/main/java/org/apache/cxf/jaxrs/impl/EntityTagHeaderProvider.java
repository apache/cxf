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

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

public class EntityTagHeaderProvider implements HeaderDelegate<EntityTag> {

    private static final String WEAK_PREFIX = "W/";
    
    public EntityTag fromString(String header) {
        
        
        if (header == null) {
            throw new IllegalArgumentException("ETag value can not be null");
        }
        
        if ("*".equals(header)) {
            return new EntityTag("*");
        }
        
        String tag = null;
        boolean weak =  false;
        int i = header.indexOf(WEAK_PREFIX);
        if (i != -1) {
            weak = true;
            if (i + 2 < header.length()) {
                tag = header.substring(i + 2);
            } else {
                return new EntityTag("", weak);
            }
        }  else {
            tag = header;
        }
        if (tag.length() < 2 || !tag.startsWith("\"") || !tag.endsWith("\"")) {
            throw new IllegalArgumentException("Misformatted ETag : " + header);
        }
        tag = tag.length() == 2 ? "" : tag.substring(1, tag.length() - 1); 
        return new EntityTag(tag, weak);
    }

    public String toString(EntityTag tag) {
        StringBuilder sb = new StringBuilder();
        if (tag.isWeak()) {
            sb.append(WEAK_PREFIX);
        }
        sb.append("\"").append(tag.getValue()).append("\"");
        return sb.toString();
    }

}
