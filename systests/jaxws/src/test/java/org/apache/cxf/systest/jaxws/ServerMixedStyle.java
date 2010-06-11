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

package org.apache.cxf.systest.jaxws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Endpoint;

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.hello_world_mixedstyle.GreeterImplMixedStyle;


public class ServerMixedStyle extends AbstractBusTestServerBase {
    static final String PORT = allocatePort(ServerMixedStyle.class);

    protected void run() {
        Object implementor = new GreeterImplMixedStyle();
        String address = "http://localhost:" + PORT + "/SoapContext/SoapPort";
        Endpoint.publish(address, implementor);
        
        Endpoint.publish("http://localhost:" + PORT + "/cxf885", new MixedTestImpl());
    }

    public static void main(String[] args) {
        try {
            ServerMixedStyle s = new ServerMixedStyle();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
    
    @WebService(targetNamespace = "http://example.com") 
    public static interface MixedTest { 
        @WebMethod(operationName = "Simple") 
        @WebResult(name = "SimpleResponse", targetNamespace = "http://example.com") 
        @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE) 
        String simple(@WebParam(name = "Simple") String req);
        
        @WebMethod(operationName = "Hello") 
        @WebResult(name = "Result") 
        @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.WRAPPED) 
        String hello(@WebParam(name = "A") String a,
                     @WebParam(name = "B") String b);
        
        @WebMethod(operationName = "Simple2") 
        @WebResult(name = "Simple2Response", targetNamespace = "http://example.com") 
        @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE) 
        String simple2(@WebParam(name = "Simple2") int a);
        
        @WebMethod(operationName = "Tripple") 
        @WebResult(name = "Result") 
        @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.WRAPPED) 
        String tripple(@WebParam(name = "A") String a,
                     @WebParam(name = "B") String b,
                     @WebParam(name = "C") String c);
    } 
    @WebService(targetNamespace = "http://example.com")
    public class MixedTestImpl implements MixedTest {

        public String hello(String a, String b) {
            return "Hello " + a + " and " + b; 
        }

        public String simple(String req) {
            return "Hello " + req;
        }

        public String simple2(int a) {
            return "Int: " + a;
        }

        public String tripple(String a, String b, String c) {
            return "Tripple: " + a + " " + b + " " + c;
        }
    }
}
