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
package org.apache.cxf.jaxrs.reactor.server;

import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.impl.AsyncResponseImpl;
import org.apache.cxf.message.Message;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ReactorInvoker extends JAXRSInvoker {
    @Override
    protected AsyncResponseImpl checkFutureResponse(Message inMessage, Object result) {
        if (result instanceof Publisher) {
            final Publisher<?> publisher = (Publisher<?>) result;
            final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
            publisher.subscribe(new Subscriber<Object>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                }

                @Override
                public void onNext(Object o) {
                    asyncResponse.resume(o);
                }

                @Override
                public void onError(Throwable throwable) {
                    asyncResponse.onError(throwable);
                }

                @Override
                public void onComplete() {
                    asyncResponse.onComplete();
                }
            });
            return asyncResponse;
        }
        return null;
    }

    private Object handleThrowable(AsyncResponseImpl asyncResponse, Throwable t) {
        //TODO: if it is a Cancelation exception => asyncResponse.cancel();
        asyncResponse.resume(t);
        return null;
    }
}
