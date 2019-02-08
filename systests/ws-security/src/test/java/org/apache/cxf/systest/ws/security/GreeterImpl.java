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

package org.apache.cxf.systest.ws.security;

import java.security.Principal;

import javax.xml.ws.WebServiceContext;


@javax.jws.WebService(
    serviceName = "SOAPServiceWSSecurity",
    portName = "TimestampSignEncrypt",
    endpointInterface = "org.apache.hello_world_soap_http.Greeter",
    targetNamespace = "http://apache.org/hello_world_soap_http",
    wsdlLocation = "org/apache/cxf/systest/ws/security/hello_world.wsdl"
)
public class GreeterImpl
    extends org.apache.hello_world_soap_http.GreeterImpl {

    private static Principal user;

    public static Principal getUser() {
        return user;
    }

    public String greetMe(String me) {
        WebServiceContext ctx = super.getContext();
        Principal p = ctx.getUserPrincipal();
        if (p != null) {
            user = p;
        }

        //System.out.println("\n\n*** GreetMe called with: " + me + "***\n\n");
        return "Hello " + me;
    }

    public String sayHi() {
        WebServiceContext ctx = super.getContext();
        Principal p = ctx.getUserPrincipal();
        if (p != null) {
            user = p;
        }
        return super.sayHi();
    }

}
