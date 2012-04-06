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

import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;

public class SlowProcessingSimulator extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getLogger(SlowProcessingSimulator.class);
    
    private long delay = 10000L;
    private String action;
    
    public SlowProcessingSimulator() {
        this(Phase.USER_PROTOCOL);
    }
    
    public SlowProcessingSimulator(String p) {
        super(p);
    }

    
    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void handleMessage(Message message) throws Fault {
        try {
            // sleep delay msec for the specified action or any action if unspecified.
            String a = getAction(message);
            LOG.log(Level.INFO, "action=" + a);
            if (null == action || action.equals(a)) {
                LOG.log(Level.INFO, "sleeping " + delay + " msec ...");
                Thread.sleep(delay);    
            }
        } catch (InterruptedException e) {
            LOG.log(Level.INFO, "interrupted");
        }
        LOG.log(Level.INFO, "continuing");
    }

    private String getAction(Message message) {
        final AddressingProperties ap = ContextUtils.retrieveMAPs(message, false, false);
        if (ap != null && ap.getAction() != null) {
            return ap.getAction().getValue();
        } 
        return (String)message.get(SoapBindingConstants.SOAP_ACTION);
    }

}
