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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.util.ByteArrayDataSource;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Element;

import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.TestBase;
import org.apache.cxf.binding.soap.TestUtil;
import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.junit.Before;
import org.junit.Test;


public class SAAJInInterceptorTest extends TestBase {

    private ReadHeadersInterceptor rhi;
    private StaxInInterceptor staxIntc = new StaxInInterceptor();
    private SAAJInInterceptor saajIntc;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        rhi = new ReadHeadersInterceptor(BusFactory.getDefaultBus(), "phase1");
        chain.add(rhi);

        saajIntc = new SAAJInInterceptor("phase2");
        chain.add(saajIntc);
        
        chain.add(new CheckFaultInterceptor("phase3"));

    }

    @Test
    public void testHandleHeader() {
        try {
            prepareSoapMessage("../test-soap-header.xml");
        } catch (IOException ioe) {
            fail("Failed in creating soap message");
        }

        staxIntc.handleMessage(soapMessage);
        rhi.handleMessage(soapMessage);
        saajIntc.handleMessage(soapMessage);

        // check the xmlReader should be placed on the first entry of the body
        // element
        XMLStreamReader xmlReader = soapMessage.getContent(XMLStreamReader.class);
        assertEquals("check the first entry of body", "itinerary", xmlReader.getLocalName());

        List<Header> eleHeaders = soapMessage.getHeaders();
        List<Element> headerChilds = new ArrayList<Element>();
        Iterator<Header> iter = eleHeaders.iterator();
        
        while (iter.hasNext()) {
            Header hdr = iter.next();

            if (hdr.getObject() instanceof Element) {
                headerChilds.add((Element) hdr.getObject());
            }
        }
        
        assertEquals(2, headerChilds.size());
    }

    private void prepareSoapMessage(String message) throws IOException {

        soapMessage = TestUtil.createEmptySoapMessage(Soap12.getInstance(), chain);
        ByteArrayDataSource bads = new ByteArrayDataSource(this.getClass().getResourceAsStream(message),
                                                           "text/xml");
        soapMessage.setContent(InputStream.class, bads.getInputStream());

    }
}
