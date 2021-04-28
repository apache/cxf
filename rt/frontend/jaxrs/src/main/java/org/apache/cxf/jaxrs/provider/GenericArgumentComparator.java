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
package org.apache.cxf.jaxrs.provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Comparator;

import org.apache.cxf.jaxrs.utils.GenericsUtils;

public class GenericArgumentComparator implements Comparator<Class<?>> {

    private final Class<?> genericInterface;

    public GenericArgumentComparator(final Class<?> genericInterface) {
        if (genericInterface == null) {
            throw new IllegalArgumentException("Generic Interface cannot be null");
        }
        if (genericInterface.getTypeParameters().length == 0) {
            throw new IllegalArgumentException("Interface has no generic type parameters: " + genericInterface);
        }
        if (genericInterface.getTypeParameters().length > 1) {
            throw new IllegalArgumentException("Interface must have only 1 generic type parameter: "
                    + genericInterface);
        }
        this.genericInterface = genericInterface;
    }

    /**
     * This comparator sorts the most specific type to the top of the list.
     *
     * Effectively, this sorts classes in descending order with java.lang.Object
     * always as the last element if present.
     */
    @Override
    public int compare(final Class<?> a, final Class<?> b) {
        /*
         * To keep things from being too abstract and confusing, this javadoc refers
         * MessageBodyReader as the value of genericInterface.
         *
         * It could be any similar interface with just one generic type parameter,
         * such as MessageBodyWriter, ContextResolver, Consumer, etc.
         */

        /*
         * Get the actual type each class specified as its MessageBodyReader generic
         * parameter.  An array of one arg will be returned or null if the class does
         * not implement MessageBodyReader.
         */
        final Type[] aTypes = GenericsUtils.getTypeArgumentsFor(genericInterface, a);
        final Type[] bTypes = GenericsUtils.getTypeArgumentsFor(genericInterface, b);

        /*
         * If either class does not implement the MessageBodyReader interface and
         * therefore returned a null Types array, that class should have the lower
         * priority.
         */
        if (aTypes == bTypes) {
            return 0;
        }
        if (aTypes == null) {
            return 1;
        }
        if (bTypes == null) {
            return -1;
        }

        /*
         * We only support interfaces like MessageBodyReader that have
         * just one type parameter.  Neither class returned a null Type
         * array so we know each properly implements the interface and
         * therefore we don't need to check array lengths.
         */
        final Type aType = aTypes[0];
        final Type bType = bTypes[0];

        return compare(aType, bType);
    }

    public int compare(final Type aType, final Type bType) {
        if (aType == bType) {
            return 0;
        }

        /*
         * At this point we're now dealing with actual the value each
         * class specified for their MessageBodyReader implementations.
         *
         * Types like String, Boolean and URI will appear as a Class.
         * Types like JAXBElement which themselves have a parameter will
         * appear as a ParameterizedType.
         *
         * Let's first evaluate them as basic classes.  Only if they're
         * the same class do we need to look at their parameters.
         */
        final Class<?> aClass = asClass(aType);
        final Class<?> bClass = asClass(bType);

        /*
         * If they aren't the same class we only need to look at the
         * classes themselves and can ignore any parameters they have
         */
        if (!aClass.equals(bClass)) {
            /*
             * For those who can't remember this cryptic method:
             *
             * Red.class.isAssignableFrom(Color.class) == false
             * Color.class.isAssignableFrom(Red.class) == true
             */

            // bClass is a more generic version of aClass
            if (bClass.isAssignableFrom(aClass)) {
                return -1;
            }

            // aClass is a more generic version of bClass
            if (aClass.isAssignableFrom(bClass)) {
                return 1;
            }

            // These classes are unrelated
            return 0;
        }

        /*
         * They are the same class, so let's look at their parameters
         * and try to sort based on those.
         */
        final Type aParam = getFirstParameterOrObject(aType);
        final Type bParam = getFirstParameterOrObject(bType);

        return compare(aParam, bParam);
    }

    private Type getFirstParameterOrObject(final Type type) {
        if (!(type instanceof ParameterizedType)) {
            return Object.class;
        }

        final ParameterizedType parameterizedType = (ParameterizedType) type;
        final Type[] types = parameterizedType.getActualTypeArguments();

        if (types.length == 0) {
            return Object.class;
        }

        /*
         * This parameterized type may have more than one
         * generic argument (like Map or Function do).  If
         * so, too bad, we're ignoring it out of laziness.
         *
         * Feel free to come here and implement what makes
         * sense to you if you need this feature.  Maybe
         * you have a Map<String,Object> and Map<String,URL>
         * situation you want to support.
         */
        return types[0];
    }

    private Class<?> asClass(final Type aType) {
        if (aType instanceof Class) {
            return (Class<?>) aType;
        }

        if (aType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) aType;
            return asClass(parameterizedType.getRawType());
        }

        if (aType instanceof TypeVariable) {
            final TypeVariable typeVariable = (TypeVariable) aType;
            final Type[] bounds = typeVariable.getBounds();

            if (bounds == null || bounds.length == 0) {
                return Object.class;
            } else {
                return asClass(bounds[0]);
            }
        }

        if (aType instanceof WildcardType) {
            // todo
            return Object.class;
        }

        return Object.class;
    }
}
