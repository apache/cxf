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

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.KeyValueToken;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;

public class KeyValueTokenBuilder implements AssertionBuilder<Element> {
    private static final String MS_NS = "http://schemas.microsoft.com/ws/2005/07/securitypolicy";

    public KeyValueTokenBuilder() {
    }
    
    public Assertion build(Element element, AssertionBuilderFactory factory) {
        
        SPConstants consts = MS_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        KeyValueToken token = new KeyValueToken(consts);
        token.setOptional(PolicyConstants.isOptional(element));
        token.setIgnorable(PolicyConstants.isIgnorable(element));
        
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
        if (polEl == null) {
            throw new IllegalArgumentException(
                "sp:KeyValueToken/wsp:Policy must have a value"
            );
        }
        Element child = DOMUtils.getFirstElement(polEl);
        if (child != null) {
            QName qname = new QName(child.getNamespaceURI(), child.getLocalName());
            if ("RsaKeyValue".equals(qname.getLocalPart())) {
                token.setForceRsaKeyValue(true);
            }
        }
        return token;
    }

    public QName[] getKnownElements() {
        return new QName[]{SP12Constants.KEYVALUE_TOKEN, new QName(MS_NS, "RsaToken")};
    }

}
