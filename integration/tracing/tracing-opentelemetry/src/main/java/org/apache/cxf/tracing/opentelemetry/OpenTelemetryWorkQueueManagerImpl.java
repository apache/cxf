package org.apache.cxf.tracing.opentelemetry;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.managers.WorkQueueManagerImpl;
import org.apache.cxf.workqueue.AutomaticWorkQueue;

public class OpenTelemetryWorkQueueManagerImpl extends WorkQueueManagerImpl {

    public OpenTelemetryWorkQueueManagerImpl(Bus bus) {
        super(bus);
    }

    @Override
    public synchronized AutomaticWorkQueue getAutomaticWorkQueue() {
        return new OpenTelemetryAutomaticWorkQueueImpl();
    }
}
