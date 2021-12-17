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

import jakarta.ws.rs.core.Response;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public final class Client {

    private Client() {
    }

    public static void main(String[] args) throws Exception {

        System.out.println("\n");
        Client c = new Client();

        // 1. get customer info
        //
        c.getCustomerInfo("fred", "fred", 123);
        c.getCustomerInfo("fred", "fredspassword", 123);
        c.getCustomerInfo("bob", "bobspassword", 123);

        // 2. add customer info
        //
        c.addCustomerInfo("fred", "fredspassword");
        c.addCustomerInfo("bob", "bobspassword");

        // 3. update customer info
        //
        c.updateCustomerInfo("fred", "fredspassword");
        c.updateCustomerInfo("bob", "bobspassword");

        // 4. delete customer info
        //
        c.deleteCustomerInfo("bob", "bobspassword", 123);
        c.deleteCustomerInfo("fred", "fredspassword", 123);


    }

    public void getCustomerInfo(String name, String password, int id) throws Exception {

        System.out.println("HTTP GET to query customer info, user : "
            + name + ", password : " + password);
        HttpGet get =
            new HttpGet("http://localhost:9002/customerservice/customers/" + id);
        setMethodHeaders(get, name, password);
        handleHttpMethod(get);
    }

    public void addCustomerInfo(String name, String password) throws Exception {

        System.out.println("HTTP POST to add customer info, user : "
            + name + ", password : " + password);
        HttpPost post = new HttpPost("http://localhost:9002/customerservice/customers");
        setMethodHeaders(post, name, password);
        HttpEntity entity = new InputStreamEntity(
            this.getClass().getClassLoader().getResourceAsStream("add_customer.xml"));
        post.setEntity(entity);

        handleHttpMethod(post);
    }

    public void updateCustomerInfo(String name, String password) throws Exception {

        System.out.println("HTTP PUT to update customer info, user : "
            + name + ", password : " + password);
        HttpPut put = new HttpPut("http://localhost:9002/customerservice/customers/123");
        setMethodHeaders(put, name, password);
        HttpEntity entity = new InputStreamEntity(
            this.getClass().getClassLoader().getResourceAsStream("update_customer.xml"));
        put.setEntity(entity);

        handleHttpMethod(put);
    }

    public void deleteCustomerInfo(String name, String password, int id) throws Exception {

        System.out.println("HTTP DELETE to update customer info, user : "
            + name + ", password : " + password);
        System.out.println("Confirming a customer with id " + id + " exists first");
        getCustomerInfo(name, password, id);
        System.out.println("Deleting now...");
        HttpDelete del =
            new HttpDelete("http://localhost:9002/customerservice/customers/" + id);
        setMethodHeaders(del, name, password);
        handleHttpMethod(del);
        System.out.println("Confirming a customer with id " + id + " does not exist anymore");
        getCustomerInfo(name, password, id);
    }

    private static void handleHttpMethod(HttpRequestBase httpMethod) throws Exception {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        try {
            CloseableHttpResponse response = httpClient.execute(httpMethod);
            System.out.println("Response status : " + response.getStatusLine().getStatusCode());

            Response.Status status =
                Response.Status.fromStatusCode(response.getStatusLine().getStatusCode());

            if (status == Response.Status.OK) {
                System.out.println(EntityUtils.toString(response.getEntity()));
            } else if (status == Response.Status.FORBIDDEN) {
                System.out.println("Authorization failure");
            } else if (status == Response.Status.UNAUTHORIZED) {
                System.out.println("Authentication failure");
            }
            System.out.println();

        } finally {
            // release any connection resources used by the method
            httpMethod.releaseConnection();
        }
    }

    private static void setMethodHeaders(HttpRequestBase httpMethod, String name, String password) {
        if (httpMethod instanceof HttpPost || httpMethod instanceof HttpPut) {
            httpMethod.addHeader("Content-Type", "application/xml");
        }
        httpMethod.addHeader("Accept", "application/xml");
        httpMethod.addHeader("Authorization",
                             "Basic " + base64Encode(name + ":" + password));

    }

    private static String base64Encode(String value) {
        return Base64Utility.encode(value.getBytes());
    }
}
