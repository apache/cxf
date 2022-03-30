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

import org.apache.cxf.message.Exchange;
import org.apache.cxf.metrics.micrometer.provider.TagsCustomizer;
import org.apache.cxf.metrics.micrometer.provider.TagsProvider;
import org.apache.cxf.metrics.micrometer.provider.TimedAnnotationProvider;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

public class MicrometerServerMetricsContext extends MicrometerMetricsContext {
    public MicrometerServerMetricsContext(MeterRegistry registry, TagsProvider tagsProvider,
                                    TimedAnnotationProvider timedAnnotationProvider,
                                    List<TagsCustomizer> tagsCustomizers, String metricName, boolean autoTimeRequests) {
        super(registry, tagsProvider, timedAnnotationProvider, tagsCustomizers, metricName, autoTimeRequests);
    }

    @Override
    public void start(Exchange ex) {
        super.start(ex.getInMessage(), ex);
    }

    @Override
    public void stop(long timeInNS, long inSize, long outSize, Exchange ex) {
        super.stop(ex.getInMessage(), timeInNS, inSize, outSize, ex);
    }
    
    @Override
    protected Iterable<Tag> getAllTags(Exchange ex) {
        return getAllTags(ex, false);
    }
    
    @Override
    protected void record(TimingContext timingContext, Exchange ex) {
        super.record(timingContext, ex, false);
    }
}
