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

package org.apache.cxf.systest.jaxws.websocket;

import java.util.LinkedList;
import java.util.List;

import jakarta.jws.WebService;
import jakarta.xml.ws.Endpoint;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.hello_world_soap_http.BaseGreeterImpl;
import org.apache.hello_world_soap_http.GreeterImpl;

public class Server extends AbstractBusTestServerBase {
    static final String PORT = allocatePort(Server.class);
    static final String BOGUS_REAL_PORT = allocatePort(Server.class, 2);

    List<Endpoint> eps = new LinkedList<>();

    protected void run() {
        Object implementor;
        String address;

        implementor = new GreeterImpl();
        address = "ws://localhost:" + PORT + "/SoapContext/SoapPort";
        Endpoint ep = Endpoint.publish(address, implementor);
        eps.add(ep);

        //publish port with soap12 binding
        address = "ws://localhost:" + PORT + "/SoapContext/SoapPort";
        EndpointImpl e = (EndpointImpl) Endpoint.create(jakarta.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING,
                                                        new Greeter12Impl());
        e.publish(address);
        eps.add(e);
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
