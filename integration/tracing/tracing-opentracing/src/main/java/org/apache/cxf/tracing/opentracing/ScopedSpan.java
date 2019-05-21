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

package org.apache.cxf.tracing.opentracing;

import io.opentracing.Scope;
import io.opentracing.Span;

/**
 * The instance of the Scope which is finishing the Span upon close() call. 
 *
 */
class ScopedSpan implements Scope {
    private final Span span;
    private final Scope scope;
    
    ScopedSpan(final Span span, final Scope scope) {
        this.span = span;
        this.scope = scope;
    }

    @Override
    public void close() {
        span.finish();
        scope.close();
    }
}
