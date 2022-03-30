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

package org.apache.cxf.metrics.micrometer.provider.jaxrs;

import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import io.micrometer.core.instrument.Tag;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JaxrsTagsTest {
    private static final String OPERATION_METRIC_NAME = "operation";
    private static final String DUMMY_OPERATOR = "getOperator";

    private JaxrsTags tags;
    private Message message;

    @Before
    public void setUp() throws NoSuchMethodException, SecurityException {
        message = new MessageImpl();
        message.setExchange(new ExchangeImpl());
        message.put("org.apache.cxf.resource.method", getClass().getDeclaredMethod("getOperator"));
        tags = new JaxrsTags();
    }
    
    @Test
    public void testOperationReturnWithUnknownWhenRequestIsNull() {
        final Tag actual = tags.operation(null);
        assertThat(actual, is(Tag.of(OPERATION_METRIC_NAME, "UNKNOWN")));
    }

    @Test
    public void testOperationReturnWithCorrectValue() {
        final Tag actual = tags.operation(message);
        assertThat(actual, is(Tag.of(OPERATION_METRIC_NAME, DUMMY_OPERATOR)));
    }
    
    @SuppressWarnings("unused")
    private void getOperator() {
        /* operation method */
    }
}
