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

package org.apache.cxf.systest.ws.rm;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.rm.RMMessageConstants;
import org.apache.cxf.ws.rm.RMProperties;

/**
 * Delay a single message
 */
public class SingleMessageDelaySimulator extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getLogger(SingleMessageDelaySimulator.class);
    
    private long delay = 10000L;
    private long messageNumber = 2;
    
    public SingleMessageDelaySimulator() {
        this(Phase.USER_PROTOCOL);
    }
    
    public SingleMessageDelaySimulator(String p) {
        super(p);
    }
    
    public void setDelay(long delay) {
        this.delay = delay;
    }
    
    public void setMessageNumber(long messageNumber) {
        this.messageNumber = messageNumber;
    }

    public void handleMessage(Message message) throws Fault {
        RMProperties rmProps = (RMProperties)message.get(RMMessageConstants.RM_PROPERTIES_OUTBOUND);
        if (rmProps != null && rmProps.getMessageNumber()  == messageNumber) {
            sleep();
        }
    }

    private void sleep() {
        LOG.log(Level.INFO, "sleeping " + delay + " msec ...");
        try {
            Thread.sleep(delay);
            LOG.log(Level.INFO, "continuing");
        } catch (InterruptedException e) {
            LOG.log(Level.INFO, "interrupted");
        }
    }

}
