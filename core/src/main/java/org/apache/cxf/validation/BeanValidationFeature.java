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
package org.apache.cxf.validation;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

@Provider(value = Type.Feature, scope = Scope.Server)
public class BeanValidationFeature extends AbstractFeature {

    private BeanValidationProvider validationProvider;

    @Override
    protected void initializeProvider(InterceptorProvider interceptorProvider, Bus bus) {
        BeanValidationInInterceptor in = new BeanValidationInInterceptor();
        BeanValidationOutInterceptor out = new BeanValidationOutInterceptor();
        if (validationProvider != null) {
            in.setProvider(validationProvider);
            out.setProvider(validationProvider);
        }
        interceptorProvider.getInInterceptors().add(in);
        interceptorProvider.getOutInterceptors().add(out);
    }

    public void setProvider(BeanValidationProvider provider) {
        this.validationProvider = provider;
    }
}
