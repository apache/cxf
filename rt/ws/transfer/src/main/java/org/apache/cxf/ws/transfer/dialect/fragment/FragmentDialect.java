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

package org.apache.cxf.ws.transfer.dialect.fragment;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.Delete;
import org.apache.cxf.ws.transfer.Get;
import org.apache.cxf.ws.transfer.Put;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.Dialect;
import org.apache.cxf.ws.transfer.dialect.fragment.faults.InvalidExpression;
import org.apache.cxf.ws.transfer.dialect.fragment.faults.UnsupportedLanguage;
import org.apache.cxf.ws.transfer.shared.TransferTools;
import org.apache.cxf.ws.transfer.shared.faults.UnknownDialect;

/**
 * Implementation of the WS-Fragment dialect.
 * 
 * @author Erich Duda
 */
public class FragmentDialect implements Dialect {
    
    private static Pattern qNamePattern;
    
    public FragmentDialect() {
        if (qNamePattern == null) {
            // See http://www.w3.org/TR/REC-xml-names/#ns-decl
            // prefix name start char
            String prefixNameStartChar = "[A-Z]|_|[a-z]|[\\x{c0}-\\x{d6}]|[\\x{d8}-\\x{f6}]|[\\x{f8}-\\x{2ff}]|"
                                 + "[\\x{370}-\\x{37d}]|[\\x{37f}-\\x{1fff}]|[\\x{200c}-\\x{200d}]|"
                                 + "[\\x{2070}-\\x{218f}]|[\\x{2c00}-\\x{2fef}]|[\\x{3001}-\\x{d7ff}]|"
                                 + "[\\x{f900}-\\x{fdcf}]|[\\x{fdf0}-\\x{fffd}]|[\\x{10000}-\\x{effff}]";
            // xml name start char
            String nameStartChar = ":|" + prefixNameStartChar;
            // prefix continue chars
            String prefixNameChar = "-|\\.|[0-9]|\\x{b7}|[\\x{0300}-\\x{036f}]|[\\x{203f}-\\x{2040}]"
                                  + "|" + prefixNameStartChar;
            // xml name continue chars
            String nameChar = ":|" + prefixNameChar;
            String qName = String.format("^((%s)(%s)*:)?(%s)(%s)*",
                                         prefixNameStartChar, prefixNameChar, nameStartChar, nameChar);
            qNamePattern = Pattern.compile(qName);
        }
    }

    @Override
    public JAXBElement<ValueType> processGet(Get body, Representation representation) {
        for (Object o : body.getAny()) {
            if (o instanceof JAXBElement && ((JAXBElement)o).getValue() instanceof ExpressionType) {
                ExpressionType expression = (ExpressionType) ((JAXBElement)o).getValue();
                if (FragmentDialectConstants.QNAME_LANGUAGE_IRI.equals(expression.getLanguage())) {
                    return generateGetResponse(processGetQName(representation, expression));
                } else if (FragmentDialectConstants.XPATH10_LANGUAGE_IRI.equals(expression.getLanguage())) {
                    return generateGetResponse(processGetXPath10(representation, expression));
                } else {
                    throw new UnsupportedLanguage();
                }
            }
        }
        // TODO: throw SOAP fault
        return null;
    }

    @Override
    public Representation processPut(Put body, Representation representation) {
        throw new UnknownDialect();
    }

    @Override
    public Representation processDelete(Delete body, Representation representation) {
        throw new UnknownDialect();
    }

    @Override
    public Representation processCreate(Create body) {
        throw new UnknownDialect();
    }
    
    private NodeList processGetQName(final Representation representation, ExpressionType expression) {
        try {
            String expressionStr = getQNameXPathFromExpression(expression);
            // Evaluate XPath
            XPath xPath = TransferTools.getXPath();
            xPath.setNamespaceContext(new NamespaceContext() {

                @Override
                public String getNamespaceURI(String prefix) {
                    if (prefix != null && !prefix.isEmpty()) {
                        Element resource = (Element) representation.getAny();
                        return resource.getAttribute("xmlns:" + prefix);
                    } else {
                        return null;
                    }
                }

                @Override
                public String getPrefix(String string) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Iterator getPrefixes(String string) {
                    throw new UnsupportedOperationException();
                }
            });
            return (NodeList) xPath.evaluate(
                    expressionStr, representation.getAny(), XPathConstants.NODESET);
        } catch (XPathExpressionException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private String getQNameXPathFromExpression(ExpressionType expression) {
        if (expression.getContent().size() == 1) {
            String expressionValue = (String) expression.getContent().get(0);
            Matcher m = qNamePattern.matcher(expressionValue);
            if (m.matches()) {
                return "/node()/" + expressionValue;
            } else {
                throw new InvalidExpression();
            }
        } else {
            throw new InvalidExpression();
        }
    }
    
    private Object processGetXPath10(final Representation representation, ExpressionType expression) {
        String expressionStr = getXPathFromExpression(expression);
        // Evaluate XPath
        XPath xPath = TransferTools.getXPath();
        xPath.setNamespaceContext(new NamespaceContext() {

            @Override
            public String getNamespaceURI(String prefix) {
                if (prefix != null && !prefix.isEmpty()) {
                    Element resource = (Element) representation.getAny();
                    return resource.getAttribute("xmlns:" + prefix);
                } else {
                    return null;
                }
            }

            @Override
            public String getPrefix(String string) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator getPrefixes(String string) {
                throw new UnsupportedOperationException();
            }
        });
        try {
            return (Node) xPath.evaluate(
                expressionStr, representation.getAny(), XPathConstants.NODE);
        } catch (XPathException ex) {
            // See https://www.java.net/node/681793
        }

        try {
            return (String) xPath.evaluate(
                expressionStr, representation.getAny(), XPathConstants.STRING);
        } catch (XPathException ex) {
            throw new InvalidExpression();
        }
    }
    
    private String getXPathFromExpression(ExpressionType expression) {
        if (expression.getContent().size() == 1) {
            return (String) expression.getContent().get(0);
        } else {
            throw new InvalidExpression();
        }
    }
    
    private JAXBElement<ValueType> generateGetResponse(Object value) {
        if (value instanceof Node) {
            return generateGetResponseNode((Node) value);
        } else if (value instanceof NodeList) {
            return generateGetResponseNodeList((NodeList) value);
        } else if (value instanceof String) {
            return generateGetResponseString((String) value);
        }
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createValue(new ValueType());
    }
    
    private JAXBElement<ValueType> generateGetResponseNodeList(NodeList nodeList) {
        ValueType resultValue = new ValueType();
        for (int i = 0; i < nodeList.getLength(); i++) {
            resultValue.getContent().add(nodeList.item(i));
        }
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createValue(resultValue);
    }
    
    private JAXBElement<ValueType> generateGetResponseNode(Node node) {
        ValueType resultValue = new ValueType();
        if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
            Element attrNodeEl = TransferTools.createElementNS(
                FragmentDialectConstants.FRAGMENT_2011_03_IRI,
                FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME
            );
            attrNodeEl.setAttribute(
                FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME_ATTR,
                node.getNodeName()
            );
            attrNodeEl.setTextContent(node.getNodeValue());
            resultValue.getContent().add(attrNodeEl);
        } else if (node.getNodeType() == Node.TEXT_NODE) {
            Element textNodeEl = TransferTools.createElementNS(
                FragmentDialectConstants.FRAGMENT_2011_03_IRI,
                FragmentDialectConstants.FRAGMENT_TEXT_NODE_NAME
            );
            textNodeEl.setNodeValue(node.getNodeValue());
            resultValue.getContent().add(textNodeEl);
        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            resultValue.getContent().add(node);
        }
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createValue(resultValue);
    }
    
    private JAXBElement<ValueType> generateGetResponseString(String value) {
        ValueType resultValue = new ValueType();
        resultValue.getContent().add(value);
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createValue(resultValue);
    }

}
