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

package org.apache.cxf.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Assert;

/**
 * XPath test assertions.
 */
public final class XPathAssert {
    private XPathAssert() {
    }

    /**
     * Assert that the following XPath query selects one or more nodes.
     *
     * @param xpath
     */
    public static NodeList assertValid(String xpath, Node node, Map<String, String> namespaces)
        throws Exception {
        if (node == null) {
            throw new NullPointerException("Node cannot be null.");
        }

        NodeList nodes = (NodeList)createXPath(namespaces).evaluate(xpath, node, XPathConstants.NODESET);

        if (nodes.getLength() == 0) {
            Assert.fail("Failed to select any nodes for expression:\n" + xpath
                         + " from document:\n" + writeNodeToString(node));
        }

        return nodes;
    }
    /**
     * Assert that the following XPath query selects one or more nodes.
     *
     * @param xpath
     */
    public static void assertValidBoolean(String xpath, Node node, Map<String, String> namespaces)
        throws Exception {
        if (node == null) {
            throw new NullPointerException("Node cannot be null.");
        }

        Boolean b = (Boolean)createXPath(namespaces).evaluate(xpath, node, XPathConstants.BOOLEAN);

        if (b == null) {
            Assert.fail("Failed to select any nodes for expression:\n" + xpath
                        + " from document:\n" + writeNodeToString(node));
        }

        if (!b.booleanValue()) {
            Assert.fail("Boolean XPath assertion evaluated to false:\n"
                        + xpath
                        + " from document:\n" + writeNodeToString(node));
        }
    }

    private static String writeNodeToString(Node node) {
        return StaxUtils.toString(node);
    }

    /**
     * Assert that the following XPath query selects no nodes.
     *
     * @param xpath
     */
    public static NodeList assertInvalid(String xpath, Node node, Map<String, String> namespaces)
        throws Exception {
        if (node == null) {
            throw new NullPointerException("Node cannot be null.");
        }

        NodeList nodes = (NodeList)createXPath(namespaces).evaluate(xpath, node, XPathConstants.NODESET);

        if (nodes.getLength() > 0) {
            String value = writeNodeToString(node);

            Assert.fail("Found multiple nodes for expression:\n" + xpath + "\n" + value);
        }

        return nodes;
    }

    /**
     * Asser that the text of the xpath node retrieved is equal to the value
     * specified.
     *
     * @param xpath
     * @param value
     * @param node
     */
    public static void assertXPathEquals(String xpath,
                                         String value,
                                         Node node,
                                         Map<String, String> namespaces)
        throws Exception {
        Object o = createXPath(namespaces).compile(xpath)
            .evaluate(node, XPathConstants.NODE);
        if (o instanceof Node) {
            Node result = (Node)o;
            String value2 = DOMUtils.getContent(result);
            Assert.assertEquals(value, value2);
            return;
        }
        o = createXPath(namespaces).compile(xpath)
            .evaluate(node, XPathConstants.STRING);
        if (o instanceof String) {
            Assert.assertEquals(value, o);
            return;
        }
        Assert.fail("No nodes were found for expression: "
            + xpath
            + " in document "
            + writeNodeToString(node));
    }

    /**
     * Asser that the text of the xpath node retrieved is equal to the value
     * specified.
     *
     * @param xpath
     * @param value
     * @param node
     */
    public static void assertXPathEquals(String xpath,
                                         QName value,
                                         Node node,
                                         Map<String, String> namespaces)
        throws Exception {
        Object o = createXPath(namespaces).compile(xpath)
            .evaluate(node, XPathConstants.NODE);
        if (o instanceof Node) {
            Node result = (Node)o;
            String value2 = DOMUtils.getContent(result);
            QName q2 = DOMUtils.createQName(value2, result);
            Assert.assertEquals(value, q2);
            return;
        }
        o = createXPath(namespaces).compile(xpath)
            .evaluate(node, XPathConstants.STRING);
        if (o instanceof String) {
            QName q2 = DOMUtils.createQName(o.toString(), node);
            Assert.assertEquals(value, q2);
            return;
        }
        Assert.fail("No nodes were found for expression: "
            + xpath
            + " in document "
            + writeNodeToString(node));
    }

    public static void assertNoFault(Node node) throws Exception {
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("s", "http://schemas.xmlsoap.org/soap/envelope/");
        namespaces.put("s12", "http://www.w3.org/2003/05/soap-envelope");

        assertInvalid("/s:Envelope/s:Body/s:Fault", node, namespaces);
        assertInvalid("/s12:Envelope/s12:Body/s12:Fault", node, namespaces);
    }

    public static void assertFault(Node node) throws Exception {
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("s", "http://schemas.xmlsoap.org/soap/envelope/");
        namespaces.put("s12", "http://www.w3.org/2003/05/soap-envelope");

        assertValid("/s:Envelope/s:Body/s:Fault", node, namespaces);
        assertValid("/s12:Envelope/s12:Body/s12:Fault", node, namespaces);
    }

    /**
     * Create the specified XPath expression with the namespaces added via
     * addNamespace().
     */
    public static XPath createXPath(Map<String, String> namespaces) throws Exception {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        try {
            xpathFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        } catch (javax.xml.xpath.XPathFactoryConfigurationException ex) {
            // ignore
        }
        XPath xpath = xpathFactory.newXPath();

        if (namespaces != null) {
            xpath.setNamespaceContext(new MapNamespaceContext(namespaces));
        }
        return xpath;
    }

    static class MapNamespaceContext implements NamespaceContext {
        private Map<String, String> namespaces;

        MapNamespaceContext(Map<String, String> namespaces) {
            super();
            this.namespaces = namespaces;
        }

        public String getNamespaceURI(String prefix) {
            return namespaces.get(prefix);
        }

        public String getPrefix(String namespaceURI) {
            for (Map.Entry<String, String> e : namespaces.entrySet()) {
                if (e.getValue().equals(namespaceURI)) {
                    return e.getKey();
                }
            }
            return null;
        }

        public Iterator<String> getPrefixes(String namespaceURI) {
            return null;
        }

    }
}
