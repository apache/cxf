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

package org.apache.cxf.systest.interceptor;

import org.apache.cxf.greeter_control.PingMeFault;
import org.apache.cxf.greeter_control.types.FaultDetail;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * 
 */
public class FaultHandlingInterceptor extends AbstractPhaseInterceptor {
    
    public FaultHandlingInterceptor() {
        super(Phase.USER_LOGICAL);
    }
    
    public synchronized void handleMessage(Message message) throws Fault {
        FaultMode mode = MessageUtils.getFaultMode(message);
        if (null != mode) {
            Throwable cause = message.getContent(Exception.class).getCause();
            
            if (FaultMode.CHECKED_APPLICATION_FAULT == mode) {
                PingMeFault original = (PingMeFault)cause;                
                FaultDetail detail = new FaultDetail();
                detail.setMajor((short)20);
                detail.setMinor((short)10);
                PingMeFault replaced = new PingMeFault(original.getMessage(), detail);
                message.setContent(Exception.class, new Fault(replaced));
            } else {
                RuntimeException original = (RuntimeException)cause;
                RuntimeException replaced = new RuntimeException(original.getMessage().toUpperCase());
                message.setContent(Exception.class, new Fault(replaced));                
            }
        }
    }
}
