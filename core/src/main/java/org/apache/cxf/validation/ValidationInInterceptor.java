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
import org.apache.cxf.phase.Phase;

public class ValidationInInterceptor extends AbstractValidationInterceptor {
    static final String INPUT_VALIDATION_FAILED = "input.validation.failed";
    
    public ValidationInInterceptor() {
        super(Phase.PRE_INVOKE);
    }
    public ValidationInInterceptor(String phase) {
        super(phase);
    }

    @Override
    protected void handleValidation(final Message message, final Object resourceInstance,
                                    final Method method, final List<Object> arguments) {
        if (arguments.size() > 0) {
            try {
                ValidationProvider provider = getProvider(message);
                provider.validateParameters(resourceInstance, method, unwrapArgs(arguments).toArray());
                message.getExchange().put(ValidationProvider.class, provider);
            } catch (ValidationException ex) {
                message.getExchange().put(INPUT_VALIDATION_FAILED, true);
                throw ex;
            }
        }
    }
    
    protected List<Object> unwrapArgs(List<Object> arguments) {
        return arguments;
    }
}
