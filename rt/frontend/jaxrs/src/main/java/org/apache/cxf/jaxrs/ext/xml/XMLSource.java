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
package org.apache.cxf.jaxrs.ext.xml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

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
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;

/**
 * Utiliity class for manipulating XML response using XPath and XSLT
 *
 */
public class XMLSource {
    private static final Logger LOG = LogUtils.getL7dLogger(XMLSource.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(XMLSource.class);
    
    private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace"; 
    
    private InputStream stream; 
    private boolean buffering;
    
    public XMLSource(InputStream is) {
        stream = is;
    }
    
    public void setBuffering(boolean enable) {
        buffering = enable;
        if (!stream.markSupported()) {
            try {
                stream = IOUtils.loadIntoBAIS(stream);
            } catch (IOException ex) {
                LOG.warning(new org.apache.cxf.common.i18n.Message("NO_SOURCE_MARK", BUNDLE).toString());
            }
        }
    }
    
    public <T> T getNode(String expression, Class<T> cls) {
        return getNode(expression, CastUtils.cast(Collections.emptyMap(), String.class, String.class), cls);
    }
    
    public <T> T getNode(String expression, Map<String, String> namespaces, Class<T> cls) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContextImpl(namespaces));
        try {
            Node node = (Node)xpath.evaluate(expression, getSource(), XPathConstants.NODE);
            if (node == null) {
                return null;
            }
            DOMSource ds = new DOMSource(node);
            return readFromSource(ds, cls);
        } catch (XPathExpressionException ex) {
            throw new IllegalArgumentException("Illegal XPath expression '" + expression + "'", ex);
        }
    }
    
    public <T> T[] getNodes(String expression, Class<T> cls) {
        return getNodes(expression, CastUtils.cast(Collections.emptyMap(), String.class, String.class), cls);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T[] getNodes(String expression, Map<String, String> namespaces, Class<T> cls) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContextImpl(namespaces));
        try {
            NodeList nodes = (NodeList)xpath.evaluate(expression, getSource(), XPathConstants.NODESET);
            if (nodes == null || nodes.getLength() == 0) {
                return null;
            }
            T[] values = (T[])Array.newInstance(cls, nodes.getLength());
 
            for (int i = 0; i < nodes.getLength(); i++) {
                DOMSource ds = new DOMSource(nodes.item(i));
                values[i] = readFromSource(ds, cls);
            }
                  
            return values;
            
        } catch (XPathExpressionException ex) {
            throw new IllegalArgumentException("Illegal XPath expression '" + expression + "'", ex);
        }
    }

    public URI getLink(String expression) {
        return getLink(expression, CastUtils.cast(Collections.emptyMap(), String.class, String.class));
    }
    
    public URI getLink(String expression, Map<String, String> namespaces) {
        String value = getValue(expression, namespaces);
        return value == null ? null : URI.create(value);
    }
    
    public URI getBaseURI() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("xml", XML_NAMESPACE);
        return getLink("/*/@xml:base", map);
    }
    
    public String getValue(String expression) {
        return getValue(expression, CastUtils.cast(Collections.emptyMap(), String.class, String.class));
    }
    
    public String getValue(String expression, Map<String, String> namespaces) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContextImpl(namespaces));
        try {
            return (String)xpath.evaluate(expression, getSource(), XPathConstants.STRING);
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
            JAXBElementProvider provider = new JAXBElementProvider();
            JAXBContext c = provider.getPackageContext(cls);
            if (c == null) {
                c = provider.getClassContext(cls);
            }
            Unmarshaller u = c.createUnmarshaller();
            return cls.cast(u.unmarshal(s));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private InputSource getSource() {
        try {
            if (buffering) {
                stream.reset();
                stream.mark(stream.available());
            }
        } catch (IOException ex) {
            LOG.warning(new org.apache.cxf.common.i18n.Message("NO_SOURCE_MARK", BUNDLE).toString());
        }
        return new InputSource(stream);
    }
}
