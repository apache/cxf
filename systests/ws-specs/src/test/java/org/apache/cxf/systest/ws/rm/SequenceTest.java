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

import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.Control;
import org.apache.cxf.greeter_control.ControlService;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.systest.ws.util.InMessageRecorder;
import org.apache.cxf.systest.ws.util.MessageFlow;
import org.apache.cxf.systest.ws.util.MessageRecorder;
import org.apache.cxf.systest.ws.util.OutMessageRecorder;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.rm.RMConstants;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.apache.cxf.ws.rm.RMInInterceptor;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMOutInterceptor;
import org.apache.cxf.ws.rm.RMProperties;
import org.apache.cxf.ws.rm.soap.RMSoapInterceptor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Tests the addition of WS-RM properties to application messages and the
 * exchange of WS-RM protocol messages.
 */
public class SequenceTest extends AbstractBusClientServerTestBase {

    private static final Logger LOG = LogUtils.getLogger(SequenceTest.class);
    private static final QName GREETMEONEWAY_NAME 
        = new QName("http://cxf.apache.org/greeter_control", "greetMeOneWay");
    private static final String GREETMEONEWAY_ACTION 
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeOneWayRequest";
    private static final QName GREETME_NAME 
        = new QName("http://cxf.apache.org/greeter_control", "greetMe");
    private static final String GREETME_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeRequest";
    private static final String GREETME_RESPONSE_ACTION
        = "http://cxf.apache.org/greeter_control/Greeter/greetMeResponse";

    private static int decoupledEndpointPort = 10000;
    private static String decoupledEndpoint;

    private Bus controlBus;
    private Control control;
    private Bus greeterBus;
    private Greeter greeter;
    private OutMessageRecorder outRecorder;
    private InMessageRecorder inRecorder;
    private Dispatch<DOMSource> dispatch;


    @BeforeClass
    public static void startServers() throws Exception {
        TestUtilities.setKeepAliveSystemProperty(false);
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }
    
    @AfterClass
    public static void cleanup() {
        TestUtilities.recoverKeepAliveSystemProperty();
    }
            
    
    @After
    public void tearDown() throws Exception {
        try {
            stopClient();
            stopControl();
        } catch (Throwable t) {
            //ignore
        }
        Thread.sleep(100);
    }

    /** 
      * Server is configured with RM interceptors, client without;
      * Addressing interceptors are installed on either side.
      * The (oneway) application request should be dispatched straight to the
      * implementor.
      */
    @Test
    @Ignore
    public void testRMServerPlainClient() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        
        controlBus = bf.createBus();
        BusFactory.setDefaultBus(controlBus);

        ControlService cs = new ControlService();
        control = cs.getControlPort();

        assertTrue("Failed to start greeter",
            control.startGreeter("org/apache/cxf/systest/ws/rm/rminterceptors.xml"));

        greeterBus = bf.createBus("org/apache/cxf/systest/ws/rm/rminterceptors.xml");
        BusFactory.setDefaultBus(greeterBus);
        removeRMInterceptors(greeterBus.getOutInterceptors());
        removeRMInterceptors(greeterBus.getOutFaultInterceptors());
        removeRMInterceptors(greeterBus.getInInterceptors());
        removeRMInterceptors(greeterBus.getInFaultInterceptors());
        LOG.fine("Initialised greeter bus with addressing but without RM interceptors");

        outRecorder = new OutMessageRecorder();
        greeterBus.getOutInterceptors().add(outRecorder);
        inRecorder = new InMessageRecorder();
        greeterBus.getInInterceptors().add(inRecorder);

        GreeterService gs = new GreeterService();
        greeter = gs.getGreeterPort();
        LOG.fine("Created greeter client.");

        ConnectionHelper.setKeepAliveConnection(greeter, true);

        greeter.greetMeOneWay("once");

    }

    // --- tests ---
    
    @Test
    public void testOnewayAnonymousAcks() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml");

        greeter.greetMeOneWay("once");
        greeter.greetMeOneWay("twice");
        greeter.greetMeOneWay("thrice");

        verifyOnewayAnonymousAcks();
    }

    @Test
    public void testOnewayAnonymousAcksProvider() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors_provider.xml");

        greeter.greetMeOneWay("once");
        greeter.greetMeOneWay("twice");
        greeter.greetMeOneWay("thrice");

        verifyOnewayAnonymousAcks();
    }

    @Test
    public void testOnewayAnonymousAcksDispatch() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", false, true);

        dispatch.getRequestContext().put(MessageContext.WSDL_OPERATION,
                                         GREETMEONEWAY_NAME);

        dispatch.invokeOneWay(getDOMRequest("One", true));
        dispatch.invokeOneWay(getDOMRequest("Two", true));
        dispatch.invokeOneWay(getDOMRequest("Three", true));

        verifyOnewayAnonymousAcks();
    }

    @Test
    public void testOnewayAnonymousAcksDispatchProvider() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors_provider.xml",
             false, 
             true);

        dispatch.getRequestContext().put(MessageContext.WSDL_OPERATION,
                                         GREETMEONEWAY_NAME);

        dispatch.invokeOneWay(getDOMRequest("One", true));
        dispatch.invokeOneWay(getDOMRequest("Two", true));
        dispatch.invokeOneWay(getDOMRequest("Three", true));

        verifyOnewayAnonymousAcks();
    }

    private void verifyOnewayAnonymousAcks() throws Exception {
        // three application messages plus createSequence

        awaitMessages(4, 4);
        
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());

        mf.verifyMessages(4, true);
        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(),
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, true);

        // createSequenceResponse plus 3 partial responses
        
        mf.verifyMessages(4, false);
        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(),
                                        RMConstants.getSequenceAcknowledgmentAction(),
                                        RMConstants.getSequenceAcknowledgmentAction(),
                                        RMConstants.getSequenceAcknowledgmentAction()};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, null, null, null}, false);
        mf.verifyAcknowledgements(new boolean[] {false, true, true, true}, false);
    }
    
    @Test
    public void testOnewayDeferredAnonymousAcks() throws Exception {
        init("org/apache/cxf/systest/ws/rm/deferred.xml");

        greeter.greetMeOneWay("once");
        greeter.greetMeOneWay("twice");

        try {
            Thread.sleep(3 * 1000);
        } catch (InterruptedException ex) {
            // ignore
        }

        greeter.greetMeOneWay("thrice");

        awaitMessages(4, 4);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
                
        // three application messages plus createSequence
        mf.verifyMessages(4, true);
        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(), GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION, GREETMEONEWAY_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, true);

        // createSequenceResponse message plus 3 partial responses, only the
        // last one should include a sequence acknowledgment

        mf.verifyMessages(4, false);
        expectedActions = 
            new String[] {RMConstants.getCreateSequenceResponseAction(), null, null, 
                          RMConstants.getSequenceAcknowledgmentAction()};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, null, null, null}, false);
        mf.verifyAcknowledgements(new boolean[] {false, false, false, true}, false);
    }
    
    @Test
    public void testOnewayDeferredNonAnonymousAcks() throws Exception {
        init("org/apache/cxf/systest/ws/rm/deferred.xml", true);

        greeter.greetMeOneWay("once");
        greeter.greetMeOneWay("twice");

        // CreateSequence plus two greetMeOneWay requests

        awaitMessages(3, 4);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        
        mf.verifyMessages(3, true);
        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(), 
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2"}, true);

        // CreateSequenceResponse plus three partial responses, no
        // acknowledgments included

        mf.verifyMessages(4, false);
        mf.verifyMessageNumbers(new String[4], false);
        mf.verifyAcknowledgements(new boolean[4], false);
        
        mf.verifyPartialResponses(3);        
        mf.purgePartialResponses();
  
        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction()};
        mf.verifyActionsIgnoringPartialResponses(expectedActions);
        mf.purge();
        
        try {
            Thread.sleep(3 * 1000);
        } catch (InterruptedException ex) {
            // ignore
        }

        // a standalone acknowledgement should have been sent from the server
        // side by now
        
        awaitMessages(0, 1);
        mf.reset(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());

        mf.verifyMessages(0, true);
        mf.verifyMessages(1, false);
        mf.verifyAcknowledgements(new boolean[] {true}, false);

    }
    
    @Test
    public void testOnewayAnonymousAcksSequenceLength1() throws Exception {
        init("org/apache/cxf/systest/ws/rm/seqlength1.xml");

        greeter.greetMeOneWay("once");
        greeter.greetMeOneWay("twice");

        // two application messages plus two createSequence plus two
        // terminateSequence

        awaitMessages(6, 6);
        
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        
        mf.verifyMessages(6, true);
        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(), 
                                                 GREETMEONEWAY_ACTION,
                                                 RMConstants.getTerminateSequenceAction(),
                                                 RMConstants.getCreateSequenceAction(), 
                                                 GREETMEONEWAY_ACTION,
                                                 RMConstants.getTerminateSequenceAction()};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", null, null, "1", null}, true);
        mf.verifyLastMessage(new boolean[] {false, true, false, false, true, false}, true);

        // createSequenceResponse message plus partial responses to
        // greetMeOneWay and terminateSequence ||: 2

        mf.verifyMessages(6, false);

        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(), 
                                        RMConstants.getSequenceAcknowledgmentAction(), null,
                                        RMConstants.getCreateSequenceResponseAction(), 
                                        RMConstants.getSequenceAcknowledgmentAction(), null};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, null, null, null, null, null}, false);
        mf.verifyLastMessage(new boolean[] {false, false, false, false, false, false}, false);
        mf.verifyAcknowledgements(new boolean[] {false, true, false, false, true, false}, false);
    }
   
    @Test
    public void testOnewayAnonymousAcksSuppressed() throws Exception {
        testOnewayAnonymousAcksSuppressed(null);
    }

    @Test
    public void testOnewayAnonymousAcksSuppressedAsyncExecutor() throws Exception {
        testOnewayAnonymousAcksSuppressed(Executors.newSingleThreadExecutor());
    }

    private void testOnewayAnonymousAcksSuppressed(Executor executor) throws Exception {

        init("org/apache/cxf/systest/ws/rm/suppressed.xml", false, executor);
 
        greeter.greetMeOneWay("once");
        greeter.greetMeOneWay("twice");
        greeter.greetMeOneWay("thrice");

        // three application messages plus createSequence
        
        awaitMessages(4, 4, 2000);
        
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        
        mf.verifyMessages(4, true);

        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(),
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION, 
                                                 GREETMEONEWAY_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, true);

        // createSequenceResponse plus 3 partial responses, none of which
        // contain an acknowledgment

        mf.verifyMessages(4, false);
        mf.verifyPartialResponses(3, new boolean[3]);
        mf.purgePartialResponses();
        
        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction()};
        mf.verifyActions(expectedActions, false);
        
        mf.purge();
        assertEquals(0, outRecorder.getOutboundMessages().size());
        assertEquals(0, inRecorder.getInboundMessages().size());

        // allow resends to kick in
        // await multiple of 3 resends to avoid shutting down server
        // in the course of retransmission - this is harmless but pollutes test output
        
        awaitMessages(3, 0, 7500);
        
    }
    
    @Test
    public void testTwowayNonAnonymous() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", true);

        assertEquals("ONE", greeter.greetMe("one"));
        assertEquals("TWO", greeter.greetMe("two"));
        assertEquals("THREE", greeter.greetMe("three"));

        verifyTwowayNonAnonymous();
    }

    @Test
    public void testTwowayNonAnonymousProvider() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors_provider.xml", true);

        assertEquals("ONE", greeter.greetMe("one"));
        assertEquals("TWO", greeter.greetMe("two"));
        assertEquals("THREE", greeter.greetMe("three"));

        verifyTwowayNonAnonymous();
    }

    @Test
    public void testTwowayNonAnonymousDispatch() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", true, true);

        dispatch.getRequestContext().put(MessageContext.WSDL_OPERATION,
                                         GREETME_NAME);

        verifyDOMResponse(dispatch.invoke(getDOMRequest("One")), "ONE");
        verifyDOMResponse(dispatch.invoke(getDOMRequest("Two")), "TWO");
        verifyDOMResponse(dispatch.invoke(getDOMRequest("Three")), "THREE");

        verifyTwowayNonAnonymous();
    }

    @Test
    public void testTwowayNonAnonymousDispatchProvider() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors_provider.xml",
             true,
             true);

        dispatch.getRequestContext().put(MessageContext.WSDL_OPERATION,
                                         GREETME_NAME);

        verifyDOMResponse(dispatch.invoke(getDOMRequest("One")), "ONE");
        verifyDOMResponse(dispatch.invoke(getDOMRequest("Two")), "TWO");
        verifyDOMResponse(dispatch.invoke(getDOMRequest("Three")), "THREE");

        verifyTwowayNonAnonymous();
    }

    private void verifyTwowayNonAnonymous() throws Exception {
    
        // CreateSequence and three greetMe messages
        // TODO there should be partial responses to the decoupled responses!

        awaitMessages(4, 8);
        
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        
        
        mf.verifyMessages(4, true);
        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(), 
                                                 GREETME_ACTION,
                                                 GREETME_ACTION, 
                                                 GREETME_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, true);
        mf.verifyLastMessage(new boolean[] {false, false, false, false}, true);
        mf.verifyAcknowledgements(new boolean[] {false, false, true, true}, true);

        // createSequenceResponse plus 3 greetMeResponse messages plus
        // one partial response for each of the four messages
        // the first partial response should no include an acknowledgement, the other three should

        mf.verifyMessages(8, false);
        mf.verifyPartialResponses(4, new boolean[4]);

        mf.purgePartialResponses();

        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(), 
                                        GREETME_RESPONSE_ACTION, 
                                        GREETME_RESPONSE_ACTION, 
                                        GREETME_RESPONSE_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, false);
        mf.verifyLastMessage(new boolean[4], false);
        mf.verifyAcknowledgements(new boolean[] {false, true, true, true}, false);
    }

    // the same as above but using endpoint specific interceptor configuration

    @Test
    public void testTwowayNonAnonymousEndpointSpecific() throws Exception {
        init("org/apache/cxf/systest/ws/rm/twoway-endpoint-specific.xml", true);


        greeter.greetMe("one");
        greeter.greetMe("two");
        greeter.greetMe("three");

        // CreateSequence and three greetMe messages
        // TODO there should be partial responses to the decoupled responses!

        awaitMessages(4, 8);
        
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        
        
        mf.verifyMessages(4, true);
        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(), 
                                                 GREETME_ACTION,
                                                 GREETME_ACTION, 
                                                 GREETME_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, true);
        mf.verifyLastMessage(new boolean[] {false, false, false, false}, true);
        mf.verifyAcknowledgements(new boolean[] {false, false, true, true}, true);

        // createSequenceResponse plus 3 greetMeResponse messages plus
        // one partial response for each of the four messages
        // the first partial response should no include an acknowledgement, the other three should

        mf.verifyMessages(8, false);
        mf.verifyPartialResponses(4, new boolean[4]);

        mf.purgePartialResponses();

        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(), 
                                        GREETME_RESPONSE_ACTION, 
                                        GREETME_RESPONSE_ACTION, 
                                        GREETME_RESPONSE_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, false);
        mf.verifyLastMessage(new boolean[4], false);
        mf.verifyAcknowledgements(new boolean[] {false, true, true, true}, false);
    }

    @Test
    public void testTwowayNonAnonymousDeferred() throws Exception {
        init("org/apache/cxf/systest/ws/rm/deferred.xml", true);

        greeter.greetMe("one");
        greeter.greetMe("two");

        // CreateSequence and three greetMe messages, no acknowledgments
        // included

        awaitMessages(3, 6);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        
        mf.verifyMessages(3, true);
        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(), 
                                                 GREETME_ACTION,
                                                 GREETME_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2"}, true);
        mf.verifyLastMessage(new boolean[3], true);
        mf.verifyAcknowledgements(new boolean[3], true);

        // CreateSequenceResponse plus 2 greetMeResponse messages plus
        // one partial response for each of the three messages no acknowledgments
        // included

        mf.verifyMessages(6, false);
        mf.verifyLastMessage(new boolean[6], false);
        mf.verifyAcknowledgements(new boolean[6], false);
        
        mf.verifyPartialResponses(3);
        mf.purgePartialResponses();
        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(), 
                                        GREETME_RESPONSE_ACTION, 
                                        GREETME_RESPONSE_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", "2"}, false);
        mf.purge();
        

        // one standalone acknowledgement should have been sent from the client and one
        // should have been received from the server
   
        awaitMessages(1, 0);
        mf.reset(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        
        mf.verifyMessageNumbers(new String[1], true);
        mf.verifyLastMessage(new boolean[1], true);
        mf.verifyAcknowledgements(new boolean[] {true}, true);

    }
    
    /**
     * A maximum sequence length of 2 is configured for the client only (server allows 10).
     * However, as we use the defaults regarding the including and acceptance
     * for inbound sequence offers and correlate offered sequences that are
     * included in a CreateSequence request and accepted with those that are
     * created on behalf of such a request, the server also tries terminate its
     * sequences. Note that as part of the sequence termination exchange a
     * standalone sequence acknowledgment needs to be sent regardless of whether
     * or nor acknowledgments are delivered steadily with every response.
     */
    @Test
    public void testTwowayNonAnonymousMaximumSequenceLength2() throws Exception {
        init("org/apache/cxf/systest/ws/rm/seqlength10.xml", true);
        
        RMManager manager = greeterBus.getExtension(RMManager.class);
        assertEquals("Unexpected maximum sequence length.", BigInteger.TEN, 
            manager.getSourcePolicy().getSequenceTerminationPolicy().getMaxLength());
        manager.getSourcePolicy().getSequenceTerminationPolicy().setMaxLength(
            new BigInteger("2"));
        
        greeter.greetMe("one");
        greeter.greetMe("two");
        greeter.greetMe("three");

        awaitMessages(7, 13, 5000);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        
        mf.verifyMessages(7, true);
        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(),
                                                 GREETME_ACTION,
                                                 GREETME_ACTION, 
                                                 RMConstants.getTerminateSequenceAction(),
                                                 RMConstants.getSequenceAckAction(),
                                                 RMConstants.getCreateSequenceAction(),
                                                 GREETME_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", null, null, null, "1"}, true);
        mf.verifyLastMessage(new boolean[] {false, false, true, false, false, false, false}, true);
        mf.verifyAcknowledgements(new boolean[] {false, false, true, false, true, false, false}, true);

        // 7 partial responses plus 2 full responses to CreateSequence requests
        // plus 3 full responses to greetMe requests plus server originiated
        // TerminateSequence request

        mf.verifyMessages(13, false);

        mf.verifyPartialResponses(7);

        mf.purgePartialResponses();

        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(),
                                        GREETME_RESPONSE_ACTION,
                                        GREETME_RESPONSE_ACTION, 
                                        RMConstants.getTerminateSequenceAction(),
                                        RMConstants.getCreateSequenceResponseAction(), 
                                        GREETME_RESPONSE_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", null, null, "1"}, false);
        boolean[] expected = new boolean[6];
        expected[2] = true;
        mf.verifyLastMessage(expected, false);
        expected[1] = true;
        expected[5] = true;
        mf.verifyAcknowledgements(expected, false);
    }
    
    @Test
    public void testTwowayAtMostOnce() throws Exception {
        doTestTwowayNoDuplicates("org/apache/cxf/systest/ws/rm/atmostonce.xml");
    }

    @Test
    public void testTwowayExactlyOnce() throws Exception {
        doTestTwowayNoDuplicates("org/apache/cxf/systest/ws/rm/exactlyonce.xml");
    }

    private void doTestTwowayNoDuplicates(String cfg) throws Exception {

        init(cfg);
        
        class MessageNumberInterceptor extends AbstractPhaseInterceptor {
            public MessageNumberInterceptor() {
                super(Phase.USER_LOGICAL);
            }
            
            public void handleMessage(Message m) {
                RMProperties rmps = RMContextUtils.retrieveRMProperties(m, true);
                if (null != rmps && null != rmps.getSequence()) {
                    rmps.getSequence().setMessageNumber(BigInteger.ONE);
                }
            }
        }
        greeterBus.getOutInterceptors().add(new MessageNumberInterceptor());
        RMManager manager = greeterBus.getExtension(RMManager.class);
        manager.getRMAssertion().getBaseRetransmissionInterval().setMilliseconds(new BigInteger("2000"));
        
        greeter.greetMe("one");
        try {
            greeter.greetMe("two");
            fail("Expected fault.");
        } catch (WebServiceException ex) {
            SoapFault sf = (SoapFault)ex.getCause();
            assertEquals("Unexpected fault code.", Soap11.getInstance().getReceiver(), sf.getFaultCode());
            assertNull("Unexpected sub code.", sf.getSubCode());
            assertTrue("Unexpected reason.", sf.getReason().endsWith("has already been delivered."));
        }
        
        // wait for resend to occur 
        
        awaitMessages(3, 3, 5000);
         
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());

        // Expected outbound:
        // CreateSequence 
        // + two requests
       
        String[] expectedActions = new String[3];
        expectedActions[0] = RMConstants.getCreateSequenceAction();        
        for (int i = 1; i < expectedActions.length; i++) {
            expectedActions[i] = GREETME_ACTION;
        }
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "1"}, true);
        mf.verifyLastMessage(new boolean[3], true);
        mf.verifyAcknowledgements(new boolean[3], true);
 
        // Expected inbound:
        // createSequenceResponse
        // + 1 response without acknowledgement
        // + 1 fault
        
        mf.verifyMessages(3, false);
        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(),
                                        GREETME_RESPONSE_ACTION, 
                                        null};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", null}, false);
        mf.verifyAcknowledgements(new boolean[3] , false);
        
    }
    
    @Test
    public void testUnknownSequence() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml");
        
        class SequenceIdInterceptor extends AbstractPhaseInterceptor {
            public SequenceIdInterceptor() {
                super(Phase.USER_LOGICAL);
            }
            
            public void handleMessage(Message m) {
                RMProperties rmps = RMContextUtils.retrieveRMProperties(m, true);
                if (null != rmps && null != rmps.getSequence()) {
                    rmps.getSequence().getIdentifier().setValue("UNKNOWN");
                }
            }
        }
        greeterBus.getOutInterceptors().add(new SequenceIdInterceptor());
        RMManager manager = greeterBus.getExtension(RMManager.class);
        manager.getRMAssertion().getBaseRetransmissionInterval().setMilliseconds(new BigInteger("2000"));
       
        try {
            greeter.greetMe("one");
            fail("Expected fault.");
        } catch (WebServiceException ex) {
            SoapFault sf = (SoapFault)ex.getCause();
            assertEquals("Unexpected fault code.", Soap11.getInstance().getSender(), sf.getFaultCode());
            assertNull("Unexpected sub code.", sf.getSubCode());
            assertTrue("Unexpected reason.", sf.getReason().endsWith("is not a known Sequence identifier."));
        }   
        
        // the third inbound message has a SequenceFault header
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        mf.verifySequenceFault(RMConstants.getUnknownSequenceFaultCode(), false, 1);
    }
    
    @Test
    public void testInactivityTimeout() throws Exception {
        init("org/apache/cxf/systest/ws/rm/inactivity-timeout.xml");
       
        greeter.greetMe("one");
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            // ignore
        }        
        
        try {
            greeter.greetMe("two");
            fail("Expected fault.");
        } catch (WebServiceException ex) {
            SoapFault sf = (SoapFault)ex.getCause();
            assertEquals("Unexpected fault code.", Soap11.getInstance().getSender(), sf.getFaultCode());
            assertNull("Unexpected sub code.", sf.getSubCode());
            assertTrue("Unexpected reason.", sf.getReason().endsWith("is not a known Sequence identifier."));
        }   
        
        awaitMessages(3, 3, 5000);
        
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        
        // Expected outbound:
        // CreateSequence 
        // + two requests (second request does not include acknowledgement for first response as 
        // in the meantime the client has terminated the sequence
       
        String[] expectedActions = new String[3];
        expectedActions[0] = RMConstants.getCreateSequenceAction();        
        for (int i = 1; i < expectedActions.length; i++) {
            expectedActions[i] = GREETME_ACTION;
        }
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2"}, true);
        mf.verifyLastMessage(new boolean[3], true);
        mf.verifyAcknowledgements(new boolean[] {false, false, false}, true);
 
        // Expected inbound:
        // createSequenceResponse
        // + 1 response with acknowledgement
        // + 1 fault without acknowledgement
        
        mf.verifyMessages(3, false);
        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(),
                                        GREETME_RESPONSE_ACTION,
                                        null};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", null}, false);
        mf.verifyAcknowledgements(new boolean[] {false, true, false} , false);
        
        // the third inbound message has a SequenceFault header
        
        mf.verifySequenceFault(RMConstants.getUnknownSequenceFaultCode(), false, 2);
     
    }

    @Test    
    public void testOnewayMessageLoss() throws Exception {
        // waite a while for the last bus shutdown
        Thread.sleep(5000);
        testOnewayMessageLoss(null);
    }
    
    @Test    
    public void testOnewayMessageLossAsyncExecutor() throws Exception {
        testOnewayMessageLoss(Executors.newSingleThreadExecutor());
    } 

    private void testOnewayMessageLoss(Executor executor) throws Exception {

        init("org/apache/cxf/systest/ws/rm/message-loss.xml", false, executor);
        
        greeterBus.getOutInterceptors().add(new MessageLossSimulator());
        RMManager manager = greeterBus.getExtension(RMManager.class);
        manager.getRMAssertion().getBaseRetransmissionInterval().setMilliseconds(new BigInteger("2000"));
        
        greeter.greetMeOneWay("one");
        greeter.greetMeOneWay("two");
        greeter.greetMeOneWay("three");
        greeter.greetMeOneWay("four");
        
        awaitMessages(7, 5, 10000);
        
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());

        // Expected outbound:
        // CreateSequence 
        // + 4 greetMe messages
        // + at least 2 resends (message may be resent multiple times depending
        // on the timing of the ACKs)
       
        String[] expectedActions = new String[7];
        expectedActions[0] = RMConstants.getCreateSequenceAction();        
        for (int i = 1; i < expectedActions.length; i++) {
            expectedActions[i] = GREETMEONEWAY_ACTION;
        }
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3", "4", "2", "4"}, true, false);
        mf.verifyLastMessage(new boolean[7], true);
        mf.verifyAcknowledgements(new boolean[7], true);
 
        // Expected inbound:
        // createSequenceResponse
        // + 2 partial responses to successfully transmitted messages
        // + 2 partial responses to resent messages
        
        mf.verifyMessages(5, false);
        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(),
                                        RMConstants.getSequenceAcknowledgmentAction(),
                                        RMConstants.getSequenceAcknowledgmentAction(),
                                        RMConstants.getSequenceAcknowledgmentAction(),
                                        RMConstants.getSequenceAcknowledgmentAction()};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, null, null, null, null}, false);
        mf.verifyAcknowledgements(new boolean[] {false, true, true, true, true}, false);
  
    }

    @Test
    public void testTwowayMessageLoss() throws Exception {
        testTwowayMessageLoss(null);
    }

    @Test
    public void testTwowayMessageLossAsyncExecutor() throws Exception {
        testTwowayMessageLoss(Executors.newSingleThreadExecutor());
    }
    
    private void testTwowayMessageLoss(Executor executor) throws Exception {

        init("org/apache/cxf/systest/ws/rm/message-loss.xml", true, executor);
        
        greeterBus.getOutInterceptors().add(new MessageLossSimulator());
        RMManager manager = greeterBus.getExtension(RMManager.class);
        manager.getRMAssertion().getBaseRetransmissionInterval().setMilliseconds(new BigInteger("2000"));

        greeter.greetMe("one");
        greeter.greetMe("two");
        greeter.greetMe("three");
        greeter.greetMe("four");
        
        awaitMessages(7, 10, 10000);
        
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());

        // Expected outbound:
        // CreateSequence 
        // + 4 greetMe messages
        // + 2 resends
       
        String[] expectedActions = new String[7];
        expectedActions[0] = RMConstants.getCreateSequenceAction();        
        for (int i = 1; i < expectedActions.length; i++) {
            expectedActions[i] = GREETME_ACTION;
        }
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "2", "3", "4", "4"}, true);
        mf.verifyLastMessage(new boolean[7], true);
        boolean[] expectedAcks = new boolean[7];
        for (int i = 2; i < expectedAcks.length; i++) {
            expectedAcks[i] = true;
        }
        mf.verifyAcknowledgements(expectedAcks , true);
 
        // Expected inbound:
        // createSequenceResponse 
        // + 4 greetMeResponse actions (to original or resent) 
        // + 5 partial responses (to CSR & each of the initial greetMe messages)
        // + at least 2 further partial response (for each of the resends)
        
        mf.verifyPartialResponses(5);
        mf.purgePartialResponses();
        
        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(),
                                        GREETME_RESPONSE_ACTION, GREETME_RESPONSE_ACTION,
                                        GREETME_RESPONSE_ACTION, GREETME_RESPONSE_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3", "4"}, false);
        mf.verifyAcknowledgements(new boolean[] {false, true, true, true, true}, false);
  
    }
    
    @Test
    public void testTwowayNonAnonymousNoOffer() throws Exception {
        init("org/apache/cxf/systest/ws/rm/no-offer.xml", true);        
        
        greeter.greetMe("one");
        // greeter.greetMe("two");

        // Outbound expected:
        // CreateSequence + greetMe + CreateSequenceResponse = 3 messages
  
        awaitMessages(3, 6);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        
        mf.verifyMessages(3, true);
        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(), 
                                                 GREETME_ACTION,
                                                 RMConstants.getCreateSequenceResponseAction()};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", null}, true);
        mf.verifyLastMessage(new boolean[] {false, false, false}, true);
        mf.verifyAcknowledgements(new boolean[] {false, false, false}, true);

        mf.verifyPartialResponses(3, new boolean[3]);
        mf.purgePartialResponses();

        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(),
                                        RMConstants.getCreateSequenceAction(), 
                                        GREETME_RESPONSE_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, null, "1"}, false);
        mf.verifyAcknowledgements(new boolean[] {false, false, false}, false);
    }

    @Test
    public void testConcurrency() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", true);

        int max = 5;
        for (int i = 0; i < max; i++) {
            greeter.greetMeAsync(Integer.toString(i));
        }

        // CreateSequence and five greetMe messages
        // full and partial responses to each

        awaitMessages(max + 1, (max * 2) + 1, 7500);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        
        mf.verifyMessages(max + 1, true);
        String[] expectedActions = new String[max + 1];
        expectedActions[0] = RMConstants.getCreateSequenceAction();
        for (int i = 1; i < expectedActions.length; i++) {
            expectedActions[i] = GREETME_ACTION;
        }
        mf.verifyActions(expectedActions, true);
    }
  
    @Test
    public void testMultiClientOneway() throws Exception {
        
        SpringBusFactory bf = new SpringBusFactory();
        String cfgResource = "org/apache/cxf/systest/ws/rm/rminterceptors.xml";            
        initControl(bf, cfgResource);
    
        class ClientThread extends Thread {
            
            Greeter greeter;
            Bus greeterBus;
            InMessageRecorder inRecorder;
            OutMessageRecorder outRecorder;  
            String id;
            
            ClientThread(SpringBusFactory bf, String cfgResource, int n) { 
                SequenceTest.this.initGreeter(bf, cfgResource, false, null);
                greeter = SequenceTest.this.greeter;
                greeterBus = SequenceTest.this.greeterBus;
                inRecorder = SequenceTest.this.inRecorder;
                outRecorder = SequenceTest.this.outRecorder;
                id = "client " + n;
            }
            
            public void run() {
                greeter.greetMeOneWay(id + ": once");
                greeter.greetMeOneWay(id + ": twice");
                greeter.greetMeOneWay(id + ": thrice");

                // three application messages plus createSequence

                awaitMessages(4, 4);
            }
        }
        
        ClientThread clients[] = new ClientThread[2];
        
        try {
            for (int i = 0; i < clients.length; i++) {
                clients[i] = new ClientThread(bf, cfgResource, i);
            }

            for (int i = 0; i < clients.length; i++) {
                clients[i].start();
            }

            for (int i = 0; i < clients.length; i++) {
                clients[i].join();
                MessageFlow mf = new MessageFlow(clients[i].outRecorder.getOutboundMessages(),
                                                 clients[i].inRecorder.getInboundMessages());

                mf.verifyMessages(4, true);
                String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(),
                                                         GREETMEONEWAY_ACTION, GREETMEONEWAY_ACTION,
                                                         GREETMEONEWAY_ACTION};
                mf.verifyActions(expectedActions, true);
                mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, true);

                // createSequenceResponse plus 3 partial responses

                mf.verifyMessages(4, false);
                expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(), 
                                                RMConstants.getSequenceAcknowledgmentAction(),
                                                RMConstants.getSequenceAcknowledgmentAction(),
                                                RMConstants.getSequenceAcknowledgmentAction()};
                mf.verifyActions(expectedActions, false);
                mf.verifyMessageNumbers(new String[] {null, null, null, null}, false);
                mf.verifyAcknowledgements(new boolean[] {false, true, true, true}, false);

            }
        } finally {
            for (int i = 0; i < clients.length; i++) {
                greeter = clients[i].greeter;
                greeterBus = clients[i].greeterBus;
                stopClient();                
            }
            greeter = null;
        }        
    }
    
    @Test
    public void testMultiClientTwoway() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        String cfgResource = "org/apache/cxf/systest/ws/rm/rminterceptors.xml";            
        initControl(bf, cfgResource);
    
        class ClientThread extends Thread {
            
            Greeter greeter;
            Bus greeterBus;
            InMessageRecorder inRecorder;
            OutMessageRecorder outRecorder;  
            String id;
            
            ClientThread(SpringBusFactory bf, String cfgResource, int n) { 
                SequenceTest.this.initGreeter(bf, cfgResource, true, null);
                greeter = SequenceTest.this.greeter;
                greeterBus = SequenceTest.this.greeterBus;
                inRecorder = SequenceTest.this.inRecorder;
                outRecorder = SequenceTest.this.outRecorder;
                id = "client " + n;
            }
            
            public void run() {
                greeter.greetMe(id + ": a");
                greeter.greetMe(id + ": b");
                greeter.greetMe(id + ": c");

                // three application messages plus createSequence

                awaitMessages(4, 8);
            }
        }
        
        ClientThread clients[] = new ClientThread[2];
        
        try {
            for (int i = 0; i < clients.length; i++) {
                clients[i] = new ClientThread(bf, cfgResource, i);
            }

            for (int i = 0; i < clients.length; i++) {
                clients[i].start();
            }

            for (int i = 0; i < clients.length; i++) {
                clients[i].join();
                MessageFlow mf = new MessageFlow(clients[i].outRecorder.getOutboundMessages(), 
                                                 clients[i].inRecorder.getInboundMessages());
                                
                mf.verifyMessages(4, true);
                String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(), 
                                                         GREETME_ACTION,
                                                         GREETME_ACTION, 
                                                         GREETME_ACTION};
                mf.verifyActions(expectedActions, true);
                mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, true);
                mf.verifyLastMessage(new boolean[] {false, false, false, false}, true);
                mf.verifyAcknowledgements(new boolean[] {false, false, true, true}, true);

                // createSequenceResponse plus 3 greetMeResponse messages plus
                // one partial response for each of the four messages
                // the first partial response should no include an acknowledgement, the other three should

                mf.verifyMessages(8, false);
                mf.verifyPartialResponses(4, new boolean[4]);

                mf.purgePartialResponses();

                expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(), 
                                                GREETME_RESPONSE_ACTION, 
                                                GREETME_RESPONSE_ACTION, 
                                                GREETME_RESPONSE_ACTION};
                mf.verifyActions(expectedActions, false);
                mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, false);
                mf.verifyLastMessage(new boolean[4], false);
                mf.verifyAcknowledgements(new boolean[] {false, true, true, true}, false);

            }
        } finally {
            for (int i = 0; i < clients.length; i++) {
                greeter = clients[i].greeter;
                greeterBus = clients[i].greeterBus;
                stopClient();                
            }
            greeter = null;
        }        
    }
    
    @Test
    public void testServerSideMessageLoss() throws Exception {
        init("org/apache/cxf/systest/ws/rm/message-loss-server.xml", true);
        
        // avoid client side message loss
        List<Interceptor> outInterceptors = greeterBus.getOutInterceptors();
        for (Interceptor i : outInterceptors) {
            if (i.getClass().equals(MessageLossSimulator.class)) {
                outInterceptors.remove(i);
                break;
            }
        }
        // avoid client side resends
        greeterBus.getExtension(RMManager.class).getRMAssertion().getBaseRetransmissionInterval()
            .setMilliseconds(new BigInteger("60000"));

        greeter.greetMe("one");
        greeter.greetMe("two");

        // outbound: CreateSequence and two greetMe messages 

        awaitMessages(3, 6);
        
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        
        
        mf.verifyMessages(3, true);
        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(), 
                                                 GREETME_ACTION, 
                                                 GREETME_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2"}, true);
        mf.verifyLastMessage(new boolean[] {false, false, false}, true);
        mf.verifyAcknowledgements(new boolean[] {false, false, true}, true);

        // createSequenceResponse plus 2 greetMeResponse messages plus
        // one partial response for each of the four messages
        // the first partial response should no include an acknowledgement, the other three should

        mf.verifyMessages(6, false);
        mf.verifyPartialResponses(3, new boolean[3]);

        mf.purgePartialResponses();

        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(), 
                                        GREETME_RESPONSE_ACTION, 
                                        GREETME_RESPONSE_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", "2"}, false);
        mf.verifyLastMessage(new boolean[3], false);
        mf.verifyAcknowledgements(new boolean[] {false, true, true}, false);
    }
     
    @Test
    public void testTerminateOnShutdown() throws Exception {
        init("org/apache/cxf/systest/ws/rm/terminate-on-shutdown.xml", true);
        
        greeter.greetMeOneWay("neutrophil");
        greeter.greetMeOneWay("basophil");
        greeter.greetMeOneWay("eosinophil");
        stopGreeterButNotCloseConduit();

        awaitMessages(6, 8);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        
        mf.verifyMessages(6, true);
        String[] expectedActions = new String[] {RMConstants.getCreateSequenceAction(), 
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION, 
                                                 GREETMEONEWAY_ACTION,
                                                 RMConstants.getLastMessageAction(),
                                                 RMConstants.getTerminateSequenceAction()};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3", "4", null}, true);
        
        // inbound: CreateSequenceResponse, out-of-band SequenceAcknowledgement
        // plus 6 partial responses
        
        mf.verifyMessages(8, false);
        mf.verifyMessageNumbers(new String[8], false);
        
        mf.verifyPartialResponses(6);
        mf.purgePartialResponses();
        
        
        expectedActions = new String[] {RMConstants.getCreateSequenceResponseAction(), 
                                        RMConstants.getSequenceAckAction()};
        mf.verifyActions(expectedActions, false);
        mf.verifyAcknowledgements(new boolean[] {false, true}, false);
        
    }    

    // --- test utilities ---

    private void init(String cfgResource) {
        init(cfgResource, false);
    }

    private void init(String cfgResource, boolean useDecoupledEndpoint) {
        init(cfgResource, useDecoupledEndpoint, false, null);
    }

    private void init(String cfgResource, boolean useDecoupledEndpoint, Executor executor) {
        init(cfgResource, useDecoupledEndpoint, false, executor);
    }

    private void init(String cfgResource, boolean useDecoupledEndpoint, boolean useDispatchClient) {
        init(cfgResource, useDecoupledEndpoint, useDispatchClient, null);
    }
    
    private void init(String cfgResource, 
                      boolean useDecoupledEndpoint, 
                      boolean useDispatchClient, 
                      Executor executor) {
        
        SpringBusFactory bf = new SpringBusFactory();
        initControl(bf, cfgResource);
        initGreeterBus(bf, cfgResource);
        if (useDispatchClient) {
            initDispatch(useDecoupledEndpoint);
        } else {
            initProxy(useDecoupledEndpoint, executor);
        }
    }
    
    private void initControl(SpringBusFactory bf, String cfgResource) {
        controlBus = bf.createBus();
        BusFactory.setDefaultBus(controlBus);

        ControlService cs = new ControlService();
        control = cs.getControlPort();
        
        assertTrue("Failed to start greeter", control.startGreeter(cfgResource));        
    }

    private void initGreeter(SpringBusFactory bf,
                             String cfgResource,
                             boolean useDecoupledEndpoint,
                             Executor executor) {
        initGreeterBus(bf, cfgResource);
        initProxy(useDecoupledEndpoint, executor);
    }
    
    private void initGreeterBus(SpringBusFactory bf,
                                String cfgResource) {
        greeterBus = bf.createBus(cfgResource);
        BusFactory.setDefaultBus(greeterBus);
        LOG.fine("Initialised greeter bus with configuration: " + cfgResource);

        outRecorder = new OutMessageRecorder();
        greeterBus.getOutInterceptors().add(outRecorder);
        inRecorder = new InMessageRecorder();
        greeterBus.getInInterceptors().add(inRecorder);
    }

    private void initDispatch(boolean useDecoupledEndpoint) {
        GreeterService gs = new GreeterService();
        dispatch = gs.createDispatch(GreeterService.GreeterPort,
                                     DOMSource.class, 
                                     Service.Mode.MESSAGE);
        dispatch.getRequestContext().put(
                                     BindingProvider.SOAPACTION_USE_PROPERTY, 
                                     false);

        if (useDecoupledEndpoint) {
            initDecoupledEndpoint(((DispatchImpl)dispatch).getClient());
        }
    }

    private void initProxy(boolean useDecoupledEndpoint, Executor executor) {        
        GreeterService gs = new GreeterService();

        if (null != executor) {
            gs.setExecutor(executor);
        }
   
        greeter = gs.getGreeterPort();
        LOG.fine("Created greeter client.");

        ConnectionHelper.setKeepAliveConnection(greeter, true);

        if (useDecoupledEndpoint) {
            initDecoupledEndpoint(ClientProxy.getClient(greeter));
        }
    }

    private void initDecoupledEndpoint(Client c) {
        // programatically configure decoupled endpoint that is guaranteed to
        // be unique across all test cases
        
        decoupledEndpointPort--;
        decoupledEndpoint = "http://localhost:" + decoupledEndpointPort + "/decoupled_endpoint";

        HTTPConduit hc = (HTTPConduit)(c.getConduit());
        HTTPClientPolicy cp = hc.getClient();
        cp.setDecoupledEndpoint(decoupledEndpoint);

        LOG.fine("Using decoupled endpoint: " + cp.getDecoupledEndpoint());
    }
    
    private void stopClient() {
        if (null != greeterBus) {
            
            //ensure we close the decoupled destination of the conduit,
            //so that release the port if the destination reference count hit zero
            if (greeter != null) {
                ClientProxy.getClient(greeter).getConduit().close();
            }
            if (dispatch != null) {
                ((DispatchImpl)dispatch).getClient().getConduit().close();
            }
            greeterBus.shutdown(true);
            greeter = null;
            dispatch = null;
            greeterBus = null;
        }
    }

    private void stopControl() {
        if (null != control) {  
            assertTrue("Failed to stop greeter", control.stopGreeter(null));
            controlBus.shutdown(true);
        }
    }
    
    private void stopGreeterButNotCloseConduit() {
        if (null != greeterBus) {
          
            greeterBus.shutdown(true);
            greeter = null;
            greeterBus = null;
        }
    }
    
    private void awaitMessages(int nExpectedOut, int nExpectedIn) {
        awaitMessages(nExpectedOut, nExpectedIn, 10000);
    }
    
    private void awaitMessages(int nExpectedOut, int nExpectedIn, int timeout) {
        MessageRecorder mr = new MessageRecorder(outRecorder, inRecorder);
        mr.awaitMessages(nExpectedOut, nExpectedIn, timeout);
    }

    private void removeRMInterceptors(List<Interceptor> interceptors) {
        for (Iterator<Interceptor> it = interceptors.iterator(); it.hasNext();) {
            Interceptor i = it.next();
            if (i instanceof RMSoapInterceptor
                || i instanceof RMOutInterceptor
                || i instanceof RMInInterceptor) {
                it.remove();
            }
        }
    }

    private DOMSource getDOMRequest(String n) throws Exception {
        return getDOMRequest(n, false);
    }

    private DOMSource getDOMRequest(String n, boolean oneway)
        throws Exception {
        InputStream is = 
            getClass().getResourceAsStream((oneway ? "oneway" : "twoway")
                                           + "Req" + n + ".xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document newDoc = builder.parse(is);
        return new DOMSource(newDoc);
    }

    private static String convertToString(DOMSource domSource)
        throws TransformerException {
        Transformer xformer =
            TransformerFactory.newInstance().newTransformer();
        StringWriter output = new StringWriter();
        xformer.transform(domSource, new StreamResult(output));
        return output.toString();
    }

    private static String parseResponse(DOMSource domResponse) {
        Element el = ((Document)domResponse.getNode()).getDocumentElement();
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        ns.put("ns", "http://cxf.apache.org/greeter_control/types");
        XPathUtils xp = new XPathUtils(ns);
        return (String)xp.getValue("/soap:Envelope/soap:Body"
                                   + "/ns:greetMeResponse/ns:responseType",
                                   el,
                                   XPathConstants.STRING);
    }

    private void verifyDOMResponse(DOMSource domResponse, String expected) 
        throws TransformerException {
        String s = convertToString(domResponse);
        assertTrue("expected: " + s + " to contain: " + expected,
                   s.indexOf(expected) != -1);
        assertEquals("unexpected response: " + s,
                     expected,
                     parseResponse(domResponse));
    }
}
