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

package demo.jaxrs.tracing.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.apache.cxf.tracing.htrace.jaxrs.HTraceFeature;
import org.apache.htrace.core.AlwaysSampler;
import org.apache.htrace.core.HTraceConfiguration;
import org.apache.htrace.core.Tracer;

import demo.jaxrs.tracing.conf.TracingConfiguration;

@ApplicationPath("/")
public class CatalogApplication extends Application {
    @Override
    public Set<Object> getSingletons() {
        try {
            return new HashSet<Object>(
                Arrays.asList(
                    new Catalog(),
                    new HTraceFeature(HTraceConfiguration.fromMap(getTracingProperties()), "catalog-server"),
                    new JsrJsonpProvider()
                )
            );
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to initaliaze JAX-RS application", ex);
        }
    }
    
    private static Map<String, String> getTracingProperties() {
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put(Tracer.SPAN_RECEIVER_CLASSES_KEY, TracingConfiguration.SPAN_RECEIVER.getName());
        properties.put(Tracer.SAMPLER_CLASSES_KEY, AlwaysSampler.class.getName());
        return properties;
    }
}
