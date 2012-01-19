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
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
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
    
    /**
     * Allows for multiple queries against the same stream
     * @param enable if set to true then multiple queries will be supported. 
     */
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
    
    /**
     * Find the matching XML node and convert it into an instance of the provided class.
     * The default JAXB MessageBodyReader is currently used in case of non-primitive types.
     * 
     * @param expression XPath expression
     * @param cls class of the node
     * @return the instance representing the matching node
     */
    public <T> T getNode(String expression, Class<T> cls) {
        return getNode(expression, CastUtils.cast(Collections.emptyMap(), String.class, String.class), cls);
    }
    
    /**
     * Find the matching XML node and convert it into an instance of the provided class.
     * The default JAXB MessageBodyReader is currently used in case of non-primitive types.
     * 
     * @param expression XPath expression
     * @param namespaces the namespaces map, prefixes which are used in the XPath expression
     *        are the keys, namespace URIs are the values; note, the prefixes do not have to match
     *        the actual ones used in the XML instance.
     * @param cls class of the node
     * @return the instance representing the matching node
     */
    public <T> T getNode(String expression, Map<String, String> namespaces, Class<T> cls) {
        Node node = (Node)evaluate(expression, namespaces, XPathConstants.NODE);
        if (node == null) {
            return null;
        }
        if (cls.isPrimitive() || cls == String.class) {
            return readPrimitiveValue(node, cls);    
        } else {
            return readNode(node, cls);
        }
    }
    
    /**
     * Find the list of matching XML nodes and convert them into
     * an array of instances of the provided class.
     * The default JAXB MessageBodyReader is currently used  in case of non-primitive types.
     * 
     * @param expression XPath expression
     * @param cls class of the node
     * @return the array of instances representing the matching nodes
     */
    public <T> T[] getNodes(String expression, Class<T> cls) {
        return getNodes(expression, CastUtils.cast(Collections.emptyMap(), String.class, String.class), cls);
    }
    
    /**
     * Find the list of matching XML nodes and convert them into
     * an array of instances of the provided class.
     * The default JAXB MessageBodyReader is currently used  in case of non-primitive types.
     * 
     * @param expression XPath expression
     * @param namespaces the namespaces map, prefixes which are used in the XPath expression
     *        are the keys, namespace URIs are the values; note, the prefixes do not have to match
     *        the actual ones used in the XML instance.
     * @param cls class of the node
     * @return the array of instances representing the matching nodes
     */
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
                values[i] = readNode(node, cls);
            }
        }
        return values;
    }

    /**
     * Find an attribute or text node representing
     * an absolute or relative link and convert it to URI
     * @param expression the XPath expression
     * @return the link
     */
    public URI getLink(String expression) {
        return getLink(expression, CastUtils.cast(Collections.emptyMap(), String.class, String.class));
    }
    
    /**
     * Find an attribute or text node representing
     * an absolute or relative link and convert it to URI
     * @param expression the XPath expression
     * @param namespaces the namespaces map, prefixes which are used in the XPath expression
     *        are the keys, namespace URIs are the values; note, the prefixes do not have to match
     *        the actual ones used in the XML instance.
     * @return the link
     */
    public URI getLink(String expression, Map<String, String> namespaces) {
        String value = getValue(expression, namespaces);
        return value == null ? null : URI.create(value);
    }
    
    /**
     * Find attributes or text nodes representing
     * absolute or relative links and convert them to URIs
     * @param expression the XPath expression
     * @param namespaces the namespaces map, prefixes which are used in the XPath expression
     *        are the keys, namespace URIs are the values; note, the prefixes do not have to match
     *        the actual ones used in the XML instance.
     * @return the links
     */
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
    
    /**
     * Returns the value of the xml:base attribute, if any.
     * This can be used to calculate an absolute URI provided 
     * the links in the actual XML instance are relative. 
     * 
     * @return the xml:base value
     */
    public URI getBaseURI() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("xml", XML_NAMESPACE);
        return getLink("/*/@xml:base", map);
    }
    
    /**
     * Find the attribute or simple/text node
     * @param expression the XPath expression
     * @return the value of the matching node
     */
    public String getValue(String expression) {
        return getValue(expression, CastUtils.cast(Collections.emptyMap(), String.class, String.class));
    }
    
    /**
     * Find the attribute or simple/text node
     * @param expression the XPath expression
     * @param namespaces the namespaces map, prefixes which are used in the XPath expression
     *        are the keys, namespace URIs are the values; note, the prefixes do not have to match
     *        the actual ones used in the XML instance.
     * @return the value of the matching node
     */
    public String getValue(String expression, Map<String, String> namespaces) {
        return getValue(expression, namespaces, String.class);
    }
    
    /**
     * Find the attributes or simple/text nodes
     * @param expression the XPath expression
     * @return the values of the matching nodes
     */
    public String[] getValues(String expression) {
        return getValues(expression, CastUtils.cast(Collections.emptyMap(), String.class, String.class));
    }
    
    /**
     * Find the attributes or simple/text nodes
     * @param expression the XPath expression
     * @param namespaces the namespaces map, prefixes which are used in the XPath expression
     *        are the keys, namespace URIs are the values; note, the prefixes do not have to match
     *        the actual ones used in the XML instance.
     * @return the values of the matching nodes
     */
    public String[] getValues(String expression, Map<String, String> namespaces) {
        return getNodes(expression, namespaces, String.class);
    }
    
    /**
     * Find the attribute or simple/text node and convert the string value to the
     * instance of the provided class, example, Integer.class. 
     * @param expression the XPath expression
     * @param namespaces the namespaces map, prefixes which are used in the XPath expression
     *        are the keys, namespace URIs are the values; note, the prefixes do not have to match
     *        the actual ones used in the XML instance.
     * @param cls the class of the response
     * @return the value
     */
    public <T> T getValue(String expression, Map<String, String> namespaces, Class<T> cls) {
        Object result = evaluate(expression, namespaces, XPathConstants.STRING);
        return result == null ? null : InjectionUtils.convertStringToPrimitive(result.toString(), cls); 
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

        public Iterator<?> getPrefixes(String namespace) {
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
        
        return InjectionUtils.convertStringToPrimitive(node.getNodeValue(), cls);
    }
    
    
    private <T> T readNode(Node node, Class<T> cls) {
        
        if (Node.class.isAssignableFrom(cls)) {
            return cls.cast(node);
        }
        
        DOMSource s = new DOMSource(node);
        if (Source.class == cls || DOMSource.class == cls) {
            return cls.cast(s);
        }
        
        try {
            
            JAXBElementProvider provider = new JAXBElementProvider();
            JAXBContext c = provider.getPackageContext(cls);
            if (c == null) {
                c = provider.getClassContext(cls);
            }
            Unmarshaller u = c.createUnmarshaller();
            if (cls.getAnnotation(XmlRootElement.class) != null) {
                return cls.cast(u.unmarshal(s));
            } else {
                return u.unmarshal(s, cls).getValue();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private InputSource getSource() {
        if (!markFailed && buffering) {
            try {
                stream.reset();
                stream.mark(stream.available());
            } catch (IOException ex) {
                LOG.fine(new org.apache.cxf.common.i18n.Message("NO_SOURCE_MARK", BUNDLE).toString());
                markFailed = true;
                try {
                    stream = IOUtils.loadIntoBAIS(stream);
                } catch (IOException ex2) {
                    throw new RuntimeException(ex2);
                }
            }
        }
        
        return new InputSource(stream);
    }
}
