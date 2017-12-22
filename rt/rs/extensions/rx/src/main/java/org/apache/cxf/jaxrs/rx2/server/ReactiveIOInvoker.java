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
package org.apache.cxf.jaxrs.rx2.server;

import java.util.concurrent.CancellationException;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.impl.AsyncResponseImpl;
import org.apache.cxf.jaxrs.reactivestreams.server.JsonStreamingAsyncSubscriber;
import org.apache.cxf.message.Message;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

//Work in Progress
public class ReactiveIOInvoker extends JAXRSInvoker {
    private boolean useStreamingSubscriberIfPossible;
    protected AsyncResponseImpl checkFutureResponse(Message inMessage, Object result) {
        if (result instanceof Flowable) {
            return handleFlowable(inMessage, (Flowable<?>)result);
        } else if (result instanceof Single) {
            return handleSingle(inMessage, (Single<?>)result);
        } else if (result instanceof Observable) {
            return handleObservable(inMessage, (Observable<?>)result);
        } else {
            return null;
        }
    }
    
    protected AsyncResponseImpl handleSingle(Message inMessage, Single<?> single) {
        final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
        single.subscribe(v -> asyncResponse.resume(v), t -> handleThrowable(asyncResponse, t));
        return asyncResponse;
    }

    protected AsyncResponseImpl handleFlowable(Message inMessage, Flowable<?> f) {
        final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
        if (isUseStreamingSubscriberIfPossible() && isJsonResponse(inMessage)) {
            f.subscribe(new JsonStreamingAsyncSubscriber<>(asyncResponse));
        } else {
            f.subscribe(v -> asyncResponse.resume(v), t -> handleThrowable(asyncResponse, t));
        }
        return asyncResponse;
    }
    
    protected boolean isJsonResponse(Message inMessage) {
        return MediaType.APPLICATION_JSON.equals(inMessage.getExchange().get(Message.CONTENT_TYPE));
    }

    protected AsyncResponseImpl handleObservable(Message inMessage, Observable<?> obs) {
        final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
        obs.subscribe(v -> asyncResponse.resume(v), t -> handleThrowable(asyncResponse, t));
        return asyncResponse;
    }

    private Object handleThrowable(AsyncResponseImpl asyncResponse, Throwable t) {
        if (t instanceof CancellationException) {
            asyncResponse.cancel();
        } else {
            asyncResponse.resume(t);
        }
        return null;
    }

    public boolean isUseStreamingSubscriberIfPossible() {
        return useStreamingSubscriberIfPossible;
    }

    public void setUseStreamingSubscriberIfPossible(boolean useStreamingSubscriberIfPossible) {
        this.useStreamingSubscriberIfPossible = useStreamingSubscriberIfPossible;
    }
}
