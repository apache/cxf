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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSClientServerResourceJacksonSpringProviderTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerResourceJacksonSpringProviders.class));
    }
    
    @Test
    public void testMultipleRootsWadl() throws Exception {
        List<Element> resourceEls = getWadlResourcesInfo("http://localhost:9080/webapp/",
                                                         "http://localhost:9080/webapp/", 2);
        String path1 = resourceEls.get(0).getAttribute("path");
        int bookStoreInd = path1.contains("/bookstore") ? 0 : 1;
        int petStoreInd = bookStoreInd == 0 ? 1 : 0;
        checkBookStoreInfo(resourceEls.get(bookStoreInd));
        checkPetStoreInfo(resourceEls.get(petStoreInd));
    }
    
    @Test
    public void testBookStoreWadl() throws Exception {
        List<Element> resourceEls = getWadlResourcesInfo("http://localhost:9080/webapp/",
                                                         "http://localhost:9080/webapp/bookstore", 1);
        checkBookStoreInfo(resourceEls.get(0));
    }
    
    @Test
    public void testPetStoreWadl() throws Exception {
        List<Element> resourceEls = getWadlResourcesInfo("http://localhost:9080/webapp/",
                                                         "http://localhost:9080/webapp/petstore", 1);
        checkPetStoreInfo(resourceEls.get(0));
    }
 
    private void checkBookStoreInfo(Element resource) {
        assertEquals("/bookstore", resource.getAttribute("path"));
    }
    
    private void checkPetStoreInfo(Element resource) {
        assertEquals("/petstore/", resource.getAttribute("path"));
    }
    
    private List<Element> getWadlResourcesInfo(String baseURI, String requestURI, int size) throws Exception {
        WebClient client = WebClient.create(requestURI + "?_wadl&_type=xml");
        Document doc = DOMUtils.readXml(new InputStreamReader(client.get(InputStream.class), "UTF-8"));
        Element root = doc.getDocumentElement();
        assertEquals("http://research.sun.com/wadl/2006/10", root.getNamespaceURI());
        assertEquals("application", root.getLocalName());
        List<Element> resourcesEls = DOMUtils.getChildrenWithName(root, 
                                            "http://research.sun.com/wadl/2006/10", "resources");
        assertEquals(1, resourcesEls.size());
        Element resourcesEl =  resourcesEls.get(0);
        assertEquals(baseURI, resourcesEl.getAttribute("base"));
        List<Element> resourceEls = 
            DOMUtils.getChildrenWithName(resourcesEl, 
                                                "http://research.sun.com/wadl/2006/10", "resource");
        assertEquals(size, resourceEls.size());
        return resourceEls;
    }
    
    @Test
    public void testGetBook123() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/webapp/bookstore/books/123"; 
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/json");
        InputStream in = connect.getInputStream();
        assertNotNull(in);           

        assertEquals("Jackson output not correct", 
                     "{\"name\":\"CXF in Action\",\"id\":123}",
                     getStringFromInputStream(in).trim());
    }
    
    @Test
    public void testPostPetStatus() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/webapp/petstore/pets";

        URL url = new URL(endpointAddress);   
        HttpURLConnection httpUrlConnection = (HttpURLConnection)url.openConnection();  
             
        httpUrlConnection.setUseCaches(false);   
        httpUrlConnection.setDefaultUseCaches(false);   
        httpUrlConnection.setDoOutput(true);   
        httpUrlConnection.setDoInput(true);   
        httpUrlConnection.setRequestMethod("POST");   
        httpUrlConnection.setRequestProperty("Accept",   "text/xml");   
        httpUrlConnection.setRequestProperty("Content-type",   "application/x-www-form-urlencoded");   
        httpUrlConnection.setRequestProperty("Connection",   "close");   

        OutputStream outputstream = httpUrlConnection.getOutputStream();
        File inputFile = new File(getClass().getResource("resources/singleValPostBody.txt").toURI());         
         
        byte[] tmp = new byte[4096];
        int i = 0;
        InputStream is = new FileInputStream(inputFile);
        try {
            while ((i = is.read(tmp)) >= 0) {
                outputstream.write(tmp, 0, i);
            }
        } finally {
            is.close();
        }

        outputstream.flush();

        int responseCode = httpUrlConnection.getResponseCode();   
        assertEquals(200, responseCode); 
        assertEquals("Wrong status returned", "open", getStringFromInputStream(httpUrlConnection
            .getInputStream()));  
        httpUrlConnection.disconnect();
    }
    
    @Test
    public void testPostPetStatus2() throws Exception {
        
        
        Socket s = new Socket("localhost", 9080);
        IOUtils.copyAndCloseInput(getClass().getResource("resources/formRequest.txt").openStream(), 
                                  s.getOutputStream());

        s.getOutputStream().flush();
        try {
            assertTrue("Wrong status returned", getStringFromInputStream(s.getInputStream())
                       .contains("open"));  
        } finally {
            s.close();
        }
    }
    
    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }

}
