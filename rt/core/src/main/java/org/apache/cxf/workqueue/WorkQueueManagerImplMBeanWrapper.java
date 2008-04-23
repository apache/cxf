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

import org.apache.cxf.Bus;
import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.annotation.ManagedAttribute;
import org.apache.cxf.management.annotation.ManagedOperation;
import org.apache.cxf.management.annotation.ManagedResource;
import org.apache.cxf.workqueue.WorkQueueManager.ThreadingModel;

@ManagedResource(componentName = "WorkQueue", 
                 description = "The CXF internal thread pool for manangement ", 
                 currencyTimeLimit = 15, persistPolicy = "OnUpdate", persistPeriod = 200)
                 
public class WorkQueueManagerImplMBeanWrapper implements ManagedComponent {    
    private static final String NAME_VALUE = "Bus.WorkQueue";
    private static final String TYPE_VALUE = "WorkQueueMBean";
    
    private WorkQueueManagerImpl wqManager;
    private AutomaticWorkQueueImpl aWorkQueue;
    private Bus bus;
    
    public WorkQueueManagerImplMBeanWrapper(WorkQueueManagerImpl wq) {
        wqManager = wq;        
        bus = wq.getBus();
        if (wqManager.autoQueue != null 
            && AutomaticWorkQueueImpl.class.isAssignableFrom(wqManager.autoQueue.getClass())) {
            aWorkQueue = (AutomaticWorkQueueImpl)wqManager.autoQueue;
        }
    }
   
    
    @ManagedOperation(currencyTimeLimit = 30)
    public void shutdown(boolean processRemainingWorkItems) {
        wqManager.shutdown(processRemainingWorkItems); 
    }
    
    @ManagedAttribute(description = "The thread pool work model",                      
                      defaultValue = "SINGLE_THREADED",
                      persistPolicy = "OnUpdate")
                      
    public String getThreadingModel() {        
        return wqManager.getThreadingModel().toString();
    }

    public void setThreadingModel(String model) {
        if (model.compareTo("SINGLE_THREADED") == 0) {
            wqManager.setThreadingModel(ThreadingModel.SINGLE_THREADED);
        }
        if (model.compareTo("MULTI_THREADED") == 0) {
            wqManager.setThreadingModel(ThreadingModel.MULTI_THREADED);
        }             
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
        
        String busId = bus.getId();        
        StringBuffer buffer = new StringBuffer();
        buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME + ":");
        buffer.append(ManagementConstants.BUS_ID_PROP + "=" + busId + ",");
        buffer.append(ManagementConstants.TYPE_PROP + "=" + TYPE_VALUE + ",");
        buffer.append(ManagementConstants.NAME_PROP + "=" + NAME_VALUE);

       
        //Use default domain name of server
        return new ObjectName(buffer.toString());
    }
    
}
