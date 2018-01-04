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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.fragment.ExpressionType;
import org.apache.cxf.ws.transfer.dialect.fragment.faults.InvalidExpression;

/**
 * Implementation of the QName language.
 */
public class FragmentDialectLanguageQName implements FragmentDialectLanguage {

    private static Pattern qNamePattern;

    public FragmentDialectLanguageQName() {
        if (qNamePattern == null) {
            String qName = getQNamePatternString();
            qNamePattern = Pattern.compile("^" + qName);
        }
    }

    /**
     * Returns regex string, which describes QName format.
     * @return
     */
    public static String getQNamePatternString() {
        // See http://www.w3.org/TR/REC-xml-names/#NT-PrefixedName
        // NCNameStartChar
        // see http://www.w3.org/TR/REC-xml-names/#NT-NCName
        // and http://www.w3.org/TR/REC-xml/#NT-NameStartChar
        String ncNameStartChar = "[A-Z]|_|[a-z]|[\\x{c0}-\\x{d6}]|[\\x{d8}-\\x{f6}]|[\\x{f8}-\\x{2ff}]|"
                + "[\\x{370}-\\x{37d}]|[\\x{37f}-\\x{1fff}]|[\\x{200c}-\\x{200d}]|"
                + "[\\x{2070}-\\x{218f}]|[\\x{2c00}-\\x{2fef}]|[\\x{3001}-\\x{d7ff}]|"
                + "[\\x{f900}-\\x{fdcf}]|[\\x{fdf0}-\\x{fffd}]|[\\x{10000}-\\x{effff}]";
        // NCNameChar
        // see http://www.w3.org/TR/REC-xml/#NT-NameChar
        String ncNameChar = ncNameStartChar
                + "|-|\\.|[0-9]|\\x{b7}|[\\x{0300}-\\x{036f}]|[\\x{203f}-\\x{2040}]";
        // NCName
        // see http://www.w3.org/TR/REC-xml/#NT-Name
        String ncName = String.format("(%s)(%s)*", ncNameStartChar, ncNameChar);
        // QName
        // see http://www.w3.org/TR/REC-xml-names/#NT-QName
        return String.format("((%s):)?(%s)", ncName, ncName);
    }

    @Override
    public Object getResourceFragment(final Representation representation, ExpressionType expression) {
        String expressionStr = getXPathFromQNameExpression(expression);
        // Evaluate XPath
        XPathUtils xu = new XPathUtils(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                if (prefix != null && !prefix.isEmpty()) {
                    Element resource = (Element) representation.getAny();
                    return resource.getAttribute("xmlns:" + prefix);
                }
                return null;
            }

            @Override
            public String getPrefix(String s) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator<String> getPrefixes(String s) {
                throw new UnsupportedOperationException();
            }
        });
        Node resource = (Node) representation.getAny();
        if (resource == null) {
            // Returns empty NodeList
            return new NodeList() {
                @Override
                public Node item(int i) {
                    return null;
                }

                @Override
                public int getLength() {
                    return 0;
                }
            };
        }
        return xu.getValueList(expressionStr, resource);
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
            }
            throw new InvalidExpression();
        }
        throw new InvalidExpression();
    }

}
