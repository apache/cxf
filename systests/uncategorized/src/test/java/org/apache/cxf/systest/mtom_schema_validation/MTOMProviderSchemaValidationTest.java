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
package org.apache.cxf.systest.mtom_schema_validation;

import java.io.File;
import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.xml.ws.soap.MTOMFeature;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class MTOMProviderSchemaValidationTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;

    private final QName serviceName = new QName("http://cxf.apache.org/", "HelloWS");

    @BeforeClass
    public static void startservers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }
    @Test
    public void testSchemaValidation() throws Exception {
        HelloWS port = createService();
        Hello request = new Hello();
        request.setArg0("value");
        URL wsdl = getClass().getResource("/wsdl_systest/mtom_provider_validate.wsdl");
        File attachment = new File(wsdl.getFile());
        request.setFile(new DataHandler(new FileDataSource(attachment)));
        HelloResponse response = port.hello(request);
        assertEquals("Hello CXF", response.getReturn());
    }

    private HelloWS createService() throws Exception {
        URL wsdl = getClass().getResource("/wsdl_systest/mtom_provider_validate.wsdl");
        assertNotNull(wsdl);

        HelloWSClient service = new HelloWSClient(wsdl, serviceName);
        assertNotNull(service);

        HelloWS port = service.getHello(new MTOMFeature());

        updateAddressPort(port, PORT);

        return port;
    }
}
