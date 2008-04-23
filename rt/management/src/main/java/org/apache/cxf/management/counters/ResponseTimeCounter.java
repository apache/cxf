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

public class ResponseTimeCounter implements ResponseTimeCounterMBean, Counter {    
    
    private ObjectName objectName;
    private AtomicInteger invocations = new AtomicInteger();
    private long totalHandlingTime;    
    private long maxHandlingTime;
    private long minHandlingTime = Integer.MAX_VALUE;
    
    public ResponseTimeCounter(ObjectName on) {
        objectName = on;     
    }
    
    public void  increase(MessageHandlingTimeRecorder mhtr) {
        invocations.getAndIncrement();
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

   
    
    
}
