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

package demo.stream.client;

import java.net.URL;
import javax.xml.namespace.QName;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;

import demo.stream.interceptor.StreamInterceptor;


public final class Client {

    private static final QName SERVICE_NAME
        = new QName("http://apache.org/hello_world_soap_http", "SOAPService");


    private Client() {
    }

    public static void main(String[] args) throws Exception {

        URL wsdlURL = Client.class.getResource("/wsdl/hello_world.wsdl");
        SOAPService ss = new SOAPService(wsdlURL, SERVICE_NAME);
        Greeter port = ss.getSoapPort();

        org.apache.cxf.endpoint.Client client = ClientProxy.getClient(port);
        client.getInInterceptors().add(new StreamInterceptor());
        client.getInFaultInterceptors().add(new StreamInterceptor());

        System.out.println("Invoking sayHi...");
        String resp = port.sayHi();
        System.out.println("Server responded with: " + resp);
        System.out.println();

        System.exit(0);
    }

}
