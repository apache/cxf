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
package org.apache.cxf.tracing;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

import static org.apache.cxf.tracing.TracerHeaders.DEFAULT_HEADER_SPAN_ID;
import static org.apache.cxf.tracing.TracerHeaders.DEFAULT_HEADER_TRACE_ID;

public abstract class AbstractTracingProvider {
    protected static String getSpanIdHeader() {
        return getHeaderOrDefault(TracerHeaders.HEADER_SPAN_ID, DEFAULT_HEADER_SPAN_ID);
    }
    
    protected static String getTraceIdHeader() {
        return getHeaderOrDefault(TracerHeaders.HEADER_TRACE_ID, DEFAULT_HEADER_TRACE_ID);
    }

    private static String getHeaderOrDefault(final String property, final String fallback) {
        final Message message = PhaseInterceptorChain.getCurrentMessage();
        
        if (message != null) {
            final Object header = message.getContextualProperty(property);
            
            if (header instanceof String) {
                final String name = (String)header;
                if (!StringUtils.isEmpty(name)) {
                    return name;
                }
            }
        }
        
        return fallback;
    }
}
