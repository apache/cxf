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
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
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
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.Context;
import org.omg.CORBA.ContextList;
import org.omg.CORBA.ExceptionList;
import org.omg.CORBA.NVList;
import org.omg.CORBA.NamedValue;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Request;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;


public class CorbaConduitTest extends Assert {
    
    private static IMocksControl control;
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
        control = EasyMock.createNiceControl();
     
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
        assertTrue("conduit should not be null", conduit != null);
    }
    
    @Test
    public void testPrepare() throws Exception {       
        setupServiceInfo("http://cxf.apache.org/bindings/corba/simple",
                         "/wsdl_corbabinding/simpleIdl.wsdl", "SimpleCORBAService",
                         "SimpleCORBAPort");

        CorbaDestination destination = new CorbaDestination(endpointInfo, orbConfig);
        
        if (System.getProperty("java.vendor").contains("IBM")) {
            //IBM requires it to activate to resolve it, but cannot
            //activate on sun without more config
            destination.activate();
        }

        CorbaConduit conduit = new CorbaConduit(endpointInfo, destination.getAddress(), orbConfig);
        CorbaMessage message = new CorbaMessage(new MessageImpl());
        try {
            conduit.prepare(message);
        } catch (Exception ex) {
            ex.printStackTrace();            
        }
        OutputStream os = message.getContent(OutputStream.class);
        assertTrue("OutputStream should not be null", os != null);        
        ORB orb2 = (ORB)message.get("orb");
        assertTrue("Orb should not be null", orb2 != null);
        Object obj = message.get("endpoint");
        assertTrue("EndpointReferenceType should not be null", obj != null);  
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
        assertTrue("ref should not be null", ref != null);
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
        assertTrue("address should not be null", address != null);
        assertEquals(address, "corbaloc::localhost:40000/Simple");                
    }
    
    @Test
    public void testClose() throws Exception {       
        Method m = CorbaConduit.class.getDeclaredMethod("buildRequest", 
            new Class[] {CorbaMessage.class, OperationType.class});
        CorbaConduit conduit = EasyMock.createMock(CorbaConduit.class, new Method[] {m});       
        
        org.omg.CORBA.Object obj = control.createMock(org.omg.CORBA.Object.class);
        CorbaMessage msg = control.createMock(CorbaMessage.class);
        EasyMock.expect(msg.get(CorbaConstants.CORBA_ENDPOINT_OBJECT)).andReturn(obj);
        Exchange exg = control.createMock(Exchange.class);
        EasyMock.expect(msg.getExchange()).andReturn(exg);
        BindingOperationInfo bopInfo = control.createMock(BindingOperationInfo.class);
        EasyMock.expect(exg.get(BindingOperationInfo.class)).andReturn(bopInfo);
        OperationType opType = control.createMock(OperationType.class);
        bopInfo.getExtensor(OperationType.class);
        EasyMock.expectLastCall().andReturn(opType);
        conduit.buildRequest(msg, opType);
        EasyMock.expectLastCall();
        OutputStream os = control.createMock(OutputStream.class);
        EasyMock.expect(msg.getContent(OutputStream.class)).andReturn(os);
        os.close();
        EasyMock.expectLastCall();
        
        control.replay();
        conduit.close(msg);
        control.verify();
    }
    
    @Test
    public void testGetTarget() throws Exception {
        CorbaConduit conduit = setupCorbaConduit(false);
        EndpointReferenceType endpoint = conduit.getTarget();
        assertTrue("EndpointReferenceType should not be null", endpoint != null);
    }
    
    @Test
    public void testGetOperationExceptions() {    
        CorbaConduit conduit = control.createMock(CorbaConduit.class);    
        OperationType opType = control.createMock(OperationType.class);
        CorbaTypeMap typeMap = control.createMock(CorbaTypeMap.class);
        
        List<RaisesType> exlist = CastUtils.cast(control.createMock(ArrayList.class));                
        opType.getRaises();
        EasyMock.expectLastCall().andReturn(exlist);
        int i = 0;
        EasyMock.expect(exlist.size()).andReturn(i);        
        RaisesType rType = control.createMock(RaisesType.class);        
        EasyMock.expect(exlist.get(0)).andReturn(rType);
        
        control.replay();
        conduit.getOperationExceptions(opType, typeMap);
        assertEquals(exlist.size(), 0);        
    }
    
    @Test
    public void testBuildRequest() throws Exception {
        CorbaConduit conduit = setupCorbaConduit(false);            
        CorbaMessage message = control.createMock(CorbaMessage.class);
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange());
        EasyMock.expectLastCall().andReturn(exchange);        
        ServiceInfo service = control.createMock(ServiceInfo.class);
        EasyMock.expect(exchange.get(ServiceInfo.class)).andReturn(service);
        List<CorbaTypeMap> list = CastUtils.cast(control.createMock(List.class));
        CorbaTypeMap typeMap = control.createMock(CorbaTypeMap.class);
        EasyMock.expect(service.getExtensors(CorbaTypeMap.class)).andReturn(list);                
        
        OperationType opType = control.createMock(OperationType.class);
        conduit.getArguments(message);
        EasyMock.expectLastCall().andReturn(null);  
        conduit.getReturn(message);
        EasyMock.expectLastCall().andReturn(null);
        conduit.getExceptionList(conduit.getOperationExceptions(opType, typeMap),
                                 message,
                                 opType);
        EasyMock.expectLastCall().andReturn(null);
        
        conduit.getRequest(message, "Hello", null, null, null);
        EasyMock.expectLastCall();
    }
    
    @Test
    public void testBuildArguments() throws Exception {
        Message msg = new MessageImpl();
        CorbaMessage message = new CorbaMessage(msg);
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
        NVList listArgs = (NVList)conduit.getArguments(message);
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

        QName objName = new QName("returnName");
        QName objIdlType = new QName(CorbaConstants.NU_WSDL_CORBA, "short", CorbaConstants.NP_WSDL_CORBA);
        TypeCode objTypeCode = orb.get_primitive_tc(TCKind.tk_short);
        CorbaPrimitiveHandler obj1 = new CorbaPrimitiveHandler(objName, objIdlType, objTypeCode, null);
        CorbaStreamable arg = message.createStreamableObject(obj1, objName);        
        
        CorbaConduit conduit = setupCorbaConduit(false);
        NamedValue ret = (NamedValue)conduit.getReturn(message);
        assertNotNull("Return should not be null", ret != null);
        assertEquals("name should be equal", ret.name(), "return");
        
        message.setStreamableReturn(arg);
        NamedValue ret2  = (NamedValue)conduit.getReturn(message);
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
        CorbaMessage message = control.createMock(CorbaMessage.class);
        /*String opName = "GreetMe";
        NVList nvlist = (NVList)orb.create_list(0);
        
        Request request = conduit.getRequest(message, "GreetMe", nvlist, null, null);
        request.invoke();
        */   
        
        org.omg.CORBA.Object obj = control.createMock(org.omg.CORBA.Object.class); 
        EasyMock.expect(message.get(CorbaConstants.CORBA_ENDPOINT_OBJECT)).andReturn(obj);
        
        //msg.put(CorbaConstants.CORBA_ENDPOINT_OBJECT, obj);        
        Request r = control.createMock(Request.class);        
        NVList nvList = (NVList)orb.create_list(0);
        NamedValue ret = control.createMock(NamedValue.class);
        ExceptionList exList = control.createMock(ExceptionList.class);        
        
        EasyMock.expect(obj._create_request((Context)EasyMock.anyObject(), 
                            EasyMock.eq("greetMe"),
                            EasyMock.isA(NVList.class),
                            EasyMock.isA(NamedValue.class),
                            EasyMock.isA(ExceptionList.class),
                            EasyMock.isA(ContextList.class)));
        EasyMock.expectLastCall().andReturn(r);
        r.invoke();
        EasyMock.expectLastCall();
        
        control.replay();
        Request request = conduit.getRequest(message, "greetMe", nvList, ret, exList);
        request.invoke();
        control.verify();
        
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
    }
           
    protected CorbaConduit setupCorbaConduit(boolean send) {
        target = EasyMock.createMock(EndpointReferenceType.class);                   
        endpointInfo = EasyMock.createMock(EndpointInfo.class);
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
        WSDLServiceFactory f = new WSDLServiceFactory(bus, wsdlUrl, new QName(ns, serviceName));

        Service service = f.create();        
        endpointInfo = service.getEndpointInfo(new QName(ns, portName));
   
    }

    
       
           
}
