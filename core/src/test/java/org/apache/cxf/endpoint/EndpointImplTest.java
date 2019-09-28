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

package org.apache.cxf.endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.EndpointInfo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 *
 */
public class EndpointImplTest {

    @Test
    public void testEqualsAndHashCode() throws Exception {
        Bus bus = new ExtensionManagerBus();
        Service svc = new ServiceImpl();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://nowhere.com/bar/foo");
        EndpointInfo ei2 = new EndpointInfo();
        ei2.setAddress("http://nowhere.com/foo/bar");

        Endpoint ep = new EndpointImpl(bus, svc, ei);
        Endpoint ep1 = new EndpointImpl(bus, svc, ei);
        Endpoint ep2 = new EndpointImpl(bus, svc, ei2);

        int hashcode = ep.hashCode();
        int hashcode1 = ep1.hashCode();
        int hashcode2 = ep2.hashCode();

        assertEquals("hashcodes must be equal", hashcode, hashcode1);
        assertNotEquals("hashcodes must not be equal", hashcode, hashcode2);

        // assertEquals("reflexivity violated", ep, ep);
        assertNotEquals("two objects must not be equal", ep, ep1);
        assertNotEquals("two objects must not be equal", ep, ep2);

        ep.put("custom", Boolean.TRUE);

        assertEquals("hashcode must remain equal", hashcode, ep.hashCode());
    }

}
