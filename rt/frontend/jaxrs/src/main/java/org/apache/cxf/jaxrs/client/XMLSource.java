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
package org.apache.cxf.jaxrs.client;

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;

import org.xml.sax.InputSource;

import org.apache.cxf.helpers.CastUtils;

public class XMLSource {
    
    private InputSource source; 
    
    public XMLSource(InputStream is) {
        source = new InputSource(is);
    }
    
    public <T> T getNode(String expression, Class<T> cls) {
        return getNode(expression, CastUtils.cast(Collections.emptyMap(), String.class, String.class), cls);
    }
    
    public <T> T getNode(String expression, Map<String, String> namespaces, Class<T> cls) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContextImpl(namespaces));
        try {
            Node node = (Node)xpath.evaluate(expression, source, XPathConstants.NODE);
            if (node == null) {
                return null;
            }
            DOMSource ds = new DOMSource(node);
            return readFromSource(ds, cls);
        } catch (XPathExpressionException ex) {
            throw new IllegalArgumentException("Illegal XPath expression '" + expression + "'", ex);
        }
    }

    private static class NamespaceContextImpl implements NamespaceContext {
        
        private Map<String, String> namespaces;
        
        public NamespaceContextImpl(Map<String, String> namespaces) {
            this.namespaces = namespaces;    
        }

        public String getNamespaceURI(String prefix) {
            return namespaces.get(prefix);
        }

        public String getPrefix(String namespace) {
            for (Map.Entry<String, String> entry : namespaces.entrySet()) {
                if (entry.getValue().equals(namespace)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        public Iterator getPrefixes(String namespace) {
            String prefix = namespaces.get(namespace);
            if (prefix == null) {
                return null;
            }
            return Collections.singletonList(prefix).iterator();
        }
    }
    
    private <T> T readFromSource(Source s, Class<T> cls) {
        try {
            JAXBContext c = JAXBContext.newInstance(new Class[]{cls});
            Unmarshaller u = c.createUnmarshaller();
            return cls.cast(u.unmarshal(s));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
