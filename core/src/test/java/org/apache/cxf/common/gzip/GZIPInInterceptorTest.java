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

package org.apache.cxf.common.gzip;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class GZIPInInterceptorTest {
    private Message message;
    
    @Before
    public void setUp() {
        final Map<String, List<String>> heeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        heeaders.put(HttpHeaderHelper.CONTENT_ENCODING, Collections.singletonList("gzip"));

        message = new MessageImpl();
        message.put(Message.PROTOCOL_HEADERS, heeaders);
    }

    @Test
    public void testNoContent() {
        final GZIPInInterceptor interceptor = new GZIPInInterceptor();
        final InputStream nullInputStream = InputStream.nullInputStream();

        message.setContent(InputStream.class, nullInputStream);
        message.put(Message.RESPONSE_CODE, 204);

        interceptor.handleMessage(message);
        assertThat(message.getContent(InputStream.class), equalTo(nullInputStream));
    }
}
