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

package org.apache.cxf.systest.cxf993;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;
import testnotification.NotificationService;
import testnotification.NotificationServicePort;
import testnotification.SendNotification;

public class Cxf993Test extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;
    private final QName serviceName = new QName("urn://testnotification", "NotificationService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @Test
    public void testBasicConnection() throws Exception {
        assertEquals("dummy", getPort().sendNotification(new SendNotification()));
    }

    private NotificationServicePort getPort() 
        throws NumberFormatException, MalformedURLException {
        URL wsdl = getClass().getResource("/wsdl_systest/cxf-993.wsdl");
        assertNotNull("WSDL is null", wsdl);

        NotificationService service = new NotificationService(wsdl, serviceName);
        assertNotNull("Service is null ", service);

        NotificationServicePort port =  service.getNotificationServicePort();
        updateAddressPort(port, PORT);
        return port;
    }
}
