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

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBElement;
import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.binding.wss10.AttributedString;
import org.apache.wss4j.binding.wss10.BinarySecurityTokenType;
import org.apache.wss4j.binding.wss10.EncodedString;
import org.apache.wss4j.binding.wss10.PasswordString;
import org.apache.wss4j.binding.wss10.UsernameTokenType;
import org.apache.wss4j.binding.wsu10.AttributedDateTime;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.token.PKIPathSecurity;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.common.util.AttachmentUtils;
import org.apache.wss4j.common.util.UsernameTokenUtil;
import org.apache.wss4j.dom.message.token.KerberosSecurity;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.impl.securityToken.KerberosServiceSecurityTokenImpl;
import org.apache.wss4j.stax.impl.securityToken.SamlSecurityTokenImpl;
import org.apache.wss4j.stax.impl.securityToken.UsernameSecurityTokenImpl;
import org.apache.wss4j.stax.impl.securityToken.X509PKIPathv1SecurityTokenImpl;
import org.apache.wss4j.stax.impl.securityToken.X509V3SecurityTokenImpl;
import org.apache.wss4j.stax.securityToken.SamlSecurityToken;
import org.apache.wss4j.stax.securityToken.UsernameSecurityToken;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.wss4j.stax.utils.WSSUtils;
import org.apache.wss4j.stax.validate.BinarySecurityTokenValidator;
import org.apache.wss4j.stax.validate.BinarySecurityTokenValidatorImpl;
import org.apache.wss4j.stax.validate.SamlTokenValidatorImpl;
import org.apache.wss4j.stax.validate.TokenContext;
import org.apache.wss4j.stax.validate.UsernameTokenValidator;
import org.apache.xml.security.binding.xop.Include;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityUtils;
import org.apache.xml.security.stax.securityToken.InboundSecurityToken;

/**
 * A Streaming SAML Token Validator implementation to validate a received Token to a
 * SecurityTokenService (STS).
 *
 * TODO Refactor this class a bit better...
 */
public class STSStaxTokenValidator
    extends SamlTokenValidatorImpl implements BinarySecurityTokenValidator, UsernameTokenValidator {

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

    @SuppressWarnings("unchecked")
    @Override
    public <T extends UsernameSecurityToken & InboundSecurityToken> T validate(UsernameTokenType usernameTokenType,
                                                                               TokenContext tokenContext)
        throws WSSecurityException {
        // If the UsernameToken is to be used for key derivation, the (1.1)
        // spec says that it cannot contain a password, and it must contain
        // an Iteration element
        final byte[] salt = XMLSecurityUtils.getQNameType(usernameTokenType.getAny(), WSSConstants.TAG_WSSE11_SALT);
        PasswordString passwordType =
            XMLSecurityUtils.getQNameType(usernameTokenType.getAny(), WSSConstants.TAG_WSSE_PASSWORD);
        final Long iteration =
            XMLSecurityUtils.getQNameType(usernameTokenType.getAny(), WSSConstants.TAG_WSSE11_ITERATION);
        if (salt != null && (passwordType != null || iteration == null)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, "badTokenType01");
        }

        boolean handleCustomPasswordTypes =
            tokenContext.getWssSecurityProperties().getHandleCustomPasswordTypes();
        boolean allowUsernameTokenNoPassword =
            tokenContext.getWssSecurityProperties().isAllowUsernameTokenNoPassword()
                || Boolean.parseBoolean((String)tokenContext.getWsSecurityContext().get(
                    WSSConstants.PROP_ALLOW_USERNAMETOKEN_NOPASSWORD));

        // Check received password type against required type
        WSSConstants.UsernameTokenPasswordType requiredPasswordType =
            tokenContext.getWssSecurityProperties().getUsernameTokenPasswordType();
        if (requiredPasswordType != null) {
            if (passwordType == null || passwordType.getType() == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
            }
            WSSConstants.UsernameTokenPasswordType usernameTokenPasswordType =
                WSSConstants.UsernameTokenPasswordType.getUsernameTokenPasswordType(passwordType.getType());
            if (requiredPasswordType != usernameTokenPasswordType) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
            }
        }

        WSSConstants.UsernameTokenPasswordType usernameTokenPasswordType =
            WSSConstants.UsernameTokenPasswordType.PASSWORD_NONE;
        if (passwordType != null && passwordType.getType() != null) {
            usernameTokenPasswordType =
                WSSConstants.UsernameTokenPasswordType.getUsernameTokenPasswordType(
                    passwordType.getType());
        }

        final AttributedString username = usernameTokenType.getUsername();
        if (username == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN,
                                          "badTokenType01");
        }

        final EncodedString encodedNonce =
                XMLSecurityUtils.getQNameType(usernameTokenType.getAny(),
                                              WSSConstants.TAG_WSSE_NONCE);
        byte[] nonceVal = null;
        if (encodedNonce != null && encodedNonce.getValue() != null) {
            nonceVal = Base64.decodeBase64(encodedNonce.getValue());
        }

        final AttributedDateTime attributedDateTimeCreated =
                XMLSecurityUtils.getQNameType(usernameTokenType.getAny(),
                                              WSSConstants.TAG_WSU_CREATED);

        String created = null;
        if (attributedDateTimeCreated != null) {
            created = attributedDateTimeCreated.getValue();
        }

        // Validate to STS if required
        boolean valid = false;
        final SoapMessage message =
            (SoapMessage)tokenContext.getWssSecurityProperties().getMsgContext();
        if (alwaysValidateToSts) {
            Element tokenElement =
                convertToDOM(username.getValue(), passwordType.getValue(),
                             passwordType.getType(), usernameTokenType.getId());
            validateTokenToSTS(tokenElement, message);
            valid = true;
        }

        if (!valid) {
            try {
                if (usernameTokenPasswordType == WSSConstants.UsernameTokenPasswordType.PASSWORD_DIGEST) {
                    if (encodedNonce == null || attributedDateTimeCreated == null) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN,
                                                      "badTokenType01");
                    }

                    if (!WSSConstants.SOAPMESSAGE_NS10_BASE64_ENCODING.equals(encodedNonce.getEncodingType())) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN,
                                                      "badTokenType01");
                    }

                    verifyDigestPassword(username.getValue(), passwordType, nonceVal, created, tokenContext);
                } else if (usernameTokenPasswordType == WSSConstants.UsernameTokenPasswordType.PASSWORD_TEXT
                        || passwordType != null && passwordType.getValue() != null
                        && usernameTokenPasswordType == WSSConstants.UsernameTokenPasswordType.PASSWORD_NONE) {

                    verifyPlaintextPassword(username.getValue(), passwordType, tokenContext);
                } else if (passwordType != null && passwordType.getValue() != null) {
                    if (!handleCustomPasswordTypes) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
                    }
                    verifyPlaintextPassword(username.getValue(), passwordType, tokenContext);
                } else {
                    if (!allowUsernameTokenNoPassword) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
                    }
                }
            } catch (WSSecurityException ex) {
                Element tokenElement =
                    convertToDOM(username.getValue(), passwordType.getValue(),
                                 passwordType.getType(), usernameTokenType.getId());
                validateTokenToSTS(tokenElement, message);
            }
        }

        final String password;
        if (passwordType != null) {
            password = passwordType.getValue();
        } else if (salt != null) {
            WSPasswordCallback pwCb = new WSPasswordCallback(username.getValue(),
                   WSPasswordCallback.USERNAME_TOKEN);
            try {
                WSSUtils.doPasswordCallback(tokenContext.getWssSecurityProperties().getCallbackHandler(), pwCb);
            } catch (WSSecurityException e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, e);
            }
            password = pwCb.getPassword();
        } else {
            password = null;
        }

        UsernameSecurityTokenImpl usernameSecurityToken = new UsernameSecurityTokenImpl(
                usernameTokenPasswordType, username.getValue(), password, created,
                nonceVal, salt, iteration,
                tokenContext.getWsSecurityContext(), usernameTokenType.getId(),
                WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE);
        usernameSecurityToken.setElementPath(tokenContext.getElementPath());
        usernameSecurityToken.setXMLSecEvent(tokenContext.getFirstXMLSecEvent());

        return (T)usernameSecurityToken;
    }

    /**
     * Verify a UsernameToken containing a password digest.
     */
    private void verifyDigestPassword(
        String username,
        PasswordString passwordType,
        byte[] nonceVal,
        String created,
        TokenContext tokenContext
    ) throws WSSecurityException {
        WSPasswordCallback pwCb = new WSPasswordCallback(username,
                null,
                passwordType.getType(),
                WSPasswordCallback.USERNAME_TOKEN);
        try {
            WSSUtils.doPasswordCallback(tokenContext.getWssSecurityProperties().getCallbackHandler(), pwCb);
        } catch (WSSecurityException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, e);
        }

        if (pwCb.getPassword() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }

        String passDigest = UsernameTokenUtil.doPasswordDigest(nonceVal, created, pwCb.getPassword());
        if (!passwordType.getValue().equals(passDigest)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }
        passwordType.setValue(pwCb.getPassword());
    }

    /**
     * Verify a UsernameToken containing a plaintext password.
     */
    private void verifyPlaintextPassword(
        String username,
        PasswordString passwordType,
        TokenContext tokenContext
    ) throws WSSecurityException {
        WSPasswordCallback pwCb = new WSPasswordCallback(username,
                null,
                passwordType.getType(),
                WSPasswordCallback.USERNAME_TOKEN);
        try {
            WSSUtils.doPasswordCallback(tokenContext.getWssSecurityProperties().getCallbackHandler(), pwCb);
        } catch (WSSecurityException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, e);
        }

        if (pwCb.getPassword() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }

        if (!passwordType.getValue().equals(pwCb.getPassword())) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }
        passwordType.setValue(pwCb.getPassword());
    }

    // Convert to DOM to send the token to the STS - it does not copy Nonce/Created/Iteration
    // values
    private Element convertToDOM(
        String username, String password, String passwordType, String id
    ) {
        Document doc = DOMUtils.getEmptyDocument();

        UsernameToken usernameToken = new UsernameToken(true, doc, passwordType);
        usernameToken.setName(username);
        usernameToken.setPassword(password);
        usernameToken.setID(id);

        usernameToken.addWSSENamespace();
        usernameToken.addWSUNamespace();

        return usernameToken.getElement();
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
        STSStaxBSTValidator(boolean alwaysValidateToSts) {
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
                        new Object[]{binarySecurityTokenType.getEncodingType()});
            }

            final byte[] securityTokenData;
            try {
                securityTokenData =
                    getBinarySecurityTokenBytes(binarySecurityTokenType, tokenContext.getWssSecurityProperties());
            } catch (XMLSecurityException e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, e);
            }
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
                } else if (WSSConstants.NS_X509_PKIPATH_V1.equals(binarySecurityTokenType.getValueType())) {
                    Crypto crypto = getCrypto(tokenContext.getWssSecurityProperties());
                    X509PKIPathv1SecurityTokenImpl x509PKIPathv1SecurityToken =
                        new X509PKIPathv1SecurityTokenImpl(
                            tokenContext.getWsSecurityContext(),
                            crypto,
                            tokenContext.getWssSecurityProperties().getCallbackHandler(),
                            securityTokenData, binarySecurityTokenType.getId(),
                            WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE,
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
                } else if (WSSConstants.NS_GSS_KERBEROS5_AP_REQ.equals(binarySecurityTokenType.getValueType())) {
                    KerberosServiceSecurityTokenImpl kerberosServiceSecurityToken =
                        new KerberosServiceSecurityTokenImpl(
                            tokenContext.getWsSecurityContext(),
                            tokenContext.getWssSecurityProperties().getCallbackHandler(),
                            securityTokenData, binarySecurityTokenType.getValueType(),
                            binarySecurityTokenType.getId(),
                            WSSecurityTokenConstants.KEYIDENTIFIER_SECURITY_TOKEN_DIRECT_REFERENCE
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
                            new Object[]{binarySecurityTokenType.getValueType()});
                }
            } catch (XMLSecurityException e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, e);
            }
        }

        private byte[] getBinarySecurityTokenBytes(BinarySecurityTokenType binarySecurityTokenType,
                                                   WSSSecurityProperties wssSecurityProperties
        ) throws XMLSecurityException {

            StringBuilder sb = new StringBuilder();

            for (Object obj : binarySecurityTokenType.getContent()) {
                if (obj instanceof String) {
                    sb.append((String)obj);
                } else if (obj instanceof JAXBElement<?>) {
                    JAXBElement<?> element = (JAXBElement<?>)obj;
                    if (XMLSecurityConstants.TAG_XOP_INCLUDE.equals(element.getName())) {
                        Include include = (Include)element.getValue();
                        if (include != null && include.getHref() != null && include.getHref().startsWith("cid:")) {
                            CallbackHandler callbackHandler = wssSecurityProperties.getAttachmentCallbackHandler();
                            return AttachmentUtils.getBytesFromAttachment(include.getHref(),
                                                                          callbackHandler,
                                                                          true);
                        }
                    }
                }
            }

            return Base64.decodeBase64(sb.toString());
        }

        // Convert to DOM to send the token to the STS
        private Element convertToDOM(
            BinarySecurityTokenType binarySecurityTokenType,
            byte[] securityTokenData
        ) throws WSSecurityException {
            Document doc = DOMUtils.getEmptyDocument();
            final BinarySecurity binarySecurity;
            if (WSSConstants.NS_X509_V3_TYPE.equals(binarySecurityTokenType.getValueType())) {
                binarySecurity = new X509Security(doc);
            } else if (WSSConstants.NS_X509_PKIPATH_V1.equals(binarySecurityTokenType.getValueType())) {
                binarySecurity = new PKIPathSecurity(doc);
            } else if (WSSConstants.NS_GSS_KERBEROS5_AP_REQ.equals(binarySecurityTokenType.getValueType())) {
                binarySecurity = new KerberosSecurity(doc);
            } else {
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN);
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
