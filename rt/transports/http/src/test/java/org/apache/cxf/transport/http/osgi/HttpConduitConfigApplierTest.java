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

package org.apache.cxf.transport.http.osgi;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.apache.cxf.transport.https.InsecureTrustManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HttpConduitConfigApplierTest {

    @Test
    public void testNormalTrustLoading() throws IOException {
        HttpConduitConfigApplier configApplier = new HttpConduitConfigApplier();

        Dictionary<String, String> configValues = new Hashtable<>();
        configValues.put("tlsClientParameters.disableCNCheck", "false");
        String address = "https://localhost:12345";
        Bus bus = new ExtensionManagerBus();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(address);
        HTTPConduit conduit = new URLConnectionHTTPConduit(bus, ei, null);

        configApplier.apply(configValues, conduit, address);

        assertNull(conduit.getTlsClientParameters().getTrustManagers());
        assertFalse(conduit.getTlsClientParameters().isDisableCNCheck());
    }

    @Test
    public void testDisableTrustVerification() throws IOException {
        HttpConduitConfigApplier configApplier = new HttpConduitConfigApplier();

        Dictionary<String, String> configValues = new Hashtable<>();
        configValues.put("tlsClientParameters.disableCNCheck", "true");
        configValues.put("tlsClientParameters.trustManagers.disableTrustVerification", "true");
        String address = "https://localhost:12345";
        Bus bus = new ExtensionManagerBus();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(address);
        HTTPConduit conduit = new URLConnectionHTTPConduit(bus, ei, null);

        configApplier.apply(configValues, conduit, address);

        assertNotNull(conduit.getTlsClientParameters().getTrustManagers());
        assertEquals(conduit.getTlsClientParameters().getTrustManagers().length, 1);
        assertTrue(conduit.getTlsClientParameters().getTrustManagers()[0].getClass()
                .getName().startsWith(InsecureTrustManager.class.getName()));
        assertTrue(conduit.getTlsClientParameters().isDisableCNCheck());
    }

    @Test
    public void testTrustVerificationEnabled() throws IOException {
        HttpConduitConfigApplier configApplier = new HttpConduitConfigApplier();

        Dictionary<String, String> configValues = new Hashtable<>();
        configValues.put("tlsClientParameters.disableCNCheck", "true");
        configValues.put("tlsClientParameters.trustManagers.disableTrustVerification", "false");
        String address = "https://localhost:12345";
        Bus bus = new ExtensionManagerBus();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(address);
        HTTPConduit conduit = new URLConnectionHTTPConduit(bus, ei, null);

        configApplier.apply(configValues, conduit, address);

        assertNotNull(conduit.getTlsClientParameters().getTrustManagers());
        assertEquals(conduit.getTlsClientParameters().getTrustManagers().length, 1);
        assertFalse(conduit.getTlsClientParameters().getTrustManagers()[0].getClass()
                .getName().startsWith(InsecureTrustManager.class.getName()));
        assertTrue(conduit.getTlsClientParameters().isDisableCNCheck());
    }

}