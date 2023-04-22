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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ParameterNameProvider;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

public class JAXRSParameterNameProvider implements ParameterNameProvider {
    @Override
    public List<String> getParameterNames(final Constructor< ? > constructor) {
        final List< String > parameterNames = new ArrayList<>();

        for (int i = 0; i < constructor.getParameterTypes().length; ++i) {
            parameterNames.add("arg" + i);
        }

        return parameterNames;
    }

    @Override
    public List<String> getParameterNames(final Method method) {
        final List< Parameter > parameters = ResourceUtils.getParameters(method);
        final List< String > parameterNames = new ArrayList<>();

        for (int i = 0; i < parameters.size(); ++i) {
            final StringBuilder sb = new StringBuilder();
            sb.append("arg").append(i);
            sb.append('(');

            Parameter parameter = parameters.get(i);
            if (parameter.getName() != null) {
                sb.append(parameter.getType().toString());
                sb.append("(\"").append(parameter.getName()).append("\")");
                sb.append(' ');
            }
            sb.append(method.getParameterTypes()[i].getSimpleName());

            sb.append(')');
            parameterNames.add(sb.toString());
        }

        return parameterNames;
    }



}
