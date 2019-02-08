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

package org.apache.cxf.systest.ws.addr_fromjava.server;

import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.addressing.WSAddressingFeature;

public class Server extends AbstractBusTestServerBase {
    static final String PORT = allocatePort(Server.class);

    EndpointImpl ep1;
    EndpointImpl ep2;

    protected void run() {
        setBus(BusFactory.getDefaultBus());
        Object implementor = new AddNumberImpl();
        String address = "http://localhost:" + PORT + "/AddNumberImplPort";
        ep1 = new EndpointImpl(implementor);
        ep1.getFeatures().add(new WSAddressingFeature());
        ep1.publish(address);

        ep2 = new EndpointImpl(new AddNumberImplNoAddr());
        ep2.publish(address + "-noaddr");
    }
    public void tearDown() {
        ep1.stop();
        ep2.stop();
        ep1 = null;
        ep2 = null;
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