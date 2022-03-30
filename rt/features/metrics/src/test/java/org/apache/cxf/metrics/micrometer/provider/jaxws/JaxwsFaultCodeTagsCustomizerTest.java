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

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.openMocks;

public class JaxwsFaultCodeTagsCustomizerTest {

    private static final ImmutableTag DUMMY_TAG = new ImmutableTag("dummyKey", "dummyValue");
    private static final String DUMMY_FAULT_CODE = "dummyFaultCode";

    private JaxwsFaultCodeTagsCustomizer underTest;

    @Mock
    private Exchange ex;

    @Mock
    private JaxwsTags jaxwsTags;
    @Mock
    private JaxwsFaultCodeProvider jaxwsFaultCodeProvider;

    @Before
    public void setUp() {
        openMocks(this);
        underTest = new JaxwsFaultCodeTagsCustomizer(jaxwsTags, jaxwsFaultCodeProvider);
    }

    @Test
    public void testAdditionalTagsShouldReturnFaultCodeAsTags() {
        // given
        doReturn(DUMMY_FAULT_CODE).when(jaxwsFaultCodeProvider).getFaultCode(ex, false);
        doReturn(DUMMY_TAG).when(jaxwsTags).faultCode(DUMMY_FAULT_CODE);

        // when
        Iterable<Tag> actual = underTest.getAdditionalTags(ex, false);

        // then
        assertThat(actual, equalTo(Tags.of(DUMMY_TAG)));
    }
}