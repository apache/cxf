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
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.corba.types.CorbaPrimitiveHandler;
import org.apache.cxf.binding.corba.utils.CorbaUtils;
import org.apache.cxf.binding.corba.utils.OrbConfig;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.OperationType;
import org.apache.cxf.binding.corba.wsdl.RaisesType;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.omg.CORBA.Context;
import org.omg.CORBA.ContextList;
import org.omg.CORBA.ExceptionList;
import org.omg.CORBA.NVList;
import org.omg.CORBA.NamedValue;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Request;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CorbaConduitTest {

    private static ORB orb;
    private static Bus bus;
    protected EndpointInfo endpointInfo;
    protected EndpointReferenceType target;
    protected MessageObserver observer;
    Message inMessage;
    CorbaBindingFactory factory;
    OrbConfig orbConfig;

    @Before
    public void setUp() throws Exception {
        bus = BusFactory.getDefaultBus();

        java.util.Properties props = System.getProperties();


        props.put("yoko.orb.id", "CXF-CORBA-Server-Binding");
        orb = ORB.init(new String[0], props);
        orbConfig = new OrbConfig();
    }

    @After
    public void tearDown() {
        if (orb != null) {
            try {
                orb.destroy();
            } catch (Exception ex) {
                // Do nothing.  Throw an Exception?
            }
        }
    }

    @Test
    public void testCorbaConduit() throws Exception {
        CorbaConduit conduit = setupCorbaConduit(false);
        assertNotNull("conduit should not be null", conduit);
    }

    @Test
    public void testPrepare() throws Exception {
        setupServiceInfo("http://cxf.apache.org/bindings/corba/simple",
                         "/wsdl_corbabinding/simpleIdl.wsdl", "SimpleCORBAService",
                         "SimpleCORBAPort");

        CorbaDestination destination = new CorbaDestination(endpointInfo, orbConfig);
        CorbaConduit conduit = new CorbaConduit(endpointInfo, destination.getAddress(), orbConfig);
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
    }


    @Test
    public void testGetTargetReference() throws Exception {
        setupServiceInfo("http://cxf.apache.org/bindings/corba/simple",
                         "/wsdl_corbabinding/simpleIdl.wsdl", "SimpleCORBAService",
                         "SimpleCORBAPort");

        CorbaDestination destination = new CorbaDestination(endpointInfo, orbConfig);
        CorbaConduit conduit = new CorbaConduit(endpointInfo, destination.getAddress(), orbConfig);

        EndpointReferenceType t = null;
        EndpointReferenceType ref = conduit.getTargetReference(t);
        assertNotNull("ref should not be null", ref);
    }

    @Test
    public void testGetAddress() throws Exception  {
        setupServiceInfo("http://cxf.apache.org/bindings/corba/simple",
                         "/wsdl_corbabinding/simpleIdl.wsdl", "SimpleCORBAService",
                         "SimpleCORBAPort");

        CorbaDestination destination = new CorbaDestination(endpointInfo, orbConfig);
        endpointInfo.setAddress("corbaloc::localhost:40000/Simple");
        CorbaConduit conduit = new CorbaConduit(endpointInfo, destination.getAddress(), orbConfig);
        String address = conduit.getAddress();
        assertNotNull("address should not be null", address);
        assertEquals(address, "corbaloc::localhost:40000/Simple");
    }

    @Test
    public void testClose() throws Exception {
        CorbaConduit conduit = mock(CorbaConduit.class);

        org.omg.CORBA.Object obj = mock(org.omg.CORBA.Object.class);
        CorbaMessage msg = mock(CorbaMessage.class);
        when(msg.get(CorbaConstants.CORBA_ENDPOINT_OBJECT)).thenReturn(obj);
        Exchange exg = mock(Exchange.class);
        when(msg.getExchange()).thenReturn(exg);
        BindingOperationInfo bopInfo = mock(BindingOperationInfo.class);
        when(exg.getBindingOperationInfo()).thenReturn(bopInfo);
        OperationType opType = mock(OperationType.class);
        when(bopInfo.getExtensor(OperationType.class)).thenReturn(opType);

        OutputStream os = mock(OutputStream.class);
        when(msg.getContent(OutputStream.class)).thenReturn(os);
        doCallRealMethod().when(os).close();
        doCallRealMethod().when(conduit).close(msg);

        conduit.close(msg);
        verify(conduit, times(1)).buildRequest(msg, opType);
        verify(os, times(1)).close();
    }

    @Test
    public void testGetTarget() throws Exception {
        CorbaConduit conduit = setupCorbaConduit(false);
        EndpointReferenceType endpoint = conduit.getTarget();
        assertNotNull("EndpointReferenceType should not be null", endpoint);
    }

    @Test
    public void testGetOperationExceptions() {
        CorbaConduit conduit = mock(CorbaConduit.class);
        OperationType opType = mock(OperationType.class);
        CorbaTypeMap typeMap = mock(CorbaTypeMap.class);

        @SuppressWarnings("unchecked")
        List<RaisesType> exlist = mock(List.class);
        when(opType.getRaises()).thenReturn(exlist);
        int i = 0;
        when(exlist.size()).thenReturn(i);
        RaisesType rType = mock(RaisesType.class);
        when(exlist.get(0)).thenReturn(rType);

        conduit.getOperationExceptions(opType, typeMap);
        assertEquals(exlist.size(), 0);
    }

    @Test
    public void testBuildRequest() throws Exception {
        CorbaConduit conduit = setupCorbaConduit(false);
        CorbaMessage message = mock(CorbaMessage.class);
        Exchange exchange = mock(Exchange.class);
        when(message.getExchange()).thenReturn(exchange);
        ServiceInfo service = mock(ServiceInfo.class);
        when(exchange.get(ServiceInfo.class)).thenReturn(service);
        @SuppressWarnings("unchecked")
        List<CorbaTypeMap> list = mock(List.class);
        CorbaTypeMap typeMap = mock(CorbaTypeMap.class);
        when(service.getExtensors(CorbaTypeMap.class)).thenReturn(list);

        OperationType opType = mock(OperationType.class);
        when(conduit.getArguments(message)).thenReturn(null);
        when(conduit.getReturn(message)).thenReturn(null);
        when(conduit.getExceptionList(conduit.getOperationExceptions(opType, typeMap),
                                 message,
                                 opType)).thenReturn(null);

        conduit.getRequest(message, "Hello", null, null, null);
    }

    @Test
    public void testBuildArguments() throws Exception {
        Message msg = new MessageImpl();
        CorbaMessage message = new CorbaMessage(msg);
        Exchange exchange = new ExchangeImpl();
        exchange.put(Bus.class, bus);
        message.setExchange(exchange);
        CorbaStreamable[] arguments = new CorbaStreamable[1];
        QName objName = new QName("object");
        QName objIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "short", CorbaConstants.NP_WSDL_CORBA);
        TypeCode objTypeCode = orb.get_primitive_tc(TCKind.tk_short);
        CorbaPrimitiveHandler obj1 = new CorbaPrimitiveHandler(objName, objIdlType, objTypeCode, null);
        CorbaStreamable arg = message.createStreamableObject(obj1, objName);
        arguments[0] = arg;
        arguments[0].setMode(org.omg.CORBA.ARG_OUT.value);

        CorbaConduit conduit = setupCorbaConduit(false);
        NVList list = conduit.getArguments(message);
        assertNotNull("list should not be null", list != null);

        message.setStreamableArguments(arguments);
        NVList listArgs = conduit.getArguments(message);
        assertNotNull("listArgs should not be null", listArgs != null);
        assertNotNull("listArgs Item should not be null", listArgs.item(0) != null);
        assertEquals("Name should be equal", listArgs.item(0).name(), "object");
        assertEquals("flags should be 2", listArgs.item(0).flags(), 2);
        assertNotNull("Any Value should not be null", listArgs.item(0).value() != null);
    }

    @Test
    public void testBuildReturn() throws Exception {
        Message msg = new MessageImpl();
        CorbaMessage message = new CorbaMessage(msg);
        Exchange exchange = new ExchangeImpl();
        exchange.put(Bus.class, bus);
        message.setExchange(exchange);

        QName objName = new QName("returnName");
        QName objIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "short", CorbaConstants.NP_WSDL_CORBA);
        TypeCode objTypeCode = orb.get_primitive_tc(TCKind.tk_short);
        CorbaPrimitiveHandler obj1 = new CorbaPrimitiveHandler(objName, objIdlType, objTypeCode, null);
        CorbaStreamable arg = message.createStreamableObject(obj1, objName);

        CorbaConduit conduit = setupCorbaConduit(false);
        NamedValue ret = conduit.getReturn(message);
        assertNotNull("Return should not be null", ret != null);
        assertEquals("name should be equal", ret.name(), "return");

        message.setStreamableReturn(arg);
        NamedValue ret2 = conduit.getReturn(message);
        assertNotNull("Return2 should not be null", ret2 != null);
        assertEquals("name should be equal", ret2.name(), "returnName");
    }

    @Test
    public void testBuildExceptionListEmpty() throws Exception {
        CorbaConduit conduit = setupCorbaConduit(false);
        Message msg = new MessageImpl();
        CorbaMessage message = new CorbaMessage(msg);
        OperationType opType = new OperationType();
        opType.setName("review_data");
        ExceptionList exList = conduit.getExceptionList(conduit.getOperationExceptions(opType, null),
                                                        message,
                                                        opType);
        assertNotNull("ExcepitonList is not null", exList != null);
        assertEquals("The list should be empty", exList.count(), 0);
    }

    @Test
    public void testBuildExceptionListWithExceptions() throws Exception {
        CorbaConduit conduit = setupCorbaConduit(false);
        Message msg = new MessageImpl();
        CorbaMessage message = new CorbaMessage(msg);
        TestUtils testUtils = new TestUtils();
        CorbaDestination destination = testUtils.getExceptionTypesTestDestination();
        EndpointInfo endpointInfo2 = destination.getEndPointInfo();
        QName name = new QName("http://schemas.apache.org/idl/except", "review_data", "");
        BindingOperationInfo bInfo = destination.getBindingInfo().getOperation(name);
        OperationType opType = bInfo.getExtensor(OperationType.class);
        CorbaTypeMap typeMap = null;
        List<TypeMappingType> corbaTypes =
            endpointInfo2.getService().getDescription().getExtensors(TypeMappingType.class);
        if (corbaTypes != null) {
            typeMap = CorbaUtils.createCorbaTypeMap(corbaTypes);
        }

        ExceptionList exList = conduit.getExceptionList(conduit.getOperationExceptions(opType, typeMap),
                                                        message,
                                                        opType);

        assertNotNull("ExceptionList is not null", exList != null);
        assertNotNull("TypeCode is not null", exList.item(0) != null);
        assertEquals("ID should be equal", exList.item(0).id(), "IDL:BadRecord:1.0");
        assertEquals("ID should be equal", exList.item(0).name(), "BadRecord");
        assertEquals("ID should be equal", exList.item(0).member_count(), 2);
        assertEquals("ID should be equal", exList.item(0).member_name(0), "reason");
        assertNotNull("Member type is not null", exList.item(0).member_type(0) != null);
    }

    @Test
    public void testInvoke() throws Exception {
        CorbaConduit conduit = setupCorbaConduit(false);
        //CorbaMessage message = new CorbaMessage(msg);
        CorbaMessage message = mock(CorbaMessage.class);
        /*String opName = "GreetMe";
        NVList nvlist = (NVList)orb.create_list(0);

        Request request = conduit.getRequest(message, "GreetMe", nvlist, null, null);
        request.invoke();
        */

        org.omg.CORBA.Object obj = mock(org.omg.CORBA.Object.class);
        when(message.get(CorbaConstants.CORBA_ENDPOINT_OBJECT)).thenReturn(obj);

        //msg.put(CorbaConstants.CORBA_ENDPOINT_OBJECT, obj);
        Request r = mock(Request.class);
        NVList nvList = orb.create_list(0);
        NamedValue ret = mock(NamedValue.class);
        ExceptionList exList = mock(ExceptionList.class);

        when(obj._create_request(nullable(Context.class),
                            eq("greetMe"),
                            isA(NVList.class),
                            isA(NamedValue.class),
                            isA(ExceptionList.class),
                            isA(ContextList.class))).thenReturn(r);

        Request request = conduit.getRequest(message, "greetMe", nvList, ret, exList);
        request.invoke();

          /* try {
                ContextList ctxList = orb.create_context_list();
                Context ctx = orb.get_default_context();
                org.omg.CORBA.Object targetObj = (org.omg.CORBA.Object)message
                    .get(CorbaConstants.CORBA_ENDPOINT_OBJECT);
                Request request = targetObj._create_request(ctx, opName, list, ret, exList, ctxList);
                request.invoke();

            } catch (java.lang.Exception ex) {
                ex.printStackTrace();
            }*/
        verify(r, times(1)).invoke();
    }

    protected CorbaConduit setupCorbaConduit(boolean send) {
        target = mock(EndpointReferenceType.class);
        endpointInfo = mock(EndpointInfo.class);
        CorbaConduit corbaConduit = new CorbaConduit(endpointInfo, target, orbConfig);

        if (send) {
            // setMessageObserver
            observer = new MessageObserver() {
                public void onMessage(Message m) {
                    inMessage = m;
                }
            };
            corbaConduit.setMessageObserver(observer);
        }

        return corbaConduit;
    }

    protected void setupServiceInfo(String ns, String wsdl, String serviceName, String portName) {
        URL wsdlUrl = getClass().getResource(wsdl);
        assertNotNull(wsdlUrl);
        WSDLServiceFactory f = new WSDLServiceFactory(bus, wsdlUrl.toString(), new QName(ns, serviceName));

        Service service = f.create();
        endpointInfo = service.getEndpointInfo(new QName(ns, portName));

    }




}
