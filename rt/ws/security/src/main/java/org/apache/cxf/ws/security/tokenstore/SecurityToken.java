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

import java.io.Serializable;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.message.token.Reference;
import org.apache.ws.security.util.XmlSchemaDateFormat;


/**
 * 
 */
public class SecurityToken implements Serializable {
    
    private static final long serialVersionUID = -8023092932997444513L;

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
     * WSU Identifier of the token
     */
    private String wsuId;
    
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
    private Properties properties;

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
    private Date created;
    
    /**
     * Expiration time
     */
    private Date expires;
    
    /**
     * Issuer end point address
     */
    private String issuerAddress;
    
    /**
     * If an encrypted key, this contains the sha1 for the key
     */
    private String encrKeySha1Value;
    
    /**
     * A hash code associated with this token. Note that it is not the hashcode of this 
     * token, but a hash corresponding to an association with this token. It could refer
     * to the hash of another SecurityToken which maps to this token. 
     */
    private int associatedHash;
    
    /**
     * The tokenType
     */
    private String tokenType;

    private X509Certificate x509cert;

    private transient Crypto crypto;
    
    /**
     * The principal of this SecurityToken
     */
    private Principal principal;
    
    public SecurityToken() {
        
    }
    
    public SecurityToken(String id) {
        this.id = id;
        if (this.id != null && this.id.length() > 0 && this.id.charAt(0) == '#') {
            this.id = this.id.substring(1);
        }
        createDefaultExpires();
    }

    public SecurityToken(String id, Date created, Date expires) {
        this.id = id;
        if (this.id != null && this.id.length() > 0 && this.id.charAt(0) == '#') {
            this.id = this.id.substring(1);
        }
        this.created = created;
        this.expires = expires;
        if (expires == null) {
            createDefaultExpires();
        }
    }
    
    public SecurityToken(String id,
                 Element tokenElem,
                 Date created,
                 Date expires) {
        this.id = id;
        if (this.id != null && this.id.length() > 0 && this.id.charAt(0) == '#') {
            this.id = this.id.substring(1);
        }
        this.token = cloneElement(tokenElem);
        this.created = created;
        this.expires = expires;
        if (expires == null) {
            createDefaultExpires();
        }
    }

    public SecurityToken(String id,
                 Element tokenElem,
                 Element lifetimeElem) {
        this.id = id;
        if (this.id != null && this.id.length() > 0 && this.id.charAt(0) == '#') {
            this.id = this.id.substring(1);
        }
        this.token = cloneElement(tokenElem);
        if (lifetimeElem != null) {
            processLifeTime(lifetimeElem);
        }
        if (expires == null) {
            createDefaultExpires();
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
            Element createdElem = 
                DOMUtils.getFirstChildWithName(lifetimeElem,
                                                WSConstants.WSU_NS,
                                                WSConstants.CREATED_LN);
            DateFormat zulu = new XmlSchemaDateFormat();
            
            this.created = zulu.parse(DOMUtils.getContent(createdElem));

            Element expiresElem = 
                DOMUtils.getFirstChildWithName(lifetimeElem,
                                                WSConstants.WSU_NS,
                                                WSConstants.EXPIRES_LN);
            this.expires = zulu.parse(DOMUtils.getContent(expiresElem));
        } catch (ParseException e) {
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
    
    public void setId(String id) {
        this.id = id;
        if (this.id != null && this.id.length() > 0 && this.id.charAt(0) == '#') {
            this.id = this.id.substring(1);
        }
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
    public Date getCreated() {
        return created;
    }

    /**
     * @return Returns the expires.
     */
    public Date getExpires() {
        return expires;
    }
    
    /**
     * Return whether this SecurityToken is expired or not
     */
    public boolean isExpired() {
        if (state == State.EXPIRED) {
            return true;
        }
        if (expires != null) {
            Date rightNow = new Date();
            if (expires.before(rightNow)) {
                state = State.EXPIRED;
                return true;
            }
        }
        return false;
    }

    /**
     * @param expires The expires to set.
     */
    public void setExpires(Date expires) {
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
    
    public void setWsuId(String wsuId) {
        this.wsuId = wsuId;
    }
    
    public String getWsuId() {
        if (wsuId != null) {
            return wsuId;
        }
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
    
    /**
     * Set a hash code associated with this token. Note that it is not the hashcode of this 
     * token, but a hash corresponding to an association with this token.
     * @param hash a hash code associated with this token
     */
    public void setAssociatedHash(int hash) {
        associatedHash = hash;
    }
    
    /**
     * Get a hash code associated with this token.
     * @return a hash code associated with this token.
     */
    public int getAssociatedHash() {
        return associatedHash;
    }
    
    /**
     * Set the principal associated with this SecurityToken
     * @param principal the principal associated with this SecurityToken
     */
    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }
    
    /**
     * Get the principal associated with this SecurityToken
     * @return the principal associated with this SecurityToken
     */
    public Principal getPrincipal() {
        return principal;
    }
    
    /**
     * Create a default Expires date 5 minutes in the future
     */
    private void createDefaultExpires() {
        expires = new Date();
        long currentTime = expires.getTime();
        expires.setTime(currentTime + 300L * 1000L);
    }

} 
