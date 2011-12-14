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

package org.apache.cxf.ws.addressing;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.phase.PhaseManagerImpl;
import org.apache.cxf.service.Service;
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
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.apache.cxf.binding.soap.Soap11.SOAP_NAMESPACE;
import static org.apache.cxf.message.Message.REQUESTOR_ROLE;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_INBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_OUTBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_OUTBOUND;

public class MAPAggregatorTest extends Assert {

    private MAPAggregator aggregator;
    private IMocksControl control;
    private AddressingPropertiesImpl expectedMAPs;
    private String expectedTo;
    private String expectedReplyTo;
    private String expectedRelatesTo;
    private String expectedAction;
    
    @Before
    public void setUp() {
        aggregator = new MAPAggregator();
        control = EasyMock.createNiceControl();
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
        control.verify();
        verifyMessage(message, true, true, true);
    }
    
    @Test
    public void testRequestorOutboundUsingAddressingMAPsInContextZeroLengthAction() 
        throws Exception {
        Message message = setUpMessage(true, true, false, true, true, true);
        aggregator.mediate(message, false);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundUsingAddressingMAPsInContextFault() 
        throws Exception {
        Message message = setUpMessage(true, true, false, true, true);
        aggregator.mediate(message, true);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundUsingAddressingNoMAPsInContext() 
        throws Exception {
        Message message = setUpMessage(true, true, false, true, false);
        aggregator.mediate(message, false);
        control.verify();
        verifyMessage(message, true, true, true);
    }
    
    @Test
    public void testRequestorOutboundUsingAddressingNoMAPsInContextFault() 
        throws Exception {
        Message message = setUpMessage(true, true, false, true, false);
        aggregator.mediate(message, true);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundNotUsingAddressing() throws Exception {
        Message message = setUpMessage(true, true, false, false);
        aggregator.mediate(message, false);
        control.verify();
        verifyMessage(message, true, true, false);
    }

    @Test
    public void testRequestorOutboundNotUsingAddressingFault() 
        throws Exception {
        Message message = setUpMessage(true, true, false, false);
        aggregator.mediate(message, true);
        control.verify();
        verifyMessage(message, true, true, false);
    }

    @Test
    public void testRequestorOutboundOnewayUsingAddressingMAPsInContext() 
        throws Exception {
        Message message = setUpMessage(true, true, true, true, true);
        aggregator.mediate(message, false);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundOnewayUsingAddressingMAPsInContextFault() 
        throws Exception {
        Message message = setUpMessage(true, true, true, true, true);
        aggregator.mediate(message, true);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundOnewayUsingAddressingNoMAPsInContext() 
        throws Exception {
        Message message = setUpMessage(true, true, true, true, false);
        aggregator.mediate(message, false);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundOnewayUsingAddressingNoMAPsInContextFault() 
        throws Exception {
        Message message = setUpMessage(true, true, true, true, false);
        aggregator.mediate(message, true);
        control.verify();
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundOnewayNotUsingAddressing() throws Exception {
        Message message = setUpMessage(true, true, true, false);
        aggregator.mediate(message, false);
        control.verify();
        verifyMessage(message, true, true, false);
    }
    

    @Test
    public void testRequestorOutboundOnewayNotUsingAddressingFault() 
        throws Exception {
        Message message = setUpMessage(true, true, true, false);
        aggregator.mediate(message, true);
        control.verify();
        verifyMessage(message, true, true, false);
    }

    @Test
    public void testResponderInboundValidMAPs() throws Exception {
        Message message = setUpMessage(false, false, false, false, false, false, true);
        aggregator.mediate(message, false);
        control.verify();
        verifyMessage(message, false, false, false);
    }
    
    @Test
    public void testResponderInboundDecoupled() throws Exception {
        Message message = 
            setUpMessage(false, false, false, true, false, true, true);
        aggregator.mediate(message, false);
        control.verify();
        verifyMessage(message, false, false, true);
    }
    
    @Test
    public void testResponderInboundOneway() throws Exception {
        Message message = 
            setUpMessage(false, false, true, true, false, true, true);
        aggregator.mediate(message, false);
        control.verify();
        verifyMessage(message, false, false, true);
    }

    @Test
    public void testResponderInboundValidMAPsFault() throws Exception {
        Message message = setUpMessage(false, false, false, false, false, false, true);
        aggregator.mediate(message, true);
        control.verify();
        verifyMessage(message, false, false, true);
    }

    @Test(expected = SoapFault.class)
    public void testResponderInboundInvalidMAPs() throws Exception {
        aggregator.getMessageIdCache().checkUniquenessAndCacheId("urn:uuid:12345");
        Message message = setUpMessage(false, false, false, false, false, false, true);
        aggregator.setAllowDuplicates(false);
        aggregator.mediate(message, false);
        control.verify();
        verifyMessage(message, false, false, false /*check*/);
    }

    @Test(expected = SoapFault.class)
    public void testResponderInboundInvalidMAPsFault() throws Exception {
        aggregator.getMessageIdCache().checkUniquenessAndCacheId("urn:uuid:12345");
        Message message = setUpMessage(false, false, false, false, false, false, true);
        aggregator.setAllowDuplicates(false);
        aggregator.mediate(message, true);
        control.verify();
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
        control.verify();
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
        control.verify();
        verifyMessage(message, false, false, false /*check*/);
    }

    @Test
    public void testResponderOutbound() throws Exception {
        Message message = setUpMessage(false, true, false);
        aggregator.mediate(message, false);
        control.verify();
        verifyMessage(message, false, true, true);
    }
    
    @Test
    public void testResponderOutboundZeroLengthAction() throws Exception {
        Message message = 
            setUpMessage(false, true, false, false, false, false, false);
        aggregator.mediate(message, false);
        control.verify();
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
        control.verify();
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
        control.verify();
        verifyMessage(message, false, true, true);
    }

    @Test
    public void testRequestorInbound() throws Exception {
        Message message = setUpMessage(true, false, false);
        aggregator.mediate(message, false);
        control.verify();
        verifyMessage(message, true, false, false /*check*/);
    }

    @Test
    public void testRequestorInboundFault() throws Exception {
        Message message = setUpMessage(true, false, false);
        aggregator.mediate(message, true);
        control.verify();
        verifyMessage(message, true, false, false /*check*/);
    }
    
    @Test
    public void testTwoWayRequestWithReplyToNone() throws Exception {
        Message message = new MessageImpl();  
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        setUpMessageProperty(message,
                             REQUESTOR_ROLE,
                             Boolean.FALSE);
        AddressingPropertiesImpl maps = new AddressingPropertiesImpl();
        EndpointReferenceType replyTo = new EndpointReferenceType();
        replyTo.setAddress(ContextUtils.getAttributedURI(Names.WSA_NONE_ADDRESS));
        maps.setReplyTo(replyTo);
        AttributedURIType id = 
            ContextUtils.getAttributedURI("urn:uuid:12345");
        maps.setMessageID(id);
        maps.setAction(ContextUtils.getAttributedURI(""));
        setUpMessageProperty(message,
                             SERVER_ADDRESSING_PROPERTIES_INBOUND,
                             maps);
        setUpMessageProperty(message,
                             "org.apache.cxf.ws.addressing.map.fault.name",
                             "NoneAddress");
        
        try {
            aggregator.mediate(message, false);
            fail("Two way request with ReplyTo address set to none must fail");
        } catch (SoapFault ex) {
            // expected
        }
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
        AddressingPropertiesImpl maps = new AddressingPropertiesImpl();
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
            (AddressingProperties)message.get(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_OUTBOUND);
        assertSame(replyTo, props.getReplyTo());
    }

    @Test
    public void testGetActionUriForNormalOp() throws Exception {
        Message message = setUpMessage(true, true, false, true, true);
        String action = aggregator.getActionUri(message, false);
        control.verify();
        assertEquals("http://foo/bar/SEI/opRequest", action);
    }
    
    @Test
    public void testGetActionUriForDispatchOp() throws Exception {
        Message message = setUpMessage(true, true, false, true, true);
        BindingOperationInfo dbop = setUpBindingOperationInfo("http://foo/bar/d",
                                                              "opDRequest",
                                                              "opDResponse",
                                                              "opDFault", 
                                                              DSEI.class.getMethod("op", new Class[0]));

        BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
        bop.setProperty("dispatchToOperation", dbop);

        String action = aggregator.getActionUri(message, false);
        control.verify();
        assertEquals("http://foo/bar/d/DSEI/opDRequest", action);
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
            
            setUpResponder(message,
                           exchange,
                           srArgs);
            
            Endpoint endpoint = control.createMock(Endpoint.class);
            endpoint.getOutInterceptors();
            EasyMock.expectLastCall().andReturn(new ArrayList()).anyTimes();
            Service serv = control.createMock(Service.class);
            endpoint.getService();
            EasyMock.expectLastCall().andReturn(serv).anyTimes();
            serv.getOutInterceptors();
            EasyMock.expectLastCall().andReturn(new ArrayList()).anyTimes();
            exchange.get(Endpoint.class);
            EasyMock.expectLastCall().andReturn(endpoint).anyTimes();
        }
        control.replay();
        return message;
    }

    private void setUpUsingAddressing(Message message,
                                      Exchange exchange,
                                      boolean usingAddressing) {
        setUpMessageExchange(message, exchange);
        Endpoint endpoint = control.createMock(Endpoint.class);
        endpoint.getOutInterceptors();
        EasyMock.expectLastCall().andReturn(new ArrayList()).anyTimes();
        
        setUpExchangeGet(exchange, Endpoint.class, endpoint);
        EndpointInfo endpointInfo = control.createMock(EndpointInfo.class);
        endpoint.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(endpointInfo).anyTimes();
        List<ExtensibilityElement> endpointExts =
            new ArrayList<ExtensibilityElement>();
        endpointInfo.getExtensors(EasyMock.eq(ExtensibilityElement.class));
        EasyMock.expectLastCall().andReturn(endpointExts).anyTimes();
        BindingInfo bindingInfo = control.createMock(BindingInfo.class);
        endpointInfo.getBinding();
        EasyMock.expectLastCall().andReturn(bindingInfo).anyTimes();
        bindingInfo.getExtensors(EasyMock.eq(ExtensibilityElement.class));
        EasyMock.expectLastCall().andReturn(Collections.EMPTY_LIST).anyTimes();
        ServiceInfo serviceInfo = control.createMock(ServiceInfo.class);
        endpointInfo.getService();
        EasyMock.expectLastCall().andReturn(serviceInfo).anyTimes();
        serviceInfo.getExtensors(EasyMock.eq(ExtensibilityElement.class));
        EasyMock.expectLastCall().andReturn(Collections.EMPTY_LIST).anyTimes();
        ExtensibilityElement ext = 
            control.createMock(ExtensibilityElement.class);
        if (usingAddressing) {
            QName elementType = usingAddressing 
                ? Names.WSAW_USING_ADDRESSING_QNAME 
                : new QName(SOAP_NAMESPACE, "encodingStyle");
            ext.getElementType();
            EasyMock.expectLastCall().andReturn(elementType).anyTimes();
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
        AddressingPropertiesImpl maps = mapsInContext 
                                        ? new AddressingPropertiesImpl()
                                        : null;
        if (zeroLengthAction) {
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
        //EasyMock.expectLastCall().andReturn(null);
    }

    private void setUpResponder(Message message,
                                Exchange exchange,
                                SetupResponderArgs args) throws Exception {

        setUpMessageProperty(message,
                             REQUESTOR_ROLE,
                             Boolean.FALSE);
        AddressingPropertiesImpl maps = new AddressingPropertiesImpl();
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
                             SERVER_ADDRESSING_PROPERTIES_INBOUND,
                             maps);
        if (!args.outbound) {
            setUpOneway(message, exchange, args.oneway);
            if (args.oneway || args.decoupled) {
                setUpRebase(message, exchange);
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
                            (Throwable) new Exception(), Fault.FAULT_CODE_SERVER));
                    expectedAction = "http://foo/bar/SEI/op/Fault/Exception";
                } else {
                    expectedAction = "http://foo/bar/SEI/opResponse";
                }
            }
            setUpMessageProperty(message,
                                 REQUESTOR_ROLE,
                                 Boolean.FALSE);
            setUpMessageProperty(message,
                                 SERVER_ADDRESSING_PROPERTIES_INBOUND,
                                 maps);            
            if (args.fault) {
                // REVISIT test double rebase does not occur
                setUpRebase(message, exchange);
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
            //EasyMock.expectLastCall().andReturn(null);
        }
    }
    
    private void setUpRebase(Message message, Exchange exchange)
        throws Exception {
        setUpMessageProperty(message,
                             "org.apache.cxf.ws.addressing.partial.response.sent",
                             Boolean.FALSE);
        Endpoint endpoint = control.createMock(Endpoint.class);
        exchange.get(Endpoint.class);
        EasyMock.expectLastCall().andReturn(endpoint);
        Binding binding = control.createMock(Binding.class);
        endpoint.getBinding();        
        EasyMock.expectLastCall().andReturn(binding).anyTimes();
        Message partialResponse = getMessage();
        binding.createMessage(EasyMock.isA(Message.class));
        EasyMock.expectLastCall().andReturn(partialResponse);

        Destination target = control.createMock(Destination.class);
        setUpMessageDestination(message, target);
        Conduit backChannel = control.createMock(Conduit.class);
        target.getBackChannel(EasyMock.eq(message),
                              EasyMock.isA(Message.class),
                              EasyMock.isA(EndpointReferenceType.class));
        EasyMock.expectLastCall().andReturn(backChannel);
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
        Conduit conduit = EasyMock.createMock(Conduit.class);
        setUpExchangeConduit(message, exchange, conduit);
        EndpointReferenceType to =
            ContextUtils.WSA_OBJECT_FACTORY.createEndpointReferenceType();
        to.setAddress(ContextUtils.getAttributedURI(expectedTo));
        conduit.getTarget();
        EasyMock.expectLastCall().andReturn(to).anyTimes();
    }
    
    private void setUpMethod(Message message, Exchange exchange, Method method) {
        setUpMessageExchange(message, exchange);
        BindingOperationInfo bindingOpInfo = setUpBindingOperationInfo("http://foo/bar",
                                                                       "opRequest",
                                                                       "opResponse",
                                                                       "opFault", method);
        setUpExchangeGet(exchange, BindingOperationInfo.class, bindingOpInfo);
        // Usual fun with EasyMock not always working as expected
        //BindingOperationInfo bindingOpInfo =
        //    EasyMock.createMock(BindingOperationInfo.class); 
        //OperationInfo opInfo = EasyMock.createMock(OperationInfo.class); 
        //bindingOpInfo.getOperationInfo();
        //EasyMock.expectLastCall().andReturn(opInfo);
        //opInfo.getProperty(EasyMock.eq(Method.class.getName()));
        //EasyMock.expectLastCall().andReturn(method);
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
        
        BindingOperationInfo bindingOpInfo = new TestBindingOperationInfo(opInfo);
        
        return bindingOpInfo;
    }
    
    private Message getMessage() {
        //return control.createMock(Message.class);
        return new MessageImpl();
    }

    private Exchange getExchange() {
        Bus bus = control.createMock(Bus.class);
        bus.getExtension(PhaseManager.class);
        EasyMock.expectLastCall().andReturn(new PhaseManagerImpl()).anyTimes();
        
        Exchange exchange = control.createMock(Exchange.class);
        exchange.get(Bus.class);
        EasyMock.expectLastCall().andReturn(bus).anyTimes();
        EasyMock.expect(exchange.isEmpty()).andReturn(true).anyTimes();
        return exchange;
    }
    
    private void setUpMessageProperty(Message message, String key, Object value) {
        //message.get(key);
        //EasyMock.expectLastCall().andReturn(value);
        message.put(key, value);
    }

    private void setUpMessageExchange(Message message, Exchange exchange) {
        //message.getExchange();
        //EasyMock.expectLastCall().andReturn(exchange);
        message.setExchange(exchange);
    }
    
    private void setUpMessageDestination(Message message, Destination target) {
        //message.getDestination();
        //EasyMock.expectLastCall().andReturn(target);
        ((MessageImpl)message).setDestination(target);    
    }

    private <T> void setUpExchangeGet(Exchange exchange, Class<T> clz, T value) {
        exchange.get(clz);
        EasyMock.expectLastCall().andReturn(value).anyTimes();
        //exchange.put(Endpoint.class, value);
    }

    private void setUpExchangeOneway(Exchange exchange, boolean oneway) {
        exchange.isOneWay();
        EasyMock.expectLastCall().andReturn(oneway).anyTimes();
        //exchange.setOneWay(oneway);
    }

    private void setUpExchangeOutbound(Exchange exchange,
                                       Message message,
                                       boolean outbound,
                                       boolean fault) {
        if (fault) {
            exchange.getOutFaultMessage();
        } else {
            exchange.getOutMessage();
        }
        EasyMock.expectLastCall().andReturn(outbound ? message : null).anyTimes();
        //exchange.setOutMessage(outbound ? message : new MessageImpl());
    }

    private void setUpExchangeConduit(Message message,
                                      Exchange exchange,
                                      Conduit conduit) {
        //exchange.getConduit(message);
        //EasyMock.expectLastCall().andReturn(conduit);
        //exchange.setConduit(conduit);
    }

    private boolean verifyMAPs(Object obj) {
        if (obj instanceof AddressingPropertiesImpl) {
            AddressingPropertiesImpl other = (AddressingPropertiesImpl)obj;
            return compareExpected(other);
        }
        return false;
    }    

    private boolean compareExpected(AddressingPropertiesImpl other) {
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
        return requestor
               ? outbound
                 ? CLIENT_ADDRESSING_PROPERTIES_OUTBOUND
                 : CLIENT_ADDRESSING_PROPERTIES_INBOUND
               : outbound
                 ? SERVER_ADDRESSING_PROPERTIES_OUTBOUND
                 : SERVER_ADDRESSING_PROPERTIES_INBOUND;
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

    private interface DSEI {
        @RequestWrapper(targetNamespace = "http://foo/bar/d", className = "DSEI", localName = "opDRequest")
        @ResponseWrapper(targetNamespace = "http://foo/bar/d", className = "DSEI", localName = "opDResponse")
        String op();
    }
    
    private static class TestBindingMessageInfo extends BindingMessageInfo {
    }

    private static class TestBindingOperationInfo extends BindingOperationInfo {
        private Map<QName, BindingFaultInfo> faults;
        
        public TestBindingOperationInfo(OperationInfo oi) {
            opInfo = oi;
            
            Collection<FaultInfo> of = opInfo.getFaults();
            if (of != null && !of.isEmpty()) {
                faults = new ConcurrentHashMap<QName, BindingFaultInfo>(of.size());
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
    
    private static class SetupMessageArgs {
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
    
    private static class SetupResponderArgs {
        boolean outbound; 
        boolean oneway;
        boolean decoupled;
        boolean zeroLengthAction;
        boolean fault;
        boolean noMessageId;
    }
}
