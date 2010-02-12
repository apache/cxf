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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
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
import javax.xml.namespace.QName;
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
import org.apache.cxf.common.util.PrimitiveUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

/**
 * Utility class for manipulating XML response using XPath and XSLT
 *
 */
public class XMLSource {
    private static final Logger LOG = LogUtils.getL7dLogger(XMLSource.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(XMLSource.class);
    
    private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace"; 
    
    private InputStream stream; 
    private boolean buffering;
    private boolean markFailed;
    
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
        Node node = (Node)evaluate(expression, namespaces, XPathConstants.NODE);
        if (node == null) {
            return null;
        }
        if (cls.isPrimitive() || cls == String.class) {
            return readPrimitiveValue(node, cls);    
        } else {
            return readFromSource(new DOMSource(node), cls);
        }
    }
    
    public <T> T[] getNodes(String expression, Class<T> cls) {
        return getNodes(expression, CastUtils.cast(Collections.emptyMap(), String.class, String.class), cls);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T[] getNodes(String expression, Map<String, String> namespaces, Class<T> cls) {
        
        NodeList nodes = (NodeList)evaluate(expression, namespaces, XPathConstants.NODESET);
        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }
        T[] values = (T[])Array.newInstance(cls, nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (InjectionUtils.isPrimitive(cls)) {
                values[i] = readPrimitiveValue(node, cls);
            } else {
                values[i] = readFromSource(new DOMSource(node), cls);
            }
        }
        return values;
    }

    public URI getLink(String expression) {
        return getLink(expression, CastUtils.cast(Collections.emptyMap(), String.class, String.class));
    }
    
    public URI getLink(String expression, Map<String, String> namespaces) {
        String value = getValue(expression, namespaces);
        return value == null ? null : URI.create(value);
    }
    
    public URI[] getLinks(String expression, Map<String, String> namespaces) {
        String[] values = getValues(expression, namespaces);
        if (values == null) {
            return null;
        }
        URI[] uris = new URI[values.length];
        for (int i = 0; i < values.length; i++) {
            uris[i] = URI.create(values[i]);
        }
        return uris;
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
        return getValue(expression, namespaces, String.class);
    }
    
    public <T> T getValue(String expression, Map<String, String> namespaces, Class<T> cls) {
        Object result = evaluate(expression, namespaces, XPathConstants.STRING);
        return result == null ? null : convertStringToPrimitive(result.toString(), cls); 
    }
    
    
    private Object evaluate(String expression, Map<String, String> namespaces, QName type) {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContextImpl(namespaces));
        try {
            return xpath.evaluate(expression, getSource(), type);
        } catch (XPathExpressionException ex) {
            throw new IllegalArgumentException("Illegal XPath expression '" + expression + "'", ex);
        }
    }
    
    
    public String[] getValues(String expression) {
        return getValues(expression, CastUtils.cast(Collections.emptyMap(), String.class, String.class));
    }
    
    public String[] getValues(String expression, Map<String, String> namespaces) {
        return getNodes(expression, namespaces, String.class);
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
    
    private <T> T readPrimitiveValue(Node node, Class<T> cls) {
        if (String.class == cls) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                XMLUtils.writeTo(new DOMSource(node), bos, 0, "", "yes");
                try {
                    return cls.cast(bos.toString("UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                    // won't happen
                }
            } else {
                return cls.cast(node.getNodeValue());
            }
        } 
        
        return convertStringToPrimitive(node.getNodeValue(), cls);
    }
    
    private <T> T convertStringToPrimitive(String value, Class<T> cls) {
        if (String.class == cls) {
            return cls.cast(value);
        }
        if (cls.isPrimitive()) {
            return cls.cast(PrimitiveUtils.read(value, cls));
        } else {
            try {
                Method m = cls.getMethod("valueOf", new Class[]{String.class});
                return cls.cast(m.invoke(null, value));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
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
            if (!markFailed && buffering) {
                try {
                    stream.reset();
                    stream.mark(stream.available());
                } catch (IOException ex) {
                    markFailed = true;
                    LOG.warning(new org.apache.cxf.common.i18n.Message("NO_SOURCE_MARK", BUNDLE).toString());
                    stream = IOUtils.loadIntoBAIS(stream);
                }
            }
        } catch (IOException ex) {
            LOG.warning(new org.apache.cxf.common.i18n.Message("NO_SOURCE_MARK", BUNDLE).toString());
        }
        return new InputSource(stream);
    }
}
