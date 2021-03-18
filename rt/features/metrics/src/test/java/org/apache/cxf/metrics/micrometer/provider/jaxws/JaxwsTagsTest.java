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


public class JaxwsTagsTest {

    private static final String OPERATION_METRIC_NAME = "operation";
    private static final String FAULTCODE_METRIC_NAME = "faultCode";

    private static final String DUMMY_OPERATOR = "getOperator";
    private static final QName DUMMY_OPERATION_QNAME = new QName("http://namespace", DUMMY_OPERATOR);

    private JaxwsTags underTest;

    @Mock
    private Message request;
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

        underTest = new JaxwsTags();
    }

    @Test
    public void testOperationReturnWithUnknownWhenRequestIsNull() {
        // given

        // when
        Tag actual = underTest.operation(null);

        // then
        assertThat(actual, is(Tag.of(OPERATION_METRIC_NAME, "UNKNOWN")));
    }

    @Test
    public void testOperationReturnWithCorrectValue() {
        // given

        // when
        Tag actual = underTest.operation(request);

        // then
        assertThat(actual, is(Tag.of(OPERATION_METRIC_NAME, DUMMY_OPERATOR)));
    }

    @Test
    public void testFaultCodeReturnWithUnknownWhenFaultCodeIsNull() {
        // given

        // when
        Tag actual = underTest.faultCode(null);

        // then
        assertThat(actual, is(Tag.of(FAULTCODE_METRIC_NAME, "None")));
    }

    @Test
    public void testFaultCodeReturnWithCorrectValue() {
        // given
        String dummyFaultCode = "dummyFaultCode";

        // when
        Tag actual = underTest.faultCode(dummyFaultCode);

        // then
        assertThat(actual, is(Tag.of(FAULTCODE_METRIC_NAME, dummyFaultCode)));
    }
}
