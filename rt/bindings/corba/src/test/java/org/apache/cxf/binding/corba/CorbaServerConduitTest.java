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
package org.apache.cxf.binding.corba;

import java.io.OutputStream;
import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.corba.runtime.CorbaStreamableImpl;
import org.apache.cxf.binding.corba.types.CorbaPrimitiveHandler;
import org.apache.cxf.binding.corba.utils.OrbConfig;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.omg.CORBA.Any;
import org.omg.CORBA.NVList;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ServerRequest;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CorbaServerConduitTest {
    protected EndpointInfo endpointInfo;
    protected EndpointReferenceType target;
    protected MessageObserver observer;

    ORB orb;
    Bus bus;
    Message inMessage;
    CorbaBindingFactory factory;
    TestUtils testUtils;
    OrbConfig orbConfig;
    CorbaTypeMap corbaTypeMap;
    private org.omg.CORBA.Object targetObject;


    @Before
    public void setUp() throws Exception {
        bus = BusFactory.getDefaultBus();

        java.util.Properties props = System.getProperties();


        props.put("yoko.orb.id", "CXF-CORBA-Server-Binding");
        orb = ORB.init(new String[0], props);
        orbConfig = new OrbConfig();
        targetObject = mock(org.omg.CORBA.Object.class);
    }

    @After
    public void tearDown() {
        bus.shutdown(true);
        if (orb != null) {
            try {
                orb.destroy();
            } catch (Exception ex) {
                // Do nothing.  Throw an Exception?
            }
        }
    }

    @Test
    public void testCorbaServerConduit() throws Exception {
        CorbaServerConduit conduit = setupCorbaServerConduit(false);
        assertNotNull("conduit should not be null", conduit);
    }

    @Test
    public void testPrepare() throws Exception {
        setupServiceInfo("http://cxf.apache.org/bindings/corba/simple",
                         "/wsdl_corbabinding/simpleIdl.wsdl", "SimpleCORBAService",
                         "SimpleCORBAPort");
        CorbaDestination destination = new CorbaDestination(endpointInfo, orbConfig);
        CorbaServerConduit conduit = new CorbaServerConduit(endpointInfo,
                                                            destination.getAddress(),
                                                            targetObject,
                                                            null,
                                                            orbConfig,
                                                            corbaTypeMap);
        CorbaMessage message = new CorbaMessage(new MessageImpl());
        try {
            conduit.prepare(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        OutputStream os = message.getContent(OutputStream.class);
        assertNotNull("OutputStream should not be null", os);
        ORB orb2 = (ORB)message.get("orb");
        assertNotNull("Orb should not be null", orb2);
        Object obj = message.get("endpoint");
        assertNotNull("EndpointReferenceType should not be null", obj);

        assertTrue("passed in targetObject is used",
                targetObject.equals(message.get(CorbaConstants.CORBA_ENDPOINT_OBJECT)));

        destination.shutdown();
    }


    @Test
    public void testGetTargetReference() throws Exception {
        setupServiceInfo("http://cxf.apache.org/bindings/corba/simple",
                         "/wsdl_corbabinding/simpleIdl.wsdl", "SimpleCORBAService",
                         "SimpleCORBAPort");

        CorbaDestination destination = new CorbaDestination(endpointInfo, orbConfig);
        CorbaServerConduit conduit = new CorbaServerConduit(endpointInfo,
                                                            destination.getAddress(),
                                                            targetObject,
                                                            null,
                                                            orbConfig,
                                                            corbaTypeMap);

        EndpointReferenceType t = null;
        EndpointReferenceType ref = conduit.getTargetReference(t);
        assertNotNull("ref should not be null", ref);
        destination.shutdown();
    }

    @Test
    public void testGetAddress() throws Exception  {
        setupServiceInfo("http://cxf.apache.org/bindings/corba/simple",
                         "/wsdl_corbabinding/simpleIdl.wsdl", "SimpleCORBAService",
                         "SimpleCORBAPort");

        CorbaDestination destination = new CorbaDestination(endpointInfo, orbConfig);
        endpointInfo.setAddress("corbaloc::localhost:40000/Simple");
        CorbaServerConduit conduit = new CorbaServerConduit(endpointInfo,
                                                            destination.getAddress(),
                                                            targetObject,
                                                            null,
                                                            orbConfig,
                                                            corbaTypeMap);
        String address = conduit.getAddress();
        assertNotNull("address should not be null", address);
        assertEquals(address, "corbaloc::localhost:40000/Simple");
    }

    @Test
    public void testClose() throws Exception {
        final Exchange exchange = mock(Exchange.class);
        final CorbaMessage inMsg = mock(CorbaMessage.class);
        when(exchange.getInMessage()).thenReturn(inMsg);
        CorbaServerConduit conduit = mock(CorbaServerConduit.class);
        CorbaMessage msg = mock(CorbaMessage.class);
        when(msg.getExchange()).thenReturn(exchange);
        doCallRealMethod().when(conduit).buildRequestResult(msg);
        doCallRealMethod().when(conduit).close(msg);

        OutputStream stream = mock(OutputStream.class);
        when(msg.getContent(OutputStream.class)).thenReturn(stream);
        doCallRealMethod().when(stream).close();

        conduit.close(msg);
        verify(conduit, times(1)).buildRequestResult(msg);
        verify(stream, times(1)).close();
    }

    @Test
    public void testBuildRequestResult() {
        NVList list = orb.create_list(0);
        CorbaServerConduit conduit = setupCorbaServerConduit(false);
        CorbaMessage msg = mock(CorbaMessage.class);
        Exchange exchange = mock(Exchange.class);
        ServerRequest request = mock(ServerRequest.class);

        when(msg.getExchange()).thenReturn(exchange);
        when(exchange.get(ServerRequest.class)).thenReturn(request);

        when(exchange.isOneWay()).thenReturn(false);
        CorbaMessage inMsg = mock(CorbaMessage.class);
        when(msg.getExchange()).thenReturn(exchange);
        when(exchange.getInMessage()).thenReturn(inMsg);

        when(inMsg.getList()).thenReturn(list);
        when(msg.getStreamableException()).thenReturn(null);
        when(msg.getStreamableArguments()).thenReturn(null);
        when(msg.getStreamableReturn()).thenReturn(null);

        conduit.buildRequestResult(msg);
    }

    @Test
    public void testBuildRequestResultException() {
        NVList list = orb.create_list(0);
        CorbaServerConduit conduit = setupCorbaServerConduit(false);
        CorbaMessage msg = mock(CorbaMessage.class);
        Exchange exchange = mock(Exchange.class);
        ServerRequest request = mock(ServerRequest.class);

        when(msg.getExchange()).thenReturn(exchange);
        when(exchange.get(ServerRequest.class)).thenReturn(request);

        when(exchange.isOneWay()).thenReturn(false);

        CorbaMessage inMsg = mock(CorbaMessage.class);
        when(msg.getExchange()).thenReturn(exchange);
        when(exchange.getInMessage()).thenReturn(inMsg);
        when(exchange.getBus()).thenReturn(bus);


        when(inMsg.getList()).thenReturn(list);
        QName objName = new QName("object");
        QName objIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "short", CorbaConstants.NP_WSDL_CORBA);
        TypeCode objTypeCode = orb.get_primitive_tc(TCKind.tk_short);
        CorbaPrimitiveHandler obj = new CorbaPrimitiveHandler(objName, objIdlType, objTypeCode, null);
        CorbaStreamable exception = new CorbaStreamableImpl(obj, objName);

        when(msg.getStreamableException()).thenReturn(exception);
        when(msg.getStreamableException()).thenReturn(exception);

        conduit.buildRequestResult(msg);
    }

    @Test
    public void testBuildRequestResultArgumentReturn() {
        CorbaStreamable[] arguments = new CorbaStreamable[1];
        QName objName = new QName("object");
        QName objIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "short", CorbaConstants.NP_WSDL_CORBA);
        TypeCode objTypeCode = orb.get_primitive_tc(TCKind.tk_short);
        CorbaPrimitiveHandler obj = new CorbaPrimitiveHandler(objName, objIdlType, objTypeCode, null);
        CorbaStreamable arg = new CorbaStreamableImpl(obj, objName);
        arguments[0] = arg;
        arguments[0].setMode(org.omg.CORBA.ARG_OUT.value);

        NVList nvlist = orb.create_list(2);
        Any value = orb.create_any();
        value.insert_Streamable(arguments[0]);
        nvlist.add_value(arguments[0].getName(), value, arguments[0].getMode());

        CorbaServerConduit conduit = setupCorbaServerConduit(false);
        CorbaMessage msg = mock(CorbaMessage.class);
        Exchange exchange = mock(Exchange.class);
        ServerRequest request = mock(ServerRequest.class);
        when(exchange.getBus()).thenReturn(bus);

        when(msg.getExchange()).thenReturn(exchange);
        when(exchange.get(ServerRequest.class)).thenReturn(request);

        when(exchange.isOneWay()).thenReturn(false);
        when(msg.getExchange()).thenReturn(exchange);
        Message message = new MessageImpl();
        CorbaMessage corbaMessage = new CorbaMessage(message);
        corbaMessage.setList(nvlist);

        when(exchange.getInMessage()).thenReturn(corbaMessage);
        when(msg.getStreamableException()).thenReturn(null);
        when(msg.getStreamableArguments()).thenReturn(arguments);
        when(msg.getStreamableReturn()).thenReturn(arg);

        conduit.buildRequestResult(msg);
    }

    @Test
    public void testGetTarget()  {
        CorbaServerConduit conduit = setupCorbaServerConduit(false);
        EndpointReferenceType endpoint = conduit.getTarget();
        assertNotNull("EndpointReferenceType should not be null", endpoint);
    }


    protected CorbaServerConduit setupCorbaServerConduit(boolean send) {
        target = mock(EndpointReferenceType.class);
        endpointInfo = mock(EndpointInfo.class);
        CorbaServerConduit corbaServerConduit =
            new CorbaServerConduit(endpointInfo, target, targetObject,
                                   null, orbConfig, corbaTypeMap);

        if (send) {
            // setMessageObserver
            observer = new MessageObserver() {
                public void onMessage(Message m) {
                    inMessage = m;
                }
            };
            corbaServerConduit.setMessageObserver(observer);
        }

        return corbaServerConduit;
    }

    protected void setupServiceInfo(String ns, String wsdl, String serviceName, String portName) {
        URL wsdlUrl = getClass().getResource(wsdl);
        assertNotNull(wsdlUrl);
        WSDLServiceFactory f = new WSDLServiceFactory(bus, wsdlUrl.toString(), new QName(ns, serviceName));

        Service service = f.create();
        endpointInfo = service.getEndpointInfo(new QName(ns, portName));

    }




}