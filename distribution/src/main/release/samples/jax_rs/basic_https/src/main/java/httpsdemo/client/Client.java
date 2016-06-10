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

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import httpsdemo.common.Customer;
import httpsdemo.common.CustomerService;

public final class Client {

    private static final String CLIENT_CONFIG_FILE = "ClientConfig.xml";
    private static final String BASE_SERVICE_URL = 
        "https://localhost:9000/customerservice/customers";
    
    private Client() {
    }

    public static void main(String args[]) throws Exception {       
        String keyStoreLoc = "src/main/config/clientKeystore.jks";

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStoreLoc), "cspass".toCharArray());

        /* 
         * Send HTTP GET request to query customer info using portable HttpClient
         * object from Apache HttpComponents
         */
        SSLSocketFactory sf = new SSLSocketFactory(keyStore, "ckpass", keyStore); 
        Scheme httpsScheme = new Scheme("https", 9000, sf);

        System.out.println("Sending HTTPS GET request to query customer info");
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getConnectionManager().getSchemeRegistry().register(httpsScheme);
        HttpGet httpget = new HttpGet(BASE_SERVICE_URL + "/123");
        BasicHeader bh = new BasicHeader("Accept", "text/xml");
        httpget.addHeader(bh);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        entity.writeTo(System.out);
        httpclient.getConnectionManager().shutdown();
        
        /*
         *  Send HTTP PUT request to update customer info, using CXF WebClient method
         *  Note: if need to use basic authentication, use the WebClient.create(baseAddress,
         *  username,password,configFile) variant, where configFile can be null if you're
         *  not using certificates.
         */
        System.out.println("\n\nSending HTTPS PUT to update customer name");
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
        System.out.println("\n\nSending HTTPS POST request to add customer");
        CustomerService proxy = JAXRSClientFactory.create(BASE_SERVICE_URL, CustomerService.class,
              CLIENT_CONFIG_FILE);
        customer = new Customer();
        customer.setName("Jack");
        resp = wc.post(customer);
        
        System.out.println("\n");
        System.exit(0);
    }
}
