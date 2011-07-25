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
package org.apache.cxf.staxutils.transform;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

class DelegatingNamespaceContext implements NamespaceContext {

    private Map<String, String> prefixes = new HashMap<String, String>();
    private NamespaceContext nc;
    private Map<String, String> nsMap;
    
    public DelegatingNamespaceContext(NamespaceContext nc, Map<String, String> nsMap) {
        this.nc = nc;
        this.nsMap = nsMap;
    }
    
    public void addPrefix(String prefix, String namespace) {
        prefixes.put(namespace, prefix);
    }
    
    public String findUniquePrefix(String namespace) {
        if (namespace.length() == 0) {
            return null;
        }
        String existingPrefix = prefixes.get(namespace);
        if (existingPrefix != null) {
            return existingPrefix;
        }
        
        int i = 0;
        while (true) {
            if (!prefixes.containsValue("ps" + ++i)) {
                String prefix = "ps" + i;
                addPrefix(prefix, namespace);
                return prefix;
            }
        }
    }
    
    public String getNamespaceURI(String prefix) {
        for (Map.Entry<String, String> entry : prefixes.entrySet()) {
            if (entry.getValue().equals(prefix)) {
                return entry.getKey();
            }
        }
        String ns = nc.getNamespaceURI(prefix);
        addPrefix(prefix, ns);
        return ns;
    }

    public String getPrefix(String ns) {
        if (ns.length() == 0) {
            return null;
        }
        String value = nsMap.get(ns);
        if (value != null && value.length() == 0) {
            return null;
        }
        
        String actualNs = value == null ? ns : value;
        if (prefixes.containsKey(actualNs)) {
            return prefixes.get(actualNs);
        }
        String prefix = nc.getPrefix(actualNs);
        addPrefix(prefix, actualNs);
        return prefix;
    }

    public Iterator getPrefixes(String ns) {
        return nc.getPrefixes(ns);
    }
    
}
