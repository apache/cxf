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
package org.apache.cxf.jaxrs.validation;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import jakarta.validation.ValidationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.logging.NoOpFaultListener;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.validation.BeanValidationInInterceptor;

@Provider
public class JAXRSBeanValidationInInterceptor extends BeanValidationInInterceptor
    implements ContainerRequestFilter {
    public JAXRSBeanValidationInInterceptor() {
    }
    public JAXRSBeanValidationInInterceptor(String phase) {
        super(phase);
    }

    @Override
    protected Object getServiceObject(Message message) {
        return ValidationUtils.getResourceInstance(message);
    }

    @Override
    protected void handleValidation(final Message message, final Object resourceInstance,
                                    final Method method, final List<Object> arguments) {
        try {
            super.handleValidation(message, resourceInstance, method, arguments);
        } catch (ValidationException ex) {
            message.put(FaultListener.class.getName(), new NoOpFaultListener());
            throw ex;
        }
    }

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        InterceptorChain chain = PhaseInterceptorChain.getCurrentMessage().getInterceptorChain();
        chain.add(this);
    }
}
