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

package org.apache.cxf.ws.rm;

import java.util.Collection;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.ws.rm.RMConfiguration.DeliveryAssurance;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.v200702.Identifier;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class RMManagerConfigurationTest {

    private Bus bus;

    @After
    public void tearDown() {
        bus.shutdown(true);
        BusFactory.setDefaultBus(null);
    }

    @Test
    public void testManagerBean() {
        SpringBusFactory factory = new SpringBusFactory();
        bus = factory.createBus("org/apache/cxf/ws/rm/manager-bean.xml", false);
        RMManager manager = bus.getExtension(RMManager.class);
        verifyManager(manager);
    }

    @Test
    public void testExactlyOnce() {
        SpringBusFactory factory = new SpringBusFactory();
        bus = factory.createBus("org/apache/cxf/ws/rm/exactly-once.xml", false);
        RMManager manager = bus.getExtension(RMManager.class);
        RMConfiguration cfg = manager.getConfiguration();
        DeliveryAssurance da = cfg.getDeliveryAssurance();
        assertEquals(da, DeliveryAssurance.EXACTLY_ONCE);
        assertFalse(cfg.isInOrder());
    }

    @Test
    public void testFeature() {
        SpringBusFactory factory = new SpringBusFactory();
        bus = factory.createBus("org/apache/cxf/ws/rm/feature.xml");
        RMManager manager = bus.getExtension(RMManager.class);
        verifyManager(manager);
    }

    private void verifyManager(RMManager manager) {
        assertNotNull(manager);
        assertTrue(manager.getSourcePolicy().getSequenceTerminationPolicy().isTerminateOnShutdown());
        assertEquals(0L, manager.getDestinationPolicy().getAcksPolicy().getIntraMessageThreshold());
        assertEquals(2000L, manager.getDestinationPolicy().getAcksPolicy().getImmediaAcksTimeout());
        assertEquals(10000L, manager.getConfiguration().getBaseRetransmissionInterval().longValue());
        assertEquals(10000L, manager.getConfiguration().getAcknowledgementInterval().longValue());
        assertEquals("http://www.w3.org/2005/08/addressing", manager.getConfiguration().getRM10AddressingNamespace());
        TestStore store = (TestStore)manager.getStore();
        assertEquals("here", store.getLocation());
        assertTrue(manager.getConfiguration().isInOrder());
    }

    static class TestStore implements RMStore {

        private String location;

        TestStore() {
            // this(null);
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }



        public void createDestinationSequence(DestinationSequence seq) {
    
        }

        public void createSourceSequence(SourceSequence seq) {
    
        }

        public Collection<DestinationSequence> getDestinationSequences(String endpointIdentifier) {
            return null;
        }

        public Collection<RMMessage> getMessages(Identifier sid, boolean outbound) {
            return null;
        }

        public Collection<SourceSequence> getSourceSequences(String endpointIdentifier) {
            return null;
        }

        public void persistIncoming(DestinationSequence seq, RMMessage msg) {
    
        }

        public void persistOutgoing(SourceSequence seq, RMMessage msg) {
    
        }

        public void removeDestinationSequence(Identifier seq) {
    
        }

        public void removeMessages(Identifier sid, Collection<Long> messageNrs, boolean outbound) {
    
        }

        public void removeSourceSequence(Identifier seq) {
    
        }

        public SourceSequence getSourceSequence(Identifier seq) {
            return null;
        }

        public DestinationSequence getDestinationSequence(Identifier seq) {
            return null;
        }

    }
}