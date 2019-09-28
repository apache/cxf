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

package org.apache.cxf.javascript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.test.AbstractCXFSpringTest;
import org.apache.cxf.testutil.common.TestUtil;
import org.springframework.context.support.GenericApplicationContext;

import org.junit.Before;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

/**
 *
 */
public class QueryHandlerTest extends AbstractCXFSpringTest {
    private static final Logger LOG = LogUtils.getL7dLogger(QueryHandlerTest.class);
    private Endpoint hwEndpoint;
    private Endpoint dlbEndpoint;

    public QueryHandlerTest() throws Exception {
        super();
    }

    /** {@inheritDoc}*/
    @Override
    protected void additionalSpringConfiguration(GenericApplicationContext context) throws Exception {
        // we don't need any.
    }

    /** {@inheritDoc}*/
    @Override
    protected String[] getConfigLocations() {
        TestUtil.getNewPortNumber("TestPort");
        return new String[] {"classpath:HelloWorldDocLitBeans.xml",
                             "classpath:DocLitBareClientTestBeans.xml",
                             "classpath:HelloWorldGreeterBeans.xml"};

    }

    @Before
    public void before() {
        ServerFactoryBean serverFactoryBean = getBean(ServerFactoryBean.class, "hw-service-endpoint");
        hwEndpoint = serverFactoryBean.create().getEndpoint();
        serverFactoryBean = getBean(ServerFactoryBean.class, "dlb-service-endpoint");
        dlbEndpoint = serverFactoryBean.create().getEndpoint();
    }

    private String getStringFromURL(URL url) throws IOException {
        InputStream jsStream = url.openStream();
        return readStringFromStream(jsStream);
    }

    private String readStringFromStream(InputStream jsStream) throws IOException {
        InputStreamReader isr = new InputStreamReader(jsStream, UTF_8);
        BufferedReader in = new BufferedReader(isr);
        String line = in.readLine();
        StringBuilder js = new StringBuilder();
        while (line != null) {
            String[] tok = line.split("\\s");

            for (int x = 0; x < tok.length; x++) {
                String token = tok[x];
                js.append("  ").append(token);
            }
            line = in.readLine();
        }
        return js.toString();
    }

    @Test
    public void hwQueryTest() throws Exception {
        URL endpointURL = new URL(hwEndpoint.getEndpointInfo().getAddress()  + "?js");
        String js = getStringFromURL(endpointURL);
        assertNotSame(0, js.length());
    }

    @Test
    public void dlbQueryTest() throws Exception {
        LOG.finest("logged to avoid warning on LOG");
        URL endpointURL = new URL(dlbEndpoint.getEndpointInfo().getAddress()  + "?js");
        URLConnection connection = endpointURL.openConnection();
        assertEquals("application/javascript;charset=UTF-8", connection.getContentType());
        InputStream jsStream = connection.getInputStream();
        String js = readStringFromStream(jsStream);
        assertNotSame("", js);
    }

    @Test
    public void utilsTest() throws Exception {
        URL endpointURL = new URL(dlbEndpoint.getEndpointInfo().getAddress()  + "?js&nojsutils");
        URLConnection connection = endpointURL.openConnection();
        assertEquals("application/javascript;charset=UTF-8", connection.getContentType());
        InputStream jsStream = connection.getInputStream();
        String jsString = readStringFromStream(jsStream);
        assertFalse(jsString.contains("function CxfApacheOrgUtil"));
    }

}