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
import java.util.Collections;

import javax.xml.namespace.QName;

import org.apache.cxf.interceptor.AbstractAttributedInterceptorProvider;
import org.apache.cxf.message.Message;
import org.apache.neethi.Assertion;

/**
 *
 */
public abstract class AbstractPolicyInterceptorProvider extends AbstractAttributedInterceptorProvider
    implements PolicyInterceptorProvider {

    private static final long serialVersionUID = 7076292509741199877L;
    private Collection<QName> assertionTypes;

    public AbstractPolicyInterceptorProvider(QName type) {
        this(Collections.singletonList(type));
    }

    public AbstractPolicyInterceptorProvider(Collection<QName> at) {
        assertionTypes = at;
    }

    public Collection<QName> getAssertionTypes() {
        return assertionTypes;
    }

    public boolean configurationPresent(Message msg, Assertion assertion) {
        return true;
    }
}
