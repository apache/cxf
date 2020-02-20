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

package org.apache.cxf.systest.ws.servicelisting;

import java.io.InputStream;
import java.net.URL;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JAXWSServicesListingTest extends AbstractBusClientServerTestBase {
    public static final String PORT = GreeterServiceListing.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(GreeterServiceListing.class, true));
        createStaticBus();
    }

    @Test
    public void testServiceListing() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/service_listing/services";
        String expectedResult =
            "http://localhost:" + PORT + "/service_listing/services/SoapContext/GreeterPort";

        URL url = new URL(endpointAddress);
        try (InputStream input = url.openStream()) {
            String result = IOUtils.readStringFromStream(input);
            assertTrue(result.contains(expectedResult));
        }
    }

    @Test
    public void testServiceListingUnformatted() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/service_listing/services?formatted=false";
        String expectedResult =
            "http://localhost:" + PORT + "/service_listing/services/SoapContext/GreeterPort";

        URL url = new URL(endpointAddress);
        try (InputStream input = url.openStream()) {
            String result = IOUtils.readStringFromStream(input);
            assertTrue(result.contains(expectedResult));
        }
    }

    @Test
    public void testEscapeHTML() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/service_listing/services/<script>alert(1)</script>/../../";

        URL url = new URL(endpointAddress);
        try (InputStream input = url.openStream()) {
            String result = IOUtils.readStringFromStream(input);
            assertFalse(result.contains("<script>"));
        }
    }

    @Test
    public void testEscapeHTMLUnformatted() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/service_listing/services/<script>alert(1)</script>/../../?formatted=false";

        URL url = new URL(endpointAddress);
        try (InputStream input = url.openStream()) {
            String result = IOUtils.readStringFromStream(input);
            assertFalse(result.contains("<script>"));
        }
    }

}
