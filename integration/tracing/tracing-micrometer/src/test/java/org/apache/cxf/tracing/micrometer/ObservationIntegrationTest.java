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
package org.apache.cxf.tracing.micrometer;

import java.util.Collections;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.test.SampleTestRunner;
import io.micrometer.tracing.test.simple.SpansAssert;

import org.junit.jupiter.api.AfterEach;


public class ObservationIntegrationTest extends SampleTestRunner {

    private static final String ADDRESS = "http://localhost:9282";
    private Server server;
    private ObservationFeature logging;
    private ObservationClientFeature clientLogging;

    @AfterEach
    public void stopServer() {
        if (server != null) {
            server.destroy();
        }
    }

    private static Server createServer(ObservationRegistry observationRegistry, Feature feature) {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setAddress(ADDRESS);
        factory.setServiceBean(new MyServiceImpl(observationRegistry));
        factory.setFeatures(Collections.singletonList(feature));
        return factory.create();
    }

    private static MyService createProxy(Feature feature) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(MyService.class);
        factory.setAddress(ADDRESS);
        factory.setFeatures(Collections.singletonList(feature));
        return (MyService)factory.create();
    }

    @Override
    public SampleTestRunnerConsumer yourCode() throws Exception {
        return (buildingBlocks, meterRegistry) -> {
            logging = new ObservationFeature(getObservationRegistry());
            clientLogging = new ObservationClientFeature(getObservationRegistry());
            server = createServer(getObservationRegistry(), logging);

            MyService myService = createProxy(clientLogging);
            myService.echo("test");

            for (FinishedSpan span : buildingBlocks.getFinishedSpans()) {
                System.out.println(span);
            }

            SpansAssert.then(buildingBlocks.getFinishedSpans())
                    .haveSameTraceId()
                       .hasSize(3);
            MeterRegistryAssert.assertThat(getMeterRegistry())
                    .hasMeterWithName("rpc.server.duration")
                    .hasMeterWithName("rpc.client.duration");
        };
    }

}
