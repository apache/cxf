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

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Tracer;

import org.apache.cxf.tracing.opentracing.jaxrs.OpenTracingClientProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Client {
    private static final Logger LOG = LoggerFactory.getLogger(Client.class); 
    
    private Client() {
    }

    public static void main(final String[] args) throws Exception {
        final Tracer tracer = new Configuration("cxf-client")
            .withSampler(new SamplerConfiguration().withType(ConstSampler.TYPE).withParam(1))
            .withReporter(new ReporterConfiguration().withSender(
                new SenderConfiguration()
                    .withEndpoint("http://localhost:14268/api/traces")
            ))
            .getTracer();
        
        final OpenTracingClientProvider provider = new OpenTracingClientProvider(tracer);
        final javax.ws.rs.client.Client client = ClientBuilder.newClient().register(provider);
        
        final Response response = client
            .target("http://localhost:8084/catalog")
            .request()
            .accept(MediaType.APPLICATION_JSON)
            .get();
      
        LOG.info("Response: {}", response.readEntity(String.class));
        response.close();
          
        // Allow Tracer to flush
        Thread.sleep(1000);
    }
}
