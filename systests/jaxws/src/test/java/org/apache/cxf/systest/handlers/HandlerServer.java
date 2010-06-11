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
package org.apache.cxf.systest.handlers;

import javax.xml.ws.Endpoint;

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

public class HandlerServer extends AbstractBusTestServerBase {

    protected void run() {
        String addNumbersPort = TestUtil.getPortNumber(HandlerServer.class, 1);
        String greeterPort = TestUtil.getPortNumber(HandlerServer.class, 2);
        
        Object implementor = new AddNumbersImpl();
        String address = "http://localhost:"
            + addNumbersPort + "/handlers/AddNumbersService/AddNumbersPort";
        Endpoint.publish(address, implementor);
        
        Object implementor1 = new org.apache.hello_world_xml_http.wrapped.GreeterImpl();
        String address1 = "http://localhost:"
            + greeterPort + "/XMLService/XMLDispatchPort";
        Endpoint.publish(address1, implementor1);
    }

    public static void main(String[] args) {
        try {
            HandlerServer s = new HandlerServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

}
