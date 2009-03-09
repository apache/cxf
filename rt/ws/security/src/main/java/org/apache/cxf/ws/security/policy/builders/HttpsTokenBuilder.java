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

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.AssertionBuilder;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.HttpsToken;


/**
 * This is a standard assertion builder implementation for the https token 
 * as specified by the ws security policy 1.2 specification. In order for this builder to be used
 * it is required that the security policy namespace uri is {@link SP12Constants#SP_NS} 
 * The builder will handle
 * <ul>
 *  <li><code>HttpBasicAuthentication</code></li>
 *  <li><code>HttpDigestAuthentication</code></li>
 *  <li><code>RequireClientCertificate</code></li>
 * </ul> 
 * alternatives in the HttpsToken considering both cases whether the policy is normalized or not.
 * 
 */
public class HttpsTokenBuilder implements AssertionBuilder {
    private static final List<QName> KNOWN_ELEMENTS 
        = Arrays.asList(SP11Constants.HTTPS_TOKEN, SP12Constants.HTTPS_TOKEN);

    
    PolicyBuilder builder;
    public HttpsTokenBuilder(PolicyBuilder b) {
        builder = b;
    }
    
    /**
     * {@inheritDoc}
     */
    public PolicyAssertion build(Element element) {
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        
        HttpsToken httpsToken = new HttpsToken(consts);
        httpsToken.setOptional(PolicyConstants.isOptional(element));

        if (consts.getVersion() == SPConstants.Version.SP_V11) {
            String attr = DOMUtils.getAttribute(element,
                                                SPConstants.REQUIRE_CLIENT_CERTIFICATE);
            if (attr != null) {
                httpsToken.setRequireClientCertificate("true".equals(attr));
            }
        } else if (consts.getVersion() == SPConstants.Version.SP_V11) {
            Element polEl = PolicyConstants.findPolicyElement(element);
             
            if (polEl != null) {
                Element child = DOMUtils.getFirstElement(polEl);
                if (child != null) {
                    if (SP12Constants.HTTP_BASIC_AUTHENTICATION.equals(DOMUtils.getElementQName(child))) {
                        httpsToken.setHttpBasicAuthentication(true);
                    } else if (SP12Constants.HTTP_DIGEST_AUTHENTICATION
                            .equals(DOMUtils.getElementQName(child))) {
                        httpsToken.setHttpDigestAuthentication(true);
                    } else if (SP12Constants.REQUIRE_CLIENT_CERTIFICATE
                            .equals(DOMUtils.getElementQName(child))) {
                        httpsToken.setRequireClientCertificate(true);
                    }
                }
            }
        }

        return httpsToken;
    }

    /**
     * {@inheritDoc}
     */
    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }
    

    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }
}
