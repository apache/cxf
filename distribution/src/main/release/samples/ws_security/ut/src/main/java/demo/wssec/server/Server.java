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

package demo.wssec.server;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;

/**
 * A DOM-based server
 */
public class Server {

    protected Server() throws Exception {
        System.out.println("Starting Server");

        Object implementor = new GreeterImpl();
        String address = "http://localhost:9000/SoapContext/GreeterPort";
        EndpointImpl impl = (EndpointImpl)Endpoint.publish(address, implementor);

        Map<String, Object> outProps = new HashMap<>();
        outProps.put("action", "UsernameToken Timestamp");

        outProps.put("passwordType", "PasswordText");
        outProps.put("user", "Alice");
        outProps.put("passwordCallbackClass", "demo.wssec.server.UTPasswordCallback");

        impl.getOutInterceptors().add(new WSS4JOutInterceptor(outProps));

        Map<String, Object> inProps = new HashMap<>();
        inProps.put("action", "UsernameToken Timestamp");
        inProps.put("passwordType", "PasswordDigest");
        inProps.put("passwordCallbackClass", "demo.wssec.server.UTPasswordCallback");

        impl.getInInterceptors().add(new WSS4JInInterceptor(inProps));
    }

    public static void main(String[] args) throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = Server.class.getResource("/wssec.xml");
        Bus bus = bf.createBus(busFile.toString());

        BusFactory.setDefaultBus(bus);

        new Server();
        System.out.println("Server ready...");

        Thread.sleep(5 * 60 * 1000);

        bus.shutdown(true);
        System.out.println("Server exiting");
        System.exit(0);
    }
}
