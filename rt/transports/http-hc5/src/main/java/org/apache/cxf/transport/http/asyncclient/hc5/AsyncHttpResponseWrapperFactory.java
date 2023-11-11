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

package org.apache.cxf.transport.http.asyncclient.hc5;

import java.util.function.Consumer;

import org.apache.cxf.Bus;
import org.apache.hc.core5.http.HttpResponse;

/**
 * The {@link Bus} extension to allow wrapping up the response processing of 
 * the {@link AsyncHTTPConduit} instance.
 */
@FunctionalInterface
public interface AsyncHttpResponseWrapperFactory {
    /** 
     * Creates new instance of the {@link AsyncHttpResponseWrapper} 
     * @return new instance of the {@link AsyncHttpResponseWrapper} (or null)
     */
    AsyncHttpResponseWrapper create();

    /**
     * The wrapper around the response that will be called by the {@link AsyncHTTPConduit} 
     * instance once the response is received. 
     */
    interface AsyncHttpResponseWrapper {
        /**
         * The callback which is called by the {@link AsyncHTTPConduit} instance once 
         * the response is received. The delegating response handler is passed as the
         * an argument and has to be called.
         * @param response the response received
         * @param delegate delegating response handler
         */
        default void responseReceived(HttpResponse response, Consumer<HttpResponse> delegate) {
            delegate.accept(response);
        }
    }
}
