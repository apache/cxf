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

package org.apache.cxf.metrics.micrometer.provider.jaxws;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.metrics.micrometer.provider.ExceptionClassProvider;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

public class JaxwsTagsProviderTest {

    @Mock
    private ExceptionClassProvider exceptionClassProvider;
    @Mock
    private JaxwsFaultCodeProvider faultCodeProvider;
    @Mock
    private Exchange exchange;
    @Mock
    private Message request;
    @Mock
    private Message response;
    @Mock
    private JaxwsTags cxfTags;

    private JaxwsTagsProvider underTest;
    private Tags expectedTags;

    @Before
    public void setUp() {
        initMocks(this);

        underTest = new JaxwsTagsProvider(exceptionClassProvider, faultCodeProvider, cxfTags);

        Tag methodTag = new ImmutableTag("method", "method");
        doReturn(methodTag).when(cxfTags).method(request);

        Tag uriTag = new ImmutableTag("uri", "uri");
        doReturn(uriTag).when(cxfTags).uri(request);

        Tag exceptionTag = new ImmutableTag("exception", "exception");
        doReturn(RuntimeException.class).when(exceptionClassProvider).getExceptionClass(exchange);
        doReturn(exceptionTag).when(cxfTags).exception(RuntimeException.class);

        Tag statusTag = new ImmutableTag("status", "status");
        doReturn(statusTag).when(cxfTags).status(response);

        Tag outcomeTag = new ImmutableTag("outcome", "outcome");
        doReturn(outcomeTag).when(cxfTags).outcome(response);

        Tag operationTag = new ImmutableTag("operation", "operation");
        doReturn(operationTag).when(cxfTags).operation(request);

        Tag faultCodeTag = new ImmutableTag("faultCode", "faultCode");
        doReturn("getFaultCode").when(faultCodeProvider).getFaultCode(exchange);
        doReturn(faultCodeTag).when(cxfTags).faultCode("getFaultCode");

        expectedTags =
                Tags.of(methodTag, uriTag, exceptionTag, statusTag, outcomeTag, operationTag, faultCodeTag);
    }

    @Test
    public void testReadTagsFromSuccessfulInputAndOutput() {
        // given
        doReturn(request).when(exchange).getInMessage();
        doReturn(response).when(exchange).getOutMessage();

        // when
        Iterable<Tag> actual = underTest.getTags(exchange);

        // then
        assertThat(actual, equalTo(expectedTags));
    }

    @Test
    public void testReadTagsFromFaultInputMessageWhenInIsUnsuccessful() {
        // given
        doReturn(null).when(exchange).getInMessage();
        doReturn(request).when(exchange).getInFaultMessage();
        doReturn(response).when(exchange).getOutMessage();

        // when
        Iterable<Tag> actual = underTest.getTags(exchange);

        // then
        assertThat(actual, equalTo(expectedTags));
    }

    @Test
    public void testReadTagsFromFaultyOutputMessageWhenOutIsUnsuccessful() {
        // given
        doReturn(request).when(exchange).getInMessage();
        doReturn(null).when(exchange).getOutMessage();
        doReturn(response).when(exchange).getOutFaultMessage();

        // when
        Iterable<Tag> actual = underTest.getTags(exchange);

        // then
        assertThat(actual, equalTo(expectedTags));
    }
}
