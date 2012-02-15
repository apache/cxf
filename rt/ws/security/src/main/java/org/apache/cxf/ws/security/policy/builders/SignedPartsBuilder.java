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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.Header;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedParts;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;


public class SignedPartsBuilder implements AssertionBuilder<Element> {
    
    public QName[] getKnownElements() {
        return new QName[]{SP11Constants.SIGNED_PARTS, SP12Constants.SIGNED_PARTS};
    }
    
    public Assertion build(Element element, AssertionBuilderFactory factory)
        throws IllegalArgumentException {
        
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        SignedEncryptedParts signedEncryptedParts = new SignedEncryptedParts(true, consts);


        Node nd = element.getFirstChild();
        while (nd != null) {
            if (nd instanceof Element) {
                processElement((Element)nd, signedEncryptedParts);                
            }
            nd = nd.getNextSibling();
        }
        
        //
        // If SignedParts is empty then default to signing the SOAP Body
        //
        if (!signedEncryptedParts.isBody() && !signedEncryptedParts.isAttachments()
            && signedEncryptedParts.getHeaders().isEmpty()) {
            signedEncryptedParts.setBody(true);
        }
        
        return signedEncryptedParts;
    }


    private void processElement(Element element, SignedEncryptedParts parent) {

        if ("Header".equals(element.getLocalName())) {

            String nameAttribute = element.getAttribute(SPConstants.NAME);
            if (nameAttribute == null) {
                nameAttribute = "";
            }
            String namespaceAttribute = element.getAttribute(SPConstants.NAMESPACE);
            if ("".equals(namespaceAttribute)) {
                throw new IllegalArgumentException(
                    "sp:SignedParts/sp:Header@Namespace must have a value"
                );
            }

            parent.addHeader(new Header(nameAttribute, namespaceAttribute));

        } else if ("Body".equals(element.getLocalName())) {
            parent.setBody(true);
        }
    }

}
