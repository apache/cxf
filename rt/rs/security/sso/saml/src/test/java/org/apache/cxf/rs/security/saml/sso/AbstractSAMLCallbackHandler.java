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

package org.apache.cxf.rs.security.saml.sso;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.ActionBean;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.bean.AuthDecisionStatementBean;
import org.apache.wss4j.common.saml.bean.AuthenticationStatementBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.KeyInfoBean;
import org.apache.wss4j.common.saml.bean.KeyInfoBean.CERT_IDENTIFIER;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.SubjectConfirmationDataBean;
import org.apache.wss4j.common.saml.bean.SubjectLocalityBean;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecEncryptedKey;

/**
 * A base implementation of a Callback Handler for a SAML assertion. By default it creates an
 * authentication assertion.
 */
public abstract class AbstractSAMLCallbackHandler implements CallbackHandler {

    public enum Statement {
        AUTHN, ATTR, AUTHZ
    };

    protected String subjectName;
    protected String subjectQualifier;
    protected String confirmationMethod;
    protected X509Certificate[] certs;
    protected Statement statement = Statement.AUTHN;
    protected CERT_IDENTIFIER certIdentifier = CERT_IDENTIFIER.X509_CERT;
    protected byte[] ephemeralKey;
    protected String issuer;
    protected String subjectNameIDFormat;
    protected String subjectLocalityIpAddress;
    protected String subjectLocalityDnsAddress;
    protected String resource;
    protected List<Object> customAttributeValues;
    protected ConditionsBean conditions;
    protected SubjectConfirmationDataBean subjectConfirmationData;
    protected Instant authnInstant;
    protected Instant sessionNotOnOrAfter;

    public Instant getSessionNotOnOrAfter() {
        return sessionNotOnOrAfter;
    }

    public void setSessionNotOnOrAfter(Instant sessionNotOnOrAfter) {
        this.sessionNotOnOrAfter = sessionNotOnOrAfter;
    }

    public Instant getAuthnInstant() {
        return authnInstant;
    }

    public void setAuthnInstant(Instant authnInstant) {
        this.authnInstant = authnInstant;
    }

    public void setSubjectConfirmationData(SubjectConfirmationDataBean subjectConfirmationData) {
        this.subjectConfirmationData = subjectConfirmationData;
    }

    public void setConditions(ConditionsBean conditionsBean) {
        this.conditions = conditionsBean;
    }

    public void setConfirmationMethod(String confMethod) {
        confirmationMethod = confMethod;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    public void setCertIdentifier(CERT_IDENTIFIER certIdentifier) {
        this.certIdentifier = certIdentifier;
    }

    public void setCerts(X509Certificate[] certs) {
        this.certs = certs;
    }

    public byte[] getEphemeralKey() {
        return ephemeralKey;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setSubjectNameIDFormat(String subjectNameIDFormat) {
        this.subjectNameIDFormat = subjectNameIDFormat;
    }

    public void setSubjectLocality(String ipAddress, String dnsAddress) {
        this.subjectLocalityIpAddress = ipAddress;
        this.subjectLocalityDnsAddress = dnsAddress;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setCustomAttributeValues(List<Object> customAttributeValues) {
        this.customAttributeValues = customAttributeValues;
    }

    /**
     * Note that the SubjectBean parameter should be null for SAML2.0
     */
    protected void createAndSetStatement(SubjectBean subjectBean, SAMLCallback callback) {
        if (statement == Statement.AUTHN) {
            AuthenticationStatementBean authBean = new AuthenticationStatementBean();
            if (subjectBean != null) {
                authBean.setSubject(subjectBean);
            }
            if (subjectLocalityIpAddress != null || subjectLocalityDnsAddress != null) {
                SubjectLocalityBean subjectLocality = new SubjectLocalityBean();
                subjectLocality.setIpAddress(subjectLocalityIpAddress);
                subjectLocality.setDnsAddress(subjectLocalityDnsAddress);
                authBean.setSubjectLocality(subjectLocality);
            }
            authBean.setAuthenticationInstant(authnInstant);
            authBean.setSessionNotOnOrAfter(sessionNotOnOrAfter);
            authBean.setAuthenticationMethod("Password");
            callback.setAuthenticationStatementData(Collections.singletonList(authBean));
        } else if (statement == Statement.ATTR) {
            AttributeStatementBean attrBean = new AttributeStatementBean();
            AttributeBean attributeBean = new AttributeBean();
            if (subjectBean != null) {
                attrBean.setSubject(subjectBean);
                attributeBean.setSimpleName("role");
                attributeBean.setQualifiedName("http://custom-ns");
            } else {
                attributeBean.setQualifiedName("role");
            }
            if (customAttributeValues != null) {
                attributeBean.setAttributeValues(customAttributeValues);
            } else {
                attributeBean.addAttributeValue("user");
            }
            attrBean.setSamlAttributes(Collections.singletonList(attributeBean));
            callback.setAttributeStatementData(Collections.singletonList(attrBean));
        } else {
            AuthDecisionStatementBean authzBean = new AuthDecisionStatementBean();
            if (subjectBean != null) {
                authzBean.setSubject(subjectBean);
            }
            ActionBean actionBean = new ActionBean();
            actionBean.setContents("Read");
            authzBean.setActions(Collections.singletonList(actionBean));
            authzBean.setResource("endpoint");
            authzBean.setDecision(AuthDecisionStatementBean.Decision.PERMIT);
            authzBean.setResource(resource);
            callback.setAuthDecisionStatementData(Collections.singletonList(authzBean));
        }
    }

    protected KeyInfoBean createKeyInfo() throws Exception {
        KeyInfoBean keyInfo = new KeyInfoBean();
        if (statement == Statement.AUTHN) {
            keyInfo.setCertificate(certs[0]);
            keyInfo.setCertIdentifer(certIdentifier);
        } else if (statement == Statement.ATTR) {
            // Build a new Document
            DocumentBuilderFactory docBuilderFactory =
                DocumentBuilderFactory.newInstance();
            docBuilderFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            // Create an Encrypted Key
            WSSecEncryptedKey encrKey = new WSSecEncryptedKey(doc);
            encrKey.setKeyIdentifierType(WSConstants.ISSUER_SERIAL);
            encrKey.setUseThisCert(certs[0]);

            KeyGenerator keyGen = KeyUtils.getKeyGenerator(WSConstants.AES_128);
            SecretKey symmetricKey = keyGen.generateKey();

            encrKey.prepare(null, symmetricKey);
            ephemeralKey = symmetricKey.getEncoded();
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
        }
        return keyInfo;
    }
}
