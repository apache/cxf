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

package org.apache.cxf.jaxws.header;



import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;

import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.AbstractJaxWsTest;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.header_test.SOAPHeaderService;
import org.apache.header_test.TestHeader;
import org.apache.header_test.TestHeaderImpl;
import org.apache.header_test.rpc.SOAPRPCHeaderService;
import org.apache.header_test.rpc.TestRPCHeader;
import org.apache.header_test.rpc.TestRPCHeaderImpl;
import org.apache.header_test.rpc.types.HeaderMessage;
import org.apache.header_test.types.TestHeader1;
import org.apache.header_test.types.TestHeader1Response;
import org.apache.header_test.types.TestHeader2;
import org.apache.header_test.types.TestHeader2Response;
import org.apache.header_test.types.TestHeader3;
import org.apache.header_test.types.TestHeader3Response;
import org.apache.header_test.types.TestHeader5;
import org.apache.header_test.types.TestHeader5ResponseBody;
import org.apache.header_test.types.TestHeader6;
import org.apache.header_test.types.TestHeader6Response;
import org.apache.tests.type_test.all.SimpleAll;
import org.apache.tests.type_test.choice.SimpleChoice;
import org.apache.tests.type_test.sequence.SimpleStruct;
import org.junit.Before;
import org.junit.Test;


public class HeaderClientServerTest extends AbstractJaxWsTest {
    private final QName serviceName = new QName("http://apache.org/header_test",
                                                "SOAPHeaderService");    
    private final QName portName = new QName("http://apache.org/header_test",
                                             "SoapHeaderPort");

    @Before
    public void setUp() throws Exception {
        BusFactory.setDefaultBus(getBus());
        
        Object implementor = new TestHeaderImpl();
        String address = "http://localhost:9104/SoapHeaderContext/SoapHeaderPort";
        EndpointImpl e = (EndpointImpl) Endpoint.publish(address, implementor);        
        e.getServer().getEndpoint().getInInterceptors().add(new LoggingInInterceptor());
        e.getServer().getEndpoint().getOutInterceptors().add(new LoggingOutInterceptor());
        
        implementor = new TestRPCHeaderImpl();
        address = "http://localhost:9104/SoapHeaderRPCContext/SoapHeaderRPCPort";
        e = (EndpointImpl)Endpoint.publish(address, implementor);        
        e.getServer().getEndpoint().getInInterceptors().add(new LoggingInInterceptor());
        e.getServer().getEndpoint().getOutInterceptors().add(new LoggingOutInterceptor());
    }

    @Test
    public void testInHeader() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapheader.wsdl");
        assertNotNull(wsdl);
        
        SOAPHeaderService service = new SOAPHeaderService(wsdl, serviceName);
        assertNotNull(service);
        TestHeader proxy = service.getPort(portName, TestHeader.class);
        try {
            TestHeader1 val = new TestHeader1();
            for (int idx = 0; idx < 2; idx++) {
                TestHeader1Response returnVal = proxy.testHeader1(val, val);
                assertNotNull(returnVal);
                assertEquals(TestHeader1.class.getSimpleName(), returnVal.getResponseType());
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    } 

    @Test
    public void testOutHeader() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapheader.wsdl");
        assertNotNull(wsdl);
        
        SOAPHeaderService service = new SOAPHeaderService(wsdl, serviceName);
        assertNotNull(service);
        TestHeader proxy = service.getPort(portName, TestHeader.class);
        try {
            TestHeader2 in = new TestHeader2();
            String val = new String(TestHeader2Response.class.getSimpleName());
            Holder<TestHeader2Response> out = new Holder<TestHeader2Response>();
            Holder<TestHeader2Response> outHeader = new Holder<TestHeader2Response>();
            for (int idx = 0; idx < 2; idx++) {
                val += idx;                
                in.setRequestType(val);
                proxy.testHeader2(in, out, outHeader);
                
                assertEquals(val, out.value.getResponseType());
                assertEquals(val, outHeader.value.getResponseType());
            }
        } catch (UndeclaredThrowableException ex) {
            ex.printStackTrace();
            throw (Exception)ex.getCause();
        } 
    } 

    @Test
    public void testInOutHeader() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapheader.wsdl");
        assertNotNull(wsdl);
        
        SOAPHeaderService service = new SOAPHeaderService(wsdl, serviceName);
        assertNotNull(service);
        TestHeader proxy = service.getPort(portName, TestHeader.class);
        
        try {
            TestHeader3 in = new TestHeader3();
            String val = new String(TestHeader3.class.getSimpleName());
            Holder<TestHeader3> inoutHeader = new Holder<TestHeader3>();
            for (int idx = 0; idx < 2; idx++) {
                val += idx;                
                in.setRequestType(val);
                inoutHeader.value = new TestHeader3();
                TestHeader3Response returnVal = proxy.testHeader3(in, inoutHeader);
                //inoutHeader copied to return
                //in copied to inoutHeader
                assertNotNull(returnVal);
                assertNull(returnVal.getResponseType());
                assertEquals(val, inoutHeader.value.getRequestType());
                
                in.setRequestType(null);
                inoutHeader.value.setRequestType(val);
                returnVal = proxy.testHeader3(in, inoutHeader);
                assertNotNull(returnVal);
                assertEquals(val, returnVal.getResponseType());
                assertNull(inoutHeader.value.getRequestType());
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        } 
    }

    @Test
    public void testReturnHeader() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapheader.wsdl");
        assertNotNull(wsdl);
        
        SOAPHeaderService service = new SOAPHeaderService(wsdl, serviceName);
        assertNotNull(service);
        TestHeader proxy = service.getPort(portName, TestHeader.class);
        try {
            Holder<TestHeader5ResponseBody> out = new Holder<TestHeader5ResponseBody>();
            Holder<TestHeader5> outHeader = new Holder<TestHeader5>();
            TestHeader5 in = new TestHeader5();
            String val = new String(TestHeader5.class.getSimpleName());
            for (int idx = 0; idx < 2; idx++) {
                val += idx;            
                in.setRequestType(val);
                proxy.testHeader5(out, outHeader, in);
                assertEquals(1000, out.value.getResponseType());
                assertEquals(val, outHeader.value.getRequestType());
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        } 
    } 
    
    @Test
    public void testHeaderPartBeforeBodyPart() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapheader.wsdl");
        assertNotNull(wsdl);
        
        SOAPHeaderService service = new SOAPHeaderService(wsdl, serviceName);
        assertNotNull(service);
        TestHeader proxy = service.getPort(portName, TestHeader.class);
    
        TestHeader6 in = new TestHeader6();
        String val = new String(TestHeader6.class.getSimpleName());
        Holder<TestHeader3> inoutHeader = new Holder<TestHeader3>();
        for (int idx = 0; idx < 1; idx++) {
            val += idx;                
            in.setRequestType(val);
            inoutHeader.value = new TestHeader3();
            TestHeader6Response returnVal = proxy.testHeaderPartBeforeBodyPart(inoutHeader, in);
            //inoutHeader copied to return
            //in copied to inoutHeader
            assertNotNull(returnVal);
            assertNull(returnVal.getResponseType());
            assertEquals(val, inoutHeader.value.getRequestType());
            
            in.setRequestType(null);
            inoutHeader.value.setRequestType(val);
            returnVal = proxy.testHeaderPartBeforeBodyPart(inoutHeader, in);
            assertNotNull(returnVal);
            assertEquals(val, returnVal.getResponseType());
            assertNull(inoutHeader.value.getRequestType());
        }
    }
    
    @Test
    public void testHeader4() {
        URL wsdl = getClass().getResource("/wsdl/soapheader.wsdl");
        assertNotNull(wsdl);
        
        SOAPHeaderService service = new SOAPHeaderService(wsdl, serviceName);
        assertNotNull(service);
        TestHeader proxy = service.getPort(portName, TestHeader.class);
        try {
            proxy.testHeader4("cxf");
        } catch (Exception e) {
            // REVISIT
            // fail("No exception should happen in testHeader4");
        }
    }

    @Test
    public void testRPCInHeader() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapheader_rpc.wsdl");
        assertNotNull(wsdl);
        
        SOAPRPCHeaderService service
            = new SOAPRPCHeaderService(wsdl, 
                new QName("http://apache.org/header_test/rpc", "SOAPRPCHeaderService"));
        assertNotNull(service);
        TestRPCHeader proxy = service.getSoapRPCHeaderPort();
        try { 
            HeaderMessage header = new HeaderMessage();
            header.setHeaderVal("header");
            
            for (int idx = 0; idx < 2; idx++) {
                String returnVal = proxy.testHeader1("part", header);
                assertNotNull(returnVal);
                assertEquals("part/header", returnVal);
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    } 
    
    @Test
    public void testRPCInOutHeader() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapheader_rpc.wsdl");
        assertNotNull(wsdl);
        
        SOAPRPCHeaderService service
            = new SOAPRPCHeaderService(wsdl, 
                new QName("http://apache.org/header_test/rpc", "SOAPRPCHeaderService"));
        assertNotNull(service);
        TestRPCHeader proxy = service.getSoapRPCHeaderPort();
        try { 
            HeaderMessage header = new HeaderMessage();
            Holder<HeaderMessage> holder = new Holder<HeaderMessage>(header);
            
            for (int idx = 0; idx < 2; idx++) {
                holder.value.setHeaderVal("header" + idx);
                String returnVal = proxy.testInOutHeader("part" + idx, holder);
                
                assertNotNull(returnVal);
                assertEquals("header" + idx, returnVal);
                assertEquals("part" + idx, holder.value.getHeaderVal());
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    } 
    
    

    @Test
    public void testHolderOutIsTheFirstMessagePart() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/soapheader.wsdl");
        assertNotNull(wsdl);
        SOAPHeaderService service = new SOAPHeaderService(wsdl, serviceName);
        assertNotNull(service);
        TestHeader proxy = service.getPort(portName, TestHeader.class);
        Holder<SimpleAll> simpleAll = new Holder<SimpleAll>();
        SimpleAll sa = new SimpleAll();
        sa.setVarAttrString("varAttrString");
        sa.setVarInt(100);
        sa.setVarString("varString");
        simpleAll.value = sa;
        SimpleChoice sc = new SimpleChoice();
        sc.setVarString("scVarString");
        SimpleStruct ss = proxy.sendReceiveAnyType(simpleAll, sc);
        assertEquals(simpleAll.value.getVarString(), "scVarString");
        assertEquals(ss.getVarInt(), 200);
        assertEquals(ss.getVarAttrString(), "varAttrStringRet");
    }
   
}
