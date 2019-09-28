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
package org.apache.cxf.ws.transfer.dialect.fragment.language;

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.fragment.ExpressionType;
import org.apache.cxf.ws.transfer.dialect.fragment.faults.InvalidExpression;

/**
 * Implementation of the XPath 1.0 language.
 */
public class FragmentDialectLanguageXPath10 implements FragmentDialectLanguage {

    private static XPathFactory xpathFactory = XPathFactory.newInstance();

    static {
        try {
            xpathFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        } catch (javax.xml.xpath.XPathFactoryConfigurationException ex) {
            // ignore
        }
    }

    @Override
    public Object getResourceFragment(final Representation representation, ExpressionType expression) {
        String expressionStr = getXPathFromExpression(expression);
        // Evaluate XPath
        XPath xPath = xpathFactory.newXPath();
        xPath.setNamespaceContext(new NamespaceContext() {

            @Override
            public String getNamespaceURI(String prefix) {
                if (prefix != null && !prefix.isEmpty()) {
                    Element resource = (Element) representation.getAny();
                    return resource.getAttribute("xmlns:" + prefix);
                }
                return null;
            }

            @Override
            public String getPrefix(String string) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator<String> getPrefixes(String string) {
                throw new UnsupportedOperationException();
            }
        });
        try {
            Object resource = representation.getAny();
            if (resource == null) {
                resource = DOMUtils.createDocument();
            }
            NodeList result = (NodeList) xPath.evaluate(
                expressionStr, resource, XPathConstants.NODESET);
            if (checkResultConstraints(result)) {
                if (result.getLength() == 0) {
                    return null;
                }
                return result;
            }
            return result.item(0);
        } catch (XPathException ex) {
            // See https://www.java.net/node/681793
        }

        try {
            return xPath.evaluate(
                expressionStr, representation.getAny(), XPathConstants.STRING);
        } catch (XPathException ex) {
            throw new InvalidExpression();
        }
    }

    /**
     * Get XPath from the Expression element.
     * @param expression
     * @return
     */
    private String getXPathFromExpression(ExpressionType expression) {
        if (expression.getContent().size() == 1) {
            return (String) expression.getContent().get(0);
        }
        throw new InvalidExpression();
    }

    /**
     * Check if result from evaluation of XPath expression fullfils constraints
     * defined in the specification.
     * See http://www.w3.org/TR/ws-fragment/#IdResSubset
     * @param result
     * @return If the result is true, the server should return all sequence of elements,
     *         otherwise it should return only the first element.
     */
    private boolean checkResultConstraints(NodeList result) {
        if (result.getLength() > 0) {
            Node firstNode = result.item(0);
            if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
                Element firstEl = (Element) firstNode;
                // QName attributes
                String localName = firstEl.getLocalName();
                String namespace = firstEl.getNamespaceURI();
                Node parent = firstEl.getParentNode();
                for (int i = 1; i < result.getLength(); i++) {
                    Node node = result.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        if (!stringEquals(element.getLocalName(), localName)) {
                            return false;
                        }
                        if (!stringEquals(element.getNamespaceURI(), namespace)) {
                            return false;
                        }
                        if (element.getParentNode() != parent) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper method for equation two strings, which can be nullable.
     * @param str1
     * @param str2
     * @return
     */
    private boolean stringEquals(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equals(str2);
    }
}
