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
package org.apache.cxf.systest.sts.x509;

import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.claims.SAMLSecurityContext;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.DefaultWSS4JSecurityContextCreator;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerResult;

/**
 * A custom WSS4JSecurityContextCreator implementation. It creates a SecurityContext using the principal from an
 * asymmetric signature, but populates the roles from a SAML Token.
 */
public class CustomWSS4JSecurityContextCreator extends DefaultWSS4JSecurityContextCreator {

    /**
     * Create a SecurityContext and store it on the SoapMessage parameter
     */
    public void createSecurityContext(SoapMessage msg, WSHandlerResult handlerResult) {
        Map<Integer, List<WSSecurityEngineResult>> actionResults = handlerResult.getActionResults();

        Principal asymmetricPrincipal = null;

        // Get Asymmetric Signature action
        List<WSSecurityEngineResult> foundResults = actionResults.get(WSConstants.SIGN);
        if (foundResults != null && !foundResults.isEmpty()) {
            for (WSSecurityEngineResult result : foundResults) {
                PublicKey publickey =
                    (PublicKey)result.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);
                X509Certificate cert =
                    (X509Certificate)result.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);

                if (publickey == null && cert == null) {
                    continue;
                }
                SecurityContext context = createSecurityContext(msg, true, result);
                if (context != null && context.getUserPrincipal() != null) {
                    asymmetricPrincipal = context.getUserPrincipal();
                    break;
                }
            }
        }

        // We must have an asymmetric principal
        if (asymmetricPrincipal == null) {
            return;
        }

        // Get signed SAML action
        SAMLSecurityContext context = null;
        foundResults = actionResults.get(WSConstants.ST_SIGNED);
        if (foundResults != null && !foundResults.isEmpty()) {
            for (WSSecurityEngineResult result : foundResults) {
                Object receivedAssertion = result.get(WSSecurityEngineResult.TAG_TRANSFORMED_TOKEN);
                if (receivedAssertion == null) {
                    receivedAssertion = result.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                }

                if (receivedAssertion instanceof SamlAssertionWrapper) {
                    String roleAttributeName =
                        (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SAML_ROLE_ATTRIBUTENAME,
                                                                       msg);
                    if (roleAttributeName == null || roleAttributeName.length() == 0) {
                        roleAttributeName = WSS4JInInterceptor.SAML_ROLE_ATTRIBUTENAME_DEFAULT;
                    }

                    ClaimCollection claims =
                        SAMLUtils.getClaims((SamlAssertionWrapper)receivedAssertion);
                    Set<Principal> roles =
                        SAMLUtils.parseRolesFromClaims(claims, roleAttributeName, null);

                    context = new SAMLSecurityContext(asymmetricPrincipal, roles, claims);
                    context.setIssuer(SAMLUtils.getIssuer(receivedAssertion));
                    context.setAssertionElement(SAMLUtils.getAssertionElement(receivedAssertion));
                    break;
                }
            }
        }

        if (context != null) {
            msg.put(SecurityContext.class, context);
        }

    }

}
