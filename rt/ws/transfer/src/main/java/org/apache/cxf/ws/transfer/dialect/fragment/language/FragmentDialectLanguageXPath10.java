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
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.fragment.ExpressionType;
import org.apache.cxf.ws.transfer.dialect.fragment.faults.InvalidExpression;
import org.apache.cxf.ws.transfer.shared.TransferTools;

/**
 *
 * @author erich
 */
public class FragmentDialectLanguageXPath10 implements FragmentDialectLanguage {

    @Override
    public Object getResourceFragment(final Representation representation, ExpressionType expression) {
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
            if (representation.getAny() == null) {
                return (Node) xPath.evaluate(
                    expressionStr, TransferTools.createDocument(), XPathConstants.NODE);
            } else {
                return (Node) xPath.evaluate(
                    expressionStr, representation.getAny(), XPathConstants.NODE);
            }
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
}
