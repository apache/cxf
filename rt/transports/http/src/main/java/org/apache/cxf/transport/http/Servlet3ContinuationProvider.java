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

package org.apache.cxf.transport.http;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.message.Message;

/**
 * 
 */
public class Servlet3ContinuationProvider implements ContinuationProvider {
    HttpServletRequest req;
    HttpServletResponse resp; 
    Message inMessage;
    
    
    public Servlet3ContinuationProvider(HttpServletRequest req,
                                        HttpServletResponse resp, 
                                        Message inMessage) {
        this.inMessage = inMessage;
        this.req = req;
        this.resp = resp;
    }

    /** {@inheritDoc}*/
    public Continuation getContinuation() {
        if (inMessage.getExchange().isOneWay()) {
            return null;
        }

        return new Servlet3Continuation();
    }
    
    public class Servlet3Continuation implements Continuation, AsyncListener {
        AsyncContext context;
        boolean isNew;
        boolean isResumed;
        boolean isPending;
        Object obj;
        
        public Servlet3Continuation() {
            // It looks current Servlet3 implementation request doesn't pass the isAsyncStart 
            // status to the redispatched request, so we use the attribute to check the statues
            isNew = req.getAttribute(AbstractHTTPDestination.CXF_CONTINUATION_MESSAGE) == null;
            if (isNew) {
                req.setAttribute(AbstractHTTPDestination.CXF_CONTINUATION_MESSAGE,
                                 inMessage.getExchange().getInMessage());
                context = req.startAsync(req, resp);
                context.addListener(this);
            } else {
                context = req.getAsyncContext();
            }
        }
        
        public boolean suspend(long timeout) {
            if (isPending) {
                return false;
            }
            context.setTimeout(timeout);
            isNew = false;
            // Need to get the right message which is handled in the interceptor chain
            inMessage.getExchange().getInMessage().getInterceptorChain().suspend();
            isPending = true;
            return true;
        }
        public void redispatch() {
            context.dispatch();
        }
        public void resume() {
            isResumed = true;
            redispatch();
        }

        public void reset() {
            context.complete();
            obj = null;
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

        public Object getObject() {
            return obj;
        }

        public void setObject(Object o) {
            obj = o;
        }

        public void onComplete(AsyncEvent event) throws IOException {
            inMessage.getExchange().getInMessage()
                .remove(AbstractHTTPDestination.CXF_CONTINUATION_MESSAGE);
            isPending = false;
        }
        public void onError(AsyncEvent event) throws IOException {
        }
        public void onStartAsync(AsyncEvent event) throws IOException {
        }
        public void onTimeout(AsyncEvent event) throws IOException {
            isPending = false;
            redispatch();
        }
        
    }
}
