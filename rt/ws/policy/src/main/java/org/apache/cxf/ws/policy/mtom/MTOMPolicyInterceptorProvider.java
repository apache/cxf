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

package org.apache.cxf.ws.policy.mtom;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;

@NoJSR250Annotations
public class MTOMPolicyInterceptorProvider extends AbstractPolicyInterceptorProvider {

    private static final long serialVersionUID = -2101800288259099105L;
    private static final Collection<QName> ASSERTION_TYPES;
    private static final MTOMPolicyInterceptor INTERCEPTOR = new MTOMPolicyInterceptor();

    static {
        Collection<QName> types = new ArrayList<>();
        types.add(MetadataConstants.MTOM_ASSERTION_QNAME);
        ASSERTION_TYPES = types;
    }

    public MTOMPolicyInterceptorProvider() {
        super(ASSERTION_TYPES);

        getInInterceptors().add(INTERCEPTOR);

        getOutInterceptors().add(INTERCEPTOR);

        getInFaultInterceptors().add(INTERCEPTOR);

        getOutFaultInterceptors().add(INTERCEPTOR);
    }

}
