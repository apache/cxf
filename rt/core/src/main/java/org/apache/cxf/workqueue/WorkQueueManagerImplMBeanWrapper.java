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
import org.apache.cxf.management.annotation.ManagedOperation;
import org.apache.cxf.management.annotation.ManagedResource;

@ManagedResource(componentName = "WorkQueueManager", 
                 description = "The CXF manangement of work queues ", 
                 currencyTimeLimit = 15, persistPolicy = "OnUpdate", persistPeriod = 200)
                 
public class WorkQueueManagerImplMBeanWrapper implements ManagedComponent {    
    static final String NAME_VALUE = "Bus.WorkQueueManager";
    static final String TYPE_VALUE = "WorkQueueManager";
    
    private WorkQueueManagerImpl wqManager;
    private Bus bus;
    
    public WorkQueueManagerImplMBeanWrapper(WorkQueueManagerImpl wq) {
        wqManager = wq;        
        bus = wq.getBus();
    }
    
    @ManagedOperation(currencyTimeLimit = 30)
    public void shutdown(boolean processRemainingWorkItems) {
        wqManager.shutdown(processRemainingWorkItems); 
    }
      
    public ObjectName getObjectName() throws JMException {
        
        String busId = bus.getId();        
        StringBuilder buffer = new StringBuilder();
        buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME + ":");
        buffer.append(ManagementConstants.BUS_ID_PROP + "=" + busId + ",");
        buffer.append("WorkQueueManager=" + NAME_VALUE);
        buffer.append("," + ManagementConstants.TYPE_PROP + "=" + TYPE_VALUE);

        //Use default domain name of server
        return new ObjectName(buffer.toString());
    }
    
}
