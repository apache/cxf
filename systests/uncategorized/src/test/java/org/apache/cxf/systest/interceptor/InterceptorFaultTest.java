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

package org.apache.cxf.systest.interceptor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.event.PrintWriterEventSender;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.Control;
import org.apache.cxf.greeter_control.ControlImpl;
import org.apache.cxf.greeter_control.ControlService;
import org.apache.cxf.greeter_control.FaultThrowingInterceptor;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.greeter_control.PingMeFault;
import org.apache.cxf.greeter_control.types.FaultLocation;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseComparator;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.MAPAggregator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class InterceptorFaultTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

    private static final Logger LOG = LogUtils.getLogger(InterceptorFaultTest.class);

    private static final QName SOAP_FAULT_CODE = new QName("http://schemas.xmlsoap.org/soap/envelope/",
                                                           "Server");
    private static final String FAULT_MESSAGE = "Could not send Message.";

    private static final String CONTROL_PORT_ADDRESS =
        "http://localhost:" + PORT + "/SoapContext/ControlPort";

    private static int decoupledEndpointPort = 1;
    private static String decoupledEndpoint;


    /**
     * Tests that a fault thrown by a server side interceptor is reported back to
     * the client in appropriate form (plain Fault in case of one way requests,
     * SoapFault in case of two way requests).
     * Also demonstrates how an interceptor on the server out fault chain can
     * distinguish different fault modes (the capability to do so is crucial to
     * QOS interceptors such as the RM, addressing and policy interceptors).
     *
     */
    public static class Server extends AbstractBusTestServerBase {

        Endpoint ep;
        protected void run() {
            SpringBusFactory factory = new SpringBusFactory();
            Bus bus = factory.createBus();
            BusFactory.setDefaultBus(bus);
            setBus(bus);

            ControlImpl implementor = new ControlImpl();
            implementor.setAddress("http://localhost:" + PORT + "/SoapContext/GreeterPort");
            GreeterImpl greeterImplementor = new GreeterImpl();
            greeterImplementor.setThrowAlways(true);
            greeterImplementor.useLastOnewayArg(true);
            implementor.setImplementor(greeterImplementor);
            ep = Endpoint.publish(CONTROL_PORT_ADDRESS, implementor);
            LOG.fine("Published control endpoint.");
        }
        public void tearDown() {
            ep.stop();
            ep = null;
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    private Bus controlBus;
    private Control control;
    private Bus greeterBus;
    private Greeter greeter;
    private List<Phase> inPhases;
    private PhaseComparator comparator;
    private Phase postUnMarshalPhase;



    @BeforeClass
    public static void startServers() throws Exception {
        System.setProperty("org.apache.cxf.transports.http_jetty.DontClosePort." + PORT, "true");
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }
    @AfterClass
    public static void reset() {
        System.clearProperty("org.apache.cxf.transports.http_jetty.DontClosePort." + PORT);
        Bus b = BusFactory.getDefaultBus(false);
        if (b == null) {
            b = BusFactory.getThreadDefaultBus(false);
        }
        if (b == null) {
            b = BusFactory.getDefaultBus();
        }
        b.shutdown(true);
    }

    @After
    public void tearDown() throws Exception {
        if (null != greeter) {
            ((java.io.Closeable)greeter).close();
            assertTrue("Failed to stop greeter.", control.stopGreeter(null));
            greeterBus.shutdown(true);
            greeterBus = null;
        }
        if (null != control) {
            assertTrue("Failed to stop greeter", control.stopGreeter(null));
            ((java.io.Closeable)control).close();
            controlBus.shutdown(true);
        }
    }

    @Test
    public void testWithoutAddressing() throws Exception {
        testWithoutAddressing(false);
    }

    @Test
    public void testRobustWithoutAddressing() throws Exception {
        testWithoutAddressing(true);
    }

    @Test
    public void testRobustFailWithoutAddressingInUserLogicalPhase() throws Exception {

        setupGreeter("org/apache/cxf/systest/interceptor/no-addr.xml", false);

        control.setRobustInOnlyMode(true);

        // behaviour is identicial for all phases
        FaultLocation location = new org.apache.cxf.greeter_control.types.ObjectFactory()
            .createFaultLocation();
        location.setPhase("user-logical");

        control.setFaultLocation(location);

        try {
            // writer to grab the content of soap fault.
            // robust is not yet used at client's side, but I think it should
            StringWriter writer = new StringWriter();
            ((Client)greeter).getInInterceptors()
                .add(new LoggingInInterceptor(new PrintWriterEventSender(new PrintWriter(writer))));
            // it should tell CXF to convert one-way robust out faults into real SoapFaultException
            ((Client)greeter).getEndpoint().put(Message.ROBUST_ONEWAY, true);
            greeter.greetMeOneWay("oneway");
            fail("Oneway operation unexpectedly succeded for phase " + location.getPhase());
        } catch (SOAPFaultException ex) {
            //expected
        }
    }

    private void testWithoutAddressing(boolean robust) throws Exception {

        setupGreeter("org/apache/cxf/systest/interceptor/no-addr.xml", false);

        control.setRobustInOnlyMode(robust);

        // all interceptors pass
        testInterceptorsPass(robust);

        // behaviour is identicial for all phases
        FaultLocation location = new org.apache.cxf.greeter_control.types.ObjectFactory()
            .createFaultLocation();

        // test failure occuring before and after logical addressing interceptor
        // won't get a fault in case of oneways non-robust for the latter (partial response already sent)
        testInterceptorFail(inPhases, location, robust);
    }

    @Test
    public void testWithAddressingAnonymousReplies() throws Exception {
        testWithAddressingAnonymousReplies(false);
    }

    @Test
    public void testRobustWithAddressingAnonymousReplies() throws Exception {
        testWithAddressingAnonymousReplies(true);
    }

    private void testWithAddressingAnonymousReplies(boolean robust) throws Exception {
        setupGreeter("org/apache/cxf/systest/interceptor/addr.xml", false);

        control.setRobustInOnlyMode(robust);

        // all interceptors pass
        testInterceptorsPass(robust);

        // test failure in phases <= Phase.UNMARSHALL
        FaultLocation location = new org.apache.cxf.greeter_control.types.ObjectFactory()
            .createFaultLocation();
        location.setAfter(MAPAggregator.class.getName());

        // test failure occuring before and after logical addressing interceptor
        // won't get a fault in case of oneways non-robust for the latter (partial response already sent)
        testInterceptorFail(inPhases, location, robust);
    }


    private void testInterceptorFail(List<Phase> phases, FaultLocation location, boolean robust)
        throws PingMeFault {
        for (Phase p : phases) {
            location.setPhase(p.getName());
            if (Phase.POST_INVOKE.equals(p.getName())) {
                break;
            }
            testFail(location, true, robust);
        }
    }

    private void testInterceptorsPass(boolean robust) {
        greeter.greetMeOneWay("one");

        // wait 5 seconds for the non-robust case
        if (!robust) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        // verify both the previous greetMeOneWay call and this greetMe call
        assertEquals("one", greeter.greetMe("two"));
        try {
            greeter.pingMe();
            fail("Expected PingMeFault not thrown.");
        } catch (PingMeFault f) {
            assertEquals(20, f.getFaultInfo().getMajor());
            assertEquals(10, f.getFaultInfo().getMinor());
        }
    }

    private void testFail(FaultLocation location, boolean usingAddressing, boolean robust)
        throws PingMeFault {
        // System.out.print("Test interceptor failing in phase: " + location.getPhase());

        control.setFaultLocation(location);

        // oneway reports a plain fault (although server sends a soap fault)

        boolean expectOnewayFault = robust;
        if (comparator.compare(getPhase(location.getPhase()), postUnMarshalPhase) < 0) {
            expectOnewayFault = true;
        }

        try {
            greeter.greetMeOneWay("oneway");
            if (expectOnewayFault) {
                fail("Oneway operation unexpectedly succeded for phase " + location.getPhase());
            }
        } catch (WebServiceException ex) {
            if (!expectOnewayFault) {
                fail("Oneway operation unexpectedly failed.");
            }
            assertEquals(FAULT_MESSAGE, ex.getMessage());
        }

        String expectedMsg = getExpectedInterceptorFaultMessage(location.getPhase());
        try {
            greeter.greetMe("cxf");
            fail("Twoway operation unexpectedly succeded.");
        } catch (WebServiceException ex) {
            Throwable cause = ex.getCause();
            SoapFault sf = (SoapFault)cause;

            assertEquals(expectedMsg, sf.getReason());
            assertEquals(SOAP_FAULT_CODE, sf.getFaultCode());
        }

        try {
            greeter.pingMe();
            fail("Expected PingMeFault not thrown.");
        } catch (WebServiceException ex) {
            Throwable cause = ex.getCause();
            SoapFault sf = (SoapFault)cause;
            assertEquals(expectedMsg, sf.getReason());
            assertEquals(SOAP_FAULT_CODE, sf.getFaultCode());
        }
    }



    private void setupGreeter(String cfgResource, boolean useDecoupledEndpoint)
        throws NumberFormatException, MalformedURLException {

        SpringBusFactory bf = new SpringBusFactory();

        controlBus = bf.createBus();
        BusFactory.setDefaultBus(controlBus);

        ControlService cs = new ControlService();
        control = cs.getControlPort();
        updateAddressPort(control, PORT);

        assertTrue("Failed to start greeter", control.startGreeter(cfgResource));

        greeterBus = bf.createBus(cfgResource);
        BusFactory.setDefaultBus(greeterBus);
        LOG.fine("Initialised greeter bus with configuration: " + cfgResource);

        if (null == comparator) {
            comparator = new PhaseComparator();
        }
        if (null == inPhases) {
            inPhases = new ArrayList<>();
            inPhases.addAll(greeterBus.getExtension(PhaseManager.class).getInPhases());
            Collections.sort(inPhases, comparator);
        }
        if (null == postUnMarshalPhase) {
            postUnMarshalPhase = getPhase(Phase.POST_UNMARSHAL);
        }

        GreeterService gs = new GreeterService();

        greeter = gs.getGreeterPort();
        updateAddressPort(greeter, PORT);
        LOG.fine("Created greeter client.");

        if (!useDecoupledEndpoint) {
            return;
        }

        // programatically configure decoupled endpoint that is guaranteed to
        // be unique across all test cases
        decoupledEndpointPort++;
        decoupledEndpoint = "http://localhost:"
            + allocatePort("decoupled-" + decoupledEndpointPort)
            + "/decoupled_endpoint";

        Client c = ClientProxy.getClient(greeter);
        HTTPConduit hc = (HTTPConduit)(c.getConduit());
        HTTPClientPolicy cp = hc.getClient();
        cp.setDecoupledEndpoint(decoupledEndpoint);

        LOG.fine("Using decoupled endpoint: " + cp.getDecoupledEndpoint());
    }

    private String getExpectedInterceptorFaultMessage(String phase) {
        return FaultThrowingInterceptor.MESSAGE_FORMAT.format(new Object[] {phase}).toUpperCase();
    }

    private Phase getPhase(String name) {
        for (Phase p : inPhases) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }
}
