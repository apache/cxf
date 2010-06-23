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

package org.apache.cxf.ws.addressing;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.common.WSDLConstants;

/***
 * Only public/static/final fields can be resolved
 * The prefix MUST ends with _PREFIX
 * Namespace MUST starts with NS_
 * The value of the _PREFIX is the suffix of NS_
 
 e.g
    public static final String WSAW_PREFIX = "wsaw";
    public static final String NS_WSAW = "http://www.w3.org/2006/05/addressing/wsdl"; 
***/

public final class NSManager {
    private final Map<String, String> cache = new HashMap<String, String>();


    public NSManager() {
        resolveConstants(JAXWSAConstants.class);
        resolveConstants(WSDLConstants.class);
    }

    private void resolveConstants(final Class clz) {
        for (Field field : clz.getFields()) {
            if (field.getName().endsWith("_PREFIX") && isPulicStaticFinal(field)) {
                try {
                    String prefix = (String) field.get(clz);
                    Field nsField = clz.getField("NS_" + prefix.toUpperCase());
                    if (nsField != null) {
                        cache.put((String)nsField.get(clz), prefix);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    public String getPrefixFromNS(String namespace) {
        return cache.get(namespace);
    }

    private boolean isPulicStaticFinal(final Field field) {
        return field.getModifiers() == (Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
    }

    public Set<String> getNamespaces() {
        return this.cache.keySet();
    }
}