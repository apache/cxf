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

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Endpoint;

import org.apache.cxf.frontend.WSDLGetUtils;
import org.apache.cxf.greeter_control.GreeterImplNoWsdl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class ServerGreeterNoWsdl extends AbstractBusTestServerBase {
    static final String PORT = allocatePort(ServerGreeterNoWsdl.class);

    protected void run() {
        Object implementor = new GreeterImplNoWsdl();
        String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";
        Endpoint ep = Endpoint.create(implementor);
        Map<String, Object> props = new HashMap<>();
        props.put(WSDLGetUtils.WSDL_CREATE_IMPORTS, Boolean.TRUE);
        ep.setProperties(props);
        ep.publish(address);
    }

    public static void main(String[] args) {
        try {
            ServerGreeterNoWsdl s = new ServerGreeterNoWsdl();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
