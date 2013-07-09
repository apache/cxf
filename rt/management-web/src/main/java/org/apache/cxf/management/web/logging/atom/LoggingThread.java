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
package org.apache.cxf.management.web.logging.atom;

/**
 * Helps disable logging from calls of the same thread. Motivation: log handlers in this package causes other
 * threads (from executor) to start logging (by using JAXB that itself uses JUL) which in turn can be caught
 * by the same handler leading to infinite loop.
 * <p>
 * Other approach than using thread local storage would be scanning of stack trace of current thread to see if
 * root of call comes from same package as package of handler - it's less effective so TLS is using here.
 */
final class LoggingThread {

    private static ThreadLocal<Boolean> threadLocal = new ThreadLocal<Boolean>();

    private LoggingThread() {
    }

    public static void markSilent(boolean silent) {
        if (silent) {
            threadLocal.set(Boolean.TRUE);
        } else {
            threadLocal.remove();
        }
    }

    public static boolean isSilent() {
        Boolean b = threadLocal.get();
        if (b != null) {
            return b;
        }
        return false;
    }
}
