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

package org.apache.cxf.transport.http_jetty.continuations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationSupport;

public class JettyContinuationWrapper implements Continuation, ContinuationListener {
    volatile boolean isNew;
    volatile boolean isResumed;
    volatile boolean isPending;
    volatile long pendingTimeout;
    volatile Object obj;
    
    private Message message;
    private org.eclipse.jetty.continuation.Continuation continuation;
    private ContinuationCallback callback;
    
    public JettyContinuationWrapper(HttpServletRequest request, 
                                    HttpServletResponse resp, 
                                    Message m) {
        continuation = ContinuationSupport.getContinuation(request);
        
        message = m;
        isNew = request.getAttribute(AbstractHTTPDestination.CXF_CONTINUATION_MESSAGE) == null;
        if (isNew) {
            request.setAttribute(AbstractHTTPDestination.CXF_CONTINUATION_MESSAGE,
                                 message.getExchange().getInMessage());
            continuation.addContinuationListener(this);
            callback = message.getExchange().get(ContinuationCallback.class);
        }
    }

    public Object getObject() {
        return obj;
    }
    public void setObject(Object userObject) {
        obj = userObject;
    }

    public void resume() {
        isResumed = true;
        isPending = false;
        continuation.resume();
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isPending() {
        return isPending;
    }

    public boolean isResumed() {
        return isResumed;
    }

    public void reset() {
        try {
            continuation.complete();
        } catch (Throwable ex) {
            // explicit complete call does not seem to work 
            // with the non-Servlet3 Jetty Continuation
        }
        obj = null;
        pendingTimeout = 0;
    }


    public boolean suspend(long timeout) {
        if (isPending && timeout != 0) {
            pendingTimeout += pendingTimeout + timeout;
        } else {
            pendingTimeout = timeout;
        }
        isNew = false;
        
        message.getExchange().getInMessage().getInterceptorChain().suspend();
        
        continuation.setTimeout(pendingTimeout);
        if (!isPending) {
            continuation.suspend();
            isPending = true;
        }
        return true;
    }
    
    protected Message getMessage() {
        Message m = message;
        if (m != null && m.getExchange().getInMessage() != null) {
            m = m.getExchange().getInMessage();
        }
        return m;
    }
    

    public void onComplete(org.eclipse.jetty.continuation.Continuation cont) {
        getMessage().remove(AbstractHTTPDestination.CXF_CONTINUATION_MESSAGE);
        isPending = false;
        pendingTimeout = 0;
        //REVISIT: isResumed = false;
        if (callback != null) {
            callback.onComplete();
        }
    }

    public void onTimeout(org.eclipse.jetty.continuation.Continuation cont) {
        isPending = false;
        pendingTimeout = 0;
        //isResumed = true;
    }
    
}
