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

import org.apache.htrace.core.Span;
import org.apache.htrace.core.Tracer;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logback conversion rule implementation to enrich log records with tracing details like spanId and tracerId. 
 * For example, here is sample logback.xml configuration snippet:
 * 
 *  <conversionRule conversionWord="trace" converterClass="org.apache.cxf.tracing.htrace.ext.LogbackSpanConverter" />
 * 
 *  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
 *      <encoder>
 *          <pattern>[%level] [%trace] %d{yyyy-MM-dd HH:mm:ss.SSS} %logger{36} %msg%n</pattern>
 *      </encoder>
 *  </appender>
 * 
 * Which produces the following sample output:
 *  
 *  [INFO] [spanId: -, tracerId: -] 2017-03-11 14:40:13.603 org.eclipse.jetty.server.Server Started @2731ms
 *  [INFO] [spanId: 6d3e0d975d4c883cce12aee1fd8f3e7e, tracerId: tracer-server/192.168.0.101] 2017-03-11 14:40:24.013 
 *     com.example.rs.PeopleRestService Getting all employees
 *  [INFO] [spanId: 6d3e0d975d4c883c7592f4c2317dec22, tracerId: tracer-server/192.168.0.101] 2017-03-11 14:40:28.017 
 *     com.example.rs.PeopleRestService Looking up manager in the DB database
 *
 */
public class LogbackSpanConverter extends ClassicConverter {
    private static final String TRACER_ID = "tracerId";
    private static final String SPAN_ID = "spanId";
    private static final String EMPTY_TRACE = String.format("%s: -, %s: -", SPAN_ID, TRACER_ID);

    @Override
    public String convert(ILoggingEvent event) {
        final Span currentSpan = Tracer.getCurrentSpan();
        
        if (currentSpan != null) {
            return new StringBuilder()
                .append(SPAN_ID)
                .append(": ")
                .append(currentSpan.getSpanId())
                .append(", ")
                .append(TRACER_ID)
                .append(": ")
                .append(currentSpan.getTracerId())
                .toString();
        }
        
        return EMPTY_TRACE;
    }
}
