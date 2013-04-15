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
package org.apache.cxf.ws.security.wss4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.junit.Test;


/**
 * In these test-cases, the client is using StaX and the service is using DOM.
 */
public class StaxToDOMRoundTripTest extends AbstractSecurityTest {
    
    @Test
    public void testUsernameTokenText() throws Exception {
        // Create + configure service
        Service service = createService();
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        inProperties.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        inProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new TestPwdCallback());
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(new XMLSecurityConstants.Action[]{WSSConstants.USERNAMETOKEN});
        properties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_TEXT);
        properties.setTokenUser("username");
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
        
        // Negative test for wrong password type
        service.getInInterceptors().remove(inInterceptor);
        inProperties.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
        inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        try {
            echo.echo("test");
            fail("Failure expected on the wrong password type");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
            String error = "The security token could not be authenticated or authorized";
            assertTrue(ex.getMessage().contains(error));
        }
    }
    
    @Test
    public void testUsernameTokenDigest() throws Exception {
        // Create + configure service
        Service service = createService();
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        inProperties.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
        inProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new TestPwdCallback());
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(new XMLSecurityConstants.Action[]{WSSConstants.USERNAMETOKEN});
        properties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_DIGEST);
        properties.setTokenUser("username");
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
        
        // Negative test for wrong password type
        service.getInInterceptors().remove(inInterceptor);
        inProperties.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        try {
            echo.echo("test");
            fail("Failure expected on the wrong password type");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
            String error = "The security token could not be authenticated or authorized";
            assertTrue(ex.getMessage().contains(error));
        }
    }
    
    @Test
    public void testEncrypt() throws Exception {
        // Create + configure service
        Service service = createService();
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT);
        inProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(WSHandlerConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(new XMLSecurityConstants.Action[]{WSSConstants.ENCRYPT});
        properties.setEncryptionUser("myalias");
        
        Properties cryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setEncryptionCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testEncryptionAlgorithms() throws Exception {
        // Create + configure service
        Service service = createService();
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT);
        inProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(WSHandlerConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(new XMLSecurityConstants.Action[]{WSSConstants.ENCRYPT});
        properties.setEncryptionUser("myalias");
        
        Properties cryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setEncryptionCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        properties.setEncryptionKeyTransportAlgorithm("http://www.w3.org/2001/04/xmlenc#rsa-1_5");
        properties.setEncryptionSymAlgorithm("http://www.w3.org/2001/04/xmlenc#tripledes-cbc");
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);
        
        try {
            echo.echo("test");
            fail("Failure expected as RSA v1.5 is not allowed by default");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
        
        inProperties.put(WSHandlerConstants.ALLOW_RSA15_KEY_TRANSPORT_ALGORITHM, "true");
        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testEncryptUsernameToken() throws Exception {
        // Create + configure service
        Service service = createService();
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(
            WSHandlerConstants.ACTION, 
            WSHandlerConstants.USERNAME_TOKEN + " " + WSHandlerConstants.ENCRYPT
        );
        inProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(WSHandlerConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(
            new XMLSecurityConstants.Action[]{WSSConstants.USERNAMETOKEN, WSSConstants.ENCRYPT}
        );
        properties.addEncryptionPart(
            new SecurePart(new QName(WSSConstants.NS_WSSE10, "UsernameToken"), SecurePart.Modifier.Element)
        );
        properties.setEncryptionUser("myalias");
        properties.setTokenUser("username");
        
        Properties cryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setEncryptionCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testSignature() throws Exception {
        // Create + configure service
        Service service = createService();
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        inProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(WSHandlerConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(new XMLSecurityConstants.Action[]{WSSConstants.SIGNATURE});
        properties.setSignatureUser("myalias");
        
        Properties cryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testTimestamp() throws Exception {
        // Create + configure service
        Service service = createService();
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.TIMESTAMP);
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(new XMLSecurityConstants.Action[]{WSSConstants.TIMESTAMP});
        
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
        
        // Negative test for no Timestamp
        service.getInInterceptors().remove(inInterceptor);
        inProperties.put(WSHandlerConstants.ACTION, "");
        inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        try {
            echo.echo("test");
            fail("Failure expected on no Timestamp");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
            String error = "An error was discovered";
            assertTrue(ex.getMessage().contains(error));
        }
    }
    
    @Test
    public void testSignatureTimestamp() throws Exception {
        // Create + configure service
        Service service = createService();
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(
            WSHandlerConstants.ACTION, 
            WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.TIMESTAMP
        );
        inProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(WSHandlerConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(
            new XMLSecurityConstants.Action[]{WSSConstants.SIGNATURE, WSSConstants.TIMESTAMP}
        );
        properties.addSignaturePart(
            new SecurePart(new QName(WSSConstants.NS_WSSE10, "Timestamp"), SecurePart.Modifier.Element)
        );
        properties.addSignaturePart(
            new SecurePart(new QName(WSSConstants.NS_SOAP11, "Body"), SecurePart.Modifier.Element)
        );
        properties.setSignatureUser("myalias");
        
        Properties cryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testSignaturePKI() throws Exception {
        // Create + configure service
        Service service = createService();
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        inProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        inProperties.put(WSHandlerConstants.SIG_VER_PROP_FILE, "cxfca.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(
            new XMLSecurityConstants.Action[]{WSSConstants.SIGNATURE}
        );
        properties.setSignatureUser("alice");
        
        Properties cryptoProperties = 
            CryptoFactory.getProperties("alice.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new KeystorePasswordCallback());
        properties.setUseSingleCert(true);
        properties.setSignatureKeyIdentifier(
            WSSecurityTokenConstants.KeyIdentifier_SecurityTokenDirectReference
        );
        
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testEncryptSignature() throws Exception {
        // Create + configure service
        Service service = createService();
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(
            WSHandlerConstants.ACTION, 
            WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT
        );
        inProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(WSHandlerConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        inProperties.put(WSHandlerConstants.DEC_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(
            new XMLSecurityConstants.Action[]{WSSConstants.ENCRYPT, WSSConstants.SIGNATURE}
        );
        properties.setEncryptionUser("myalias");
        properties.setSignatureUser("myalias");
        
        Properties cryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setEncryptionCryptoProperties(cryptoProperties);
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testSignatureConfirmation() throws Exception {
        // Create + configure service
        Service service = createService();
        
        Map<String, Object> inProperties = new HashMap<String, Object>();
        inProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        inProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new TestPwdCallback());
        inProperties.put(WSHandlerConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);
        service.getInInterceptors().add(inInterceptor);
        
        Map<String, Object> outProperties = new HashMap<String, Object>();
        outProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        outProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new TestPwdCallback());
        outProperties.put(WSHandlerConstants.SIG_PROP_FILE, "outsecurity.properties");
        outProperties.put(WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION, "true");
        outProperties.put(WSHandlerConstants.USER, "myalias");
        
        WSS4JOutInterceptor domOhandler = new WSS4JOutInterceptor(outProperties);
        service.getOutInterceptors().add(domOhandler);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(new XMLSecurityConstants.Action[]{WSSConstants.SIGNATURE});
        properties.setSignatureUser("myalias");
        
        Properties cryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(cryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);
        
        WSSSecurityProperties staxInProperties = new WSSSecurityProperties();
        staxInProperties.setCallbackHandler(new TestPwdCallback());
        Properties staxInCryptoProperties = 
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        staxInProperties.setSignatureVerificationCryptoProperties(staxInCryptoProperties);
        staxInProperties.setEnableSignatureConfirmationVerification(true);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(staxInProperties);
        client.getInInterceptors().add(inhandler);

        assertEquals("test", echo.echo("test"));
    }
    
    private Service createService() {
        // Create the Service
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceBean(new EchoImpl());
        factory.setAddress("local://Echo");
        factory.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        Server server = factory.create();
        
        Service service = server.getEndpoint().getService();
        service.getInInterceptors().add(new LoggingInInterceptor());
        service.getOutInterceptors().add(new LoggingOutInterceptor());
        
        return service;
    }
    
    private Echo createClientProxy() {
        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setServiceClass(Echo.class);
        proxyFac.setAddress("local://Echo");
        proxyFac.getClientFactoryBean().setTransportId(LocalTransportFactory.TRANSPORT_ID);
        
        return (Echo)proxyFac.create();
    }
}
