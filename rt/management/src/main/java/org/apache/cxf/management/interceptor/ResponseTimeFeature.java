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
package org.apache.cxf.management.interceptor;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

public class ResponseTimeFeature extends AbstractFeature {
    private static final ResponseTimeMessageInInterceptor IN =
        new ResponseTimeMessageInInterceptor();
    private static final ResponseTimeMessageInvokerInterceptor INVOKER = 
        new ResponseTimeMessageInvokerInterceptor();
    private static final ResponseTimeMessageOutInterceptor OUT = 
        new ResponseTimeMessageOutInterceptor();
    
    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        provider.getInInterceptors().add(IN);
        provider.getInFaultInterceptors().add(IN);
        provider.getInInterceptors().add(INVOKER);
        provider.getOutInterceptors().add(OUT);
        
    }

}
