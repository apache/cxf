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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.cxf.helpers.IOUtils;

public final class Client {
    private Client() {
    }

    public static void main(String args[]) throws Exception {               
        final String url = "http://localhost:9000/catalog";
        final HttpClient httpClient = new HttpClient();
                        
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

    private static void list(final String url, final HttpClient httpClient) 
        throws IOException, HttpException {
        
        System.out.println("Sent HTTP GET request to query all books in catalog");
        
        final GetMethod get = new GetMethod(url);
        try {
            int status = httpClient.executeMethod(get);
            if (status == 200) {   
                System.out.println(get.getResponseBodyAsString());
            }
        } finally {
            get.releaseConnection();
        }
    }
    
    private static void search(final String url, final HttpClient httpClient, final String expression) 
        throws IOException, HttpException {
            
        System.out.println("Sent HTTP GET request to search the books in catalog: " + expression);
        
        final GetMethod get = new GetMethod(url + "/search");
        get.setQueryString("$filter=" + expression);
        
        try {
            int status = httpClient.executeMethod(get);
            if (status == 200) {   
                System.out.println(get.getResponseBodyAsString());
            }
        } finally {
            get.releaseConnection();
        }
    }
    

    private static void uploadToCatalog(final String url, final HttpClient httpClient,
            final String filename) throws IOException, HttpException {
        
        System.out.println("Sent HTTP POST request to upload the file into catalog: " + filename);
        
        final PostMethod post = new PostMethod(url);
        final Part[] parts = {
            new FilePart(filename,
                new ByteArrayPartSource(filename, 
                    IOUtils.readBytesFromStream(Client.class.getResourceAsStream("/" + filename)) 
                ) 
            )
        };
        
        post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
        
        try {
            int status = httpClient.executeMethod(post);
            if (status == 201) {   
                System.out.println(post.getResponseHeader("Location"));
            } else if (status == 409) {   
                System.out.println("Document already exists: " + filename);
            }

        } finally {
            post.releaseConnection();
        }
    }
    
    private static void delete(final String url, final HttpClient httpClient) 
        throws IOException, HttpException {
                
        System.out.println("Sent HTTP DELETE request to remove all books from catalog");
        
        final DeleteMethod delete = new DeleteMethod(url);            
        try {
            int status = httpClient.executeMethod(delete);
            if (status == 200) {   
                System.out.println(delete.getResponseBodyAsString());
            }
        } finally {
            delete.releaseConnection();
        }
    }

}
