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
package org.apache.cxf.systest.ws.addr_feature;

import java.io.IOException;
import java.util.concurrent.Future;

import javax.jws.WebService;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Response;
import javax.xml.ws.soap.Addressing;

import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.addressing.soap.DecoupledFaultHandler;
import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.NoSuchCodeLitFault;
import org.apache.hello_world_soap_http.types.BareDocumentResponse;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.apache.hello_world_soap_http.types.GreetMeSometimeResponse;
import org.apache.hello_world_soap_http.types.SayHiResponse;
import org.apache.hello_world_soap_http.types.TestDocLitFaultResponse;
import org.apache.hello_world_soap_http.types.TestNillableResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class FaultToEndpointServer extends AbstractBusTestServerBase {  
    static final String FAULT_PORT = allocatePort(FaultToEndpointServer.class);
    static final String PORT = allocatePort(FaultToEndpointServer.class, 1);
   
    EndpointImpl ep;
    private org.eclipse.jetty.server.Server faultToserver;
    protected void run()  { 
        faultToserver = new org.eclipse.jetty.server.Server(Integer.parseInt(FAULT_PORT));
        faultToserver.setHandler(new HelloHandler());
        try {
            faultToserver.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        
        setBus(BusFactory.getDefaultBus());
        Object implementor = new AddNumberImpl();
        String address = "http://localhost:" + PORT + "/jaxws/add";

        ep = (EndpointImpl) Endpoint.create(implementor);
        ep.getInInterceptors().add(new DecoupledFaultHandler());
        ep.getFeatures().add(new WSAddressingFeature());
        ep.publish(address); 
        
        Object implementor2 = new GreeterImpl();
        String address2 = "http://localhost:" + PORT + "/jaxws/greeter";
        ep = (EndpointImpl) Endpoint.create(implementor2);
        ep.getInInterceptors().add(new DecoupledFaultHandler());
        ep.getFeatures().add(new WSAddressingFeature());
        ep.publish(address2);
    }
    
    public void tearDown() throws Exception {
        if (faultToserver != null) {
            faultToserver.stop();
            faultToserver.destroy();
            faultToserver = null;
        }
        
        ep.stop();
        ep = null;        
    }

    public static void main(String[] args) {
        try {
            FaultToEndpointServer server = new FaultToEndpointServer();
            server.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
       
    


    public static class HelloHandler extends AbstractHandler {
        private static String faultRequestPath;

        public void handle(String target, Request baseRequest, HttpServletRequest request,
                           HttpServletResponse response) throws IOException, ServletException {
            response.setContentType("text/html;charset=utf-8");
            faultRequestPath = request.getPathInfo();
            if ("/faultTo".equals(faultRequestPath)) {
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            baseRequest.setHandled(true);
            response.getWriter().println("Received");
        }

        public static String getFaultRequestPath() {
            return faultRequestPath;
        }

    }
    
    @WebService(serviceName = "SOAPServiceAddressing", 
                portName = "SoapPort", 
                endpointInterface = "org.apache.hello_world_soap_http.Greeter", 
                targetNamespace = "http://apache.org/hello_world_soap_http",
                wsdlLocation = "testutils/hello_world.wsdl")
    @Addressing
    public class GreeterImpl implements Greeter {

        public String greetMe(String me) {
            return "Hello " + me;
        }

        public String greetMeLater(long delay) {
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
            return "Hello, finally";
        }

        public void greetMeOneWay(String requestType) {
            throw new RuntimeException("intended error"); 
        }

        public String sayHi() {
            return null;
        }
        
        public void testDocLitFault(String faultType) throws BadRecordLitFault, NoSuchCodeLitFault {
        }

        public BareDocumentResponse testDocLitBare(String in) {
            return null;
        }

        public String greetMeSometime(String me) {
            return null;
        }
        
        public Future<?>  greetMeSometimeAsync(String requestType, 
                                               AsyncHandler<GreetMeSometimeResponse> asyncHandler) { 
            return null; 
        }
        
        public Response<GreetMeSometimeResponse> greetMeSometimeAsync(String requestType) { 
            return null; 
        }
        
        public Response<TestDocLitFaultResponse> testDocLitFaultAsync(String faultType) {  
            return null; 
        }
        
        public Response<BareDocumentResponse> testDocLitBareAsync(String bare) {
            return null;
        }
        
        public Future<?> greetMeAsync(String requestType, AsyncHandler<GreetMeResponse> asyncHandler) { 
            return null; 
        }
        
        public Response<GreetMeResponse> greetMeAsync(String requestType) { 
            return null; 
        }
        
        public Future<?> greetMeLaterAsync(long requestType, AsyncHandler<GreetMeLaterResponse> asyncHandler) { 
            return null; 
        }
        
        public Response<GreetMeLaterResponse> greetMeLaterAsync(long requestType) { 
            return null; 
        }
        
        public Future<?> sayHiAsync(AsyncHandler<SayHiResponse> asyncHandler) { 
            return null; 
        }
        
        public Response<SayHiResponse> sayHiAsync() { 
            return null; 
        }

        public String testNillable(String nillElem, int intElem) {
            return null;
        }

        public Response<TestNillableResponse> testNillableAsync(String nillElem,
                                                                int intElem) {
            return null;
        }
        
        public Future<?> testNillableAsync(String nillElem, 
                                           int intElem,
                                           AsyncHandler<TestNillableResponse> asyncHandler) {
            return null;
        }

        public Future<?> testDocLitFaultAsync(String faultType,
                                              AsyncHandler<TestDocLitFaultResponse> asyncHandler) {
            return null;
        }
        
    }
}   


