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

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jakarta.jws.WebService;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceProvider;
import org.apache.cxf.annotations.UseAsyncMethod;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.ServerAsyncResponse;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.hello_world_soap_http.BaseGreeterImpl;
import org.apache.hello_world_soap_http.DocLitBareGreeterImpl;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.apache.hello_world_soap_http.types.GreetMeResponse;

public class Server extends AbstractBusTestServerBase {
    static final String PORT = allocatePort(Server.class);
    static final String BARE_PORT = allocatePort(Server.class, 1);
    static final String BOGUS_REAL_PORT = allocatePort(Server.class, 2);

    List<Endpoint> eps = new LinkedList<>();

    protected void run() {
        URL url = getClass().getResource("fault-stack-trace.xml");
        if (url != null) {
            System.setProperty("cxf.config.file.url", url.toString());
        }
        Object implementor;
        String address;

        implementor = new AsyncGreeter();
        address = "http://localhost:" + PORT + "/SoapContext/AsyncSoapPort";
        eps.add(Endpoint.publish(address, implementor));

        implementor = new AsyncEchoProvider();
        address = "http://localhost:" + PORT + "/SoapContext/AsyncEchoProvider";
        eps.add(Endpoint.publish(address, implementor));

        implementor = new GreeterImplMultiPort();
        address = "http://localhost:" + PORT + "/MultiPort/GreeterPort";
        eps.add(Endpoint.publish(address, implementor));

        implementor = new DocLitBareGreeterMultiPort();
        address = "http://localhost:" + PORT + "/MultiPort/DocBarePort";
        eps.add(Endpoint.publish(address, implementor));

        implementor = new GreeterImpl();
        address = "http://localhost:" + PORT + "/SoapContext/SoapPort";
        Endpoint ep = Endpoint.publish(address, implementor);
        eps.add(ep);

        implementor = new GreeterImpl();
        address = "http://localhost:" + PORT + "/SoapContext/SoapPortWithGzip";
        Endpoint ep2 = Endpoint.publish(address, implementor);
        ((EndpointImpl)ep2).getService().getInInterceptors().add(new GZIPInInterceptor());
        ((EndpointImpl)ep2).getService().getOutInterceptors().add(new GZIPOutInterceptor());
        eps.add(ep2);

        implementor = new RefGreeterImpl();
        address = "http://localhost:" + PORT + "/SoapContext/SoapPort2";
        eps.add(Endpoint.publish(address, implementor));

        //publish port with soap12 binding
        address = "http://localhost:" + PORT + "/SoapContext/SoapPort";


        EndpointImpl e = (EndpointImpl) Endpoint.create(jakarta.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING,
                                                        new Greeter12Impl());
        e.publish(address);
        eps.add(e);

        implementor = new DocLitBareGreeterImpl();
        address = "http://localhost:" + BARE_PORT + "/SoapContext/SoapPort";
        eps.add(Endpoint.publish(address, implementor));


        implementor = new GreeterImplBogus();
        address = "http://localhost:" + BOGUS_REAL_PORT + "/SoapContext/SoapPort";
        eps.add(Endpoint.publish(address, implementor));
    }

    public void tearDown() {
        while (!eps.isEmpty()) {
            Endpoint ep = eps.remove(0);
            ep.stop();
        }
    }

    @WebService(endpointInterface = "org.apache.hello_world_soap_http.Greeter",
                targetNamespace = "http://apache.org/hello_world_soap_http")
    public class Greeter12Impl extends BaseGreeterImpl {

    }

    @WebService(endpointInterface = "org.apache.hello_world_soap_http.Greeter",
        targetNamespace = "http://apache.org/hello_world_soap_http")
    public class AsyncGreeter extends BaseGreeterImpl {

        @UseAsyncMethod
        public String greetMe(String me) {
            throw new RuntimeException("Should be async");
        }

        public Future<?> greetMeAsync(final String requestType,
                                      final AsyncHandler<GreetMeResponse> asyncHandler) {
            final ServerAsyncResponse<GreetMeResponse> r = new ServerAsyncResponse<>();
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                    GreetMeResponse resp = new GreetMeResponse();
                    resp.setResponseType("Hello " + requestType);
                    r.set(resp);
                    asyncHandler.handleResponse(r);
                }
            } .start();
            return r;
        }
    }

    @WebServiceProvider
    @ServiceMode(value = Service.Mode.PAYLOAD)
    public class AsyncEchoProvider implements Provider<StreamSource> {

        @Override
        @UseAsyncMethod
        public StreamSource invoke(StreamSource request) {
            throw new RuntimeException("Should be async");
        }

        public Future<?> invokeAsync(final StreamSource s, final AsyncHandler<Source> asyncHandler) {
            final ServerAsyncResponse<Source> r = new ServerAsyncResponse<>();
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    r.set(s);
                    asyncHandler.handleResponse(r);
                }
            } .start();
            return r;
        }
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

    @WebService(serviceName = "SOAPService",
                portName = "SoapPort",
                endpointInterface = "org.apache.cxf.hello_world.elrefs.Greeter",
                targetNamespace = "http://apache.org/hello_world_soap_http",
                wsdlLocation = "testutils/hello_world_ref.wsdl")
    public class RefGreeterImpl implements org.apache.cxf.hello_world.elrefs.Greeter {
        public String greetMe(String requestType) {
            return "Hello " + requestType;
        }

        public String sayHi() {
            return "hi";
        }
    }

    @WebService(serviceName = "SOAPServiceBogusAddressTest",
                portName = "SoapPort",
                endpointInterface = "org.apache.hello_world_soap_http.Greeter",
                targetNamespace = "http://apache.org/hello_world_soap_http",
                wsdlLocation = "testutils/hello_world.wsdl")
    public class GreeterImplBogus extends GreeterImpl {

    }

    @WebService(serviceName = "SOAPServiceMultiPortTypeTest",
                portName = "GreeterPort",
                endpointInterface = "org.apache.hello_world_soap_http.Greeter",
                targetNamespace = "http://apache.org/hello_world_soap_http",
                wsdlLocation = "testutils/hello_world.wsdl")
    public class GreeterImplMultiPort extends GreeterImpl {

    }

    @WebService(serviceName = "SOAPServiceMultiPortTypeTest",
            portName = "DocLitBarePort",
            endpointInterface = "org.apache.hello_world_soap_http.DocLitBare",
            targetNamespace = "http://apache.org/hello_world_soap_http",
            wsdlLocation = "testutils/hello_world.wsdl")
    public class DocLitBareGreeterMultiPort extends DocLitBareGreeterImpl {

    }
}
