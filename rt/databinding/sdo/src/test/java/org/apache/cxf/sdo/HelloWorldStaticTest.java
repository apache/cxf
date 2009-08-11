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

package org.apache.cxf.sdo;



import javax.jws.WebService;
import javax.xml.namespace.QName;


import org.apache.cxf.frontend.ServerFactoryBean;

import org.junit.Before;

import helloworld.static_types.sdo.Structure;

/**
 * 
 */
public class HelloWorldStaticTest extends AbstractHelloWorldTest {


    @Before 
    public void setUp() throws Exception {
        super.setUp();
        createService(Server.class, new Server(), "TestService", null);
    }
    
    
    @WebService(targetNamespace = "http://apache.org/cxf/databinding/sdo/hello_world_soap_http",
                name = "Greeter",
                serviceName = "TestService",
                endpointInterface = "helloworld.static_types.ws.Greeter")
    public static class Server implements helloworld.static_types.ws.Greeter {
        public java.lang.String sayHi() {
            return "Hi!";
        }

        public void pingMe() {
        }

        public void greetMeOneWay(String s) {
        }

        public java.lang.String greetMe(String s) {
            return "Hello " + s;            
        }

        public Structure echoStruct(Structure struct) {
            return struct;
        }
    }
    
    
    protected ServerFactoryBean createServiceFactory(Class serviceClass, 
                                                     Object serviceBean, 
                                                     String address, 
                                                     QName name,
                                                     SDODataBinding binding) {
        ServerFactoryBean sf = super.createServiceFactory(serviceClass, serviceBean, address, name, binding);
        sf.setWsdlLocation(HelloWorldStaticTest.class
                               .getResource("/wsdl_sdo/HelloService_static.wsdl").toString());
        sf.setServiceName(new QName("http://apache.org/cxf/databinding/sdo/hello_world_soap_http",
                                    "SOAPService"));
        sf.setEndpointName(new QName("http://apache.org/cxf/databinding/sdo/hello_world_soap_http",
                                     "SoapPort"));
        return sf;
    }
    
}
