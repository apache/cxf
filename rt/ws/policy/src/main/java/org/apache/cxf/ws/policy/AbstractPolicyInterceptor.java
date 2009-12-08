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

package org.apache.cxf.ws.policy;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;

/**
 * 
 */
public abstract class AbstractPolicyInterceptor extends AbstractPhaseInterceptor<Message> {
    
    protected Bus bus;
    
    public AbstractPolicyInterceptor(String phase) {
        super(phase);
    }
    public AbstractPolicyInterceptor(String id, String phase) {
        super(id, phase);
    }
    
    public void setBus(Bus b) {
        bus = b;
    }
    
    public Bus getBus() {
        return bus;
    }
    
    public void handleMessage(Message message) throws Fault {
        try {
            handle(message);
        } catch (PolicyException ex) {
            throw new Fault(ex);
        }
    }

    protected void getTransportAssertions(Message message) {
        Exchange ex = message.getExchange();
        Assertor assertor = null;
        Conduit conduit = ex.getConduit(message);
        if (conduit instanceof Assertor) {
            assertor = (Assertor)conduit;
        } else {
            Destination destination = ex.getDestination();
            if (destination instanceof Assertor) {
                assertor = (Assertor)destination;
            }
        }
        if (null != assertor) {
            assertor.assertMessage(message);
        }
    }
    
    protected BindingFaultInfo getBindingFaultInfo(Message msg, Exception ex, BindingOperationInfo boi) {
        BindingFaultInfo bfi = msg.get(BindingFaultInfo.class);        
        if (null == bfi && ex != null) {
            Throwable cause = ex.getCause();
            if (null == cause) {
                return null;
            }
            for (BindingFaultInfo b : boi.getFaults()) {
                Class<?> faultClass = b.getFaultInfo().getProperty(Class.class.getName(), Class.class);
                if (faultClass != null && faultClass.isAssignableFrom(cause.getClass())) {
                    bfi = b;
                    msg.put(BindingFaultInfo.class, bfi);
                    break;
                }
            }  
            if (null == bfi && null != boi.getWrappedOperation()) {
                for (BindingFaultInfo b : boi.getWrappedOperation().getFaults()) {
                    Class<?> faultClass = b.getFaultInfo().getProperty(Class.class.getName(), Class.class);
                    if (faultClass != null && faultClass.isAssignableFrom(cause.getClass())) {
                        bfi = b;
                        msg.put(BindingFaultInfo.class, bfi);
                        break;
                    }
                }  
            }
        }
        return bfi;
    }
    
    protected abstract void handle(Message message) throws PolicyException;

}
