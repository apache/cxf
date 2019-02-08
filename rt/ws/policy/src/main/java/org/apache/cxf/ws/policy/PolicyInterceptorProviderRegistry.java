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
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.extension.Registry;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.neethi.Assertion;


/**
 * InterceptorProviderRegistry is used to manage InterceptorProviders that provide
 * assertion domain specific interceptors.
 */
public interface PolicyInterceptorProviderRegistry
    extends Registry<QName, Set<PolicyInterceptorProvider>> {

    /**
     * Register the builder for all qnames from the provider
     * getAssertionTypes call.
     * @param provider the provider to register
     */
    void register(PolicyInterceptorProvider provider);

    List<Interceptor<? extends Message>>
    getInterceptorsForAlternative(Collection<? extends Assertion> alterative,
                                  boolean out, boolean fault);

    List<Interceptor<? extends Message>> getInInterceptorsForAssertion(QName qn);

    List<Interceptor<? extends Message>> getInFaultInterceptorsForAssertion(QName qn);

    List<Interceptor<? extends Message>> getOutInterceptorsForAssertion(QName qn);

    List<Interceptor<? extends Message>> getOutFaultInterceptorsForAssertion(QName qn);
}
