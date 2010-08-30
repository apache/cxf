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

package test.client;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

import test.service.HelloWorld;

public final class ClientHTTP {

     public static void main(String args[]) throws Exception {
    	Logger.getLogger("").setLevel(Level.FINE);
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.getOutInterceptors().add(new LoggingInInterceptor());
    	factory.setServiceClass(HelloWorld.class);
    	factory.setAddress("http://localhost:9000/helloWorld");
    	HelloWorld client = (HelloWorld) factory.create();
    	
    	String reply = client.sayHi("HI");
        System.out.println("Server said: " + reply);
        System.exit(0); 
    }

}
