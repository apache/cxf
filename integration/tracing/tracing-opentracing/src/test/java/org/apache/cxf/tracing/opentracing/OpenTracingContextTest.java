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

import io.opentracing.ScopeManager;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class OpenTracingContextTest {
    private OpenTracingContext context; 
    private Tracer tracer;

    @Before
    public void setUp() {
        tracer = new MockTracer();
        context = new OpenTracingContext(tracer);
    }
    
    @Test
    public void testUnwrapTracer() {
        assertThat(context.unwrap(Tracer.class), sameInstance(tracer));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testUnwrapUnsupportedClass() {
        context.unwrap(ScopeManager.class);
    }

}
