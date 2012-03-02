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

package org.apache.cxf.systest.provider;


import java.io.InputStream;

import javax.xml.ws.Endpoint;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.junit.BeforeClass;
import org.junit.Test;

public class CXF4130Test extends AbstractBusClientServerTestBase {

    public static final String ADDRESS 
        = "http://localhost:" + TestUtil.getPortNumber(Server.class)
            + "/InBand33MessageServiceProvider/InBandSoapHeaderSoapHttpPort";
    
    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            Object implementor = new CXF4130Provider();
            Endpoint.publish(ADDRESS, implementor);                                 
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }
    
    @Test
    public void testCxf4130() throws Exception {
        InputStream body = getClass().getResourceAsStream("cxf4130data.txt");
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(ADDRESS);
        post.setRequestEntity(new InputStreamRequestEntity(body, "text/xml"));
        client.executeMethod(post); 

        Document doc = StaxUtils.read(post.getResponseBodyAsStream());
        Element root = doc.getDocumentElement();
        Node child = root.getFirstChild();
        
        boolean foundBody = false;
        while (child != null) {
            if ("Body".equals(child.getLocalName())) {
                foundBody = true;
                assertEquals(1, child.getChildNodes().getLength());
                assertEquals("FooResponse", child.getFirstChild().getLocalName());
            }
            child = child.getNextSibling();
        }
        assertTrue("Did not find the soap:Body element", foundBody);
    }

}
