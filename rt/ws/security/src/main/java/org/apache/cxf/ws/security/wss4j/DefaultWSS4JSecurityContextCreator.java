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
package org.apache.cxf.ws.security.wss4j;

import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.interceptor.security.RolePrefixSecurityContextImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.claims.SAMLSecurityContext;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.token.PKIPathSecurity;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerResult;

/**
 * The default implementation to create a SecurityContext from a set of WSS4J processing results.
 */
public class DefaultWSS4JSecurityContextCreator implements WSS4JSecurityContextCreator {

    private static final List<Integer> DEFAULT_SECURITY_PRIORITIES = new ArrayList<>();
    static {
        DEFAULT_SECURITY_PRIORITIES.add(WSConstants.ST_SIGNED);
        DEFAULT_SECURITY_PRIORITIES.add(WSConstants.ST_UNSIGNED);
        DEFAULT_SECURITY_PRIORITIES.add(WSConstants.UT);
        DEFAULT_SECURITY_PRIORITIES.add(WSConstants.BST);
        DEFAULT_SECURITY_PRIORITIES.add(WSConstants.SIGN);
        DEFAULT_SECURITY_PRIORITIES.add(WSConstants.UT_NOPASSWORD);
    }

    private List<Integer> securityPriorities = new ArrayList<>(DEFAULT_SECURITY_PRIORITIES);

    /**
     * Create a SecurityContext and store it on the SoapMessage parameter
     */
    public void createSecurityContext(SoapMessage msg, WSHandlerResult handlerResult) {

        boolean allowUnsignedSamlPrincipals =
            SecurityUtils.getSecurityPropertyBoolean(
                SecurityConstants.ENABLE_UNSIGNED_SAML_ASSERTION_PRINCIPAL, msg, false
            );
        boolean allowUTNoPassword =
            SecurityUtils.getSecurityPropertyBoolean(
                SecurityConstants.ENABLE_UT_NOPASSWORD_PRINCIPAL, msg, false
            );

        boolean useJAASSubject = true;
        String useJAASSubjectStr =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SC_FROM_JAAS_SUBJECT, msg);
        if (useJAASSubjectStr != null) {
            useJAASSubject = Boolean.parseBoolean(useJAASSubjectStr);
        }

        // Now go through the results in a certain order to set up a security context. Highest priority is first.
        Map<Integer, List<WSSecurityEngineResult>> actionResults = handlerResult.getActionResults();
        for (Integer resultPriority : securityPriorities) {
            if ((resultPriority == WSConstants.ST_UNSIGNED && !allowUnsignedSamlPrincipals)
                || (resultPriority == WSConstants.UT_NOPASSWORD && !allowUTNoPassword)) {
                continue;
            }

            List<WSSecurityEngineResult> foundResults = actionResults.get(resultPriority);
            if (foundResults != null && !foundResults.isEmpty()) {
                for (WSSecurityEngineResult result : foundResults) {

                    if (!skipResult(resultPriority, result)) {
                        SecurityContext context = createSecurityContext(msg, useJAASSubject, result);
                        if (context != null) {
                            msg.put(SecurityContext.class, context);
                            return;
                        }
                    }
                }
            }
        }
    }

    private boolean skipResult(Integer resultPriority, WSSecurityEngineResult result) {
        Object binarySecurity = result.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
        PublicKey publickey =
            (PublicKey)result.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);
        X509Certificate cert =
            (X509Certificate)result.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);

        return resultPriority == WSConstants.BST
            && (binarySecurity instanceof X509Security || binarySecurity instanceof PKIPathSecurity)
            || resultPriority == WSConstants.SIGN && publickey == null && cert == null;
    }

    protected SecurityContext createSecurityContext(
        SoapMessage msg, boolean useJAASSubject, WSSecurityEngineResult wsResult
    ) {
        final Principal p = (Principal)wsResult.get(WSSecurityEngineResult.TAG_PRINCIPAL);
        final Subject subject = (Subject)wsResult.get(WSSecurityEngineResult.TAG_SUBJECT);

        if (subject != null && !(p instanceof KerberosPrincipal) && useJAASSubject) {
            String roleClassifier =
                (String)msg.getContextualProperty(SecurityConstants.SUBJECT_ROLE_CLASSIFIER);
            if (roleClassifier != null && !"".equals(roleClassifier)) {
                String roleClassifierType =
                    (String)msg.getContextualProperty(SecurityConstants.SUBJECT_ROLE_CLASSIFIER_TYPE);
                if (roleClassifierType == null || "".equals(roleClassifierType)) {
                    roleClassifierType = "prefix";
                }
                return new RolePrefixSecurityContextImpl(subject, roleClassifier, roleClassifierType);
            }
            return new DefaultSecurityContext(p, subject);
        } else if (p != null) {
            boolean utWithCallbacks =
                MessageUtils.getContextualBoolean(msg, SecurityConstants.VALIDATE_TOKEN, true);
            if (!utWithCallbacks) {
                WSS4JTokenConverter.convertToken(msg, p);
            }
            Object receivedAssertion = wsResult.get(WSSecurityEngineResult.TAG_TRANSFORMED_TOKEN);
            if (receivedAssertion == null) {
                receivedAssertion = wsResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
            }
            if (wsResult.get(WSSecurityEngineResult.TAG_DELEGATION_CREDENTIAL) != null) {
                msg.put(SecurityConstants.DELEGATED_CREDENTIAL,
                        wsResult.get(WSSecurityEngineResult.TAG_DELEGATION_CREDENTIAL));
            }

            if (receivedAssertion instanceof SamlAssertionWrapper) {
                String roleAttributeName = (String)SecurityUtils.getSecurityPropertyValue(
                        SecurityConstants.SAML_ROLE_ATTRIBUTENAME, msg);
                if (roleAttributeName == null || roleAttributeName.length() == 0) {
                    roleAttributeName = WSS4JInInterceptor.SAML_ROLE_ATTRIBUTENAME_DEFAULT;
                }

                ClaimCollection claims =
                    SAMLUtils.getClaims((SamlAssertionWrapper)receivedAssertion);
                Set<Principal> roles =
                    SAMLUtils.parseRolesFromClaims(claims, roleAttributeName, null);

                SAMLSecurityContext context =
                    new SAMLSecurityContext(p, roles, claims);
                context.setIssuer(SAMLUtils.getIssuer(receivedAssertion));
                context.setAssertionElement(SAMLUtils.getAssertionElement(receivedAssertion));
                return context;
            }
            return createSecurityContext(p);
        }

        return null;
    }

    protected SecurityContext createSecurityContext(final Principal p) {
        return new SecurityContext() {

            public Principal getUserPrincipal() {
                return p;
            }

            public boolean isUserInRole(String arg0) {
                return false;
            }
        };
    }

    public List<Integer> getSecurityPriorities() {
        return securityPriorities;
    }

    public void setSecurityPriorities(List<Integer> securityPriorities) {
        this.securityPriorities = securityPriorities;
    }

}
