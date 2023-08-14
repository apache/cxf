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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

import jakarta.validation.Configuration;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.ValidationProviderResolver;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.MethodDescriptor;
import jakarta.validation.spi.ValidationProvider;
import org.apache.cxf.common.logging.LogUtils;

import static java.util.Collections.emptySet;

public class BeanValidationProvider implements AutoCloseable {
    private static final Logger LOG = LogUtils.getL7dLogger(BeanValidationProvider.class);

    private final Runnable close;
    private final Supplier<Validator> factory;
    private final RuntimeCache runtimeCache; // /!must only be created when a single factory is used

    public BeanValidationProvider() {
        try {
            final ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
            this.factory = vf::getValidator;
            this.close = vf::close;
            this.runtimeCache = new RuntimeCache();
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
            final ValidatorFactory vf = factoryCfg.buildValidatorFactory();
            this.factory = vf::getValidator;
            this.close = vf::close;
            this.runtimeCache = new RuntimeCache();
        } catch (final ValidationException ex) {
            LOG.severe("Bean Validation provider can not be found, no validation will be performed");
            throw ex;
        }
    }

    public BeanValidationProvider(Validator validator) {
        if (validator == null) {
            throw new NullPointerException("Validator is null");
        }
        this.factory = () -> validator;
        this.close = () -> {
        };
        this.runtimeCache = new RuntimeCache();
    }

    public BeanValidationProvider(ValidatorFactory factory) {
        if (factory == null) {
            throw new NullPointerException("Factory is null");
        }
        this.factory = factory::getValidator;
        this.close = () -> {
        };
        this.runtimeCache = new RuntimeCache();
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
            final ValidatorFactory vf = factoryCfg.buildValidatorFactory();
            this.factory = vf::getValidator;
            this.close = () -> {
            };
            this.runtimeCache = new RuntimeCache();
        } catch (final ValidationException ex) {
            LOG.severe("Bean Validation provider can not be found, no validation will be performed");
            throw ex;
        }
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
        final Validator validator = factory.get();
        final ExecutableValidator methodValidator = validator.forExecutables();
        if (runtimeCache == null || runtimeCache.shouldValidateParameters(validator, method)) {
            final Set<ConstraintViolation<T>> violations = methodValidator.validateParameters(instance,
                    method, arguments);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
    }

    public< T > void validateReturnValue(final T instance, final Method method, final Object returnValue) {
        final Validator validator = factory.get();
        final ExecutableValidator methodValidator = validator.forExecutables();
        if (runtimeCache == null || runtimeCache.shouldValidateReturnedValue(validator, method)) {
            final Set<ConstraintViolation<T>> violations = methodValidator.validateReturnValue(instance,
                    method, returnValue);
            if (!violations.isEmpty()) {
                throw new ResponseConstraintViolationException(violations);
            }
        }
    }

    public< T > void validateReturnValue(final T bean) {
        Validator validator = factory.get();
        if (runtimeCache != null && bean != null
                && !runtimeCache.shouldValidateBean(validator, bean.getClass())) {
            return;
        }
        final Set<ConstraintViolation< T > > violations = doValidateBean(validator, bean);
        if (!violations.isEmpty()) {
            throw new ResponseConstraintViolationException(violations);
        }
    }

    public< T > void validateBean(final T bean) {
        final Set<ConstraintViolation< T > > violations = doValidateBean(factory.get(), bean);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private< T > Set<ConstraintViolation< T > > doValidateBean(final Validator validator, final T bean) {
        if (validator.getConstraintsForClass(bean.getClass()).isBeanConstrained()) {
            return validator.validate(bean);
        }
        return emptySet();
    }

    @Override
    public void close() {
        close.run();
    }

    // only created when there is a single validator/factory so it is safe to cache
    // note: the validator is passed as param to avoid to create useless ones
    private static final class RuntimeCache {
        private final ConcurrentMap<Class<?>, Boolean> types = new ConcurrentHashMap<>();
        private final ConcurrentMap<Method, Boolean> params = new ConcurrentHashMap<>();
        private final ConcurrentMap<Method, Boolean> returnedValues = new ConcurrentHashMap<>();

        public boolean shouldValidateParameters(final Validator validator, final Method method) {
            return params.computeIfAbsent(method, m -> {
                final MethodDescriptor constraint = validator
                    .getConstraintsForClass(m.getDeclaringClass())
                    .getConstraintsForMethod(m.getName(), m.getParameterTypes());
                return constraint != null && constraint.hasConstrainedParameters();
            });
        }

        public boolean shouldValidateReturnedValue(final Validator validator, final Method method) {
            return returnedValues.computeIfAbsent(method, m -> {
                final MethodDescriptor constraint = validator
                    .getConstraintsForClass(m.getDeclaringClass())
                    .getConstraintsForMethod(m.getName(), method.getParameterTypes());
                return constraint != null && constraint.hasConstrainedReturnValue();
            });
        }

        public boolean shouldValidateBean(final Validator validator, final Class<?> clazz) {
            return types.computeIfAbsent(clazz, it -> validator.getConstraintsForClass(it).isBeanConstrained());
        }
    }
}
