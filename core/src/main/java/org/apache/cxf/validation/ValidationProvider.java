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
import java.util.Set;
import java.util.logging.Logger;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;

import org.apache.cxf.common.logging.LogUtils;

public class ValidationProvider {
    private static final Logger LOG = LogUtils.getL7dLogger(ValidationProvider.class);
    
    private final ValidatorFactory factory;
    
    public ValidationProvider() {
        try {
            factory = Validation.buildDefaultValidatorFactory();
        } catch (final ValidationException ex) {
            LOG.severe("Bean Validation provider could be found, no validation will be performed");
            throw ex;
        }
    }
    
    public ValidationProvider(ValidationProviderResolver resolver) {
        try {
            Configuration<?> cfg = Validation.byDefaultProvider().providerResolver(resolver).configure();
            factory = cfg.buildValidatorFactory();
        } catch (final ValidationException ex) {
            LOG.severe("Bean Validation provider could be found, no validation will be performed");
            throw ex;
        }
    }
    
    public <T extends Configuration<T>> ValidationProvider(
        Class<javax.validation.spi.ValidationProvider<T>> providerType, 
        ValidationProviderResolver resolver) {
        try {
            Configuration<?> cfg = Validation.byProvider(providerType).providerResolver(resolver).configure();
            factory = cfg.buildValidatorFactory();
        } catch (final ValidationException ex) {
            LOG.severe("Bean Validation provider could be found, no validation will be performed");
            throw ex;
        }
    }
    
    public ValidationProvider(ValidatorFactory factory) {
        if (factory == null) {
            throw new NullPointerException("Factory is null");
        }
        this.factory = factory;
    }
    
    public< T > void validateParameters(final T instance, final Method method, final Object[] arguments) {
        
        final ExecutableValidator methodValidator = getExecutableValidator();
        final Set< ConstraintViolation< T > > violations = methodValidator.validateParameters(instance, 
            method, arguments);
        
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }                
    }
    
    public< T > void validateReturnValue(final T instance, final Method method, final Object returnValue) {
        final ExecutableValidator methodValidator = getExecutableValidator();
        final Set<ConstraintViolation< T > > violations = methodValidator.validateReturnValue(instance, 
            method, returnValue);
        
        if (!violations.isEmpty()) {
            throw new ResponseConstraintViolationException(violations);
        }                
    }
    
    public< T > void validateReturnValue(final T returnValue) {
        final Set<ConstraintViolation< T > > violations = doValidateBean(returnValue);
        if (!violations.isEmpty()) {
            throw new ResponseConstraintViolationException(violations);
        }                
    }
    
    public< T > void validateBean(final T bean) {
        final Set<ConstraintViolation< T > > violations = doValidateBean(bean);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }                
    }
    
    private< T > Set<ConstraintViolation< T > > doValidateBean(final T bean) {
        return factory.getValidator().validate(bean);
    }
    
    private ExecutableValidator getExecutableValidator() {
        
        return factory.getValidator().forExecutables();
    }
}
