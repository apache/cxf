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

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

public class EntityTagHeaderProvider implements HeaderDelegate<EntityTag> {

    private static final String WEAK_PREFIX = "W/";

    public EntityTag fromString(String header) {


        if (header == null) {
            throw new IllegalArgumentException("ETag value can not be null");
        }

        if ("*".equals(header)) {
            return new EntityTag("*");
        }

        String tag;
        boolean weak = false;
        final String trimmed = header.trim();
        // See please https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag for weak validator 
        if (trimmed.startsWith(WEAK_PREFIX)) {
            weak = true;
            if (trimmed.length() > 2) {
                tag = trimmed.substring(2);
            } else {
                return new EntityTag("", weak);
            }
        }  else {
            tag = trimmed;
        }
        if (tag.length() > 0 && !tag.startsWith("\"") && !tag.endsWith("\"")) {
            return new EntityTag(tag, weak);
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
        String tagValue = tag.getValue();
        if (!tagValue.startsWith("\"")) {
            sb.append('"').append(tagValue).append('"');
        } else {
            sb.append(tagValue);
        }
        return sb.toString();
    }

}
