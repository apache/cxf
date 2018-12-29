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

package org.apache.cxf.transport.local;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class LocalDestinationTest {

    /**
     * Tests if the status code is available after closing the destination so that it can be logged.
     * Note that this test verifies the current approach of setting the status code if it is not set earlier.
     *
     * @throws Exception
     */
    @Test
    public void testStatusCodeSetAfterClose() throws Exception {
        Bus bus = BusFactory.getDefaultBus();
        LocalTransportFactory factory = new LocalTransportFactory();

        EndpointInfo ei = new EndpointInfo(null, "http://schemas.xmlsoap.org/soap/http");
        ei.setAddress("http://localhost/test");

        LocalDestination d = (LocalDestination) factory.getDestination(ei, bus);
        MessageImpl m = new MessageImpl();

        Conduit conduit = factory.getConduit(ei, bus);
        m.put(LocalConduit.IN_CONDUIT, conduit);
        Exchange ex = new ExchangeImpl();
        ex.put(Bus.class, bus);
        m.setExchange(ex);

        Integer code = (Integer)m.get(Message.RESPONSE_CODE);
        assertNull(code);

        Conduit backChannel = d.getBackChannel(m);

        backChannel.close(m);

        code = (Integer)m.get(Message.RESPONSE_CODE);
        assertNotNull(code);
        assertEquals(200, code.intValue());
    }

}