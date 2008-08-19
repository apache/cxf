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

package org.apache.cxf.systest.ws.addr_fromwsdl;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.soap.AddressingFeature;

import org.apache.cxf.systest.ws.AbstractWSATestBase;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersFault_Exception;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersPortType;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class WSAFromWSDLTest extends AbstractWSATestBase {

    private static final String BASE_URI = "http://apache.org/cxf/systest/ws/addr_feature/" 
        + "AddNumbersPortType/";

    private final QName serviceName = new QName("http://apache.org/cxf/systest/ws/addr_feature/",
                                                "AddNumbersService");

    @Before
    public void setUp() throws Exception {
        createBus();
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @Test
    public void testAddNumbers() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getPort();

        assertEquals(3, port.addNumbers(1, 2));

        String expectedOut = BASE_URI + "addNumbersRequest";
        String expectedIn = BASE_URI + "addNumbersResponse";
        
        assertTrue(output.toString().indexOf(expectedOut) != -1);
        assertTrue(input.toString().indexOf(expectedIn) != -1);
    }

    @Test
    public void testAddNumbers2() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getPort();

        assertEquals(3, port.addNumbers2(1, 2));

        String expectedOut = BASE_URI + "add2In";
        String expectedIn = BASE_URI + "add2Out";

        assertTrue(output.toString().indexOf(expectedOut) != -1);
        assertTrue(input.toString().indexOf(expectedIn) != -1);
    }


    @Test
    public void testAddNumbers3() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getPort();

        assertEquals(3, port.addNumbers3(1, 2));

        String expectedOut = "3in";
        String expectedIn = "3out";

        assertTrue(output.toString().indexOf(expectedOut) != -1);
        assertTrue(input.toString().indexOf(expectedIn) != -1);
    }


    @Test
    public void testAddNumbersFault() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getPort();

        try {
            port.addNumbers(-1, 2);
        } catch (AddNumbersFault_Exception ex) {
            assert true;
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }

        String expectedOut = BASE_URI + "addNumbersRequest";
        String expectedIn = BASE_URI + "Fault/addNumbersFault";

        assertTrue(output.toString().indexOf(expectedOut) != -1);
        assertTrue(input.toString().indexOf(expectedIn) != -1);
    }

    @Test
    public void testAddNumbersFault3() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getPort();

        try {
            port.addNumbers3(-1, 2);
        } catch (AddNumbersFault_Exception ex) {
            assert true;
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }

        String expectedOut = "3in";
        String expectedIn = "3fault";

        assertTrue(output.toString().indexOf(expectedOut) != -1);
        assertTrue(input.toString().indexOf(expectedIn) != -1);
    }

    private AddNumbersPortType getPort() {
        URL wsdl = getClass().getResource("/wsdl_systest/add_numbers.wsdl");
        assertNotNull("WSDL is null", wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull("Service is null ", service);
        return service.getAddNumbersPort(new AddressingFeature());
    }
}
