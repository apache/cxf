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
package org.apache.cxf.interceptor;

import java.io.StringWriter;
import java.util.logging.Logger;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("deprecation")
public class AbstractLoggingInterceptorTest {

    private static final String PAYLOAD = "0123456789";

    @Test
    public void testWritePayloadWithNoLimit() throws Exception {
        TestLoggingInterceptor interceptor = new TestLoggingInterceptor();
        interceptor.setLimit(-1);
        assertEquals(PAYLOAD, write(interceptor, PAYLOAD));
    }

    @Test
    public void testWritePayloadTruncatedToLimit() throws Exception {
        TestLoggingInterceptor interceptor = new TestLoggingInterceptor();
        interceptor.setLimit(4);
        assertEquals("0123", write(interceptor, PAYLOAD));
    }

    @Test
    public void testWritePayloadUnderLimit() throws Exception {
        TestLoggingInterceptor interceptor = new TestLoggingInterceptor();
        interceptor.setLimit(100);
        assertEquals(PAYLOAD, write(interceptor, PAYLOAD));
    }

    private static String write(TestLoggingInterceptor interceptor, String payload) throws Exception {
        StringWriter stringWriter = new StringWriter();
        stringWriter.write(payload);
        StringBuilder builder = new StringBuilder();
        interceptor.writePayload(builder, stringWriter, "text/plain");
        return builder.toString();
    }

    private static final class TestLoggingInterceptor extends AbstractLoggingInterceptor {
        private static final Logger LOG = Logger.getLogger(TestLoggingInterceptor.class.getName());

        TestLoggingInterceptor() {
            super(Phase.RECEIVE);
        }

        @Override
        protected Logger getLogger() {
            return LOG;
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            // not used
        }
    }
}
