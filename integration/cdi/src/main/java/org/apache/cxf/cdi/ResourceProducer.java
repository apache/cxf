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
package org.apache.cxf.cdi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

public class ResourceProducer {

    public Object createContextValue(Type type) {
        Message currentMessage = PhaseInterceptorChain.getCurrentMessage();
        if (currentMessage == null) {
            return null;
        }
        Type genericType = null;
        Class<?> contextType;
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            genericType = parameterizedType.getActualTypeArguments()[0];
            contextType = (Class<?>)parameterizedType.getRawType();
        } else {
            contextType = (Class<?>)type;
        }
        return JAXRSUtils.createContextValue(currentMessage, genericType, contextType);
    }
}
