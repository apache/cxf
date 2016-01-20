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

package org.apache.cxf.systest.jaxrs.security.oauth2.grants;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.oauth2.auth.saml.Saml2BearerAuthOutInterceptor;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.jwt.JwtBearerGrant;
import org.apache.cxf.rs.security.oauth2.grants.saml.Saml2BearerGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.saml.Constants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.saml.SAMLUtils;
import org.apache.cxf.rs.security.saml.SAMLUtils.SelfSignInfo;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.OAuth2TestUtils;
import org.apache.cxf.systest.jaxrs.security.oauth2.common.SamlCallbackHandler;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;
import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSOAuth2Test extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerOAuth2.PORT;
    private static final String CRYPTO_RESOURCE_PROPERTIES =
        "org/apache/cxf/systest/jaxrs/security/alice.properties";
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerOAuth2.class, true));
    }
   
    @Test
    public void testSAML2BearerGrant() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2/token";
        WebClient wc = createWebClient(address);
        
        Crypto crypto = new CryptoLoader().loadCrypto(CRYPTO_RESOURCE_PROPERTIES);
        SelfSignInfo signInfo = new SelfSignInfo(crypto, "alice", "password"); 
        
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(false);
        String audienceURI = "https://localhost:" + PORT + "/oauth2/token";
        samlCallbackHandler.setAudience(audienceURI);
        SamlAssertionWrapper assertionWrapper = SAMLUtils.createAssertion(samlCallbackHandler,
                                                                          signInfo);
        Document doc = DOMUtils.newDocument();
        Element assertionElement = assertionWrapper.toDOM(doc);
        String assertion = DOM2Writer.nodeToString(assertionElement);
        
        Saml2BearerGrant grant = new Saml2BearerGrant(assertion);
        ClientAccessToken at = OAuthClientUtils.getAccessToken(wc, 
                                        new Consumer("alice", "alice"), 
                                        grant,
                                        false);
        assertNotNull(at.getTokenKey());
    }
    
    @Test
    public void testSAML2BearerAuthenticationDirect() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2-auth/token";
        WebClient wc = createWebClient(address);
        
        Crypto crypto = new CryptoLoader().loadCrypto(CRYPTO_RESOURCE_PROPERTIES);
        SelfSignInfo signInfo = new SelfSignInfo(crypto, "alice", "password"); 
        
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(true);
        samlCallbackHandler.setIssuer("alice");
        String audienceURI = "https://localhost:" + PORT + "/oauth2-auth/token";
        samlCallbackHandler.setAudience(audienceURI);
        SamlAssertionWrapper assertionWrapper = SAMLUtils.createAssertion(samlCallbackHandler,
                                                                          signInfo);
        Document doc = DOMUtils.newDocument();
        Element assertionElement = assertionWrapper.toDOM(doc);
        String assertion = DOM2Writer.nodeToString(assertionElement);
        
        String encodedAssertion = Base64UrlUtility.encode(assertion);
        
        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_TYPE, Constants.CLIENT_AUTH_SAML2_BEARER);
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_PARAM, encodedAssertion);
        
        ClientAccessToken at = OAuthClientUtils.getAccessToken(wc, 
                                                               new CustomGrant(),
                                                               extraParams);
        assertNotNull(at.getTokenKey());
    }
    
    @Test
    public void testTwoWayTLSAuthentication() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2/token";
        WebClient wc = createWebClient(address);
        
        ClientAccessToken at = OAuthClientUtils.getAccessToken(wc, new CustomGrant());
        assertNotNull(at.getTokenKey());
    }
    
    @Test
    public void testSAML2BearerAuthenticationInterceptor() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2-auth/token";
        WebClient wc = createWebClientWithProps(address);
        
        ClientAccessToken at = OAuthClientUtils.getAccessToken(wc, 
                                                               new CustomGrant());
        assertNotNull(at.getTokenKey());
    }
    
    @Test
    public void testJWTBearerGrant() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2/token";
        WebClient wc = createWebClient(address);
        
        // Create the JWT Token
        String token = OAuth2TestUtils.createToken("resourceOwner", "alice", address, true, true);
        
        JwtBearerGrant grant = new JwtBearerGrant(token);
        ClientAccessToken at = OAuthClientUtils.getAccessToken(wc, 
                                        new Consumer("alice", "alice"), 
                                        grant,
                                        false);
        assertNotNull(at.getTokenKey());
    }
    
    @Test
    public void testJWTBearerAuthenticationDirect() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2-auth-jwt/token";
        WebClient wc = createWebClient(address);
        
        // Create the JWT Token
        String token = OAuth2TestUtils.createToken("resourceOwner", "alice", address, true, true);
        
        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_TYPE,
                        "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_PARAM, token);
        
        ClientAccessToken at = OAuthClientUtils.getAccessToken(wc, 
                                                               new CustomGrant(),
                                                               extraParams);
        assertNotNull(at.getTokenKey());
    }
   
    //
    // Some negative tests for authentication
    //
    
    @Test
    public void testSAML11() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2-auth/token";
        WebClient wc = createWebClient(address);
        
        String audienceURI = "https://localhost:" + PORT + "/oauth2-auth/token";
        String assertion = OAuth2TestUtils.createToken(audienceURI, false, true);
        String encodedAssertion = Base64UrlUtility.encode(assertion);
        
        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_TYPE, Constants.CLIENT_AUTH_SAML2_BEARER);
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_PARAM, encodedAssertion);
        
        try {
            OAuthClientUtils.getAccessToken(wc, new CustomGrant(), extraParams);
            fail("Failure expected on a SAML 1.1 Assertion");
        } catch (OAuthServiceException ex) {
            // expected
        }
    }
    
    @Test
    public void testSAMLAudRestr() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2-auth/token";
        WebClient wc = createWebClient(address);
        
        String audienceURI = "https://localhost:" + PORT + "/oauth2-auth/token2";
        String assertion = OAuth2TestUtils.createToken(audienceURI, true, true);
        String encodedAssertion = Base64UrlUtility.encode(assertion);
        
        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_TYPE, Constants.CLIENT_AUTH_SAML2_BEARER);
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_PARAM, encodedAssertion);
        
        try {
            OAuthClientUtils.getAccessToken(wc, new CustomGrant(), extraParams);
            fail("Failure expected on a bad audience restriction");
        } catch (OAuthServiceException ex) {
            // expected
        }
    }
    
    @Test
    public void testSAMLBadSubjectName() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2-auth/token";
        WebClient wc = createWebClient(address);
        
        String audienceURI = "https://localhost:" + PORT + "/oauth2-auth/token";
        
        // Create the SAML Assertion
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(true);
        samlCallbackHandler.setSubjectName("bob");
        samlCallbackHandler.setAudience(audienceURI);
        
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(samlCallbackHandler, samlCallback);

        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(samlCallback);
        if (samlCallback.isSignAssertion()) {
            samlAssertion.signAssertion(
                samlCallback.getIssuerKeyName(),
                samlCallback.getIssuerKeyPassword(),
                samlCallback.getIssuerCrypto(),
                samlCallback.isSendKeyValue(),
                samlCallback.getCanonicalizationAlgorithm(),
                samlCallback.getSignatureAlgorithm()
            );
        }
        
        String assertion = samlAssertion.assertionToString();
        
        String encodedAssertion = Base64UrlUtility.encode(assertion);
        
        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_TYPE, Constants.CLIENT_AUTH_SAML2_BEARER);
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_PARAM, encodedAssertion);
        
        try {
            OAuthClientUtils.getAccessToken(wc, new CustomGrant(), extraParams);
            fail("Failure expected on a bad subject name");
        } catch (OAuthServiceException ex) {
            // expected
        }
    }
    
    @Test
    public void testSAMLUnsigned() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2-auth/token";
        WebClient wc = createWebClient(address);
        
        String audienceURI = "https://localhost:" + PORT + "/oauth2-auth/token";
        String assertion = OAuth2TestUtils.createToken(audienceURI, true, false);
        String encodedAssertion = Base64UrlUtility.encode(assertion);
        
        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_TYPE, Constants.CLIENT_AUTH_SAML2_BEARER);
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_PARAM, encodedAssertion);
        
        try {
            OAuthClientUtils.getAccessToken(wc, new CustomGrant(), extraParams);
            fail("Failure expected on an unsigned token");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @Test
    public void testSAMLHolderOfKey() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2-auth/token";
        WebClient wc = createWebClient(address);
        
        String audienceURI = "https://localhost:" + PORT + "/oauth2-auth/token";
        
        // Create the SAML Assertion
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(true);
        samlCallbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        samlCallbackHandler.setSubjectName("alice");
        samlCallbackHandler.setAudience(audienceURI);
        
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(samlCallbackHandler, samlCallback);

        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(samlCallback);
        if (samlCallback.isSignAssertion()) {
            samlAssertion.signAssertion(
                samlCallback.getIssuerKeyName(),
                samlCallback.getIssuerKeyPassword(),
                samlCallback.getIssuerCrypto(),
                samlCallback.isSendKeyValue(),
                samlCallback.getCanonicalizationAlgorithm(),
                samlCallback.getSignatureAlgorithm()
            );
        }
        
        String assertion = samlAssertion.assertionToString();
        
        String encodedAssertion = Base64UrlUtility.encode(assertion);
        
        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_TYPE, Constants.CLIENT_AUTH_SAML2_BEARER);
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_PARAM, encodedAssertion);
        
        try {
            OAuthClientUtils.getAccessToken(wc, new CustomGrant(), extraParams);
            fail("Failure expected on a bad subject confirmation method");
        } catch (OAuthServiceException ex) {
            // expected
        }
    }
    
    @Test
    public void testJWTBadSubjectName() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2-auth-jwt/token";
        WebClient wc = createWebClient(address);
        
        // Create the JWT Token
        String token = OAuth2TestUtils.createToken("resourceOwner", "bob", address, true, true);
        
        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_TYPE,
                        "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_PARAM, token);
        
        try {
            OAuthClientUtils.getAccessToken(wc, new CustomGrant(), extraParams);
            fail("Failure expected on a bad subject name");
        } catch (OAuthServiceException ex) {
            // expected
        }
    }
    
    @Test
    public void testJWTUnsigned() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2-auth-jwt/token";
        WebClient wc = createWebClient(address);
        
        // Create the JWT Token
        String token = OAuth2TestUtils.createToken("resourceOwner", "alice", address,
                                                   true, false);
        
        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_TYPE,
                        "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_PARAM, token);
        
        try {
            OAuthClientUtils.getAccessToken(wc, new CustomGrant(), extraParams);
            fail("Failure expected on an unsigned token");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @Test
    public void testJWTNoIssuer() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2-auth-jwt/token";
        WebClient wc = createWebClient(address);
        
        // Create the JWT Token
        String token = OAuth2TestUtils.createToken(null, "alice", address, true, true);
        
        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_TYPE,
                        "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_PARAM, token);
        
        try {
            OAuthClientUtils.getAccessToken(wc, new CustomGrant(), extraParams);
            fail("Failure expected on no issuer");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @Test
    public void testJWTNoExpiry() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2-auth-jwt/token";
        WebClient wc = createWebClient(address);
        
        // Create the JWT Token
        String token = OAuth2TestUtils.createToken("resourceOwner", "alice", 
                                                   address, false, true);
        
        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_TYPE,
                        "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_PARAM, token);
        
        try {
            OAuthClientUtils.getAccessToken(wc, new CustomGrant(), extraParams);
            fail("Failure expected on no expiry");
        } catch (Exception ex) {
            // expected
        }
    }
    
    @Test
    public void testJWTBadAudienceRestriction() throws Exception {
        String address = "https://localhost:" + PORT + "/oauth2-auth-jwt/token";
        WebClient wc = createWebClient(address);
        
        // Create the JWT Token
        String token = OAuth2TestUtils.createToken("resourceOwner", "alice", 
                                                   address + "/badtoken", true, true);
        
        Map<String, String> extraParams = new HashMap<String, String>();
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_TYPE,
                        "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        extraParams.put(Constants.CLIENT_AUTH_ASSERTION_PARAM, token);
        
        try {
            OAuthClientUtils.getAccessToken(wc, new CustomGrant(), extraParams);
            fail("Failure expected on a bad audience restriction");
        } catch (Exception ex) {
            // expected
        }
    }
    
    private WebClient createWebClient(String address) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSOAuth2Test.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        WebClient wc = bean.createWebClient();
        wc.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON);
        return wc;
    }
    
    private WebClient createWebClientWithProps(String address) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSOAuth2Test.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("security.callback-handler", 
                       "org.apache.cxf.systest.jaxrs.security.saml.KeystorePasswordCallback");
        
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(true);
        samlCallbackHandler.setIssuer("alice");
        String audienceURI = "https://localhost:" + PORT + "/oauth2-auth/token";
        samlCallbackHandler.setAudience(audienceURI);
        properties.put("security.saml-callback-handler", samlCallbackHandler);
        
        properties.put("security.signature.username", "alice");
        properties.put("security.signature.properties", CRYPTO_RESOURCE_PROPERTIES);
        bean.setProperties(properties);
        
        bean.getOutInterceptors().add(new Saml2BearerAuthOutInterceptor());
        
        WebClient wc = bean.createWebClient();
        wc.type(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON);
        return wc;
    }
    
    private static class CustomGrant implements AccessTokenGrant {

        private static final long serialVersionUID = -4007538779198315873L;

        @Override
        public String getType() {
            return "custom_grant";
        }

        @Override
        public MultivaluedMap<String, String> toMap() {
            MultivaluedMap<String, String> map = new MetadataMap<String, String>();
            map.putSingle(OAuthConstants.GRANT_TYPE, "custom_grant");
            return map;
        }
        
    }
    
}
