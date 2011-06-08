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

package demo.sts.provider.token;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.common.security.SecurityToken;
import org.apache.cxf.common.security.UsernameToken;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.UseKeyType;
import org.apache.cxf.ws.security.sts.provider.model.secext.KeyIdentifierType;
import org.apache.cxf.ws.security.sts.provider.model.secext.SecurityTokenReferenceType;
import org.apache.cxf.ws.security.sts.provider.model.xmldsig.KeyInfoType;
import org.apache.cxf.ws.security.sts.provider.model.xmldsig.X509DataType;
import org.apache.cxf.ws.security.sts.provider.operation.IssueOperation;
import org.apache.ws.security.WSConstants;
import org.apache.xml.security.utils.Constants;
import org.opensaml.common.xml.SAMLConstants;

import demo.sts.provider.cert.CertificateVerifier;
import demo.sts.provider.cert.CertificateVerifierConfig;

public class SAMLTokenIssueOperation implements IssueOperation {

    private static final org.apache.cxf.ws.security.sts.provider.model.ObjectFactory WS_TRUST_FACTORY 
        = new org.apache.cxf.ws.security.sts.provider.model.ObjectFactory();
    private static final org.apache.cxf.ws.security.sts.provider.model.secext.ObjectFactory WSSE_FACTORY 
        = new org.apache.cxf.ws.security.sts.provider.model.secext.ObjectFactory();

    private static final String SIGN_FACTORY_TYPE = "DOM";
    private static final String JKS_INSTANCE = "JKS";
    private static final String X_509 = "X.509";
    private static final QName TOKEN_TYPE = 
        new QName(WSConstants.WSSE11_NS, WSConstants.TOKEN_TYPE, WSConstants.WSSE11_PREFIX);

    private static final QName QNAME_WST_TOKEN_TYPE = WS_TRUST_FACTORY
            .createTokenType("").getName();

    private List<TokenProvider> tokenProviders;
    private CertificateVerifierConfig certificateVerifierConfig;


    public void setTokenProviders(List<TokenProvider> tokenProviders) {
        this.tokenProviders = tokenProviders;
    }

    public void setCertificateVerifierConfig(
            CertificateVerifierConfig certificateVerifierConfig) {
        this.certificateVerifierConfig = certificateVerifierConfig;
    }

    public RequestSecurityTokenResponseCollectionType issue(
            RequestSecurityTokenType request,
            WebServiceContext context) {

        String tokenType = SAMLConstants.SAML20_NS;
        X509Certificate certificate = null;
        String username = null;

        // parse input arguments
        for (Object requestObject : request.getAny()) {
            // certificate
            try {
                if (certificate == null) {
                    certificate = getCertificateFromRequest(requestObject);
                }
            } catch (CertificateException e) {
                throw new STSException(
                        "Can't extract X509 certificate from request", e);
            }

            // TokenType
            if (requestObject instanceof JAXBElement) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>) requestObject;
                if (QNAME_WST_TOKEN_TYPE.equals(jaxbElement.getName())) {
                    tokenType = (String) jaxbElement.getValue();
                }
            }
        }
        if (certificate == null) {
            if (context == null || context.getMessageContext() == null) {
                throw new STSException("No message context found");
            }
            //find the username
            MessageContext ctx = context.getMessageContext();
            UsernameToken unt = (UsernameToken)ctx.get(SecurityToken.class.getName());
            if (unt != null) {
                username = unt.getName();
            }
        }

        // check input arguments
        if (certificate != null) { // certificate
            try {
                verifyCertificate(certificate);
            } catch (Exception e) {
                throw new STSException(
                        "Can't verify X509 certificate from request", e);
            }
        }

        // create token
        TokenProvider tokenProvider = null;
        for (TokenProvider tp : tokenProviders) {
            if (tokenType.equals(tp.getTokenType())) {
                tokenProvider = tp;
                break;
            }
        }
        if (tokenProvider == null) {
            throw new STSException(
                    "No token provider found for requested token type: "
                            + tokenType);
        }

        Element elementToken = null;

        if (certificate != null) {
            elementToken = tokenProvider.createToken(certificate);
        } else {
            elementToken = tokenProvider.createToken(username);
        }

        String tokenId = tokenProvider.getTokenId(elementToken);
        signSAML(elementToken, tokenId);

        // prepare response
        RequestSecurityTokenResponseType response = wrapAssertionToResponse(
                tokenType, elementToken, tokenId);

        RequestSecurityTokenResponseCollectionType responseCollection = WS_TRUST_FACTORY
                .createRequestSecurityTokenResponseCollectionType();
        responseCollection.getRequestSecurityTokenResponse().add(response);
        return responseCollection;
    }

    private void verifyCertificate(X509Certificate certificate) throws Exception {
        KeyStore ks = KeyStore.getInstance(JKS_INSTANCE);

        ks.load(this.getClass().getResourceAsStream(
                certificateVerifierConfig.getStorePath()),
                certificateVerifierConfig.getStorePwd().toCharArray());
        Set<X509Certificate> trustedRootCerts = new HashSet<X509Certificate>();
        for (String alias : certificateVerifierConfig.getTrustCertAliases()) {
            java.security.cert.Certificate stsCert = ks.getCertificate(alias);
            trustedRootCerts.add((X509Certificate) stsCert);
        }

        CertificateVerifier.verifyCertificate(certificate, trustedRootCerts,
                certificateVerifierConfig.isVerifySelfSignedCert());
    }

    private RequestSecurityTokenResponseType wrapAssertionToResponse(
            String tokenType, Element samlAssertion, String tokenId) {
        RequestSecurityTokenResponseType response = WS_TRUST_FACTORY
                .createRequestSecurityTokenResponseType();

        // TokenType
        JAXBElement<String> jaxbTokenType = WS_TRUST_FACTORY
                .createTokenType(tokenType);
        response.getAny().add(jaxbTokenType);

        // RequestedSecurityToken
        RequestedSecurityTokenType requestedTokenType = WS_TRUST_FACTORY
                .createRequestedSecurityTokenType();
        JAXBElement<RequestedSecurityTokenType> requestedToken = WS_TRUST_FACTORY
                .createRequestedSecurityToken(requestedTokenType);
        requestedTokenType.setAny(samlAssertion);
        response.getAny().add(requestedToken);

        // RequestedAttachedReference
        RequestedReferenceType requestedReferenceType = WS_TRUST_FACTORY
                .createRequestedReferenceType();
        SecurityTokenReferenceType securityTokenReferenceType = WSSE_FACTORY
                .createSecurityTokenReferenceType();
        KeyIdentifierType keyIdentifierType = WSSE_FACTORY
                .createKeyIdentifierType();
        keyIdentifierType.setValue(tokenId);
        JAXBElement<KeyIdentifierType> keyIdentifier = WSSE_FACTORY
                .createKeyIdentifier(keyIdentifierType);
        
        if (WSConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType) 
            || WSConstants.SAML_NS.equals(tokenType)) {
            securityTokenReferenceType.getOtherAttributes().put(
                TOKEN_TYPE, WSConstants.WSS_SAML_TOKEN_TYPE
            );
            keyIdentifierType.setValueType(WSConstants.WSS_SAML_KI_VALUE_TYPE);
        } else if (WSConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
            || WSConstants.SAML2_NS.equals(tokenType)) {
            securityTokenReferenceType.getOtherAttributes().put(
                TOKEN_TYPE, WSConstants.WSS_SAML2_TOKEN_TYPE
            );
            keyIdentifierType.setValueType(WSConstants.WSS_SAML2_KI_VALUE_TYPE);
        }
        
        securityTokenReferenceType.getAny().add(keyIdentifier);
        requestedReferenceType
                .setSecurityTokenReference(securityTokenReferenceType);

        JAXBElement<RequestedReferenceType> requestedAttachedReference = WS_TRUST_FACTORY
                .createRequestedAttachedReference(requestedReferenceType);
        response.getAny().add(requestedAttachedReference);

        // RequestedUnattachedReference
        JAXBElement<RequestedReferenceType> requestedUnattachedReference = WS_TRUST_FACTORY
                .createRequestedUnattachedReference(requestedReferenceType);
        response.getAny().add(requestedUnattachedReference);

        return response;
    }

    private X509Certificate getCertificateFromRequest(Object requestObject) throws CertificateException {
        UseKeyType useKeyType = extractType(requestObject, UseKeyType.class);
        byte[] x509 = null;
        if (null != useKeyType) {
            KeyInfoType keyInfoType = extractType(useKeyType.getAny(),
                    KeyInfoType.class);
            if (null != keyInfoType) {
                for (Object keyInfoContent : keyInfoType.getContent()) {
                    X509DataType x509DataType = extractType(keyInfoContent,
                            X509DataType.class);
                    if (null != x509DataType) {
                        for (Object x509Object : x509DataType
                                .getX509IssuerSerialOrX509SKIOrX509SubjectName()) {
                            x509 = extractType(x509Object, byte[].class);
                            if (null != x509) {
                                break;
                            }
                        }
                    }
                }
            } else {
                Element elementNSImpl = (Element) useKeyType.getAny();
                NodeList x509CertData = elementNSImpl.getElementsByTagNameNS(
                       Constants.SignatureSpecNS, Constants._TAG_X509CERTIFICATE);
                if (x509CertData != null && x509CertData.getLength() > 0) {
                    try {
                        x509 = Base64Utility.decode(x509CertData.item(0)
                                                    .getTextContent());
                    } catch (Exception e) {
                        throw new STSException(e.getMessage(), e);
                    }
                }
            }
            if (x509 != null) {
                CertificateFactory cf = CertificateFactory.getInstance(X_509);
                Certificate certificate = cf
                        .generateCertificate(new ByteArrayInputStream(x509));
                return (X509Certificate) certificate;
            }

        }
        return null;
    }

    private static <T> T extractType(Object param, Class<T> clazz) {
        if (param instanceof JAXBElement) {
            JAXBElement<?> jaxbElement = (JAXBElement<?>) param;
            if (clazz == jaxbElement.getDeclaredType()) {
                return clazz.cast(jaxbElement.getValue());
            }
        }
        return null;
    }


    private void signSAML(Element assertionDocument, String tokenId) {

        InputStream isKeyStore = this.getClass().getResourceAsStream(
                certificateVerifierConfig.getStorePath());

        KeyStoreInfo keyStoreInfo = new KeyStoreInfo(isKeyStore,
                certificateVerifierConfig.getStorePwd(),
                certificateVerifierConfig.getKeySignAlias(),
                certificateVerifierConfig.getKeySignPwd());

        signXML(assertionDocument, tokenId, keyStoreInfo);

    }

    private void signXML(Element target, String refId, KeyStoreInfo keyStoreInfo) {

        org.apache.xml.security.Init.init();

        XMLSignatureFactory signFactory = XMLSignatureFactory
                .getInstance(SIGN_FACTORY_TYPE);
        try {
            DigestMethod method = signFactory.newDigestMethod(
                    DigestMethod.SHA1, null);
            Transform transform = signFactory.newTransform(
                    Transform.ENVELOPED,
                    (TransformParameterSpec) null);
            Reference ref = signFactory.newReference('#' + refId, method,
                    Collections.singletonList(transform), null, null);

            CanonicalizationMethod canonMethod = signFactory
                    .newCanonicalizationMethod(
                            CanonicalizationMethod.EXCLUSIVE,
                            (C14NMethodParameterSpec) null);
            SignatureMethod signMethod = signFactory.newSignatureMethod(
                    SignatureMethod.RSA_SHA1, null);
            SignedInfo si = signFactory.newSignedInfo(canonMethod, signMethod,
                    Collections.singletonList(ref));

            KeyStore.PrivateKeyEntry keyEntry = getKeyEntry(keyStoreInfo);
            if (keyEntry == null) {
                throw new IllegalStateException(
                        "Key is not found in keystore. Alias: "
                                + keyStoreInfo.getAlias());
            }

            KeyInfo ki = getKeyInfo(signFactory, keyEntry);

            DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(),
                    target);

            XMLSignature signature = signFactory.newXMLSignature(si, ki);

            signature.sign(dsc);

        } catch (Exception e) {
            throw new STSException("Cannot sign xml document: "
                    + e.getMessage(), e);
        }
    }

    private PrivateKeyEntry getKeyEntry(KeyStoreInfo keyStoreInfo) throws Exception {

        KeyStore ks = KeyStore.getInstance(JKS_INSTANCE);
        ByteArrayInputStream is = new ByteArrayInputStream(
                keyStoreInfo.getContent());
        ks.load(is, keyStoreInfo.getStorePassword().toCharArray());
        KeyStore.PasswordProtection passwordProtection = new KeyStore.PasswordProtection(
                keyStoreInfo.getKeyPassword().toCharArray());
        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) ks
                .getEntry(keyStoreInfo.getAlias(), passwordProtection);
        return keyEntry;
    }

    private KeyInfo getKeyInfo(XMLSignatureFactory signFactory,
            PrivateKeyEntry keyEntry) {

        X509Certificate cert = (X509Certificate) keyEntry.getCertificate();

        KeyInfoFactory kif = signFactory.getKeyInfoFactory();
        List<Object> x509Content = new ArrayList<Object>();
        x509Content.add(cert.getSubjectX500Principal().getName());
        x509Content.add(cert);
        X509Data xd = kif.newX509Data(x509Content);
        return kif.newKeyInfo(Collections.singletonList(xd));
    }

    public class KeyStoreInfo {

        private byte[] content;
        private String storePassword;
        private String alias;
        private String keyPassword;

        public KeyStoreInfo(InputStream is, String storePassword, String alias,
                String keyPassword) {
            this.content = getBytes(is);
            this.alias = alias;
            this.storePassword = storePassword;
            this.keyPassword = keyPassword;
        }

        public byte[] getContent() {
            return content;
        }

        public String getAlias() {
            return alias;
        }

        public String getStorePassword() {
            return storePassword;
        }

        public String getKeyPassword() {
            return keyPassword;
        }

        private byte[] getBytes(InputStream is) {
            try {
                int len;
                int size = 1024;
                byte[] buf;

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                buf = new byte[size];
                while ((len = is.read(buf, 0, size)) != -1) {
                    bos.write(buf, 0, len);
                }
                buf = bos.toByteArray();
                return buf;
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Cannot read keystore content: " + e.getMessage(), e);
            }
        }

    }
}
