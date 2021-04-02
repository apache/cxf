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

import java.lang.reflect.Method;
import java.util.List;

import javax.validation.ValidationException;

import org.apache.cxf.message.Message;

public abstract class AbstractBeanValidationInterceptor extends AbstractValidationInterceptor {
    protected AbstractBeanValidationInterceptor(String phase) {
        super(phase);
    }

    @Override
    protected Object getServiceObject(Message message) {
        return checkNotNull(super.getServiceObject(message), "SERVICE_OBJECT_NULL");
    }

    @Override
    protected Method getServiceMethod(Message message) {
        return (Method)checkNotNull(super.getServiceMethod(message), "SERVICE_METHOD_NULL");
    }

    private Object checkNotNull(Object object, String name) {
        if (object == null) {
            String message = new org.apache.cxf.common.i18n.Message(name, BUNDLE).toString();
            LOG.severe(message);
            throw new ValidationException(message);
        }
        return object;
    }

    @Override
    protected void handleValidation(final Message message, final Object resourceInstance,
                                    final Method method, final List<Object> arguments) {
        if (!arguments.isEmpty()) {
            BeanValidationProvider provider = getProvider(message);
            provider.validateParameters(resourceInstance, method, unwrapArgs(arguments).toArray());
            message.getExchange().put(BeanValidationProvider.class, provider);
        }
    }

    protected List<Object> unwrapArgs(List<Object> arguments) {
        return arguments;
    }
}
