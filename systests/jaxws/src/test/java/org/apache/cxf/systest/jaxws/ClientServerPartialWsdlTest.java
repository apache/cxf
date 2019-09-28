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

package org.apache.cxf.systest.jaxws;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.dynamic.DynamicClientFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClientServerPartialWsdlTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(ServerPartialWsdl.class);

    private final QName serviceName1 = new QName("http://jaxws.systest.cxf.apache.org/",
                                                "AddNumbersImplPartial1Service");
    private final QName portName1 = new QName("http://jaxws.systest.cxf.apache.org/",
                                             "AddNumbersImplPartial1Port");
    private final QName serviceName2 = new QName("http://jaxws.systest.cxf.apache.org/",
                                                "AddNumbersImplPartial2Service");
    private final QName portName2 = new QName("http://jaxws.systest.cxf.apache.org/",
                                             "AddNumbersImplPartial2Port");



    @BeforeClass
    public static void startServers() throws Exception {

        assertTrue("server did not launch correctly", launchServer(ServerPartialWsdl.class, true));

    }

    @Test
    public void testCXF4676Partial1() throws Exception {
        DynamicClientFactory dcf = DynamicClientFactory.newInstance();
        Client client = dcf.createClient("http://localhost:"
            + PORT + "/AddNumbersImplPartial1Service?wsdl", serviceName1, portName1);
        updateAddressPort(client, PORT);
        Object[] result = client.invoke("addTwoNumbers", 10, 20);
        assertNotNull("no response received from service", result);
        assertEquals(30, result[0]);
    }

    @Test
    public void testCXF4676partial2() throws Exception {
        DynamicClientFactory dcf = DynamicClientFactory.newInstance();
        Client client = dcf.createClient("http://localhost:"
            + PORT + "/AddNumbersImplPartial2Service?wsdl", serviceName2, portName2);
        updateAddressPort(client, PORT);
        Object[] result = client.invoke("addTwoNumbers", 10, 20);
        assertNotNull("no response received from service", result);
        assertEquals(30, result[0]);

    }

}
