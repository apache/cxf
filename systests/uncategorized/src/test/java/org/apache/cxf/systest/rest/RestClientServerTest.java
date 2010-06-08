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

package org.apache.cxf.systest.rest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;

import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_xml_http.wrapped.XMLService;
import org.junit.BeforeClass;
import org.junit.Test;

public class RestClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;
    private final QName serviceName = new QName("http://apache.org/hello_world_xml_http/wrapped",
                                                "XMLService");

    private final QName portName = new QName("http://apache.org/hello_world_xml_http/wrapped",
                                             "RestProviderPort");

    private final String endpointAddress =
        "http://localhost:" + PORT + "/XMLService/RestProviderPort/Customer"; 
   
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }
   
    @Test
    public void testHttpPOSTDispatchXMLBinding() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world_xml_wrapped.wsdl");
        assertNotNull(wsdl);

        XMLService service = new XMLService(wsdl, serviceName);
        assertNotNull(service);

        InputStream is = getClass().getResourceAsStream("resources/CustomerJohnReq.xml");
        Document doc = XMLUtils.parse(is);
        DOMSource reqMsg = new DOMSource(doc);
        assertNotNull(reqMsg);

        Dispatch<DOMSource> disp = service.createDispatch(portName, DOMSource.class, Service.Mode.PAYLOAD);
        updateAddressPort(disp, PORT);
        DOMSource result = disp.invoke(reqMsg);
        assertNotNull(result);

        Node respDoc = result.getNode();
        assertEquals("Customer", respDoc.getFirstChild().getLocalName());
    }

    @Test
    public void testHttpGET() throws Exception {
        URL url = new URL(endpointAddress + "?name=john&address=20");
        InputStream in = url.openStream();
        assertNotNull(in);       
    }

    @Test
    public void testHttpPOSTDispatchHTTPBinding() throws Exception {
        Service service = Service.create(serviceName);
        service.addPort(portName, HTTPBinding.HTTP_BINDING, endpointAddress);
        Dispatch<Source> dispatcher = service.createDispatch(portName, Source.class, Service.Mode.MESSAGE);
        Map<String, Object> requestContext = dispatcher.getRequestContext();
        requestContext.put(MessageContext.HTTP_REQUEST_METHOD, "POST");
        InputStream is = getClass().getResourceAsStream("resources/CustomerJohnReq.xml");
        Source result = dispatcher.invoke(new StreamSource(is));
        String tempstring = source2String(result);
        assertTrue("Result should start with Customer", tempstring.startsWith("<ns4:Customer"));
        assertTrue("Result should have CustomerID", tempstring.lastIndexOf(">123456<") > 0);
    }
    
    private String source2String(Source source) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamResult sr = new StreamResult(bos);
        Transformer trans = TransformerFactory.newInstance().newTransformer();
        Properties oprops = new Properties();
        oprops.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperties(oprops);
        trans.transform(source, sr);
        return bos.toString();
    }
}
