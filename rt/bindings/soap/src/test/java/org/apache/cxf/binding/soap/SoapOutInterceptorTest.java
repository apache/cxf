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

package org.apache.cxf.binding.soap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.staxutils.StaxUtils;
import org.junit.Before;
import org.junit.Test;

public class SoapOutInterceptorTest extends TestBase {
    private ReadHeadersInterceptor rhi;
    private SoapOutInterceptor soi;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        StaxInInterceptor sii = new StaxInInterceptor("phase1");
        chain.add(sii);

        rhi = new ReadHeadersInterceptor(BusFactory.getDefaultBus(), "phase2");
        chain.add(rhi);

        soi = new SoapOutInterceptor(BusFactory.getDefaultBus(), "phase3");
        chain.add(soi);
    }

    @Test
    public void testHandleMessage() throws Exception {
        prepareSoapMessage();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        soapMessage.setContent(OutputStream.class, out);
        soapMessage.setContent(XMLStreamWriter.class, StaxUtils.createXMLStreamWriter(out));
        
        soapMessage.getInterceptorChain().doIntercept(soapMessage);
        
        assertNotNull(soapMessage.getHeaders());

        Exception oe = (Exception)soapMessage.getContent(Exception.class);
        if (oe != null) {
            throw oe;
        }

        InputStream bis = new ByteArrayInputStream(out.toByteArray());
        XMLStreamReader xmlReader = StaxUtils.createXMLStreamReader(bis);
        assertInputStream(xmlReader);
    }

    private void assertInputStream(XMLStreamReader xmlReader) throws Exception {
        assertEquals(XMLStreamReader.START_ELEMENT, xmlReader.nextTag());
        assertEquals(Soap12.getInstance().getEnvelope(), xmlReader.getName());

        assertEquals(XMLStreamReader.START_ELEMENT, xmlReader.nextTag());
        assertEquals(Soap12.getInstance().getHeader(), xmlReader.getName());

        assertEquals(XMLStreamReader.START_ELEMENT, xmlReader.nextTag());
        assertEquals("reservation", xmlReader.getLocalName());

        assertEquals(XMLStreamReader.START_ELEMENT, xmlReader.nextTag());
        assertEquals("reference", xmlReader.getLocalName());
        // I don't think we're writing the body yet...
        //
        // assertEquals(XMLStreamReader.START_ELEMENT, xmlReader.nextTag());
        // assertEquals(Soap12.getInstance().getBody(), xmlReader.getName());
    }

    private void prepareSoapMessage() throws IOException {
        soapMessage = TestUtil.createEmptySoapMessage(Soap12.getInstance(), chain);

        soapMessage.setContent(InputStream.class, getClass().getResourceAsStream("test-soap-header.xml"));
    }

}
