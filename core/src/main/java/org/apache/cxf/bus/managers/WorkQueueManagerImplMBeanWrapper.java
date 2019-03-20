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

    private final WorkQueueManagerImpl wqManager;
    private final String objectName;

    public WorkQueueManagerImplMBeanWrapper(WorkQueueManagerImpl wq) {
        wqManager = wq;

        //Use default domain name of server
        objectName = new StringBuilder(ManagementConstants.DEFAULT_DOMAIN_NAME).append(':')
                .append(ManagementConstants.BUS_ID_PROP).append('=').append(wqManager.getBus().getId()).append(',')
                .append("WorkQueueManager=").append(NAME_VALUE)
                .append(',').append(ManagementConstants.TYPE_PROP).append('=').append(TYPE_VALUE).append(',')
                // Added the instance id to make the ObjectName unique
                .append(ManagementConstants.INSTANCE_ID_PROP).append('=').append(wqManager.hashCode()).toString();
    }

    @ManagedOperation(currencyTimeLimit = 30)
    public void shutdown(boolean processRemainingWorkItems) {
        wqManager.shutdown(processRemainingWorkItems);
    }

    public ObjectName getObjectName() throws JMException {
        return new ObjectName(objectName);
    }

}
