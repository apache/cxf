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

package org.apache.cxf.sts;

import java.util.List;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.realm.Relationship;
import org.apache.cxf.sts.token.realm.RelationshipResolver;
import org.apache.cxf.sts.token.realm.SAMLRealmCodec;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.crypto.Crypto;

/**
 * This MBean represents the properties associated with the STS. It contains a single operation
 * "configureProperties()" which allows subclasses to perform any custom loading/processing of the
 * properties.
 */
public interface STSPropertiesMBean {

    /**
     * Load/process the CallbackHandler, Crypto objects, etc.
     */
    void configureProperties() throws STSException;

    /**
     * Set the CallbackHandler object.
     * @param callbackHandler the CallbackHandler object.
     */
    void setCallbackHandler(CallbackHandler callbackHandler);

    /**
     * Get the CallbackHandler object.
     * @return the CallbackHandler object.
     */
    CallbackHandler getCallbackHandler();

    /**
     * Set the signature Crypto object
     * @param signatureCrypto the signature Crypto object
     */
    void setSignatureCrypto(Crypto signatureCrypto);

    /**
     * Get the signature Crypto object
     * @return the signature Crypto object
     */
    Crypto getSignatureCrypto();

    /**
     * Set the username/alias to use to sign any issued tokens
     * @param signatureUsername the username/alias to use to sign any issued tokens
     */
    void setSignatureUsername(String signatureUsername);

    /**
     * Get the username/alias to use to sign any issued tokens
     * @return the username/alias to use to sign any issued tokens
     */
    String getSignatureUsername();

    /**
     * Set the encryption Crypto object
     * @param encryptionCrypto the encryption Crypto object
     */
    void setEncryptionCrypto(Crypto encryptionCrypto);

    /**
     * Get the encryption Crypto object
     * @return the encryption Crypto object
     */
    Crypto getEncryptionCrypto();

    /**
     * Set the username/alias to use to encrypt any issued tokens. This is a default value - it
     * can be configured per Service in the ServiceMBean.
     * @param encryptionUsername the username/alias to use to encrypt any issued tokens
     */
    void setEncryptionUsername(String encryptionUsername);

    /**
     * Get the username/alias to use to encrypt any issued tokens. This is a default value - it
     * can be configured per Service in the ServiceMBean
     * @return the username/alias to use to encrypt any issued tokens
     */
    String getEncryptionUsername();

    /**
     * Set the EncryptionProperties to use.
     * @param encryptionProperties the EncryptionProperties to use.
     */
    void setEncryptionProperties(EncryptionProperties encryptionProperties);

    /**
     * Get the EncryptionProperties to use.
     * @return the EncryptionProperties to use.
     */
    EncryptionProperties getEncryptionProperties();

    /**
     * Set the STS issuer name
     * @param issuer the STS issuer name
     */
    void setIssuer(String issuer);

    /**
     * Get the STS issuer name
     * @return the STS issuer name
     */
    String getIssuer();

    /**
     * Set the SignatureProperties to use.
     * @param signatureProperties the SignatureProperties to use.
     */
    void setSignatureProperties(SignatureProperties signatureProperties);

    /**
     * Get the SignatureProperties to use.
     * @return the SignatureProperties to use.
     */
    SignatureProperties getSignatureProperties();

    /**
     * Set the RealmParser object to use.
     * @param realmParser the RealmParser object to use.
     */
    void setRealmParser(RealmParser realmParser);

    /**
     * Get the RealmParser object to use.
     * @return the RealmParser object to use.
     */
    RealmParser getRealmParser();

    /**
     * Set the IdentityMapper object to use.
     * @param identityMapper the IdentityMapper object to use.
     */
    void setIdentityMapper(IdentityMapper identityMapper);

    /**
     * Get the IdentityMapper object to use.
     * @return the IdentityMapper object to use.
     */
    IdentityMapper getIdentityMapper();

    /**
     * Set the list of Relationship objects to use.
     * @param relationships the List<Relationship> object to use.
     */
    void setRelationships(List<Relationship> relationships);

    /**
     * Get the list of Relationship objects to use.
     * @return the List<Relationship> object to use.
     */
    List<Relationship> getRelationships();

    /**
     * Get the RelationshipResolver objects to use.
     * @return the RelationshipResolver object to use.
     */
    RelationshipResolver getRelationshipResolver();

    /**
     * Get the SAML Realm Codec object to use.
     * @return the SAMLRealmCodec object to use.
     */
    SAMLRealmCodec getSamlRealmCodec();

    /**
     * Set the SAML Realm Codec object to use.
     * @param samlRealmCodec the SAMLRealmCodec object to use.
     */
    void setSamlRealmCodec(SAMLRealmCodec samlRealmCodec);

    /**
     * Get whether to validate a client Public Key or Certificate presented as part of a
     * UseKey element. This is true by default.
     */
    boolean isValidateUseKey();

    /**
     * Set whether to validate a client Public Key or Certificate presented as part of a
     * UseKey element. If this is set to true (the default), the public key must be trusted
     * by the Signature Crypto of the STS.
     *
     * @param validateUseKey whether to validate a client UseKey or not.
     */
    void setValidateUseKey(boolean validateUseKey);

}
