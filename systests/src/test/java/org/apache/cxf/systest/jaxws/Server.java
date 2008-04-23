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

import java.net.URL;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.hello_world_soap_http.DocLitBareGreeterImpl;
import org.apache.hello_world_soap_http.GreeterImpl;

public class Server extends AbstractBusTestServerBase {

    protected void run() {
        URL url = getClass().getResource("fault-stack-trace.xml");
        if (url != null) {
            System.setProperty("cxf.config.file.url", url.toString());
        }
        Object implementor;
        String address;
        
        implementor = new GreeterImplMultiPort();
        address = "http://localhost:9020/MultiPort/GreeterPort";
        Endpoint.publish(address, implementor);

        implementor = new DocLitBareGreeterMultiPort();
        address = "http://localhost:9021/MultiPort/DocBarePort";
        Endpoint.publish(address, implementor);
        
        
        implementor = new GreeterImpl();
        address = "http://localhost:9000/SoapContext/SoapPort";
        Endpoint.publish(address, implementor);
        
        //publish port with soap12 binding
        address = "http://localhost:9009/SoapContext/SoapPort";
        EndpointImpl e = (EndpointImpl) Endpoint.create(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING, 
                                                        implementor);
        e.publish(address);
        
        implementor = new DocLitBareGreeterImpl();
        address = "http://localhost:7600/SoapContext/SoapPort";
        Endpoint.publish(address, implementor);
        
        
        implementor = new GreeterImplBogus();
        address = "http://localhost:9015/SoapContext/SoapPort";
        Endpoint.publish(address, implementor);
        

    }

    public static void main(String[] args) {
        try {
            Server s = new Server();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
    
    
    @WebService(serviceName = "SOAPServiceBogusAddressTest",
                portName = "SoapPort",
                endpointInterface = "org.apache.hello_world_soap_http.Greeter",
                targetNamespace = "http://apache.org/hello_world_soap_http",
                wsdlLocation = "testutils/hello_world.wsdl")
    public class GreeterImplBogus extends GreeterImpl {
    
    } 
    
    @WebService(serviceName = "SOAPServiceMultiPortTypeTest", 
                portName = "GreeterPort", 
                endpointInterface = "org.apache.hello_world_soap_http.Greeter",
                targetNamespace = "http://apache.org/hello_world_soap_http",
                wsdlLocation = "testutils/hello_world.wsdl") 
    public class GreeterImplMultiPort extends GreeterImpl {
    
    } 
    
    @WebService(serviceName = "SOAPServiceMultiPortTypeTest", 
            portName = "DocLitBarePort", 
            endpointInterface = "org.apache.hello_world_soap_http.DocLitBare",
            targetNamespace = "http://apache.org/hello_world_soap_http",
            wsdlLocation = "testutils/hello_world.wsdl")
    public class DocLitBareGreeterMultiPort extends DocLitBareGreeterImpl {
    
    }  
}
