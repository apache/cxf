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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.w3c.dom.NodeList;

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
import org.apache.cxf.jaxb.DatatypeFactory;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.systest.ws.util.MessageFlow;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.recorders.InMessageRecorder;
import org.apache.cxf.testutil.recorders.MessageRecorder;
import org.apache.cxf.testutil.recorders.OutMessageRecorder;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.VersionTransformer.Names200408;
import org.apache.cxf.ws.rm.DestinationSequence;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMProperties;
import org.apache.cxf.ws.rm.SourceSequence;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.v200702.Identifier;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the addition of WS-RM properties to application messages and the
 * exchange of WS-RM protocol messages.
 */
public class SequenceTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;

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
    private static final String RM10_GENERIC_FAULT_ACTION
        = "http://schemas.xmlsoap.org/ws/2004/08/addressing/fault";

    private static String decoupledEndpoint;
    private static int decoupledCount = 1;
    private Bus controlBus;
    private Control control;
    private Bus greeterBus;
    private Greeter greeter;
    private OutMessageRecorder outRecorder;
    private InMessageRecorder inRecorder;
    private Dispatch<DOMSource> dispatch;


    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }


    @After
    public void tearDown() throws Exception {
        try {
            stopClient();
            stopControl();
        } catch (Throwable t) {
            //ignore
        }
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

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        mf.verifyMessages(4, true);
        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, true);

        // createSequenceResponse plus 3 partial responses

        mf.verifyMessages(4, false);
        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION};
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

        awaitMessages(4, 2);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        // three application messages plus createSequence
        mf.verifyMessages(4, true);
        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION, GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION, GREETMEONEWAY_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, true);

        // createSequenceResponse message plus 3 partial responses, only the
        // last one should include a sequence acknowledgment

        mf.verifyMessages(2, false);
        expectedActions =
            new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                          RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, null}, false);
        mf.verifyAcknowledgements(new boolean[] {false, true}, false);
    }

    @Test
    public void testOnewayDeferredNonAnonymousAcks() throws Exception {
        init("org/apache/cxf/systest/ws/rm/deferred.xml", true);

        greeter.greetMeOneWay("once");
        greeter.greetMeOneWay("twice");

        // CreateSequence plus two greetMeOneWay requests

        awaitMessages(3, 1);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        mf.verifyMessages(3, true);
        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2"}, true);

        // CreateSequenceResponse plus three partial responses, no
        // acknowledgments included

        mf.verifyMessages(1, false);
        mf.verifyMessageNumbers(new String[1], false);
        mf.verifyAcknowledgements(new boolean[1], false);

        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION};
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

        awaitMessages(6, 4);

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        mf.verifyMessages(6, true);
        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 RM10Constants.TERMINATE_SEQUENCE_ACTION,
                                                 RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 RM10Constants.TERMINATE_SEQUENCE_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", null, null, "1", null}, true);
        mf.verifyLastMessage(new boolean[] {false, true, false, false, true, false}, true);

        // createSequenceResponse message plus partial responses to
        // greetMeOneWay and terminateSequence ||: 2

        mf.verifyMessages(4, false);

        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION,
                                        RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, null, null, null}, false);
        mf.verifyLastMessage(new boolean[] {false, false, false, false}, false);
        mf.verifyAcknowledgements(new boolean[] {false, true, false, true}, false);
    }

    @Test
    public void testOnewayAnonymousAcksClientSequenceDemarcation() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml");
        greeter.greetMeOneWay("once");

        ((BindingProvider)greeter).getRequestContext().
            put(RMManager.WSRM_LAST_MESSAGE_PROPERTY, Boolean.TRUE);
        greeter.greetMeOneWay("twice");

        ((BindingProvider)greeter).getRequestContext().
            remove(RMManager.WSRM_LAST_MESSAGE_PROPERTY);
        greeter.greetMeOneWay("thrice");

        // three application messages plus two createSequence plus one
        // terminateSequence

        awaitMessages(6, 5);

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        mf.verifyMessages(6, true);
        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 RM10Constants.TERMINATE_SEQUENCE_ACTION,
                                                 RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETMEONEWAY_ACTION};

        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", null, null, "1"}, true);
        mf.verifyLastMessage(new boolean[] {false, false, true, false, false, false}, true);

        // createSequenceResponse message plus partial responses to
        // greetMeOneWay and terminateSequence ||: 2

        mf.verifyMessages(5, false);

        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION,
                                        RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, null, null, null, null}, false);
        mf.verifyLastMessage(new boolean[] {false, false, false, false, false}, false);
        mf.verifyAcknowledgements(new boolean[] {false, true, true, false, true}, false);
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

        awaitMessages(4, 1, 1000);

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        mf.verifyMessages(4, true);

        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, true);

        // createSequenceResponse plus 3 partial responses, none of which
        // contain an acknowledgment

        mf.verifyMessages(1, false);

        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION};
        mf.verifyActions(expectedActions, false);

        mf.purge();
        assertEquals(0, outRecorder.getOutboundMessages().size());
        assertEquals(0, inRecorder.getInboundMessages().size());

        // allow resends to kick in
        // first duplicate received will trigger acknowledgement
        awaitMessages(1, 1, 3000);

        mf.reset(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages());
        mf.verifyMessages(1, true);
        mf.verifyMessages(1, false);
        mf.verifyAcknowledgements(new boolean[] {true}, false);

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

        // TODO: temporarily commented out for first version of new RM code
        //verifyTwowayNonAnonymous();
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

        // TODO: temporarily commented out for first version of new RM code
//        verifyTwowayNonAnonymous();
    }

    @Test
    public void testTwowayAnonymousSequenceLength1() throws Exception {
        init("org/apache/cxf/systest/ws/rm/seqlength1.xml");

        String v = greeter.greetMe("once");
        assertEquals("Unexpected response", "ONCE", v);
        // outbound: CS, greetReq,     TS, SA
        // inbound: CSR, greetResp+SA,   , TS

        awaitMessages(4, 3);

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        mf.verifyMessages(4, true);
        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETME_ACTION,
                                                 RM10Constants.TERMINATE_SEQUENCE_ACTION,
                                                 RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION};

        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", null, null}, true);
        mf.verifyLastMessage(new boolean[] {false, true, false, false}, true);
        mf.verifyAcknowledgements(new boolean[] {false, false, false, true}, true);

        mf.verifyMessages(3, false);

        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        GREETME_RESPONSE_ACTION,
                                        RM10Constants.TERMINATE_SEQUENCE_ACTION};

        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", null}, false);
        mf.verifyLastMessage(new boolean[] {false, true, false}, false);
        mf.verifyAcknowledgements(new boolean[] {false, true, false}, false);

    }

    private void verifyTwowayNonAnonymous() throws Exception {

        // CreateSequence and three greetMe messages
        // TODO there should be partial responses to the decoupled responses!

        awaitMessages(4, 4);

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);


        mf.verifyMessages(4, true);
        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
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

        mf.verifyMessages(4, false);

        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
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

        awaitMessages(4, 4);

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);


        mf.verifyMessages(4, true);
        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
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

        mf.verifyMessages(4, false);

        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
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

        awaitMessages(3, 3);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        mf.verifyMessages(3, true);
        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETME_ACTION,
                                                 GREETME_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2"}, true);
        mf.verifyLastMessage(new boolean[3], true);
        mf.verifyAcknowledgements(new boolean[3], true);

        // CreateSequenceResponse plus 2 greetMeResponse messages plus
        // one partial response for each of the three messages no acknowledgments
        // included

        mf.verifyMessages(3, false);
        mf.verifyLastMessage(new boolean[3], false);
        mf.verifyAcknowledgements(new boolean[3], false);

        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
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

    // A maximum sequence length of 2 is configured for the client only (server allows 10).
    // However, as we use the defaults regarding the including and acceptance
    // for inbound sequence offers and correlate offered sequences that are
    // included in a CreateSequence request and accepted with those that are
    // created on behalf of such a request, the server also tries terminate its
    // sequences.
    @Test
    public void testTwowayNonAnonymousMaximumSequenceLength2() throws Exception {
        init("org/apache/cxf/systest/ws/rm/seqlength10.xml", true);

        RMManager manager = greeterBus.getExtension(RMManager.class);
        assertEquals("Unexpected maximum sequence length.", 10,
            manager.getSourcePolicy().getSequenceTerminationPolicy().getMaxLength());
        manager.getSourcePolicy().getSequenceTerminationPolicy().setMaxLength(2);

        greeter.greetMe("one");
        greeter.greetMe("two");
        greeter.greetMe("three");

        awaitMessages(7, 6, 5000);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        mf.verifyMessages(7, true);
        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETME_ACTION,
                                                 GREETME_ACTION,
                                                 RM10Constants.TERMINATE_SEQUENCE_ACTION,
                                                 RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION,
                                                 RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETME_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", null, null, null, "1"}, true);
        mf.verifyLastMessage(new boolean[] {false, false, true, false, false, false, false}, true);
        mf.verifyAcknowledgements(new boolean[] {false, false, true, false, true, false, false}, true);

        // 7 partial responses plus 2 full responses to CreateSequence requests
        // plus 3 full responses to greetMe requests plus server originiated
        // TerminateSequence request

        mf.verifyMessages(6, false);

        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        GREETME_RESPONSE_ACTION,
                                        GREETME_RESPONSE_ACTION,
                                        RM10Constants.TERMINATE_SEQUENCE_ACTION,
                                        RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
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

        class MessageNumberInterceptor extends AbstractPhaseInterceptor<Message> {
            MessageNumberInterceptor() {
                super(Phase.PRE_STREAM);
            }

            public void handleMessage(Message m) {
                RMProperties rmps = RMContextUtils.retrieveRMProperties(m, true);
                if (null != rmps && null != rmps.getSequence()) {
                    rmps.getSequence().setMessageNumber(Long.valueOf(1));
                }
            }
        }
        greeterBus.getOutInterceptors().add(new MessageNumberInterceptor());
        RMManager manager = greeterBus.getExtension(RMManager.class);
        manager.getConfiguration().setBaseRetransmissionInterval(Long.valueOf(2000));

        greeter.greetMe("one");
        try {
            ((BindingProvider)greeter).getRequestContext().put("cxf.synchronous.timeout", 5000);
            String s = greeter.greetMe("two");
            fail("Expected timeout. Received response: " + s);
        } catch (WebServiceException ex) {
            assertTrue("Unexpected exception cause", ex.getCause() instanceof IOException);
            IOException ie = (IOException)ex.getCause();
            assertTrue("Unexpected IOException message", ie.getMessage().startsWith("Timed out"));
        }

        // wait for resend to occur

        awaitMessages(4, 3, 5000);

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        // Expected outbound:
        // CreateSequence
        // + two requests
        // + acknowledgement

        String[] expectedActions = new String[4];
        expectedActions[0] = RM10Constants.CREATE_SEQUENCE_ACTION;
        expectedActions[1] = GREETME_ACTION;
        expectedActions[2] = GREETME_ACTION;
        expectedActions[3] = RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION;
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "1", null}, true);
        mf.verifyLastMessage(new boolean[expectedActions.length], true);
        mf.verifyAcknowledgements(new boolean[] {false, false, false, true}, true);

        // Expected inbound:
        // createSequenceResponse
        // + 1 response without acknowledgement
        // + 1 acknowledgement/last message

        mf.verifyMessages(3, false);
        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        GREETME_RESPONSE_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", null}, false);
        mf.verifyAcknowledgements(new boolean[] {false, false, true}, false);
    }

    @Test
    public void testUnknownSequence() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml");

        class SequenceIdInterceptor extends AbstractPhaseInterceptor<Message> {
            SequenceIdInterceptor() {
                super(Phase.PRE_STREAM);
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
        manager.getConfiguration().setBaseRetransmissionInterval(Long.valueOf(2000));

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
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);
        mf.verifySequenceFault(RM10Constants.UNKNOWN_SEQUENCE_FAULT_QNAME, false, 1);
        String[] expectedActions = new String[3];
        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        RM10_GENERIC_FAULT_ACTION};
        mf.verifyActions(expectedActions, false);
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

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        // Expected outbound:
        // CreateSequence
        // + two requests (second request does not include acknowledgement for first response as
        // in the meantime the client has terminated the sequence

        String[] expectedActions = new String[3];
        expectedActions[0] = RM10Constants.CREATE_SEQUENCE_ACTION;
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
        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        GREETME_RESPONSE_ACTION,
                                        RM10_GENERIC_FAULT_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", null}, false);
        mf.verifyAcknowledgements(new boolean[] {false, true, false}, false);

        // the third inbound message has a SequenceFault header
        mf.verifySequenceFault(RM10Constants.UNKNOWN_SEQUENCE_FAULT_QNAME, false, 2);
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
        manager.getConfiguration().setBaseRetransmissionInterval(Long.valueOf(2000));

        greeter.greetMeOneWay("one");
        greeter.greetMeOneWay("two");
        greeter.greetMeOneWay("three");
        greeter.greetMeOneWay("four");

        awaitMessages(7, 5, 10000);

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        // Expected outbound:
        // CreateSequence
        // + 4 greetMe messages
        // + at least 2 resends (message may be resent multiple times depending
        // on the timing of the ACKs)

        String[] expectedActions = new String[7];
        expectedActions[0] = RM10Constants.CREATE_SEQUENCE_ACTION;
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
        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION};
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
        manager.getConfiguration().setBaseRetransmissionInterval(Long.valueOf(2000));

        greeter.greetMe("one");
        greeter.greetMe("two");
        greeter.greetMe("three");
        greeter.greetMe("four");

        awaitMessages(7, 5, 10000);

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        // Expected outbound:
        // CreateSequence
        // + 4 greetMe messages
        // + 2 resends

        String[] expectedActions = new String[7];
        expectedActions[0] = RM10Constants.CREATE_SEQUENCE_ACTION;
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
        mf.verifyAcknowledgements(expectedAcks, true);

        // Expected inbound:
        // createSequenceResponse
        // + 4 greetMeResponse actions (to original or resent)
        // + 5 partial responses (to CSR & each of the initial greetMe messages)
        // + at least 2 further partial response (for each of the resends)

        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
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

        awaitMessages(3, 3);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        mf.verifyMessages(3, true);
        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETME_ACTION,
                                                 RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", null}, true);
        mf.verifyLastMessage(new boolean[] {false, false, false}, true);
        mf.verifyAcknowledgements(new boolean[] {false, false, false}, true);

        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        RM10Constants.CREATE_SEQUENCE_ACTION,
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

        awaitMessages(max + 1, 1, 7500);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        mf.verifyMessages(max + 1, true);
        String[] expectedActions = new String[max + 1];
        expectedActions[0] = RM10Constants.CREATE_SEQUENCE_ACTION;
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

        ClientThread[] clients = new ClientThread[2];

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
                    clients[i].inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME,
                    RM10Constants.NAMESPACE_URI);

                mf.verifyMessages(4, true);
                String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
                                                         GREETMEONEWAY_ACTION, GREETMEONEWAY_ACTION,
                                                         GREETMEONEWAY_ACTION};
                mf.verifyActions(expectedActions, true);
                mf.verifyMessageNumbers(new String[] {null, "1", "2", "3"}, true);

                // createSequenceResponse plus 3 partial responses

                mf.verifyMessages(4, false);
                expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                                RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION,
                                                RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION,
                                                RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION};
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
                super("client " + n);
                SequenceTest.this.initGreeter(bf, cfgResource, true, null);
                greeter = SequenceTest.this.greeter;
                greeterBus = SequenceTest.this.greeterBus;
                inRecorder = SequenceTest.this.inRecorder;
                outRecorder = SequenceTest.this.outRecorder;
                id = "client " + n;
            }

            public void run() {
                String s = greeter.greetMe(id + ": a").toLowerCase();
                if (!s.contains(id)) {
                    System.out.println("Correlation problem <" + s + ">  <" + id + ">");
                }
                s = greeter.greetMe(id + ": b").toLowerCase();
                if (!s.contains(id)) {
                    System.out.println("Correlation problem <" + s + ">  <" + id + ">");
                }
                s = greeter.greetMe(id + ": c").toLowerCase();
                if (!s.contains(id)) {
                    System.out.println("Correlation problem <" + s + ">  <" + id + ">");
                }

                // three application messages plus createSequence

                awaitMessages(5, 4);
            }
        }

        ClientThread[] clients = new ClientThread[2];

        try {
            for (int i = 0; i < clients.length; i++) {
                clients[i] = new ClientThread(bf, cfgResource, i);
            }

            for (int i = 0; i < clients.length; i++) {
                clients[i].start();
            }
            for (int i = 0; i < clients.length; i++) {
                clients[i].join();
            }
            for (int i = 0; i < clients.length; i++) {
                MessageFlow mf = new MessageFlow(clients[i].outRecorder.getOutboundMessages(),
                    clients[i].inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME,
                    RM10Constants.NAMESPACE_URI);

                mf.verifyMessages(5, true);
                String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
                                                         GREETME_ACTION,
                                                         GREETME_ACTION,
                                                         GREETME_ACTION,
                                                         RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION};
                mf.verifyActions(expectedActions, true);
                mf.verifyMessageNumbers(new String[] {null, "1", "2", "3", null}, true);
                mf.verifyLastMessage(new boolean[] {false, false, false, false, false}, true);
                mf.verifyAcknowledgements(new boolean[] {false, false, true, true, true}, true);

                // createSequenceResponse plus 3 greetMeResponse messages plus
                // one sequence ack response.

                mf.verifyMessages(4, false);

                expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
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
        List<Interceptor<? extends Message>> outInterceptors = greeterBus.getOutInterceptors();
        for (Interceptor<? extends Message> i : outInterceptors) {
            if (i.getClass().equals(MessageLossSimulator.class)) {
                outInterceptors.remove(i);
                break;
            }
        }
        // avoid client side resends
        greeterBus.getExtension(RMManager.class).getConfiguration()
            .setBaseRetransmissionInterval(Long.valueOf(60000));

        greeter.greetMe("one");
        greeter.greetMe("two");

        // outbound: CreateSequence and two greetMe messages

        awaitMessages(3, 3);

        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);


        mf.verifyMessages(3, true);
        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETME_ACTION,
                                                 GREETME_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2"}, true);
        mf.verifyLastMessage(new boolean[] {false, false, false}, true);
        mf.verifyAcknowledgements(new boolean[] {false, false, true}, true);

        // createSequenceResponse plus 2 greetMeResponse messages plus
        // one partial response for each of the four messages
        // the first partial response should no include an acknowledgement, the other three should

        mf.verifyMessages(3, false);

        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        GREETME_RESPONSE_ACTION,
                                        GREETME_RESPONSE_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyMessageNumbers(new String[] {null, "1", "2"}, false);
        mf.verifyLastMessage(new boolean[3], false);
        mf.verifyAcknowledgements(new boolean[] {false, true, true}, false);
    }

    @Test
    public void testCreateSequenceAfterSequenceExpiration() throws Exception {
        init("org/apache/cxf/systest/ws/rm/expire-fast-seq.xml", true);

        RMManager manager = greeterBus.getExtension(RMManager.class);

        assertEquals("Unexpected expiration", DatatypeFactory.createDuration("PT5S"),
                     manager.getSourcePolicy().getSequenceExpiration());

        // phase one
        greeter.greetMeOneWay("one");
        greeter.greetMeOneWay("two");

        // let the first sequence expire
        Thread.sleep(8000);

        // expecting 3 outbounds and 2 inbounds
        awaitMessages(3, 2, 5000);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages(),
            Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        // CS, GA, GA
        mf.verifyMessages(3, true);
        verifyCreateSequenceAction(0, "PT5S", mf, true);

        String[] expectedActions = new String[] {RM10Constants.INSTANCE.getCreateSequenceAction(),
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2"}, true);

        mf.verifyAcknowledgementRange(1, 2);

        // phase two

        outRecorder.getOutboundMessages().clear();
        inRecorder.getInboundMessages().clear();

        greeter.greetMeOneWay("three");

        // expecting 2 outbounds and 2 inbounds
        awaitMessages(2, 2, 5000);

        mf = new MessageFlow(outRecorder.getOutboundMessages(), inRecorder.getInboundMessages(),
            Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        // CS, GA
        mf.verifyMessages(2, true);
        verifyCreateSequenceAction(0, "PT5S", mf, true);

        expectedActions = new String[] {RM10Constants.INSTANCE.getCreateSequenceAction(),
                                        GREETMEONEWAY_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1"}, true);

        // PR, CSR, PR, ACK
        mf.verifyMessages(2, false);

        expectedActions = new String[] {RM10Constants.INSTANCE.getCreateSequenceResponseAction(),
                                        RM10Constants.INSTANCE.getSequenceAckAction()};
        mf.verifyActions(expectedActions, false);

        mf.purge();
        assertEquals(0, outRecorder.getOutboundMessages().size());
        assertEquals(0, inRecorder.getInboundMessages().size());
    }

    @Test
    public void testTerminateOnShutdown() throws Exception {
        init("org/apache/cxf/systest/ws/rm/terminate-on-shutdown.xml", true);

        RMManager manager = greeterBus.getExtension(RMManager.class);
        // this test also verify the DB is correctly being updated during the shutdown
        RMMemoryStore store = new RMMemoryStore();
        manager.setStore(store);

        greeter.greetMeOneWay("neutrophil");
        greeter.greetMeOneWay("basophil");
        greeter.greetMeOneWay("eosinophil");
        stopGreeterButNotCloseConduit();

        awaitMessages(6, 2);
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);

        mf.verifyMessages(6, true);
        String[] expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 GREETMEONEWAY_ACTION,
                                                 RM10Constants.CLOSE_SEQUENCE_ACTION,
                                                 RM10Constants.TERMINATE_SEQUENCE_ACTION};
        mf.verifyActions(expectedActions, true);
        mf.verifyMessageNumbers(new String[] {null, "1", "2", "3", "4", null}, true);

        // inbound: CreateSequenceResponse, out-of-band SequenceAcknowledgement
        // plus 6 partial responses

        mf.verifyMessages(2, false);
        mf.verifyMessageNumbers(new String[2], false);

        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION};
        mf.verifyActions(expectedActions, false);
        mf.verifyAcknowledgements(new boolean[] {false, true}, false);

        // additional check to verify the operations performed on DB
        assertEquals("sequences not released from DB", 0, store.ssmap.size());
        assertEquals("messages not released from DB", 0, store.ommap.size());
        assertEquals("sequence not closed in DB", 1, store.ssclosed.size());
    }

    @Test
    public void testCreateSequenceRefused() throws Exception {
        init("org/apache/cxf/systest/ws/rm/limit-seqs.xml");

        RMManager manager = greeterBus.getExtension(RMManager.class);
        assertEquals("Unexpected maximum sequence count.", 1, manager.getDestinationPolicy().getMaxSequences());

        greeter.greetMe("one");

        //hold onto the greeter to keep the sequence open
        Closeable oldGreeter = (Closeable)greeter;

        // force greeter to be re-initialized so that a new sequence is created
        initProxy(false, null);

        try {
            greeter.greetMe("two");
            fail("Expected fault.");
        } catch (WebServiceException ex) {
            // sequence creation refused
        }

        // the third inbound message has a SequenceFault header
        MessageFlow mf = new MessageFlow(outRecorder.getOutboundMessages(),
            inRecorder.getInboundMessages(), Names200408.WSA_NAMESPACE_NAME, RM10Constants.NAMESPACE_URI);
        mf.verifySequenceFault(RM10Constants.CREATE_SEQUENCE_REFUSED_FAULT_QNAME, false, 2);
        String[] expectedActions = new String[3];
        expectedActions = new String[] {RM10Constants.CREATE_SEQUENCE_RESPONSE_ACTION,
                                        GREETME_RESPONSE_ACTION,
                                        RM10_GENERIC_FAULT_ACTION};
        mf.verifyActions(expectedActions, false);

        //now close the old greeter to cleanup the sequence
        oldGreeter.close();
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
        try {
            updateAddressPort(control, PORT);
        } catch (Exception ex) {
            //ignore
        }

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
        try {
            updateAddressPort(dispatch, PORT);
        } catch (Exception e) {
            //ignore
        }
        dispatch.getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, Boolean.FALSE);

        if (useDecoupledEndpoint) {
            initDecoupledEndpoint(((DispatchImpl<?>)dispatch).getClient());
        }
    }

    private void initProxy(boolean useDecoupledEndpoint, Executor executor) {
        GreeterService gs = new GreeterService();

        if (null != executor) {
            gs.setExecutor(executor);
        }

        greeter = gs.getGreeterPort();
        try {
            updateAddressPort(greeter, PORT);
        } catch (Exception e) {
            //ignore
        }
        LOG.fine("Created greeter client.");

        ConnectionHelper.setKeepAliveConnection(greeter, true);

        if (useDecoupledEndpoint) {
            initDecoupledEndpoint(ClientProxy.getClient(greeter));
        }
    }

    private void initDecoupledEndpoint(Client c) {
        // programatically configure decoupled endpoint that is guaranteed to
        // be unique across all test cases
        decoupledEndpoint = "http://localhost:"
            + allocatePort("decoupled-" + decoupledCount++) + "/decoupled_endpoint";

        HTTPConduit hc = (HTTPConduit)(c.getConduit());
        HTTPClientPolicy cp = hc.getClient();
        cp.setDecoupledEndpoint(decoupledEndpoint);

        LOG.fine("Using decoupled endpoint: " + cp.getDecoupledEndpoint());
    }

    private void stopClient() throws IOException {
        if (null != greeterBus) {
            //ensure we close the decoupled destination of the conduit,
            //so that release the port if the destination reference count hit zero
            if (greeter instanceof Closeable) {
                ((Closeable)greeter).close();
            }
            if (dispatch instanceof Closeable) {
                ((Closeable)dispatch).close();
            }
            greeterBus.shutdown(true);
            greeter = null;
            dispatch = null;
            greeterBus = null;
        }
    }

    private void stopControl() throws IOException {
        if (null != control) {
            assertTrue("Failed to stop greeter", control.stopGreeter(null));
            ((Closeable)control).close();
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
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter output = new StringWriter();
        transformer.transform(domSource, new StreamResult(output));
        return output.toString();
    }

    private static String parseResponse(DOMSource domResponse) {
        Element el = ((Document)domResponse.getNode()).getDocumentElement();
        Map<String, String> ns = new HashMap<>();
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

    public void verifyCreateSequenceAction(int index, String expiration, MessageFlow mf, boolean outbound)
        throws Exception {
        Document d = mf.getMessage(index, outbound);

        String expires = getCreateSequenceExpires(d);

        assertEquals("Unexpected expires-value", expiration, expires);
    }

    private String getCreateSequenceExpires(Document document) throws Exception {
        Element envelopeElement = document.getDocumentElement();
        QName qname = RM10Constants.INSTANCE.getCreateSequenceOperationName();
        NodeList nodes =
            envelopeElement.getElementsByTagNameNS(qname.getNamespaceURI(), qname.getLocalPart());

        if (nodes.getLength() == 1) {
            Element element = MessageFlow.getNamedElement((Element)nodes.item(0), "Expires");
            if (element != null) {
                return MessageFlow.getText(element);
            }
        }
        return null;
    }

    private static class RMMemoryStore implements RMStore {
        // during this particular test, the operations are expected to be invoked sequentially so use just HashMap
        Map<Identifier, SourceSequence> ssmap = new HashMap<>();
        Map<Identifier, DestinationSequence> dsmap = new HashMap<>();
        Map<Identifier, Collection<RMMessage>> ommap = new HashMap<>();
        Map<Identifier, Collection<RMMessage>> immap = new HashMap<>();
        Set<Identifier> ssclosed = new HashSet<>();

        @Override
        public void createSourceSequence(SourceSequence seq) {
            ssmap.put(seq.getIdentifier(), seq);
        }

        @Override
        public void createDestinationSequence(DestinationSequence seq) {
            dsmap.put(seq.getIdentifier(), seq);
        }

        @Override
        public SourceSequence getSourceSequence(Identifier seq) {
            return ssmap.get(seq);
        }

        @Override
        public DestinationSequence getDestinationSequence(Identifier seq) {
            return dsmap.get(seq);
        }

        @Override
        public void removeSourceSequence(Identifier seq) {
            ssmap.remove(seq);
        }

        @Override
        public void removeDestinationSequence(Identifier seq) {
            dsmap.remove(seq);
        }

        @Override
        public Collection<SourceSequence> getSourceSequences(String endpointIdentifier) {
            return ssmap.values();
        }

        @Override
        public Collection<DestinationSequence> getDestinationSequences(String endpointIdentifier) {
            return dsmap.values();
        }

        @Override
        public Collection<RMMessage> getMessages(Identifier sid, boolean outbound) {
            return outbound ? ommap.get(sid) : immap.get(sid);
        }

        @Override
        public void persistOutgoing(SourceSequence seq, RMMessage msg) {
            Collection<RMMessage> cm = getMessages(seq.getIdentifier(), ommap);
            if (msg != null) {
                //  update the sequence status and add the message
                cm.add(msg);
            } else {
                // update only the sequence status
                if (seq.isLastMessage()) {
                    ssclosed.add(seq.getIdentifier());
                }
            }
        }

        @Override
        public void persistIncoming(DestinationSequence seq, RMMessage msg) {
            Collection<RMMessage> cm = getMessages(seq.getIdentifier(), immap);
            if (msg != null) {
                //  update the sequence status and add the message
                cm.add(msg);
            } else {
                // update only the sequence status
            }
        }

        @Override
        public void removeMessages(Identifier sid, Collection<Long> messageNrs, boolean outbound) {
            removeMessages(sid, messageNrs, outbound ? ommap : immap);
        }

        private Collection<RMMessage> getMessages(Identifier seq, Map<Identifier, Collection<RMMessage>> map) {
            Collection<RMMessage> cm = map.get(seq);
            if (cm == null) {
                cm = new LinkedList<>();
                map.put(seq, cm);
            }
            return cm;
        }

        private void removeMessages(Identifier sid, Collection<Long> messageNrs,
                                    Map<Identifier, Collection<RMMessage>> map) {
            for (Iterator<RMMessage> it = map.get(sid).iterator(); it.hasNext();) {
                RMMessage m = it.next();
                if (messageNrs.contains(m.getMessageNumber())) {
                    it.remove();
                }
            }
            if (map.get(sid).isEmpty()) {
                map.remove(sid);
            }
        }
    }
}
