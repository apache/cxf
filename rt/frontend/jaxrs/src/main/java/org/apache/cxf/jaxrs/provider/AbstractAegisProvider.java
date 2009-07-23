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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.aegis.AegisContext;

public abstract class AbstractAegisProvider 
    implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
    
    private static Map<Class<?>, AegisContext> classContexts = new WeakHashMap<Class<?>, AegisContext>();
    protected boolean writeXsiType = true;
    protected boolean readXsiType = true;
    @Context 
    protected ContextResolver<AegisContext> resolver;
    
    public void setWriteXsiType(boolean write) {
        writeXsiType = write;
    }
    
    public void setReadXsiType(boolean read) {
        readXsiType = read;
    }
    
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        return isSupported(type, genericType, anns);
    }
    
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return isSupported(type, genericType, annotations);
    }

    public long getSize(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return -1;
    }

    protected AegisContext getAegisContext(Class<?> type, Type genericType) {
        
        if (resolver != null) {
            AegisContext context = resolver.getContext(type);
            // it's up to the resolver to keep its contexts in a map
            if (context != null) {
                return context;
            }
        }
        
        return getClassContext(type, genericType);
    }
    
    
    private void addType(Set<Class<?>> rootClasses, Type cls, boolean allowArray) {
        if (cls instanceof Class) {
            if (((Class)cls).isArray() && !allowArray) {
                rootClasses.add(((Class)cls).getComponentType());
            } else {
                rootClasses.add((Class)cls);
            }
        } else if (cls instanceof ParameterizedType) {
            for (Type t2 : ((ParameterizedType)cls).getActualTypeArguments()) {
                addType(rootClasses, t2, false);
            }
        } else if (cls instanceof GenericArrayType) {
            GenericArrayType gt = (GenericArrayType)cls;
            Class ct = (Class) gt.getGenericComponentType();
            ct = Array.newInstance(ct, 0).getClass();

            rootClasses.add(ct);
        }
    }
    private AegisContext getClassContext(Class<?> type, Type genericType) {
        synchronized (classContexts) {
            AegisContext context = classContexts.get(type);
            if (context == null) {
                context = new AegisContext();
                context.setWriteXsiTypes(writeXsiType); 
                context.setReadXsiTypes(readXsiType);
                Set<Class<?>> rootClasses = new HashSet<Class<?>>();
                rootClasses.add(type);
                if (!(genericType instanceof Class)) {
                    addType(rootClasses, genericType, true);
                }
                context.setRootClasses(rootClasses);
                context.initialize();
                classContexts.put(type, context);
            }
            return context;
        }
    }
    
    /**
     * For Aegis, it's not obvious to me how we'd decide that a type was hopeless.
     * @param type
     * @param genericType
     * @param annotations
     * @return
     */
    protected boolean isSupported(Class<?> type, Type genericType, Annotation[] annotations) {
        return true;
    }
    
    void clearContexts() {
        classContexts.clear();
    }
}
