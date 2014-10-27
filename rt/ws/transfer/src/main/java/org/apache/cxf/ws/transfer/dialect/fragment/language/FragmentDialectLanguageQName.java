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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.fragment.ExpressionType;
import org.apache.cxf.ws.transfer.dialect.fragment.faults.InvalidExpression;
import org.apache.cxf.ws.transfer.shared.TransferTools;

/**
 * Implementation of the QName language.
 * @author Erich Duda
 */
public class FragmentDialectLanguageQName implements FragmentDialectLanguage {

    private static Pattern qNamePattern;
    
    public FragmentDialectLanguageQName() {
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
    public Object getResourceFragment(final Representation representation, ExpressionType expression) {
        try {
            String expressionStr = getXPathFromQNameExpression(expression);
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
    
    /**
     * Converts expression in QName language to XPath expression.
     * @param expression Expression in QName language.
     * @return Expression in XPath language.
     */
    private String getXPathFromQNameExpression(ExpressionType expression) {
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
    
}
