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

package org.apache.cxf.binding.soap.interceptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.message.Message;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class SoapActionInInterceptorTest extends Assert {
    private IMocksControl control;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }

    @Test
    public void testGetSoapActionForSOAP11() throws Exception {
        SoapMessage message = setUpMessage("text/xml", Soap11.getInstance(), "urn:cxf"); 
        control.replay();
        String action = SoapActionInInterceptor.getSoapAction(message);
        assertEquals("urn:cxf", action);
        control.verify();
    }

    @Test
    public void testGetSoapActionForSOAP11None() throws Exception {
        SoapMessage message = setUpMessage("text/xml", Soap11.getInstance(), null);  
        control.replay();
        String action = SoapActionInInterceptor.getSoapAction(message);
        assertNull(action);
        control.verify();
    }

    @Test
    public void testGetSoapActionForSOAP12() throws Exception {
        SoapMessage message = setUpMessage("application/soap+xml; action=\"urn:cxf\"", Soap12.getInstance(), null);
        control.replay();
        String action = SoapActionInInterceptor.getSoapAction(message);
        assertEquals("urn:cxf", action);
        control.verify();
    }

    @Test
    public void testGetSoapActionForSOAP12None() throws Exception {
        SoapMessage message = setUpMessage("application/soap+xml", Soap12.getInstance(), null);
        control.replay();
        String action = SoapActionInInterceptor.getSoapAction(message);
        assertNull(action);
        control.verify();
    }

    @Test
    public void testGetSoapActionForSOAP11SwA() throws Exception {
        SoapMessage message = setUpMessage("multipart/related", Soap11.getInstance(), "urn:cxf"); 
        control.replay();
        String action = SoapActionInInterceptor.getSoapAction(message);
        assertEquals("urn:cxf", action);
        control.verify();
    }

    @Test
    // note this combination of SOAP12 with SwA is not normative, but some systems may use it.
    // here the optional start-info is used to encode action as in start-info="application/soap+xml; action=\"urn:cxf\""
    public void testGetSoapActionForSOAP12SwAWithStartInfo() throws Exception {
        SoapMessage message = setUpMessage(
            "multipart/related; start-info=\"application/soap+xml; action=\\\"urn:cxf\\\"",
            Soap12.getInstance(), null); 
        control.replay();
        String action = SoapActionInInterceptor.getSoapAction(message);
        assertEquals("urn:cxf", action);
        control.verify();
    }

    @Test
    // note this combination of SOAP12 with SwA is not normative, but some systems use it.
    // here the action property is set as in action="urn:cxf", although this usage is invalid because the action
    // property is not part of the multipart/related media type.
    public void testGetSoapActionForSOAP12SwAWithAction() throws Exception {
        SoapMessage message = setUpMessage(
            "multipart/related; start-info=\"application/soap+xml\"; action=\"urn:cxf\"",
            Soap12.getInstance(), null); 
        control.replay();
        String action = SoapActionInInterceptor.getSoapAction(message);
        assertEquals("urn:cxf", action);
        control.verify();
    }

    @Test
    // note this combination of SOAP12 with SwA is not normative, but some systems may use it.
    // here the action property is only set at the part header as in action="urn:cxf"
    public void testGetSoapActionForSOAP12SwAWithActionInPartHeaders() throws Exception {
        SoapMessage message = setUpMessage(
            "multipart/related",
            Soap12.getInstance(), "urn:cxf"); 
        control.replay();
        String action = SoapActionInInterceptor.getSoapAction(message);
        assertEquals("urn:cxf", action);
        control.verify();
    }

    @Test
    // note this combination of SOAP12 with SwA is not normative, but some systems may use it.
    public void testGetSoapActionForSOAP12SwANone() throws Exception {
        SoapMessage message = setUpMessage(
            "multipart/related",
            Soap12.getInstance(), null); 
        control.replay();
        String action = SoapActionInInterceptor.getSoapAction(message);
        assertNull(action);
        control.verify();
    }

    @Test
    public void testGetSoapActionForSOAP11MTOM() throws Exception {
        SoapMessage message = setUpMessage(
            "multipart/related; type=\"application/xop+xml\"; start-info=\"text/xml\"",
            Soap11.getInstance(), "urn:cxf"); 
        control.replay();
        String action = SoapActionInInterceptor.getSoapAction(message);
        assertEquals("urn:cxf", action);
        control.verify();
    }

    @Test
    // some systems use this form, although this is not spec-conformant as 
    // the action property is not part of the multipart/related media type.
    // here the action propety is set as in start-info="application/soap+xml"; action="urn:cxf"
    public void testGetSoapActionForSOAP12MTOMWithAction() throws Exception {
        SoapMessage message = setUpMessage(
            "multipart/related; type=\"application/xop+xml\""
                + "; start-info=\"application/soap+xml\"; action=\"urn:cxf\"",
            Soap11.getInstance(), "urn:cxf"); 
        control.replay();
        String action = SoapActionInInterceptor.getSoapAction(message);
        assertEquals("urn:cxf", action);
        control.verify();
    }

    private SoapMessage setUpMessage(String contentType, SoapVersion version, String prop) {
        SoapMessage message = control.createMock(SoapMessage.class);
        Map<String, List<String>> headers = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        Map<String, List<String>> partHeaders = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        if (version instanceof Soap11 && prop != null) {
            headers.put("SOAPAction", Collections.singletonList(prop));
        } else if (version instanceof Soap12 && prop != null) {
            partHeaders.put(Message.CONTENT_TYPE, 
                            Collections.singletonList("application/soap+xml; action=\"" + prop + "\""));
        }
        EasyMock.expect(message.getVersion()).andReturn(version).anyTimes();
        EasyMock.expect(message.get(Message.CONTENT_TYPE)).andReturn(contentType).anyTimes();
        EasyMock.expect(message.get(Message.PROTOCOL_HEADERS)).andReturn(headers).anyTimes();
        EasyMock.expect(message.get(AttachmentDeserializer.ATTACHMENT_PART_HEADERS)).andReturn(partHeaders).anyTimes();
        return message;
    }
}
