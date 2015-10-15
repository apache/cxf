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
package org.apache.cxf.systest.schemaimport;

import javax.xml.ws.Endpoint;

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(Server.class);

    public Server() {
    }

    protected void run() {
        Object implementor = new SayHiImpl();
        String address = "http://localhost:" + PORT + "/schemaimport/sayHi";
        Endpoint.publish(address, implementor);
        Object implementor2 = new TestEndpointImpl();
        String address2 = "http://localhost:" + PORT + "/schemaimport/service";
        Endpoint.publish(address2, implementor2);
        Object implementor3 = new ServiceImpl();
        String address3 = "http://localhost:" + PORT + "/schemainclude/service";
        Endpoint.publish(address3, implementor3);
        Object implementor4 = new SayHiImpl2();
        String address4 = "http://localhost:" + PORT + "/schemaimport/sayHi2";
        Endpoint.publish(address4, implementor4);
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
}
