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
package org.apache.cxf.sts.token.renewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.ws.WebServiceContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.Lifetime;
import org.apache.cxf.sts.request.ReceivedKey;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.request.Renewing;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.DefaultConditionsProvider;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.validator.SAMLTokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.util.XmlSchemaDateFormat;
import org.junit.BeforeClass;

/**
 * Some unit tests for renewing a SAML token via the SAMLTokenRenewer with proof of possession enabled
 * (message level, not TLS).
 */
public class SAMLTokenRenewerPOPTest extends org.junit.Assert {
    
    private static TokenStore tokenStore;
    
    @BeforeClass
    public static void init() {
        tokenStore = new DefaultInMemoryTokenStore();
    }
    
    /**
     * Renew a valid SAML1 Assertion
     */
    @org.junit.Test
    public void renewValidSAML1Assertion() throws Exception {
        // Create the Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken = 
            createSAMLAssertion(
                WSConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50000, true, false
            );
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        
        // Validate the Assertion
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();
        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);
        
        assertTrue(samlTokenValidator.canHandleToken(validateTarget));
        
        TokenValidatorResponse validatorResponse = 
                samlTokenValidator.validateToken(validatorParameters);
        assertTrue(validatorResponse != null);
        assertTrue(validatorResponse.getToken() != null);
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);
        
        // Renew the Assertion
        TokenRenewerParameters renewerParameters = new TokenRenewerParameters();
        renewerParameters.setAppliesToAddress("http://dummy-service.com/dummy");
        renewerParameters.setStsProperties(validatorParameters.getStsProperties());
        renewerParameters.setPrincipal(new CustomTokenPrincipal("alice"));
        renewerParameters.setWebServiceContext(validatorParameters.getWebServiceContext());
        renewerParameters.setKeyRequirements(validatorParameters.getKeyRequirements());
        renewerParameters.setTokenRequirements(validatorParameters.getTokenRequirements());
        renewerParameters.setTokenStore(validatorParameters.getTokenStore());
        renewerParameters.setToken(validatorResponse.getToken());
        
        TokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        assertTrue(samlTokenRenewer.canHandleToken(validatorResponse.getToken()));
        
        try {
            samlTokenRenewer.renewToken(renewerParameters);
            fail("Expected failure on lack of proof of possession");
        } catch (Exception ex) {
            // expected
        }
        
        List<WSSecurityEngineResult> signedResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityEngineResult signedResult = new WSSecurityEngineResult(WSConstants.SIGN);
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("myclientkey");
        signedResult.put(
            WSSecurityEngineResult.TAG_X509_CERTIFICATES, crypto.getX509Certificates(cryptoType)
        );
        signedResults.add(signedResult);
        
        List<WSHandlerResult> handlerResults = new ArrayList<>();
        WSHandlerResult handlerResult = 
            new WSHandlerResult(null, signedResults,
                                Collections.singletonMap(WSConstants.SIGN, signedResults));
        handlerResults.add(handlerResult);
        
        WebServiceContext context = validatorParameters.getWebServiceContext();
        context.getMessageContext().put(WSHandlerConstants.RECV_RESULTS, handlerResults);

        // Now successfully renew the token
        TokenRenewerResponse renewerResponse = 
                samlTokenRenewer.renewToken(renewerParameters);
        assertTrue(renewerResponse != null);
        assertTrue(renewerResponse.getToken() != null);
    }
    
    /**
     * Renew a valid SAML1 Assertion
     */
    @org.junit.Test
    public void renewValidSAML1AssertionWrongPOP() throws Exception {
        // Create the Assertion
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken = 
            createSAMLAssertion(
                WSConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler, 50000, true, false
            );
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        
        // Validate the Assertion
        TokenValidator samlTokenValidator = new SAMLTokenValidator();
        TokenValidatorParameters validatorParameters = createValidatorParameters();
        TokenRequirements tokenRequirements = validatorParameters.getTokenRequirements();
        ReceivedToken validateTarget = new ReceivedToken(samlToken);
        tokenRequirements.setValidateTarget(validateTarget);
        validatorParameters.setToken(validateTarget);
        
        assertTrue(samlTokenValidator.canHandleToken(validateTarget));
        
        TokenValidatorResponse validatorResponse = 
                samlTokenValidator.validateToken(validatorParameters);
        assertTrue(validatorResponse != null);
        assertTrue(validatorResponse.getToken() != null);
        assertTrue(validatorResponse.getToken().getState() == STATE.VALID);
        
        // Renew the Assertion
        TokenRenewerParameters renewerParameters = new TokenRenewerParameters();
        renewerParameters.setAppliesToAddress("http://dummy-service.com/dummy");
        renewerParameters.setStsProperties(validatorParameters.getStsProperties());
        renewerParameters.setPrincipal(new CustomTokenPrincipal("alice"));
        renewerParameters.setWebServiceContext(validatorParameters.getWebServiceContext());
        renewerParameters.setKeyRequirements(validatorParameters.getKeyRequirements());
        renewerParameters.setTokenRequirements(validatorParameters.getTokenRequirements());
        renewerParameters.setTokenStore(validatorParameters.getTokenStore());
        renewerParameters.setToken(validatorResponse.getToken());
        
        TokenRenewer samlTokenRenewer = new SAMLTokenRenewer();
        assertTrue(samlTokenRenewer.canHandleToken(validatorResponse.getToken()));
        
        try {
            samlTokenRenewer.renewToken(renewerParameters);
            fail("Expected failure on lack of proof of possession");
        } catch (Exception ex) {
            // expected
        }
        
        List<WSSecurityEngineResult> signedResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityEngineResult signedResult = new WSSecurityEngineResult(WSConstants.SIGN);
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("myservicekey");
        signedResult.put(
            WSSecurityEngineResult.TAG_X509_CERTIFICATES, crypto.getX509Certificates(cryptoType)
        );
        signedResults.add(signedResult);
        
        List<WSHandlerResult> handlerResults = new ArrayList<>();
        WSHandlerResult handlerResult = 
            new WSHandlerResult(null, signedResults,
                                Collections.singletonMap(WSConstants.SIGN, signedResults));
        handlerResults.add(handlerResult);
        
        WebServiceContext context = validatorParameters.getWebServiceContext();
        context.getMessageContext().put(WSHandlerConstants.RECV_RESULTS, handlerResults);

        try {
            samlTokenRenewer.renewToken(renewerParameters);
            fail("Expected failure on wrong signature key");
        } catch (Exception ex) {
            // expected
        }
    }
    
    
    private TokenValidatorParameters createValidatorParameters() throws WSSecurityException {
        TokenValidatorParameters parameters = new TokenValidatorParameters();
        
        TokenRequirements tokenRequirements = new TokenRequirements();
        parameters.setTokenRequirements(tokenRequirements);
        
        KeyRequirements keyRequirements = new KeyRequirements();
        parameters.setKeyRequirements(keyRequirements);
        
        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        parameters.setWebServiceContext(webServiceContext);
        
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
        parameters.setTokenStore(tokenStore);
        
        return parameters;
    }
    
    private Element createSAMLAssertion(
            String tokenType, Crypto crypto, String signatureUsername,
            CallbackHandler callbackHandler, long ttlMs, boolean allowRenewing,
            boolean allowRenewingAfterExpiry
    ) throws WSSecurityException {
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        DefaultConditionsProvider conditionsProvider = new DefaultConditionsProvider();
        conditionsProvider.setAcceptClientLifetime(true);
        samlTokenProvider.setConditionsProvider(conditionsProvider);
        TokenProviderParameters providerParameters = 
            createProviderParameters(
                    tokenType, STSConstants.PUBLIC_KEY_KEYTYPE, crypto, signatureUsername, callbackHandler
            );
        
        Renewing renewing = new Renewing();
        renewing.setAllowRenewing(allowRenewing);
        renewing.setAllowRenewingAfterExpiry(allowRenewingAfterExpiry);
        providerParameters.getTokenRequirements().setRenewing(renewing);

        if (ttlMs != 0) {
            Lifetime lifetime = new Lifetime();
            Date creationTime = new Date();
            Date expirationTime = new Date();
            expirationTime.setTime(creationTime.getTime() + ttlMs);

            XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
            lifetime.setCreated(fmt.format(creationTime));
            lifetime.setExpires(fmt.format(expirationTime));

            providerParameters.getTokenRequirements().setLifetime(lifetime);
        }

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        return providerResponse.getToken();
    }    
    
    private TokenProviderParameters createProviderParameters(
        String tokenType, String keyType, Crypto crypto, 
        String signatureUsername, CallbackHandler callbackHandler
    ) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        keyRequirements.setKeyType(keyType);
        ReceivedKey receivedKey = new ReceivedKey();
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("myclientkey");
        receivedKey.setX509Cert(crypto.getX509Certificates(cryptoType)[0]);
        keyRequirements.setReceivedKey(receivedKey);
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
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setSignatureUsername(signatureUsername);
        stsProperties.setCallbackHandler(callbackHandler);
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);

        parameters.setEncryptionProperties(new EncryptionProperties());
        parameters.setTokenStore(tokenStore);
        
        return parameters;
    }
    
    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "stsstore.jks");
        
        return properties;
    }
    
    
}
