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
package org.apache.cxf.tracing.brave.jaxrs;

import java.util.UUID;

import brave.Tracing;
import brave.http.HttpTracing;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BraveFeature implements Feature {
    private final HttpTracing brave;

    public BraveFeature() {
        this("cxf-svc-" + UUID.randomUUID().toString());
    }

    public BraveFeature(final String name) {
        this(
            HttpTracing
                .newBuilder(Tracing.newBuilder().localServiceName(name).build())
                .build()
        );
    }

    public BraveFeature(final Tracing tracing) {
        this(
          HttpTracing
              .newBuilder(tracing)
              .build()
        );
    }

    public BraveFeature(final HttpTracing brave) {
        this.brave = brave;
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new BraveProvider(brave));
        context.register(new BraveContextProvider(brave));
        return true;
    }
}
