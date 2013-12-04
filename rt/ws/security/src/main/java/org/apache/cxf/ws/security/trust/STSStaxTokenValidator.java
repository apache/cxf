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
package org.apache.cxf.ws.security.trust;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.commons.codec.binary.Base64;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;

import org.apache.wss4j.binding.wss10.BinarySecurityTokenType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.message.token.BinarySecurity;
import org.apache.wss4j.dom.message.token.KerberosSecurity;
import org.apache.wss4j.dom.message.token.PKIPathSecurity;
import org.apache.wss4j.dom.message.token.X509Security;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.impl.securityToken.KerberosServiceSecurityTokenImpl;
import org.apache.wss4j.stax.impl.securityToken.SamlSecurityTokenImpl;
import org.apache.wss4j.stax.impl.securityToken.X509PKIPathv1SecurityTokenImpl;
import org.apache.wss4j.stax.impl.securityToken.X509V3SecurityTokenImpl;
import org.apache.wss4j.stax.securityToken.SamlSecurityToken;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.wss4j.stax.validate.BinarySecurityTokenValidator;
import org.apache.wss4j.stax.validate.BinarySecurityTokenValidatorImpl;
import org.apache.wss4j.stax.validate.SamlTokenValidatorImpl;
import org.apache.wss4j.stax.validate.TokenContext;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.securityToken.InboundSecurityToken;

/**
 * A Streaming SAML Token Validator implementation to validate a received Token to a 
 * SecurityTokenService (STS).
 */
public class STSStaxTokenValidator 
    extends SamlTokenValidatorImpl implements BinarySecurityTokenValidator {
    
    private boolean alwaysValidateToSts;
    
    public STSStaxTokenValidator() {
        // 
    }
    
    /**
     * Construct a new instance.
     * @param alwaysValidateToSts whether to always validate the token to the STS
     */
    public STSStaxTokenValidator(boolean alwaysValidateToSts) {
        this.alwaysValidateToSts = alwaysValidateToSts;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T extends SamlSecurityToken & InboundSecurityToken> T validate(
                                                 final SamlAssertionWrapper samlAssertionWrapper,
                                                 final InboundSecurityToken subjectSecurityToken,
                                                 final TokenContext tokenContext) throws WSSecurityException {
        // Check conditions
        checkConditions(samlAssertionWrapper);
        
        // Check OneTimeUse Condition
        checkOneTimeUse(samlAssertionWrapper, 
                        tokenContext.getWssSecurityProperties().getSamlOneTimeUseReplayCache());
        
        // Validate the assertion against schemas/profiles
        validateAssertion(samlAssertionWrapper);

        Crypto sigVerCrypto = null;
        if (samlAssertionWrapper.isSigned()) {
            sigVerCrypto = tokenContext.getWssSecurityProperties().getSignatureVerificationCrypto();
        }
        
        final SoapMessage message = 
            (SoapMessage)tokenContext.getWssSecurityProperties().getMsgContext();
        
        // Validate to STS if required
        boolean valid = false;
        if (alwaysValidateToSts) {
            Element tokenElement = samlAssertionWrapper.getElement();
            validateTokenToSTS(tokenElement, message);
            valid = true;
        }
        final boolean stsValidated = valid;
        
        SamlSecurityTokenImpl securityToken = new SamlSecurityTokenImpl(
                samlAssertionWrapper, subjectSecurityToken,
                tokenContext.getWsSecurityContext(),
                sigVerCrypto,
                WSSecurityTokenConstants.KeyIdentifier_NoKeyInfo,
                tokenContext.getWssSecurityProperties()) {
            
            @Override
            public void verify() throws XMLSecurityException {
                if (stsValidated) {
                    // Already validated
                    return;
                }
                try {
                    super.verify();
                } catch (XMLSecurityException ex) {
                    SamlAssertionWrapper assertion = super.getSamlAssertionWrapper();
                    Element tokenElement = assertion.getElement();
                    validateTokenToSTS(tokenElement, message);
                }
            }
            
        };

        securityToken.setElementPath(tokenContext.getElementPath());
        securityToken.setXMLSecEvent(tokenContext.getFirstXMLSecEvent());

        return (T)securityToken;
    }
    
    @Override
    public InboundSecurityToken validate(final BinarySecurityTokenType binarySecurityTokenType,
                                         final TokenContext tokenContext)
        throws WSSecurityException {
        STSStaxBSTValidator validator = new STSStaxBSTValidator(alwaysValidateToSts);
        return validator.validate(binarySecurityTokenType, tokenContext);
    }
    
    private static void validateTokenToSTS(Element tokenElement, SoapMessage message) 
        throws WSSecurityException {
        SecurityToken token = new SecurityToken();
        token.setToken(tokenElement);
        
        STSClient c = STSUtils.getClient(message, "sts");
        synchronized (c) {
            System.setProperty("noprint", "true");
            try {
                c.validateSecurityToken(token);
            } catch (Exception e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, e);
            }
        }
    }
    
    /**
     * A Streaming SAML Token Validator implementation to validate a BinarySecurityToken to a 
     * SecurityTokenService (STS).
     */
    private static class STSStaxBSTValidator extends BinarySecurityTokenValidatorImpl {
        
        private boolean alwaysValidateToSts;
        
        /**
         * Construct a new instance.
         * @param alwaysValidateToSts whether to always validate the token to the STS
         */
        public STSStaxBSTValidator(boolean alwaysValidateToSts) {
            this.alwaysValidateToSts = alwaysValidateToSts;
        }

        @Override
        public InboundSecurityToken validate(final BinarySecurityTokenType binarySecurityTokenType,
                                             final TokenContext tokenContext)
            throws WSSecurityException {

            //only Base64Encoding is supported
            if (!WSSConstants.SOAPMESSAGE_NS10_BASE64_ENCODING.equals(
                binarySecurityTokenType.getEncodingType())
            ) {
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, "badEncoding",
                        binarySecurityTokenType.getEncodingType());
            }

            final byte[] securityTokenData = Base64.decodeBase64(binarySecurityTokenType.getValue());
            final SoapMessage message = 
                (SoapMessage)tokenContext.getWssSecurityProperties().getMsgContext();
            
            // Validate to STS if required
            boolean valid = false;
            if (alwaysValidateToSts) {
                Element tokenElement = 
                    convertToDOM(binarySecurityTokenType, securityTokenData);
                validateTokenToSTS(tokenElement, message);
                valid = true;
            }
            final boolean stsValidated = valid;
            
            try {
                if (WSSConstants.NS_X509_V3_TYPE.equals(binarySecurityTokenType.getValueType())) {
                    Crypto crypto = getCrypto(tokenContext.getWssSecurityProperties());
                    X509V3SecurityTokenImpl x509V3SecurityToken = new X509V3SecurityTokenImpl(
                            tokenContext.getWsSecurityContext(),
                            crypto,
                            tokenContext.getWssSecurityProperties().getCallbackHandler(),
                            securityTokenData, binarySecurityTokenType.getId(),
                            tokenContext.getWssSecurityProperties()
                    ) {
                        
                        @Override
                        public void verify() throws XMLSecurityException {
                            if (stsValidated) {
                                // Already validated
                                return;
                            }
                            try {
                                super.verify();
                            } catch (XMLSecurityException ex) {
                                Element tokenElement = 
                                    convertToDOM(binarySecurityTokenType, securityTokenData);
                                validateTokenToSTS(tokenElement, message);
                            }
                        }
                    };
                    x509V3SecurityToken.setElementPath(tokenContext.getElementPath());
                    x509V3SecurityToken.setXMLSecEvent(tokenContext.getFirstXMLSecEvent());
                    return x509V3SecurityToken;
                } else if (WSSConstants.NS_X509PKIPathv1.equals(binarySecurityTokenType.getValueType())) {
                    Crypto crypto = getCrypto(tokenContext.getWssSecurityProperties());
                    X509PKIPathv1SecurityTokenImpl x509PKIPathv1SecurityToken = 
                        new X509PKIPathv1SecurityTokenImpl(
                            tokenContext.getWsSecurityContext(),
                            crypto,
                            tokenContext.getWssSecurityProperties().getCallbackHandler(),
                            securityTokenData, binarySecurityTokenType.getId(),
                            WSSecurityTokenConstants.KeyIdentifier_SecurityTokenDirectReference,
                            tokenContext.getWssSecurityProperties()
                        ) {
                            @Override
                            public void verify() throws XMLSecurityException {
                                if (stsValidated) {
                                    // Already validated
                                    return;
                                }
                                try {
                                    super.verify();
                                } catch (XMLSecurityException ex) {
                                    Element tokenElement = 
                                        convertToDOM(binarySecurityTokenType, securityTokenData);
                                    validateTokenToSTS(tokenElement, message);
                                }
                            }
                        };
                    x509PKIPathv1SecurityToken.setElementPath(tokenContext.getElementPath());
                    x509PKIPathv1SecurityToken.setXMLSecEvent(tokenContext.getFirstXMLSecEvent());
                    return x509PKIPathv1SecurityToken;
                } else if (WSSConstants.NS_GSS_Kerberos5_AP_REQ.equals(binarySecurityTokenType.getValueType())) {
                    KerberosServiceSecurityTokenImpl kerberosServiceSecurityToken = 
                        new KerberosServiceSecurityTokenImpl(
                            tokenContext.getWsSecurityContext(),
                            tokenContext.getWssSecurityProperties().getCallbackHandler(),
                            securityTokenData, binarySecurityTokenType.getValueType(),
                            binarySecurityTokenType.getId(),
                            WSSecurityTokenConstants.KeyIdentifier_SecurityTokenDirectReference
                        ) {
                            @Override
                            public void verify() throws XMLSecurityException {
                                if (stsValidated) {
                                    // Already validated
                                    return;
                                }
                                try {
                                    super.verify();
                                } catch (XMLSecurityException ex) {
                                    Element tokenElement = 
                                        convertToDOM(binarySecurityTokenType, securityTokenData);
                                    validateTokenToSTS(tokenElement, message);
                                }
                            }
                        };
                    kerberosServiceSecurityToken.setElementPath(tokenContext.getElementPath());
                    kerberosServiceSecurityToken.setXMLSecEvent(tokenContext.getFirstXMLSecEvent());
                    return kerberosServiceSecurityToken;
                } else {
                    throw new WSSecurityException(
                            WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, "invalidValueType",
                            binarySecurityTokenType.getValueType());
                }
            } catch (XMLSecurityException e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, e);
            }
        }
        
        // Convert to DOM to send the token to the STS
        private Element convertToDOM(
            BinarySecurityTokenType binarySecurityTokenType,
            byte[] securityTokenData
        ) {
            Document doc = DOMUtils.newDocument();
            BinarySecurity binarySecurity = null;
            if (WSSConstants.NS_X509_V3_TYPE.equals(binarySecurityTokenType.getValueType())) {
                binarySecurity = new X509Security(doc);
            } else if (WSSConstants.NS_X509PKIPathv1.equals(binarySecurityTokenType.getValueType())) {
                binarySecurity = new PKIPathSecurity(doc);
            } else if (WSSConstants.NS_GSS_Kerberos5_AP_REQ.equals(binarySecurityTokenType.getValueType())) {
                binarySecurity = new KerberosSecurity(doc);
            }
            
            binarySecurity.addWSSENamespace();
            binarySecurity.addWSUNamespace();
            binarySecurity.setEncodingType(binarySecurityTokenType.getEncodingType());
            binarySecurity.setValueType(binarySecurityTokenType.getValueType());
            binarySecurity.setID(binarySecurityTokenType.getId());
            binarySecurity.setToken(securityTokenData);
            
            return binarySecurity.getElement();
        }
    }
}
