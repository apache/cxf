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
import org.w3c.dom.Element;

import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.Lifetime;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.ws.security.CustomTokenPrincipal;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.util.DOM2Writer;
import org.apache.ws.security.util.XmlSchemaDateFormat;


/**
 * Some unit tests for creating SAML Tokens with lifetime
 */
public class SAMLProviderLifetimeTest extends org.junit.Assert {
    
    /**
     * Issue SAML 2 token with a valid requested lifetime
     */
    @org.junit.Test
    public void testSaml2ValidLifetime() throws Exception {
        
        int requestedLifetime = 60;
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);
               
        TokenProviderParameters providerParameters = 
            createProviderParameters(
                WSConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );
        
        // Set expected lifetime to 1 minute
        Date creationTime = new Date();
        Date expirationTime = new Date();
        expirationTime.setTime(creationTime.getTime() + (requestedLifetime * 1000L));
        Lifetime lifetime = new Lifetime();
        XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
        lifetime.setCreated(fmt.format(creationTime));
        lifetime.setExpires(fmt.format(expirationTime));
        providerParameters.getTokenRequirements().setLifetime(lifetime);    
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        assertEquals(providerResponse.getLifetime(), requestedLifetime);
        Element token = providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
    }
    
    
    
    /**
     * Issue SAML 2 token with a lifetime configured in SAMLTokenProvider
     * No specific lifetime requested
     */
    @org.junit.Test
    public void testSaml2ProviderLifetime() throws Exception {
        
        long providerLifetime = 10 * 600L;
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setLifetime(providerLifetime);
        samlTokenProvider.setConditionsProvider(conditionsProvider);
                       
        TokenProviderParameters providerParameters = 
            createProviderParameters(
                WSConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        assertEquals(providerResponse.getLifetime(), providerLifetime);
        Element token = providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
    }
    
    
    /**
     * Issue SAML 2 token with a with a lifetime
     * which exceeds configured maximum lifetime
     */
    @org.junit.Test
    public void testSaml2ExceededConfiguredMaxLifetime() throws Exception {
        
        long maxLifetime = 30 * 60L;  // 30 minutes
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setMaxLifetime(maxLifetime);
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);
                       
        TokenProviderParameters providerParameters = 
            createProviderParameters(
                WSConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );
        
        // Set expected lifetime to 35 minutes
        long requestedLifetime = 35 * 60L;
        Date creationTime = new Date();
        Date expirationTime = new Date();
        expirationTime.setTime(creationTime.getTime() + (requestedLifetime * 1000L));
        Lifetime lifetime = new Lifetime();
        XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
        lifetime.setCreated(fmt.format(creationTime));
        lifetime.setExpires(fmt.format(expirationTime));
        providerParameters.getTokenRequirements().setLifetime(lifetime);         
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML2_TOKEN_TYPE));
        
        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected due to exceeded lifetime");
        } catch (STSException ex) {
            //expected
        }
    }
    
    /**
     * Issue SAML 2 token with a with a lifetime
     * which exceeds default maximum lifetime
     */
    @org.junit.Test
    public void testSaml2ExceededDefaultMaxLifetime() throws Exception {
        
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);
                               
        TokenProviderParameters providerParameters = 
            createProviderParameters(
                WSConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );
        
        // Set expected lifetime to Default max lifetime plus 1
        long requestedLifetime = DefaultConditionsProvider.DEFAULT_MAX_LIFETIME + 1;
        Date creationTime = new Date();
        Date expirationTime = new Date();
        expirationTime.setTime(creationTime.getTime() + (requestedLifetime * 1000L));
        Lifetime lifetime = new Lifetime();
        XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
        lifetime.setCreated(fmt.format(creationTime));
        lifetime.setExpires(fmt.format(expirationTime));
        providerParameters.getTokenRequirements().setLifetime(lifetime);         
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML2_TOKEN_TYPE));
        
        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected due to exceeded lifetime");
        } catch (STSException ex) {
            //expected
        }
    }
    
    /**
     * Issue SAML 2 token with a with a lifetime
     * which exceeds configured maximum lifetime
     * Lifetime reduced to maximum lifetime
     */
    @org.junit.Test
    public void testSaml2ExceededConfiguredMaxLifetimeButUpdated() throws Exception {
        
        long maxLifetime = 30 * 60L;  // 30 minutes
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setMaxLifetime(maxLifetime);
        conditionsProvider.setFailLifetimeExceedance(false);
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);
                       
        TokenProviderParameters providerParameters = 
            createProviderParameters(
                WSConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );
        
        // Set expected lifetime to 35 minutes
        long requestedLifetime = 35 * 60L;
        Date creationTime = new Date();
        Date expirationTime = new Date();
        expirationTime.setTime(creationTime.getTime() + (requestedLifetime * 1000L));
        Lifetime lifetime = new Lifetime();
        XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
        lifetime.setCreated(fmt.format(creationTime));
        lifetime.setExpires(fmt.format(expirationTime));
        providerParameters.getTokenRequirements().setLifetime(lifetime);         
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        assertEquals(providerResponse.getLifetime(), maxLifetime);
        Element token = providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
    }
    
    /**
     * Issue SAML 2 token with a near future Created Lifetime. This should pass as we allow a future
     * dated Lifetime up to 60 seconds to avoid clock skew problems.
     */
    @org.junit.Test
    public void testSaml2NearFutureCreatedLifetime() throws Exception {
        
        int requestedLifetime = 60;
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);
               
        TokenProviderParameters providerParameters = 
            createProviderParameters(
                WSConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );
        
        // Set expected lifetime to 1 minute
        Date creationTime = new Date();
        Date expirationTime = new Date();
        expirationTime.setTime(creationTime.getTime() + (requestedLifetime * 1000L));
        creationTime.setTime(creationTime.getTime() + (10 * 1000L));
        Lifetime lifetime = new Lifetime();
        XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
        lifetime.setCreated(fmt.format(creationTime));
        lifetime.setExpires(fmt.format(expirationTime));
        providerParameters.getTokenRequirements().setLifetime(lifetime);    
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        assertEquals(providerResponse.getLifetime(), 60 - 10);
        Element token = providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
    }
    
    /**
     * Issue SAML 2 token with a future Created Lifetime. This should fail as we only allow a future
     * dated Lifetime up to 60 seconds to avoid clock skew problems.
     */
    @org.junit.Test
    public void testSaml2FarFutureCreatedLifetime() throws Exception {
        
        int requestedLifetime = 60;
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);
               
        TokenProviderParameters providerParameters = 
            createProviderParameters(
                WSConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );
        
        // Set expected lifetime to 1 minute
        Date creationTime = new Date();
        creationTime.setTime(creationTime.getTime() + (60L * 2L * 1000L));
        Date expirationTime = new Date();
        expirationTime.setTime(creationTime.getTime() + (requestedLifetime * 1000L));
        Lifetime lifetime = new Lifetime();
        XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
        lifetime.setCreated(fmt.format(creationTime));
        lifetime.setExpires(fmt.format(expirationTime));
        providerParameters.getTokenRequirements().setLifetime(lifetime);    
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML2_TOKEN_TYPE));
        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected on a Created Element too far in the future");
        } catch (STSException ex) {
            // expected
        }
        
        // Now allow this sort of Created Element
        conditionsProvider.setFutureTimeToLive(60L * 60L);
        
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        Element token = providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
    }
    
    /**
     * Issue SAML 2 token with no Expires element. This will be rejected, but will default to the
     * configured TTL and so the request will pass.
     */
    @org.junit.Test
    public void testSaml2NoExpires() throws Exception {
        
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);
               
        TokenProviderParameters providerParameters = 
            createProviderParameters(
                WSConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );
        
        // Set expected lifetime to 1 minute
        Date creationTime = new Date();
        creationTime.setTime(creationTime.getTime() + (60L * 2L * 1000L));
        Lifetime lifetime = new Lifetime();
        XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
        lifetime.setCreated(fmt.format(creationTime));
        providerParameters.getTokenRequirements().setLifetime(lifetime);    
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML2_TOKEN_TYPE));

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        assertEquals(providerResponse.getLifetime(), conditionsProvider.getLifetime());
        Element token = providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
    }
    
    private TokenProviderParameters createProviderParameters(
            String tokenType, String keyType
    ) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        keyRequirements.setKeyType(keyType);
        parameters.setKeyRequirements(keyRequirements);

        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        parameters.setWebServiceContext(webServiceContext);

        parameters.setAppliesToAddress("http://dummy-service.com/dummy");

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);

        parameters.setEncryptionProperties(new EncryptionProperties());

        return parameters;
    }

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin"
        );
        properties.put("org.apache.ws.security.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.ws.security.crypto.merlin.keystore.file", "stsstore.jks");
        
        return properties;
    }
    
  
    
}
