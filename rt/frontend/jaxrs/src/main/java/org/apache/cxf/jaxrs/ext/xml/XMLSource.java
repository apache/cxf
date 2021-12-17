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

import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * Utility class for manipulating XML response using XPath and XSLT
 *
 */
public class XMLSource {

    private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";

    private InputStream stream;
    private Document doc;

    public XMLSource(InputStream is) {
        stream = is;
    }

    /**
     * Allows for multiple queries against the same stream by buffering to DOM
     */
    public void setBuffering() {
        try {
            doc = StaxUtils.read(stream);
            stream = null;
        } catch (XMLStreamException e) {
            throw new Fault(e); 
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
    @SuppressWarnings("unchecked")
    public <T> T getNode(String expression, Map<String, String> namespaces, Class<T> cls) {
        Object obj = evaluate(expression, namespaces, XPathConstants.NODE);
        if (obj == null) {
            return null;
        }
        if (obj instanceof Node) {
            Node node = (Node)obj;
            if (cls.isPrimitive() || cls == String.class) {
                return (T)readPrimitiveValue(node, cls);
            }
            return readNode(node, cls);
        }
        return cls.cast(evaluate(expression, namespaces, XPathConstants.STRING));
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
                values[i] = (T)readPrimitiveValue(node, cls);
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
        Map<String, String> map = new LinkedHashMap<>();
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
    @SuppressWarnings("unchecked")
    public <T> T getValue(String expression, Map<String, String> namespaces, Class<T> cls) {
        Object result = evaluate(expression, namespaces, XPathConstants.STRING);
        return result == null ? null : (T)InjectionUtils.convertStringToPrimitive(result.toString(), cls);
    }


    private Object evaluate(String expression, Map<String, String> namespaces, QName type) {
        XPathFactory factory = XPathFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        } catch (XPathFactoryConfigurationException e) {
            throw new RuntimeException(e);
        }
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new NamespaceContextImpl(namespaces));
        boolean releaseDoc = false;
        try {
            if (stream != null) {
                //xalan xpath evaluate parses to a DOM via a DocumentBuilderFactory, but doesn't
                //set the SecureProcessing on that. Since a DOM is always created, might as well
                //do it via stax and avoid the service factory performance hits that the
                //DocumentBuilderFactory will entail as well as get the extra security
                //that woodstox provides
                setBuffering();
                releaseDoc = true;
            }
            return xpath.compile(expression).evaluate(doc, type);
        } catch (XPathExpressionException ex) {
            throw new IllegalArgumentException("Illegal XPath expression '" + expression + "'", ex);
        } finally {
            if (releaseDoc) {
                //don't need to maintain the doc
                doc = null;
            }
        }
    }


    private static class NamespaceContextImpl implements NamespaceContext {

        private Map<String, String> namespaces;

        NamespaceContextImpl(Map<String, String> namespaces) {
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

        public Iterator<String> getPrefixes(String namespace) {
            String prefix = namespaces.get(namespace);
            if (prefix == null) {
                return null;
            }
            return Collections.singletonList(prefix).iterator();
        }
    }

    private <T> Object readPrimitiveValue(Node node, Class<T> cls) {
        if (String.class == cls) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return StaxUtils.toString((Element)node);
            }
            return cls.cast(node.getNodeValue());
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

            JAXBElementProvider<?> provider = new JAXBElementProvider<>();
            JAXBContext c = provider.getPackageContext(cls);
            if (c == null) {
                c = provider.getClassContext(cls);
            }
            Unmarshaller u = c.createUnmarshaller();
            try {
                if (cls.getAnnotation(XmlRootElement.class) != null) {
                    return cls.cast(u.unmarshal(s));
                }
                return u.unmarshal(s, cls).getValue();
            } finally {
                JAXBUtils.closeUnmarshaller(u);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
