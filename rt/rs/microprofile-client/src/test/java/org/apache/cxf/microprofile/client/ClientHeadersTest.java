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
package org.apache.cxf.microprofile.client;

import java.net.URI;

import org.apache.cxf.microprofile.client.mock.HeaderCaptureClientRequestFilter;
import org.apache.cxf.microprofile.client.mock.HeadersFactoryClient;
import org.apache.cxf.microprofile.client.mock.HeadersOnInterfaceClient;
import org.apache.cxf.microprofile.client.mock.HeadersOnMethodClient;
import org.apache.cxf.microprofile.client.mock.MyClientHeadersFactory;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import org.junit.Before;
import org.junit.Test;

import static org.apache.cxf.microprofile.client.mock.HeaderCaptureClientRequestFilter.getOutboundHeaders;
import static org.apache.cxf.microprofile.client.mock.MyClientHeadersFactory.getInitialHeaders;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ClientHeadersTest {

    @Before
    public void clearHeaders() {
        HeaderCaptureClientRequestFilter.setOutboundHeaders(null);
        MyClientHeadersFactory.setInitialHeaders(null);
    }

    @Test
    public void testClientHeaderParamsOnInterface() {
        HeadersOnInterfaceClient client = RestClientBuilder.newBuilder()
                                                           .baseUri(URI.create("http://localhost/notUsed"))
                                                           .register(HeaderCaptureClientRequestFilter.class)
                                                           .build(HeadersOnInterfaceClient.class);
        assertEquals("SUCCESS", client.put("ignored"));
        assertNotNull(getOutboundHeaders());
        assertEquals("value1", getOutboundHeaders().getFirst("IntfHeader1"));
        assertEquals("value2,value3", getOutboundHeaders().getFirst("IntfHeader2"));
        assertEquals("HeadersOnInterfaceClientValueForIntfHeader3", getOutboundHeaders().getFirst("IntfHeader3"));
        assertEquals("valueForIntfHeader4", getOutboundHeaders().getFirst("IntfHeader4"));
    }

    @Test
    public void testClientHeaderParamsOnMethod() {
        HeadersOnMethodClient client = RestClientBuilder.newBuilder()
                                                        .baseUri(URI.create("http://localhost/notUsed"))
                                                        .register(HeaderCaptureClientRequestFilter.class)
                                                        .build(HeadersOnMethodClient.class);
        assertEquals("SUCCESS", client.delete("ignored"));
        assertNotNull(getOutboundHeaders());
        assertEquals("valueA", getOutboundHeaders().getFirst("MethodHeader1"));
        assertEquals("valueB,valueC", getOutboundHeaders().getFirst("MethodHeader2"));
        assertEquals("HeadersOnMethodClientValueForMethodHeader3", getOutboundHeaders().getFirst("MethodHeader3"));
        assertEquals("valueForMethodHeader4", getOutboundHeaders().getFirst("MethodHeader4"));
    }

    @Test
    public void testClientHeadersFactory() {
        HeadersFactoryClient client = RestClientBuilder.newBuilder()
                                                       .baseUri(URI.create("http://localhost/notUsed"))
                                                       .register(HeaderCaptureClientRequestFilter.class)
                                                       .build(HeadersFactoryClient.class);
        assertEquals("SUCCESS", client.get("headerParamValue1"));
        assertNotNull(getInitialHeaders());
        assertEquals("headerParamValue1", getInitialHeaders().getFirst("HeaderParam1"));
        assertEquals("abc", getInitialHeaders().getFirst("IntfHeader1"));
        assertEquals("def", getInitialHeaders().getFirst("MethodHeader1"));

        assertNotNull(getOutboundHeaders());
        assertEquals("1eulaVmaraPredaeh", getOutboundHeaders().getFirst("HeaderParam1"));
        assertEquals("cba", getOutboundHeaders().getFirst("IntfHeader1"));
        assertEquals("fed", getOutboundHeaders().getFirst("MethodHeader1"));
        
    }
}
