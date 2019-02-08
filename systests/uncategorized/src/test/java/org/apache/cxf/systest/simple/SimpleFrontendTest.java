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

package org.apache.cxf.systest.simple;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.systest.simple.impl.WSSimpleImpl;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class SimpleFrontendTest {
    static final String PORT1 = TestUtil.getPortNumber(SimpleFrontendTest.class);


    static Bus bus;
    static String add11 = "http://localhost:" + PORT1 + "/test11";


    @BeforeClass
    public static void createServers() throws Exception {
        bus = BusFactory.getDefaultBus();
        ServerFactoryBean sf = new ServerFactoryBean();
        sf.setServiceBean(new WSSimpleImpl());
        sf.setAddress(add11);
        sf.setBus(bus);
        sf.create();


    }

    @AfterClass
    public static void shutdown() throws Exception {
        bus.shutdown(true);
    }


    @Test
    public void testGetWSDL() throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + PORT1 + "/test11?wsdl");
        CloseableHttpResponse httpResponse = client.execute(get);
        String response = EntityUtils.toString(httpResponse.getEntity());
        assertFalse(response.indexOf("import") >= 0);
        assertFalse(response.indexOf("?wsdl?wsdl") >= 0);
    }

}