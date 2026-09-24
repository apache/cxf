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
package org.apache.cxf.sts.token.validator;

import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.token.realm.CertConstraintsParser;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SignatureTrustValidator;
import org.apache.wss4j.dom.validate.Validator;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.X509Data;

/**
 * This class validates an X.509 V.3 certificate (received as a BinarySecurityToken or an X509Data
 * DOM Element). The cert must be known (or trusted) by the STS crypto object.
 */
public class X509TokenValidator implements TokenValidator {

    public static final String X509_V3_TYPE = WSConstants.X509TOKEN_NS + "#X509v3";

    public static final String BASE64_ENCODING = WSConstants.SOAPMESSAGE_NS + "#Base64Binary";

    private static final Logger LOG = LogUtils.getL7dLogger(X509TokenValidator.class);

    private Validator validator = new SignatureTrustValidator();

    private CertConstraintsParser certConstraints = new CertConstraintsParser();

    private boolean validateProofOfPossession;

    /**
     * Set a list of Strings corresponding to regular expression constraints on the subject DN
     * of a certificate
     */
    public void setSubjectConstraints(List<String> subjectConstraints) {
        certConstraints.setSubjectConstraints(subjectConstraints);
    }

    /**
     * Whether to require the requestor to prove possession of the private key that corresponds to
     * the X.509 certificate being validated. This is disabled by default.
     *
     * <p>An X.509 certificate is public data, so trust-chain verification alone does not establish
     * that the requestor is the certificate's subject. When the Validate operation is reachable by
     * untrusted callers, this lets anyone holding a copy of any certificate that chains to the STS
     * truststore have that certificate marked VALID - and, via WS-Trust token transformation
     * (Validate with a requested TokenType), obtain an STS-issued token for the certificate's
     * subject. Enabling this check requires the requestor to prove possession of the private key (a
     * message signature made with, or a TLS client certificate matching, the validated certificate)
     * before the token is considered VALID.
     *
     * <p><b>Note:</b> this is off by default because it is incompatible with brokered validation, a
     * common deployment where a trusted intermediary (for example a service that already
     * authenticated the client) forwards the client's bare certificate to the STS for
     * validation/transformation over a separately secured channel. In that pattern the intermediary
     * does not hold the client's private key, so it cannot prove possession at the STS. Enable this
     * only when the Validate operation may be reached by untrusted callers and brokered validation
     * is not in use; otherwise restrict access to the Validate endpoint instead.
     *
     * @param validateProofOfPossession whether to require proof of possession (default false)
     */
    public void setValidateProofOfPossession(boolean validateProofOfPossession) {
        this.validateProofOfPossession = validateProofOfPossession;
    }

    /**
     * Set the WSS4J Validator instance to use to validate the token.
     * @param validator the WSS4J Validator instance to use to validate the token
     */
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument.
     */
    public boolean canHandleToken(ReceivedToken validateTarget) {
        return canHandleToken(validateTarget, null);
    }

    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument. The realm is ignored in this token Validator.
     */
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        Object token = validateTarget.getToken();
        if (token instanceof BinarySecurityTokenType
            && X509_V3_TYPE.equals(((BinarySecurityTokenType)token).getValueType())) {
            return true;
        } else if (token instanceof Element
            && WSS4JConstants.SIG_NS.equals(((Element)token).getNamespaceURI())
            && WSS4JConstants.X509_DATA_LN.equals(((Element)token).getLocalName())) {
            return true;
        }
        return false;
    }

    /**
     * Validate a Token using the given TokenValidatorParameters.
     */
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        LOG.fine("Validating X.509 Token");
        STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
        CallbackHandler callbackHandler = stsProperties.getCallbackHandler();

        // See CXF-4028
        Crypto crypto = stsProperties.getEncryptionCrypto();
        if (crypto == null) {
            crypto = stsProperties.getSignatureCrypto();
        }

        RequestData requestData = new RequestData();
        requestData.setSigVerCrypto(crypto);
        requestData.setWssConfig(WSSConfig.getNewInstance());
        requestData.setCallbackHandler(callbackHandler);
        requestData.setMsgContext(tokenParameters.getMessageContext());
        requestData.setSubjectCertConstraints(certConstraints.getCompiledSubjectContraints());

        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(STATE.INVALID);
        response.setToken(validateTarget);

        final BinarySecurity binarySecurity;
        if (validateTarget.isBinarySecurityToken()) {
            BinarySecurityTokenType binarySecurityType = (BinarySecurityTokenType)validateTarget.getToken();

            // Test the encoding type
            String encodingType = binarySecurityType.getEncodingType();
            if (!BASE64_ENCODING.equals(encodingType)) {
                LOG.fine("Bad encoding type attribute specified: " + encodingType);
                return response;
            }

            //
            // Turn the received JAXB object into a DOM element
            //
            Document doc = DOMUtils.getEmptyDocument();
            binarySecurity = new X509Security(doc);
            binarySecurity.setEncodingType(encodingType);
            binarySecurity.setValueType(binarySecurityType.getValueType());
            String data = binarySecurityType.getValue();

            Node textNode = doc.createTextNode(data);
            binarySecurity.getElement().appendChild(textNode);
        } else if (validateTarget.isDOMElement()) {
            try {
                Document doc = DOMUtils.getEmptyDocument();
                binarySecurity = new X509Security(doc);
                binarySecurity.setEncodingType(BASE64_ENCODING);
                X509Data x509Data = new X509Data((Element)validateTarget.getToken(), "");
                if (x509Data.containsCertificate()) {
                    X509Certificate cert = x509Data.itemCertificate(0).getX509Certificate();
                    ((X509Security)binarySecurity).setX509Certificate(cert);
                }
            } catch (XMLSecurityException ex) {
                LOG.log(Level.WARNING, "", ex);
                return response;
            }
        } else {
            return response;
        }

        //
        // Validate the token
        //
        try {
            Credential credential = new Credential();
            credential.setBinarySecurityToken(binarySecurity);
            if (crypto != null) {
                X509Certificate cert = ((X509Security)binarySecurity).getX509Certificate(crypto);
                credential.setCertificates(new X509Certificate[]{cert});
            }

            Credential returnedCredential = validator.validate(credential, requestData);
            X509Certificate[] validatedCerts = returnedCredential.getCertificates();

            // The certificate is trusted, but a certificate is public data. Unless the requestor
            // has proven possession of the corresponding private key, we must not confer the
            // certificate subject's identity - otherwise anyone holding a copy of a trusted
            // certificate could have a token issued in that subject's name via token
            // transformation. See setValidateProofOfPossession().
            if (validateProofOfPossession
                && !verifyProofOfPossession(validatedCerts, tokenParameters.getMessageContext())) {
                LOG.log(
                    Level.WARNING,
                    "Failed to verify the proof of possession of the private key corresponding to "
                    + "the X.509 certificate being validated"
                );
                return response;
            }

            Principal principal = returnedCredential.getPrincipal();
            if (principal == null) {
                principal = validatedCerts[0].getSubjectX500Principal();
            }
            response.setPrincipal(principal);
            validateTarget.setState(STATE.VALID);
            LOG.fine("X.509 Token successfully validated");
        } catch (WSSecurityException ex) {
            LOG.log(Level.WARNING, "", ex);
        }
        return response;
    }

    /**
     * Verify that the requestor proved possession of the private key corresponding to (one of) the
     * validated certificate(s), either by signing the request message with it or by presenting it
     * as a TLS client certificate.
     */
    protected boolean verifyProofOfPossession(
        X509Certificate[] validatedCerts,
        Map<String, Object> messageContext
    ) {
        if (validatedCerts == null || validatedCerts.length == 0 || messageContext == null) {
            return false;
        }

        // Certificate(s) used to sign the request message
        final List<WSHandlerResult> handlerResults =
            CastUtils.cast((List<?>) messageContext.get(WSHandlerConstants.RECV_RESULTS));
        if (handlerResults != null && !handlerResults.isEmpty()) {
            final List<WSSecurityEngineResult> signedResults = new ArrayList<>();
            for (WSHandlerResult handlerResult : handlerResults) {
                if (handlerResult.getActionResults().containsKey(WSConstants.SIGN)) {
                    signedResults.addAll(handlerResult.getActionResults().get(WSConstants.SIGN));
                }
                if (handlerResult.getActionResults().containsKey(WSConstants.UT_SIGN)) {
                    signedResults.addAll(handlerResult.getActionResults().get(WSConstants.UT_SIGN));
                }
            }
            for (WSSecurityEngineResult signedResult : signedResults) {
                X509Certificate signingCert =
                    (X509Certificate)signedResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                if (matchesValidatedCert(signingCert, validatedCerts)) {
                    return true;
                }
            }
        }

        // Certificate presented at the TLS layer
        TLSSessionInfo tlsInfo = (TLSSessionInfo)messageContext.get(TLSSessionInfo.class.getName());
        if (tlsInfo != null && tlsInfo.getPeerCertificates() != null) {
            for (Certificate tlsCert : tlsInfo.getPeerCertificates()) {
                if (tlsCert instanceof X509Certificate
                    && matchesValidatedCert((X509Certificate)tlsCert, validatedCerts)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean matchesValidatedCert(X509Certificate presentedCert, X509Certificate[] validatedCerts) {
        if (presentedCert == null) {
            return false;
        }
        for (X509Certificate validatedCert : validatedCerts) {
            if (presentedCert.equals(validatedCert)) {
                return true;
            }
        }
        return false;
    }

}
