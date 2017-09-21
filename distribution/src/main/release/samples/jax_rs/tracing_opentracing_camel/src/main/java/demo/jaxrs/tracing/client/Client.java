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

import com.uber.jaeger.Tracer;
import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsFactoryImpl;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.samplers.ConstSampler;
import com.uber.jaeger.senders.HttpSender;

import org.apache.cxf.tracing.opentracing.jaxrs.OpenTracingClientProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Client {
    private static final Logger LOG = LoggerFactory.getLogger(Client.class); 
    
    private Client() {
    }

    public static void main(final String[] args) throws Exception {
        final Metrics metrics = new Metrics(new StatsFactoryImpl(new NullStatsReporter()));
        
        final Tracer.Builder builder = new Tracer.Builder(
                "cxf-client", 
                new RemoteReporter(new HttpSender("http://localhost:14268/api/traces"), 1000, 100, metrics),
                new ConstSampler(true)
            );
        
        final OpenTracingClientProvider provider = new OpenTracingClientProvider(builder.build());
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
