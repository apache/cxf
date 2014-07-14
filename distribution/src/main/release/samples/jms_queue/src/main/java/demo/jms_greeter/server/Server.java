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

import java.util.Collections;

import javax.jms.ConnectionFactory;
import javax.xml.ws.Endpoint;

import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.jms.ConnectionFactoryFeature;

public class Server {
    Endpoint ep;

    protected Server() throws Exception {
        System.out.println("Starting Server");
        Object implementor = new GreeterJMSImpl();
        ep = Endpoint.publish(null, implementor);
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });
    }

    /**
     * If you prefer to define the ConnectionFactory directly instead of using a JNDI look.
    // You can inject is like this:
     * @param impl
     * @param cf
     */
    protected void publishEndpoint(Object impl, ConnectionFactory cf) {
        EndpointImpl epi = (EndpointImpl)Endpoint.create(impl);
        epi.setFeatures(Collections.singletonList(new ConnectionFactoryFeature(cf)));
        epi.publish();
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
