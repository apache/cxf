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

package demo.hw.server;

import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

public class Server {

    protected Server(String wsdl) throws Exception {
        System.out.println("Starting Server");
        String address = "http://localhost:9000/SoapContext/SoapPort";

        System.out.println("Starting Server");
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        //svrFactory.setServiceClass(Greeter.class);
        svrFactory.setWsdlLocation(wsdl);
        svrFactory.setAddress(address);
        svrFactory.setServiceBean(new GreeterImpl());
        svrFactory.create();
    }

    public static void main(String args[]) throws Exception {
        new Server(args[0]);
        System.out.println("Server ready...");

        Thread.sleep(5 * 60 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }
}
