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

package org.apache.cxf.osgi.hibernate.validation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

//CHECKSTYLE:OFF
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.ValidationProviderResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ValidationProvider;
import javax.validation.Validation;
import javax.validation.bootstrap.ProviderSpecificBootstrap;
//CHECKSTYLE:ON

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;

/**
 * It's difficult-to-impossible to make the SPI mechanism work correctly for JSR-303 in Karaf.
 * So, this class provides an alternative; it produces Hibernate ValidationFactory instances
 * explicitly.
 */
public final class ValidationHelper {
    static ValidatorFactory validatorFactory;

    static final class HibernateValidationOSGIServicesProviderResolver implements ValidationProviderResolver {
        /**
         * Singleton instance.
         */
        private static ValidationProviderResolver instance;
        /**
         * Validation providers.
         */
        private final transient List<ValidationProvider<?>> providers = new ArrayList<>();

        private HibernateValidationOSGIServicesProviderResolver() {
            super();

        }

        /**
         * Singleton.
         *
         * @return the Singleton instance
         */
        public static synchronized ValidationProviderResolver getInstance() {
            if (instance == null) {
                instance = new HibernateValidationOSGIServicesProviderResolver();
                ((HibernateValidationOSGIServicesProviderResolver) instance).providers
                        .add(new HibernateValidator());
            }
            return instance;
        }

        /**
         * gets providers.
         * @return the validation providers
         */
        @Override
        public List<ValidationProvider<?>> getValidationProviders() {
            return this.providers;
        }

    }

    private ValidationHelper() {
        //
    }

    // wrap all calls in TCCL
    static final class ValidatorProxy implements InvocationHandler {

        private Object obj;
        private ClassLoader classLoader;


        private ValidatorProxy(Object obj) {
            this.obj = obj;
            this.classLoader = ValidationHelper.class.getClassLoader();
        }

        public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
            Object result;
            ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                result = m.invoke(obj, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } finally {
                Thread.currentThread().setContextClassLoader(oldTccl);
            }
            return result;
        }
    }

    private static <T> T proxy(Class<T> clazz, T obj) {
        return clazz.cast(java.lang.reflect.Proxy.newProxyInstance(
                obj.getClass().getClassLoader(),
                obj.getClass().getInterfaces(),
                new ValidatorProxy(obj)));
    }

    // Possibly this should also do the TCCL wrap for all the methods.
    static class ValidatorFactoryProxy implements ValidatorFactory {
        private ValidatorFactory proxied;

        public ValidatorFactoryProxy(ValidatorFactory proxied) {
            this.proxied = proxied;
        }

        @Override
        public Validator getValidator() {
            return proxy(Validator.class, proxied.getValidator());
        }

        @Override
        public ValidatorContext usingContext() {
            return proxied.usingContext();
        }

        @Override
        public MessageInterpolator getMessageInterpolator() {
            return proxied.getMessageInterpolator();
        }

        @Override
        public TraversableResolver getTraversableResolver() {
            return proxied.getTraversableResolver();
        }

        @Override
        public ConstraintValidatorFactory getConstraintValidatorFactory() {
            return proxied.getConstraintValidatorFactory();
        }

        @Override
        public ParameterNameProvider getParameterNameProvider() {
            return proxied.getParameterNameProvider();
        }

        @Override
        public <T> T unwrap(Class<T> type) {
            return proxied.unwrap(type);
        }

        @Override
        public void close() {
            proxied.close();
        }
    }

    /**
     * returns the validatorfactory.
     * @return the singleton validatorfactory
     */
    public static synchronized ValidatorFactory getValidatorFactory() {
        ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();
        try {
            // our bundle class loader should have wstx in it, which Hibernate seems to want.
            Thread.currentThread().setContextClassLoader(ValidationHelper.class.getClassLoader());
            if (validatorFactory == null) {
                final ProviderSpecificBootstrap<HibernateValidatorConfiguration> validationBootStrap = Validation
                        .byProvider(HibernateValidator.class);

                // bootstrap to properly resolve in an OSGi environment
                validationBootStrap
                        .providerResolver(HibernateValidationOSGIServicesProviderResolver
                                .getInstance());

                final HibernateValidatorConfiguration configure = validationBootStrap
                        .configure();
                validatorFactory = new ValidatorFactoryProxy(configure.buildValidatorFactory());


            }
            return validatorFactory;
        } finally {
            Thread.currentThread().setContextClassLoader(oldTccl);
        }
    }
}
