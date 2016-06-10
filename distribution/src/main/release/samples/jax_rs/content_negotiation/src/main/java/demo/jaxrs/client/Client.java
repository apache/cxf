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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

public final class Client {

    private Client() {
    }

    public static void main(String args[]) throws Exception {
        // Sent HTTP GET request to query customer info, expect XML
        System.out.println("Sent HTTP GET request to query customer info, expect XML");
        GetMethod get = new GetMethod("http://localhost:9000/customerservice/customers/123");
        get.addRequestHeader("Accept", "application/xml");
        HttpClient httpclient = new HttpClient();

        try {
            int result = httpclient.executeMethod(get);
            System.out.println("Response status code: " + result);
            System.out.println("Response body: ");
            System.out.println(get.getResponseBodyAsString());
        } finally {
            get.releaseConnection();
        }

        // Sent HTTP GET request to query customer info, expect JSON.
        System.out.println("\n");
        System.out.println("Sent HTTP GET request to query customer info, expect JSON");
        get = new GetMethod("http://localhost:9000/customerservice/customers/123");
        get.addRequestHeader("Accept", "application/json");
        httpclient = new HttpClient();

        try {
            int result = httpclient.executeMethod(get);
            System.out.println("Response status code: " + result);
            System.out.println("Response body: ");
            System.out.println(get.getResponseBodyAsString());
        } finally {
            get.releaseConnection();
        }

        System.out.println("\n");
        System.out.println("Sent HTTP GET request to query customer info, expect XML");
        //The default behavior without setting Accept header explicitly is depending on your client.
        //In the case of  HTTP Client, the Accept header will be absent. The CXF server will treat this
        //as "*/*", XML format is returned
        get = new GetMethod("http://localhost:9000/customerservice/customers/123");
        httpclient = new HttpClient();

        try {
            int result = httpclient.executeMethod(get);
            System.out.println("Response status code: " + result);
            System.out.println("Response body: ");
            System.out.println(get.getResponseBodyAsString());
        } finally {
            get.releaseConnection();
        }

        System.out.println("\n");
        System.exit(0);
    }
}
