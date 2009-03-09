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

import org.w3c.dom.Element;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.AssertionBuilder;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.KeyValueToken;


public class KeyValueTokenBuilder implements AssertionBuilder {
    private static final String MS_NS = "http://schemas.microsoft.com/ws/2005/07/securitypolicy";
    private static final List<QName> KNOWN_ELEMENTS 
        = Arrays.asList(SP12Constants.KEYVALUE_TOKEN,
                        new QName(MS_NS, "RsaToken"));

    public KeyValueTokenBuilder() {
    }
    
    public PolicyAssertion build(Element element) {
        
        SPConstants consts = MS_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        KeyValueToken token = new KeyValueToken(consts);
        token.setOptional(PolicyConstants.isOptional(element));
        
        String attribute = element.getAttributeNS(element.getNamespaceURI(), SPConstants.ATTR_INCLUDE_TOKEN);
        if (StringUtils.isEmpty(attribute)) {
            attribute = element.getAttributeNS(consts.getNamespace(), SPConstants.ATTR_INCLUDE_TOKEN);
        }
        if (StringUtils.isEmpty(attribute)) {
            attribute = element.getAttributeNS(SP11Constants.INSTANCE.getNamespace(),
                                               SPConstants.ATTR_INCLUDE_TOKEN);
        }
        if (!StringUtils.isEmpty(attribute)) {
            token.setInclusion(consts.getInclusionFromAttributeValue(attribute));
        }

        Element polEl = PolicyConstants.findPolicyElement(element);
        if (polEl != null) {
            Element child = DOMUtils.getFirstElement(polEl);
            if (child != null) {
                QName qname = new QName(child.getNamespaceURI(), child.getLocalName());
                if ("RsaKeyValue".equals(qname.getLocalPart())) {
                    token.setForceRsaKeyValue(true);
                }
            }
        }
        return token;
    }

    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }

    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }
}
