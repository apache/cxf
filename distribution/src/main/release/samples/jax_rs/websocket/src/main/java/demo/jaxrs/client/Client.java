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

package demo.jaxrs.client;

import java.io.InputStream;
import java.util.List;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;

import demo.jaxrs.server.Server;

public final class Client {

    private Client() {
    }

    public static void main(String args[]) throws Exception {

        // Create a websocket client and connect to the target service
        WebSocketTestClient client = new WebSocketTestClient(Server.HOST_URL + Server.CONTEXT_PATH);
        client.connect();

        // Sent GET request to query customer info
        System.out.println("Sent GET request to query customer info");
        client.sendTextMessage("GET " + Server.CONTEXT_PATH + "/customerservice/customers/123");
        client.await(5);
        List<WebSocketTestClient.Response> responses = client.getReceivedResponses();
        System.out.println(responses.get(0));

        // Sent GET request to query sub resource product info
        client.reset(1);
        System.out.println("Sent GET request to query sub resource product info");
        client.sendTextMessage("GET " + Server.CONTEXT_PATH + "/customerservice/orders/223/products/323");
        client.await(5);
        responses = client.getReceivedResponses();
        System.out.println(responses.get(0));

        // Sent PUT request to update customer info
        client.reset(1);
        System.out.println("Sent PUT request to update customer info");
        String inputData = getStringFromInputStream(Client.class.getResourceAsStream("/update_customer.xml"));
        client.sendTextMessage("PUT " + Server.CONTEXT_PATH + "/customerservice/customers\r\n"
                               + "Content-Type: text/xml; charset=ISO-8859-1\r\n\r\n"
                               + inputData);
        client.await(5);
        responses = client.getReceivedResponses();
        System.out.println(responses.get(0));
        
        // Sent POST request to add customer
        client.reset(1);
        System.out.println("Sent POST request to add customer");
        inputData = getStringFromInputStream(Client.class.getResourceAsStream("/add_customer.xml"));
        client.sendTextMessage("POST " + Server.CONTEXT_PATH + "/customerservice/customers\r\nContent-Type: text/xml; "
                               + "charset=ISO-8859-1\r\nAccept: text/xml\r\n\r\n" + inputData);
        client.await(5);
        responses = client.getReceivedResponses();
        System.out.println(responses.get(0));

        // Create another websocket client and connect to the target service
        WebSocketTestClient client2 = new WebSocketTestClient(Server.HOST_URL + Server.CONTEXT_PATH);
        client2.connect();

        // Sent GET request to monitor the customer activities
        client2.reset(1);
        System.out.println("Sent GET request to monitor activities");
        client2.sendTextMessage("GET " + Server.CONTEXT_PATH + "/customerservice/monitor");
        client2.await(5);
        responses = client2.getReceivedResponses();
        System.out.println(responses.get(0));
        
        // one retrieval, one delete
        client2.reset(2);
        client.reset(2);
        client.sendTextMessage("GET " + Server.CONTEXT_PATH + "/customerservice/customers/123");
        client.sendTextMessage("DELETE " + Server.CONTEXT_PATH + "/customerservice/customers/235");

        client2.await(5);
        responses = client2.getReceivedResponses();
        for (Object o : responses) {
            System.out.println(o);    
        }
        
        client.close();
        client2.close();
        System.exit(0);
    }

    private static String getStringFromInputStream(InputStream in) throws Exception {
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        bos.close();
        return bos.getOut().toString();
    }

}
