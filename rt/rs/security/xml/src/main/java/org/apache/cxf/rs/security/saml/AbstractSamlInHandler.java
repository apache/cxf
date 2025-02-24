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

package org.apache.cxf.rs.security.saml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.RSSecurityUtils;
import org.apache.cxf.rs.security.saml.authorization.SecurityContextProvider;
import org.apache.cxf.rs.security.saml.authorization.SecurityContextProviderImpl;
import org.apache.cxf.rs.security.xml.AbstractXmlSecInHandler;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.WSProviderConfig;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.saml.WSSSAMLKeyInfoProcessor;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SamlAssertionValidator;
import org.apache.wss4j.dom.validate.Validator;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;

@PreMatching
public abstract class AbstractSamlInHandler implements ContainerRequestFilter {

    private static final Logger LOG =
        LogUtils.getL7dLogger(AbstractSamlInHandler.class);

    static {
        WSProviderConfig.init();
    }

    private Validator samlValidator = new SamlAssertionValidator();
    private boolean keyInfoMustBeAvailable = true;
    private SecurityContextProvider scProvider = new SecurityContextProviderImpl();

    public void setValidator(Validator validator) {
        samlValidator = validator;
    }

    public void setSecurityContextProvider(SecurityContextProvider p) {
        scProvider = p;
    }


    protected void validateToken(Message message, InputStream tokenStream) {

        Element token = readToken(message, tokenStream);
        validateToken(message, token);

    }

    protected Element readToken(Message message, InputStream tokenStream) {

        try {
            Document doc = StaxUtils.read(new InputStreamReader(tokenStream, StandardCharsets.UTF_8));
            return doc.getDocumentElement();
        } catch (Exception ex) {
            throwFault("Assertion can not be read as XML document", ex);
        }
        return null;

    }

    protected void validateToken(Message message, Element tokenElement) {
        validateToken(message, toWrapper(tokenElement));
    }

    protected SamlAssertionWrapper toWrapper(Element tokenElement) {
        try {
            return new SamlAssertionWrapper(tokenElement);
        } catch (Exception ex) {
            throwFault("Assertion can not be validated", ex);
        }
        return null;
    }

    protected void validateToken(Message message, SamlAssertionWrapper assertion) {
        try {
            RequestData data = new RequestData();
            data.setMsgContext(message);

            // Add Audience Restrictions for SAML
            configureAudienceRestriction(message, data);

            if (assertion.isSigned()) {
                WSSConfig cfg = WSSConfig.getNewInstance();
                data.setWssConfig(cfg);
                data.setCallbackHandler(RSSecurityUtils.getCallbackHandler(message, this.getClass()));
                try {
                    data.setSigVerCrypto(new CryptoLoader().getCrypto(message,
                                                SecurityConstants.SIGNATURE_CRYPTO,
                                                SecurityConstants.SIGNATURE_PROPERTIES));
                } catch (IOException ex) {
                    throwFault("Crypto can not be loaded", ex);
                }

                boolean enableRevocation = false;
                String enableRevocationStr =
                    (String)org.apache.cxf.rt.security.utils.SecurityUtils.getSecurityPropertyValue(
                        SecurityConstants.ENABLE_REVOCATION, message);
                if (enableRevocationStr != null) {
                    enableRevocation = Boolean.parseBoolean(enableRevocationStr);
                }
                data.setEnableRevocation(enableRevocation);

                Signature sig = assertion.getSignature();
                WSDocInfo docInfo = new WSDocInfo(sig.getDOM().getOwnerDocument());
                data.setWsDocInfo(docInfo);

                SAMLKeyInfo samlKeyInfo = null;

                KeyInfo keyInfo = sig.getKeyInfo();
                if (keyInfo != null) {
                    samlKeyInfo = SAMLUtil.getCredentialFromKeyInfo(
                        keyInfo.getDOM(), new WSSSAMLKeyInfoProcessor(data),
                        data.getSigVerCrypto()
                    );
                } else if (!keyInfoMustBeAvailable) {
                    samlKeyInfo = createKeyInfoFromDefaultAlias(data.getSigVerCrypto());
                }

                assertion.verifySignature(samlKeyInfo);
                assertion.parseSubject(
                    new WSSSAMLKeyInfoProcessor(data), data.getSigVerCrypto()
                );
            } else if (getTLSCertificates(message) == null) {
                throwFault("Assertion must be signed", null);
            }
            if (samlValidator != null) {
                Credential credential = new Credential();
                credential.setSamlAssertion(assertion);
                samlValidator.validate(credential, data);
            }


            checkSubjectConfirmationData(message, assertion);
            setSecurityContext(message, assertion);

        } catch (Exception ex) {
            throwFault("Assertion can not be validated", ex);
        }
    }

    protected void configureAudienceRestriction(Message msg, RequestData reqData) {
        reqData.setAudienceRestrictions(SAMLUtils.getAudienceRestrictions(msg, false));
    }

    protected SAMLKeyInfo createKeyInfoFromDefaultAlias(Crypto sigCrypto) throws WSSecurityException {
        try {
            X509Certificate[] certs = RSSecurityUtils.getCertificates(sigCrypto,
                                                                    sigCrypto.getDefaultX509Identifier());
            SAMLKeyInfo samlKeyInfo = new SAMLKeyInfo(new X509Certificate[]{certs[0]});
            samlKeyInfo.setPublicKey(certs[0].getPublicKey());
            return samlKeyInfo;
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Error in loading the certificates: " + ex.getMessage(), ex);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_SIGNATURE, ex);
        }
    }

    protected void checkSubjectConfirmationData(Message message, SamlAssertionWrapper assertion) {
        String valSAMLSubjectConf =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION,
                                                           message);
        boolean validateSAMLSubjectConf = true;
        if (valSAMLSubjectConf != null) {
            validateSAMLSubjectConf = Boolean.parseBoolean(valSAMLSubjectConf);
        }

        if (validateSAMLSubjectConf) {
            Certificate[] tlsCerts = getTLSCertificates(message);
            if (!checkHolderOfKey(message, assertion, tlsCerts)) {
                throwFault("Holder Of Key claim fails", null);
            }
            if (!checkSenderVouches(message, assertion, tlsCerts)) {
                throwFault("Sender vouchers claim fails", null);
            }
            if (!checkBearer(assertion, tlsCerts)) {
                throwFault("Bearer claim fails", null);
            }
        }
    }

    protected void setSecurityContext(Message message, SamlAssertionWrapper wrapper) {
        if (scProvider != null) {
            SecurityContext sc = scProvider.getSecurityContext(message, wrapper);
            message.put(SecurityContext.class, sc);
        }
    }

    private Certificate[] getTLSCertificates(Message message) {
        TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
        return tlsInfo != null ? tlsInfo.getPeerCertificates() : null;
    }

    protected void throwFault(String error, Exception ex) {
        // TODO: get bundle resource message once this filter is moved
        // to rt/rs/security
        String errorMsg = error;
        if (ex != null) {
            errorMsg += ": " + ExceptionUtils.getStackTrace(ex);
        }
        LOG.warning(errorMsg);
        Response response = JAXRSUtils.toResponseBuilder(401).entity(error).build();
        throw ExceptionUtils.toNotAuthorizedException(null, response);
    }

    /**
     * Check the sender-vouches requirements against the received assertion. The SAML
     * Assertion and the request body must be signed by the same signature.
     */
    protected boolean checkSenderVouches(
        Message message,
        SamlAssertionWrapper assertionWrapper,
        Certificate[] tlsCerts
    ) {
        //
        // If we have a 2-way TLS connection, then we don't have to check that the
        // assertion + body are signed

        // If no body is available (ex, with GET) then consider validating that
        // the base64-encoded token is signed by the same signature
        //
        if (tlsCerts != null && tlsCerts.length > 0) {
            return true;
        }
        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
        for (String confirmationMethod : confirmationMethods) {
            if (OpenSAMLUtil.isMethodSenderVouches(confirmationMethod)) {

                Element signedElement = message.getContent(Element.class);
                Node assertionParent = assertionWrapper.getElement().getParentNode();

                // if we have a shared parent signed node then we can assume both
                // this SAML assertion and the main payload have been signed by the same
                // signature
                if (assertionParent != signedElement) {
                    // if not then try to compare if the same cert/key was used to sign SAML token
                    // and the payload
                    SAMLKeyInfo subjectKeyInfo = assertionWrapper.getSignatureKeyInfo();
                    if (!compareCredentials(subjectKeyInfo, message, tlsCerts)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }



    protected boolean checkHolderOfKey(Message message,
                                    SamlAssertionWrapper assertionWrapper,
                                    Certificate[] tlsCerts) {
        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
        for (String confirmationMethod : confirmationMethods) {
            if (OpenSAMLUtil.isMethodHolderOfKey(confirmationMethod)) {
                SAMLKeyInfo subjectKeyInfo = assertionWrapper.getSubjectKeyInfo();
                if (!compareCredentials(subjectKeyInfo, message, tlsCerts)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Compare the credentials of the assertion to the credentials used in 2-way TLS or those
     * used to verify signatures.
     * Return true on a match
     * @param subjectKeyInfo the SAMLKeyInfo object
     * @param message the current Message
     * @param tlsCerts the TLS client certificates
     * @return true if the credentials of the assertion were used to verify a signature
     */
    private boolean compareCredentials(
        SAMLKeyInfo subjectKeyInfo,
        Message message,
        Certificate[] tlsCerts
    ) {
        X509Certificate[] subjectCerts = subjectKeyInfo.getCerts();
        PublicKey subjectPublicKey = subjectKeyInfo.getPublicKey();

        //
        // Try to match the TLS certs first
        //
        if (tlsCerts != null && tlsCerts.length > 0 && subjectCerts != null
            && subjectCerts.length > 0 && tlsCerts[0].equals(subjectCerts[0])) {
            return true;
        } else if (tlsCerts != null && tlsCerts.length > 0 
            && tlsCerts[0].getPublicKey().equals(subjectPublicKey)) {
            return true;
        }

        //
        // Now try the message-level signatures
        //
        try {
            X509Certificate signingCert =
                (X509Certificate)message.getExchange().getInMessage().get(
                    AbstractXmlSecInHandler.SIGNING_CERT);

            if (subjectCerts != null && subjectCerts.length > 0
                && signingCert != null && signingCert.equals(subjectCerts[0])) {
                return true;
            }

            PublicKey signingKey =
                (PublicKey)message.getExchange().getInMessage().get(
                    AbstractXmlSecInHandler.SIGNING_PUBLIC_KEY);
            if (signingKey != null && signingKey.equals(subjectPublicKey)) {
                return true;
            }
        } catch (Exception ex) {
            // ignore
        }

        return false;
    }

    protected boolean checkBearer(SamlAssertionWrapper assertionWrapper, Certificate[] tlsCerts) {
        List<String> confirmationMethods = assertionWrapper.getConfirmationMethods();
        for (String confirmationMethod : confirmationMethods) {
            boolean isBearer = isMethodBearer(confirmationMethod);
            if (isBearer && !assertionWrapper.isSigned() && (tlsCerts == null || tlsCerts.length == 0)) {
                return false;
            }
            // do some more validation - time based, etc
        }
        return true;
    }

    private boolean isMethodBearer(String confirmMethod) {
        return confirmMethod != null && confirmMethod.startsWith("urn:oasis:names:tc:SAML:")
                && confirmMethod.endsWith(":cm:bearer");
    }

    public void setKeyInfoMustBeAvailable(boolean keyInfoMustBeAvailable) {
        this.keyInfoMustBeAvailable = keyInfoMustBeAvailable;
    }
}
