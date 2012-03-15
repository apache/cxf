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

import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSUtils;

import org.apache.ws.security.conversation.ConversationConstants;
import org.apache.ws.security.conversation.ConversationException;
import org.apache.ws.security.message.token.SecurityContextToken;

/**
 * A TokenProvider implementation that provides a SecurityContextToken.
 */
public class SCTProvider implements TokenProvider {
    
    private static final Logger LOG = LogUtils.getL7dLogger(SCTProvider.class);
    private boolean returnEntropy = true;
    private long lifetime = 300L;
    
    /**
     * Return the lifetime of the generated SCT
     * @return the lifetime of the generated SCT
     */
    public long getLifetime() {
        return lifetime;
    }

    /**
     * Set the lifetime of the generated SCT
     * @param lifetime the lifetime of the generated SCT
     */
    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
    }

    /**
     * Return true if this TokenProvider implementation is capable of providing a token
     * that corresponds to the given TokenType.
     */
    public boolean canHandleToken(String tokenType) {
        return canHandleToken(tokenType, null);
    }

    /**
     * Return true if this TokenProvider implementation is capable of providing a token
     * that corresponds to the given TokenType in a given realm. The realm is ignored in this 
     * token provider.
     */
    public boolean canHandleToken(String tokenType, String realm) {
        if (STSUtils.TOKEN_TYPE_SCT_05_02.equals(tokenType) 
            || STSUtils.TOKEN_TYPE_SCT_05_12.equals(tokenType)) {
            return true;
        }
        return false;
    }
        
    /**
     * Set whether Entropy is returned to the client or not
     * @param returnEntropy whether Entropy is returned to the client or not
     */
    public void setReturnEntropy(boolean returnEntropy) {
        this.returnEntropy = returnEntropy;
    }

    /**
     * Get whether Entropy is returned to the client or not
     * @return whether Entropy is returned to the client or not
     */
    public boolean isReturnEntropy() {
        return returnEntropy;
    }
    
    /**
     * Create a token given a TokenProviderParameters
     */
    public TokenProviderResponse createToken(TokenProviderParameters tokenParameters) {
        TokenRequirements tokenRequirements = tokenParameters.getTokenRequirements();
        LOG.fine("Handling token of type: " + tokenRequirements.getTokenType());
        
        if (tokenParameters.getTokenStore() == null) {
            LOG.log(Level.FINE, "A cache must be configured to use the SCTProvider");
            throw new STSException("Can't serialize SCT", STSException.REQUEST_FAILED);
        }

        SymmetricKeyHandler keyHandler = new SymmetricKeyHandler(tokenParameters);
        keyHandler.createSymmetricKey();
        
        try {
            Document doc = DOMUtils.createDocument();
            SecurityContextToken sct =
                new SecurityContextToken(getWSCVersion(tokenRequirements.getTokenType()), doc);
    
            TokenProviderResponse response = new TokenProviderResponse();
            response.setToken(sct.getElement());
            response.setTokenId(sct.getIdentifier());
            if (returnEntropy) {
                response.setEntropy(keyHandler.getEntropyBytes());
            }
            long keySize = keyHandler.getKeySize();
            response.setKeySize(keySize);
            response.setComputedKey(keyHandler.isComputedKey());
            
            // putting the secret key into the cache
            Date expires = null;
            if (lifetime > 0) {
                expires = new Date();
                long currentTime = expires.getTime();
                expires.setTime(currentTime + (lifetime * 1000L));
            }
            
            SecurityToken token = new SecurityToken(sct.getIdentifier(), null, expires);
            token.setSecret(keyHandler.getSecret());
            token.setPrincipal(tokenParameters.getPrincipal());
            if (tokenParameters.getRealm() != null) {
                Properties props = token.getProperties();
                if (props == null) {
                    props = new Properties();
                }
                props.setProperty(STSConstants.TOKEN_REALM, tokenParameters.getRealm());
                token.setProperties(props);
            }
            if (lifetime > 0) {
                Integer lifetimeInteger = new Integer(Long.valueOf(lifetime).intValue());
                tokenParameters.getTokenStore().add(token, lifetimeInteger);
            } else {
                tokenParameters.getTokenStore().add(token);
            }

            // Create the references
            TokenReference attachedReference = new TokenReference();
            attachedReference.setIdentifier(sct.getID());
            attachedReference.setUseDirectReference(true);
            attachedReference.setWsseValueType(tokenRequirements.getTokenType());
            response.setAttachedReference(attachedReference);
            
            TokenReference unAttachedReference = new TokenReference();
            unAttachedReference.setIdentifier(sct.getIdentifier());
            unAttachedReference.setUseDirectReference(true);
            unAttachedReference.setWsseValueType(tokenRequirements.getTokenType());
            response.setUnattachedReference(unAttachedReference);
            
            response.setLifetime(lifetime);
            
            return response;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "", e);
            throw new STSException("Can't serialize SCT", e, STSException.REQUEST_FAILED);
        }
    }
    
    /**
     * Get the Secure Conversation version from the TokenType parameter
     */
    private static int getWSCVersion(String tokenType) throws ConversationException {
        if (tokenType == null) {
            return ConversationConstants.DEFAULT_VERSION;
        }

        if (tokenType.startsWith(ConversationConstants.WSC_NS_05_02)) {
            return ConversationConstants.getWSTVersion(ConversationConstants.WSC_NS_05_02);
        } else if (tokenType.startsWith(ConversationConstants.WSC_NS_05_12)) {
            return ConversationConstants.getWSTVersion(ConversationConstants.WSC_NS_05_12);
        } else {
            throw new ConversationException("unsupportedSecConvVersion");
        }
    }
    
    
}
