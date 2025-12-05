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
package org.apache.cxf.sts.token.provider;

import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedCredential;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.UsernameTokenPrincipal;
import org.apache.wss4j.common.saml.bean.KeyInfoBean;
import org.apache.wss4j.common.saml.bean.KeyInfoBean.CERT_IDENTIFIER;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.message.WSSecEncryptedKey;

/**
 * A default implementation of SubjectProvider to create a SAML Assertion. The Subject name is the name
 * of the current principal, the subject name qualifier is a default URL that can be configured, and the
 * subject confirmation method is created according to the token type and key type. If the Subject
 * Confirmation Method is SymmetricKey or PublicKey, the appropriate KeyInfoBean object is created and
 * attached to the Subject.
 */
public class DefaultSubjectProvider implements SubjectProvider {

    private static final Logger LOG = LogUtils.getL7dLogger(DefaultSubjectProvider.class);
    private String subjectNameQualifier = "http://cxf.apache.org/sts";
    private String subjectNameIDFormat;

    /**
     * Set the SubjectNameQualifier.
     */
    public void setSubjectNameQualifier(String subjectNameQualifier) {
        this.subjectNameQualifier = subjectNameQualifier;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Setting Subject Name Qualifier: " + subjectNameQualifier);
        }
    }

    /**
     * Set the SubjectNameIDFormat.
     */
    public void setSubjectNameIDFormat(String subjectNameIDFormat) {
        this.subjectNameIDFormat = subjectNameIDFormat;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Setting Subject Name format: " + subjectNameIDFormat);
        }
    }

    /**
     * Get a SubjectBean object.
     */
    public SubjectBean getSubject(SubjectProviderParameters subjectProviderParameters) {

        // 1. Get the principal
        Principal principal = getPrincipal(subjectProviderParameters);
        if (principal == null) {
            LOG.fine("Error in getting principal");
            throw new STSException("Error in getting principal", STSException.REQUEST_FAILED);
        }

        // 2. Create the SubjectBean using the principal
        SubjectBean subjectBean = createSubjectBean(principal, subjectProviderParameters);

        // 3. Create the KeyInfoBean and set it on the SubjectBean
        KeyInfoBean keyInfo = createKeyInfo(subjectProviderParameters);
        subjectBean.setKeyInfo(keyInfo);

        return subjectBean;
    }

    /**
     * Get the Principal (which is used as the Subject). By default, we check the following (in order):
     *  - A valid OnBehalfOf principal
     *  - A valid principal associated with a token received as ValidateTarget
     *  - The principal associated with the request. We don't need to check to see if it is "valid" here, as it
     *    is not parsed by the STS (but rather the WS-Security layer).
     */
    protected Principal getPrincipal(SubjectProviderParameters subjectProviderParameters) {
        TokenProviderParameters providerParameters = subjectProviderParameters.getProviderParameters();

        Principal principal = null;
        //TokenValidator in IssueOperation has validated the ReceivedToken
        //if validation was successful, the principal was set in ReceivedToken
        if (providerParameters.getTokenRequirements().getOnBehalfOf() != null) {
            ReceivedToken receivedToken = providerParameters.getTokenRequirements().getOnBehalfOf();
            if (receivedToken.getState() == STATE.VALID) {
                principal = receivedToken.getPrincipal();
            }
        } else if (providerParameters.getTokenRequirements().getValidateTarget() != null) {
            ReceivedToken receivedToken = providerParameters.getTokenRequirements().getValidateTarget();
            if (receivedToken.getState() == STATE.VALID) {
                principal = receivedToken.getPrincipal();
            }
        } else {
            principal = providerParameters.getPrincipal();
        }

        return principal;
    }

    /**
     * Create the SubjectBean using the specified principal.
     */
    protected SubjectBean createSubjectBean(
        Principal principal, SubjectProviderParameters subjectProviderParameters
    ) {
        TokenProviderParameters providerParameters = subjectProviderParameters.getProviderParameters();
        TokenRequirements tokenRequirements = providerParameters.getTokenRequirements();
        KeyRequirements keyRequirements = providerParameters.getKeyRequirements();

        String tokenType = tokenRequirements.getTokenType();
        String keyType = keyRequirements.getKeyType();
        String confirmationMethod = getSubjectConfirmationMethod(tokenType, keyType);

        String subjectName = principal.getName();
        String localSubjectNameIDFormat = subjectNameIDFormat;
        if (SAML2Constants.NAMEID_FORMAT_UNSPECIFIED.equals(localSubjectNameIDFormat)
            && principal instanceof X500Principal) {
            // Just use the "cn" instead of the entire DN
            try {
                LdapName ln = new LdapName(principal.getName());

                for (Rdn rdn : ln.getRdns()) {
                    if ("CN".equalsIgnoreCase(rdn.getType()) && rdn.getValue() instanceof String) {
                        subjectName = (String)rdn.getValue();
                        break;
                    }
                }
            } catch (Throwable ex) {
                subjectName = principal.getName();
                //Ignore, not X500 compliant thus use the whole string as the value
            }
        } else if (!SAML2Constants.NAMEID_FORMAT_UNSPECIFIED.equals(localSubjectNameIDFormat)) {
            /* Set subjectNameIDFormat correctly based on type of principal
                unless already set to some value other than unspecified */
            if (principal instanceof UsernameTokenPrincipal) {
                localSubjectNameIDFormat = SAML2Constants.NAMEID_FORMAT_PERSISTENT;
            } else if (principal instanceof X500Principal) {
                localSubjectNameIDFormat = SAML2Constants.NAMEID_FORMAT_X509_SUBJECT_NAME;
            } else if (principal instanceof KerberosPrincipal) {
                localSubjectNameIDFormat = SAML2Constants.NAMEID_FORMAT_KERBEROS;
            } else if (localSubjectNameIDFormat == null) {
                localSubjectNameIDFormat = SAML2Constants.NAMEID_FORMAT_UNSPECIFIED;
            }
        }

        SubjectBean subjectBean =
            new SubjectBean(subjectName, subjectNameQualifier, confirmationMethod);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Creating new subject with principal name: " + principal.getName());
        }
        subjectBean.setSubjectNameIDFormat(localSubjectNameIDFormat);

        return subjectBean;
    }

    /**
     * Get the SubjectConfirmation method given a tokenType and keyType
     */
    protected String getSubjectConfirmationMethod(String tokenType, String keyType) {
        if (WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
            || WSS4JConstants.SAML_NS.equals(tokenType)) {
            if (STSConstants.SYMMETRIC_KEY_KEYTYPE.equals(keyType)
                || STSConstants.PUBLIC_KEY_KEYTYPE.equals(keyType)) {
                return SAML1Constants.CONF_HOLDER_KEY;
            }
            return SAML1Constants.CONF_BEARER;
        }
        if (STSConstants.SYMMETRIC_KEY_KEYTYPE.equals(keyType)
            || STSConstants.PUBLIC_KEY_KEYTYPE.equals(keyType)) {
            return SAML2Constants.CONF_HOLDER_KEY;
        }
        return SAML2Constants.CONF_BEARER;
    }

    /**
     * Create and return the KeyInfoBean to be inserted into the SubjectBean
     */
    protected KeyInfoBean createKeyInfo(SubjectProviderParameters subjectProviderParameters) {
        TokenProviderParameters providerParameters = subjectProviderParameters.getProviderParameters();
        KeyRequirements keyRequirements = providerParameters.getKeyRequirements();
        STSPropertiesMBean stsProperties = providerParameters.getStsProperties();

        String keyType = keyRequirements.getKeyType();

        if (STSConstants.SYMMETRIC_KEY_KEYTYPE.equals(keyType)) {
            Crypto crypto = stsProperties.getEncryptionCrypto();

            EncryptionProperties encryptionProperties = providerParameters.getEncryptionProperties();
            String encryptionName = encryptionProperties.getEncryptionName();
            if (encryptionName == null) {
                // Fall back on the STS encryption name
                encryptionName = stsProperties.getEncryptionUsername();
            }
            if (encryptionName == null) {
                LOG.fine("No encryption Name is configured for Symmetric KeyType");
                throw new STSException("No Encryption Name is configured", STSException.REQUEST_FAILED);
            }

            final CryptoType cryptoType;

            // Check for using of service endpoint (AppliesTo) as certificate identifier
            if (STSConstants.USE_ENDPOINT_AS_CERT_ALIAS.equals(encryptionName)) {
                if (providerParameters.getAppliesToAddress() == null) {
                    throw new STSException("AppliesTo is not initilaized for encryption name "
                                           + STSConstants.USE_ENDPOINT_AS_CERT_ALIAS);
                }
                cryptoType = new CryptoType(CryptoType.TYPE.ENDPOINT);
                cryptoType.setEndpoint(providerParameters.getAppliesToAddress());
            } else {
                cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
                cryptoType.setAlias(encryptionName);
            }

            try {
                X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
                if ((certs == null) || (certs.length == 0)) {
                    throw new STSException("Encryption certificate is not found for alias: " + encryptionName);
                }
                Document doc = subjectProviderParameters.getDoc();
                byte[] secret = subjectProviderParameters.getSecret();
                return createEncryptedKeyKeyInfo(certs[0], secret, doc, encryptionProperties, crypto);
            } catch (WSSecurityException ex) {
                LOG.log(Level.WARNING, "", ex);
                throw new STSException(ex.getMessage(), ex);
            }
        } else if (STSConstants.PUBLIC_KEY_KEYTYPE.equals(keyType)) {
            ReceivedCredential receivedCredential = keyRequirements.getReceivedCredential();

            // Validate UseKey trust
            if (stsProperties.isValidateUseKey() && stsProperties.getSignatureCrypto() != null) {
                if (receivedCredential.getX509Cert() != null) {
                    try {
                        Collection<Pattern> constraints = Collections.emptyList();
                        stsProperties.getSignatureCrypto().verifyTrust(
                            new X509Certificate[]{receivedCredential.getX509Cert()}, false, constraints, null);
                    } catch (WSSecurityException e) {
                        LOG.log(Level.FINE, "Error in trust validation of UseKey: ", e);
                        throw new STSException("Error in trust validation of UseKey", STSException.REQUEST_FAILED);
                    }
                }
                if (receivedCredential.getPublicKey() != null) {
                    try {
                        stsProperties.getSignatureCrypto().verifyTrust(receivedCredential.getPublicKey());
                    } catch (WSSecurityException e) {
                        LOG.log(Level.FINE, "Error in trust validation of UseKey: ", e);
                        throw new STSException("Error in trust validation of UseKey", STSException.REQUEST_FAILED);
                    }
                }
            }

            return createPublicKeyKeyInfo(receivedCredential.getX509Cert(), receivedCredential.getPublicKey());
        }

        return null;
    }

    /**
     * Create a KeyInfoBean that contains an X.509 certificate or Public Key
     */
    protected static KeyInfoBean createPublicKeyKeyInfo(X509Certificate certificate, PublicKey publicKey) {
        KeyInfoBean keyInfo = new KeyInfoBean();

        if (certificate != null) {
            keyInfo.setCertificate(certificate);
            keyInfo.setCertIdentifer(CERT_IDENTIFIER.X509_CERT);
        } else if (publicKey != null) {
            keyInfo.setPublicKey(publicKey);
            keyInfo.setCertIdentifer(CERT_IDENTIFIER.KEY_VALUE);
        }

        return keyInfo;
    }

    /**
     * Create an EncryptedKey KeyInfo.
     */
    protected static KeyInfoBean createEncryptedKeyKeyInfo(
        X509Certificate certificate,
        byte[] secret,
        Document doc,
        EncryptionProperties encryptionProperties,
        Crypto encryptionCrypto
    ) throws WSSecurityException {
        KeyInfoBean keyInfo = new KeyInfoBean();

        // Create an EncryptedKey
        WSSecEncryptedKey encrKey = new WSSecEncryptedKey(doc);
        encrKey.setKeyIdentifierType(encryptionProperties.getKeyIdentifierType());
        encrKey.setUseThisCert(certificate);
        encrKey.setKeyEncAlgo(encryptionProperties.getKeyWrapAlgorithm());

        final SecretKey symmetricKey;
        if (secret != null) {
            symmetricKey = KeyUtils.prepareSecretKey(encryptionProperties.getEncryptionAlgorithm(), secret);
        } else {
            KeyGenerator keyGen = KeyUtils.getKeyGenerator(encryptionProperties.getEncryptionAlgorithm());
            symmetricKey = keyGen.generateKey();
        }

        encrKey.prepare(encryptionCrypto, symmetricKey);
        Element encryptedKeyElement = encrKey.getEncryptedKeyElement();

        // Append the EncryptedKey to a KeyInfo element
        Element keyInfoElement =
            doc.createElementNS(
                WSS4JConstants.SIG_NS, WSS4JConstants.SIG_PREFIX + ":" + WSS4JConstants.KEYINFO_LN
            );
        keyInfoElement.setAttributeNS(
            WSS4JConstants.XMLNS_NS, "xmlns:" + WSS4JConstants.SIG_PREFIX, WSS4JConstants.SIG_NS
        );
        keyInfoElement.appendChild(encryptedKeyElement);

        keyInfo.setElement(keyInfoElement);

        return keyInfo;
    }
}
