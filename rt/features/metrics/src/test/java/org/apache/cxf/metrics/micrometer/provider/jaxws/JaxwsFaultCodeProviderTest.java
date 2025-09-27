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
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.apache.cxf.message.FaultMode.RUNTIME_FAULT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.openMocks;

@SuppressWarnings("PMD.UselessPureMethodCall")
public class JaxwsFaultCodeProviderTest {

    private static final String RUNTIME_FAULT_STRING = "RUNTIME_FAULT";

    private JaxwsFaultCodeProvider underTest;

    @Mock
    private Exchange ex;
    @Mock
    private Message message;

    @Before
    public void setUp() {
        openMocks(this);
        underTest = new JaxwsFaultCodeProvider();
    }

    @Test
    public void testReturnTheTypeOfFaultModeIfPresent() {
        // given
        doReturn(RUNTIME_FAULT).when(ex).get(FaultMode.class);

        // when
        String actual = underTest.getFaultCode(ex, false);

        // then
        assertThat(actual, equalTo(RUNTIME_FAULT_STRING));
    }

    @Test
    public void testFaultModeIsNotPresentButOutFaultModeIsPresentThenShouldReturnThat() {
        // given
        doReturn(null).when(ex).get(FaultMode.class);
        doReturn(message).when(ex).getOutFaultMessage();
        doReturn(RUNTIME_FAULT).when(message).get(FaultMode.class);

        // when
        String actual = underTest.getFaultCode(ex, false);

        // then
        assertThat(actual, equalTo(RUNTIME_FAULT_STRING));
    }
    
    @Test
    public void testFaultModeIsNotPresentButInFaultModeIsPresentThenShouldReturnThat() {
        // given
        doReturn(null).when(ex).get(FaultMode.class);
        doReturn(message).when(ex).getInFaultMessage();
        doReturn(RUNTIME_FAULT).when(message).get(FaultMode.class);

        // when
        String actual = underTest.getFaultCode(ex, true);

        // then
        assertThat(actual, equalTo(RUNTIME_FAULT_STRING));
    }


    @Test
    public void testFaultModeIsNotPresentButOutFaultModeIsMissingThenShouldReturnNull() {
        // given
        doReturn(null).when(ex).get(FaultMode.class);
        doReturn(message).when(ex).getOutFaultMessage();

        // when
        String actual = underTest.getFaultCode(ex, false);

        // then
        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testNeitherFaultModeNorOutFaultModePresentsThenShouldNotReturnInMessageFaultMode() {
        // given
        doReturn(null).when(ex).get(FaultMode.class);
        doReturn(null).when(ex).getOutFaultMessage();
        doReturn(message).when(ex).getInMessage();
        doReturn(RUNTIME_FAULT).when(message).get(FaultMode.class);

        // when
        String actual = underTest.getFaultCode(ex, false);

        // then
        assertThat(actual, is(nullValue()));
    }
    
    @Test
    public void testNeitherFaultModeNorOutFaultModePresentsThenShouldNotReturnOutMessageFaultMode() {
        // given
        doReturn(null).when(ex).get(FaultMode.class);
        doReturn(null).when(ex).getInFaultMessage();
        doReturn(message).when(ex).getOutMessage();
        doReturn(RUNTIME_FAULT).when(message).get(FaultMode.class);

        // when
        String actual = underTest.getFaultCode(ex, true);

        // then
        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testFaultModeIsNotPresentButInModeIsMissingThenShouldReturnNull() {
        // given
        doReturn(null).when(ex).get(FaultMode.class);
        doReturn(null).when(ex).getOutFaultMessage();
        doReturn(message).when(ex).getInMessage();

        // when
        String actual = underTest.getFaultCode(ex, false);

        // then
        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testCannotGetFaultModeThanShouldReturnNull() {
        // given
        doReturn(null).when(ex).get(FaultMode.class);
        doReturn(null).when(ex).getOutFaultMessage();
        doReturn(null).when(ex).getInMessage();

        // when
        String actual = underTest.getFaultCode(ex, false);

        // then
        assertThat(actual, is(nullValue()));
    }
}
