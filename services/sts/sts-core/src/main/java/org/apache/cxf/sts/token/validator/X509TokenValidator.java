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
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
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
import org.apache.wss4j.dom.handler.RequestData;
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

    /**
     * Set a list of Strings corresponding to regular expression constraints on the subject DN
     * of a certificate
     */
    public void setSubjectConstraints(List<String> subjectConstraints) {
        certConstraints.setSubjectConstraints(subjectConstraints);
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
            Principal principal = returnedCredential.getPrincipal();
            if (principal == null) {
                principal = returnedCredential.getCertificates()[0].getSubjectX500Principal();
            }
            response.setPrincipal(principal);
            validateTarget.setState(STATE.VALID);
            LOG.fine("X.509 Token successfully validated");
        } catch (WSSecurityException ex) {
            LOG.log(Level.WARNING, "", ex);
        }
        return response;
    }

}
