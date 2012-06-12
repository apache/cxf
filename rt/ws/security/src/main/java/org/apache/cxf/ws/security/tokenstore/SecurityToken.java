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
    
    private static final long serialVersionUID = 3820740387121650613L;

    /**
     * Token identifier
     */
    private String id;
    
    /**
     * WSU Identifier of the token
     */
    private String wsuId;
    
    /**
     * The actual token in its current state
     */
    private transient Element token;
    
    /**
     * The RequestedAttachedReference element
     * NOTE : The oasis-200401-wss-soap-message-security-1.0 spec allows 
     * an extensibility mechanism for wsse:SecurityTokenReference and 
     * wsse:Reference. Hence we cannot limit to the 
     * wsse:SecurityTokenReference\wsse:Reference case and only hold the URI and 
     * the ValueType values.
     */
    private transient Element attachedReference;
    
    /**
     * The RequestedUnattachedReference element
     * NOTE : The oasis-200401-wss-soap-message-security-1.0 spec allows 
     * an extensibility mechanism for wsse:SecurityTokenReference and 
     * wsse:Reference. Hence we cannot limit to the 
     * wsse:SecurityTokenReference\wsse:Reference case and only hold the URI and 
     * the ValueType values.
     */
    private transient Element unattachedReference;
    
    /**
     * A bag to hold any other properties
     */
    private Properties properties;

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
     * A hash code associated with this token.
     */
    private int tokenHash;
    
    /**
     * This holds the identifier of another SecurityToken which represents a transformed
     * version of this token. 
     */
    private String transformedTokenIdentifier;
    
    /**
     * The tokenType
     */
    private String tokenType;

    private X509Certificate x509cert;

    private transient Crypto crypto;
    
    /**
     * The principal of this SecurityToken
     */
    private transient Principal principal;
    
    public SecurityToken() {
        
    }
    
    public SecurityToken(String id) {
        this.id = id;
        if (this.id != null && this.id.length() > 0 && this.id.charAt(0) == '#') {
            this.id = this.id.substring(1);
        }
    }

    public SecurityToken(String id, Date created, Date expires) {
        this.id = id;
        if (this.id != null && this.id.length() > 0 && this.id.charAt(0) == '#') {
            this.id = this.id.substring(1);
        }
        if (created != null) {
            this.created = new Date(created.getTime());
        }
        if (expires != null) {
            this.expires = new Date(expires.getTime());
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
        if (created != null) {
            this.created = new Date(created.getTime());
        }
        if (expires != null) {
            this.expires = new Date(expires.getTime());
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
     * Get the identifier corresponding to a transformed version of this token
     */
    public String getTransformedTokenIdentifier() {
        return transformedTokenIdentifier;
    }

    /**
     * Set the identifier corresponding to a transformed version of this token
     */
    public void setTransformedTokenIdentifier(String transformedTokenIdentifier) {
        this.transformedTokenIdentifier = transformedTokenIdentifier;
    }
    
    /**
     * Set the id
     */
    public void setId(String id) {
        this.id = id;
        if (this.id != null && this.id.length() > 0 && this.id.charAt(0) == '#') {
            this.id = this.id.substring(1);
        }
    }
    
    /**
     * @return Returns the id.
     */
    public String getId() {
        return id;
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
        if (created == null) {
            return null;
        }
        return (Date)created.clone();
    }

    /**
     * @return Returns the expires.
     */
    public Date getExpires() {
        if (expires == null) {
            return null;
        }
        return (Date)expires.clone();
    }
    
    /**
     * Return whether this SecurityToken is expired or not
     */
    public boolean isExpired() {
        if (expires != null) {
            Date rightNow = new Date();
            if (expires.before(rightNow)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param expires The expires to set.
     */
    public void setExpires(Date expires) {
        if (expires == null) {
            this.expires = null;
        } else {
            this.expires = new Date(expires.getTime());
        }
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
     * Set a hash code associated with this token.
     * @param hash a hash code associated with this token
     */
    public void setTokenHash(int hash) {
        tokenHash = hash;
    }
    
    /**
     * Get a hash code associated with this token.
     * @return a hash code associated with this token.
     */
    public int getTokenHash() {
        return tokenHash;
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
    
} 
