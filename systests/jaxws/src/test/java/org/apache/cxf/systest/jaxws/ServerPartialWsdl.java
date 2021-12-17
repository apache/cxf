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

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class ServerPartialWsdl extends AbstractBusTestServerBase {
    static final String PORT = allocatePort(ServerPartialWsdl.class);

    protected void run() {
        Object implementor1 = new AddNumbersImplPartial1();
        String address1 = "http://localhost:"
            + PORT + "/AddNumbersImplPartial1Service";
        Endpoint.publish(address1, implementor1);
        Object implementor2 = new AddNumbersImplPartial2();
        String address2 = "http://localhost:"
            + PORT + "/AddNumbersImplPartial2Service";
        Endpoint.publish(address2, implementor2);
    }

    public static void main(String[] args) {
        try {
            ServerPartialWsdl s = new ServerPartialWsdl();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
