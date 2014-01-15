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

import java.io.File;
import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;

public class Server {

    protected Server(URL busURL) throws Exception {
        System.out.println("Starting Server");

        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus(busURL.toString());
        BusFactory.setDefaultBus(bus);

        //Object implementor = new GreeterImpl();
        //String address = "https://localhost:9001/SoapContext/SoapPort";
        //Endpoint.publish(address, implementor);
    }

    public static void main(String args[]) throws Exception {

        if (args.length == 0) {
            System.out.println("please specify configuration file");
            System.exit(1);
        }

        System.out.println("The server's security configuration will be taken "
                           + "from server.xml using the bean name : "
                           + "\"{http://apache.org/hello_world_soap_http}"
                           + "GreeterImplPort.http-destination\".");
        System.out.println();

        URL busURL;
        File busFile = new File(args[0]);
        if (busFile.exists()) {
            busURL = busFile.toURI().toURL();
        } else {
            busURL = new URL(args[0]);
        }

        new Server(busURL);
        System.out.println("Server ready...");

        Thread.sleep(5 * 60 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }
}
