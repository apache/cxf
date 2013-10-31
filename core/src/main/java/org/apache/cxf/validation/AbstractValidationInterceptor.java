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
import java.util.logging.Logger;

import javax.validation.ValidationException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingOperationInfo;

public abstract class AbstractValidationInterceptor extends AbstractPhaseInterceptor< Message > {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractValidationInterceptor.class);
    
    private volatile ValidationProvider provider;
    
    public AbstractValidationInterceptor(String phase) {
        super(phase);
    }
    
    public void setProvider(ValidationProvider provider) {
        this.provider = provider;
    }
    
    @Override
    public void handleMessage(Message message) throws Fault {        
        final Object resourceInstance = getResourceInstance(message);
        if (resourceInstance == null) {
            String error = "Resource instance is not available";
            LOG.severe(error);
            throw new ValidationException(error);
        }
        
        final Method method = getResourceMethod(message);
        if (method == null) {
            String error = "Resource method is not available";
            LOG.severe(error);
            throw new ValidationException(error);
        }
        
        
        final List< Object > arguments = MessageContentsList.getContentsList(message);
        
        handleValidation(message, resourceInstance, method, arguments);
                    
    }
    
    protected abstract Object getResourceInstance(Message message);
    
    protected Method getResourceMethod(Message message) {
        Message inMessage = message.getExchange().getInMessage();
        Method method = (Method)inMessage.get("org.apache.cxf.resource.method");
        if (method == null) {
            BindingOperationInfo bop = inMessage.getExchange().get(BindingOperationInfo.class);
            if (bop != null) {
                MethodDispatcher md = (MethodDispatcher) 
                    inMessage.getExchange().get(Service.class).get(MethodDispatcher.class.getName());
                method = md.getMethod(bop);
            }
        }
        return method;
    }
    
    protected abstract void handleValidation(final Message message, final Object resourceInstance,
                                             final Method method, final List<Object> arguments);


    protected ValidationProvider getProvider(Message message) {
        if (provider == null) {
            Object prop = message.getContextualProperty(ValidationProvider.class.getName());
            if (prop != null) {
                provider = (ValidationProvider)prop;    
            } else {
                provider = new ValidationProvider();
            }
        }
        return provider;
    }

    
}

