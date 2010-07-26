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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.Service;
import javax.xml.ws.http.HTTPException;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.headers.HeaderTester;
import org.apache.headers.XMLHeaderService;
import org.apache.headers.types.InHeader;
import org.apache.headers.types.InHeaderResponse;
import org.apache.headers.types.InoutHeader;
import org.apache.headers.types.InoutHeaderResponse;
import org.apache.headers.types.OutHeader;
import org.apache.headers.types.OutHeaderResponse;
import org.apache.headers.types.SOAPHeaderData;
import org.apache.hello_world_xml_http.bare.Greeter;
import org.apache.hello_world_xml_http.bare.XMLService;
import org.apache.hello_world_xml_http.bare.types.MyComplexStructType;
import org.apache.hello_world_xml_http.mixed.types.SayHi;
import org.apache.hello_world_xml_http.mixed.types.SayHiResponse;
import org.apache.hello_world_xml_http.wrapped.GreeterFaultImpl;
import org.apache.hello_world_xml_http.wrapped.PingMeFault;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientServerXMLTest extends AbstractBusClientServerTestBase {
    static final String REG_PORT = allocatePort(ServerXMLBinding.class);
    static final String WRAP_PORT = allocatePort(ServerXMLBinding.class, 1);
    static final String MIX_PORT = allocatePort(ServerXMLBinding.class, 2);

    private final QName barePortName = new QName("http://apache.org/hello_world_xml_http/bare", "XMLPort");

    private final QName wrapServiceName = new QName("http://apache.org/hello_world_xml_http/wrapped",
            "XMLService");

    private final QName mixedServiceName = new QName("http://apache.org/hello_world_xml_http/mixed",
                                                     "XMLService");

    private final QName wrapPortName = new QName("http://apache.org/hello_world_xml_http/wrapped", "XMLPort");

    private final QName mixedPortName = new QName("http://apache.org/hello_world_xml_http/mixed", "XMLPort");

    private final QName wrapFakePortName = new QName("http://apache.org/hello_world_xml_http/wrapped",
            "FakePort");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(ServerXMLBinding.class));
    }

    @Test
    public void testBareBasicConnection() throws Exception {

        XMLService service = new XMLService();
        assertNotNull(service);

        String response1 = "Hello ";
        String response2 = "Bonjour";
        try {
            Greeter greeter = service.getPort(barePortName, Greeter.class);
            updateAddressPort(greeter, REG_PORT);
            String username = System.getProperty("user.name");
            String reply = greeter.greetMe(username);

            assertNotNull("no response received from service", reply);
            assertEquals(response1 + username, reply);

            reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals(response2, reply);

            MyComplexStructType argument = new MyComplexStructType();
            MyComplexStructType retVal = null;

            String str1 = "this is element 1";
            String str2 = "this is element 2";
            int int1 = 42;
            argument.setElem1(str1);
            argument.setElem2(str2);
            argument.setElem3(int1);
            retVal = greeter.sendReceiveData(argument);

            assertEquals(str1, retVal.getElem1());
            assertEquals(str2, retVal.getElem2());
            assertEquals(int1, retVal.getElem3());

        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
    }

    @Test
    public void testBareGetGreetMe() throws Exception {
        HttpURLConnection httpConnection =
            getHttpConnection("http://localhost:" + REG_PORT 
                              + "/XMLService/XMLPort/greetMe/requestType/cxf");
        httpConnection.connect();

        assertEquals(200, httpConnection.getResponseCode());

        assertEquals("text/xml;charset=utf-8", httpConnection.getContentType().toLowerCase());
        assertEquals("OK", httpConnection.getResponseMessage());

        InputStream in = httpConnection.getInputStream();
        assertNotNull(in);

        Document doc = XMLUtils.parse(in);
        assertNotNull(doc);

        Map<String, String> ns = new HashMap<String, String>();
        ns.put("ns2", "http://apache.org/hello_world_xml_http/bare/types");
        XPathUtils xu = new XPathUtils(ns);
        String response = (String) xu.getValue("//ns2:responseType/text()", doc, XPathConstants.STRING);
        assertEquals("Hello cxf", response);
    }

    @Test
    public void testWrapBasicConnection() throws Exception {

        org.apache.hello_world_xml_http.wrapped.XMLService service =
            new org.apache.hello_world_xml_http.wrapped.XMLService(
                this.getClass().getResource("/wsdl/hello_world_xml_wrapped.wsdl"), wrapServiceName);
        assertNotNull(service);

        String response1 = new String("Hello ");
        String response2 = new String("Bonjour");
        try {
            org.apache.hello_world_xml_http.wrapped.Greeter greeter = service.getPort(wrapPortName,
                    org.apache.hello_world_xml_http.wrapped.Greeter.class);
            updateAddressPort(greeter, WRAP_PORT);

            String username = System.getProperty("user.name");
            String reply = greeter.greetMe(username);

            assertNotNull("no response received from service", reply);
            assertEquals(response1 + username, reply);

            reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals(response2, reply);

            greeter.greetMeOneWay(System.getProperty("user.name"));

        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
    }

    @Test
    public void testMixedConnection() throws Exception {

        org.apache.hello_world_xml_http.mixed.XMLService service =
            new org.apache.hello_world_xml_http.mixed.XMLService(
                this.getClass().getResource("/wsdl/hello_world_xml_mixed.wsdl"), mixedServiceName);
        assertNotNull(service);

        String response1 = new String("Hello ");
        String response2 = new String("Bonjour");
        try {
            org.apache.hello_world_xml_http.mixed.Greeter greeter = service.getPort(mixedPortName,
                    org.apache.hello_world_xml_http.mixed.Greeter.class);
            updateAddressPort(greeter, MIX_PORT);
            String username = System.getProperty("user.name");
            String reply = greeter.greetMe(username);

            assertNotNull("no response received from service", reply);
            assertEquals(response1 + username, reply);
            
            SayHi request = new SayHi();

            SayHiResponse response = greeter.sayHi1(request);
            assertNotNull("no response received from service", response);
            assertEquals(response2, response.getResponseType());

            greeter.greetMeOneWay(System.getProperty("user.name"));

        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
    }

    @Test
    public void testAddPort() throws Exception {
        URL url = getClass().getResource("/wsdl/hello_world_xml_wrapped.wsdl");
        
        Service service = Service.create(url, wrapServiceName);
        assertNotNull(service);

        service.addPort(wrapFakePortName, "http://cxf.apache.org/bindings/xformat",
                "http://localhost:" + WRAP_PORT + "/XMLService/XMLPort");

        String response1 = new String("Hello ");
        String response2 = new String("Bonjour");

        org.apache.hello_world_xml_http.wrapped.Greeter greeter = service.getPort(wrapPortName,
                org.apache.hello_world_xml_http.wrapped.Greeter.class);
        updateAddressPort(greeter, WRAP_PORT);

        try {
            String username = System.getProperty("user.name");
            String reply = greeter.greetMe(username);

            assertNotNull("no response received from service", reply);
            assertEquals(response1 + username, reply);

            reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals(response2, reply);
            
            BindingProvider bp = (BindingProvider) greeter;
            Map<String, Object> responseContext = bp.getResponseContext();
            Integer responseCode = (Integer) responseContext.get(Message.RESPONSE_CODE);
            assertEquals(200, responseCode.intValue());

            greeter.greetMeOneWay(System.getProperty("user.name"));

        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
       
    }

    @Test
    public void testXMLFault() throws Exception {
        org.apache.hello_world_xml_http.wrapped.XMLService service =
            new org.apache.hello_world_xml_http.wrapped.XMLService(
                this.getClass().getResource("/wsdl/hello_world_xml_wrapped.wsdl"), wrapServiceName);
        assertNotNull(service);
        org.apache.hello_world_xml_http.wrapped.Greeter greeter = service.getPort(wrapPortName,
                org.apache.hello_world_xml_http.wrapped.Greeter.class);
        updateAddressPort(greeter, WRAP_PORT);
        try {
            greeter.pingMe();
            fail("did not catch expected PingMeFault exception");
        } catch (PingMeFault ex) {
            assertEquals("minor value", 1, (int)ex.getFaultInfo().getMinor());
            assertEquals("major value", 2, (int)ex.getFaultInfo().getMajor());

            BindingProvider bp = (BindingProvider) greeter;
            Map<String, Object> responseContext = bp.getResponseContext();
            String contentType = (String) responseContext.get(Message.CONTENT_TYPE);
            assertEquals("text/xml;charset=utf-8", contentType.toLowerCase());
            Integer responseCode = (Integer) responseContext.get(Message.RESPONSE_CODE);
            assertEquals(500, responseCode.intValue());
        }

        org.apache.hello_world_xml_http.wrapped.Greeter greeterFault = service.getXMLFaultPort();
        updateAddressPort(greeterFault, REG_PORT);
        try {
            greeterFault.pingMe();
            fail("did not catch expected runtime exception");
        } catch (HTTPException ex) {
            assertTrue("check expected message of exception", ex.getCause().getMessage().indexOf(
                    GreeterFaultImpl.RUNTIME_EXCEPTION_MESSAGE) >= 0);
        }
    }

    @Test
    public void testXMLBindingOfSoapHeaderWSDL() throws Exception {
        XMLHeaderService service = new XMLHeaderService();
        HeaderTester port = service.getXMLPort9000();
        updateAddressPort(port, REG_PORT);
        try {
            verifyInHeader(port);
            verifyInOutHeader(port);
            verifyOutHeader(port);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
    }

    public void verifyInHeader(HeaderTester proxy) throws Exception {
        InHeader me = new InHeader();
        me.setRequestType("InHeaderRequest");
        SOAPHeaderData headerInfo = new SOAPHeaderData();
        headerInfo.setMessage("message");
        headerInfo.setOriginator("originator");
        InHeaderResponse resp = proxy.inHeader(me, headerInfo);
        assertNotNull(resp);
        assertEquals("check returned response type", "requestType=InHeaderRequest"
                    + "\nheaderData.message=message" + "\nheaderData.getOriginator=originator",
                    resp.getResponseType());
    }

    public void verifyInOutHeader(HeaderTester proxy) throws Exception {
        InoutHeader me = new InoutHeader();
        me.setRequestType("InoutHeaderRequest");
        SOAPHeaderData headerInfo = new SOAPHeaderData();
        headerInfo.setMessage("inoutMessage");
        headerInfo.setOriginator("inoutOriginator");
        Holder<SOAPHeaderData> holder = new Holder<SOAPHeaderData>();
        holder.value = headerInfo;
        InoutHeaderResponse resp = proxy.inoutHeader(me, holder);
        assertNotNull(resp);
        assertEquals("check return value",
                     "requestType=InoutHeaderRequest",
                     resp.getResponseType());
        
        assertEquals("check inout value",
                     "message=inoutMessage",
                     holder.value.getMessage());
        assertEquals("check inout value",
                     "orginator=inoutOriginator",
                     holder.value.getOriginator());        
    }

    public void verifyOutHeader(HeaderTester proxy) throws Exception {
        OutHeader me = new OutHeader();
        me.setRequestType("OutHeaderRequest");
        
        Holder<OutHeaderResponse> outHeaderHolder = new Holder<OutHeaderResponse>();
        Holder<SOAPHeaderData> soapHeaderHolder = new Holder<SOAPHeaderData>();
        proxy.outHeader(me, outHeaderHolder, soapHeaderHolder);
        assertNotNull(outHeaderHolder.value);
        assertNotNull(soapHeaderHolder.value);
        assertEquals("check out value",
                     "requestType=OutHeaderRequest",
                     outHeaderHolder.value.getResponseType());
        
        assertEquals("check out value",
                     "message=outMessage",
                     soapHeaderHolder.value.getMessage());

        assertEquals("check out value",
                     "orginator=outOriginator",
                     soapHeaderHolder.value.getOriginator());
        
    }

}
