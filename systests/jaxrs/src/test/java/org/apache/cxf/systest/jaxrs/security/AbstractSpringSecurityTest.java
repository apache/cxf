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

package org.apache.cxf.systest.jaxrs.security;

import java.io.InputStream;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public abstract class AbstractSpringSecurityTest extends AbstractBusClientServerTestBase {

    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }

    protected String base64Encode(String value) {
        return Base64Utility.encode(value.getBytes());
    }

    protected void getBook(String endpointAddress, String user, String password,
                         int expectedStatus)
        throws Exception {

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(endpointAddress);
        get.addHeader("Accept", "application/xml");
        get.addHeader("Authorization",
                             "Basic " + base64Encode(user + ":" + password));
        try {
            CloseableHttpResponse response = client.execute(get);
            assertEquals(expectedStatus, response.getStatusLine().getStatusCode());
            if (expectedStatus == 200) {
                String content = EntityUtils.toString(response.getEntity());
                String resource = "/org/apache/cxf/systest/jaxrs/resources/expected_get_book123.txt";
                InputStream expected = getClass().getResourceAsStream(resource);
                assertEquals("Expected value is wrong",
                             stripXmlInstructionIfNeeded(getStringFromInputStream(expected)),
                             stripXmlInstructionIfNeeded(content));
            }
        } finally {
            get.releaseConnection();
        }

    }
    private String stripXmlInstructionIfNeeded(String str) {
        if (str != null && str.startsWith("<?xml")) {
            int index = str.indexOf("?>");
            str = str.substring(index + 2);
        }
        return str;
    }
}
