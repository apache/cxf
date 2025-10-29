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

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.openMocks;

@SuppressWarnings("PMD.UselessPureMethodCall")
public class DefaultExceptionClassProviderTest {

    private static final Exception EXCEPTION_CAUSE = new CauseException();
    private static final Exception FAULT_EXCEPTION = new Fault(EXCEPTION_CAUSE);

    @Mock
    private Exchange exchange;
    @Mock
    private Message request;
    @Mock
    private Message faultResponse;

    private DefaultExceptionClassProvider underTest;

    @Before
    public void setUp() {
        openMocks(this);

        underTest = new DefaultExceptionClassProvider();
    }

    @Test
    public void testGetExceptionClassReturnCauseExceptionFromExchange() {
        // given
        Class<?> expected = CauseException.class;

        doReturn(FAULT_EXCEPTION).when(exchange).get(Exception.class);

        // when
        Class<?> actual = underTest.getExceptionClass(exchange, false);

        // then
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testGetExceptionClassReturnCauseExceptionFromOutFaultMessage() {
        // given
        Class<?> expected = CauseException.class;

        doReturn(faultResponse).when(exchange).getOutFaultMessage();
        doReturn(FAULT_EXCEPTION).when(faultResponse).get(Exception.class);

        // when
        Class<?> actual = underTest.getExceptionClass(exchange, false);

        // then
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testGetExceptionClassReturnCauseExceptionFromInMessage() {
        // given
        Class<?> expected = CauseException.class;

        doReturn(request).when(exchange).getInMessage();
        doReturn(FAULT_EXCEPTION).when(request).get(Exception.class);

        // when
        Class<?> actual = underTest.getExceptionClass(exchange, false);

        // then
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testGetExceptionClassReturnWithNullWhenThereIsNoCause() {
        // given
        doReturn(new RuntimeException()).when(exchange).get(Exception.class);

        // when
        Class<?> actual = underTest.getExceptionClass(exchange, false);

        // then
        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testGetExceptionClassReturnWithNullWhenThereIsNoFault() {
        // given

        // when
        Class<?> actual = underTest.getExceptionClass(exchange, false);

        // then
        assertThat(actual, is(nullValue()));
    }

    private static final class CauseException extends RuntimeException {
        private static final long serialVersionUID = 5321136931639340427L;
    }
}
