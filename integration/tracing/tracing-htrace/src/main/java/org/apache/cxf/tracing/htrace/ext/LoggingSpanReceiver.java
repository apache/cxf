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

package org.apache.cxf.tracing.htrace.ext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.htrace.core.HTraceConfiguration;
import org.apache.htrace.core.Span;
import org.apache.htrace.core.SpanReceiver;
import org.apache.htrace.core.TimelineAnnotation;

/**
 * Span receiver implementation which outputs spans into logs. 
 */
public class LoggingSpanReceiver extends SpanReceiver {
    public static final String LOG_LEVEL_KEY = "cxf.log.level";
    public static final String LOG_LEVEL_ERROR = Level.SEVERE.getName();
    public static final String LOG_LEVEL_DEBUG = Level.FINE.getName();
    public static final String LOG_LEVEL_INFO = Level.INFO.getName();
    public static final String LOG_LEVEL_WARN = Level.WARNING.getName();

    private interface Stringifier<T> {
        String toString(T value);
    }
    
    private static final Logger LOG = LogUtils.getL7dLogger(LoggingSpanReceiver.class);
    private final Level level;

    private final Stringifier<TimelineAnnotation> timelineStringifier = new Stringifier<TimelineAnnotation>() {
        @Override
        public String toString(TimelineAnnotation annotation) {
            final StringBuilder sb = new StringBuilder();
            append(sb, "time", annotation.getTime());
            append(sb, "message", annotation.getMessage(), true);
            return "[" + sb.toString() + "]";
        }
    };

    public LoggingSpanReceiver(HTraceConfiguration conf) {
        level = Level.parse(conf.get(LOG_LEVEL_KEY, LOG_LEVEL_DEBUG));
    }

    @Override
    public void receiveSpan(Span span) {
        LOG.log(level, toString(span));
    }

    @Override
    public void close() throws IOException {
    }
    
    
    /**
     * Sample log statements:
     * 
     * INFO org.apache.cxf.tracing.htrace.ext.LoggingSpanReceiver - spanId=e5999a29a1ea468201acac30ec04ae39 
     *   tracerId="tracer-server/192.168.0.100" start=1488049449621 stop=1488049451623 description="Get Employees" 
     *   parents=[e5999a29a1ea4682d346ae17e51e0bd4] kvs=[] timelines=[[time=1488049451623 
     *   message="Getting all employees"]]
     * 
     * INFO org.apache.cxf.tracing.htrace.ext.LoggingSpanReceiver - spanId=e5999a29a1ea4682ac0a9ad638e084ed 
     *   tracerId="tracer-client/192.168.0.100" start=1488049449074 stop=1488049454894 
     *   description="GET http://localhost:8282/rest/api/people" parents=[] kvs=[] timelines=[]
     */
    
    
    private String toString(Span span) {
        final StringBuilder sb = new StringBuilder();
        
        if (span.getSpanId().isValid()) {
            append(sb, "spanId", span.getSpanId().toString());
        }
        
        String tracerId = span.getTracerId();
        if (!StringUtils.isEmpty(tracerId)) {
            append(sb, "tracerId", tracerId, true);
        }
        
        if (span.getStartTimeMillis() != 0) {
            append(sb, "start", span.getStartTimeMillis());
        }
        
        if (span.getStopTimeMillis() != 0) {
            append(sb, "stop", span.getStopTimeMillis());
        }
        
        if (!StringUtils.isEmpty(span.getDescription())) {
            append(sb, "description", span.getDescription(), true);
        }
        
        append(sb, "parents", span.getParents());
        append(sb, "kvs", span.getKVAnnotations());
        append(sb, "timelines", span.getTimelineAnnotations(), timelineStringifier);
        
        return sb.toString();
    }

    private void append(StringBuilder sb, String key, Map<String, String> values) {
        final StringBuilder inner = new StringBuilder();
        
        for (final Map.Entry<String, String> entry : values.entrySet()) {
            append(inner, quote(entry.getKey()), entry.getValue(), true);
        }

        append(sb, key, inner.insert(0, "[").append("]").toString());
    }
    
    private<T> void append(StringBuilder sb, String key, Collection<T> values, Stringifier<T> stringifier) {
        append(sb, key, toString(values, stringifier));
    }
    
    private<T> void append(StringBuilder sb, String key, T[] values) {
        append(sb, key, toString(values));
    }
    
    private void append(StringBuilder sb, String key, long value) {
        append(sb, key, Long.toString(value));
    }
    
    private void append(StringBuilder sb, String key, String value) {
        append(sb, key, value, false);
    }
    
    private void append(StringBuilder sb, String key, String value, boolean quoted) {
        if (sb.length() > 0) {
            sb.append(" ");
        }
        
        sb.append(key).append("=");
        quote(sb, value, quoted);
    }

    private String quote(String value) {
        final StringBuilder sb = new StringBuilder();
        quote(sb, value, true);
        return sb.toString();
    }
    
    private<T> String toString(Collection<T> values, Stringifier<T> stringifier) {
        final Collection<String> converted = new ArrayList<>();
        
        for (final T value: values) {
            converted.add(stringifier.toString(value));
        }
        
        return Arrays.toString(converted.toArray());
    }
    
    private<T> String toString(T[] values) {
        final Collection<String> converted = new ArrayList<>();
        
        for (final T value: values) {
            converted.add(value.toString());
        }
        
        return Arrays.toString(converted.toArray());
    }
    
    private void quote(StringBuilder sb, String value, boolean quoted) {
        if (quoted) {
            sb.append("\"");
        }
        
        sb.append(value);
        if (quoted) {
            sb.append("\"");
        }
    }
}
