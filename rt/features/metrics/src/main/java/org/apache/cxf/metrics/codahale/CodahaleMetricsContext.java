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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.metrics.MetricsContext;

/**
 *
 */
public class CodahaleMetricsContext implements MetricsContext, Closeable {
    protected Counter inFlight;
    protected Timer totals;
    protected Timer uncheckedApplicationFaults;
    protected Timer checkedApplicationFaults;
    protected Timer runtimeFaults;
    protected Timer logicalRuntimeFaults;
    protected Meter incomingData;
    protected Meter outgoingData;

    protected final String baseName;
    protected final MetricRegistry registry;

    public CodahaleMetricsContext(String prefix, MetricRegistry registry) {
        baseName = prefix;
        this.registry = registry;
        totals = registry.timer(baseName + "Attribute=Totals");
        uncheckedApplicationFaults = registry.timer(baseName
                                                    + "Attribute=Unchecked Application Faults");
        checkedApplicationFaults = registry.timer(baseName + "Attribute=Checked Application Faults");
        runtimeFaults = registry.timer(baseName + "Attribute=Runtime Faults");
        logicalRuntimeFaults = registry.timer(baseName + "Attribute=Logical Runtime Faults");
        inFlight = registry.counter(baseName + "Attribute=In Flight");
        incomingData = registry.meter(baseName + "Attribute=Data Read");
        outgoingData = registry.meter(baseName + "Attribute=Data Written");
    }

    @Override
    public void close() throws IOException {
        registry.remove(baseName + "Attribute=Totals");
        registry.remove(baseName + "Attribute=Unchecked Application Faults");
        registry.remove(baseName + "Attribute=Checked Application Faults");
        registry.remove(baseName + "Attribute=Runtime Faults");
        registry.remove(baseName + "Attribute=Logical Runtime Faults");
        registry.remove(baseName + "Attribute=In Flight");
        registry.remove(baseName + "Attribute=Data Read");
        registry.remove(baseName + "Attribute=Data Written");
    }


    public void start(Exchange ex) {
        inFlight.inc();
    }

    public void stop(long timeInNS, long inSize, long outSize, Exchange ex) {
        totals.update(timeInNS, TimeUnit.NANOSECONDS);

        if (inSize != -1) {
            incomingData.mark(inSize);
        }
        if (outSize != -1) {
            outgoingData.mark(outSize);
        }
        FaultMode fm = ex.get(FaultMode.class);
        if (fm == null && ex.getOutFaultMessage() != null) {
            fm = ex.getOutFaultMessage().get(FaultMode.class);
        }
        if (fm == null && ex.getInMessage() != null) {
            fm = ex.getInMessage().get(FaultMode.class);
        }
        if (fm != null) {
            switch (fm) {
            case CHECKED_APPLICATION_FAULT:
                checkedApplicationFaults.update(timeInNS,  TimeUnit.NANOSECONDS);
                break;
            case UNCHECKED_APPLICATION_FAULT:
                uncheckedApplicationFaults.update(timeInNS,  TimeUnit.NANOSECONDS);
                break;
            case RUNTIME_FAULT:
                runtimeFaults.update(timeInNS,  TimeUnit.NANOSECONDS);
                break;
            case LOGICAL_RUNTIME_FAULT:
                logicalRuntimeFaults.update(timeInNS,  TimeUnit.NANOSECONDS);
                break;
            default:
            }
        }
        inFlight.dec();
    }

    public Counter getInFlight() {
        return inFlight;
    }

    public Timer getTotals() {
        return totals;
    }

    public Timer getUncheckedApplicationFaults() {
        return uncheckedApplicationFaults;
    }

    public Timer getCheckedApplicationFaults() {
        return checkedApplicationFaults;
    }

    public Timer getRuntimeFaults() {
        return runtimeFaults;
    }

    public Timer getLogicalRuntimeFaults() {
        return logicalRuntimeFaults;
    }

    public Meter getIncomingData() {
        return incomingData;
    }

    public Meter getOutgoingData() {
        return outgoingData;
    }

    public String getBaseName() {
        return baseName;
    }

    public MetricRegistry getRegistry() {
        return registry;
    }

}
