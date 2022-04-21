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

package org.apache.cxf.systest.brave;

public final class BraveTestSupport {
    public static final String TRACE_ID_NAME = "X-B3-TraceId";
    public static final String SPAN_ID_NAME = "X-B3-SpanId";
    public static final String PARENT_SPAN_ID_NAME = "X-B3-ParentSpanId";
    public static final String SAMPLED_NAME = "X-B3-Sampled";
    
    private BraveTestSupport() {
    }
    
    public static class SpanId {
        private long traceId;
        private long spanId;
        private Long parentId;
        private boolean sampled;
         
        public SpanId traceId(long id) {
            this.traceId = id;
            return this;
        }

        public SpanId parentId(Long id) {
            this.parentId = id;
            return this;
        }

        public SpanId spanId(long id) {
            this.spanId = id;
            return this;
        }
        
        public SpanId sampled(boolean s) {
            this.sampled = s;
            return this;
        }
        
        public long traceId() {
            return traceId;
        }
        
        public long spanId() {
            return spanId;
        }
        
        public Long parentId() {
            return parentId;
        }
        
        public boolean sampled() {
            return sampled;
        }
    }
}
