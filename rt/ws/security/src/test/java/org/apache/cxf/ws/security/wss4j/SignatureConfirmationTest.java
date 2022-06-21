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

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;

import jakarta.xml.soap.SOAPMessage;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * This a test of the Signature Confirmation functionality that is contained in the
 * WS-Security 1.1 specification. The requestor signs an outbound SOAP message and saves
 * the signature. The responder processes the inbound SOAP message and saves the received
 * signature. Then in the responding message the received signature is attached in the
 * form of a wsse11:SignatureConfirmation blob. The requestor processes this blob and
 * checks to make sure that the signature value contained therein matches the saved value.
 */
public class SignatureConfirmationTest extends AbstractSecurityTest {

    public SignatureConfirmationTest() {
    }

    @org.junit.Test
    public void testSignatureConfirmationRequest() throws Exception {
        Document doc = readDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = getSoapMessageForDom(doc);

        msg.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        msg.put(ConfigurationConstants.ENABLE_SIGNATURE_CONFIRMATION, "true");
        msg.put(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        msg.put(ConfigurationConstants.USER, "myalias");
        msg.put("password", "myAliasPassword");
        //
        // This is necessary to convince the WSS4JOutInterceptor that we're
        // functioning as a requestor
        //
        msg.put(org.apache.cxf.message.Message.REQUESTOR_ROLE, true);

        handler.handleMessage(msg);

        SOAPMessage saajMsg = msg.getContent(SOAPMessage.class);
        doc = saajMsg.getSOAPPart();

        assertValid("//wsse:Security", doc);
        assertValid("//wsse:Security/ds:Signature", doc);

        //
        // Save the signature for future confirmation
        //
        Set<Integer> sigv = CastUtils.cast((Set<?>)msg.get(WSHandlerConstants.SEND_SIGV));
        assertNotNull(sigv);
        assertFalse(sigv.isEmpty());

        byte[] docbytes = getMessageBytes(doc);
        StaxUtils.read(new ByteArrayInputStream(docbytes));

        WSS4JInInterceptor inHandler = new WSS4JInInterceptor();

        SoapMessage inmsg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        inHandler.setProperty(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        inHandler.setProperty(ConfigurationConstants.ENABLE_SIGNATURE_CONFIRMATION, "true");

        inHandler.handleMessage(inmsg);

        //
        // Check that the inbound signature result was saved
        //
        List<WSHandlerResult> sigReceived =
            CastUtils.cast((List<?>)inmsg.get(WSHandlerConstants.RECV_RESULTS));
        assertNotNull(sigReceived);
        assertFalse(sigReceived.isEmpty());

        testSignatureConfirmationResponse(sigv, sigReceived);
    }


    private void testSignatureConfirmationResponse(
        Set<Integer> sigSaved,
        List<WSHandlerResult> sigReceived
    ) throws Exception {
        Document doc = readDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = getSoapMessageForDom(doc);

        msg.put(ConfigurationConstants.ACTION, ConfigurationConstants.TIMESTAMP);
        msg.put(WSHandlerConstants.RECV_RESULTS, sigReceived);

        handler.handleMessage(msg);

        SOAPMessage saajMsg = msg.getContent(SOAPMessage.class);
        doc = saajMsg.getSOAPPart();

        assertValid("//wsse:Security", doc);
        // assertValid("//wsse:Security/wsse11:SignatureConfirmation", doc);

        byte[] docbytes = getMessageBytes(doc);
        StaxUtils.read(new ByteArrayInputStream(docbytes));

        WSS4JInInterceptor inHandler = new WSS4JInInterceptor();

        SoapMessage inmsg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        inHandler.setProperty(ConfigurationConstants.ACTION, ConfigurationConstants.TIMESTAMP);
        inmsg.put(WSHandlerConstants.SEND_SIGV, sigSaved);

        inHandler.handleMessage(inmsg);
    }

}
