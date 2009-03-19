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
package org.apache.cxf.performance.complex_type.server;

import javax.xml.ws.Endpoint;

import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;



public class Server implements Runnable {

    
    public Server(String address) throws Exception {
        System.out.println("Starting Server");
        Object implementor = new ServerImpl();
        Endpoint.publish(address, implementor);
        System.out.println("Server published " + address);
    }
    
    public static void main(String args[]) throws Exception {
        String host = "localhost";
        String protocol = "http";
        String cfg = null;
        boolean wait = true;
        for (int x = 0; x < args.length; x++) {
            if ("-host".equals(args[x])) {
                host = args[x + 1];
                x++;
            } else if ("-protocol".equals(args[x])) {
                protocol = args[x + 1];
                x++;
            } else if ("-BUScfg".equals(args[x])) {
                cfg = args[x + 1];
                x++;
            } else if ("-nowait".equals(args[x])) {
                wait = false;
            }
        }
        if (cfg == null || "none".equals(cfg)) {
            BusFactory.getDefaultBus();
        } else {
            BusFactory.setDefaultBus(new SpringBusFactory().createBus(cfg));
        }
    
        Server server = new Server(protocol + "://" + host 
                                   + ":8080/cxf-benchmark-soapdoclit/services/SoapHttpDocLitPort");
        server.run();
        if (wait) {
            Thread.sleep(10000000);
        }
    }
    
    public void run() {
        System.out.println("running server");
        System.out.println("READY");
    }
    
    void shutdown(boolean wait) {
        System.out.println("shutting down server");
    }

    
}
