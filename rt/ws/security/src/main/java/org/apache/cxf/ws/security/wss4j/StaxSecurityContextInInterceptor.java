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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.interceptor.security.RolePrefixSecurityContextImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.claims.SAMLSecurityContext;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.SAMLTokenPrincipal;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.stax.securityEvent.KerberosTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.KeyValueTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.SamlTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.UsernameTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.wss4j.stax.securityEvent.X509TokenSecurityEvent;
import org.apache.wss4j.stax.securityToken.SubjectAndPrincipalSecurityToken;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants.Event;
import org.apache.xml.security.stax.securityToken.SecurityTokenConstants.TokenUsage;

/**
 * This interceptor handles parsing the StaX WS-Security results (events) + sets up the
 * security context appropriately.
 */
public class StaxSecurityContextInInterceptor extends AbstractPhaseInterceptor<SoapMessage> {

    /**
     * This configuration tag specifies the default attribute name where the roles are present
     * The default is "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role".
     */
    public static final String SAML_ROLE_ATTRIBUTENAME_DEFAULT =
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    public StaxSecurityContextInInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }

    @Override
    public void handleMessage(SoapMessage soapMessage) throws Fault {

        @SuppressWarnings("unchecked")
        final List<SecurityEvent> incomingSecurityEventList =
            (List<SecurityEvent>)soapMessage.get(SecurityEvent.class.getName() + ".in");

        if (incomingSecurityEventList != null) {
            try {
                doResults(soapMessage, incomingSecurityEventList);
            } catch (WSSecurityException e) {
                throw createSoapFault(soapMessage.getVersion(), e);
            }
        }
    }

    private void doResults(SoapMessage msg, List<SecurityEvent> incomingSecurityEventList) throws WSSecurityException {

        // Now go through the results in a certain order to set up a security context. Highest priority is first.

        List<Event> desiredSecurityEvents = new ArrayList<>();
        desiredSecurityEvents.add(WSSecurityEventConstants.SAML_TOKEN);
        desiredSecurityEvents.add(WSSecurityEventConstants.USERNAME_TOKEN);
        desiredSecurityEvents.add(WSSecurityEventConstants.KERBEROS_TOKEN);
        desiredSecurityEvents.add(WSSecurityEventConstants.X509Token);
        desiredSecurityEvents.add(WSSecurityEventConstants.KeyValueToken);

        for (Event desiredEvent : desiredSecurityEvents) {
            SubjectAndPrincipalSecurityToken token = null;
            try {
                token = getSubjectPrincipalToken(incomingSecurityEventList, desiredEvent, msg);
            } catch (XMLSecurityException ex) {
                // proceed
            }
            if (token != null) {
                Principal p = token.getPrincipal();
                Subject subject = token.getSubject();

                if (subject != null) {
                    String roleClassifier =
                        (String)msg.getContextualProperty(SecurityConstants.SUBJECT_ROLE_CLASSIFIER);
                    if (roleClassifier != null && !"".equals(roleClassifier)) {
                        String roleClassifierType =
                            (String)msg.getContextualProperty(SecurityConstants.SUBJECT_ROLE_CLASSIFIER_TYPE);
                        if (roleClassifierType == null || "".equals(roleClassifierType)) {
                            roleClassifierType = "prefix";
                        }
                        msg.put(
                            SecurityContext.class,
                            new RolePrefixSecurityContextImpl(subject, roleClassifier, roleClassifierType)
                        );
                    } else {
                        msg.put(SecurityContext.class, new DefaultSecurityContext(subject));
                    }
                    break;
                } else if (p != null) {

                    if (desiredEvent == WSSecurityEventConstants.SAML_TOKEN) {
                        String roleAttributeName = (String)SecurityUtils.getSecurityPropertyValue(
                                SecurityConstants.SAML_ROLE_ATTRIBUTENAME, msg);
                        if (roleAttributeName == null || roleAttributeName.length() == 0) {
                            roleAttributeName = SAML_ROLE_ATTRIBUTENAME_DEFAULT;
                        }

                        Object receivedAssertion = ((SAMLTokenPrincipal)token.getPrincipal()).getToken();
                        if (receivedAssertion != null) {
                            ClaimCollection claims =
                                SAMLUtils.getClaims((SamlAssertionWrapper)receivedAssertion);
                            Set<Principal> roles =
                                SAMLUtils.parseRolesFromClaims(claims, roleAttributeName, null);

                            SAMLSecurityContext context =
                                new SAMLSecurityContext(p, roles, claims);

                            msg.put(SecurityContext.class, context);
                        }
                    } else {
                        msg.put(SecurityContext.class, createSecurityContext(p));
                    }
                    break;
                }
            }
        }
    }

    private SubjectAndPrincipalSecurityToken getSubjectPrincipalToken(List<SecurityEvent> incomingSecurityEventList,
                                                                      Event desiredEvent,
                                                                      Message msg) throws XMLSecurityException {
        for (SecurityEvent event : incomingSecurityEventList) {
            if (desiredEvent == event.getSecurityEventType()) {
                if (event.getSecurityEventType() == WSSecurityEventConstants.USERNAME_TOKEN
                    && isUsernameTokenEventAllowed((UsernameTokenSecurityEvent)event, msg)) {
                    return ((UsernameTokenSecurityEvent)event).getSecurityToken();
                } else if (event.getSecurityEventType() == WSSecurityEventConstants.SAML_TOKEN
                    && isSamlEventAllowed((SamlTokenSecurityEvent)event, msg)) {
                    return ((SamlTokenSecurityEvent)event).getSecurityToken();
                } else if (event.getSecurityEventType() == WSSecurityEventConstants.X509Token
                    && isUsedForPublicKeySignature(((X509TokenSecurityEvent)event).getSecurityToken())) {
                    return ((X509TokenSecurityEvent)event).getSecurityToken();
                } else if (event.getSecurityEventType() == WSSecurityEventConstants.KeyValueToken
                    && isUsedForPublicKeySignature(((KeyValueTokenSecurityEvent)event).getSecurityToken())) {
                    return ((KeyValueTokenSecurityEvent)event).getSecurityToken();
                } else if (event.getSecurityEventType() == WSSecurityEventConstants.KERBEROS_TOKEN) {
                    return ((KerberosTokenSecurityEvent)event).getSecurityToken();
                }
            }
        }
        return null;
    }

    private boolean isUsedForPublicKeySignature(
        SubjectAndPrincipalSecurityToken token
    ) throws XMLSecurityException {
        if (token == null) {
            return false;
        }

        // Check first of all that the token is used for Signature
        List<TokenUsage> tokenUsages = token.getTokenUsages();
        boolean usedForSignature = false;

        if (tokenUsages != null) {
            for (TokenUsage usage : tokenUsages) {
                if ("MainSignature".equals(usage.getName())) {
                    usedForSignature = true;
                    break;
                }
            }
        }

        if (!usedForSignature) {
            return false;
        }

        // Now check that a PublicKey/X509Certificate was used
        return token.getPublicKey() != null
            || (token.getX509Certificates() != null && token.getX509Certificates().length > 0);
    }

    private boolean isSamlEventAllowed(SamlTokenSecurityEvent event, Message msg) {
        if (event == null) {
            return false;
        }

        boolean allowUnsignedSamlPrincipals =
            SecurityUtils.getSecurityPropertyBoolean(
                SecurityConstants.ENABLE_UNSIGNED_SAML_ASSERTION_PRINCIPAL, msg, false
            );

        // The SAML Assertion must be signed by default
        return event.getSecurityToken() != null
            && event.getSecurityToken().getSamlAssertionWrapper() != null
            && (allowUnsignedSamlPrincipals || event.getSecurityToken().getSamlAssertionWrapper().isSigned());
    }

    private boolean isUsernameTokenEventAllowed(UsernameTokenSecurityEvent event, Message msg) {
        if (event == null) {
            return false;
        }

        boolean allowUTNoPassword =
            SecurityUtils.getSecurityPropertyBoolean(
                SecurityConstants.ENABLE_UT_NOPASSWORD_PRINCIPAL, msg, false
            );

        // The "no password" case is not allowed by default
        return event.getSecurityToken() != null
            && (allowUTNoPassword || event.getSecurityToken().getPassword() != null);
    }

    private SecurityContext createSecurityContext(final Principal p) {
        return new SecurityContext() {

            public Principal getUserPrincipal() {
                return p;
            }

            public boolean isUserInRole(String arg0) {
                return false;
            }
        };
    }

    /**
     * Create a SoapFault from a WSSecurityException, following the SOAP Message Security
     * 1.1 specification, chapter 12 "Error Handling".
     *
     * When the Soap version is 1.1 then set the Fault/Code/Value from the fault code
     * specified in the WSSecurityException (if it exists).
     *
     * Otherwise set the Fault/Code/Value to env:Sender and the Fault/Code/Subcode/Value
     * as the fault code from the WSSecurityException.
     */
    private SoapFault createSoapFault(SoapVersion version, WSSecurityException e) {
        SoapFault fault;
        javax.xml.namespace.QName faultCode = e.getFaultCode();
        if (version.getVersion() == 1.1 && faultCode != null) {
            fault = new SoapFault(e.getMessage(), e, faultCode);
        } else {
            fault = new SoapFault(e.getMessage(), e, version.getSender());
            if (version.getVersion() != 1.1 && faultCode != null) {
                fault.setSubCode(faultCode);
            }
        }
        return fault;
    }
}
