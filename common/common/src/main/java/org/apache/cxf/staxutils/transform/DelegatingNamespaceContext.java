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

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

class DelegatingNamespaceContext implements NamespaceContext {
    private NamespaceContext nc;
    private Map<String, String> nsMap;
    private List<Map<String, String>> namespaces;
    private List<Map<String, String>> prefixes;

    public DelegatingNamespaceContext(NamespaceContext nc, Map<String, String> nsMap) {
        this.nc = nc;
        this.nsMap = nsMap;
        namespaces = new LinkedList<Map<String, String>>();
        prefixes = new LinkedList<Map<String, String>>();
    }
    
    public void down() {
        ((LinkedList<Map<String, String>>)namespaces).addFirst(new HashMap<String, String>(8));
        ((LinkedList<Map<String, String>>)prefixes).addFirst(new HashMap<String, String>(8));
    }

    public void up() {
        ((LinkedList<Map<String, String>>)namespaces).removeFirst();
        ((LinkedList<Map<String, String>>)prefixes).removeFirst();
    }
    
    public void addPrefix(String prefix, String ns) {
        ((LinkedList<Map<String, String>>)namespaces).getFirst().put(prefix, ns);
         ((LinkedList<Map<String, String>>)prefixes).getFirst().put(ns, prefix);
    }
    
    public String findUniquePrefix(String ns) {
        if (ns.length() == 0) {
            return null;
        }
        String existingPrefix = getPrefix(ns);
        if (existingPrefix != null) {
            return existingPrefix;
        }
        
        int i = 0;
        while (true) {
            String prefix = "ps" + ++i;
            if (getNamespaceURI(prefix) == null) {
                addPrefix(prefix, ns);
                return prefix;
            }
        }
    }
    
    public String getNamespaceURI(String prefix) {
        if (!namespaces.isEmpty()) {
            Map<String, String> cache = ((LinkedList<Map<String, String>>)namespaces).getFirst();
            for (Map<String, String> nss : namespaces) {
                String ns = nss.get(prefix);
                if (ns != null) {
                    cache.put(prefix, ns);
                    return ns;
                }
            }
        }
        if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
            return XMLConstants.XML_NS_URI;
        } else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
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
        if (value != null) {
            ns = value;
        }
        
        if (!prefixes.isEmpty()) {
            Map<String, String> cache = ((LinkedList<Map<String, String>>)prefixes).getFirst();
            for (Map<String, String> pfs : prefixes) {
                String prefix = pfs.get(ns);
                if (prefix != null && ns.equals(getNamespaceURI(prefix))) {
                    cache.put(ns, prefix);
                    return prefix;
                }
            }
        }
        if (XMLConstants.XML_NS_URI.equals(ns)) {
            return XMLConstants.XML_NS_PREFIX;
        } else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(ns)) {
            return XMLConstants.XMLNS_ATTRIBUTE;
        }
        
        String prefix = nc.getPrefix(ns);
        if (prefix != null) {
            addPrefix(prefix, ns);
        }
        return prefix;
    }

    public Iterator<String> getPrefixes(String ns) {
        List<String> pl = new LinkedList<String>();
        for (Map<String, String> pfs : prefixes) {
            String pf = pfs.get(ns);
            if (pf != null && ns.equals(getNamespaceURI(pf))) {
                pl.add(pf);
            }
        }
        return pl.iterator();
    }
    
}
