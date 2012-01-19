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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

import org.apache.cxf.helpers.DOMUtils;

/**
 * XPath test assertions.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
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
            throw new AssertionFailedError("Failed to select any nodes for expression:\n" + xpath
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
            throw new AssertionFailedError("Failed to select any nodes for expression:\n" + xpath
                                           + " from document:\n" + writeNodeToString(node));
        }
        
        if (!b.booleanValue()) {
            throw new AssertionFailedError("Boolean XPath assertion evaluated to false:\n"
                                           + xpath
                                           + " from document:\n" + writeNodeToString(node));
        }
    }

    private static String writeNodeToString(Node node) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            DOMUtils.writeXml(node, bos);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
        return bos.toString();
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

            throw new AssertionFailedError("Found multiple nodes for expression:\n" + xpath + "\n" + value);
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
        Node result = (Node)createXPath(namespaces).evaluate(xpath, node, XPathConstants.NODE);
        if (result == null) {
            throw new AssertionFailedError("No nodes were found for expression: " 
                                           + xpath 
                                           + " in document " 
                                           + writeNodeToString(node));
        }

        String value2 = DOMUtils.getContent(result);

        Assert.assertEquals(value, value2);
    }

    public static void assertNoFault(Node node) throws Exception {
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("s", "http://schemas.xmlsoap.org/soap/envelope/");
        namespaces.put("s12", "http://www.w3.org/2003/05/soap-envelope");

        assertInvalid("/s:Envelope/s:Body/s:Fault", node, namespaces);
        assertInvalid("/s12:Envelope/s12:Body/s12:Fault", node, namespaces);
    }

    public static void assertFault(Node node) throws Exception {
        Map<String, String> namespaces = new HashMap<String, String>();
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
        XPath xpath = XPathFactory.newInstance().newXPath();

        if (namespaces != null) {
            xpath.setNamespaceContext(new MapNamespaceContext(namespaces));
        }

        return xpath;
    }

    static class MapNamespaceContext implements NamespaceContext {
        private Map<String, String> namespaces;

        public MapNamespaceContext(Map<String, String> namespaces) {
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

        public Iterator getPrefixes(String namespaceURI) {
            return null;
        }

    }
}
