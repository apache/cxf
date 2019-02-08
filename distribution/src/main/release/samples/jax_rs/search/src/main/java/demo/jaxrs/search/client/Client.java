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

package demo.jaxrs.search.client;

import java.io.IOException;

import org.apache.cxf.helpers.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public final class Client {
    private Client() {
    }

    public static void main(String[] args) throws Exception {
        final String url = "http://localhost:9000/catalog";
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        uploadToCatalog(url, httpClient, "jsr339-jaxrs-2.0-final-spec.pdf");
        uploadToCatalog(url, httpClient, "JavaWebSocketAPI_1.0_Final.pdf");
        uploadToCatalog(url, httpClient, "apache-cxf-tika-lucene.odt");

        list(url, httpClient);

        search(url, httpClient, "ct==java");
        search(url, httpClient, "ct==websockets");

        search(url, httpClient, "ct==Java");
        search(url, httpClient, "ct==WebSockets");

        search(url, httpClient, "ct==jaxrs,source==*jaxrs*");
        search(url, httpClient, "ct==tika");

        delete(url, httpClient);
    }

    private static void list(final String url, final CloseableHttpClient httpClient)
        throws IOException {

        System.out.println("Sent HTTP GET request to query all books in catalog");

        final HttpGet get = new HttpGet(url);
        try {
            CloseableHttpResponse response = httpClient.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                System.out.println(EntityUtils.toString(response.getEntity()));
            }
        } finally {
            get.releaseConnection();
        }
    }

    private static void search(final String url, final CloseableHttpClient httpClient, final String expression)
        throws IOException {

        System.out.println("Sent HTTP GET request to search the books in catalog: " + expression);

        final HttpGet get = new HttpGet(url + "/search?$filter=" + expression);

        try {
            CloseableHttpResponse response = httpClient.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                System.out.println(EntityUtils.toString(response.getEntity()));
            }
        } finally {
            get.releaseConnection();
        }
    }


    private static void uploadToCatalog(final String url, final CloseableHttpClient httpClient,
            final String filename) throws IOException {

        System.out.println("Sent HTTP POST request to upload the file into catalog: " + filename);

        final HttpPost post = new HttpPost(url);
        MultipartEntity entity = new MultipartEntity();
        byte[] bytes = IOUtils.readBytesFromStream(Client.class.getResourceAsStream("/" + filename));
        entity.addPart(filename, new ByteArrayBody(bytes, filename));

        post.setEntity(entity);

        try {
            CloseableHttpResponse response = httpClient.execute(post);
            if (response.getStatusLine().getStatusCode() == 201) {
                System.out.println(response.getFirstHeader("Location"));
            } else if (response.getStatusLine().getStatusCode() == 409) {
                System.out.println("Document already exists: " + filename);
            }

        } finally {
            post.releaseConnection();
        }
    }

    private static void delete(final String url, final CloseableHttpClient httpClient)
        throws IOException {

        System.out.println("Sent HTTP DELETE request to remove all books from catalog");

        final HttpDelete delete = new HttpDelete(url);
        try {
            CloseableHttpResponse response = httpClient.execute(delete);
            if (response.getStatusLine().getStatusCode() == 200) {
                System.out.println(EntityUtils.toString(response.getEntity()));
            }
        } finally {
            delete.releaseConnection();
        }
    }

}
