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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

import org.apache.cxf.helpers.CastUtils;

class DelegatingNamespaceContext implements NamespaceContext {
    private List<Map<String, String>> prefixes;
    private NamespaceContext nc;
    private Map<String, String> nsMap;
    
    public DelegatingNamespaceContext(NamespaceContext nc, Map<String, String> nsMap) {
        this.nc = nc;
        this.nsMap = nsMap;
        this.prefixes =  new LinkedList<Map<String, String>>();
        this.prefixes.add(new HashMap<String, String>());
    }
    
    public void down() {
        Map<String, String> pm = new HashMap<String, String>();
        if (prefixes.size() > 0) {
            pm.putAll(prefixes.get(0));
        }
        prefixes.add(0, pm);
    }
    
    public void up() {
        prefixes.remove(0);
    }
    
    public void addPrefix(String prefix, String namespace) {
        prefixes.get(0).put(namespace, prefix);
    }
    
    public String findUniquePrefix(String namespace) {
        if (namespace.length() == 0) {
            return null;
        }
        String existingPrefix = prefixes.get(0).get(namespace);
        if (existingPrefix != null) {
            return existingPrefix;
        }
        
        int i = 0;
        while (true) {
            if (!prefixes.get(0).containsValue("ps" + ++i)) {
                String prefix = "ps" + i;
                addPrefix(prefix, namespace);
                return prefix;
            }
        }
    }
    
    public String getNamespaceURI(String prefix) {
        for (Map.Entry<String, String> entry : prefixes.get(0).entrySet()) {
            if (entry.getValue().equals(prefix)) {
                return entry.getKey();
            }
        }
        String ns = nc.getNamespaceURI(prefix);
        if (ns != null && ns.length() > 0) {
            addPrefix(prefix, ns);
        }
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
        if (prefixes.get(0).containsKey(actualNs)) {
            return prefixes.get(0).get(actualNs);
        }
        String prefix = nc.getPrefix(actualNs);
        if (prefix != null) {
            addPrefix(prefix, actualNs);
        }
        return prefix;
    }

    public Iterator<String> getPrefixes(String ns) {
        return CastUtils.cast(nc.getPrefixes(ns));
    }
    
}
