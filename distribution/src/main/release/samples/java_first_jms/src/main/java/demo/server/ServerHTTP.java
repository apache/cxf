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

package demo.server;

import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import demo.service.HelloWorld;
import demo.service.impl.HelloWorldImpl;



public final class ServerHTTP {
    private ServerHTTP() {
        //
    }

    public static void main(String args[]) throws Exception {
        System.out.println("Starting Server");
        Object implementor = new HelloWorldImpl();
        // Create our Server
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(HelloWorld.class);
        svrFactory.setAddress("http://localhost:9000/helloWorld");
        svrFactory.setServiceBean(implementor);
        svrFactory.create();

        System.in.read();
        System.exit(0);
    }
}
