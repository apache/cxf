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

package org.apache.cxf.phase;

/**
 * Represents transport-specific exceptions which are used to indicate that
 * a given invocation was suspended
 */
public class AbortedInvocationException extends RuntimeException {

    private static final long serialVersionUID = 6889545463301144757L;


    public AbortedInvocationException(Throwable cause) {
        super(cause);
    }

    public AbortedInvocationException() {
    }


    /**
     * Returns a transport-specific runtime exception
     * @return RuntimeException the transport-specific runtime exception,
     *         can be null for asynchronous transports
     */
    public RuntimeException getRuntimeException() {
        Throwable ex = getCause();
        return ex instanceof RuntimeException ? (RuntimeException)ex : null;
    }
}
