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

package org.apache.cxf.systest.jaxws;

import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;
import javax.jws.soap.SOAPBinding.Use;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.hello_world_rpclit.GreeterRPCLit;
import org.apache.hello_world_rpclit.SOAPServiceRPCLit;
import org.apache.hello_world_rpclit.types.MyComplexStruct;
import org.apache.hello_world_soap_http.RPCLitGreeterImpl;
import org.junit.BeforeClass;
import org.junit.Test;


public class ClientServerRPCLitTest extends AbstractBusClientServerTestBase {

    private final QName portName = new QName("http://apache.org/hello_world_rpclit", "SoapPortRPCLit");

    public static class Server extends AbstractBusTestServerBase {        

        protected void run()  {
            String address;
            Object implementor = new RPCLitGreeterImpl();
            address = "http://localhost:9002/SOAPServiceRPCLit/SoapPort";
            Endpoint.publish(address, implementor);  
            address = "http://localhost:9002/TestRPCWsdl";
            Endpoint.publish(address, new MyService());  
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
   
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }
    
    @Test
    public void testBasicConnection() throws Exception {
        
        SOAPServiceRPCLit service = new SOAPServiceRPCLit();
        assertNotNull(service);
        
        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        try {
            GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class);
            for (int idx = 0; idx < 1; idx++) {
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull("no response received from service", greeting);
                String exResponse = response1 + idx;
                assertEquals(exResponse, greeting);

                String reply = greeter.sayHi();
                assertNotNull("no response received from service", reply);
                assertEquals(response2, reply);
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    @Test
    public void testComplexType() throws Exception {
        SOAPServiceRPCLit service = new SOAPServiceRPCLit();
        assertNotNull(service);

        GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class);

        MyComplexStruct in = new MyComplexStruct(); 
        in.setElem1("elem1");
        in.setElem2("elem2");
        in.setElem3(45);

        try {            
            MyComplexStruct out = greeter.sendReceiveData(in); 
            assertNotNull("no response received from service", out);
            assertEquals(in.getElem1(), out.getElem1());
            assertEquals(in.getElem2(), out.getElem2());
            assertEquals(in.getElem3(), out.getElem3());
        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
        
    }
    
    @Test
    public void testNoElementParts() throws Exception {
        HttpURLConnection httpConnection = 
            getHttpConnection("http://localhost:9002/TestRPCWsdl?wsdl");    
        httpConnection.connect();        
        
        assertEquals(200, httpConnection.getResponseCode());
        assertEquals("OK", httpConnection.getResponseMessage());
        InputStream in = httpConnection.getInputStream();
        assertNotNull(in);
        
        Document doc = XMLUtils.parse(in);
        assertNotNull(doc);
        
        
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("soap", Soap11.SOAP_NAMESPACE);
        ns.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        ns.put("xs", "http://www.w3.org/2001/XMLSchema");
        
        
        XPathUtils xu = new XPathUtils(ns);
        
        //make sure the wrapper types are anonymous types
        NodeList ct = (NodeList) xu.getValue("//wsdl:definitions/wsdl:message/wsdl:part[@element != '']",
                                             doc, XPathConstants.NODESET);
        assertNotNull(ct);
        assertEquals(0, ct.getLength());
        
        ct = (NodeList) xu.getValue("//wsdl:definitions/wsdl:message/wsdl:part[@type != '']",
                                     doc, XPathConstants.NODESET);
        assertEquals(4, ct.getLength());
    }
    
    @WebService(serviceName = "MyObjectService", portName = "MyObjectServicePort")
    @SOAPBinding(use = Use.LITERAL, style = Style.RPC)
    public static class MyService {
            
        @WebMethod
        public MyObject getMyObject(@WebParam(name = "longField1") long longField1) {
            return generateMyObject();
        }

        @WebMethod
        public int updateMyObject(@WebParam(name = "myObject") MyObject myObject) {
            return 3;
        }
        private static MyObject generateMyObject() {
            MyObject myObject = new MyObject();
            
            long tempLong = 1;
            myObject.setStringField1("S:" + tempLong++);
            myObject.setStringField2("S:" + tempLong++);
            myObject.setLongField1(tempLong++);
            myObject.setLongField2(tempLong++);
            return myObject;
        }
    }
    
    @XmlType(name = "MyObject")
    @XmlAccessorType(XmlAccessType.PROPERTY)
    @XmlRootElement(name = "MyObject")
    public static class MyObject {
        private String stringField1;
        private String stringField2;
        private long longField1;
        private long longField2;
        
        public String getStringField1() {
            return stringField1;
        }
        public void setStringField1(String stringField1) {
            this.stringField1 = stringField1;
        }
        public String getStringField2() {
            return stringField2;
        }
        public void setStringField2(String stringField2) {
            this.stringField2 = stringField2;
        }
        public long getLongField1() {
            return longField1;
        }
        public void setLongField1(long longField1) {
            this.longField1 = longField1;
        }
        public long getLongField2() {
            return longField2;
        }
        public void setLongField2(long longField2) {
            this.longField2 = longField2;
        }
    }
}
