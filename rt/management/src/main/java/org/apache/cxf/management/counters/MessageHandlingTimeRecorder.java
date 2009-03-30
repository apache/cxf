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

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;

/* recoder the message actually handle begin and end time */ 
public class MessageHandlingTimeRecorder {
    private Exchange exchange;
    private long beginTime;
    private long endTime;
    private FaultMode faultMode;
    private boolean oneWay;
    
    public MessageHandlingTimeRecorder(Exchange ex) {
        exchange = ex;
        exchange.put(MessageHandlingTimeRecorder.class, this);
    }
    
    public boolean isOneWay() {
        return oneWay;
    }
    
    public void setOneWay(boolean ow) {
        oneWay = ow;
    }
    
    public Exchange getHandleExchange() {
        return exchange;    
    }
    
    public void beginHandling() {
        beginTime = System.nanoTime() / 1000;
    }
    
    public void endHandling() {
        endTime = System.nanoTime() / 1000;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public long getHandlingTime() {        
        return endTime - beginTime;
    }

    public FaultMode getFaultMode() {
        return faultMode;
    }

    public void setFaultMode(FaultMode faultMode) {
        this.faultMode = faultMode;
    }
}
