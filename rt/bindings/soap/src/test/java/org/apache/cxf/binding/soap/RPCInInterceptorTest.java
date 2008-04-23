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

package org.apache.cxf.binding.soap;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.soap.interceptor.RPCInInterceptor;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.hello_world_rpclit.types.MyComplexStruct;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Before;
import org.junit.Test;

public class RPCInInterceptorTest extends TestBase {

    private static final String TNS = "http://apache.org/hello_world_rpclit";

    private static final String OPNAME = "sendReceiveData";

    private IMocksControl control = EasyMock.createNiceControl();
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        ServiceInfo si = getMockedServiceModel(this.getClass().getResource("/wsdl/hello_world_rpc_lit.wsdl")
                .toString());
        BindingInfo bi = si.getBinding(new QName(TNS, "Greeter_SOAPBinding_RPCLit"));
        BindingOperationInfo boi = bi.getOperation(new QName(TNS, OPNAME));
        boi.getOperationInfo().getInput().getMessagePartByIndex(0).setTypeClass(MyComplexStruct.class);
        boi.getOperationInfo().getInput().getMessagePartByIndex(0).setIndex(1);
        boi.getOperationInfo().getOutput().getMessagePartByIndex(0).setTypeClass(MyComplexStruct.class);
        boi.getOperationInfo().getOutput().getMessagePartByIndex(0).setIndex(0);
        soapMessage.getExchange().put(BindingOperationInfo.class, boi);

        control.reset(); 
        Service service = control.createMock(Service.class);
        JAXBDataBinding dataBinding = new JAXBDataBinding(MyComplexStruct.class);
        service.getDataBinding();
        EasyMock.expectLastCall().andReturn(dataBinding).anyTimes();
        service.getServiceInfos();
        List<ServiceInfo> list = Arrays.asList(si);
        EasyMock.expectLastCall().andReturn(list).anyTimes();
        
        soapMessage.getExchange().put(Service.class, service);
        soapMessage.getExchange().put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.FALSE);
        control.replay();
    }

    @Test
    public void testInterceptorRPCLitOutbound() throws Exception {
        RPCInInterceptor interceptor = new RPCInInterceptor();

        soapMessage.setContent(XMLStreamReader.class, XMLInputFactory.newInstance().createXMLStreamReader(
                getTestStream(getClass(), "/rpc-resp.xml")));
        soapMessage.put(Message.REQUESTOR_ROLE, Boolean.TRUE);

        interceptor.handleMessage(soapMessage);

        List<?> parameters = (List<?>) soapMessage.getContent(List.class);
        assertEquals(1, parameters.size());

        Object obj = parameters.get(0);
        assertTrue(obj instanceof MyComplexStruct);
        MyComplexStruct s = (MyComplexStruct) obj;
        assertEquals("elem1", s.getElem1());
        assertEquals("elem2", s.getElem2());
        assertEquals(45, s.getElem3());
    }

    @Test
    public void testInterceptorRPCLitInbound() throws Exception {
        RPCInInterceptor interceptor = new RPCInInterceptor();
        soapMessage.setContent(XMLStreamReader.class, XMLInputFactory.newInstance().createXMLStreamReader(
                getTestStream(getClass(), "/rpc-req.xml")));

        interceptor.handleMessage(soapMessage);

        List<?> parameters = (List<?>) soapMessage.getContent(List.class);
        assertEquals(2, parameters.size());

        Object obj = parameters.get(1);
        assertTrue(obj instanceof MyComplexStruct);
        MyComplexStruct s = (MyComplexStruct) obj;
        assertEquals("elem1", s.getElem1());
        assertEquals("elem2", s.getElem2());
        assertEquals(45, s.getElem3());
    }

}
