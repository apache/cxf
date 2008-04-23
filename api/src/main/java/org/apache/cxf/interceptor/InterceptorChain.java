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

package org.apache.cxf.interceptor;

import java.util.Collection;
import java.util.ListIterator;

import org.apache.cxf.message.Message;
import org.apache.cxf.transport.MessageObserver;

/**
 * Base interface for all interceptor chains.  An interceptor chain is an
 * ordered list of interceptors associated with one portion of the web service
 * processing pipeline.  Interceptor chains are defined for either the SOAP 
 * client's request or response handling, the web service's, or error handling
 * interceptor chains for SOAP faults.
 */
public interface InterceptorChain extends Iterable<Interceptor<? extends Message>> {
    
    enum State {
        PAUSED,
        EXECUTING,
        COMPLETE,
        ABORTED,
    };
    
    String STARTING_AFTER_INTERCEPTOR_ID = "starting_after_interceptor_id";
    String STARTING_AT_INTERCEPTOR_ID = "starting_at_interceptor_id";
    
    void add(Interceptor i);
    
    void add(Collection<Interceptor> i);
    
    void remove(Interceptor i);
    
    boolean doIntercept(Message message);
    
    boolean doInterceptStartingAfter(Message message, String startingAfterInterceptorID);

    boolean doInterceptStartingAt(Message message, String startingAtInterceptorID);

    void pause();
    
    void resume();
    
    void reset();
    
    ListIterator<Interceptor<? extends Message>> getIterator();

    MessageObserver getFaultObserver();
    
    void setFaultObserver(MessageObserver i);

    void abort();
}
