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
package org.apache.cxf.jaxrs.reactivestreams.server;

import java.util.concurrent.CancellationException;

import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.impl.AsyncResponseImpl;
import org.apache.cxf.message.Message;
import org.reactivestreams.Publisher;


public abstract class AbstractReactiveInvoker extends JAXRSInvoker {
    private boolean useStreamingSubscriberIfPossible = true;
    
    
    protected Object handleThrowable(AsyncResponseImpl asyncResponse, Throwable t) {
        if (t instanceof CancellationException) {
            asyncResponse.cancel();
        } else {
            asyncResponse.resume(t);
        }
        return null;
    }
    
    protected boolean isJsonResponse(Message inMessage) {
        return MediaType.APPLICATION_JSON.equals(inMessage.getExchange().get(Message.CONTENT_TYPE));
    }

    public boolean isUseStreamingSubscriberIfPossible() {
        return useStreamingSubscriberIfPossible;
    }

    protected boolean isStreamingSubscriberUsed(Publisher<?> publisher,
                                                AsyncResponse asyncResponse, 
                                                Message inMessage) {
        if (isUseStreamingSubscriberIfPossible() && isJsonResponse(inMessage)) {
            publisher.subscribe(new JsonStreamingAsyncSubscriber<>(asyncResponse));
            return true;
        } else {
            return false;
        }
    }
    
    public void setUseStreamingSubscriberIfPossible(boolean useStreamingSubscriberIfPossible) {
        this.useStreamingSubscriberIfPossible = useStreamingSubscriberIfPossible;
    }
}
