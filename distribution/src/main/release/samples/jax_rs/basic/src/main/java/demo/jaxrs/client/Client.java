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

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.resource.URIResolver;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public final class Client {

    private Client() {
    }

    public static void main(String[] args) throws Exception {
        // Sent HTTP GET request to query all customer info
        /*
         * URL url = new URL("http://localhost:9000/customers");
         * System.out.println("Invoking server through HTTP GET to query all
         * customer info"); InputStream in = url.openStream(); StreamSource
         * source = new StreamSource(in); printSource(source);
         */

        // Sent HTTP GET request to query customer info
        System.out.println("Sent HTTP GET request to query customer info");
        URL url = new URL("http://localhost:9000/customerservice/customers/123");
        InputStream in = url.openStream();
        System.out.println(getStringFromInputStream(in));

        // Sent HTTP GET request to query sub resource product info
        System.out.println("\n");
        System.out.println("Sent HTTP GET request to query sub resource product info");
        url = new URL("http://localhost:9000/customerservice/orders/223/products/323");
        in = url.openStream();
        System.out.println(getStringFromInputStream(in));

        // Sent HTTP PUT request to update customer info
        System.out.println("\n");
        System.out.println("Sent HTTP PUT request to update customer info");
        Client client = new Client();
        String inputFile = client.getClass().getResource("/update_customer.xml").getFile();
        try (URIResolver resolver = new URIResolver(inputFile)) {
            File input = new File(resolver.getURI());
    
            HttpPut put = new HttpPut("http://localhost:9000/customerservice/customers");
            put.setEntity(new FileEntity(input, ContentType.TEXT_XML));
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            try {
                CloseableHttpResponse response = httpClient.execute(put);
                System.out.println("Response status code: " + response.getStatusLine().getStatusCode());
                System.out.println("Response body: ");
                System.out.println(EntityUtils.toString(response.getEntity()));
            } finally {
                // Release current connection to the connection pool once you are
                // done
                put.releaseConnection();
            }
        }

        // Sent HTTP POST request to add customer
        System.out.println("\n");
        System.out.println("Sent HTTP POST request to add customer");
        inputFile = client.getClass().getResource("/add_customer.xml").getFile();
        try (URIResolver resolver = new URIResolver(inputFile)) {
            File input = new File(resolver.getURI());
    
            HttpPost post = new HttpPost("http://localhost:9000/customerservice/customers");
            post.addHeader("Accept", "text/xml");
            post.setEntity(new FileEntity(input, ContentType.TEXT_XML));
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
    
            try {
                CloseableHttpResponse response = httpClient.execute(post);
                System.out.println("Response status code: " + response.getStatusLine().getStatusCode());
                System.out.println("Response body: ");
                System.out.println(EntityUtils.toString(response.getEntity()));
            } finally {
                // Release current connection to the connection pool once you are
                // done
                post.releaseConnection();
            }
        }

        System.out.println("\n");
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
