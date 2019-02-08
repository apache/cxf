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
package org.apache.cxf.jaxrs.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Message;

public class AbstractInterceptorContextImpl extends AbstractPropertiesImpl {

    private Class<?> cls;
    private Type type;
    private Annotation[] anns;
    public AbstractInterceptorContextImpl(Class<?> cls,
                                        Type type,
                                        Annotation[] anns,
                                        Message message) {
        super(message);
        this.cls = cls;
        this.type = type;
        this.anns = anns;
    }

    public Class<?> getType() {
        return cls;
    }

    public Annotation[] getAnnotations() {
        return anns;
    }

    public Type getGenericType() {
        return type;
    }

    public void setAnnotations(Annotation[] annotations) {
        if (annotations == null) {
            throw new NullPointerException();
        }
        anns = annotations;

    }

    public void setGenericType(Type genType) {
        type = genType;
    }

    public void setType(Class<?> ctype) {
        if (cls != null && !cls.isAssignableFrom(ctype)) {
            providerSelectionPropertyChanged();
        }
        cls = ctype;
    }

    protected void providerSelectionPropertyChanged() {
        m.put(ProviderFactory.PROVIDER_SELECTION_PROPERTY_CHANGED, Boolean.TRUE);
    }
}
