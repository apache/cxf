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

import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationInfo;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.message.Message;
import org.mortbay.jetty.RetryRequest;
import org.mortbay.util.ajax.ContinuationSupport;

public class JettyContinuationWrapper implements Continuation {

    private org.mortbay.util.ajax.Continuation continuation;
    private Message message;
    
    
    public JettyContinuationWrapper(HttpServletRequest request, Message m) {
        continuation = ContinuationSupport.getContinuation(request, null); 
        message = m;
    }

    public Object getObject() {
        Object o = continuation.getObject();
        if (o instanceof ContinuationInfo) {
            return ((ContinuationInfo)o).getUserObject();
        }
        return o;
    }

    public boolean isNew() {
        return continuation.isNew() || (!continuation.isPending() && !continuation.isResumed());
    }

    public boolean isPending() {
        return continuation.isPending();
    }

    public boolean isResumed() {
        return continuation.isResumed();
    }

    public void reset() {
        continuation.reset();
    }

    public void resume() {
        continuation.resume();
    }

    public void setObject(Object userObject) {
        
        ContinuationInfo ci = null;
        
        Object obj = continuation.getObject();
        if (obj instanceof ContinuationInfo) {
            ci = (ContinuationInfo)obj;
        } else {
            ci = new ContinuationInfo(message);
            ci.setUserObject(obj);
        }
        if (message != userObject) {
            ci.setUserObject(userObject);
        }
        continuation.setObject(ci);
    }

    public boolean suspend(long timeout) {
        
        Object obj = continuation.getObject();
        if (obj == null) {
            continuation.setObject(new ContinuationInfo(message));
        }
        try {
            return continuation.suspend(timeout);
        } catch (RetryRequest ex) {
            throw new SuspendedInvocationException(ex);
        }
    }
    
    public void done() {
        ContinuationInfo ci = null;
        Object obj = continuation.getObject();
        if (obj instanceof ContinuationInfo) {
            ci = (ContinuationInfo)obj;
            continuation.setObject(ci.getUserObject());
        }
        continuation.reset();
    }

    protected Message getMessage() {
        return message;
    }
    
    public org.mortbay.util.ajax.Continuation getContinuation() {
        return continuation;
    }
    
    
}
