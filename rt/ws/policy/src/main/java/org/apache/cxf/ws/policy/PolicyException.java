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

import java.util.ResourceBundle;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.i18n.UncheckedException;

/**
 * PolicyException is the type of RuntimeException thrown by any exceptions encountered
 * by the policy framework.
 */
public class PolicyException extends UncheckedException {
    private static final long serialVersionUID = -6384955089085130084L;
    private static final ResourceBundle BUNDLE
        = BundleUtils.getBundle(PolicyException.class, "APIMessages");


    public PolicyException(Message msg, Throwable t) {
        super(msg, t);
    }

    public PolicyException(Message message) {
        super(message);
    }

    public PolicyException(Throwable cause) {
        super(cause);
    }

    public PolicyException(AssertionInfo info) {
        super(new Message("ASSERTION_NOT_ASSERTED", BUNDLE,
                          info.getAssertion().getName(), info.getErrorMessage()));
    }
}