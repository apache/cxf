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

package org.apache.cxf.binding.soap.saaj;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Element;

import jakarta.xml.soap.SOAPMessage;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.TestBase;
import org.apache.cxf.binding.soap.TestUtil;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

import org.junit.Before;
import org.junit.Test;


public class SAAJOutInterceptorTest extends TestBase {

    private SoapOutInterceptor soi;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        soi = new SoapOutInterceptor(BusFactory.getDefaultBus());
    }

    @Test
    public void testHandleHeader() throws Exception {
        soapMessage = TestUtil.createEmptySoapMessage(Soap11.getInstance(), chain);
        soapMessage.setContent(OutputStream.class, new ByteArrayOutputStream());

        SOAPMessage m = SAAJFactoryResolver.createMessageFactory(soapMessage.getVersion()).createMessage();

        InputStream ins = getClass().getResourceAsStream("../test-soap-header.xml");
        m.getSOAPPart().setContent(new StreamSource(ins));

        Element el = DOMUtils.getFirstElement(m.getSOAPPart().getEnvelope().getHeader());
        el = (Element)DOMUtils.getDomElement(el);
        List<Header> h = soapMessage.getHeaders();
        while (el != null) {
            h.add(new SoapHeader(DOMUtils.getElementQName(el), el));
            el = DOMUtils.getNextElement(el);
        }
        soapMessage.setContent(SOAPMessage.class, m);
        W3CDOMStreamWriter writer = new SAAJStreamWriter(m.getSOAPPart());
        soapMessage.setContent(XMLStreamWriter.class, writer);
        soi.handleMessage(soapMessage);

    }
}
