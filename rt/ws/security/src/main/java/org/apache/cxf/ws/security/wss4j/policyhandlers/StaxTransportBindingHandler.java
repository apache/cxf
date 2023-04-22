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

package org.apache.cxf.ws.security.wss4j.policyhandlers;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import jakarta.xml.soap.SOAPException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.cxf.ws.security.wss4j.TokenStoreCallbackHandler;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractToken.DerivedKeys;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.Header;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.KerberosToken;
import org.apache.wss4j.policy.model.KeyValueToken;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.wss4j.policy.model.SecureConversationToken;
import org.apache.wss4j.policy.model.SecurityContextToken;
import org.apache.wss4j.policy.model.SignedElements;
import org.apache.wss4j.policy.model.SignedParts;
import org.apache.wss4j.policy.model.SpnegoContextToken;
import org.apache.wss4j.policy.model.SupportingTokens;
import org.apache.wss4j.policy.model.TransportBinding;
import org.apache.wss4j.policy.model.TransportToken;
import org.apache.wss4j.policy.model.UsernameToken;
import org.apache.wss4j.policy.model.X509Token;
import org.apache.wss4j.policy.model.XPath;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.xml.security.stax.ext.OutboundSecurityContext;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.SecurePart.Modifier;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;

/**
 *
 */
public class StaxTransportBindingHandler extends AbstractStaxBindingHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(StaxTransportBindingHandler.class);
    private TransportBinding tbinding;

    public StaxTransportBindingHandler(
        WSSSecurityProperties properties,
        SoapMessage msg,
        TransportBinding tbinding,
        OutboundSecurityContext outboundSecurityContext
    ) {
        super(properties, msg, tbinding, outboundSecurityContext);
        this.tbinding = tbinding;
    }

    public void handleBinding() {
        AssertionInfoMap aim = getMessage().get(AssertionInfoMap.class);
        configureTimestamp(aim);

        if (this.isRequestor()) {
            if (tbinding != null) {
                assertPolicy(tbinding.getName());
                String asymSignatureAlgorithm =
                    (String)getMessage().getContextualProperty(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM);
                if (asymSignatureAlgorithm != null && tbinding.getAlgorithmSuite() != null) {
                    tbinding.getAlgorithmSuite().getAlgorithmSuiteType().setAsymmetricSignature(asymSignatureAlgorithm);
                }
                String symSignatureAlgorithm =
                    (String)getMessage().getContextualProperty(SecurityConstants.SYMMETRIC_SIGNATURE_ALGORITHM);
                if (symSignatureAlgorithm != null && tbinding.getAlgorithmSuite() != null) {
                    tbinding.getAlgorithmSuite().getAlgorithmSuiteType().setSymmetricSignature(symSignatureAlgorithm);
                }

                TransportToken token = tbinding.getTransportToken();
                if (token.getToken() instanceof IssuedToken) {
                    try {
                        SecurityToken secToken = getSecurityToken();
                        if (secToken == null) {
                            unassertPolicy(token.getToken(), "No transport token id");
                            return;
                        }
                        addIssuedToken(token.getToken(), secToken, false, false);
                    } catch (TokenStoreException e) {
                        LOG.log(Level.FINE, e.getMessage(), e);
                        throw new Fault(e);
                    }
                }
                assertToken(token.getToken());
                assertTokenWrapper(token);
            }

            try {
                handleNonEndorsingSupportingTokens(aim);
                handleEndorsingSupportingTokens(aim);
            } catch (Exception e) {
                LOG.log(Level.FINE, e.getMessage(), e);
                throw new Fault(e);
            }
        } else {
            try {
                handleNonEndorsingSupportingTokens(aim);
            } catch (Exception e) {
                LOG.log(Level.FINE, e.getMessage(), e);
                throw new Fault(e);
            }
            if (tbinding != null) {
                assertPolicy(tbinding.getName());
                if (tbinding.getTransportToken() != null) {
                    assertTokenWrapper(tbinding.getTransportToken());
                    assertToken(tbinding.getTransportToken().getToken());

                    try {
                        handleEndorsingSupportingTokens(aim);
                    } catch (Exception e) {
                        LOG.log(Level.FINE, e.getMessage(), e);
                        throw new Fault(e);
                    }
                }
            }
            addSignatureConfirmation(null);
        }

        configureLayout(aim);
        if (tbinding != null) {
            assertAlgorithmSuite(tbinding.getAlgorithmSuite());
            assertWSSProperties(tbinding.getName().getNamespaceURI());
            assertTrustProperties(tbinding.getName().getNamespaceURI());
        }
        assertPolicy(SP12Constants.SIGNED_PARTS);
        assertPolicy(SP11Constants.SIGNED_PARTS);
        assertPolicy(SP12Constants.ENCRYPTED_PARTS);
        assertPolicy(SP11Constants.ENCRYPTED_PARTS);

        putCustomTokenAfterSignature();
    }

    /**
     * Handle the non-endorsing supporting tokens
     */
    private void handleNonEndorsingSupportingTokens(AssertionInfoMap aim) throws Exception {
        Collection<AssertionInfo> ais;

        ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SIGNED_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                SupportingTokens sgndSuppTokens = (SupportingTokens)ai.getAssertion();
                if (sgndSuppTokens != null) {
                    addSignedSupportingTokens(sgndSuppTokens);
                }
                ai.setAsserted(true);
            }
        }

        ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                SupportingTokens sgndSuppTokens = (SupportingTokens)ai.getAssertion();
                if (sgndSuppTokens != null) {
                    addSignedSupportingTokens(sgndSuppTokens);
                }
                ai.setAsserted(true);
            }
        }

        ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.ENCRYPTED_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                SupportingTokens encrSuppTokens = (SupportingTokens)ai.getAssertion();
                if (encrSuppTokens != null) {
                    addSignedSupportingTokens(encrSuppTokens);
                }
                ai.setAsserted(true);
            }
        }

        ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                SupportingTokens suppTokens = (SupportingTokens)ai.getAssertion();
                if (suppTokens != null && suppTokens.getTokens() != null
                    && suppTokens.getTokens().size() > 0) {
                    handleSupportingTokens(suppTokens, false, false);
                }
                ai.setAsserted(true);
            }
        }
    }

    private void addSignedSupportingTokens(SupportingTokens sgndSuppTokens)
        throws Exception {
        for (AbstractToken token : sgndSuppTokens.getTokens()) {
            assertToken(token);
            if (token != null && !isTokenRequired(token.getIncludeTokenType())) {
                continue;
            }

            if (token instanceof UsernameToken) {
                addUsernameToken((UsernameToken)token);
            } else if (token instanceof IssuedToken) {
                addIssuedToken(token, getSecurityToken(), false, false);
            } else if (token instanceof KerberosToken) {
                addKerberosToken((KerberosToken)token, false, false, false);
            } else if (token instanceof SamlToken) {
                addSamlToken((SamlToken)token, false, false);
            } else if (token != null) {
                throw new Exception(token.getName() + " is not supported in the streaming code");
            } else {
                throw new Exception("A null token was supplied to the streaming code");
            }
        }
    }

    /**
     * Handle the endorsing supporting tokens
     */
    private void handleEndorsingSupportingTokens(AssertionInfoMap aim) throws Exception {
        Collection<AssertionInfo> ais;

        ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            SupportingTokens sgndSuppTokens = null;
            for (AssertionInfo ai : ais) {
                sgndSuppTokens = (SupportingTokens)ai.getAssertion();
                ai.setAsserted(true);
            }
            if (sgndSuppTokens != null) {
                for (AbstractToken token : sgndSuppTokens.getTokens()) {
                    handleEndorsingToken(token, sgndSuppTokens);
                }
            }
        }

        ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.ENDORSING_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            SupportingTokens endSuppTokens = null;
            for (AssertionInfo ai : ais) {
                endSuppTokens = (SupportingTokens)ai.getAssertion();
                ai.setAsserted(true);
            }

            if (endSuppTokens != null) {
                for (AbstractToken token : endSuppTokens.getTokens()) {
                    handleEndorsingToken(token, endSuppTokens);
                }
            }
        }
        ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            SupportingTokens endSuppTokens = null;
            for (AssertionInfo ai : ais) {
                endSuppTokens = (SupportingTokens)ai.getAssertion();
                ai.setAsserted(true);
            }

            if (endSuppTokens != null) {
                for (AbstractToken token : endSuppTokens.getTokens()) {
                    handleEndorsingToken(token, endSuppTokens);
                }
            }
        }
        ais = PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        if (!ais.isEmpty()) {
            SupportingTokens endSuppTokens = null;
            for (AssertionInfo ai : ais) {
                endSuppTokens = (SupportingTokens)ai.getAssertion();
                ai.setAsserted(true);
            }

            if (endSuppTokens != null) {
                for (AbstractToken token : endSuppTokens.getTokens()) {
                    handleEndorsingToken(token, endSuppTokens);
                }
            }
        }
    }

    private void handleEndorsingToken(
        AbstractToken token, SupportingTokens wrapper
    ) throws Exception {
        assertToken(token);
        if (token != null && !isTokenRequired(token.getIncludeTokenType())) {
            return;
        }

        if (token instanceof IssuedToken) {
            SecurityToken securityToken = getSecurityToken();
            addIssuedToken(token, securityToken, false, true);
            signPartsAndElements(wrapper.getSignedParts(), wrapper.getSignedElements());

            WSSSecurityProperties properties = getProperties();
            if (securityToken != null && securityToken.getSecret() != null) {
                properties.setSignatureAlgorithm(
                    tbinding.getAlgorithmSuite().getAlgorithmSuiteType().getSymmetricSignature());
            } else {
                properties.setSignatureAlgorithm(
                    tbinding.getAlgorithmSuite().getAlgorithmSuiteType().getAsymmetricSignature());
            }
            properties.setSignatureCanonicalizationAlgorithm(tbinding.getAlgorithmSuite().getC14n().getValue());
            AlgorithmSuiteType algType = tbinding.getAlgorithmSuite().getAlgorithmSuiteType();
            properties.setSignatureDigestAlgorithm(algType.getDigest());
        } else if (token instanceof SecureConversationToken
            || token instanceof SecurityContextToken || token instanceof SpnegoContextToken) {
            SecurityToken securityToken = getSecurityToken();
            addIssuedToken(token, securityToken, false, true);

            WSSSecurityProperties properties = getProperties();
            if (securityToken != null) {
                storeSecurityToken(token, securityToken);

                // Set up CallbackHandler which wraps the configured Handler
                TokenStoreCallbackHandler callbackHandler =
                    new TokenStoreCallbackHandler(
                        properties.getCallbackHandler(), TokenStoreUtils.getTokenStore(message)
                    );

                properties.setCallbackHandler(callbackHandler);
            }

            doSignature(token, wrapper);

            properties.setIncludeSignatureToken(true);
            properties.setSignatureAlgorithm(
                tbinding.getAlgorithmSuite().getAlgorithmSuiteType().getSymmetricSignature());
            properties.setSignatureCanonicalizationAlgorithm(
                tbinding.getAlgorithmSuite().getC14n().getValue());
            AlgorithmSuiteType algType = tbinding.getAlgorithmSuite().getAlgorithmSuiteType();
            properties.setSignatureDigestAlgorithm(algType.getDigest());
        } else if (token instanceof X509Token || token instanceof KeyValueToken) {
            doSignature(token, wrapper);
        } else if (token instanceof SamlToken) {
            addSamlToken((SamlToken)token, false, true);
            signPartsAndElements(wrapper.getSignedParts(), wrapper.getSignedElements());

            WSSSecurityProperties properties = getProperties();
            properties.setSignatureAlgorithm(
                       tbinding.getAlgorithmSuite().getAlgorithmSuiteType().getAsymmetricSignature());
            properties.setSignatureCanonicalizationAlgorithm(
                       tbinding.getAlgorithmSuite().getC14n().getValue());
            AlgorithmSuiteType algType = tbinding.getAlgorithmSuite().getAlgorithmSuiteType();
            properties.setSignatureDigestAlgorithm(algType.getDigest());
        } else if (token instanceof UsernameToken) {
            throw new Exception("Endorsing UsernameTokens are not supported in the streaming code");
        } else if (token instanceof KerberosToken) {
            WSSSecurityProperties properties = getProperties();
            properties.addAction(XMLSecurityConstants.SIGNATURE);
            configureSignature(token, false);

            addKerberosToken((KerberosToken)token, false, true, false);
            signPartsAndElements(wrapper.getSignedParts(), wrapper.getSignedElements());

            properties.setSignatureAlgorithm(
                       tbinding.getAlgorithmSuite().getAlgorithmSuiteType().getSymmetricSignature());
            properties.setSignatureCanonicalizationAlgorithm(
                       tbinding.getAlgorithmSuite().getC14n().getValue());
            AlgorithmSuiteType algType = tbinding.getAlgorithmSuite().getAlgorithmSuiteType();
            properties.setSignatureDigestAlgorithm(algType.getDigest());
        }
    }

    private void doSignature(AbstractToken token, SupportingTokens wrapper)
        throws Exception {

        signPartsAndElements(wrapper.getSignedParts(), wrapper.getSignedElements());

        // Action
        WSSSecurityProperties properties = getProperties();
        WSSConstants.Action actionToPerform = XMLSecurityConstants.SIGNATURE;
        if (token.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            actionToPerform = WSSConstants.SIGNATURE_WITH_DERIVED_KEY;
        }
        properties.addAction(actionToPerform);

        configureSignature(token, false);
        if (token.getDerivedKeys() == DerivedKeys.RequireDerivedKeys) {
            properties.setSignatureAlgorithm(
                   tbinding.getAlgorithmSuite().getAlgorithmSuiteType().getSymmetricSignature());
        }
    }

    /**
     * Identifies the portions of the message to be signed/encrypted.
     */
    private void signPartsAndElements(
        SignedParts signedParts,
        SignedElements signedElements
    ) throws SOAPException {
        WSSSecurityProperties properties = getProperties();
        List<SecurePart> signatureParts = properties.getSignatureSecureParts();

        // Add timestamp
        if (timestampAdded) {
            SecurePart part =
                new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), Modifier.Element);
            signatureParts.add(part);
        }

        // Add SignedParts
        if (signedParts != null) {
            if (signedParts.isBody()) {
                SecurePart part =
                    new SecurePart(new QName(WSSConstants.NS_SOAP11, "Body"), Modifier.Element);
                signatureParts.add(part);
            }

            for (Header head : signedParts.getHeaders()) {
                SecurePart part =
                    new SecurePart(new QName(head.getNamespace(), head.getName()), Modifier.Element);
                part.setRequired(false);
                signatureParts.add(part);
            }
        }

        // Handle SignedElements
        if (signedElements != null && signedElements.getXPaths() != null) {
            for (XPath xPath : signedElements.getXPaths()) {
                List<QName> qnames =
                    org.apache.wss4j.policy.stax.PolicyUtils.getElementPath(xPath);
                if (!qnames.isEmpty()) {
                    SecurePart part =
                        new SecurePart(qnames.get(qnames.size() - 1), Modifier.Element);
                    signatureParts.add(part);
                }
            }
        }
    }


}
