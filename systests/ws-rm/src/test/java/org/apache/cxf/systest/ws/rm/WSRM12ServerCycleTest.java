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

import java.io.Closeable;

import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.soap.AddressingFeature;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.greeter_control.Control;
import org.apache.cxf.greeter_control.ControlService;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.feature.RMFeature;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.persistence.jdbc.RMTxStore;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class WSRM12ServerCycleTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(WSRM12ServerCycleTest.class);
    private static final String CFG_PERSISTENT = "/org/apache/cxf/systest/ws/rm/persistent.xml";
    private static final String CFG_SIMPLE = "/org/apache/cxf/systest/ws/rm/simple.xml";

    private static final long DEFAULT_BASE_RETRANSMISSION_INTERVAL = 4000L;
    private static final long DEFAULT_ACKNOWLEDGEMENT_INTERVAL = 2000L;

    public static class Server extends AbstractBusTestServerBase {
        String port;
        String pfx;
        Endpoint ep;
        public Server(String[] args) {
            port = args[0];
            pfx = args[1];
        }

        protected void run()  {
            SpringBusFactory factory = new SpringBusFactory();
            Bus bus = factory.createBus();
            BusFactory.setDefaultBus(bus);
            setBus(bus);

            //System.out.println("Created control bus " + bus);
            ControlImpl implementor = new ControlImpl();
            implementor.setDbName(pfx + "-server");
            implementor.setAddress("http://localhost:" + port + "/SoapContext/GreeterPort");
            GreeterImpl greeterImplementor = new GreeterImpl();
            implementor.setImplementor(greeterImplementor);
            ep = Endpoint.publish("http://localhost:" + port + "/SoapContext/ControlPort", implementor);
            BusFactory.setDefaultBus(null);
            BusFactory.setThreadDefaultBus(null);
        }
        public void tearDown() {
            ep.stop();
            ep = null;
        }
        public static void main(String[] args) throws Exception {
            new Server(args).start();
        }
    }


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        RMTxStore.deleteDatabaseFiles("cxf7392-server", true);
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, null, new String[] {PORT, "cxf7392"}, true));

    }

    private String getPrefix() {
        return "cxf7392";
    }

    public static RMFeature wsrm() {
        return wsrm(DEFAULT_BASE_RETRANSMISSION_INTERVAL, DEFAULT_ACKNOWLEDGEMENT_INTERVAL);
    }

    public static RMFeature wsrm(long brtxInterval, long ackInterval) {
        RMAssertion.BaseRetransmissionInterval baseRetransmissionInterval
            = new RMAssertion.BaseRetransmissionInterval();
        baseRetransmissionInterval.setMilliseconds(Long.valueOf(brtxInterval));
        RMAssertion.AcknowledgementInterval acknowledgementInterval = new RMAssertion.AcknowledgementInterval();
        acknowledgementInterval.setMilliseconds(Long.valueOf(ackInterval));

        RMAssertion rmAssertion = new RMAssertion();
        rmAssertion.setAcknowledgementInterval(acknowledgementInterval);
        rmAssertion.setBaseRetransmissionInterval(baseRetransmissionInterval);

        AcksPolicyType acksPolicy = new AcksPolicyType();
        acksPolicy.setIntraMessageThreshold(0);
        DestinationPolicyType destinationPolicy = new DestinationPolicyType();
        destinationPolicy.setAcksPolicy(acksPolicy);

        RMFeature feature = new RMFeature();
        feature.setRMAssertion(rmAssertion);
        feature.setDestinationPolicy(destinationPolicy);
        feature.setRMNamespace(RM11Constants.NAMESPACE_URI);

        return feature;
    }

    @Test
    public void testPersistentSequences() throws Exception {
        runTest(CFG_PERSISTENT, false);
    }

    @Test
    public void testNonPersistentSequence() throws Exception {
        runTest(CFG_SIMPLE, true);
    }

    @Test
    public void testNonPersistentSequenceNoTransformer() throws Exception {
        try {
            //CXF-7392
            System.setProperty("javax.xml.transform.TransformerFactory", "foo.snarf");
            runTest(CFG_SIMPLE, true);
        } finally {
            System.clearProperty("javax.xml.transform.TransformerFactory");
        }
    }


    public void runTest(String cfg, boolean faultOnRestart) throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        ControlService cs = new ControlService();
        Control control = cs.getControlPort();
        ConnectionHelper.setKeepAliveConnection(control, false, true);
        updateAddressPort(control, PORT);

        assertTrue("Failed to start greeter", control.startGreeter(cfg));

        System.setProperty("db.name", getPrefix() + "-recovery");
        Bus greeterBus = new SpringBusFactory().createBus();
        System.clearProperty("db.name");
        BusFactory.setDefaultBus(greeterBus);

        // avoid early client resends
        greeterBus.getExtension(RMManager.class).getConfiguration()
            .setBaseRetransmissionInterval(Long.valueOf(60000));
        GreeterService gs = new GreeterService();
        Greeter greeter = gs.getGreeterPort(new LoggingFeature(), new AddressingFeature(), wsrm());
        updateAddressPort(greeter, PORT);

        greeter.greetMe("one");
        greeter.greetMe("two");
        greeter.greetMe("three");


        control.stopGreeter(cfg);

        //make sure greeter is down
        Thread.sleep(1000);
        control.startGreeter(cfg);

        //CXF-7392
        if (faultOnRestart) {
            try {
                greeter.greetMe("four");
            } catch (SOAPFaultException ex) {
                assertTrue(ex.getMessage().contains("wsrm:Identifier"));
                //expected, sequence identifier doesn't exist on other side
            }
        } else {
            // this should work as the sequence should be recovered on the server side
            greeter.greetMe("four");
        }


        ((Closeable)greeter).close();
        greeterBus.shutdown(true);
        control.stopGreeter(cfg);
        bus.shutdown(true);
    }

}
