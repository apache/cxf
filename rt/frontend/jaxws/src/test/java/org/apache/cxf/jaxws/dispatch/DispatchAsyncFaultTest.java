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

package org.apache.cxf.jaxws.dispatch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Response;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.AddressingFeature;
import jakarta.xml.ws.soap.SOAPBinding;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.jaxws.AbstractJaxWsTest;
import org.apache.cxf.jaxws.MessageReplayObserver;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.Destination;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * CXF-6301: a SOAP 1.2 fault carrying WS-Addressing headers but no wsa:RelatesTo must be
 * delivered to an AsyncHandler, as it already is for a synchronous invocation.
 */
public class DispatchAsyncFaultTest extends AbstractJaxWsTest {
    private static final QName SERVICE_NAME = new QName("http://cxf.apache.org/test/dispatch", "DispatchTest");

    private static final QName PORT_NAME = new QName("http://cxf.apache.org/test/dispatch", "DispatchPort");

    private static final String ADDRESS = "http://localhost:9121/SoapContext/DispatchPort";

    private static final String REQ_RESOURCE = "/org/apache/cxf/jaxws/dispatch/OperationRequest.xml";

    private static final String FAULT_RESOURCE = "/org/apache/cxf/jaxws/dispatch/Soap12FaultNoRelatesTo.xml";

    private Destination d;

    @Before
    public void setUp() throws Exception {
        EndpointInfo ei = new EndpointInfo(null, "http://schemas.xmlsoap.org/soap/http");
        ei.setAddress(ADDRESS);

        d = localTransport.getDestination(ei, bus);
        d.setMessageObserver(new MessageReplayObserver(FAULT_RESOURCE));
    }

    @Test
    public void testSyncFaultWithoutRelatesTo() throws Exception {
        Dispatch<Source> disp = createDispatch();
        try {
            disp.invoke(createRequest());
            fail("Expected SOAPFaultException");
        } catch (SOAPFaultException ex) {
            assertEquals("NotSupported", ex.getMessage());
        }
    }

    @Test
    public void testAsyncHandlerFaultWithoutRelatesTo() throws Exception {
        Dispatch<Source> disp = createDispatch();
        CompletableFuture<Response<Source>> handled = new CompletableFuture<>();
        disp.invokeAsync(createRequest(), handled::complete);

        Response<Source> response = handled.get(30, TimeUnit.SECONDS);
        try {
            response.get();
            fail("Expected ExecutionException");
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof SoapFault);
            assertEquals("NotSupported", ex.getCause().getMessage());
        }
    }

    @Test
    public void testAsyncResponseFaultWithoutRelatesTo() throws Exception {
        Dispatch<Source> disp = createDispatch();
        Response<Source> response = disp.invokeAsync(createRequest());
        try {
            response.get(30, TimeUnit.SECONDS);
            fail("Expected ExecutionException");
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof SOAPFaultException);
            assertEquals("NotSupported", ex.getCause().getMessage());
        }
    }

    private Dispatch<Source> createDispatch() {
        ServiceImpl service = new ServiceImpl(getBus(), null, SERVICE_NAME, null, new AddressingFeature());
        service.addPort(PORT_NAME, SOAPBinding.SOAP12HTTP_BINDING, ADDRESS);
        return service.createDispatch(PORT_NAME, Source.class, Service.Mode.PAYLOAD);
    }

    private Source createRequest() throws Exception {
        Document doc = StaxUtils.read(getResourceAsStream(REQ_RESOURCE));
        return new DOMSource(doc);
    }
}
