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

package org.apache.cxf.systest.soap_udp;

import jakarta.jws.WebService;
import jakarta.xml.ws.Endpoint;
import org.apache.cxf.annotations.GZIP;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.hello_world_soap_http.BaseGreeterImpl;

public class Server extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(Server.class);

    Endpoint ep;
    protected void run() {
        Object implementor = new GreeterImpl();
        String address = "soap.udp://:" + PORT;
        ep = Endpoint.publish(address, implementor);
    }
    @Override
    public void tearDown() {
        if (ep != null) {
            ep.stop();
        }

    }


    public static void main(String[] args) {
        try {
            Server s = new Server();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }


    @WebService(serviceName = "SOAPService",
                portName = "SoapPort",
                endpointInterface = "org.apache.hello_world_soap_http.Greeter",
                targetNamespace = "http://apache.org/hello_world_soap_http",
                wsdlLocation = "testutils/hello_world.wsdl")
    @GZIP(threshold = 50)
    public class GreeterImpl extends BaseGreeterImpl {


    }
}

