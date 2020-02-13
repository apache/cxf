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

package org.apache.cxf.metrics.micrometer;

import java.util.Collections;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.metrics.micrometer.MicrometerMetricsContext.TimingContext;
import org.apache.cxf.metrics.micrometer.provider.TagsProvider;
import org.apache.cxf.metrics.micrometer.provider.TimedAnnotationProvider;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldReader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class MicrometerMetricsContextTest {

    private static final long DUMMY_LONG = 0L;
    private static final Tag DUMMY_TAG = Tag.of("dummyKey", "dummyValue");
    private static final String DUMMY_METRIC = "dummyMetric";

    @Captor
    ArgumentCaptor<TimingContext> timingContextCaptor;
    @Captor
    ArgumentCaptor<Timer> timerArgumentCaptor;

    @Mock
    private Timer.Sample sample;
    @Mock
    private TagsProvider tagsProvider;
    @Mock
    private TimedAnnotationProvider timedAnnotationProvider;

    @Mock
    private Message request;
    @Mock
    private Exchange exchange;

    private MeterRegistry registry = new SimpleMeterRegistry();

    private MicrometerMetricsContext underTest;

    @Before
    public void setUp() {
        initMocks(this);

        doReturn(request).when(exchange).getInMessage();
        doReturn(Collections.singletonList(DUMMY_TAG)).when(tagsProvider).getTags(exchange);

        underTest =
                new MicrometerMetricsContext(
                        registry, tagsProvider, timedAnnotationProvider, DUMMY_METRIC, true);
    }

    @Test
    public void testStartAttachTimingContext() throws NoSuchFieldException {
        // given in setUp

        // when
        underTest.start(exchange);

        // then
        verify(request).setContent(eq(TimingContext.class), timingContextCaptor.capture());

        Object actual = getTimer(timingContextCaptor);

        assertThat(actual, equalTo(registry.config().clock()));
    }

    @Test
    public void testStopCallsStopOnTimer() {
        // given
        TimingContext timingContext = new TimingContext(sample);

        doReturn(timingContext).when(request).getContent(TimingContext.class);

        // when
        underTest.stop(DUMMY_LONG, DUMMY_LONG, DUMMY_LONG, exchange);

        // then
        verify(sample).stop(timerArgumentCaptor.capture());

        Meter.Id id = timerArgumentCaptor.getValue().getId();

        assertThat(id.getName(), equalTo(DUMMY_METRIC));
        assertThat(id.getTags(), contains(DUMMY_TAG));
    }

    private Object getTimer(ArgumentCaptor<TimingContext> content) throws NoSuchFieldException {
        Timer.Sample timerSample = content.getValue().getTimerSample();

        return new FieldReader(timerSample, timerSample.getClass().getDeclaredField("clock")).read();
    }
}
