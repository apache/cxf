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

package org.apache.cxf.systest.source;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.hello_world_soap_http_source.source.GreetMeFault;
import org.apache.hello_world_soap_http_source.source.Greeter;
import org.apache.hello_world_soap_http_source.source.SOAPService;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class ClientServerSourceTest extends AbstractBusClientServerTestBase {
    static final String WSDL_PORT = TestUtil.getPortNumber(Server.class);

    private static final QName SERVICE_NAME
        = new QName("http://apache.org/hello_world_soap_http_source/source", "SOAPService");


    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    private Element getElement(Node nd) {
        if (nd instanceof Document) {
            return ((Document)nd).getDocumentElement();
        }
        return (Element)nd;
    }

    @Test
    public void testCallFromClient() throws Exception {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("org/apache/cxf/systest/source/cxf.xml");
        BusFactory.setDefaultBus(bus);
        URL wsdl = this.getClass().getResource("/wsdl_systest_databinding/source/hello_world.wsdl");
        assertNotNull("We should have found the WSDL here. ", wsdl);

        SOAPService ss = new SOAPService(wsdl, SERVICE_NAME);
        Greeter port = ss.getSoapPort();
        updateAddressPort(port, WSDL_PORT);

        ClientProxy.getClient(port).getInInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(port).getOutInterceptors().add(new LoggingOutInterceptor());

        Document doc = DOMUtils.newDocument();
        doc.appendChild(doc.createElementNS("http://apache.org/hello_world_soap_http_source/source/types",
                                            "ns1:sayHi"));
        DOMSource ds = new DOMSource(doc);
        DOMSource resp = port.sayHi(ds);
        assertEquals("We should get the right response", "Bonjour",
                     DOMUtils.getContent(getElement(resp.getNode().getFirstChild().getFirstChild())));

        doc = DOMUtils.newDocument();
        Element el = doc.createElementNS("http://apache.org/hello_world_soap_http_source/source/types",
            "ns1:greetMe");
        Element el2 = doc.createElementNS("http://apache.org/hello_world_soap_http_source/source/types",
            "ns1:requestType");
        el2.appendChild(doc.createTextNode("Willem"));
        el.appendChild(el2);
        doc.appendChild(el);
        ds = new DOMSource(doc);
        resp = port.greetMe(ds);
        assertEquals("We should get the right response", "Hello Willem",
                     DOMUtils.getContent(DOMUtils.getFirstElement(getElement(resp.getNode()))));

        try {
            doc = DOMUtils.newDocument();
            el = doc.createElementNS("http://apache.org/hello_world_soap_http_source/source/types",
                "ns1:greetMe");
            el2 = doc.createElementNS("http://apache.org/hello_world_soap_http_source/source/types",
                "ns1:requestType");
            el2.appendChild(doc.createTextNode("fault"));
            el.appendChild(el2);
            doc.appendChild(el);
            ds = new DOMSource(doc);
            port.greetMe(ds);
            fail("Should have been a fault");
        } catch (GreetMeFault ex) {
            assertEquals("Some fault detail", DOMUtils.getContent(getElement(ex.getFaultInfo().getNode())));
        }

    }

}
