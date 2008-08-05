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

package demo.ws_rm.server;

import java.net.URL;
import javax.xml.ws.Endpoint;


import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;

public class Server {

    protected Server() throws Exception {
        System.out.println("Starting Server");


        Object implementor = new GreeterImpl();
        String address = "http://localhost:9000/SoapContext/GreeterPort";
        Endpoint e = Endpoint.publish(address, implementor);
    }

    public static void main(String args[]) throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = Server.class.getResource("ws_rm.xml");
        Bus bus = bf.createBus(busFile.toString());
        bf.setDefaultBus(bus);

        new Server();
        System.out.println("Server ready...");

        Thread.sleep(5 * 60 * 1000);

        bus.shutdown(true);
        System.out.println("Server exiting");
        System.exit(0);
    }
}
