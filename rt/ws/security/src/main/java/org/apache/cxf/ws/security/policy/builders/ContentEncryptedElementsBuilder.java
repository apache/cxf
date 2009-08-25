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
package org.apache.cxf.ws.security.policy.builders;

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.AssertionBuilder;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.ContentEncryptedElements;


public class ContentEncryptedElementsBuilder implements AssertionBuilder {
    public static final List<QName> KNOWN_ELEMENTS 
        = Collections.singletonList(SP12Constants.CONTENT_ENCRYPTED_ELEMENTS);
    
    public PolicyAssertion build(Element element) {
        
        ContentEncryptedElements contentEncryptedElements 
            = new ContentEncryptedElements(SP12Constants.INSTANCE);
        String attrXPathVersion = DOMUtils.getAttribute(element, SP12Constants.ATTR_XPATH_VERSION);
        
        if (attrXPathVersion != null) {
            contentEncryptedElements.setXPathVersion(attrXPathVersion);
        }
        Node nd = element.getFirstChild();
        while (nd != null) {
            if (nd instanceof Element) {
                processElement((Element)nd, contentEncryptedElements);                
            }
            nd = nd.getNextSibling();
        }
        
        return contentEncryptedElements;
    }
    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }
    private void processElement(Element element, ContentEncryptedElements parent) {
        if (SPConstants.XPATH_EXPR.equals(element.getLocalName())) {
            parent.addXPathExpression(DOMUtils.getRawContent(element));
            addNamespaces(element, parent);
        }
    }
    private void addNamespaces(Node element, ContentEncryptedElements parent) {
        if (element.getParentNode() != null) {
            addNamespaces(element.getParentNode(), parent);
        }
        if (element instanceof Element) {
            Element el = (Element)element;
            NamedNodeMap map = el.getAttributes();
            for (int x = 0; x < map.getLength(); x++) {
                Attr attr = (Attr)map.item(x);
                if ("xmlns".equals(attr.getPrefix())) {
                    parent.addDeclaredNamespaces(attr.getValue(), attr.getLocalName());
                }
            }
        }
    }

    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
