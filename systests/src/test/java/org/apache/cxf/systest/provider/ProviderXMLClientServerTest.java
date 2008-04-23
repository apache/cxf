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

package org.apache.cxf.systest.provider;

import java.io.InputStream;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_xml_http.wrapped.XMLService;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProviderXMLClientServerTest extends AbstractBusClientServerTestBase {
    private final QName serviceName = new QName(
            "http://apache.org/hello_world_xml_http/wrapped", "XMLService");

    private final QName portName = new QName(
            "http://apache.org/hello_world_xml_http/wrapped", "XMLProviderPort");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                launchServer(XMLServer.class));
    }

    @Test
    public void testDOMSourcePAYLOAD() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world_xml_wrapped.wsdl");
        assertNotNull(wsdl);

        XMLService service = new XMLService(wsdl, serviceName);
        assertNotNull(service);

        InputStream is = getClass().getResourceAsStream(
                "/messages/XML_GreetMeDocLiteralReq.xml");
        Document doc = XMLUtils.parse(is);
        DOMSource reqMsg = new DOMSource(doc);
        assertNotNull(reqMsg);

        Dispatch<DOMSource> disp = service.createDispatch(portName,
                DOMSource.class, Service.Mode.PAYLOAD);
        DOMSource result = disp.invoke(reqMsg);
        assertNotNull(result);

        Node respDoc = result.getNode();
        assertEquals("greetMeResponse", respDoc.getFirstChild().getLocalName());
        assertEquals("TestXMLBindingProviderMessage", respDoc.getFirstChild()
                .getTextContent());
    }

}
