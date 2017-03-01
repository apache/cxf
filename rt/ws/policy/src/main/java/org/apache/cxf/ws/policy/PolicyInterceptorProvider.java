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

package org.apache.cxf.ws.policy;

import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Message;
import org.apache.neethi.Assertion;

/**
 *
 */
public interface PolicyInterceptorProvider extends InterceptorProvider {
    /**
     * Returns a collection of QNames describing the xml schema types of the assertions that
     * this interceptor implements.
     *
     * @return collection of QNames of known assertion types
     */
    Collection<QName> getAssertionTypes();

    /**
     * Return false if the message does not contain enough contextual configuration to preemtively
     * support the given assertion.  Otherwise, return true.  If false, the PolicyEngine.supportsAlternative
     * method will not select this policy and will attempt a different alternative.
     *
     * Example: If the context does not contain login information, an assertion that requires it
     * could return false to allow the Alternative selection algorithms to try a different alternative.
     * @param msg The contextual message, may be null if no message is in context at this point
     * @param assertion
     * @return
     */
    boolean configurationPresent(Message msg, Assertion assertion);
}
