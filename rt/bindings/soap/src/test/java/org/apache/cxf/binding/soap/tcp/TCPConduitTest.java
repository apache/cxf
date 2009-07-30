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

package org.apache.cxf.binding.soap.tcp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

@Ignore
public class TCPConduitTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testTCPConduit() {
        //TCPConduit tcpConduit = new TCPConduit(null);
    }

    @Test
    public void testPrepare() {
        int num1 = 2;
        int num2 = 3;
        /*
        final String messageData = "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\""
            + " xmlns:a=\"http://www.w3.org/2005/08/addressing\"><s:Header><a:Action s:mustUnderstand=\"1\">"
            + "http://tempuri.org/ICalculator/add</a:Action>"
            + "<a:MessageID>urn:uuid:e2606099-5bef-4db2-b661-19a883bab4e7</a:MessageID><a:ReplyTo>"
            + "<a:Address>http://www.w3.org/2005/08/addressing/anonymous</a:Address></a:ReplyTo>"
            + "<a:To s:mustUnderstand=\"1\">soap.tcp://localhost:9999/calculator</a:To></s:Header><s:Body>"
            + "<add xmlns=\"http://tempuri.org/\">"
            + "<i>" + num1 + "</i>"
            + "<j>" + num2 + "</j>"
            + "</add></s:Body></s:Envelope>";
            */
        
        
        final String messageData = "<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<S:Body><ns2:add xmlns:ns2=\"http://calculator.me.org/\"><i>"
            + num1 + "</i><j>" + num2 + "</j></ns2:add></S:Body></S:Envelope>";
        
        final AttributedURIType a = new AttributedURIType();
        a.setValue("soap.tcp://localhost:8080/CalculatorApp/CalculatorWSService");
        final EndpointReferenceType t = new EndpointReferenceType();
        t.setAddress(a);
        
        try {
            final TCPConduit tcpConduit = new TCPConduit(t);
            tcpConduit.setMessageObserver(new TestMessageObserver());
            final Message msg = getNewMessage();
            
            tcpConduit.prepare(msg);
            
            final OutputStream out = msg.getContent(OutputStream.class);
            out.write(messageData.getBytes("UTF-8"));
            out.flush();
            out.close();
            tcpConduit.close(msg);
            
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private Message getNewMessage() {
        Message message = new MessageImpl();
        message = new SoapMessage(message);
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        List<String> contentTypes = new ArrayList<String>();
        contentTypes.add("text/xml");
        contentTypes.add("charset=utf8");
        headers.put("content-type", contentTypes);
        message.put(Message.PROTOCOL_HEADERS, headers);
        return message;
    }

    private class TestMessageObserver implements MessageObserver {

        public void onMessage(final Message message) {
            int correctResult = 5;
            assertNotNull(message);
            InputStream input = message.getContent(InputStream.class);
            byte response[] = null;
            try {
                response = new byte[input.available()];
                input.read(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(response);

                XMLStreamReader xmlReader = StaxUtils.createXMLStreamReader(bais, "UTF-8");
                while (xmlReader.hasNext()) {
                    xmlReader.next();
                    if (xmlReader.getEventType() == XMLStreamReader.START_ELEMENT
                        && xmlReader.getLocalName().equals("addResult")) {
                        assertEquals(correctResult, Integer.parseInt(xmlReader.getElementText()));
                    }
                }
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
        }
        
    }
}
