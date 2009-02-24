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

import java.math.BigInteger;
import java.util.Collection;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 */
public class RMManagerConfigurationTest extends Assert {

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
        assertNotNull(manager.getDeliveryAssurance().getAtLeastOnce());
        assertTrue(manager.getDeliveryAssurance().isSetAtLeastOnce());
        assertNotNull(manager.getDeliveryAssurance().getAtMostOnce());
        assertTrue(manager.getDeliveryAssurance().isSetAtMostOnce());
        assertNotNull(manager.getDeliveryAssurance().getExactlyOnce());
        assertTrue(manager.getDeliveryAssurance().isSetExactlyOnce());
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
        assertEquals(10000L, manager.getRMAssertion().getBaseRetransmissionInterval()
                     .getMilliseconds().longValue());
        assertEquals(10000L, manager.getRMAssertion().getAcknowledgementInterval()
                     .getMilliseconds().longValue());        
        TestStore store = (TestStore)manager.getStore();
        assertEquals("here", store.getLocation());     
        assertNotNull(manager.getDeliveryAssurance().getInOrder());
    }

    static class TestStore implements RMStore {
        
        private String location;
        
        public TestStore() {
            // this(null);
        }
        
        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }



        public void createDestinationSequence(DestinationSequence seq) {
            // TODO Auto-generated method stub
            
        }

        public void createSourceSequence(SourceSequence seq) {
            // TODO Auto-generated method stub
            
        }

        public Collection<DestinationSequence> getDestinationSequences(String endpointIdentifier) {
            // TODO Auto-generated method stub
            return null;
        }

        public Collection<RMMessage> getMessages(Identifier sid, boolean outbound) {
            // TODO Auto-generated method stub
            return null;
        }

        public Collection<SourceSequence> getSourceSequences(String endpointIdentifier) {
            // TODO Auto-generated method stub
            return null;
        }

        public void persistIncoming(DestinationSequence seq, RMMessage msg) {
            // TODO Auto-generated method stub
            
        }

        public void persistOutgoing(SourceSequence seq, RMMessage msg) {
            // TODO Auto-generated method stub
            
        }

        public void removeDestinationSequence(Identifier seq) {
            // TODO Auto-generated method stub
            
        }

        public void removeMessages(Identifier sid, Collection<BigInteger> messageNrs, boolean outbound) {
            // TODO Auto-generated method stub
            
        }

        public void removeSourceSequence(Identifier seq) {
            // TODO Auto-generated method stub
            
        }
        
    }
}
