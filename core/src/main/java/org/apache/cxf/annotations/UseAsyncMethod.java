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

package org.apache.cxf.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Instructs the runtime to dispatch using the async method
 * on service if continuations are available.  This only applies
 * to the JAX-WS frontend at this time.
 *
 * Instead of calling the "X methodName(Y, Z...) method, it will
 * call the "Future<?> methodName(Y, Z, ... AsyncHandler<MethodResponse>)"
 * method passing in an AsyncHandler that you will need to call when
 * the response is ready.   An example would be:
 *
 * <pre>
 * public Future<?> greetMeAsync(final String requestType,
 *                               final AsyncHandler<GreetMeResponse> asyncHandler) {
 *     final ServerAsyncResponse<GreetMeResponse> r = new ServerAsyncResponse<>();
 *     new Thread() {
 *         public void run() {
 *            //do some work on a backgound thread to generate the response...
 *            GreetMeResponse resp = new GreetMeResponse();
 *            resp.setResponseType("Hello " + requestType);
 *            r.set(resp);
 *            asyncHandler.handleResponse(r);
 *         }
 *    } .start();
 *    return r;
 * }
 * </pre>
 *
 * The use of the org.apache.cxf.jaxws.ServerAsyncResponse class for the response
 * as shown above can simplify things and is recommended.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Inherited
public @interface UseAsyncMethod {

    /**
     * By default, if continuations are not available,
     * it will use the non-async method.  If you ALWAYS
     * want the async method called, set this to true.  However,
     * that can cause threads to block.
     */
    boolean always() default false;
}

