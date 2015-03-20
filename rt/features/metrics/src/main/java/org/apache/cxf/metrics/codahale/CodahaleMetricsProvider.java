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

package org.apache.cxf.metrics.codahale;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ObjectNameFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.metrics.MetricsContext;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;

/**
 * 
 */
@NoJSR250Annotations
public class CodahaleMetricsProvider implements MetricsProvider {
    private static final String QUESTION_MARK = "?";
    private static final String ESCAPED_QUESTION_MARK = "\\?";

    protected Bus bus;
    protected MetricRegistry registry;
    
    /**
     * 
     */
    public CodahaleMetricsProvider(Bus b) {
        this.bus = b;
        registry = b.getExtension(MetricRegistry.class);
        bus = b;
        if (registry == null) {
            registry = new MetricRegistry();
            setupJMXReporter(b, registry);
        }

    }
    
    protected final void setupJMXReporter(Bus b, MetricRegistry reg) {
        InstrumentationManager im = b.getExtension(InstrumentationManager.class);
        if (im != null) {
            JmxReporter reporter = JmxReporter.forRegistry(reg).registerWith(im.getMBeanServer())
                .inDomain("org.apache.cxf")
                .createsObjectNamesWith(new ObjectNameFactory() {
                    public ObjectName createName(String type, String domain, String name) {
                        try {
                            return new ObjectName(name);
                        } catch (MalformedObjectNameException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .build();
            reporter.start();
        }
    }

    protected String escapePatternChars(String value) {
        // This can be replaced if really needed with pattern-based matching
        if (value.lastIndexOf(QUESTION_MARK) != -1) {
            value = value.replace(QUESTION_MARK, ESCAPED_QUESTION_MARK);
        }
        return value;
    }

    StringBuilder getBaseServiceName(Endpoint endpoint, boolean asClient) {
        StringBuilder buffer = new StringBuilder();
        if (endpoint.get("org.apache.cxf.management.service.counter.name") != null) {
            buffer.append((String)endpoint.get("org.apache.cxf.management.service.counter.name"));
        } else {
            Service service = endpoint.getService();

            String serviceName = "\"" + escapePatternChars(service.getName().toString()) + "\"";
            String portName = "\"" + endpoint.getEndpointInfo().getName().getLocalPart() + "\"";

            buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME + ":");
            buffer.append(ManagementConstants.BUS_ID_PROP + "=" + bus.getId() + ",");
            buffer.append(ManagementConstants.TYPE_PROP).append("=Metrics");
            if (asClient) {
                buffer.append(".Client,");
            } else {
                buffer.append(".Server,");
            }
            buffer.append(ManagementConstants.SERVICE_NAME_PROP + "=" + serviceName + ",");

            buffer.append(ManagementConstants.PORT_NAME_PROP + "=" + portName + ",");
        }
        return buffer;
    }
    
    
    /** {@inheritDoc}*/
    @Override
    public MetricsContext createEndpointContext(final Endpoint endpoint, boolean asClient) {
        StringBuilder buffer = getBaseServiceName(endpoint, asClient);
        final String baseName = buffer.toString();
        return new CodahaleMetricsContext(baseName, registry);
    }

    /** {@inheritDoc}*/
    @Override
    public MetricsContext createOperationContext(Endpoint endpoint, BindingOperationInfo boi, boolean asClient) {
        StringBuilder buffer = getBaseServiceName(endpoint, asClient);
        buffer.append("Operation=").append(boi.getName().getLocalPart()).append(',');
        return new CodahaleMetricsContext(buffer.toString(), registry);
    }

}
