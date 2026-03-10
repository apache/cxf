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

import jakarta.jws.WebService;
import jakarta.xml.ws.Endpoint;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class ClientServerExceptionServer extends AbstractBusTestServerBase {
    static final String PORT = allocatePort(ClientServerExceptionServer.class);

    @WebService(endpointInterface = "org.apache.cxf.systest.jaxws.ExceptionService",
        serviceName = "ExceptionService", targetNamespace = "http://cxf.apache.org/")
    static class ExceptionServiceImpl implements ExceptionService {
        @Override
        public String saySomething(String text) throws IllegalArgumentException {
            throw new IllegalArgumentException("Simulated!");
        }

        @Override
        public String sayNothing(String text) throws SayException {
            throw new SayException("Simulated!", 100);
        }
    }

    protected void run() {
        Object implementor = new ExceptionServiceImpl();
        String address = "http://localhost:" + PORT + "/ExceptionService";
        Endpoint.publish(address, implementor);
    }

    public static void main(String[] args) {
        try {
            ClientServerExceptionServer s = new ClientServerExceptionServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
