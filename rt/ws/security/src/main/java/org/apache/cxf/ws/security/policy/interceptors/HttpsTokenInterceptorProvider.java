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

package org.apache.cxf.ws.security.policy.interceptors;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transport.http.URLConnectionInfo;
import org.apache.cxf.transport.http.UntrustedURLConnectionIOException;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxInInterceptor;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.HttpsToken;
import org.apache.wss4j.stax.impl.securityToken.HttpsSecurityTokenImpl;
import org.apache.wss4j.stax.securityEvent.HttpsTokenSecurityEvent;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;

/**
 *
 */
public class HttpsTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {

    private static final Logger LOG = LogUtils.getL7dLogger(HttpsTokenInterceptorProvider.class);

    private static final long serialVersionUID = -13951002554477036L;

    public HttpsTokenInterceptorProvider() {
        super(Arrays.asList(SP11Constants.TRANSPORT_TOKEN, SP12Constants.TRANSPORT_TOKEN,
                            SP11Constants.ISSUED_TOKEN, SP12Constants.ISSUED_TOKEN,
                            SP11Constants.HTTPS_TOKEN, SP12Constants.HTTPS_TOKEN));
        HttpsTokenOutInterceptor outInterceptor = new HttpsTokenOutInterceptor();
        this.getOutInterceptors().add(outInterceptor);
        this.getOutFaultInterceptors().add(outInterceptor);

        HttpsTokenInInterceptor inInterceptor = new HttpsTokenInInterceptor();
        this.getInInterceptors().add(inInterceptor);
        this.getInFaultInterceptors().add(inInterceptor);
    }

    private static Map<String, List<String>> getProtocolHeaders(Message message) {
        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
        if (null == headers) {
            return Collections.emptyMap();
        }
        return headers;
    }

    static class HttpsTokenOutInterceptor extends AbstractPhaseInterceptor<Message> {
        HttpsTokenOutInterceptor() {
            super(Phase.PRE_STREAM);
        }
        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais =
                    PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.HTTPS_TOKEN);
                if (ais.isEmpty()) {
                    return;
                }
                if (isRequestor(message)) {
                    assertHttps(aim, ais, message);
                } else {
                    //server side should be checked on the way in
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }
                }
            }
        }
        private void assertHttps(AssertionInfoMap aim, Collection<AssertionInfo> ais, Message message) {
            for (AssertionInfo ai : ais) {
                HttpsToken token = (HttpsToken)ai.getAssertion();
                String scheme = (String)message.get("http.scheme");
                ai.setAsserted(true);
                Map<String, List<String>> headers = getProtocolHeaders(message);

                if ("https".equals(scheme)) {
                    if (token.getAuthenticationType()
                        == HttpsToken.AuthenticationType.RequireClientCertificate) {
                        boolean disableClientCertCheck =
                            MessageUtils.getContextualBoolean(message,
                                                              SecurityConstants.DISABLE_REQ_CLIENT_CERT_CHECK,
                                                              false);
                        if (!disableClientCertCheck) {
                            final MessageTrustDecider orig = message.get(MessageTrustDecider.class);
                            MessageTrustDecider trust = new MessageTrustDecider() {
                                public void establishTrust(String conduitName,
                                                           URLConnectionInfo connectionInfo,
                                                           Message message)
                                    throws UntrustedURLConnectionIOException {
                                    if (orig != null) {
                                        orig.establishTrust(conduitName, connectionInfo, message);
                                    }
                                    HttpsURLConnectionInfo info = (HttpsURLConnectionInfo)connectionInfo;
                                    if (info.getLocalCertificates() == null
                                        || info.getLocalCertificates().length == 0) {
                                        throw new UntrustedURLConnectionIOException(
                                            "RequireClientCertificate is set, "
                                            + "but no local certificates were negotiated.  Is"
                                            + " the server set to ask for client authorization?");
                                    }
                                }
                            };
                            message.put(MessageTrustDecider.class, trust);
                        }
                        PolicyUtils.assertPolicy(aim, new QName(token.getName().getNamespaceURI(),
                                                                SPConstants.REQUIRE_CLIENT_CERTIFICATE));
                    }
                    if (token.getAuthenticationType() == HttpsToken.AuthenticationType.HttpBasicAuthentication) {
                        List<String> auth = headers.get("Authorization");
                        if (auth == null || auth.isEmpty()
                            || !auth.get(0).startsWith("Basic")) {
                            ai.setNotAsserted("HttpBasicAuthentication is set, but not being used");
                        } else {
                            PolicyUtils.assertPolicy(aim,
                                                     new QName(token.getName().getNamespaceURI(),
                                                               SPConstants.HTTP_BASIC_AUTHENTICATION));
                        }
                    }
                    if (token.getAuthenticationType() == HttpsToken.AuthenticationType.HttpDigestAuthentication) {
                        List<String> auth = headers.get("Authorization");
                        if (auth == null || auth.isEmpty()
                            || !auth.get(0).startsWith("Digest")) {
                            ai.setNotAsserted("HttpDigestAuthentication is set, but not being used");
                        } else {
                            PolicyUtils.assertPolicy(aim,
                                                     new QName(token.getName().getNamespaceURI(),
                                                               SPConstants.HTTP_DIGEST_AUTHENTICATION));
                        }
                    }
                } else {
                    ai.setNotAsserted("Not an HTTPs connection");
                }
                if (!ai.isAsserted()) {
                    throw new PolicyException(ai);
                }
            }
        }

    }

    static class HttpsTokenInInterceptor extends AbstractPhaseInterceptor<Message> {
        HttpsTokenInInterceptor() {
            super(Phase.PRE_STREAM);
            addBefore(WSS4JStaxInInterceptor.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais =
                    PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.HTTPS_TOKEN);
                boolean requestor = isRequestor(message);
                if (ais.isEmpty()) {
                    if (!requestor) {
                        try {
                            assertNonHttpsTransportToken(message);
                        } catch (XMLSecurityException e) {
                            LOG.fine(e.getMessage());
                        }
                    }
                    return;
                }
                if (!requestor) {
                    try {
                        assertHttps(aim, ais, message);
                    } catch (XMLSecurityException e) {
                        LOG.fine(e.getMessage());
                    }
                    // Store the TLS principal on the message context
                    SecurityContext sc = message.get(SecurityContext.class);
                    if (sc == null || sc.getUserPrincipal() == null) {
                        TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
                        if (tlsInfo != null && tlsInfo.getPeerCertificates() != null
                                && tlsInfo.getPeerCertificates().length > 0
                                && tlsInfo.getPeerCertificates()[0] instanceof X509Certificate
                        ) {
                            X509Certificate cert = (X509Certificate)tlsInfo.getPeerCertificates()[0];
                            message.put(
                                SecurityContext.class, createSecurityContext(cert.getSubjectX500Principal())
                            );
                        }
                    }

                } else {
                    //client side should be checked on the way out
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }

                    PolicyUtils.assertPolicy(aim, SPConstants.HTTP_DIGEST_AUTHENTICATION);
                    PolicyUtils.assertPolicy(aim, SPConstants.HTTP_BASIC_AUTHENTICATION);
                    PolicyUtils.assertPolicy(aim, SPConstants.REQUIRE_CLIENT_CERTIFICATE);
                }
            }
        }

        private void assertHttps(
            AssertionInfoMap aim,
            Collection<AssertionInfo> ais,
            Message message
        ) throws XMLSecurityException {
            List<SecurityEvent> securityEvents = getSecurityEventList(message);
            AuthorizationPolicy policy = message.get(AuthorizationPolicy.class);

            for (AssertionInfo ai : ais) {
                boolean asserted = true;
                HttpsToken token = (HttpsToken)ai.getAssertion();

                HttpsTokenSecurityEvent httpsTokenSecurityEvent = new HttpsTokenSecurityEvent();

                Map<String, List<String>> headers = getProtocolHeaders(message);
                if (token.getAuthenticationType() == HttpsToken.AuthenticationType.HttpBasicAuthentication) {
                    List<String> auth = headers.get("Authorization");
                    if (auth == null || auth.isEmpty()
                        || !auth.get(0).startsWith("Basic")) {
                        asserted = false;
                    } else {
                        httpsTokenSecurityEvent.setAuthenticationType(
                            HttpsTokenSecurityEvent.AuthenticationType.HttpBasicAuthentication
                        );
                        HttpsSecurityTokenImpl httpsSecurityToken =
                            new HttpsSecurityTokenImpl(true, policy.getUserName());
                        httpsSecurityToken.addTokenUsage(WSSecurityTokenConstants.TOKENUSAGE_MAIN_SIGNATURE);
                        httpsTokenSecurityEvent.setSecurityToken(httpsSecurityToken);
                        PolicyUtils.assertPolicy(aim,
                                                 new QName(token.getName().getNamespaceURI(),
                                                           SPConstants.HTTP_BASIC_AUTHENTICATION));
                    }
                }
                if (token.getAuthenticationType() == HttpsToken.AuthenticationType.HttpDigestAuthentication) {
                    List<String> auth = headers.get("Authorization");
                    if (auth == null || auth.isEmpty()
                        || !auth.get(0).startsWith("Digest")) {
                        asserted = false;
                    } else {
                        httpsTokenSecurityEvent.setAuthenticationType(
                            HttpsTokenSecurityEvent.AuthenticationType.HttpDigestAuthentication
                        );
                        HttpsSecurityTokenImpl httpsSecurityToken =
                            new HttpsSecurityTokenImpl(false, policy.getUserName());
                        httpsSecurityToken.addTokenUsage(WSSecurityTokenConstants.TOKENUSAGE_MAIN_SIGNATURE);
                        httpsTokenSecurityEvent.setSecurityToken(httpsSecurityToken);
                        PolicyUtils.assertPolicy(aim,
                                                 new QName(token.getName().getNamespaceURI(),
                                                           SPConstants.HTTP_DIGEST_AUTHENTICATION));
                    }
                }

                TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
                if (tlsInfo != null) {
                    if (token.getAuthenticationType()
                        == HttpsToken.AuthenticationType.RequireClientCertificate) {
                        if (tlsInfo.getPeerCertificates() == null
                            || tlsInfo.getPeerCertificates().length == 0) {
                            asserted = false;
                        } else {
                            PolicyUtils.assertPolicy(aim,
                                                     new QName(token.getName().getNamespaceURI(),
                                                               SPConstants.REQUIRE_CLIENT_CERTIFICATE));
                        }
                    }

                    if (tlsInfo.getPeerCertificates() != null && tlsInfo.getPeerCertificates().length > 0) {
                        httpsTokenSecurityEvent.setAuthenticationType(
                            HttpsTokenSecurityEvent.AuthenticationType.HttpsClientCertificateAuthentication
                        );
                        HttpsSecurityTokenImpl httpsSecurityToken =
                            new HttpsSecurityTokenImpl((X509Certificate)tlsInfo.getPeerCertificates()[0]);
                        httpsSecurityToken.addTokenUsage(WSSecurityTokenConstants.TOKENUSAGE_MAIN_SIGNATURE);
                        httpsTokenSecurityEvent.setSecurityToken(httpsSecurityToken);
                    } else if (httpsTokenSecurityEvent.getAuthenticationType() == null) {
                        httpsTokenSecurityEvent.setAuthenticationType(
                            HttpsTokenSecurityEvent.AuthenticationType.HttpsNoAuthentication
                        );
                        HttpsSecurityTokenImpl httpsSecurityToken = new HttpsSecurityTokenImpl();
                        httpsSecurityToken.addTokenUsage(WSSecurityTokenConstants.TOKENUSAGE_MAIN_SIGNATURE);
                        httpsTokenSecurityEvent.setSecurityToken(httpsSecurityToken);
                    }
                } else {
                    asserted = false;
                }

                ai.setAsserted(asserted);

                if (asserted) {
                    securityEvents.add(httpsTokenSecurityEvent);
                }
            }
        }

        // We might have an IssuedToken TransportToken
        private void assertNonHttpsTransportToken(Message message) throws XMLSecurityException {
            TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
            if (tlsInfo != null) {
                HttpsTokenSecurityEvent httpsTokenSecurityEvent = new HttpsTokenSecurityEvent();
                if (tlsInfo.getPeerCertificates() != null && tlsInfo.getPeerCertificates().length > 0) {
                    httpsTokenSecurityEvent.setAuthenticationType(
                        HttpsTokenSecurityEvent.AuthenticationType.HttpsClientCertificateAuthentication
                    );
                    HttpsSecurityTokenImpl httpsSecurityToken =
                        new HttpsSecurityTokenImpl((X509Certificate)tlsInfo.getPeerCertificates()[0]);
                    httpsSecurityToken.addTokenUsage(WSSecurityTokenConstants.TOKENUSAGE_MAIN_SIGNATURE);
                    httpsTokenSecurityEvent.setSecurityToken(httpsSecurityToken);
                } else if (httpsTokenSecurityEvent.getAuthenticationType() == null) {
                    httpsTokenSecurityEvent.setAuthenticationType(
                        HttpsTokenSecurityEvent.AuthenticationType.HttpsNoAuthentication
                    );
                    HttpsSecurityTokenImpl httpsSecurityToken = new HttpsSecurityTokenImpl();
                    httpsSecurityToken.addTokenUsage(WSSecurityTokenConstants.TOKENUSAGE_MAIN_SIGNATURE);
                    httpsTokenSecurityEvent.setSecurityToken(httpsSecurityToken);
                }
                List<SecurityEvent> securityEvents = getSecurityEventList(message);
                securityEvents.add(httpsTokenSecurityEvent);
            }
        }

        private List<SecurityEvent> getSecurityEventList(Message message) {
            @SuppressWarnings("unchecked")
            List<SecurityEvent> securityEvents =
                (List<SecurityEvent>) message.getExchange().get(SecurityEvent.class.getName() + ".out");
            if (securityEvents == null) {
                securityEvents = new ArrayList<>();
                message.getExchange().put(SecurityEvent.class.getName() + ".out", securityEvents);
            }

            return securityEvents;
        }

        private SecurityContext createSecurityContext(final Principal p) {
            return new SecurityContext() {
                public Principal getUserPrincipal() {
                    return p;
                }
                public boolean isUserInRole(String role) {
                    return false;
                }
            };
        }
    }
}
