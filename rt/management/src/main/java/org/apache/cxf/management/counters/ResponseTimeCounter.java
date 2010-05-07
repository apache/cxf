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
package org.apache.cxf.management.counters;


import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectName;

import org.apache.cxf.message.FaultMode;

public class ResponseTimeCounter implements ResponseTimeCounterMBean, Counter {    
    
    private ObjectName objectName;
    private AtomicInteger invocations = new AtomicInteger();
    private AtomicInteger checkedApplicationFaults = new AtomicInteger();
    private AtomicInteger unCheckedApplicationFaults = new AtomicInteger();
    private AtomicInteger runtimeFaults = new AtomicInteger();
    private AtomicInteger logicalRuntimeFaults = new AtomicInteger();
    private long totalHandlingTime;    
    private long maxHandlingTime;
    private long minHandlingTime = Integer.MAX_VALUE;
    
    public ResponseTimeCounter(ObjectName on) {
        objectName = on;     
    }
    
    public void  increase(MessageHandlingTimeRecorder mhtr) {
        invocations.getAndIncrement();
        FaultMode faultMode = mhtr.getFaultMode();
        if (null == faultMode) {
            // no exception occured
        } else {
            switch (faultMode) {
            case CHECKED_APPLICATION_FAULT:
                checkedApplicationFaults.incrementAndGet();
                break;
            case LOGICAL_RUNTIME_FAULT:
                logicalRuntimeFaults.incrementAndGet();
                break;
            case RUNTIME_FAULT:
                runtimeFaults.incrementAndGet();
                break;
            case UNCHECKED_APPLICATION_FAULT:
                unCheckedApplicationFaults.incrementAndGet();
                break;
            default:
                runtimeFaults.incrementAndGet();
                break;
            }
        }
        
        long handlingTime = 0;
        if (mhtr.isOneWay()) {
            // We can count the response time 
            if (mhtr.getEndTime() > 0) {
                handlingTime = mhtr.getHandlingTime(); 
            }    
        } else {
            handlingTime = mhtr.getHandlingTime(); 
        }
            
        totalHandlingTime = totalHandlingTime + handlingTime;
        if (maxHandlingTime < handlingTime) {
            maxHandlingTime = handlingTime;
        }
        if (minHandlingTime > handlingTime) {
            minHandlingTime = handlingTime;
        }
    }

    public void reset() {
        invocations.set(0);
        checkedApplicationFaults.set(0);
        unCheckedApplicationFaults.set(0);
        runtimeFaults.set(0);
        logicalRuntimeFaults.set(0);
        
        totalHandlingTime = 0;    
        maxHandlingTime = 0;
        minHandlingTime = Integer.MAX_VALUE;   
    }
    
    public ObjectName getObjectName() {
        return objectName;
    }

    public Number getAvgResponseTime() {        
        return (int)(totalHandlingTime / invocations.get());
    }
    
    public Number getMaxResponseTime() {        
        return maxHandlingTime;
    }

    public Number getMinResponseTime() {        
        return minHandlingTime;
    }

    public Number getNumInvocations() {        
        return invocations.get();
    }

    public Number getNumCheckedApplicationFaults() {
        return checkedApplicationFaults.get();
    }

    public Number getNumLogicalRuntimeFaults() {
        return logicalRuntimeFaults.get();
    }
    
    public Number getNumRuntimeFaults() {
        return runtimeFaults.get();
    }
    
    public Number getNumUnCheckedApplicationFaults() {
        return unCheckedApplicationFaults.get();
    }
    
    public Number getTotalHandlingTime() {        
        return totalHandlingTime;
    }  

}
