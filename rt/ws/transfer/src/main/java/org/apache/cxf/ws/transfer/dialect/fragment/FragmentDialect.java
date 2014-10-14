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
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Element;
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
            System.out.println("EXPRESSION: " + expressionStr);
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
        throw new UnsupportedOperationException();
    }
    
    private JAXBElement<ValueType> generateGetResponse(Object value) {
        if (value instanceof NodeList) {
            System.out.println("NodeList Instance");
            return generateGetResponseNodeList((NodeList) value);
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
}
