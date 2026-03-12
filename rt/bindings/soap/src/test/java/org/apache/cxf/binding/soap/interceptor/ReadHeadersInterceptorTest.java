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

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class ReadHeadersInterceptorTest {
    private static final byte[] TEST_SOAP =
        ("<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'"
            + " xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'"
            + " xmlns:xs='http://www.w3.org/2001/XMLSchema' xmlns:bar='tmp:bar'"
            + " xmlns:x='http://example.com/schema'>"
            + "<soap:Header testAttr='testValue'>"
            + "<x:AuthToken>Token</x:AuthToken>"
            + "</soap:Header>"
            + "<soap:Body>"
            + "<ns2:payload xmlns:ns2='urn:tmp:foo'/>"
            + "</soap:Body>"
            + "</soap:Envelope>").getBytes();

    private ReadHeadersInterceptor interceptor;

    @Before
    public void setUp() {
        interceptor = new ReadHeadersInterceptor(null);
    }

    @Test
    public void testNotAddNSContext() throws Exception {
        SoapMessage message = setUpMessage();
        interceptor.handleMessage(message);
        Map<String, String> nsc = CastUtils.cast((Map<?, ?>)message.get("soap.body.ns.context"));
        assertNull(nsc);
    }

    @Test
    public void testAddNSContext() throws Exception {
        SoapMessage message = setUpMessage();
        message.put("org.apache.cxf.binding.soap.addNamespaceContext", "true");
        interceptor.handleMessage(message);
        Map<String, String> nsc = CastUtils.cast((Map<?, ?>)message.get("soap.body.ns.context"));
        assertNotNull(nsc);
        assertEquals("http://www.w3.org/2001/XMLSchema-instance", nsc.get("xsi"));
        assertEquals("http://www.w3.org/2001/XMLSchema", nsc.get("xs"));
        assertEquals("tmp:bar", nsc.get("bar"));

    }

    @Test
    public void testHeaderAttributes() throws Exception {
        SoapMessage message = setUpMessage();
        interceptor.handleMessage(message);
        List<Header> cxfHeaders = message.getHeaders();
        assertEquals(1, cxfHeaders.size());
        Element headerElement = (Element) cxfHeaders.get(0).getObject();
        NamedNodeMap attributes = headerElement.getAttributes();

        assertEquals("AuthToken", headerElement.getLocalName());
        assertEquals("http://example.com/schema", headerElement.getNamespaceURI());
        // The test SOAP header attribute "testAttr", must not be transferred to child element "AuthToken"
        assertEquals(0, attributes.getLength());
    }

    private SoapMessage setUpMessage() throws Exception {
        SoapMessage message = new SoapMessage(Soap11.getInstance());
        message.setContent(XMLStreamReader.class, StaxUtils.createXMLStreamReader(new ByteArrayInputStream(TEST_SOAP)));
        return message;
    }

}