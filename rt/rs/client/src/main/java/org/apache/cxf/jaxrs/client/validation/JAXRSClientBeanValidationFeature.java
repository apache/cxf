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
package org.apache.cxf.jaxrs.client.validation;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.validation.ClientBeanValidationFeature;

@Provider(value = Type.Feature, scope = Scope.Client)
public class JAXRSClientBeanValidationFeature extends ClientBeanValidationFeature {
    public JAXRSClientBeanValidationFeature() {
        super(new Portable());
    }

    public void setWrapInProcessingException(boolean wrapInProcessingException) {
        Portable.class.cast(getDelegate()).setWrapInProcessingException(wrapInProcessingException);
    }

    public static class Portable extends ClientBeanValidationFeature.Portable {
        private boolean wrapInProcessingException;
        @Override
        public void doInitializeProvider(InterceptorProvider interceptorProvider, Bus bus) {
            JAXRSClientBeanValidationOutInterceptor out = new JAXRSClientBeanValidationOutInterceptor();
            out.setWrapInProcessingException(wrapInProcessingException);
            super.addInterceptor(interceptorProvider, out);
        }
        public void setWrapInProcessingException(boolean wrapInProcessingException) {
            this.wrapInProcessingException = wrapInProcessingException;
        }
    }
}
