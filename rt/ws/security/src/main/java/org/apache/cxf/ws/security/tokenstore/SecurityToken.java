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

package org.apache.cxf.ws.security.tokenstore;

import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Properties;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.message.token.Reference;


/**
 * 
 */
public class SecurityToken {
    public enum State {
        UNKNOWN,
        ISSUED, 
        EXPIRED, 
        CANCELLED, 
        RENEWED
    };
    
    /**
     * Token identifier
     */
    private String id;
    
    /**
     * Current state of the token
     */
    private State state = State.UNKNOWN;
    
    /**
     * The actual token in its current state
     */
    private Element token;
    
    /**
     * The token in its previous state
     */
    private Element previousToken;
    
    /**
     * The RequestedAttachedReference element
     * NOTE : The oasis-200401-wss-soap-message-security-1.0 spec allows 
     * an extensibility mechanism for wsse:SecurityTokenReference and 
     * wsse:Reference. Hence we cannot limit to the 
     * wsse:SecurityTokenReference\wsse:Reference case and only hold the URI and 
     * the ValueType values.
     */
    private Element attachedReference;
    
    /**
     * The RequestedUnattachedReference element
     * NOTE : The oasis-200401-wss-soap-message-security-1.0 spec allows 
     * an extensibility mechanism for wsse:SecurityTokenReference and 
     * wsse:Reference. Hence we cannot limit to the 
     * wsse:SecurityTokenReference\wsse:Reference case and only hold the URI and 
     * the ValueType values.
     */
    private Element unattachedReference;
    
    /**
     * A bag to hold any other properties
     */
    private Properties  properties;

    /**
     * A flag to assist the TokenStorage
     */
    private boolean changed;
    
    /**
     * The secret associated with the Token
     */
    private byte[] secret;
    
    /**
     * Created time
     */
    private Calendar created;
    
    /**
     * Expiration time
     */
    private Calendar expires;
    
    /**
     * Issuer end point address
     */
    private String issuerAddress;
    
    /**
     * If an encrypted key, this contains the sha1 for the key
     */
    private String encrKeySha1Value;
    
    
    /**
     * The tokenType
     */
    private String tokenType;

    private X509Certificate x509cert;

    private Crypto crypto;
    
    public SecurityToken() {
        
    }
    public SecurityToken(String id, Calendar created, Calendar expires) {
        this.id = id;
        this.created = created;
        this.expires = expires;
    }
    
    public SecurityToken(String id,
                 Element tokenElem,
                 Calendar created,
                 Calendar expires) {
        this.id = id;
        this.token = cloneElement(tokenElem);
        this.created = created;
        this.expires = expires;
    }

    public SecurityToken(String id,
                 Element tokenElem,
                 Element lifetimeElem) {
        this.id = id;
        this.token = cloneElement(tokenElem);
        if (lifetimeElem != null) {
            processLifeTime(lifetimeElem);
        }
    }
    private static Element cloneElement(Element el) {
        try {
            W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
            writer.setNsRepairing(true);
            StaxUtils.copy(el, writer);
            return writer.getDocument().getDocumentElement();
        } catch (Exception ex) {
            //ignore
        }
        return el;
    }
    /**
     * @param lifetimeElem
     * @throws TrustException 
     */
    private void processLifeTime(Element lifetimeElem) {
        try {
            DatatypeFactory factory = DatatypeFactory.newInstance();
            
            Element createdElem = 
                DOMUtils.getFirstChildWithName(lifetimeElem,
                                                WSConstants.WSU_NS,
                                                WSConstants.CREATED_LN);
            this.created = factory.newXMLGregorianCalendar(DOMUtils.getContent(createdElem))
                .toGregorianCalendar();

            Element expiresElem = 
                DOMUtils.getFirstChildWithName(lifetimeElem,
                                                WSConstants.WSU_NS,
                                                WSConstants.EXPIRES_LN);
            this.expires = factory.newXMLGregorianCalendar(DOMUtils.getContent(expiresElem))
                .toGregorianCalendar();
        } catch (DatatypeConfigurationException e) {
            //shouldn't happen
        }
    }

    /**
     * @return Returns the changed.
     */
    public boolean isChanged() {
        return changed;
    }

    /**
     * @param chnaged The changed to set.
     */
    public void setChanged(boolean chnaged) {
        this.changed = chnaged;
    }
    
    /**
     * @return Returns the properties.
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * @param properties The properties to set.
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * @return Returns the state.
     */
    public State getState() {
        return state;
    }

    /**
     * @param state The state to set.
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * @return Returns the token.
     */
    public Element getToken() {
        return token;
    }

    /**
     * @param token The token to set.
     */
    public void setToken(Element token) {
        this.token = token;
    }

    /**
     * @return Returns the id.
     */
    public String getId() {
        return id;
    }

    /**
     * @return Returns the presivousToken.
     */
    public Element getPreviousToken() {
        return previousToken;
    }

    /**
     * @param presivousToken The presivousToken to set.
     */
    public void setPreviousToken(Element previousToken) {
        this.previousToken = cloneElement(previousToken);
    }

    /**
     * @return Returns the secret.
     */
    public byte[] getSecret() {
        return secret;
    }

    /**
     * @param secret The secret to set.
     */
    public void setSecret(byte[] secret) {
        this.secret = secret;
    }

    /**
     * @return Returns the attachedReference.
     */
    public Element getAttachedReference() {
        return attachedReference;
    }

    /**
     * @param attachedReference The attachedReference to set.
     */
    public void setAttachedReference(Element attachedReference) {
        if (attachedReference != null) {
            this.attachedReference = cloneElement(attachedReference);
        }
    }

    /**
     * @return Returns the unattachedReference.
     */
    public Element getUnattachedReference() {
        return unattachedReference;
    }

    /**
     * @param unattachedReference The unattachedReference to set.
     */
    public void setUnattachedReference(Element unattachedReference) {
        if (unattachedReference != null) {
            this.unattachedReference = cloneElement(unattachedReference);
        }
    }

    /**
     * @return Returns the created.
     */
    public Calendar getCreated() {
        return created;
    }

    /**
     * @return Returns the expires.
     */
    public Calendar getExpires() {
        return expires;
    }

    /**
     * @param expires The expires to set.
     */
    public void setExpires(Calendar expires) {
        this.expires = expires;
    }

    public String getIssuerAddress() {
        return issuerAddress;
    }

    public void setIssuerAddress(String issuerAddress) {
        this.issuerAddress = issuerAddress;
    }
    

    /**
     * @param sha SHA1 of the encrypted key
     */
    public void setSHA1(String sha) {
        this.encrKeySha1Value = sha;
    }
    
    /** 
     * @return SHA1 value of the encrypted key 
     */
    public String getSHA1() {
        return encrKeySha1Value;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String s) {
        tokenType = s;
    }
    
    
    public String getWsuId() {
        Element elem = getAttachedReference();
        if (elem != null) {
            String t = getIdFromSTR(elem);
            if (t != null) {
                return t;
            }
        }
        elem = getUnattachedReference();
        if (elem != null) {
            String t = getIdFromSTR(elem);
            if (t != null) {
                return t;
            }
        }
        return null;
    }   
    
    public static String getIdFromSTR(Element str) {
        Element child = DOMUtils.getFirstElement(str);
        if (child == null) {
            return null;
        }
        
        if ("KeyInfo".equals(child.getLocalName())
            && WSConstants.SIG_NS.equals(child.getNamespaceURI())) {
            return DOMUtils.getContent(child);
        } else if (Reference.TOKEN.getLocalPart().equals(child.getLocalName())
            && Reference.TOKEN.getNamespaceURI().equals(child.getNamespaceURI())) {
            return child.getAttribute("URI").substring(1);
        }
        return null;
    }
    public void setX509Certificate(X509Certificate cert, Crypto cpt) {
        x509cert = cert;
        crypto = cpt;
    }
    public X509Certificate getX509Certificate() {
        return x509cert;
    }
    public Crypto getCrypto() {
        return crypto;
    }


} 
