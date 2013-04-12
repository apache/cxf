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
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.junit.Test;


/**
 */
public class StaxRoundTripTest extends AbstractSecurityTest {
    
    @Test
    public void testUsernameTokenText() throws Exception {
        // Create + configure service
        Service service = createService();
        
        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);

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
    }
    
    @Test
    public void testUsernameTokenDigest() throws Exception {
        // Create + configure service
        Service service = createService();
        
        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);
        
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
    }
    
    @Test
    public void testEncrypt() throws Exception {
        // Create + configure service
        Service service = createService();
        
        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties = 
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setDecryptionCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(new XMLSecurityConstants.Action[]{WSSConstants.ENCRYPT});
        properties.setEncryptionUser("myalias");
        
        Properties outCryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setEncryptionCryptoProperties(outCryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testEncryptUsernameToken() throws Exception {
        // Create + configure service
        Service service = createService();
        
        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties = 
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setDecryptionCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);
        
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
        
        Properties outCryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setEncryptionCryptoProperties(outCryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testSignature() throws Exception {
        // Create + configure service
        Service service = createService();
        
        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties = 
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(new XMLSecurityConstants.Action[]{WSSConstants.SIGNATURE});
        properties.setSignatureUser("myalias");
        
        Properties outCryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(outCryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testTimestamp() throws Exception {
        // Create + configure service
        Service service = createService();
        
        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);
        
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
    }
    
    @Test
    public void testSignatureTimestamp() throws Exception {
        // Create + configure service
        Service service = createService();
        
        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties = 
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);
        
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
            new SecurePart(new QName(WSSConstants.NS_WSU10, "Timestamp"), SecurePart.Modifier.Element)
        );
        properties.addSignaturePart(
            new SecurePart(new QName(WSSConstants.NS_SOAP11, "Body"), SecurePart.Modifier.Element)
        );
        properties.setSignatureUser("myalias");
        
        Properties outCryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(outCryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testSignaturePKI() throws Exception {
        // Create + configure service
        Service service = createService();
        
        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties = 
            CryptoFactory.getProperties("cxfca.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);
        
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
        
        Properties outCryptoProperties = 
            CryptoFactory.getProperties("alice.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(outCryptoProperties);
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
        
        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties = 
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        inProperties.setDecryptionCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);
        
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
        
        Properties outCryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(outCryptoProperties);
        properties.setEncryptionCryptoProperties(outCryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testSignatureConfirmation() throws Exception {
        // Create + configure service
        Service service = createService();
        
        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.setCallbackHandler(new TestPwdCallback());
        Properties cryptoProperties = 
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        inProperties.setSignatureVerificationCryptoProperties(cryptoProperties);
        WSS4JStaxInInterceptor inhandler = new WSS4JStaxInInterceptor(inProperties);
        service.getInInterceptors().add(inhandler);
        
        WSSSecurityProperties outProperties = new WSSSecurityProperties();
        outProperties.setOutAction(new XMLSecurityConstants.Action[]{WSSConstants.SIGNATURE_CONFIRMATION});
        outProperties.setSignatureUser("myalias");
        
        Properties outCryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        outProperties.setSignatureCryptoProperties(outCryptoProperties);
        outProperties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor staxOhandler = new WSS4JStaxOutInterceptor(outProperties);
        service.getOutInterceptors().add(staxOhandler);
        
        // Create + configure client
        Echo echo = createClientProxy();
        
        Client client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.setOutAction(new XMLSecurityConstants.Action[]{WSSConstants.SIGNATURE});
        properties.setSignatureUser("myalias");
        
        Properties clientOutCryptoProperties = 
            CryptoFactory.getProperties("outsecurity.properties", this.getClass().getClassLoader());
        properties.setSignatureCryptoProperties(clientOutCryptoProperties);
        properties.setCallbackHandler(new TestPwdCallback());
        WSS4JStaxOutInterceptor ohandler = new WSS4JStaxOutInterceptor(properties);
        client.getOutInterceptors().add(ohandler);
        
        WSSSecurityProperties staxInProperties = new WSSSecurityProperties();
        staxInProperties.setCallbackHandler(new TestPwdCallback());
        Properties staxInCryptoProperties = 
            CryptoFactory.getProperties("insecurity.properties", this.getClass().getClassLoader());
        staxInProperties.setSignatureVerificationCryptoProperties(staxInCryptoProperties);
        staxInProperties.setEnableSignatureConfirmationVerification(true);
        WSS4JStaxInInterceptor inhandler2 = new WSS4JStaxInInterceptor(staxInProperties);
        client.getInInterceptors().add(inhandler2);

        assertEquals("test", echo.echo("test"));
    }
    
   /*
    * TODO
    @Test
    public void testOverrideCustomAction() throws Exception {
        SOAPMessage saaj = readSAAJDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(msg);

        msg.setContent(SOAPMessage.class, saaj);
        
        CountingUsernameTokenAction action = new CountingUsernameTokenAction();
        Map<Object, Object> customActions = new HashMap<Object, Object>(1);
        customActions.put(WSConstants.UT, action);
                
        msg.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        msg.put(WSHandlerConstants.SIG_PROP_FILE, "outsecurity.properties");
        msg.put(WSHandlerConstants.USER, "username");
        msg.put("password", "myAliasPassword");
        msg.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        msg.put(WSS4JOutInterceptor.WSS4J_ACTION_MAP, customActions);
        handler.handleMessage(msg);

        SOAPPart doc = saaj.getSOAPPart();
        assertValid("//wsse:Security", doc);
        assertValid("//wsse:Security/wsse:UsernameToken", doc);
        assertValid("//wsse:Security/wsse:UsernameToken/wsse:Username[text()='username']", doc);
        // Test to see that the plaintext password is used in the header
        assertValid("//wsse:Security/wsse:UsernameToken/wsse:Password[text()='myAliasPassword']", doc);
        assertEquals(1, action.getExecutions());
        
        try {
            customActions.put(WSConstants.UT, new Object());
            handler.handleMessage(msg);
        } catch (SoapFault e) {
            assertEquals("An invalid action configuration was defined.", e.getMessage());
        }
        
        try {
            customActions.put(new Object(), CountingUsernameTokenAction.class);
            handler.handleMessage(msg);
        } catch (SoapFault e) {
            assertEquals("An invalid action configuration was defined.", e.getMessage());
        }
    }
    
    
    @Test
    public void testAddCustomAction() throws Exception {
        SOAPMessage saaj = readSAAJDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(msg);

        msg.setContent(SOAPMessage.class, saaj);
        
        CountingUsernameTokenAction action = new CountingUsernameTokenAction();
        Map<Object, Object> customActions = new HashMap<Object, Object>(1);
        customActions.put(12345, action);
                
        msg.put(WSHandlerConstants.ACTION, "12345");
        msg.put(WSHandlerConstants.SIG_PROP_FILE, "outsecurity.properties");
        msg.put(WSHandlerConstants.USER, "username");
        msg.put("password", "myAliasPassword");
        msg.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        msg.put(WSS4JOutInterceptor.WSS4J_ACTION_MAP, customActions);
        handler.handleMessage(msg);

        SOAPPart doc = saaj.getSOAPPart();
        assertValid("//wsse:Security", doc);
        assertValid("//wsse:Security/wsse:UsernameToken", doc);
        assertValid("//wsse:Security/wsse:UsernameToken/wsse:Username[text()='username']", doc);
        // Test to see that the plaintext password is used in the header
        assertValid("//wsse:Security/wsse:UsernameToken/wsse:Password[text()='myAliasPassword']", doc);
        assertEquals(1, action.getExecutions());
    }
    
    private static class CountingUsernameTokenAction extends UsernameTokenAction {

        private int executions;
        
        @Override
        public void execute(WSHandler handler, int actionToDo, Document doc,
                RequestData reqData) throws WSSecurityException {
            
            this.executions++;
            reqData.setPwType(WSConstants.PW_TEXT);
            super.execute(handler, actionToDo, doc, reqData);
        }

        public int getExecutions() {
            return this.executions;
        }
    }
    */
    
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
