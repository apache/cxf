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

package demo.jaxrs.tracing.client;

import org.apache.cxf.tracing.opentelemetry.jaxrs.OpenTelemetryClientProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import demo.jaxrs.tracing.OpenTelemetrySetup;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public final class Client {
    private static final Logger LOG = LoggerFactory.getLogger(Client.class); 
    
    private Client() {
    }

    public static void main(final String[] args) throws Exception {
        try (final OpenTelemetrySdk otel = OpenTelemetrySetup.setup("cxf-client")) {
            final OpenTelemetryClientProvider provider = new OpenTelemetryClientProvider(otel, "cxf-client");
            final jakarta.ws.rs.client.Client client = ClientBuilder.newClient().register(provider);
            
            final Response response = client
                .target("http://localhost:8084/catalog")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
          
            LOG.info("Response: {}", response.readEntity(String.class));
            response.close();
        }
    }
}
