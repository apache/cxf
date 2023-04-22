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

package org.apache.cxf.ws.security.wss4j.policyvalidators;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.token.PKIPathSecurity;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.token.KerberosSecurity;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractSecuredParts;
import org.apache.wss4j.policy.model.AbstractSecurityAssertion;
import org.apache.wss4j.policy.model.AbstractToken;
import org.apache.wss4j.policy.model.AbstractToken.DerivedKeys;
import org.apache.wss4j.policy.model.EncryptedElements;
import org.apache.wss4j.policy.model.EncryptedParts;
import org.apache.wss4j.policy.model.Header;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.RequiredElements;
import org.apache.wss4j.policy.model.SignedElements;
import org.apache.wss4j.policy.model.SignedParts;
import org.apache.wss4j.policy.model.SupportingTokens;

/**
 * A base class to use to validate various SupportingToken policies.
 */
public abstract class AbstractSupportingTokenPolicyValidator extends AbstractSecurityPolicyValidator {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractSupportingTokenPolicyValidator.class);

    private SignedElements signedElements;
    private EncryptedElements encryptedElements;
    private SignedParts signedParts;
    private EncryptedParts encryptedParts;
    private boolean enforceEncryptedTokens = true;

    protected abstract boolean isSigned();
    protected abstract boolean isEncrypted();
    protected abstract boolean isEndorsing();

    /**
     * Process UsernameTokens.
     */
    protected boolean processUsernameTokens(PolicyValidatorParameters parameters, boolean derived) {
        if (!parameters.isUtWithCallbacks()) {
            return true;
        }

        if (parameters.getUsernameTokenResults().isEmpty()) {
            return false;
        }

        List<WSSecurityEngineResult> tokenResults = new ArrayList<>();
        tokenResults.addAll(parameters.getUsernameTokenResults());

        if (isSigned() && !areTokensSigned(tokenResults, parameters.getSignedResults(),
                                           parameters.getEncryptedResults(),
                                           parameters.getMessage())) {
            return false;
        }
        if (isEncrypted() && !areTokensEncrypted(tokenResults, parameters.getEncryptedResults())) {
            return false;
        }

        if (derived && parameters.getResults().getActionResults().containsKey(WSConstants.DKT)) {
            for (WSSecurityEngineResult wser : parameters.getUsernameTokenResults()) {
                byte[] secret = (byte[])wser.get(WSSecurityEngineResult.TAG_SECRET);
                if (secret != null) {
                    WSSecurityEngineResult dktResult =
                        getMatchingDerivedKey(secret, parameters.getResults());
                    if (dktResult != null) {
                        tokenResults.add(dktResult);
                    }
                }
            }
        }

        return !((isEndorsing() && !checkEndorsed(tokenResults, parameters.getSignedResults(),
                                             parameters.getMessage(),
                                             parameters.getTimestampElement()))
            || !validateSignedEncryptedPolicies(tokenResults, parameters.getSignedResults(),
                                                parameters.getEncryptedResults(),
                                                parameters.getMessage()));
    }


    /**
     * Process SAML Tokens. Only signed results are supported.
     */
    protected boolean processSAMLTokens(PolicyValidatorParameters parameters, boolean derived) {
        if (parameters.getSamlResults().isEmpty()) {
            return false;
        }

        List<WSSecurityEngineResult> tokenResults = new ArrayList<>();
        tokenResults.addAll(parameters.getSamlResults());


        if (isSigned() && !areTokensSigned(tokenResults, parameters.getSignedResults(),
                                           parameters.getEncryptedResults(),
                                           parameters.getMessage())) {
            return false;
        }
        if (isEncrypted() && !areTokensEncrypted(tokenResults,
                                                 parameters.getEncryptedResults())) {
            return false;
        }

        if (derived && parameters.getResults().getActionResults().containsKey(WSConstants.DKT)) {
            List<WSSecurityEngineResult> dktResults = new ArrayList<>(tokenResults.size());
            for (WSSecurityEngineResult wser : tokenResults) {
                SamlAssertionWrapper assertion =
                    (SamlAssertionWrapper)wser.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                if (assertion != null && assertion.getSubjectKeyInfo() != null
                    && assertion.getSubjectKeyInfo().getSecret() != null) {
                    WSSecurityEngineResult dktResult =
                        getMatchingDerivedKey(assertion.getSubjectKeyInfo().getSecret(), parameters.getResults());
                    if (dktResult != null) {
                        dktResults.add(dktResult);
                    }
                }
            }
            tokenResults.addAll(dktResults);
        }


        if (isEndorsing() && !checkEndorsed(tokenResults, parameters.getSignedResults(),
                                            parameters.getMessage(),
                                            parameters.getTimestampElement())) {
            return false;
        }

        return validateSignedEncryptedPolicies(tokenResults, parameters.getSignedResults(),
                                             parameters.getEncryptedResults(),
                                             parameters.getMessage());
    }


    /**
     * Process Kerberos Tokens.
     */
    protected boolean processKerberosTokens(PolicyValidatorParameters parameters, boolean derived) {
        List<WSSecurityEngineResult> tokenResults = null;
        if (parameters.getResults().getActionResults().containsKey(WSConstants.BST)) {
            tokenResults = new ArrayList<>();
            for (WSSecurityEngineResult wser
                : parameters.getResults().getActionResults().get(WSConstants.BST)) {
                BinarySecurity binarySecurity =
                    (BinarySecurity)wser.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                if (binarySecurity instanceof KerberosSecurity) {
                    tokenResults.add(wser);
                }
            }
        }

        if (tokenResults == null || tokenResults.isEmpty()) {
            return false;
        }

        if (isSigned() && !areTokensSigned(tokenResults, parameters.getSignedResults(),
                                           parameters.getEncryptedResults(),
                                           parameters.getMessage())) {
            return false;
        }
        if (isEncrypted() && !areTokensEncrypted(tokenResults, parameters.getEncryptedResults())) {
            return false;
        }

        if (derived && parameters.getResults().getActionResults().containsKey(WSConstants.DKT)) {
            List<WSSecurityEngineResult> dktResults = new ArrayList<>(tokenResults.size());
            for (WSSecurityEngineResult wser : tokenResults) {
                byte[] secret = (byte[])wser.get(WSSecurityEngineResult.TAG_SECRET);
                WSSecurityEngineResult dktResult =
                    getMatchingDerivedKey(secret, parameters.getResults());
                if (dktResult != null) {
                    dktResults.add(dktResult);
                }
            }
            tokenResults.addAll(dktResults);
        }
        if (isEndorsing() && !checkEndorsed(tokenResults, parameters.getSignedResults(),
                                            parameters.getMessage(),
                                            parameters.getTimestampElement())) {
            return false;
        }

        return validateSignedEncryptedPolicies(tokenResults, parameters.getSignedResults(),
                                             parameters.getEncryptedResults(),
                                             parameters.getMessage());
    }


    /**
     * Process X509 Tokens.
     */
    protected boolean processX509Tokens(PolicyValidatorParameters parameters, boolean derived) {
        List<WSSecurityEngineResult> tokenResults = null;
        if (parameters.getResults().getActionResults().containsKey(WSConstants.BST)) {
            tokenResults = new ArrayList<>();
            for (WSSecurityEngineResult wser
                : parameters.getResults().getActionResults().get(WSConstants.BST)) {
                BinarySecurity binarySecurity =
                    (BinarySecurity)wser.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                if (binarySecurity instanceof X509Security
                    || binarySecurity instanceof PKIPathSecurity) {
                    tokenResults.add(wser);
                }
            }
        }

        if (tokenResults == null || tokenResults.isEmpty()) {
            return false;
        }

        if (isSigned() && !areTokensSigned(tokenResults, parameters.getSignedResults(),
                                           parameters.getEncryptedResults(),
                                           parameters.getMessage())) {
            return false;
        }
        if (isEncrypted() && !areTokensEncrypted(tokenResults, parameters.getEncryptedResults())) {
            return false;
        }

        if (derived && parameters.getResults().getActionResults().containsKey(WSConstants.DKT)) {
            List<WSSecurityEngineResult> dktResults = new ArrayList<>(tokenResults.size());
            for (WSSecurityEngineResult wser : tokenResults) {
                WSSecurityEngineResult resultToStore =
                    processX509DerivedTokenResult(wser, parameters.getResults());
                if (resultToStore != null) {
                    dktResults.add(resultToStore);
                }
            }
            tokenResults.addAll(dktResults);
        }

        if (isEndorsing() && !checkEndorsed(tokenResults, parameters.getSignedResults(),
                                            parameters.getMessage(),
                                            parameters.getTimestampElement())) {
            return false;
        }

        return validateSignedEncryptedPolicies(tokenResults, parameters.getSignedResults(),
                                             parameters.getEncryptedResults(),
                                             parameters.getMessage());
    }

    /**
     * Process KeyValue Tokens.
     */
    protected boolean processKeyValueTokens(PolicyValidatorParameters parameters) {
        List<WSSecurityEngineResult> tokenResults = null;
        if (parameters.getSignedResults() != null && !parameters.getSignedResults().isEmpty()) {
            tokenResults = new ArrayList<>();
            for (WSSecurityEngineResult wser : parameters.getSignedResults()) {
                PublicKey publicKey =
                    (PublicKey)wser.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);
                if (publicKey != null) {
                    tokenResults.add(wser);
                }
            }
        }

        if (tokenResults == null || tokenResults.isEmpty()) {
            return false;
        }

        if (isSigned() && !areTokensSigned(tokenResults, parameters.getSignedResults(),
                                           parameters.getEncryptedResults(),
                                           parameters.getMessage())) {
            return false;
        }
        if (isEncrypted() && !areTokensEncrypted(tokenResults, parameters.getEncryptedResults())) {
            return false;
        }
        if (isEndorsing() && !checkEndorsed(tokenResults, parameters.getSignedResults(),
                                            parameters.getMessage(),
                                            parameters.getTimestampElement())) {
            return false;
        }

        return validateSignedEncryptedPolicies(tokenResults, parameters.getSignedResults(),
                                             parameters.getEncryptedResults(),
                                             parameters.getMessage());
    }

    /**
     * Validate (SignedParts|SignedElements|EncryptedParts|EncryptedElements) policies of this
     * SupportingToken.
     */
    private boolean validateSignedEncryptedPolicies(List<WSSecurityEngineResult> tokenResults,
                                                    List<WSSecurityEngineResult> signedResults,
                                                    List<WSSecurityEngineResult> encryptedResults,
                                                    Message message) {
        if (!validateSignedEncryptedParts(signedParts, false, signedResults, tokenResults, message)) {
            return false;
        }

        if (!validateSignedEncryptedParts(encryptedParts, true, encryptedResults, tokenResults, message)) {
            return false;
        }

        if (!validateSignedEncryptedElements(signedElements, signedResults, tokenResults, message)) {
            return false;
        }

        return validateSignedEncryptedElements(encryptedElements, encryptedResults, tokenResults, message);
    }


    /**
     * Process Security Context Tokens.
     */
    protected boolean processSCTokens(PolicyValidatorParameters parameters, boolean derived) {
        if (!parameters.getResults().getActionResults().containsKey(WSConstants.SCT)) {
            return false;
        }
        List<WSSecurityEngineResult> tokenResults = new ArrayList<>();
        tokenResults.addAll(parameters.getResults().getActionResults().get(WSConstants.SCT));

        if (isSigned() && !areTokensSigned(tokenResults, parameters.getSignedResults(),
                                           parameters.getEncryptedResults(),
                                           parameters.getMessage())) {
            return false;
        }
        if (isEncrypted() && !areTokensEncrypted(tokenResults, parameters.getEncryptedResults())) {
            return false;
        }

        if (derived && parameters.getResults().getActionResults().containsKey(WSConstants.DKT)) {
            List<WSSecurityEngineResult> dktResults = new ArrayList<>(tokenResults.size());
            for (WSSecurityEngineResult wser : tokenResults) {
                byte[] secret = (byte[])wser.get(WSSecurityEngineResult.TAG_SECRET);
                WSSecurityEngineResult dktResult =
                    getMatchingDerivedKey(secret, parameters.getResults());
                if (dktResult != null) {
                    dktResults.add(dktResult);
                }
            }
            tokenResults.addAll(dktResults);
        }

        if (isEndorsing() && !checkEndorsed(tokenResults, parameters.getSignedResults(),
                                            parameters.getMessage(),
                                            parameters.getTimestampElement())) {
            return false;
        }

        return validateSignedEncryptedPolicies(tokenResults, parameters.getSignedResults(),
                                             parameters.getEncryptedResults(),
                                             parameters.getMessage());
    }

    /**
     * Find an EncryptedKey element that has a cert that matches the cert of the signature, then
     * find a DerivedKey element that matches that EncryptedKey element.
     */
    private WSSecurityEngineResult processX509DerivedTokenResult(WSSecurityEngineResult result,
                                                                 WSHandlerResult results) {
        X509Certificate cert =
            (X509Certificate)result.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        WSSecurityEngineResult encrResult = getMatchingEncryptedKey(cert, results);
        if (encrResult != null) {
            byte[] secret = (byte[])encrResult.get(WSSecurityEngineResult.TAG_SECRET);
            WSSecurityEngineResult dktResult = getMatchingDerivedKey(secret, results);
            if (dktResult != null) {
                return dktResult;
            }
        }
        return null;
    }

    /**
     * Get a security result representing a Derived Key that has a secret key that
     * matches the parameter.
     */
    private WSSecurityEngineResult getMatchingDerivedKey(byte[] secret,
                                                         WSHandlerResult results) {
        for (WSSecurityEngineResult wser : results.getActionResults().get(WSConstants.DKT)) {
            byte[] dktSecret = (byte[])wser.get(WSSecurityEngineResult.TAG_SECRET);
            if (Arrays.equals(secret, dktSecret)) {
                return wser;
            }
        }
        return null;
    }

    /**
     * Get a security result representing an EncryptedKey that matches the parameter.
     */
    private WSSecurityEngineResult getMatchingEncryptedKey(X509Certificate cert,
                                                           WSHandlerResult results) {
        if (results.getActionResults().containsKey(WSConstants.ENCR)) {
            for (WSSecurityEngineResult wser : results.getActionResults().get(WSConstants.ENCR)) {
                X509Certificate encrCert =
                    (X509Certificate)wser.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                if (cert.equals(encrCert)) {
                    return wser;
                }
            }
        }
        return null;
    }

    protected boolean isTLSInUse(Message message) {
        // See whether TLS is in use or not
        TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
        return tlsInfo != null;
    }

    /**
     * Check the endorsing supporting token policy. If we're using the Transport Binding then
     * check that the Timestamp is signed. Otherwise, check that the signature is signed.
     * @return true if the endorsed supporting token policy is correct
     */
    private boolean checkEndorsed(List<WSSecurityEngineResult> tokenResults,
                                  List<WSSecurityEngineResult> signedResults,
                                  Message message,
                                  Element timestamp) {
        boolean endorsingSatisfied = false;
        if (isTLSInUse(message)) {
            endorsingSatisfied = checkTimestampIsSigned(tokenResults, signedResults, timestamp);
        }
        if (!endorsingSatisfied) {
            endorsingSatisfied = checkSignatureIsSigned(tokenResults, signedResults);
        }
        return endorsingSatisfied;
    }


    /**
     * Return true if a list of tokens were signed, false otherwise.
     */
    private boolean areTokensSigned(List<WSSecurityEngineResult> tokens,
                                    List<WSSecurityEngineResult> signedResults,
                                    List<WSSecurityEngineResult> encryptedResults,
                                    Message message) {
        if (!isTLSInUse(message)) {
            for (WSSecurityEngineResult wser : tokens) {
                Element tokenElement = (Element)wser.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
                if (tokenElement == null
                    || !isTokenSigned(tokenElement, signedResults, encryptedResults)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return true if a list of tokens were encrypted, false otherwise.
     */
    private boolean areTokensEncrypted(List<WSSecurityEngineResult> tokens,
                                       List<WSSecurityEngineResult> encryptedResults) {
        if (enforceEncryptedTokens) {
            for (WSSecurityEngineResult wser : tokens) {
                Element tokenElement = (Element)wser.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
                if (tokenElement == null || !isTokenEncrypted(tokenElement, encryptedResults)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return true if the Timestamp is signed by one of the token results
     * @param tokenResults A list of WSSecurityEngineResults corresponding to tokens
     * @return true if the Timestamp is signed
     */
    private boolean checkTimestampIsSigned(List<WSSecurityEngineResult> tokenResults,
                                           List<WSSecurityEngineResult> signedResults,
                                           Element timestamp) {
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> sl =
                CastUtils.cast((List<?>)signedResult.get(
                    WSSecurityEngineResult.TAG_DATA_REF_URIS
                ));
            if (sl != null) {
                for (WSDataRef dataRef : sl) {
                    if (timestamp == dataRef.getProtectedElement()
                        && checkSignatureOrEncryptionResult(signedResult, tokenResults)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Return true if the Signature is itself signed by one of the token results
     * @param tokenResults A list of WSSecurityEngineResults corresponding to tokens
     * @return true if the Signature is itself signed
     */
    private boolean checkSignatureIsSigned(List<WSSecurityEngineResult> tokenResults,
                                           List<WSSecurityEngineResult> signedResults) {
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> sl =
                CastUtils.cast((List<?>)signedResult.get(
                    WSSecurityEngineResult.TAG_DATA_REF_URIS
                ));
            if (sl != null && !sl.isEmpty()) {
                for (WSDataRef dataRef : sl) {
                    QName signedQName = dataRef.getName();
                    if (WSConstants.SIGNATURE.equals(signedQName)
                        && checkSignatureOrEncryptionResult(signedResult, tokenResults)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check that a WSSecurityEngineResult corresponding to a signature or encryption uses the same
     * signing/encrypting credential as one of the tokens.
     * @param result a WSSecurityEngineResult corresponding to a signature or encryption
     * @param tokenResult A list of WSSecurityEngineResults corresponding to tokens
     * @return
     */
    private boolean checkSignatureOrEncryptionResult(
        WSSecurityEngineResult result,
        List<WSSecurityEngineResult> tokenResult
    ) {
        // See what was used to sign/encrypt this result
        X509Certificate cert =
            (X509Certificate)result.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        byte[] secret = (byte[])result.get(WSSecurityEngineResult.TAG_SECRET);
        PublicKey publicKey =
            (PublicKey)result.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);

        // Now see if the same credential exists in the tokenResult list
        for (WSSecurityEngineResult token : tokenResult) {
            Integer actInt = (Integer)token.get(WSSecurityEngineResult.TAG_ACTION);
            BinarySecurity binarySecurity =
                (BinarySecurity)token.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
            if (binarySecurity instanceof X509Security
                || binarySecurity instanceof PKIPathSecurity) {
                X509Certificate foundCert =
                    (X509Certificate)token.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
                if (foundCert.equals(cert)) {
                    return true;
                }
            } else if (actInt.intValue() == WSConstants.ST_SIGNED
                || actInt.intValue() == WSConstants.ST_UNSIGNED) {
                SamlAssertionWrapper assertionWrapper =
                    (SamlAssertionWrapper)token.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                SAMLKeyInfo samlKeyInfo = assertionWrapper.getSubjectKeyInfo();
                if (samlKeyInfo != null) {
                    X509Certificate[] subjectCerts = samlKeyInfo.getCerts();
                    byte[] subjectSecretKey = samlKeyInfo.getSecret();
                    PublicKey subjectPublicKey = samlKeyInfo.getPublicKey();
                    if ((cert != null && subjectCerts != null && cert.equals(subjectCerts[0]))
                        || (subjectSecretKey != null && Arrays.equals(subjectSecretKey, secret))
                        || (subjectPublicKey != null && subjectPublicKey.equals(publicKey))) {
                        return true;
                    }
                }
            } else if (publicKey != null) {
                PublicKey foundPublicKey =
                    (PublicKey)token.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);
                if (publicKey.equals(foundPublicKey)) {
                    return true;
                }
            } else {
                byte[] foundSecret = (byte[])token.get(WSSecurityEngineResult.TAG_SECRET);
                byte[] derivedKey =
                    (byte[])token.get(WSSecurityEngineResult.TAG_ENCRYPTED_EPHEMERAL_KEY);
                if ((foundSecret != null && Arrays.equals(foundSecret, secret))
                    || (derivedKey != null && Arrays.equals(derivedKey, secret))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Validate the SignedParts or EncryptedParts policies
     */
    private boolean validateSignedEncryptedParts(
        AbstractSecuredParts parts,
        boolean content,
        List<WSSecurityEngineResult> protResults,
        List<WSSecurityEngineResult> tokenResults,
        Message message
    ) {
        if (parts == null) {
            return true;
        }

        if (parts.isBody()) {
            SOAPMessage soapMessage = message.getContent(SOAPMessage.class);
            final Element soapBody;
            try {
                soapBody = soapMessage.getSOAPBody();
            } catch (SOAPException ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
                return false;
            }

            if (!checkProtectionResult(soapBody, content, protResults, tokenResults)) {
                return false;
            }
        }

        for (Header h : parts.getHeaders()) {
            SOAPMessage soapMessage = message.getContent(SOAPMessage.class);
            final Element soapHeader;
            try {
                soapHeader = soapMessage.getSOAPHeader();
            } catch (SOAPException ex) {
                LOG.log(Level.FINE, ex.getMessage(), ex);
                return false;
            }

            final List<Element> elements;
            if (h.getName() == null) {
                elements = DOMUtils.getChildrenWithNamespace(soapHeader, h.getNamespace());
            } else {
                elements = DOMUtils.getChildrenWithName(soapHeader, h.getNamespace(), h.getName());
            }

            for (Element el : elements) {
                el = (Element)DOMUtils.getDomElement(el);
                if (!checkProtectionResult(el, false, protResults, tokenResults)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check that an Element is signed or encrypted by one of the token results
     */
    private boolean checkProtectionResult(
        Element elementToProtect,
        boolean content,
        List<WSSecurityEngineResult> protResults,
        List<WSSecurityEngineResult> tokenResults
    ) {
        elementToProtect = (Element)DOMUtils.getDomElement(elementToProtect);
        for (WSSecurityEngineResult result : protResults) {
            List<WSDataRef> dataRefs =
                CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            if (dataRefs != null) {
                for (WSDataRef dataRef : dataRefs) {
                    if (elementToProtect == dataRef.getProtectedElement()
                        && content == dataRef.isContent()
                        && checkSignatureOrEncryptionResult(result, tokenResults)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Validate SignedElements or EncryptedElements policies
     */
    private boolean validateSignedEncryptedElements(
        RequiredElements elements,
        List<WSSecurityEngineResult> protResults,
        List<WSSecurityEngineResult> tokenResults,
        Message message
    ) {
        if (elements == null) {
            return true;
        }

        List<org.apache.wss4j.policy.model.XPath> xpaths = elements.getXPaths();

        //Map<String, String> namespaces = elements.getDeclaredNamespaces();
        //List<String> xpaths = elements.getXPathExpressions();

        if (xpaths != null && !xpaths.isEmpty()) {
            SOAPMessage soapMessage = message.getContent(SOAPMessage.class);
            Element soapEnvelope = soapMessage.getSOAPPart().getDocumentElement();

            // XPathFactory and XPath are not thread-safe so we must recreate them
            // each request.
            final XPathFactory factory = XPathFactory.newInstance();
            try {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            } catch (javax.xml.xpath.XPathFactoryConfigurationException ex) {
                // ignore
            }

            final XPath xpath = factory.newXPath();

            MapNamespaceContext namespaceContext = new MapNamespaceContext();

            for (org.apache.wss4j.policy.model.XPath xPath : xpaths) {
                Map<String, String> namespaceMap = xPath.getPrefixNamespaceMap();
                if (namespaceMap != null) {
                    namespaceContext.addNamespaces(namespaceMap);
                }
            }
            xpath.setNamespaceContext(namespaceContext);

            for (org.apache.wss4j.policy.model.XPath xPath : xpaths) {
                if (!checkXPathResult(soapEnvelope, xpath, xPath.getXPath(), protResults, tokenResults)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check a particular XPath result
     */
    private boolean checkXPathResult(
        Element soapEnvelope,
        XPath xpath,
        String xPathString,
        List<WSSecurityEngineResult> protResults,
        List<WSSecurityEngineResult> tokenResults
    ) {
        // Get the matching nodes
        NodeList list;
        try {
            list = (NodeList)xpath.evaluate(xPathString,
                                            soapEnvelope,
                                            XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            LOG.log(Level.FINE, e.getMessage(), e);
            return false;
        }

        // If we found nodes then we need to do the check.
        if (list.getLength() != 0) {
            // For each matching element, check for a ref that
            // covers it.
            for (int x = 0; x < list.getLength(); x++) {
                final Element el = (Element)list.item(x);

                if (!checkProtectionResult(el, false, protResults, tokenResults)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return true if a token was signed, false otherwise.
     */
    private boolean isTokenSigned(Element token, List<WSSecurityEngineResult> signedResults,
                                  List<WSSecurityEngineResult> encryptedResults) {
        for (WSSecurityEngineResult signedResult : signedResults) {
            List<WSDataRef> dataRefs =
                CastUtils.cast((List<?>)signedResult.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            for (WSDataRef dataRef : dataRefs) {
                if (token == dataRef.getProtectedElement()
                    || isEncryptedTokenSigned(token, dataRef, encryptedResults)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isEncryptedTokenSigned(Element token, WSDataRef signedRef,
                                           List<WSSecurityEngineResult> encryptedResults) {
        if (signedRef.getProtectedElement() != null
            && "EncryptedData".equals(signedRef.getProtectedElement().getLocalName())
            && WSS4JConstants.ENC_NS.equals(signedRef.getProtectedElement().getNamespaceURI())) {
            String encryptedDataId =
                signedRef.getProtectedElement().getAttributeNS(null, "Id");
            for (WSSecurityEngineResult result : encryptedResults) {
                List<WSDataRef> encryptedDataRefs =
                    CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
                if (encryptedDataRefs != null) {
                    for (WSDataRef encryptedDataRef : encryptedDataRefs) {
                        if (token == encryptedDataRef.getProtectedElement()
                            && (encryptedDataRef.getWsuId() != null
                                && encryptedDataRef.getWsuId().equals(encryptedDataId))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Return true if a token was encrypted, false otherwise.
     */
    private boolean isTokenEncrypted(Element token, List<WSSecurityEngineResult> encryptedResults) {
        for (WSSecurityEngineResult result : encryptedResults) {
            List<WSDataRef> dataRefs =
                CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            if (dataRefs != null) {
                for (WSDataRef dataRef : dataRefs) {
                    if (token == dataRef.getProtectedElement()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void setSignedElements(SignedElements signedElements) {
        this.signedElements = signedElements;
    }

    public void setEncryptedElements(EncryptedElements encryptedElements) {
        this.encryptedElements = encryptedElements;
    }

    public void setSignedParts(SignedParts signedParts) {
        this.signedParts = signedParts;
    }

    public void setEncryptedParts(EncryptedParts encryptedParts) {
        this.encryptedParts = encryptedParts;
    }

    protected void assertSecurePartsIfTokenNotRequired(
        SupportingTokens supportingToken, AssertionInfoMap aim
    ) {
        String namespace = supportingToken.getName().getNamespaceURI();
        if (supportingToken.getSignedParts() != null) {
            assertSecurePartsIfTokenNotRequired(supportingToken.getSignedParts(),
                                                new QName(namespace, SPConstants.SIGNED_PARTS), aim);
        }
        if (supportingToken.getSignedElements() != null) {
            assertSecurePartsIfTokenNotRequired(supportingToken.getSignedElements(),
                                                new QName(namespace, SPConstants.SIGNED_ELEMENTS), aim);
        }
        if (supportingToken.getEncryptedParts() != null) {
            assertSecurePartsIfTokenNotRequired(supportingToken.getEncryptedParts(),
                                                new QName(namespace, SPConstants.ENCRYPTED_PARTS), aim);
        }
        if (supportingToken.getEncryptedElements() != null) {
            assertSecurePartsIfTokenNotRequired(supportingToken.getEncryptedElements(),
                                                new QName(namespace, SPConstants.ENCRYPTED_ELEMENTS), aim);
        }
    }

    protected void assertSecurePartsIfTokenNotRequired(
        AbstractSecurityAssertion securedPart, QName name, AssertionInfoMap aim
    ) {
        Collection<AssertionInfo> ais = aim.get(name);
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion().equals(securedPart)) {
                    ai.setAsserted(true);
                }
            }
        }
    }
    public boolean isEnforceEncryptedTokens() {
        return enforceEncryptedTokens;
    }
    public void setEnforceEncryptedTokens(boolean enforceEncryptedTokens) {
        this.enforceEncryptedTokens = enforceEncryptedTokens;
    }

    protected void assertDerivedKeys(AbstractToken token, AssertionInfoMap aim) {
        DerivedKeys derivedKeys = token.getDerivedKeys();
        if (derivedKeys != null) {
            PolicyUtils.assertPolicy(aim, new QName(token.getName().getNamespaceURI(), derivedKeys.name()));
        }
    }

    protected static boolean isSamlTokenRequiredForIssuedToken(IssuedToken issuedToken) {
        Element template = issuedToken.getRequestSecurityTokenTemplate();
        if (template != null) {
            Element child = DOMUtils.getFirstElement(template);
            while (child != null) {
                if ("TokenType".equals(child.getLocalName())) {
                    String content = child.getTextContent();
                    return WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(content)
                        || WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(content);
                }
                child = DOMUtils.getNextElement(child);
            }
        }
        return false;
    }

}
