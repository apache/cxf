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
import java.util.ResourceBundle;
import java.util.logging.Logger;

import jakarta.validation.executable.ExecutableType;
import jakarta.validation.executable.ValidateOnExecution;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.service.invoker.FactoryInvoker;
import org.apache.cxf.service.invoker.Invoker;

public abstract class AbstractValidationInterceptor extends AbstractPhaseInterceptor< Message >
        implements AutoCloseable {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractValidationInterceptor.class);
    protected static final ResourceBundle BUNDLE = BundleUtils.getBundle(AbstractValidationInterceptor.class);

    private Object serviceObject;
    private boolean customProvider;
    private volatile BeanValidationProvider provider;

    public AbstractValidationInterceptor(String phase) {
        super(phase);
    }

    public void setServiceObject(Object object) {
        this.serviceObject = object;
    }

    public void setProvider(BeanValidationProvider provider) {
        this.provider = provider;
    }

    @Override
    public void close() {
        if (customProvider) {
            provider.close();
        }
    }

    @Override
    public void handleMessage(Message message) {
        final Object theServiceObject = getServiceObject(message);
        if (theServiceObject == null) {
            return;
        }

        final Method method = getServiceMethod(message);
        if (method == null) {
            return;
        }
        
        ValidateOnExecution validateOnExec = method.getAnnotation(ValidateOnExecution.class);
        if (validateOnExec != null) {
            ExecutableType[] execTypes = validateOnExec.type();
            if (execTypes.length == 1 && execTypes[0] == ExecutableType.NONE) {
                return;
            }
        }


        final List< Object > arguments = MessageContentsList.getContentsList(message);

        handleValidation(message, theServiceObject, method, arguments);

    }

    protected Object getServiceObject(Message message) {
        if (serviceObject != null) {
            return serviceObject;
        }
        Object current = message.getExchange().get(Message.SERVICE_OBJECT);
        if (current != null) {
            return current;
        }
        Endpoint e = message.getExchange().getEndpoint();
        if (e != null && e.getService() != null) {
            Invoker invoker = e.getService().getInvoker();
            if (invoker instanceof FactoryInvoker) {
                FactoryInvoker factoryInvoker = (FactoryInvoker)invoker;
                if (factoryInvoker.isSingletonFactory()) {
                    return factoryInvoker.getServiceObject(message.getExchange());
                }
            }
        }
        return null;
    }

    protected Method getServiceMethod(Message message) {
        Message inMessage = message.getExchange().getInMessage();
        Method method = null;
        if (inMessage != null) {
            method = MessageUtils.getTargetMethod(inMessage).orElse(null);
        }
        if (method == null) {
            method = message.getExchange().get(Method.class);
        }
        return method;
    }

    protected abstract void handleValidation(Message message, Object resourceInstance,
                                             Method method, List<Object> arguments);


    protected BeanValidationProvider getProvider(Message message) {
        if (provider == null) {
            Object prop = message.getContextualProperty(BeanValidationProvider.class.getName());
            if (prop != null) {
                provider = (BeanValidationProvider)prop;
            } else {
                // don't create 2 validator factories and one not released!
                synchronized (this) {
                    if (provider == null) {
                        provider = new BeanValidationProvider();
                        customProvider = true;
                    }
                }
            }
        }
        return provider;
    }


}

