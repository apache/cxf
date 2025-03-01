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

package org.apache.cxf.performance.https.server;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

public class Server implements Runnable {

    static {
        // set the configuration file
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus("ServerConfig.xml");
        BusFactory.setDefaultBus(bus);
    }

    protected Server(String[] args) throws Exception {
        String host = "localhost";
        String protocol = "https";
        for (int x = 0; x < args.length; x++) {
            if ("-host".equals(args[x])) {
                host = args[x + 1];
                x++;
            } else if ("-protocol".equals(args[x])) {
                protocol = args[x + 1];
                x++;
            }
        }
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(CustomerServiceImpl.class);
        sf.setResourceProvider(CustomerServiceImpl.class,
            new SingletonResourceProvider(new CustomerServiceImpl()));
        sf.setAddress(protocol + "://" + host + ":9000/");
        sf.create();
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(args);
        System.out.println("Server ready...");
        boolean wait = true;

        for (int x = 0; x < args.length; x++) {
            if ("-nowait".equals(args[x])) {
                wait = false;
            }
        }

        server.run();
        if (wait) {
            Thread.sleep(10000000);
        }
    }

    @Override
    public void run() {
        System.out.println("running server");
        System.out.println("READY");
    }
}
