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
 * ordered list of interceptors associated with one portion of the message
 * processing pipeline. Interceptor chains are defined for a client's request
 * processing, response processing, and incoming SOAP fault processing. Interceptor
 * chains are defined for a service's request processing, response processing, and
 * outgoing SOAP fault processing.
 */
public interface InterceptorChain extends Iterable<Interceptor<? extends Message>> {

    enum State {
        PAUSED,
        SUSPENDED,
        EXECUTING,
        COMPLETE,
        ABORTED,
    }

    String STARTING_AFTER_INTERCEPTOR_ID = "starting_after_interceptor_id";
    String STARTING_AT_INTERCEPTOR_ID = "starting_at_interceptor_id";

    /**
     * Adds a single interceptor to the interceptor chain.
     *
     * @param i the interceptor to add
     */
    void add(Interceptor<? extends Message> i);

    /**
     * Adds multiple interceptors to the interceptor chain.
     * @param i the interceptors to add to the chain
     */
    void add(Collection<Interceptor<? extends Message>> i);

    void remove(Interceptor<? extends Message> i);

    boolean doIntercept(Message message);

    boolean doInterceptStartingAfter(Message message, String startingAfterInterceptorID);

    boolean doInterceptStartingAt(Message message, String startingAtInterceptorID);

    /**
     * Pauses the current chain.   When the stack unwinds, the chain will just
     * return from the doIntercept method normally.
     */
    void pause();

    /**
     * Suspends the current chain.  When the stack unwinds, the chain back up
     * the iterator by one (so on resume, the interceptor that called pause will
     * be re-entered) and then throw a SuspendedInvocationException to the caller
     */
    void suspend();

    /**
     * Resumes the chain.  The chain will use the current thread to continue processing
     * the last message that was passed into doIntercept
     */
    void resume();

    /**
     * If the chain is marked as paused, this will JUST mark the chain as
     * in the EXECUTING phase.   This is useful if an interceptor pauses the chain,
     * but then immediately decides it should not have done that.   It can unpause
     * the chain and return normally and the normal processing will continue.
     */
    void unpause();


    void reset();

    State getState();

    ListIterator<Interceptor<? extends Message>> getIterator();

    MessageObserver getFaultObserver();

    void setFaultObserver(MessageObserver i);

    void abort();
}
