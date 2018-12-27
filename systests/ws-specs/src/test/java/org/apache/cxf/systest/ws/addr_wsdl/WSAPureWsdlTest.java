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

package org.apache.cxf.systest.ws.addr_wsdl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Response;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.systest.ws.AbstractWSATestBase;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersPortType;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersResponse;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersService;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.soap.MAPCodec;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WSAPureWsdlTest extends AbstractWSATestBase {
    static final String PORT = allocatePort(Server.class);
    static final String PORT2 = allocatePort(Server.class, 1);

    private final QName serviceName = new QName("http://apache.org/cxf/systest/ws/addr_feature/",
                                                "AddNumbersService");

    @Before
    public void setUp() throws Exception {
        createBus();
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testBasicInvocationTimeouts() throws Exception {
        AddNumbersPortType port = getPort();

        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                        "http://localhost:" + PORT + "/jaxws/add");
        HTTPConduit conduit = (HTTPConduit)((Client)port).getConduit();
        conduit.getClient().setConnectionTimeout(25);
        conduit.getClient().setReceiveTimeout(10);

        try {
            //read timeout
            port.addNumbersAsync(5092, 25).get();
            fail("should have failed");
        } catch (Exception t) {
            //expected
            assertTrue(t.getCause().toString(), t.getCause() instanceof java.net.SocketTimeoutException);
        }

        AsyncHandler<AddNumbersResponse> handler = new AsyncHandler<AddNumbersResponse>() {
            public void handleResponse(Response<AddNumbersResponse> res) {
                //System.out.println("in handle response");
                synchronized (this) {
                    notifyAll();
                }
            }
        };
        synchronized (handler) {
            port.addNumbersAsync(5092,  25, handler);
            handler.wait(1000);
        }

        try {
            //connection timeout
            ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                            "http://localhost:" + PORT2 + "/jaxws/add");
            port.addNumbersAsync(25, 25).get();
            fail("should have failed");
        } catch (Exception t) {
            //expected
            assertTrue(t.getCause().getCause().toString(),
                       t.getCause().getCause() instanceof java.net.ConnectException
                       ||  t.getCause().getCause() instanceof java.net.SocketTimeoutException);
        }
        synchronized (handler) {
            port.addNumbersAsync(25,  25, handler);
            handler.wait(1000);
        }
        MAPCodec mp = getMAPCodec((Client)port);
        assertEquals(0, mp.getUncorrelatedExchanges().size());
    }

    MAPCodec getMAPCodec(Client port) {
        for (Interceptor<? extends Message> f : port.getOutInterceptors()) {
            if (f instanceof MAPCodec) {
                return (MAPCodec)f;
            }
        }
        return null;
    }

    @Test
    public void testBasicInvocation() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        Response<AddNumbersResponse> resp;
        AddNumbersPortType port = getPort();

        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                        "http://localhost:" + PORT + "/jaxws/add");

        assertEquals(3, port.addNumbers(1, 2));

        String base = "http://apache.org/cxf/systest/ws/addr_feature/AddNumbersPortType/";

        assertLogContains(output.toString(), "//wsa:Action", base + "addNumbersRequest");
        assertLogContains(input.toString(), "//wsa:Action", base + "addNumbersResponse");

        resp = port.addNumbers3Async(1, 2);
        assertEquals(3, resp.get().getReturn());

        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                        "http://localhost:" + PORT + "/doesntexist");
        resp = port.addNumbers3Async(1, 2);
        try {
            resp.get();
        } catch (ExecutionException ex) {
            assertTrue("Found " + ex.getCause().getClass(), ex.getCause() instanceof IOException);
            Client c = ClientProxy.getClient(port);
            for (Interceptor<? extends Message> m : c.getOutInterceptors()) {
                if (m instanceof MAPCodec) {
                    assertTrue(((MAPCodec)m).getUncorrelatedExchanges().isEmpty());
                }
            }
        }
    }
    @Test
    public void testProviderEndpoint() throws Exception {
        String base = "http://apache.org/cxf/systest/ws/addr_feature/AddNumbersPortType/";

        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getPort();
        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                        "http://localhost:" + PORT + "/jaxws/add-provider");
        assertEquals(3, port.addNumbers(1, 2));


        assertLogContains(output.toString(), "//wsa:Action", base + "addNumbersRequest");
        assertLogContains(input.toString(), "//wsa:Action", base + "addNumbersResponse");

        output.reset();
        input.reset();

        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
            "http://localhost:" + PORT + "/jaxws/add-providernows");
        assertEquals(3, port.addNumbers(1, 2));

        assertLogContains(output.toString(), "//wsa:Action", base + "addNumbersRequest");
        assertLogContains(input.toString(), "//wsa:Action", base + "addNumbersResponse");
    }

    @Test
    public void testBasicDispatchInvocation() throws Exception {
        String req = "<addNumbers xmlns=\"http://apache.org/cxf/systest/ws/addr_feature/\">"
            + "<number1>1</number1><number2>2</number2></addNumbers>";
        String base = "http://apache.org/cxf/systest/ws/addr_feature/AddNumbersPortType/";

        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        URL wsdl = getClass().getResource("/wsdl_systest_wsspec/add_numbers.wsdl");
        assertNotNull("WSDL is null", wsdl);
        AddNumbersService service = new AddNumbersService(wsdl, serviceName);


        Dispatch<Source> disp = service.createDispatch(AddNumbersService.AddNumbersPort,
                                                       Source.class, Mode.PAYLOAD);

        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:" + PORT + "/jaxws/add");

        disp.invoke(new StreamSource(new StringReader(req)));


        assertLogContains(output.toString(), "//wsa:Action", base + "addNumbersRequest");
        assertLogContains(input.toString(), "//wsa:Action", base + "addNumbersResponse");


        output.reset();
        input.reset();

        disp = service.createDispatch(AddNumbersService.AddNumbersPort,
                                      Source.class, Mode.PAYLOAD);

        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:" + PORT + "/jaxws/add");

        //set the operation name so action can be pulled from the wsdl
        disp.getRequestContext().put(MessageContext.WSDL_OPERATION,
                                     new QName("http://apache.org/cxf/systest/ws/addr_feature/",
                                               "addNumbers"));

        disp.invoke(new StreamSource(new StringReader(req)));

        assertLogContains(output.toString(), "//wsa:Action", base + "addNumbersRequest");
        assertLogContains(input.toString(), "//wsa:Action", base + "addNumbersResponse");
    }

    @Test
    public void testDispatchActionMissmatch() throws Exception {
        String req = "<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                    + "<S:Body><addNumbers3 xmlns=\"http://apache.org/cxf/systest/ws/addr_feature/\">"
                    + "<number1>1</number1><number2>2</number2></addNumbers3>"
                    + "</S:Body></S:Envelope>";
        //String base = "http://apache.org/cxf/systest/ws/addr_feature/AddNumbersPortType/";
        String expectedOut = "http://bad.action";

        URL wsdl = getClass().getResource("/wsdl_systest_wsspec/add_numbers.wsdl");
        assertNotNull("WSDL is null", wsdl);
        AddNumbersService service = new AddNumbersService(wsdl, serviceName);


        Dispatch<Source> disp = service.createDispatch(AddNumbersService.AddNumbersPort,
                                                       Source.class, Mode.MESSAGE);

        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                     "http://localhost:" + PORT + "/jaxws/add");

        //manually set the action
        disp.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY,
                                     expectedOut);
        disp.getRequestContext().put(ContextUtils.ACTION,
                                     expectedOut + "/wsaAction");
        try {
            disp.invoke(new StreamSource(new StringReader(req)));
            fail("no exception");
        } catch (SOAPFaultException f) {
            //expected
        }
    }

    private AddNumbersPortType getPort() {
        URL wsdl = getClass().getResource("/wsdl_systest_wsspec/add_numbers.wsdl");
        assertNotNull("WSDL is null", wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull("Service is null ", service);
        return service.getAddNumbersPort();
    }
}