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

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.metrics.micrometer.MicrometerMetricsContext.TimingContext;
import org.apache.cxf.metrics.micrometer.provider.TagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.TagsProvider;
import org.apache.cxf.metrics.micrometer.provider.TimedAnnotationProvider;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldReader;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

public class MicrometerClientMetricsContextTest {

    private static final long DUMMY_LONG = 0L;
    private static final Tag DEFAULT_DUMMY_TAG = Tag.of("defaultDummyKey", "dummyValue");
    private static final Tag FIRST_ADDITIONAL_DUMMY_TAG = Tag.of("firstAdditionalDummyKey", "dummyValue");
    private static final Tag SECOND_ADDITIONAL_DUMMY_TAG = Tag.of("secondAdditionalDummyKey", "dummyValue");

    private static final String DUMMY_METRIC = "dummyMetric";
    private static final String FIRST_TIMED_ANNOTATION_DUMMY_VALUE = "firstTimedAnnotationDummyValue";
    private static final String SECOND_TIMED_ANNOTATION_DUMMY_VALUE = "secondTimedAnnotationDummyValue";

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

    @Mock
    private Timed firstTimedAnnotation;
    @Mock
    private Timed secondTimedAnnotation;

    @Mock
    private TagsCustomizer firstTagsCustomizer;
    @Mock
    private TagsCustomizer secondTagsCustomizer;

    private MeterRegistry registry = new SimpleMeterRegistry();

    private MicrometerMetricsContext underTest;

    @Before
    public void setUp() {
        openMocks(this);

        doReturn(request).when(exchange).getOutMessage();
        doReturn(singletonList(DEFAULT_DUMMY_TAG)).when(tagsProvider).getTags(exchange, true);
        doReturn(singletonList(FIRST_ADDITIONAL_DUMMY_TAG)).when(firstTagsCustomizer)
            .getAdditionalTags(exchange, true);
        doReturn(singletonList(SECOND_ADDITIONAL_DUMMY_TAG)).when(secondTagsCustomizer)
            .getAdditionalTags(exchange, true);

        underTest = new MicrometerClientMetricsContext(registry, tagsProvider, timedAnnotationProvider,
            asList(firstTagsCustomizer, secondTagsCustomizer), DUMMY_METRIC);
    }

    @Test
    public void testStartShouldAttachTimingContext() throws NoSuchFieldException {
        // given in setUp

        // when
        underTest.start(exchange);

        // then
        verify(request).setContent(eq(TimingContext.class), timingContextCaptor.capture());

        Object actual = getTimer(timingContextCaptor);

        assertThat(actual, equalTo(registry.config().clock()));
    }

    @Test
    public void testStopShouldCallStopOnTimer() {
        // given
        TimingContext timingContext = new TimingContext(sample);

        doReturn(timingContext).when(request).getContent(TimingContext.class);

        // when
        underTest.stop(DUMMY_LONG, DUMMY_LONG, DUMMY_LONG, exchange);

        // then
        verify(sample).stop(timerArgumentCaptor.capture());

        Meter.Id id = timerArgumentCaptor.getValue().getId();

        assertThat(id.getName(), equalTo(DUMMY_METRIC));
        assertThat(id.getTags(), contains(DEFAULT_DUMMY_TAG, FIRST_ADDITIONAL_DUMMY_TAG, SECOND_ADDITIONAL_DUMMY_TAG));
    }

    @Test
    public void testStopShouldNotRecordWhenTimerIsMissing() {
        // given

        // when
        underTest.stop(DUMMY_LONG, DUMMY_LONG, DUMMY_LONG, exchange);

        // then
        verifyNoInteractions(sample);
    }

    @Test
    public void testStopShouldCallStopOnAllTimedAnnotations() {
        // given
        doReturn(new HashSet<>(asList(firstTimedAnnotation, secondTimedAnnotation)))
                .when(timedAnnotationProvider).getTimedAnnotations(exchange, true);

        doReturn(FIRST_TIMED_ANNOTATION_DUMMY_VALUE).when(firstTimedAnnotation).value();
        doReturn("").when(firstTimedAnnotation).description();
        doReturn(new double[]{}).when(firstTimedAnnotation).percentiles();
        doReturn(new double[]{}).when(firstTimedAnnotation).serviceLevelObjectives();


        doReturn(SECOND_TIMED_ANNOTATION_DUMMY_VALUE).when(secondTimedAnnotation).value();
        doReturn("").when(secondTimedAnnotation).description();
        doReturn(new double[]{}).when(secondTimedAnnotation).percentiles();
        doReturn(new double[]{}).when(secondTimedAnnotation).serviceLevelObjectives();

        TimingContext timingContext = new TimingContext(sample);

        doReturn(timingContext).when(request).getContent(TimingContext.class);

        // when
        underTest.stop(DUMMY_LONG, DUMMY_LONG, DUMMY_LONG, exchange);

        // then
        verify(sample, times(2)).stop(timerArgumentCaptor.capture());

        List<Meter.Id> timers = timerArgumentCaptor.getAllValues().stream().map(Meter::getId)
                .collect(Collectors.toList());

        assertThat(timers, hasItems(
                new Meter.Id(FIRST_TIMED_ANNOTATION_DUMMY_VALUE,
                        Tags.of(DEFAULT_DUMMY_TAG, FIRST_ADDITIONAL_DUMMY_TAG, SECOND_ADDITIONAL_DUMMY_TAG),
                        null,
                        null,
                        Meter.Type.OTHER),
                new Meter.Id(SECOND_TIMED_ANNOTATION_DUMMY_VALUE,
                        Tags.of(DEFAULT_DUMMY_TAG, FIRST_ADDITIONAL_DUMMY_TAG, SECOND_ADDITIONAL_DUMMY_TAG),
                        null,
                        null,
                        Meter.Type.OTHER)));
    }

    private Object getTimer(ArgumentCaptor<TimingContext> content) throws NoSuchFieldException {
        Timer.Sample timerSample = content.getValue().getTimerSample();

        return new FieldReader(timerSample, timerSample.getClass().getDeclaredField("clock")).read();
    }
}
