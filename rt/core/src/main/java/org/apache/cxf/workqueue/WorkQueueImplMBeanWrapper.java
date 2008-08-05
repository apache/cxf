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

package org.apache.cxf.workqueue;

import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.annotation.ManagedAttribute;
import org.apache.cxf.management.annotation.ManagedResource;

@ManagedResource(componentName = "WorkQueue", 
                 description = "The CXF work queue", 
                 currencyTimeLimit = 15, persistPolicy = "OnUpdate", persistPeriod = 200)
                 
public class WorkQueueImplMBeanWrapper implements ManagedComponent {    
    private static final String TYPE_VALUE = "WorkQueues";
    
    private AutomaticWorkQueueImpl aWorkQueue;
    
    public WorkQueueImplMBeanWrapper(AutomaticWorkQueueImpl wq) {
        aWorkQueue = wq;        
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
        WorkQueueManager mgr = aWorkQueue.getManager();
        String busId = "cxf";
        if (mgr instanceof WorkQueueManagerImpl) {
            busId = ((WorkQueueManagerImpl)mgr).getBus().getId();
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME + ":");
        buffer.append(ManagementConstants.BUS_ID_PROP + "=" + busId + ",");
        buffer.append(WorkQueueManagerImplMBeanWrapper.TYPE_VALUE + "=");
        buffer.append(WorkQueueManagerImplMBeanWrapper.NAME_VALUE + ",");
        buffer.append(ManagementConstants.TYPE_PROP + "=" + TYPE_VALUE + ",");
        buffer.append(ManagementConstants.NAME_PROP + "=" + aWorkQueue.getName());
       
        //Use default domain name of server
        return new ObjectName(buffer.toString());
    }
    
}
