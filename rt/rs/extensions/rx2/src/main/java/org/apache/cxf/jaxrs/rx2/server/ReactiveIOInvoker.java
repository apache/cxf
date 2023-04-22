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

import java.util.Collections;

import org.apache.cxf.jaxrs.impl.AsyncResponseImpl;
import org.apache.cxf.jaxrs.reactivestreams.server.AbstractReactiveInvoker;
import org.apache.cxf.message.Message;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

public class ReactiveIOInvoker extends AbstractReactiveInvoker {
    protected AsyncResponseImpl checkFutureResponse(Message inMessage, Object result) {
        if (result instanceof Flowable) {
            return handleFlowable(inMessage, (Flowable<?>)result);
        } else if (result instanceof Single) {
            return handleSingle(inMessage, (Single<?>)result);
        } else if (result instanceof Observable) {
            return handleObservable(inMessage, (Observable<?>)result);
        } else if (result instanceof Maybe) {
            return handleMaybe(inMessage, (Maybe<?>)result);
        }
        return null;
    }
    
    protected AsyncResponseImpl handleMaybe(Message inMessage, Maybe<?> maybe) {
        final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
        Disposable d = subscribe(maybe, asyncResponse);
        if (d == null) {
            throw new IllegalStateException("Subscribe did not return a Disposable");
        }
        return asyncResponse;
    }
    
    protected AsyncResponseImpl handleSingle(Message inMessage, Single<?> single) {
        final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
        Disposable d = single.subscribe(asyncResponse::resume, t -> handleThrowable(asyncResponse, t));
        if (d == null) {
            throw new IllegalStateException("Subscribe did not return a Disposable");
        }
        return asyncResponse;
    }

    protected AsyncResponseImpl handleFlowable(Message inMessage, Flowable<?> f) {
        final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
        if (!isStreamingSubscriberUsed(f, asyncResponse, inMessage)) {
            Disposable d = subscribe(f, asyncResponse);
            if (d == null) {
                throw new IllegalStateException("Subscribe did not return a Disposable");
            }
        }
        return asyncResponse;
    }
    
    protected AsyncResponseImpl handleObservable(Message inMessage, Observable<?> obs) {
        final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
        Disposable d = subscribe(obs, asyncResponse);
        if (d == null) {
            throw new IllegalStateException("Subscribe did not return a Disposable");
        }
        return asyncResponse;
    }

    private <T> Disposable subscribe(final Flowable<T> f, final AsyncResponseImpl asyncResponse) {
        return f
            .switchIfEmpty(Flowable.<T>empty().doOnComplete(() -> asyncResponse.resume(Collections.emptyList())))
            .subscribe(asyncResponse::resume, t -> handleThrowable(asyncResponse, t));
    }

    private <T> Disposable subscribe(final Observable<T> obs, final AsyncResponseImpl asyncResponse) {
        return obs
            .switchIfEmpty(Observable.<T>empty().doOnComplete(() -> asyncResponse.resume(Collections.emptyList())))
            .subscribe(asyncResponse::resume, t -> handleThrowable(asyncResponse, t));
    }
    
    private <T> Disposable subscribe(Maybe<T> maybe, final AsyncResponseImpl asyncResponse) {
        return maybe
            .switchIfEmpty(Maybe.<T>empty().doOnComplete(() -> asyncResponse.resume(null)))
            .subscribe(asyncResponse::resume, t -> handleThrowable(asyncResponse, t));
    }
}
