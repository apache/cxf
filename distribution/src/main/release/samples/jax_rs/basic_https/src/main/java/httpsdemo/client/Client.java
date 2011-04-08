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

package httpsdemo.client;

import java.io.File;
import javax.ws.rs.core.Response;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.contrib.ssl.AuthSSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import httpsdemo.common.Customer;
import httpsdemo.common.CustomerService;

public final class Client {

    private static final String CLIENT_CONFIG_FILE = "ClientConfig.xml";
    private static final String BASE_SERVICE_URL = 
        "https://localhost:9000/customerservice/customers";
    
    private Client() {
    }

    public static void main(String args[]) throws Exception {
       
        File clientKeystore = new File("certs/clientKeystore.jks");
        File truststore = new File("certs/commonTruststore.jks");

        // Send HTTP GET request to query customer info - using portable HttpClient method
        Protocol authhttps = new Protocol("https",
                new AuthSSLProtocolSocketFactory(clientKeystore.toURI().toURL(), "password",
                truststore.toURI().toURL(), "password"),
                9000);
        Protocol.registerProtocol("https", authhttps);

        System.out.println("Sending HTTPS GET request to query customer info");
        HttpClient httpclient = new HttpClient();
        GetMethod httpget = new GetMethod(BASE_SERVICE_URL + "/123");
        httpget.addRequestHeader("Accept" , "text/xml");
        
        // If Basic Authentication required could use: 
        /*
        String authorizationHeader = "Basic " 
           + org.apache.cxf.common.util.Base64Utility.encode("username:password".getBytes());
        httpget.addRequestHeader("Authorization", authorizationHeader);
        */
        try {
            httpclient.executeMethod(httpget);
            System.out.println(httpget.getResponseBodyAsString());
        } finally {
            httpget.releaseConnection();
        }

        /*
         *  Send HTTP PUT request to update customer info, using CXF WebClient method
         *  Note: if need to use basic authentication, use the WebClient.create(baseAddress,
         *  username,password,configFile) variant, where configFile can be null if you're
         *  not using certificates.
         */
        System.out.println("Sending HTTPS PUT to update customer name");
        WebClient wc = WebClient.create(BASE_SERVICE_URL, CLIENT_CONFIG_FILE);
        Customer customer = new Customer();
        customer.setId(123);
        customer.setName("Mary");
        Response resp = wc.put(customer);

        /*
         *  Send HTTP POST request to add customer, using JAXRSClientProxy
         *  Note: if need to use basic authentication, use the JAXRSClientFactory.create(baseAddress,
         *  username,password,configFile) variant, where configFile can be null if you're
         *  not using certificates.
         */
        System.out.println("\n");
        System.out.println("Sending HTTPS POST request to add customer");
        CustomerService proxy = JAXRSClientFactory.create(BASE_SERVICE_URL, CustomerService.class,
              CLIENT_CONFIG_FILE);
        customer = new Customer();
        customer.setName("Jack");
        resp = wc.post(customer);
        
        System.out.println("\n");
        System.exit(0);
    }
}
