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

package org.apache.cxf.ws.addressing.impl;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import jakarta.xml.ws.RequestWrapper;
import jakarta.xml.ws.ResponseWrapper;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.bus.managers.PhaseManagerImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo.Type;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.WSAContextUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.apache.cxf.binding.soap.Soap11.SOAP_NAMESPACE;
import static org.apache.cxf.message.Message.REQUESTOR_ROLE;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MAPAggregatorTest {

    private MAPAggregatorImpl aggregator;
    private AddressingProperties expectedMAPs;
    private String expectedTo;
    private String expectedReplyTo;
    private String expectedRelatesTo;
    private String expectedAction;

    @Before
    public void setUp() {
        aggregator = new MAPAggregatorImpl();
    }

    @After
    public void tearDown() {
        expectedMAPs = null;
        expectedTo = null;
        expectedReplyTo = null;
        expectedRelatesTo = null;
        expectedAction = null;
    }

    @Test
    public void testRequestorOutboundUsingAddressingMAPsInContext()
        throws Exception {
        Message message = setUpMessage(true, true, false, true, true);
        aggregator.mediate(message, false);
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundUsingAddressingMAPsInContextZeroLengthAction()
        throws Exception {
        Message message = setUpMessage(true, true, false, true, true, true);
        aggregator.mediate(message, false);
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundUsingAddressingMAPsInContextFault()
        throws Exception {
        Message message = setUpMessage(true, true, false, true, true);
        aggregator.mediate(message, true);
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundUsingAddressingNoMAPsInContext()
        throws Exception {
        Message message = setUpMessage(true, true, false, true, false);
        aggregator.mediate(message, false);
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundUsingAddressingNoMAPsInContextFault()
        throws Exception {
        Message message = setUpMessage(true, true, false, true, false);
        aggregator.mediate(message, true);
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundNotUsingAddressing() throws Exception {
        Message message = setUpMessage(true, true, false, false);
        aggregator.mediate(message, false);
        verifyMessage(message, true, true, false);
    }

    @Test
    public void testRequestorOutboundNotUsingAddressingFault()
        throws Exception {
        Message message = setUpMessage(true, true, false, false);
        aggregator.mediate(message, true);
        verifyMessage(message, true, true, false);
    }

    @Test
    public void testRequestorOutboundOnewayUsingAddressingMAPsInContext()
        throws Exception {
        Message message = setUpMessage(true, true, true, true, true);
        aggregator.mediate(message, false);
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundOnewayUsingAddressingMAPsInContextFault()
        throws Exception {
        Message message = setUpMessage(true, true, true, true, true);
        aggregator.mediate(message, true);
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundOnewayUsingAddressingNoMAPsInContext()
        throws Exception {
        Message message = setUpMessage(true, true, true, true, false);
        aggregator.mediate(message, false);
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundOnewayUsingAddressingNoMAPsInContextFault()
        throws Exception {
        Message message = setUpMessage(true, true, true, true, false);
        aggregator.mediate(message, true);
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundOnewayNotUsingAddressing() throws Exception {
        Message message = setUpMessage(true, true, true, false);
        aggregator.mediate(message, false);
        verifyMessage(message, true, true, false);
    }


    @Test
    public void testRequestorOutboundOnewayNotUsingAddressingFault()
        throws Exception {
        Message message = setUpMessage(true, true, true, false);
        aggregator.mediate(message, true);
        verifyMessage(message, true, true, false);
    }

    @Test
    public void testResponderInboundValidMAPs() throws Exception {
        Message message = setUpMessage(false, false, false, false, false, false, true);
        aggregator.mediate(message, false);
        verifyMessage(message, false, false, false);
    }

    @Test
    public void testResponderInboundDecoupled() throws Exception {
        Message message =
            setUpMessage(false, false, false, true, false, true, true);
        aggregator.mediate(message, false);
        verifyMessage(message, false, false, true);
    }

    @Test
    public void testResponderInboundOneway() throws Exception {
        Message message =
            setUpMessage(false, false, true, true, false, true, true);
        aggregator.mediate(message, false);
        verifyMessage(message, false, false, true);
    }

    @Test
    public void testResponderInboundValidMAPsFault() throws Exception {
        Message message = setUpMessage(false, false, false, false, false, false, true);
        aggregator.mediate(message, true);
        verifyMessage(message, false, false, true);
    }

    @Test(expected = SoapFault.class)
    public void testResponderInboundInvalidMAPs() throws Exception {
        aggregator.getMessageIdCache().checkUniquenessAndCacheId("urn:uuid:12345");
        Message message = setUpMessage(false, false, false, false, false, false, true);
        aggregator.setAllowDuplicates(false);
        aggregator.mediate(message, false);
        verifyMessage(message, false, false, false /*check*/);
    }

    @Test(expected = SoapFault.class)
    public void testResponderInboundInvalidMAPsFault() throws Exception {
        aggregator.getMessageIdCache().checkUniquenessAndCacheId("urn:uuid:12345");
        Message message = setUpMessage(false, false, false, false, false, false, true);
        aggregator.setAllowDuplicates(false);
        aggregator.mediate(message, true);
        verifyMessage(message, false, false, false /*check*/);
    }

    @Test(expected = SoapFault.class)
    public void testResponderInboundInvalidMAPsNoMessageIdReqResp() throws Exception {
        SetupMessageArgs args = new SetupMessageArgs();
        args.requestor = false;
        args.outbound = false;
        args.oneway = false;
        args.usingAddressing = false;
        args.mapsInContext = false;
        args.decoupled = false;
        args.zeroLengthAction = true;
        args.fault = false;
        args.noMessageId = true;

        Message message = setUpMessage(args);
        aggregator.setAllowDuplicates(false);
        aggregator.mediate(message, true);
        verifyMessage(message, false, false, false /*check*/);
    }

    @Test()
    public void testResponderInboundNoMessageIdOneWay() throws Exception {
        SetupMessageArgs args = new SetupMessageArgs();
        args.requestor = false;
        args.outbound = false;
        args.oneway = true;
        args.usingAddressing = false;
        args.mapsInContext = false;
        args.decoupled = false;
        args.zeroLengthAction = true;
        args.fault = false;
        args.noMessageId = true;

        Message message = setUpMessage(args);
        aggregator.setAllowDuplicates(false);
        aggregator.mediate(message, true);
        verifyMessage(message, false, false, false /*check*/);
    }

    @Test
    public void testResponderOutbound() throws Exception {
        Message message = setUpMessage(false, true, false);
        aggregator.mediate(message, false);
        verifyMessage(message, false, true, true);
    }

    @Test
    public void testResponderOutboundZeroLengthAction() throws Exception {
        Message message =
            setUpMessage(false, true, false, false, false, false, false);
        aggregator.mediate(message, false);
        verifyMessage(message, false, true, true);
    }

    @Test
    public void testResponderOutboundNoMessageId() throws Exception {
        SetupMessageArgs args = new SetupMessageArgs();
        args.requestor = false;
        args.outbound = true;
        args.oneway = false;
        args.usingAddressing = false;
        args.mapsInContext = false;
        args.decoupled = false;
        args.zeroLengthAction = false;
        args.fault = false;
        args.noMessageId = true;

        Message message =
            setUpMessage(args);
        aggregator.mediate(message, false);
        verifyMessage(message, false, true, true);
    }

    @Test
    public void testResponderOutboundFault() throws Exception {
        SetupMessageArgs args = new SetupMessageArgs();
        args.requestor = false;
        args.outbound = true;
        args.oneway = false;
        args.usingAddressing = false;
        args.mapsInContext = false;
        args.decoupled = true;
        args.zeroLengthAction = false;
        args.fault = true;
        args.noMessageId = false;

        Message message = setUpMessage(args);

        aggregator.mediate(message, true);
        verifyMessage(message, false, true, true);
    }

    @Test
    public void testRequestorInbound() throws Exception {
        Message message = setUpMessage(true, false, false);
        aggregator.mediate(message, false);
        verifyMessage(message, true, false, false /*check*/);
    }

    @Test
    public void testRequestorInboundFault() throws Exception {
        Message message = setUpMessage(true, false, false);
        aggregator.mediate(message, true);
        verifyMessage(message, true, false, false /*check*/);
    }

    @Test(expected = SoapFault.class)
    public void testTwoWayRequestWithReplyToNone() throws Exception {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        exchange.setOutMessage(message);
        message.setExchange(exchange);
        setUpMessageProperty(message,
                             REQUESTOR_ROLE,
                             Boolean.FALSE);
        AddressingProperties maps = new AddressingProperties();
        EndpointReferenceType replyTo = new EndpointReferenceType();
        replyTo.setAddress(ContextUtils.getAttributedURI(Names.WSA_NONE_ADDRESS));
        maps.setReplyTo(replyTo);
        AttributedURIType id =
            ContextUtils.getAttributedURI("urn:uuid:12345");
        maps.setMessageID(id);
        maps.setAction(ContextUtils.getAttributedURI(""));
        setUpMessageProperty(message,
                             ADDRESSING_PROPERTIES_OUTBOUND,
                             maps);
        setUpMessageProperty(message,
                             "org.apache.cxf.ws.addressing.map.fault.name",
                             "NoneAddress");

        aggregator.mediate(message, false);
    }

    @Test
    public void testReplyToWithAnonymousAddressRetained() throws Exception {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        exchange.setOutMessage(message);
        setUpMessageProperty(message,
                             REQUESTOR_ROLE,
                             Boolean.TRUE);
        AddressingProperties maps = new AddressingProperties();
        EndpointReferenceType replyTo = new EndpointReferenceType();
        replyTo.setAddress(ContextUtils.getAttributedURI(Names.WSA_ANONYMOUS_ADDRESS));
        maps.setReplyTo(replyTo);
        AttributedURIType id =
            ContextUtils.getAttributedURI("urn:uuid:12345");
        maps.setMessageID(id);
        maps.setAction(ContextUtils.getAttributedURI(""));
        setUpMessageProperty(message,
                             CLIENT_ADDRESSING_PROPERTIES,
                             maps);
        aggregator.mediate(message, false);
        AddressingProperties props =
            (AddressingProperties)message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND);
        assertSame(replyTo, props.getReplyTo());
    }

    @Test
    public void testGetActionUriForNormalOp() throws Exception {
        Message message = setUpMessage(true, true, false, true, true);
        String action = aggregator.getActionUri(message, false);
        assertEquals("http://foo/bar/SEI/opRequest", action);
    }

    @Test
    public void testGetReplyToUsingBaseAddress() throws Exception {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);

        final String localReplyTo = "/SoapContext/decoupled";
        final String decoupledEndpointBase = "http://localhost:8181/cxf";
        final String replyTo = decoupledEndpointBase + localReplyTo;

        ServiceInfo s = new ServiceInfo();
        Service svc = new ServiceImpl(s);
        EndpointInfo ei = new EndpointInfo();
        InterfaceInfo ii = s.createInterface(new QName("FooInterface"));
        s.setInterface(ii);
        ii.addOperation(new QName("fooOp"));
        SoapBindingInfo b = new SoapBindingInfo(s, "http://schemas.xmlsoap.org/soap/", Soap11.getInstance());
        b.setTransportURI("http://schemas.xmlsoap.org/soap/http");
        ei.setBinding(b);

        ei.setAddress("http://nowhere.com/bar/foo");
        ei.setName(new QName("http://nowhere.com/port", "foo"));
        Bus bus = new ExtensionManagerBus();
        DestinationFactoryManager dfm = mock(DestinationFactoryManager.class);
        DestinationFactory df = mock(DestinationFactory.class);
        Destination d = mock(Destination.class);
        bus.setExtension(dfm, DestinationFactoryManager.class);
        when(dfm.getDestinationFactoryForUri(localReplyTo)).thenReturn(df);
        when(df.getDestination(any(EndpointInfo.class), any(Bus.class))).thenReturn(d);
        when(d.getAddress()).thenReturn(EndpointReferenceUtils.getEndpointReference(localReplyTo));

        Endpoint ep = new EndpointImpl(bus, svc, ei);
        exchange.put(Endpoint.class, ep);
        exchange.put(Bus.class, bus);
        exchange.setOutMessage(message);
        setUpMessageProperty(message,
                             REQUESTOR_ROLE,
                             Boolean.TRUE);
        message.getContextualProperty(WSAContextUtils.REPLYTO_PROPERTY);
        message.put(WSAContextUtils.REPLYTO_PROPERTY, localReplyTo);
        message.put(WSAContextUtils.DECOUPLED_ENDPOINT_BASE_PROPERTY, decoupledEndpointBase);

        AddressingProperties maps = new AddressingProperties();
        AttributedURIType id =
            ContextUtils.getAttributedURI("urn:uuid:12345");
        maps.setMessageID(id);
        maps.setAction(ContextUtils.getAttributedURI(""));
        setUpMessageProperty(message,
                             CLIENT_ADDRESSING_PROPERTIES,
                             maps);
        aggregator.mediate(message, false);
        AddressingProperties props =
            (AddressingProperties)message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND);

        assertEquals(replyTo, props.getReplyTo().getAddress().getValue());
    }

    private Message setUpMessage(boolean requestor,
                                 boolean outbound,
                                 boolean oneway)
        throws Exception {
        return setUpMessage(requestor, outbound, oneway, false, false, false);
    }


    private Message setUpMessage(boolean requestor,
                                 boolean outbound,
                                 boolean oneway,
                                 boolean usingAddressing)
        throws Exception {
        return setUpMessage(requestor,
                            outbound,
                            oneway,
                            usingAddressing,
                            false,
                            false);
    }


    private Message setUpMessage(boolean requestor,
                                 boolean outbound,
                                 boolean oneway,
                                 boolean usingAddressing,
                                 boolean mapsInContext)
        throws Exception {
        return setUpMessage(requestor,
                            outbound,
                            oneway,
                            usingAddressing,
                            mapsInContext,
                            false);
    }


    private Message setUpMessage(boolean requestor,
                                 boolean outbound,
                                 boolean oneway,
                                 boolean usingAddressing,
                                 boolean mapsInContext,
                                 boolean decoupled)
        throws Exception {
        return setUpMessage(requestor,
                            outbound,
                            oneway,
                            usingAddressing,
                            mapsInContext,
                            decoupled,
                            false);
    }



    private Message setUpMessage(boolean requestor,
                                 boolean outbound,
                                 boolean oneway,
                                 boolean usingAddressing,
                                 boolean mapsInContext,
                                 boolean decoupled,
                                 boolean zeroLengthAction)
        throws Exception {

        SetupMessageArgs args = new SetupMessageArgs();
        args.requestor = requestor;
        args.outbound = outbound;
        args.oneway = oneway;
        args.usingAddressing = usingAddressing;
        args.mapsInContext = mapsInContext;
        args.decoupled = decoupled;
        args.zeroLengthAction = zeroLengthAction;
        args.fault = false;
        args.noMessageId = false;

        return setUpMessage(args);
    }

    private Message setUpMessage(SetupMessageArgs args)
        throws Exception {

        Message message = getMessage();
        Exchange exchange = getExchange();
        setUpOutbound(message, exchange, args.outbound, args.fault);
        setUpMessageProperty(message,
                             REQUESTOR_ROLE,
                             Boolean.valueOf(args.requestor));
        if (args.outbound && args.requestor) {
            if (args.usingAddressing) {
                setUpConduit(message, exchange);
            }
            setUpUsingAddressing(message, exchange, args.usingAddressing);
            if (args.usingAddressing) {
                setUpRequestor(message,
                               exchange,
                               args.oneway,
                               args.mapsInContext,
                               args.decoupled,
                               args.zeroLengthAction);
            }
        } else if (!args.requestor) {
            SetupResponderArgs srArgs = new SetupResponderArgs();
            srArgs.oneway = args.oneway;
            srArgs.outbound = args.outbound;
            srArgs.decoupled = args.decoupled;
            srArgs.zeroLengthAction = args.zeroLengthAction;
            srArgs.fault = args.fault;
            srArgs.noMessageId = args.noMessageId;

            Endpoint endpoint = mock(Endpoint.class);
            when(exchange.getEndpoint()).thenReturn(endpoint);

            setUpResponder(message,
                           exchange,
                           srArgs, 
                           endpoint);

            when(endpoint.getOutInterceptors()).thenReturn(new ArrayList<Interceptor<? extends Message>>());
            Service serv = mock(Service.class);
            when(endpoint.getService()).thenReturn(serv);
            when(serv.getOutInterceptors()).thenReturn(new ArrayList<Interceptor<? extends Message>>());
        }
        return message;
    }

    private void setUpUsingAddressing(Message message,
                                      Exchange exchange,
                                      boolean usingAddressing) {
        setUpMessageExchange(message, exchange);
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getOutInterceptors()).thenReturn(new ArrayList<Interceptor<? extends Message>>());

        setUpExchangeGet(exchange, Endpoint.class, endpoint);
        EndpointInfo endpointInfo = mock(EndpointInfo.class);
        when(endpoint.getEndpointInfo()).thenReturn(endpointInfo);
        List<ExtensibilityElement> endpointExts =
            new ArrayList<>();
        when(endpointInfo.getExtensors(eq(ExtensibilityElement.class))).thenReturn(endpointExts);
        BindingInfo bindingInfo = mock(BindingInfo.class);
        when(endpointInfo.getBinding()).thenReturn(bindingInfo);
        when(bindingInfo.getExtensors(eq(ExtensibilityElement.class))).thenReturn(Collections.emptyList());
        ServiceInfo serviceInfo = mock(ServiceInfo.class);
        when(endpointInfo.getService()).thenReturn(serviceInfo);
        when(serviceInfo.getExtensors(eq(ExtensibilityElement.class))).thenReturn(Collections.emptyList());
        ExtensibilityElement ext =
            mock(ExtensibilityElement.class);
        if (usingAddressing) {
            QName elementType = usingAddressing
                ? Names.WSAW_USING_ADDRESSING_QNAME
                : new QName(SOAP_NAMESPACE, "encodingStyle");
            when(ext.getElementType()).thenReturn(elementType);
            endpointExts.add(ext);
        }
    }

    private void setUpRequestor(Message message,
                                Exchange exchange,
                                boolean oneway,
                                boolean mapsInContext,
                                boolean decoupled,
                                boolean zeroLengthAction) throws Exception {
        setUpMessageProperty(message,
                             REQUESTOR_ROLE,
                             Boolean.valueOf(Boolean.TRUE));
        AddressingProperties maps = mapsInContext
                                        ? new AddressingProperties()
                                        : null;
        if (zeroLengthAction && maps != null) {
            maps.setAction(ContextUtils.getAttributedURI(""));
        }
        setUpMessageProperty(message,
                                CLIENT_ADDRESSING_PROPERTIES,
                                maps);
        Method method = SEI.class.getMethod("op", new Class[0]);
        if (!zeroLengthAction) {
            setUpMethod(message, exchange, method);
            setUpMessageProperty(message,
                                    REQUESTOR_ROLE,
                                    Boolean.TRUE);
            expectedAction = "http://foo/bar/SEI/opRequest";
        }
        setUpMessageProperty(message,
                                REQUESTOR_ROLE,
                                Boolean.TRUE);
        setUpOneway(message, exchange, oneway);
        expectedMAPs = maps;
        expectedTo = Names.WSA_NONE_ADDRESS;
        expectedReplyTo = oneway
                          ? Names.WSA_NONE_ADDRESS
                          : Names.WSA_ANONYMOUS_ADDRESS;
        // Now verified via verifyMessage()
        //EasyMock.eq(CLIENT_ADDRESSING_PROPERTIES_OUTBOUND);
        //EasyMock.reportMatcher(new MAPMatcher());
        //message.put(CLIENT_ADDRESSING_PROPERTIES_OUTBOUND,
        //            mapsInContext
        //            ? maps
        //            : new AddressingPropertiesImpl());
        //whenLastCall().thenReturn(null);
    }

    private void setUpResponder(Message message,
                                Exchange exchange,
                                SetupResponderArgs args,
                                Endpoint endpoint) throws Exception {

        setUpMessageProperty(message,
                             REQUESTOR_ROLE,
                             Boolean.FALSE);
        AddressingProperties maps = new AddressingProperties();
        EndpointReferenceType replyTo = new EndpointReferenceType();
        replyTo.setAddress(
            ContextUtils.getAttributedURI(args.decoupled
                                          ? "http://localhost:9999/decoupled"
                                          : Names.WSA_ANONYMOUS_ADDRESS));
        maps.setReplyTo(replyTo);
        EndpointReferenceType faultTo = new EndpointReferenceType();
        faultTo.setAddress(
            ContextUtils.getAttributedURI(args.decoupled
                                          ? "http://localhost:9999/fault"
                                          : Names.WSA_ANONYMOUS_ADDRESS));
        maps.setFaultTo(faultTo);

        if (!args.noMessageId) {
            AttributedURIType id =
                ContextUtils.getAttributedURI("urn:uuid:12345");
            maps.setMessageID(id);
        }

        if (args.zeroLengthAction) {
            maps.setAction(ContextUtils.getAttributedURI(""));
        }
        setUpMessageProperty(message,
                             ADDRESSING_PROPERTIES_INBOUND,
                             maps);
        if (!args.outbound) {
            setUpOneway(message, exchange, args.oneway);
            if (args.oneway || args.decoupled) {
                setUpRebase(message, exchange, endpoint);
            }
        }

        if (args.outbound || ((DefaultMessageIdCache) aggregator.getMessageIdCache())
            .getMessageIdSet().size() > 0) {
            if (!args.zeroLengthAction) {
                Method method = SEI.class.getMethod("op", new Class[0]);
                setUpMethod(message, exchange, method);
                setUpMessageProperty(message,
                                     REQUESTOR_ROLE,
                                     Boolean.FALSE);

                if (args.fault) {
                    message.setContent(Exception.class, new SoapFault("blah",
                            new Exception(), Fault.FAULT_CODE_SERVER));
                    expectedAction = "http://foo/bar/SEI/op/Fault/Exception";
                } else {
                    expectedAction = "http://foo/bar/SEI/opResponse";
                }
            }
            setUpMessageProperty(message,
                                 REQUESTOR_ROLE,
                                 Boolean.FALSE);
            setUpMessageProperty(message,
                                 ADDRESSING_PROPERTIES_INBOUND,
                                 maps);
            if (args.fault) {
                // REVISIT test double rebase does not occur
                setUpRebase(message, exchange, endpoint);
            }
            expectedTo = args.decoupled
                         ? args.fault
                           ? "http://localhost:9999/fault"
                           : "http://localhost:9999/decoupled"
                         : Names.WSA_ANONYMOUS_ADDRESS;

            if (maps.getMessageID() != null && maps.getMessageID().getValue() != null) {
                expectedRelatesTo = maps.getMessageID().getValue();
            } else {
                expectedRelatesTo = Names.WSA_UNSPECIFIED_RELATIONSHIP;
            }
            // Now verified via verifyMessage()
            //EasyMock.eq(SERVER_ADDRESSING_PROPERTIES_OUTBOUND);
            //EasyMock.reportMatcher(new MAPMatcher());
            //message.put(SERVER_ADDRESSING_PROPERTIES_OUTBOUND,
            //            new AddressingPropertiesImpl());
            //whenLastCall().thenReturn(null);
        }
    }

    private void setUpRebase(Message message, Exchange exchange, Endpoint endpoint)
        throws Exception {
        setUpMessageProperty(message,
                             "org.apache.cxf.ws.addressing.partial.response.sent",
                             Boolean.FALSE);
        Binding binding = mock(Binding.class);
        when(endpoint.getBinding()).thenReturn(binding);
        Message partialResponse = getMessage();
        when(binding.createMessage(isA(Message.class))).thenReturn(partialResponse);

        Destination target = mock(Destination.class);
        setUpMessageDestination(message, target);
        Conduit backChannel = mock(Conduit.class);
        when(target.getBackChannel(eq(message))).thenReturn(backChannel);
        // REVISIT test interceptor chain setup & send
    }

    private void setUpOneway(Message message, Exchange exchange, boolean oneway) {
        setUpMessageExchange(message, exchange);
        setUpExchangeOneway(exchange, oneway);
    }

    private void setUpOutbound(Message message, Exchange exchange, boolean outbound, boolean fault) {
        setUpMessageExchange(message, exchange);
        setUpExchangeOutbound(exchange, message, outbound, fault);
    }

    private void setUpConduit(Message message, Exchange exchange) {
        setUpMessageExchange(message, exchange);
        Conduit conduit = mock(Conduit.class);
        setUpExchangeConduit(message, exchange, conduit);
        EndpointReferenceType to =
            ContextUtils.WSA_OBJECT_FACTORY.createEndpointReferenceType();
        to.setAddress(ContextUtils.getAttributedURI(expectedTo));
        when(conduit.getTarget()).thenReturn(to);
    }

    private void setUpMethod(Message message, Exchange exchange, Method method) {
        setUpMessageExchange(message, exchange);
        BindingOperationInfo bindingOpInfo = setUpBindingOperationInfo("http://foo/bar",
                                                                       "opRequest",
                                                                       "opResponse",
                                                                       "opFault", method);
        setUpExchangeGet(exchange, BindingOperationInfo.class, bindingOpInfo);
        when(exchange.getBindingOperationInfo()).thenReturn(bindingOpInfo);
        // Usual fun with EasyMock not always working as expected
        //BindingOperationInfo bindingOpInfo =
        //    EasyMock.createMock(BindingOperationInfo.class);
        //OperationInfo opInfo = EasyMock.createMock(OperationInfo.class);
        //bindingOpInfo.getOperationInfo();
        //whenLastCall().thenReturn(opInfo);
        //opInfo.getProperty(EasyMock.eq(Method.class.getName()));
        //whenLastCall().thenReturn(method);
    }

    private BindingOperationInfo setUpBindingOperationInfo(String nsuri,
                                                           String opreq,
                                                           String opresp,
                                                           String opfault, Method method) {
        ServiceInfo si = new ServiceInfo();
        InterfaceInfo iinf = new InterfaceInfo(si,
                                               new QName(nsuri, method.getDeclaringClass().getSimpleName()));
        OperationInfo opInfo = iinf.addOperation(new QName(nsuri, method.getName()));
        opInfo.setProperty(Method.class.getName(), method);
        opInfo.setInput(opreq, opInfo.createMessage(new QName(nsuri, opreq), Type.INPUT));
        opInfo.setOutput(opresp, opInfo.createMessage(new QName(nsuri, opresp), Type.INPUT));
        FaultInfo finfo = opInfo.addFault(new QName(nsuri, opfault), new QName(nsuri, opfault));
        finfo.addMessagePart("fault");

        return new TestBindingOperationInfo(opInfo);
    }

    private Message getMessage() {
        //return mock(Message.class);
        return new MessageImpl();
    }

    private Exchange getExchange() {
        Bus bus = mock(Bus.class);
        when(bus.getExtension(PhaseManager.class)).thenReturn(new PhaseManagerImpl());

        Exchange exchange = mock(Exchange.class);
        when(exchange.getBus()).thenReturn(bus);
        when(exchange.isEmpty()).thenReturn(true);
        return exchange;
    }

    private void setUpMessageProperty(Message message, String key, Object value) {
        //message.get(key);
        //whenLastCall().thenReturn(value);
        message.put(key, value);
    }

    private void setUpMessageExchange(Message message, Exchange exchange) {
        //message.getExchange();
        //whenLastCall().thenReturn(exchange);
        message.setExchange(exchange);
    }

    private void setUpMessageDestination(Message message, Destination target) {
        //message.getDestination();
        //whenLastCall().thenReturn(target);
        ((MessageImpl)message).setDestination(target);
    }

    private <T> void setUpExchangeGet(Exchange exchange, Class<T> clz, T value) {
        when(exchange.get(clz)).thenReturn(value);
        //exchange.put(Endpoint.class, value);
    }

    private void setUpExchangeOneway(Exchange exchange, boolean oneway) {
        when(exchange.isOneWay()).thenReturn(oneway);
        //exchange.setOneWay(oneway);
    }

    private void setUpExchangeOutbound(Exchange exchange,
                                       Message message,
                                       boolean outbound,
                                       boolean fault) {
        if (fault) {
            when(exchange.getOutFaultMessage()).thenReturn(outbound ? message : null);
        } else {
            when(exchange.getOutMessage()).thenReturn(outbound ? message : null);
        }
        //exchange.setOutMessage(outbound ? message : new MessageImpl());
    }

    private void setUpExchangeConduit(Message message,
                                      Exchange exchange,
                                      Conduit conduit) {
        //exchange.getConduit(message);
        //whenLastCall().thenReturn(conduit);
        //exchange.setConduit(conduit);
    }

    private boolean verifyMAPs(Object obj) {
        if (obj instanceof AddressingProperties) {
            AddressingProperties other = (AddressingProperties)obj;
            return compareExpected(other);
        }
        return false;
    }

    private boolean compareExpected(AddressingProperties other) {
        boolean ret = false;
        if (expectedMAPs == null || expectedMAPs == other) {
            boolean toOK =
                expectedTo == null
                || expectedTo.equals(other.getTo().getValue());
            boolean replyToOK =
                expectedReplyTo == null
                || expectedReplyTo.equals(
                       other.getReplyTo().getAddress().getValue());
            boolean relatesToOK =
                expectedRelatesTo == null
                || expectedRelatesTo.equals(
                       other.getRelatesTo().getValue());
            boolean actionOK =
                expectedAction == null
                || expectedAction.equals(other.getAction().getValue());
            boolean messageIdOK = other.getMessageID() != null;
            ret = toOK
                  && replyToOK
                  && relatesToOK
                  && actionOK
                  && messageIdOK;
        }
        return ret;
    }

    private String getMAPProperty(boolean requestor, boolean outbound) {
        return outbound
                 ? ADDRESSING_PROPERTIES_OUTBOUND
                 : ADDRESSING_PROPERTIES_INBOUND;
    }

    private void verifyMessage(Message message,
                               boolean requestor,
                               boolean outbound,
                               boolean expectMapsInContext) {
        if (expectMapsInContext) {
            assertTrue("unexpected MAPs",
                       verifyMAPs(message.get(getMAPProperty(requestor, outbound))));
        }
    }

    private interface SEI {
        @RequestWrapper(targetNamespace = "http://foo/bar", className = "SEI", localName = "opRequest")
        @ResponseWrapper(targetNamespace = "http://foo/bar", className = "SEI", localName = "opResponse")
        String op();
    }

    private static final class TestBindingMessageInfo extends BindingMessageInfo {
    }

    private static final class TestBindingOperationInfo extends BindingOperationInfo {
        private Map<QName, BindingFaultInfo> faults;

        TestBindingOperationInfo(OperationInfo oi) {
            opInfo = oi;

            Collection<FaultInfo> of = opInfo.getFaults();
            if (of != null && !of.isEmpty()) {
                faults = new ConcurrentHashMap<>(of.size());
                for (FaultInfo fault : of) {
                    faults.put(fault.getFaultName(), new BindingFaultInfo(fault, this));
                }
            }
        }

        public BindingMessageInfo getInput() {
            return new TestBindingMessageInfo();
        }

        public BindingMessageInfo getOutput() {
            return new TestBindingMessageInfo();
        }

        @Override
        public Collection<BindingFaultInfo> getFaults() {
            return Collections.unmodifiableCollection(this.faults.values());
        }
    }

    private static final class SetupMessageArgs {
        boolean requestor;
        boolean outbound;
        boolean oneway;
        boolean usingAddressing;
        boolean mapsInContext;
        boolean decoupled;
        boolean zeroLengthAction;
        boolean fault;
        boolean noMessageId;
    }

    private static final class SetupResponderArgs {
        boolean outbound;
        boolean oneway;
        boolean decoupled;
        boolean zeroLengthAction;
        boolean fault;
        boolean noMessageId;
    }
}