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
package org.apache.cxf.systest.soapfault;

import javax.xml.namespace.QName;

import org.apache.cxf.soapfault.SoapFaultPortType;
import org.apache.cxf.soapfault.SoapFaultService;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import org.xmlsoap.schemas.soap.envelope.Fault;

public class SOAPFaultRequestTestCase extends AbstractClientServerTestBase {
    private static final String PORT = Server.PORT;
    private final QName portName = new QName("http://cxf.apache.org/soapfault", "SoapFaultPortType");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @Test
    public void testSendSoapFaultRequest() throws Exception {

        SoapFaultService service = new SoapFaultService();
        assertNotNull(service);

        SoapFaultPortType soapFaultPort = service.getPort(portName, SoapFaultPortType.class);
        updateAddressPort(soapFaultPort, PORT);

        Fault fault = new Fault();
        fault.setFaultstring("ClientSetFaultString");
        fault.setFaultcode(new QName("http://cxf.apache.org/soapfault", "ClientSetError"));
        soapFaultPort.soapFault(fault);

    }

}
