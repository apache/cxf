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
package org.apache.cxf.systest.jaxws.httpget;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.BeforeClass;
import org.junit.Test;


public class JavaFirstHttpGetTest extends AbstractBusClientServerTestBase {
    private static final String PORT = TestUtil.getPortNumber(Server.class); 
    private static final String BASE_URL = "http://localhost:"
            + PORT + "/JavaFirstHttpGetTest";
    
    public static class Server extends AbstractBusTestServerBase {        
        protected void run() {
            MyImplementation implementor = new MyImplementation();
            JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
            svrFactory.setServiceClass(MyInterface.class);
            svrFactory.setAddress(BASE_URL);
            svrFactory.setServiceBean(implementor);
            svrFactory.getInInterceptors().add(new LoggingInInterceptor());
            svrFactory.getOutInterceptors().add(new LoggingOutInterceptor());
            svrFactory.create();
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
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }
    
    @Test
    public void testDate() throws Exception {
        URLConnection urlCon = new URL(BASE_URL + "/test2?date=2010-09-13T14:23:30.879%2B02:00")
            .openConnection();
        InputStream ins = urlCon.getInputStream();
        String ret = IOUtils.toString(ins);
        assertTrue(!ret.contains("Fault"));
        assertTrue(ret.contains("2010"));
    }
    @Test
    public void testEnum() throws Exception {
        URLConnection urlCon = new URL(BASE_URL + "/test4?myEnum=A")
            .openConnection();
        InputStream ins = urlCon.getInputStream();
        String ret = IOUtils.toString(ins);
        assertTrue(ret, !ret.contains("Fault"));
        assertTrue(ret, ret.contains("A"));
    }
    @Test
    public void testNull() throws Exception {
        URLConnection urlCon = new URL(BASE_URL + "/test7")
            .openConnection();
        InputStream ins = urlCon.getInputStream();
        String ret = IOUtils.toString(ins);
        assertTrue(ret, ret.contains("&lt;null"));
    }
    @Test
    public void testNullPrimitive() throws Exception {
        HttpURLConnection urlCon = (HttpURLConnection)(new URL(BASE_URL + "/test8")
            .openConnection());
        assertEquals(500, urlCon.getResponseCode()); //FAULT
        InputStream ins = urlCon.getErrorStream();
        String ret = IOUtils.toString(ins);
        assertTrue(ret, ret.contains("Fault"));
    }
    
}