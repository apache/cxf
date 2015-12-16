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
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ParameterNameProvider;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;
import javax.validation.spi.ValidationProvider;

import org.apache.cxf.common.logging.LogUtils;

public class BeanValidationProvider {
    private static final Logger LOG = LogUtils.getL7dLogger(BeanValidationProvider.class);
    
    private final ValidatorFactory factory;
    private ClassLoader validateContextClassloader;
    
    public BeanValidationProvider() {
        try {
            factory = Validation.buildDefaultValidatorFactory();
        } catch (final ValidationException ex) {
            LOG.severe("Bean Validation provider can not be found, no validation will be performed");
            throw ex;
        }
    }
    
    public BeanValidationProvider(ParameterNameProvider parameterNameProvider) {
        this(new ValidationConfiguration(parameterNameProvider));
    }
    
    public BeanValidationProvider(ValidationConfiguration cfg) {
        try {
            Configuration<?> factoryCfg = Validation.byDefaultProvider().configure();
            initFactoryConfig(factoryCfg, cfg);
            factory = factoryCfg.buildValidatorFactory();
        } catch (final ValidationException ex) {
            LOG.severe("Bean Validation provider can not be found, no validation will be performed");
            throw ex;
        }
    }
    
    public BeanValidationProvider(ValidatorFactory factory) {
        if (factory == null) {
            throw new NullPointerException("Factory is null");
        }
        this.factory = factory;
    }
    
    public BeanValidationProvider(ValidationProviderResolver resolver) {
        this(resolver, null);
    }
    
    public <T extends Configuration<T>, U extends ValidationProvider<T>> BeanValidationProvider(
        ValidationProviderResolver resolver,
        Class<U> providerType) {
        this(resolver, providerType, null);
    }

    public <T extends Configuration<T>, U extends ValidationProvider<T>> BeanValidationProvider(
        ValidationProviderResolver resolver,
        Class<U> providerType,
        ValidationConfiguration cfg) {
        try {
            Configuration<?> factoryCfg = providerType != null 
                ? Validation.byProvider(providerType).providerResolver(resolver).configure()
                : Validation.byDefaultProvider().providerResolver(resolver).configure();   
            initFactoryConfig(factoryCfg, cfg);
            factory = factoryCfg.buildValidatorFactory();
        } catch (final ValidationException ex) {
            LOG.severe("Bean Validation provider can not be found, no validation will be performed");
            throw ex;
        }
    }

    public ClassLoader getValidateContextClassloader() {
        return validateContextClassloader;
    }

    public void setValidateContextClassloader(ClassLoader validateContextClassloader) {
        this.validateContextClassloader = validateContextClassloader;
    }

    private static void initFactoryConfig(Configuration<?> factoryCfg, ValidationConfiguration cfg) {
        if (cfg != null) {
            factoryCfg.parameterNameProvider(cfg.getParameterNameProvider());
            factoryCfg.messageInterpolator(cfg.getMessageInterpolator());
            factoryCfg.traversableResolver(cfg.getTraversableResolver());
            factoryCfg.constraintValidatorFactory(cfg.getConstraintValidatorFactory());
            for (Map.Entry<String, String> entry : cfg.getProperties().entrySet()) {
                factoryCfg.addProperty(entry.getKey(), entry.getValue());
            }
        }
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
    
    public< T > void validateReturnValue(final T bean) {
        final Set<ConstraintViolation< T > > violations = doValidateBean(bean);
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
        ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
        try {
            // In OSGi, hibernate's hunt for an EL provided can fail without this.
            if (validateContextClassloader != null) {
                Thread.currentThread().setContextClassLoader(validateContextClassloader);
            }
            return factory.getValidator().validate(bean);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTccl);
        }
    }
    
    private ExecutableValidator getExecutableValidator() {
        
        return factory.getValidator().forExecutables();
    }
}
