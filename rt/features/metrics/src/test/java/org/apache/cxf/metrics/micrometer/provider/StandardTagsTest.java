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

package org.apache.cxf.metrics.micrometer.provider;

import javax.xml.namespace.QName;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;

import io.micrometer.core.instrument.Tag;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.openMocks;


@SuppressWarnings("PMD.UselessPureMethodCall")
public class StandardTagsTest {

    private static final String DUMMY_METHOD_NAME = "dummyMethod";
    private static final String DUMMY_STATUS_CODE = "200";
    private static final String DUMMY_URI = "/dummyUri";

    private static final String METHOD_METRIC_NAME = "method";
    private static final String STATUS_METRIC_NAME = "status";
    private static final String URI_METRIC_NAME = "uri";
    private static final String EXCEPTION_METRIC_NAME = "exception";
    private static final String OUTCOME_METRIC_NAME = "outcome";

    private static final String DUMMY_OPERATOR = "getOperator";
    private static final QName DUMMY_OPERATION_QNAME = new QName("http://namespace", DUMMY_OPERATOR);

    private StandardTags underTest;

    @Mock
    private Message request;
    @Mock
    private Message response;
    @Mock
    private Exchange exchange;
    @Mock
    private BindingOperationInfo bindingOperationInfo;

    @Before
    public void setUp() {
        openMocks(this);

        doReturn(exchange).when(request).getExchange();
        doReturn(bindingOperationInfo).when(exchange).getBindingOperationInfo();
        doReturn(DUMMY_OPERATION_QNAME).when(bindingOperationInfo).getName();

        underTest = new StandardTags();
    }

    @Test
    public void testOutcomeReturnWithCorrectValueWhen100() {
        // given
        doReturn(100).when(response).get(Message.RESPONSE_CODE);

        // when
        Tag actual = underTest.outcome(response);

        // then
        assertThat(actual, is(Tag.of(OUTCOME_METRIC_NAME, "INFORMATIONAL")));
    }

    @Test
    public void testOutcomeReturnWithCorrectValueWhen200() {
        // given
        doReturn(200).when(response).get(Message.RESPONSE_CODE);

        // when
        Tag actual = underTest.outcome(response);

        // then
        assertThat(actual, is(Tag.of(OUTCOME_METRIC_NAME, "SUCCESS")));
    }

    @Test
    public void testOutcomeReturnWithCorrectValueWhen300() {
        // given
        doReturn(300).when(response).get(Message.RESPONSE_CODE);

        // when
        Tag actual = underTest.outcome(response);

        // then
        assertThat(actual, is(Tag.of(OUTCOME_METRIC_NAME, "REDIRECTION")));
    }

    @Test
    public void testOutcomeReturnWithCorrectValueWhen400() {
        // given
        doReturn(400).when(response).get(Message.RESPONSE_CODE);

        // when
        Tag actual = underTest.outcome(response);

        // then
        assertThat(actual, is(Tag.of(OUTCOME_METRIC_NAME, "CLIENT_ERROR")));
    }

    @Test
    public void testOutcomeReturnWithCorrectValueWhen500() {
        // given
        doReturn(500).when(response).get(Message.RESPONSE_CODE);

        // when
        Tag actual = underTest.outcome(response);

        // then
        assertThat(actual, is(Tag.of(OUTCOME_METRIC_NAME, "SERVER_ERROR")));
    }

    @Test
    public void testMethodReturnWithUnknownWhenRequestIsNull() {
        // given

        // when
        Tag actual = underTest.method(null);

        // then
        assertThat(actual, is(Tag.of(METHOD_METRIC_NAME, "UNKNOWN")));
    }

    @Test
    public void testMethodReturnWithUnknownWhenMethodIsNull() {
        // given

        // when
        Tag actual = underTest.method(request);

        // then
        assertThat(actual, is(Tag.of(METHOD_METRIC_NAME, "UNKNOWN")));
    }

    @Test
    public void testMethodReturnWithUnknownWhenMethodIsNotString() {
        // given
        doReturn(new Object()).when(request).get(Message.HTTP_REQUEST_METHOD);

        // when
        Tag actual = underTest.method(request);

        // then
        assertThat(actual, is(Tag.of(METHOD_METRIC_NAME, "UNKNOWN")));
    }

    @Test
    public void testMethodReturnWithCorrectValue() {
        // given
        doReturn(DUMMY_METHOD_NAME).when(request).get(Message.HTTP_REQUEST_METHOD);

        // when
        Tag actual = underTest.method(request);

        // then
        assertThat(actual, is(Tag.of(METHOD_METRIC_NAME, DUMMY_METHOD_NAME)));
    }

    @Test
    public void testStatusReturnWithUnknownWhenResponseIsNull() {
        // given

        // when
        Tag actual = underTest.status(null);

        // then
        assertThat(actual, is(Tag.of(STATUS_METRIC_NAME, "UNKNOWN")));
    }

    @Test
    public void testStatusReturnWith200WhenResponseCodeIsNotSet() {
        // given

        // when
        Tag actual = underTest.status(response);

        // then
        assertThat(actual, is(Tag.of(STATUS_METRIC_NAME, "200")));
    }

    @Test
    public void testStatusReturnWith202WhenResponseCodeIsNullAndResponseIsPartial() {
        // given
        doReturn(null).when(request).get(Message.RESPONSE_CODE);
        doReturn(true).when(request).get(Message.EMPTY_PARTIAL_RESPONSE_MESSAGE);

        // when
        Tag actual = underTest.status(request);

        // then
        assertThat(actual, is(Tag.of(STATUS_METRIC_NAME, "202")));
    }

    @Test
    public void testStatusReturnWithCorrectValue() {
        // given
        doReturn(Integer.valueOf(DUMMY_STATUS_CODE)).when(request).get(Message.RESPONSE_CODE);

        // when
        Tag actual = underTest.status(request);

        // then
        assertThat(actual, is(Tag.of(STATUS_METRIC_NAME, DUMMY_STATUS_CODE)));
    }

    @Test
    public void testUriReturnWithUnknownWhenRequestIsNull() {
        // given

        // when
        Tag actual = underTest.uri(null);

        // then
        assertThat(actual, is(Tag.of(URI_METRIC_NAME, "UNKNOWN")));
    }

    @Test
    public void testUriReturnWithUnknownWhenBasePathIsNull() {
        // given

        // when
        Tag actual = underTest.uri(request);

        // then
        assertThat(actual, is(Tag.of(URI_METRIC_NAME, "UNKNOWN")));
    }

    @Test
    public void testUriReturnWithUnknownWhenMethodIsNotString() {
        // given
        doReturn(new Object()).when(request).get(Message.BASE_PATH);

        // when
        Tag actual = underTest.uri(request);

        // then
        assertThat(actual, is(Tag.of(URI_METRIC_NAME, "UNKNOWN")));
    }

    @Test
    public void testUriReturnWithCorrectValue() {
        // given
        doReturn(DUMMY_URI).when(request).get(Message.REQUEST_URI);

        // when
        Tag actual = underTest.uri(request);

        // then
        assertThat(actual, is(Tag.of(URI_METRIC_NAME, DUMMY_URI)));
    }

    @Test
    public void testExceptionReturnWithUnknownWhenExceptionClassIsNull() {
        // given

        // when
        Tag actual = underTest.exception(null);

        // then
        assertThat(actual, is(Tag.of(EXCEPTION_METRIC_NAME, "None")));
    }

    @Test
    public void testExceptionReturnWithSimpleName() {
        // given

        // when
        Tag actual = underTest.exception(RuntimeException.class);

        // then
        assertThat(actual, is(Tag.of(EXCEPTION_METRIC_NAME, "RuntimeException")));
    }

    @Test
    public void testExceptionReturnWithNameWhenExceptionIsAnonymous() {
        // given
        Exception exception = new Exception() {
            private static final long serialVersionUID = 1L;
        };

        // when
        Tag actual = underTest.exception(exception.getClass());

        // then
        assertThat(actual, is(Tag.of(EXCEPTION_METRIC_NAME, exception.getClass().getName())));
    }

    @Test
    public void testOutcomeReturnWithUnknownWhenResponseIsNull() {
        // given

        // when
        Tag actual = underTest.outcome(null);

        // then
        assertThat(actual, is(Tag.of(OUTCOME_METRIC_NAME, "UNKNOWN")));
    }

    @Test
    public void testOutcomeReturnWithUnknownWhenResponseCodeIsNull() {
        // given

        // when
        Tag actual = underTest.outcome(response);

        // then
        assertThat(actual, is(Tag.of(OUTCOME_METRIC_NAME, "SUCCESS")));
    }
}
