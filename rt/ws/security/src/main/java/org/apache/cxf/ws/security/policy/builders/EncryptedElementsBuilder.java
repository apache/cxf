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

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.AssertionBuilder;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedElements;


public class EncryptedElementsBuilder implements AssertionBuilder {
    private static final List<QName> KNOWN_ELEMENTS 
        = Arrays.asList(SP11Constants.ENCRYPTED_ELEMENTS, SP12Constants.ENCRYPTED_ELEMENTS);

    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }
     
    public PolicyAssertion build(Element element)
        throws IllegalArgumentException {
        
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        SignedEncryptedElements signedEncryptedElements = new SignedEncryptedElements(false,
                                                                                      consts);

        String attribute = element.getAttributeNS(consts.getNamespace(), SPConstants.XPATH_VERSION);
        if (attribute != null) {
            signedEncryptedElements.setXPathVersion(attribute);
        }

        Node nd = element.getFirstChild();
        while (nd != null) {
            if (nd instanceof Element) {
                processElement((Element)nd, signedEncryptedElements);                
            }
            nd = nd.getNextSibling();
        }
        return signedEncryptedElements;
    }


    private void processElement(Element element, SignedEncryptedElements parent) {
        if (SPConstants.XPATH_EXPR.equals(element.getLocalName())) {
            parent.addXPathExpression(DOMUtils.getRawContent(element));
            addNamespaces(element, parent);
        }
    }
    private void addNamespaces(Node element, SignedEncryptedElements parent) {
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
