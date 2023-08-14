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

package org.apache.cxf.systest.ws.rm;

import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.ws.rm.AcknowledgementNotification;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class ManagedEndpointsTest extends AbstractClientServerTestBase {

    public static final String PORT = allocatePort(ManagedEndpointsTest.class);

    private static final String[] EMPTY_SIGNATURE = new String[0];
    private static final String[] ONESTRING_SIGNATURE = new String[]{"java.lang.String"};
    private static final String[] ONEBOOLEAN_SIGNATURE = new String[]{"boolean"};

    private static final String SERVER_CFG = "/org/apache/cxf/systest/ws/rm/managed-server.xml";
    private static final String CLIENT_CFG = "/org/apache/cxf/systest/ws/rm/managed-client.xml";

    private static final Logger LOG = LogUtils.getLogger(ManagedEndpointsTest.class);
    private static Bus serverBus;
    private Bus clientBus;

    public static class InProcessServer extends AbstractBusTestServerBase {
        private Endpoint ep;

        @Override
        public void run() {
            SpringBusFactory bf = new SpringBusFactory();
            serverBus = bf.createBus(SERVER_CFG);
            BusFactory.setDefaultBus(serverBus);
            setBus(serverBus);

            GreeterImpl implementor = new GreeterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";

            ep = Endpoint.create(implementor);
            ep.publish(address);

            LOG.info("Published greeter endpoint.");
        }
        @Override
        public void tearDown() throws Exception {
            ep.stop();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(InProcessServer.class));
    }

    @Before
    public void prepareClient() {
        SpringBusFactory bf = new SpringBusFactory();
        clientBus = bf.createBus(CLIENT_CFG);
        MessageLossSimulator mls = new MessageLossSimulator();
        clientBus.getOutInterceptors().add(mls);

        BusFactory.setDefaultBus(clientBus);
    }

    @After
    public void stopBus() throws Exception {
        clientBus.shutdown(true);
    }

    @Test
    public void testManagedEndpointsOneway() throws Exception {
        RMManager clientManager = clientBus.getExtension(RMManager.class);
        RMManager serverManager = serverBus.getExtension(RMManager.class);

        InstrumentationManager serverIM = serverBus.getExtension(InstrumentationManager.class);
        MBeanServer mbs = serverIM.getMBeanServer();
        assertNotNull("MBeanServer must be available.", mbs);

        ObjectName clientManagerName = RMUtils.getManagedObjectName(clientManager);
        ObjectName serverManagerName = RMUtils.getManagedObjectName(serverManager);

        Object o;
        GreeterService gs = new GreeterService();
        final Greeter greeter = gs.getGreeterPort();
        updateAddressPort(greeter, ManagedEndpointsTest.PORT);
        LOG.fine("Created greeter client.");

        org.apache.cxf.endpoint.Endpoint ep = ClientProxy.getClient(greeter).getEndpoint();
        String epId = RMUtils.getEndpointIdentifier(ep, clientBus);
        greeter.greetMeOneWay("one"); // sent

        o = mbs.invoke(clientManagerName, "getEndpointIdentifiers", null, null);
        verifyArray("Expected endpoint identifier", o, new String[]{epId}, true);

        o = mbs.invoke(serverManagerName, "getEndpointIdentifiers", null, null);
        verifyArray("Expected endpoint identifier", o, new String[]{epId}, true);

        ObjectName clientEndpointName = RMUtils.getManagedObjectName(clientManager, ep);
        // we need to find out serverEndpointName by using the query name
        ObjectName serverEndpointName = getEndpointName(mbs, serverManager);

        String sseqId = getSingleSourceSequenceId(mbs, clientEndpointName);

        o = mbs.invoke(clientEndpointName, "getCurrentSourceSequenceId", null, null);
        assertEquals("Expected sequence identifier", sseqId, o);

        o = mbs.invoke(serverEndpointName, "getDestinationSequenceIds", null, null);
        verifyArray("Expected sequence identifier", o, new String[]{sseqId}, false);

        String dseqId = getSingleDestinationSequenceId(mbs, clientEndpointName);

        testOperation(mbs, greeter, clientEndpointName, serverEndpointName, sseqId, dseqId);

        mbs.invoke(clientEndpointName, "terminateSourceSequence", new Object[]{sseqId}, ONESTRING_SIGNATURE);
        o = mbs.invoke(clientEndpointName, "getSourceSequenceIds",
            new Object[]{true}, ONEBOOLEAN_SIGNATURE);
        assertTrue("Source sequence terminated", o instanceof String[] && 0 == ((String[])o).length);

        mbs.invoke(clientEndpointName, "terminateDestinationSequence", new Object[]{dseqId}, ONESTRING_SIGNATURE);
        o = mbs.invoke(clientEndpointName, "getDestinationSequenceIds",
            new Object[]{}, EMPTY_SIGNATURE);
        assertTrue("Destination sequence terminated", o instanceof String[] && 0 == ((String[])o).length);

    }

    @Test
    public void testManagedEndpointsOneway12() throws Exception {
        RMManager clientManager = clientBus.getExtension(RMManager.class);
        RMManager serverManager = serverBus.getExtension(RMManager.class);

        InstrumentationManager serverIM = serverBus.getExtension(InstrumentationManager.class);
        MBeanServer mbs = serverIM.getMBeanServer();
        assertNotNull("MBeanServer must be available.", mbs);

        ObjectName clientManagerName = RMUtils.getManagedObjectName(clientManager);
        ObjectName serverManagerName = RMUtils.getManagedObjectName(serverManager);

        Object o;
        GreeterService gs = new GreeterService();
        final Greeter greeter = gs.getGreeterPort();
        updateAddressPort(greeter, ManagedEndpointsTest.PORT);
        LOG.fine("Created greeter client.");

        ClientProxy.getClient(greeter).getRequestContext().put(RMManager.WSRM_VERSION_PROPERTY,
            RM11Constants.NAMESPACE_URI);

        org.apache.cxf.endpoint.Endpoint ep = ClientProxy.getClient(greeter).getEndpoint();
        String epId = RMUtils.getEndpointIdentifier(ep, clientBus);
        greeter.greetMeOneWay("one"); // sent

        o = mbs.invoke(clientManagerName, "getEndpointIdentifiers", null, null);
        verifyArray("Expected endpoint identifier", o, new String[]{epId}, true);

        o = mbs.invoke(serverManagerName, "getEndpointIdentifiers", null, null);
        verifyArray("Expected endpoint identifier", o, new String[]{epId}, true);

        ObjectName clientEndpointName = RMUtils.getManagedObjectName(clientManager, ep);
        // we need to find out serverEndpointName by using the query name
        ObjectName serverEndpointName = getEndpointName(mbs, serverManager);

        String sseqId = getSingleSourceSequenceId(mbs, clientEndpointName);

        o = mbs.invoke(clientEndpointName, "getCurrentSourceSequenceId", null, null);
        assertEquals("Expected sequence identifier", sseqId, o);

        o = mbs.invoke(serverEndpointName, "getDestinationSequenceIds", null, null);
        verifyArray("Expected sequence identifier", o, new String[]{sseqId}, false);

        String dseqId = getSingleDestinationSequenceId(mbs, clientEndpointName);

        testOperation(mbs, greeter, clientEndpointName, serverEndpointName, sseqId, dseqId);

        mbs.invoke(clientEndpointName, "closeSourceSequence", new Object[]{sseqId}, ONESTRING_SIGNATURE);
        o = mbs.invoke(clientEndpointName, "getSourceSequenceIds",
            new Object[]{true}, ONEBOOLEAN_SIGNATURE);
        verifyArray("Expected sequence identifier", o, new String[]{sseqId}, true);

        mbs.invoke(clientEndpointName, "terminateSourceSequence", new Object[]{sseqId}, ONESTRING_SIGNATURE);
        o = mbs.invoke(clientEndpointName, "getSourceSequenceIds",
            new Object[]{true}, ONEBOOLEAN_SIGNATURE);
        assertTrue("Source sequence terminated", o instanceof String[] && 0 == ((String[])o).length);

        mbs.invoke(clientEndpointName, "terminateDestinationSequence", new Object[]{dseqId}, ONESTRING_SIGNATURE);
        o = mbs.invoke(clientEndpointName, "getDestinationSequenceIds",
            new Object[]{}, EMPTY_SIGNATURE);
        assertTrue("Destination sequence terminated", o instanceof String[] && 0 == ((String[])o).length);

    }

    private void testOperation(MBeanServer mbs, final Greeter greeter, ObjectName clientEndpointName,
        ObjectName serverEndpointName, String sseqId, String dseqId) throws ReflectionException,
        InstanceNotFoundException, MBeanException, InterruptedException {
        AcknowledgementListener listener = new AcknowledgementListener();
        mbs.addNotificationListener(clientEndpointName, listener, null, null);
        Object o;
        o = mbs.invoke(serverEndpointName, "getSourceSequenceIds",
                       new Object[]{true}, ONEBOOLEAN_SIGNATURE);
        verifyArray("Expected sequence identifier", o, new String[]{dseqId}, false);

        o = mbs.invoke(clientEndpointName, "getQueuedMessageTotalCount",
                       new Object[]{true}, ONEBOOLEAN_SIGNATURE);
        assertTrue("No queued message", o instanceof Integer && 0 == ((Integer)o).intValue());

        o = mbs.invoke(clientEndpointName, "getQueuedMessageCount",
                       new Object[]{sseqId, true}, new String[]{"java.lang.String", "boolean"});
        assertTrue("No queued message", o instanceof Integer && 0 == ((Integer)o).intValue());

        o = mbs.invoke(clientEndpointName, "getCurrentSourceSequence", null, null);
        verifySourceSequence(o, sseqId, 1, 0);

        o = mbs.invoke(clientEndpointName, "getSourceSequences",
                       new Object[]{true}, ONEBOOLEAN_SIGNATURE);
        assertTrue("One sequence message", o instanceof CompositeData[] && 1 == ((CompositeData[])o).length);
        verifySourceSequence(((CompositeData[])o)[0], sseqId, 1, 0);

        o = mbs.invoke(clientEndpointName, "getSourceSequenceAcknowledgedRange",
                       new Object[]{sseqId}, ONESTRING_SIGNATURE);
        verifyArray("Expected range", o, new Long[]{1L, 1L}, true);

        o = mbs.invoke(clientEndpointName, "getUnAcknowledgedMessageIdentifiers",
                       new Object[]{sseqId}, ONESTRING_SIGNATURE);
        assertTrue("No unacknowledged message", o instanceof Long[] && 0 == ((Long[])o).length);

        greeter.greetMeOneWay("two"); // getting lost
        greeter.greetMeOneWay("three"); // sent

        o = mbs.invoke(clientEndpointName, "getQueuedMessageTotalCount",
                       new Object[]{true}, ONEBOOLEAN_SIGNATURE);
        assertTrue("One queued message", o instanceof Integer && 1 == ((Integer)o).intValue());

        o = mbs.invoke(clientEndpointName, "getSourceSequenceAcknowledgedRange",
                       new Object[]{sseqId}, ONESTRING_SIGNATURE);
        verifyArray("Expected range", o, new Long[]{1L, 1L, 3L, 3L}, true);
        assertEquals(3L, listener.getLastAcknowledgement());

        o = mbs.invoke(clientEndpointName, "getUnAcknowledgedMessageIdentifiers",
                       new Object[]{sseqId}, ONESTRING_SIGNATURE);
        assertTrue("One unacknowledged message", o instanceof Long[] && 1 == ((Long[])o).length);

        o = mbs.invoke(clientEndpointName, "getRetransmissionStatus",
                       new Object[]{sseqId, 2}, new String[]{"java.lang.String", "long"});
        verifyRetransmissionStatus(o, 2L, 0);

        o = mbs.invoke(serverEndpointName, "getDestinationSequenceAcknowledgedRange",
                       new Object[]{sseqId}, ONESTRING_SIGNATURE);
        verifyArray("Expected range", o, new Long[]{1L, 1L, 3L, 3L}, true);

        // 3 sec retry interval
        LOG.info("waiting for 3 secs for the retry to complete ...");
        Thread.sleep(3000L);

        o = mbs.invoke(clientEndpointName, "getQueuedMessageTotalCount",
                       new Object[]{true}, ONEBOOLEAN_SIGNATURE);
        assertTrue(o instanceof Integer);
        int count = 0;
        while (((Integer)o).intValue() > 0) {
            Thread.sleep(200L);
            count++;
            if (count > 20) {
                fail("Failed to empty the resend queue");
            }
            o = mbs.invoke(clientEndpointName, "getQueuedMessageTotalCount",
                           new Object[]{true}, ONEBOOLEAN_SIGNATURE);
            assertTrue(o instanceof Integer);
        }
        assertTrue("No queued message " + o, o instanceof Integer && 0 == ((Integer)o).intValue());

        o = mbs.invoke(clientEndpointName, "getSourceSequenceAcknowledgedRange",
                       new Object[]{sseqId}, ONESTRING_SIGNATURE);
        verifyArray("Expected range", o, new Long[]{1L, 3L}, true);

        o = mbs.invoke(serverEndpointName, "getDestinationSequenceAcknowledgedRange",
                       new Object[]{sseqId}, ONESTRING_SIGNATURE);
        verifyArray("Expected range", o, new Long[]{1L, 3L}, true);

        o = mbs.invoke(clientEndpointName, "getUnAcknowledgedMessageIdentifiers",
                       new Object[]{sseqId}, ONESTRING_SIGNATURE);
        assertTrue("No unacknowledged message", o instanceof Long[] && 0 == ((Long[])o).length);
        
        assertEquals(2L, listener.getLastAcknowledgement());
    }

    private String getSingleDestinationSequenceId(MBeanServer mbs, ObjectName clientEndpointName)
        throws ReflectionException, InstanceNotFoundException, MBeanException {
        Object o;
        o = mbs.invoke(clientEndpointName, "getDestinationSequenceIds", null, null);
        assertTrue("One sequence expected", o instanceof String[] && 1 == ((String[])o).length);
        return ((String[])o)[0];
    }

    private String getSingleSourceSequenceId(MBeanServer mbs, ObjectName clientEndpointName)
        throws ReflectionException, InstanceNotFoundException, MBeanException {
        Object o;
        o = mbs.invoke(clientEndpointName, "getSourceSequenceIds",
                       new Object[]{true}, ONEBOOLEAN_SIGNATURE);
        assertTrue("One sequence expected", o instanceof String[] && 1 == ((String[])o).length);
        return ((String[])o)[0];
    }

    @Test
    public void testSuspendAndResumeSourceSequence() throws Exception {
        RMManager clientManager = clientBus.getExtension(RMManager.class);

        InstrumentationManager serverIM = serverBus.getExtension(InstrumentationManager.class);
        MBeanServer mbs = serverIM.getMBeanServer();
        assertNotNull("MBeanServer must be available.", mbs);

        Object o;
        GreeterService gs = new GreeterService();
        final Greeter greeter = gs.getGreeterPort();
        updateAddressPort(greeter, ManagedEndpointsTest.PORT);
        LOG.fine("Created greeter client.");

        org.apache.cxf.endpoint.Endpoint ep = ClientProxy.getClient(greeter).getEndpoint();

        ObjectName clientEndpointName = RMUtils.getManagedObjectName(clientManager, ep);

        greeter.greetMeOneWay("one"); // sent

        o = mbs.invoke(clientEndpointName, "getCurrentSourceSequenceId", null, null);
        assertTrue(o instanceof String);
        String sseqId = (String)o;

        o = mbs.invoke(clientEndpointName, "getUnAcknowledgedMessageIdentifiers",
                       new Object[]{sseqId}, ONESTRING_SIGNATURE);
        assertTrue("No unacknowledged message", o instanceof Long[] && 0 == ((Long[])o).length);

        greeter.greetMeOneWay("two"); // sent but suspended
        greeter.greetMeOneWay("three"); // sent but suspended

        o = mbs.invoke(clientEndpointName, "getQueuedMessageTotalCount",
                       new Object[]{true}, ONEBOOLEAN_SIGNATURE);
        assertTrue("One queued message", o instanceof Integer && 1 == ((Integer)o).intValue());

        mbs.invoke(clientEndpointName, "suspendSourceQueue",
                   new Object[]{sseqId}, ONESTRING_SIGNATURE);
        LOG.info("suspended the source queue: " + sseqId);


        // 3 sec retry interval + 1 sec
        LOG.info("waiting for 4 secs for the retry (suspended)...");
        Thread.sleep(4000L);

        o = mbs.invoke(clientEndpointName, "getQueuedMessageTotalCount",
                       new Object[]{true}, ONEBOOLEAN_SIGNATURE);
        assertTrue("One queued message", o instanceof Integer && 1 == ((Integer)o).intValue());

        mbs.invoke(clientEndpointName, "resumeSourceQueue",
                   new Object[]{sseqId}, ONESTRING_SIGNATURE);
        LOG.info("resumed the source queue: " + sseqId);

        o = mbs.invoke(clientEndpointName, "getQueuedMessageTotalCount",
                       new Object[]{true}, ONEBOOLEAN_SIGNATURE);
        int count = 0;
        assertTrue(o instanceof Integer);
        while (((Integer)o).intValue() > 0) {
            Thread.sleep(200L);
            count++;
            if (count > 100) {
                //up to 20 seconds to do the resend, should be within 3 or 4
                fail("Failed to empty the resend queue");
            }
            o = mbs.invoke(clientEndpointName, "getQueuedMessageTotalCount",
                           new Object[]{true}, ONEBOOLEAN_SIGNATURE);
            assertTrue(o instanceof Integer);
        }

        assertTrue("No queued messages", o instanceof Integer && 0 == ((Integer)o).intValue());
    }

    private static <T> void verifyArray(String desc, Object value, T[] target, boolean exact) {
        assertTrue(desc, target.getClass().isInstance(value));
        @SuppressWarnings("unchecked")
        T[] values = (T[])value;
        if (exact) {
            // exact-match
            assertEquals(desc + " length", target.length, values.length);
        } else {
            // partial-match (the values must contain the target)
            assertTrue(desc + " length", target.length <= values.length);
        }
        int d = 0;
        for (int i = 0; i < target.length; i++) {
            while (!target[i].equals(values[i + d])) {
                if (d >= values.length - target.length) {
                    break;
                }
                d++;
            }
            assertEquals(desc, target[i], values[i + d]);
        }
    }

    private static void verifySourceSequence(Object value, String sid, long num, int qsize) {
        assertTrue(value instanceof CompositeData);
        CompositeData cd = (CompositeData)value;
        verifyValue(cd, "sequenceId", sid);
        verifyValue(cd, "currentMessageNumber", num);
        verifyValue(cd, "queuedMessageCount", qsize);
    }

    private static void verifyRetransmissionStatus(Object value, long num, int count) {
        assertTrue(value instanceof CompositeData);
        CompositeData cd = (CompositeData)value;
        verifyValue(cd, "messageNumber", num);
        verifyValue(cd, "retries", count);
        Date now = new Date();
        if (count > 0) {
            assertTrue(now.after((Date)getValue(cd, "previous")));
        }
        assertTrue(now.before((Date)getValue(cd, "next")));
    }

    private static void verifyValue(CompositeData cd, String key, Object value) {
        Object o = getValue(cd, key);
        assertEquals("Expected value", value, o);
    }

    private static Object getValue(CompositeData cd, String key) {
        Object o = null;
        try {
            o = cd.get(key);
        } catch (Exception e) {
            fail("Unable to retrieve the value for " + key);
        }
        return o;
    }

    private static ObjectName getEndpointName(MBeanServer mbs, RMManager manager) throws Exception {
        ObjectName serviceEndpointQueryName = new ObjectName(
            ManagementConstants.DEFAULT_DOMAIN_NAME + ":" + ManagementConstants.BUS_ID_PROP
            + "=" + manager.getBus().getId() + "," + ManagementConstants.TYPE_PROP + "=WSRM.Endpoint,*");
        Set<?> s = mbs.queryNames(serviceEndpointQueryName, null);
        Iterator<?> it = s.iterator();
        return (ObjectName)it.next();
    }

    private static final class AcknowledgementListener implements NotificationListener {
        private AtomicLong lastAcknowledgement = new AtomicLong();

        @Override
        public void handleNotification(Notification notification, Object handback) {
            if (notification instanceof AcknowledgementNotification) {
                AcknowledgementNotification ack = (AcknowledgementNotification)notification;
                lastAcknowledgement.set(ack.getMessageNumber());
            }
        }

        public long getLastAcknowledgement() {
            return lastAcknowledgement.get();
        }
    }
}
