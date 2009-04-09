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

package org.apache.cxf.systest.jaxrs;

import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSClientServerNonSpringBookTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookNonSpringServer.class));
    }
    
    
    @Test
    public void testGetBook123Singleton() throws Exception {
        getAndCompareAsStrings("http://localhost:9080/singleton/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        
    }
    
    @Test
    public void testGetBook123ApplicationSingleton() throws Exception {
        getAndCompareAsStrings("http://localhost:9080/application/bookstore/books/123",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        
    }
    
    @Test
    public void testGetBook123ApplicationPerRequest() throws Exception {
        getAndCompareAsStrings("http://localhost:9080/application/bookstore2/bookheaders",
                               "resources/expected_get_book123.txt",
                               "application/xml", 200);
        
    }
    
    private void getAndCompareAsStrings(String address, 
                                        String resourcePath,
                                        String acceptType,
                                        int status) throws Exception {
        String expected = getStringFromInputStream(
                              getClass().getResourceAsStream(resourcePath));
        getAndCompare(address,
                      expected,
                      acceptType,
                      acceptType,
                      status);
    }
    
    
    
    private void getAndCompare(String address, 
                               String expectedValue,
                               String acceptType,
                               String expectedContentType,
                               int expectedStatus) throws Exception {
        GetMethod get = new GetMethod(address);
        get.setRequestHeader("Accept", acceptType);
        get.setRequestHeader("Accept-Language", "da;q=0.8,en");
        get.setRequestHeader("Book", "1,2,3");
        HttpClient httpClient = new HttpClient();
        try {
            int result = httpClient.executeMethod(get);
            assertEquals(expectedStatus, result);
            String content = getStringFromInputStream(get.getResponseBodyAsStream());
            assertEquals("Expected value is wrong", 
                         expectedValue, content);
            if (expectedContentType != null) {
                Header ct = get.getResponseHeader("Content-Type");
                assertEquals("Wrong type of response", expectedContentType, ct.getValue());
            }
        } finally {
            get.releaseConnection();
        }
    }
    
    
    private String getStringFromInputStream(InputStream in) throws Exception {        
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        bos.close();
        return bos.getOut().toString();        
    }

}
