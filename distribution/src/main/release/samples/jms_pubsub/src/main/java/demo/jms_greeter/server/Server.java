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

package demo.jms_greeter.server;

import javax.xml.ws.Endpoint;

public class Server {
    Endpoint ep;

    protected Server() throws Exception {
        System.out.println("Starting Server");
        ep = Endpoint.publish(null, new GreeterJMSImpl());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });
    }
    public void shutdown() {
        if (ep != null) {
            ep.stop();
            ep = null;
        }
    }

    public static void main(String args[]) throws Exception {
        Server s = new Server();
        System.out.println("Server ready...");
        try {
            Thread.sleep(5 * 60 * 1000);
            System.out.println("Server exiting");
        } finally {
            s.shutdown();
        }
    }
}
