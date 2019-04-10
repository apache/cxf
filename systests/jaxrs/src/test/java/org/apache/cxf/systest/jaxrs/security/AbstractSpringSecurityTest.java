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

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import static org.junit.Assert.assertEquals;

public abstract class AbstractSpringSecurityTest extends AbstractBusClientServerTestBase {

    protected void getBook(String endpointAddress, String user, String password, int expectedStatus) throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            CloseableHttpResponse response = client.execute(RequestBuilder
                .get(endpointAddress)
                .addHeader(HttpHeaders.ACCEPT, "application/xml")
                .addHeader(HttpHeaders.AUTHORIZATION, DefaultBasicAuthSupplier.getBasicAuthHeader(user, password))
                    .build());
            assertEquals(expectedStatus, response.getStatusLine().getStatusCode());
            if (expectedStatus == 200) {
                try (InputStream expected = getClass()
                        .getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/expected_get_book123.txt")) {
                    assertEquals("Expected value is wrong",
                            stripXmlInstructionIfNeeded(IOUtils.toString(expected)),
                            stripXmlInstructionIfNeeded(EntityUtils.toString(response.getEntity())));
                }
            }
        }
    }

    private static String stripXmlInstructionIfNeeded(String str) {
        if (str != null && str.startsWith("<?xml")) {
            return str.substring(str.indexOf("?>") + 2);
        }
        return str;
    }
}
