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
package org.apache.cxf.systest.jaxws;

import java.io.StringReader;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;


import jakarta.annotation.Resource;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceProvider;
import org.apache.cxf.message.Message;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.Timer;


@WebServiceProvider(serviceName = "MetricsService", portName = "MetricsPort", targetNamespace = "urn:metrics")
@ServiceMode(Service.Mode.MESSAGE)
@BindingType(jakarta.xml.ws.http.HTTPBinding.HTTP_BINDING)
public class MicrometerMetricsHttpProvider implements Provider<Source> {

    @Resource
    private WebServiceContext wsContext;
    
    private final MeterRegistry meterRegistry;
    
    public MicrometerMetricsHttpProvider(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    
    @Override
    public Source invoke(Source request) {
        try {
            var ctx = wsContext.getMessageContext();

            ctx.put(Message.CONTENT_TYPE, "application/xml; charset=UTF-8");
            ctx.put(Message.RESPONSE_CODE, 200);

            StringBuilder sb = new StringBuilder(8 * 1024);
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sb.append("<metrics>\n");

            for (Meter meter : this.meterRegistry.getMeters()) {
                final Meter.Id id = meter.getId();
                final String name = id.getName();

                final long[] countHolder = new long[] {
                0L
                };

                meter.match((Gauge g) -> null, (Counter counter) -> {
                    countHolder[0] = (long)counter.count();
                    return null;
                }, (Timer timer) -> {
                    countHolder[0] = timer.count();
                    return null;
                }, (DistributionSummary summary) -> {
                    countHolder[0] = summary.count();
                    return null;
                }, (LongTaskTimer longTaskTimer) -> {
                    countHolder[0] = longTaskTimer.activeTasks();
                    return null;
                }, (TimeGauge timeGauge) -> null, (FunctionCounter functionCounter) -> {
                    countHolder[0] = (long)functionCounter.count();
                    return null;
                }, (FunctionTimer functionTimer) -> {
                    countHolder[0] = (long)functionTimer.count();
                    return null;
                }, (Meter other) -> null);

                sb.append("  <meter name=\"").append(xmlEscape(name)).append("\" count=\"")
                    .append(countHolder[0]).append("\"/>\n");
            }

            sb.append("</metrics>\n");

            return new StreamSource(new StringReader(sb.toString()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String xmlEscape(String s) {
        if (s == null) {
            return "";
        }
        String r = s;
        r = r.replace("&", "&amp;");
        r = r.replace("<", "&lt;");
        r = r.replace(">", "&gt;");
        r = r.replace("\"", "&quot;");
        r = r.replace("'", "&apos;");
        return r;
    }

}
