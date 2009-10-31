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

package org.apache.cxf.aegis.jaxws;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.services.Echo;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.test.AbstractCXFTest;

import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class AegisJaxwsGetTest extends AbstractCXFTest {
    
    @Before
    public void before() throws Exception {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setAddress("http://localhost:9167/Echo");
        sf.setDataBinding(new AegisDatabinding());
        sf.setServiceBean(new Echo());
        Server server = sf.create();
        // turn off nanny in URIMappingInterceptor
        server.getEndpoint()
            .getService().put(AbstractInDatabindingInterceptor.NO_VALIDATE_PARTS, Boolean.TRUE);
        
        ServerFactoryBean sf2 = new ServerFactoryBean();
        sf2.setAddress("http://localhost:9167/SimpleEcho");
        sf2.setDataBinding(new AegisDatabinding());
        sf2.setServiceBean(new Echo());
        server = sf2.create();
        // turn off nanny in URIMappingInterceptor
        server.getEndpoint()
            .getService().put(AbstractInDatabindingInterceptor.NO_VALIDATE_PARTS, Boolean.TRUE);
    }
    
    
    private HttpClient createClient() {
        HttpClient httpClient = new HttpClient();
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
        return httpClient;
    }
    
    @Test
    public void testGetEcho() throws Exception {
        HttpClient httpClient = createClient();
        String url = "http://localhost:9167/Echo/echo/echo/hello";
        HttpMethod method = null;
        method = new GetMethod(url);
        int status = httpClient.executeMethod(method);
        assertEquals(HttpStatus.SC_OK, status);
        String result = method.getResponseBodyAsString();
        assertTrue(result.contains("hello"));
        method.releaseConnection();
    }
    @Test
    public void testGetEchoSimple() throws Exception {
        HttpClient httpClient = createClient();
        String url = "http://localhost:9167/SimpleEcho/simpleEcho/string/hello";
        HttpMethod method = null;
        method = new GetMethod(url);
        int status = httpClient.executeMethod(method);
        assertEquals(HttpStatus.SC_OK, status);
        String result = method.getResponseBodyAsString();
        assertTrue(result.contains("hello"));
        method.releaseConnection();
    }
}
