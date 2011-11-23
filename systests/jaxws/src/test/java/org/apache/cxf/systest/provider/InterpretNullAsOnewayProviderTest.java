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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Provider;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Testing the null response behavior of jaxws provider (jaxws 2.2 section 5.1.1)
 */
public class InterpretNullAsOnewayProviderTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

    private static final String ADDRESS1 = "http://localhost:" + PORT + "/test/nullable1";
    private static final String ADDRESS2 = "http://localhost:" + PORT + "/test/nullable2";
    
    public static class Server extends AbstractBusTestServerBase {
    
        protected void run() {
            // endpoint not interpreting null as oneway
            NullProviderService servant1 = new NullProviderService();
            Endpoint ep1 = Endpoint.publish(ADDRESS1, servant1);
            assertNotNull("endpoint published", ep1);
            
            // endpoint interpreting null as oneway
            NullProviderService servant2 = new NullProviderService();
            Endpoint ep2 = Endpoint.publish(ADDRESS2, servant2);
            assertNotNull("endpoint published", ep2);
            ep2.getProperties().put("jaxws.provider.interpretNullAsOneway", Boolean.TRUE);
        }
    
        public static void main(String[] args) throws Exception { 
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

    @javax.xml.ws.WebServiceProvider(serviceName = "NullService",
                                     portName = "NullPort")
    @javax.xml.ws.ServiceMode(value = javax.xml.ws.Service.Mode.PAYLOAD)
    public static class NullProviderService implements Provider<Source> {
        public Source invoke(Source request) {
            return null;
        }
    }
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }
    
    @Test
    public void testNotInterpretNullAsOneway() throws Exception {
        HttpURLConnection conn = postRequest(ADDRESS1);
        assertTrue("Soap fault must be returned", 400 <= conn.getResponseCode());
    }
    
    @Test
    public void testInterpretNullAsOneway() throws Exception {
        HttpURLConnection conn = postRequest(ADDRESS2);
        assertEquals("http 202 must be returned", 202, conn.getResponseCode());
    }
    
    private static HttpURLConnection postRequest(String address) throws Exception {
        URL url = new URL(address);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        InputStream in = InterpretNullAsOnewayProviderTest.class.
            getResourceAsStream("resources/sayHiDocLiteralReq.xml");
        assertNotNull("could not load test data", in);

        conn.setRequestMethod("POST");
        conn.addRequestProperty("Content-Type", "text/xml");
        OutputStream out = conn.getOutputStream();
        IOUtils.copy(in, out);
        out.close();

        return conn;
    }
}
