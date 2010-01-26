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

import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.w3c.dom.Document;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.action.UsernameTokenAction;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandler;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.junit.Test;


/**
 * @author <a href="mailto:tsztelak@gmail.com">Tomasz Sztelak</a>
 */
public class WSS4JOutInterceptorTest extends AbstractSecurityTest {
    
    @Test
    public void testUsernameTokenText() throws Exception {
        SOAPMessage saaj = readSAAJDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(msg);

        msg.setContent(SOAPMessage.class, saaj);

        msg.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        msg.put(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        msg.put(WSHandlerConstants.USER, "username");
        msg.put("password", "myAliasPassword");
        msg.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        handler.handleMessage(msg);

        SOAPPart doc = saaj.getSOAPPart();
        assertValid("//wsse:Security", doc);
        assertValid("//wsse:Security/wsse:UsernameToken", doc);
        assertValid("//wsse:Security/wsse:UsernameToken/wsse:Username[text()='username']", doc);
        // Test to see that the plaintext password is used in the header
        assertValid("//wsse:Security/wsse:UsernameToken/wsse:Password[text()='myAliasPassword']", doc);
    }
        
    @Test
    public void testUsernameTokenDigest() throws Exception {
        SOAPMessage saaj = readSAAJDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(msg);

        msg.setContent(SOAPMessage.class, saaj);

        msg.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        msg.put(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        msg.put(WSHandlerConstants.USER, "username");
        msg.put("password", "myAliasPassword");
        msg.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
        handler.handleMessage(msg);

        SOAPPart doc = saaj.getSOAPPart();
        assertValid("//wsse:Security", doc);
        assertValid("//wsse:Security/wsse:UsernameToken", doc);
        assertValid("//wsse:Security/wsse:UsernameToken/wsse:Username[text()='username']", doc);
        // Test to see that the password digest is used in the header
        assertInvalid("//wsse:Security/wsse:UsernameToken/wsse:Password[text()='myAliasPassword']", doc);
    }

    @Test
    public void testEncrypt() throws Exception {
        SOAPMessage saaj = readSAAJDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(msg);

        msg.setContent(SOAPMessage.class, saaj);
        
        msg.put(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT);
        msg.put(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        msg.put(WSHandlerConstants.ENC_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        msg.put(WSHandlerConstants.USER, "myalias");
        msg.put("password", "myAliasPassword");

        handler.handleMessage(msg);

        SOAPPart doc = saaj.getSOAPPart();
        assertValid("//wsse:Security", doc);
        assertValid("//s:Body/xenc:EncryptedData", doc);
    }
    
    @Test
    public void testSignature() throws Exception {
        SOAPMessage saaj = readSAAJDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(msg);

        msg.setContent(SOAPMessage.class, saaj);

        msg.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        msg.put(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        msg.put(WSHandlerConstants.USER, "myAlias");
        msg.put("password", "myAliasPassword");

        handler.handleMessage(msg);

        SOAPPart doc = saaj.getSOAPPart();
        assertValid("//wsse:Security", doc);
        assertValid("//wsse:Security/ds:Signature", doc);
    }

    @Test
    public void testTimestamp() throws Exception {
        SOAPMessage saaj = readSAAJDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(msg);

        msg.setContent(SOAPMessage.class, saaj);
        
        ohandler.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.TIMESTAMP);
        ohandler.setProperty(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        msg.put(WSHandlerConstants.USER, "myalias");
        msg.put("password", "myAliasPassword");

        handler.handleMessage(msg);

        SOAPPart doc = saaj.getSOAPPart();
        assertValid("//wsse:Security", doc);
        assertValid("//wsse:Security/wsu:Timestamp", doc);
    }
    
    @Test
    public void testCustomAction() throws Exception {
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
        msg.put(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/outsecurity.properties");
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
            customActions.put(new Object(), action);
            handler.handleMessage(msg);
        } catch (SoapFault e) {
            assertEquals("An invalid action configuration was defined.", e.getMessage());
        }
    }
    
    private static class CountingUsernameTokenAction extends UsernameTokenAction {

        private int executions;
        
        @Override
        public void execute(WSHandler handler, int actionToDo, Document doc,
                RequestData reqData) throws WSSecurityException {
            
            this.executions++;
            super.execute(handler, actionToDo, doc, reqData);
        }

        public int getExecutions() {
            return this.executions;
        }
    }
}
