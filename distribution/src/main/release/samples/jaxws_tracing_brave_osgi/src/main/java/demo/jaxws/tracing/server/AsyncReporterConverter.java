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

package demo.jaxws.tracing.server;

import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;

import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;

/**
 * Converts generic AsyncReporter<?> to AsyncReporter<Span> (see please
 * https://issues.apache.org/jira/browse/ARIES-1607, https://issues.apache.org/jira/browse/ARIES-960
 * and https://issues.apache.org/jira/browse/ARIES-1500)
 *  
 */
public class AsyncReporterConverter implements Converter {
    @Override
    public boolean canConvert(Object source, ReifiedType target) {
        return source instanceof AsyncReporter<?> && target.getRawClass() == AsyncReporter.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object convert(Object source, ReifiedType target) throws Exception {
        if (source instanceof AsyncReporter<?>) {
            return (AsyncReporter<Span>)source;
        } else {
            throw new RuntimeException("Unable to convert from " + source + " to " + target);
        }
    }
}
