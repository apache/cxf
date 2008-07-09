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
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.AssertionBuilder;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.builder.xml.XmlPrimitiveAssertion;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.HttpsToken;
import org.apache.neethi.Policy;


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
    PolicyBuilder builder;
    public HttpsTokenBuilder(PolicyBuilder b) {
        builder = b;
    }
    
    /**
     * {@inheritDoc}
     */
    public PolicyAssertion build(Element element) {
        HttpsToken httpsToken = new HttpsToken(SP12Constants.INSTANCE);
        
        Policy policy = builder.getPolicy(DOMUtils.getFirstElement(element));
        policy = (Policy) policy.normalize(false);
        
        for (Iterator iterator = policy.getAlternatives(); iterator.hasNext();) {
            processAlternative((List) iterator.next(), httpsToken);
            break; // since there should be only one alternative
        }
        
        return httpsToken;
    }

    /**
     * {@inheritDoc}
     */
    public List<QName> getKnownElements() {
        return Collections.singletonList(SP12Constants.HTTPS_TOKEN);
    }
    
    /**
     * Process policy alternatives inside the HttpsToken element.
     * Essentially this method will search for<br>
     * <ul>
     *  <li><code>HttpBasicAuthentication</code></li>
     *  <li><code>HttpDigestAuthentication</code></li>
     *  <li><code>RequireClientCertificate</code></li>
     * </ul>
     * elements.
     * @param assertions the list of assertions to be searched through.
     * @param parent the https token, that is to be populated with retrieved data.
     */
    private void processAlternative(List assertions, HttpsToken parent) {
        
        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
            XmlPrimitiveAssertion primtive = (XmlPrimitiveAssertion) iterator.next();
            QName qname = primtive.getName();
            
            if (qname != null) {
                if (SP12Constants.HTTP_BASIC_AUTHENTICATION.equals(qname)) {
                    parent.setHttpBasicAuthentication(true);
                } else if (SP12Constants.HTTP_DIGEST_AUTHENTICATION.equals(qname)) {
                    parent.setHttpDigestAuthentication(true);
                } else if (SP12Constants.REQUIRE_CLIENT_CERTIFICATE.equals(qname)) {
                    parent.setRequireClientCertificate(true);
                }
            }
        }
    }

    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }
}
