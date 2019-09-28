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

package org.apache.cxf.bus.managers;

import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.annotation.ManagedAttribute;
import org.apache.cxf.management.annotation.ManagedResource;
import org.apache.cxf.workqueue.AutomaticWorkQueueImpl;
import org.apache.cxf.workqueue.WorkQueueManager;

@ManagedResource(componentName = "WorkQueue",
                 description = "The CXF work queue",
                 currencyTimeLimit = 15, persistPolicy = "OnUpdate", persistPeriod = 200)

public class WorkQueueImplMBeanWrapper implements ManagedComponent {
    private static final String TYPE_VALUE = "WorkQueues";

    private final AutomaticWorkQueueImpl aWorkQueue;
    private final String objectName;

    public WorkQueueImplMBeanWrapper(AutomaticWorkQueueImpl wq,
                                     WorkQueueManager mgr) {
        aWorkQueue = wq;

        //Use default domain name of server
        StringBuilder sb = new StringBuilder(ManagementConstants.DEFAULT_DOMAIN_NAME).append(':');
        if (!aWorkQueue.isShared()) {
            String busId = Bus.DEFAULT_BUS_ID;
            if (mgr instanceof WorkQueueManagerImpl) {
                busId = ((WorkQueueManagerImpl) mgr).getBus().getId();
            }
            sb  .append(ManagementConstants.BUS_ID_PROP).append('=').append(busId).append(',')
                .append(WorkQueueManagerImplMBeanWrapper.TYPE_VALUE).append('=')
                .append(WorkQueueManagerImplMBeanWrapper.NAME_VALUE).append(',');
        } else {
            sb.append(ManagementConstants.BUS_ID_PROP).append("=Shared,");
            //buffer.append(WorkQueueManagerImplMBeanWrapper.TYPE_VALUE).append("=Shared,");
        }
        sb  .append(ManagementConstants.TYPE_PROP).append('=').append(TYPE_VALUE).append(',')
            .append(ManagementConstants.NAME_PROP).append('=').append(aWorkQueue.getName()).append(',')
            // Added the instance id to make the ObjectName unique
            .append(ManagementConstants.INSTANCE_ID_PROP).append('=').append(aWorkQueue.hashCode());
        objectName = sb.toString();
    }

    @ManagedAttribute(description = "The WorkQueueMaxSize",
                      persistPolicy = "OnUpdate")
    public long getWorkQueueMaxSize() {
        return aWorkQueue.getMaxSize();
    }

    @ManagedAttribute(description = "The WorkQueue Current size",
                      persistPolicy = "OnUpdate")
    public long getWorkQueueSize() {
        return aWorkQueue.getSize();
    }

    @ManagedAttribute(description = "The largest number of threads")
    public int getLargestPoolSize() {
        return aWorkQueue.getLargestPoolSize();
    }

    @ManagedAttribute(description = "The current number of threads")
    public int getPoolSize() {
        return aWorkQueue.getPoolSize();
    }

    @ManagedAttribute(description = "The number of threads currently busy")
    public int getActiveCount() {
        return aWorkQueue.getActiveCount();
    }

    @ManagedAttribute(description = "The WorkQueue has nothing to do",
                      persistPolicy = "OnUpdate")
    public boolean isEmpty() {
        return aWorkQueue.isEmpty();
    }

    @ManagedAttribute(description = "The WorkQueue is very busy")
    public boolean isFull() {
        return aWorkQueue.isFull();
    }

    @ManagedAttribute(description = "The WorkQueue HighWaterMark",
                      persistPolicy = "OnUpdate")
    public int getHighWaterMark() {
        return aWorkQueue.getHighWaterMark();
    }
    public void setHighWaterMark(int hwm) {
        aWorkQueue.setHighWaterMark(hwm);
    }

    @ManagedAttribute(description = "The WorkQueue LowWaterMark",
                      persistPolicy = "OnUpdate")
    public int getLowWaterMark() {
        return aWorkQueue.getLowWaterMark();
    }

    public void setLowWaterMark(int lwm) {
        aWorkQueue.setLowWaterMark(lwm);
    }

    public ObjectName getObjectName() throws JMException {
        return new ObjectName(objectName);
    }

}
