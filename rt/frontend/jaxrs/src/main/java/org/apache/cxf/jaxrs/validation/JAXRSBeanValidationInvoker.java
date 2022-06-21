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

import jakarta.ws.rs.core.Response;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.validation.BeanValidationProvider;


public class JAXRSBeanValidationInvoker extends JAXRSInvoker {
    private volatile BeanValidationProvider provider;
    private boolean validateServiceObject = true;

    @Override
    public Object invoke(Exchange exchange, final Object serviceObject, Method m, List<Object> params) {
        Message message = JAXRSUtils.getCurrentMessage();

        BeanValidationProvider theProvider = getProvider(message);
        try {
            if (isValidateServiceObject()) {
                theProvider.validateBean(serviceObject);
            }

            theProvider.validateParameters(serviceObject, m, params.toArray());

            Object response = super.invoke(exchange, serviceObject, m, params);

            if (response instanceof MessageContentsList) {
                MessageContentsList list = (MessageContentsList)response;
                if (list.size() == 1) {
                    Object entity = list.get(0);

                    if (entity instanceof Response) {
                        theProvider.validateReturnValue(serviceObject, m, ((Response)entity).getEntity());
                    } else {
                        theProvider.validateReturnValue(serviceObject, m, entity);
                    }
                }
            }
            return response;
        } catch (Fault ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new Fault(ex);
        }

    }

    protected BeanValidationProvider getProvider(Message message) {
        if (provider == null) {
            Object prop = message.getContextualProperty(BeanValidationProvider.class.getName());
            if (prop != null) {
                provider = (BeanValidationProvider)prop;
            } else {
                provider = new BeanValidationProvider();
            }
        }
        return provider;
    }

    public void setProvider(BeanValidationProvider provider) {
        this.provider = provider;
    }

    public boolean isValidateServiceObject() {
        return validateServiceObject;
    }

    public void setValidateServiceObject(boolean validateServiceObject) {
        this.validateServiceObject = validateServiceObject;
    }
}
