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

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.metrics.MetricsContext;
import org.apache.cxf.metrics.micrometer.provider.TagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.TagsProvider;
import org.apache.cxf.metrics.micrometer.provider.TimedAnnotationProvider;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import static java.util.stream.Stream.concat;
import static java.util.stream.StreamSupport.stream;

public class MicrometerMetricsContext implements MetricsContext {

    private static final Logger LOG =
            LogUtils.getL7dLogger(MicrometerMetricsContext.class);

    private final MeterRegistry registry;
    private final TagsProvider tagsProvider;
    private final TimedAnnotationProvider timedAnnotationProvider;
    private final List<TagsCustomizer> tagsCustomizers;

    private final String metricName;
    private final boolean autoTimeRequests;

    public MicrometerMetricsContext(MeterRegistry registry, TagsProvider tagsProvider,
                                    TimedAnnotationProvider timedAnnotationProvider,
                                    List<TagsCustomizer> tagsCustomizers, String metricName, boolean autoTimeRequests) {
        this.registry = registry;
        this.tagsProvider = tagsProvider;
        this.timedAnnotationProvider = timedAnnotationProvider;
        this.tagsCustomizers = tagsCustomizers;
        this.metricName = metricName;
        this.autoTimeRequests = autoTimeRequests;
    }

    @Override
    public void start(Exchange ex) {
        Message request = ex.getInMessage();
        TimingContext timingContext = TimingContext.get(request);
        if (timingContext == null) {
            startAndAttachTimingContext(request);
        }
    }

    @Override
    public void stop(long timeInNS, long inSize, long outSize, Exchange ex) {
        Message request = ex.getInMessage();
        TimingContext timingContext = TimingContext.get(request);
        if (timingContext == null) {
            LOG.warning("Unable for record metric for exchange: " + ex);
        } else {
            record(timingContext, ex);
        }
    }

    private void startAndAttachTimingContext(Message request) {
        Timer.Sample timerSample = Timer.start(this.registry);
        TimingContext timingContext = new TimingContext(timerSample);
        timingContext.attachTo(request);
    }

    private void record(TimingContext timingContext, Exchange ex) {
        Set<Timed> annotations = timedAnnotationProvider.getTimedAnnotations(ex);
        Timer.Sample timerSample = timingContext.getTimerSample();
        Supplier<Iterable<Tag>> tags = () -> getAllTags(ex);

        if (annotations.isEmpty()) {
            if (this.autoTimeRequests) {
                stop(timerSample, tags, Timer.builder(this.metricName));
            }
        } else {
            for (Timed annotation : annotations) {
                stop(timerSample, tags, Timer.builder(annotation, this.metricName));
            }
        }
    }

    private Iterable<Tag> getAllTags(Exchange ex) {
        Stream<Tag> defaultTags = getStreamFrom(this.tagsProvider.getTags(ex));
        Stream<Tag> additionalTags =
                tagsCustomizers.stream()
                        .map(tagsCustomizer -> tagsCustomizer.getAdditionalTags(ex))
                        .flatMap(this::getStreamFrom);

        return concat(defaultTags, additionalTags)
                .collect(Collectors.toList());
    }

    private Stream<Tag> getStreamFrom(Iterable<Tag> tags) {
        return stream(tags.spliterator(), false);
    }

    private void stop(Timer.Sample timerSample, Supplier<Iterable<Tag>> tags, Timer.Builder builder) {
        timerSample.stop(builder.tags(tags.get()).register(this.registry));
    }

    /**
     * Context object attached to a request to retain information across the multiple filter calls
     * that happen with async requests.
     */
    static class TimingContext {

        private final Timer.Sample timerSample;

        TimingContext(Timer.Sample timerSample) {
            this.timerSample = timerSample;
        }

        public Timer.Sample getTimerSample() {
            return this.timerSample;
        }

        public void attachTo(Message request) {
            request.setContent(TimingContext.class, this);
        }

        public static TimingContext get(Message request) {
            return request.getContent(TimingContext.class);
        }
    }
}
