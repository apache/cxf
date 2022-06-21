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

package org.apache.cxf.systest.exception;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GenericExceptionTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;
    private final QName serviceName = new QName("http://cxf.apache.org/test/HelloService", "HelloService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));

    }

    @Test
    public void testGenericException() throws Exception {
        String address = "http://localhost:" + PORT + "/generic";
        URL wsdlURL = new URL(address + "?wsdl");
        //check wsdl element
        InputStream ins = wsdlURL.openStream();

        Document doc = StaxUtils.read(ins);

        Map<String, String> ns = new HashMap<>();
        ns.put("xsd", "http://www.w3.org/2001/XMLSchema");
        ns.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        ns.put("tns", "http://cxf.apache.org/test/HelloService");
        XPathUtils xpu = new XPathUtils(ns);


        Node nd = xpu.getValueNode("//xsd:complexType[@name='objectWithGenerics']", doc);
        assertNotNull(nd);
        assertNotNull(xpu.getValueNode("//xsd:element[@name='a']", nd));
        assertNotNull(xpu.getValueNode("//xsd:element[@name='b']", nd));

        Service service = Service.create(wsdlURL, serviceName);
        service.addPort(new QName("http://cxf.apache.org/test/HelloService", "HelloPort"),
                        SOAPBinding.SOAP11HTTP_BINDING, address);
        GenericsEcho port = service
            .getPort(new QName("http://cxf.apache.org/test/HelloService", "HelloPort"), GenericsEcho.class);
        try {
            port.echo("test");
            fail("Exception is expected");
        } catch (GenericsException e) {
            ObjectWithGenerics<Boolean, Integer> genericObj = e.getObj();
            assertTrue(genericObj.getA());
            assertEquals(100, genericObj.getB().intValue());
        }

    }

}
