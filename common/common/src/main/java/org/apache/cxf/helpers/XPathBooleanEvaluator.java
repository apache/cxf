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

package org.apache.cxf.helpers;

import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;

/**
 * Utility class to make the javax.xml.xpath package pass painful.
 */
public class XPathBooleanEvaluator {
    private XPathExpression expression;
    
    /**
     * Construct an evaluator that returns a boolean over a given expression with one namespace prefix.
     * @param prefix
     * @param uri
     * @param expressionString
     * @throws XPathExpressionException
     */
    public XPathBooleanEvaluator(String prefix, String uri, 
                                 String expressionString) throws XPathExpressionException {
        MapNamespaceContext namespaceContext = new MapNamespaceContext();
        namespaceContext.addNamespace(prefix, uri);
        XPath xpath = XPathUtils.getFactory().newXPath();
        xpath.setNamespaceContext(namespaceContext);
        expression = xpath.compile(expressionString);
    }

    /**
     * Construct an evaluator that returns a boolean over a given expression with a map
     * of namespace prefixes.
     * @param mappings
     * @param expressionString
     * @throws XPathExpressionException
     */
    public XPathBooleanEvaluator(Map<String, String> mappings, String expressionString)
        throws XPathExpressionException {
        MapNamespaceContext namespaceContext = new MapNamespaceContext(mappings);
        XPath xpath = XPathUtils.getFactory().newXPath();
        xpath.setNamespaceContext(namespaceContext);
        expression = xpath.compile(expressionString);
    }
    
    /**
     * Evaluate the xpath against a given DOM node.
     * @param context
     * @return
     * @throws XPathExpressionException
     */
    public boolean evaluate(Node context) throws XPathExpressionException {
        return ((Boolean)expression.evaluate(context, XPathConstants.BOOLEAN)).booleanValue();
    }
}
