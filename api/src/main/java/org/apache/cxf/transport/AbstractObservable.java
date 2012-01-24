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

package org.apache.cxf.transport;

import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

public abstract class AbstractObservable implements Observable {

    protected MessageObserver incomingObserver;

    /**
     * Register a message observer for incoming messages.
     * 
     * @param observer the observer to notify on receipt of incoming
     * message
     */
    public synchronized void setMessageObserver(MessageObserver observer) {
        if (observer != incomingObserver) {
            MessageObserver old = incomingObserver;
            incomingObserver = observer;
            if (observer != null) {
                getLogger().fine("registering incoming observer: " + observer);
                if (old == null) {
                    try {
                        activate();
                    } catch (RuntimeException ex) {
                        incomingObserver = null;
                        throw ex;
                    }
                }
            } else {
                if (old != null) {
                    getLogger().fine("unregistering incoming observer: " + old);
                    deactivate();
                }
            }
        }
    }
   
    /**
     * @return the observer to notify on receipt of incoming message
     */
    public MessageObserver getMessageObserver() {
        return incomingObserver;
    }

    /**
     * Get the target reference .
     * 
     * @param ei the corresponding EndpointInfo
     * @return the actual target
     */
    protected static EndpointReferenceType getTargetReference(EndpointInfo ei, Bus bus) {
        return getTargetReference(ei, null, bus);
    }
    
    /**
     * Get the target endpoint reference.
     * 
     * @param ei the corresponding EndpointInfo
     * @param t the given target EPR if available
     * @param bus the Bus
     * @return the actual target
     */
    protected static EndpointReferenceType getTargetReference(EndpointInfo ei,
                                                              EndpointReferenceType t,
                                                              Bus bus) {
        EndpointReferenceType ref = null;
        if (null == t) {
            ref = new EndpointReferenceType();
            AttributedURIType address = new AttributedURIType();
            address.setValue(ei.getAddress());
            ref.setAddress(address);
            if (ei.getService() != null) {
                EndpointReferenceUtils.setServiceAndPortName(ref, 
                                                             ei.getService().getName(), 
                                                             ei.getName().getLocalPart());
            }
        } else {
            ref = t;
        }
        return ref;
    }
    
    /**
     * Activate messages flow.
     */
    protected void activate() {
        // nothing to do by default
    }

    /**
     * Deactivate messages flow.
     */
    protected void deactivate() {
        // nothing to do by default        
    }
    
    /**
     * @return the logger to use
     */
    protected abstract Logger getLogger();

}
