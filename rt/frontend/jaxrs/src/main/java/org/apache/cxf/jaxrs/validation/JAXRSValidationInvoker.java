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

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

import javax.validation.ValidationException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.validation.ValidationProvider;


public class JAXRSValidationInvoker extends JAXRSInvoker {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSValidationInvoker.class);
    private volatile ValidationProvider provider;
    
    @Override
    public Object invoke(Exchange exchange, final Object serviceObject, Method m, List<Object> params) {
        Message message = JAXRSUtils.getCurrentMessage();
        
        if (!ValidationUtils.isAnnotatedMethodAvailable(message)) {
            String error = "Resource method is not available";
            LOG.severe(error);
            throw new ValidationException(error);
        }
        
        ValidationProvider theProvider = getProvider(message);
        
        theProvider.validate(serviceObject, m, params.toArray());
        
        Object response = super.invoke(exchange, serviceObject, m, params);
        
        Object responseToValidate = ValidationUtils.getResponseObject(response);
        if (responseToValidate != null) {
            theProvider.validate(serviceObject, m, responseToValidate);
        }
        
        return response;
    }
    
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
    
    public void setProvider(ValidationProvider provider) {
        this.provider = provider;
    }
}
