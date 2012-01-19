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

package org.apache.cxf.service.factory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

import javax.xml.namespace.QName;

import org.apache.cxf.common.util.ParamReader;
import org.apache.cxf.helpers.ServiceUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;

public class DefaultServiceConfiguration extends AbstractServiceConfiguration {

    public DefaultServiceConfiguration() {
        
    }
    
    @Override
    public QName getOperationName(InterfaceInfo service, Method method) {
        boolean fromWsdl = this.getServiceFactory().isFromWsdl();
        String ns = service.getName().getNamespaceURI();
        String local = method.getName();
        
        QName name = new QName(ns, local);
        
        if (fromWsdl && service.getOperation(name) != null) {
            //just matching the ops in the class to the ops on the wsdl
            //probably should check the params and such
            return name;
        }
        
        if (service.getOperation(name) == null) {
            return name;
        }

        int i = 1;
        while (true) {
            name = new QName(ns, local + i);
            if (service.getOperation(name) == null) {
                return name;
            } else {
                i++;
            }
        }
    }

    @Override
    public QName getFaultName(InterfaceInfo service, OperationInfo o, Class<?> exClass, Class<?> beanClass) {
        String name = ServiceUtils.makeServiceNameFromClassName(beanClass);
        return new QName(service.getName().getNamespaceURI(), name);
    }

    @Override
    public QName getInPartName(OperationInfo op, Method method, int paramNumber) {
        return getInParameterName(op, method, paramNumber);
    }

    @Override
    public QName getOutPartName(OperationInfo op, Method method, int paramNumber) {
        return getOutParameterName(op, method, paramNumber);
    }

    @Override
    public QName getInParameterName(OperationInfo op, Method method, int paramNumber) {
        return new QName(op.getName().getNamespaceURI(), 
                         getDefaultLocalName(op, method, paramNumber, "arg"));
    }

    @Override
    public QName getInputMessageName(OperationInfo op, Method method) {
        return new QName(op.getName().getNamespaceURI(), op.getName().getLocalPart());
    }

    @Override
    public QName getOutParameterName(OperationInfo op, Method method, int paramNumber) {
        return new QName(op.getName().getNamespaceURI(), 
                         getDefaultLocalName(op, method, paramNumber, "return"));
    }

    private String getDefaultLocalName(OperationInfo op, Method method, int paramNumber, String prefix) {
        Class<?> impl = getServiceFactory().getServiceClass();
        // try to grab the implementation class so we can read the debug symbols from it
        if (impl != null) {
            try {
                method = impl.getMethod(method.getName(), method.getParameterTypes());
            } catch (Exception e) {
                throw new ServiceConstructionException(e);
            }
        }
        
        return DefaultServiceConfiguration.createName(method, paramNumber, op.getInput()
            .getMessageParts().size(), false, prefix);
    }

    public static String createName(final Method method, final int paramNumber, final int currentSize,
                              boolean addMethodName, final String flow) {
        String paramName = "";

        if (paramNumber != -1) {
            String[] names = ParamReader.getParameterNamesFromDebugInfo(method);

            // get the specific parameter name from the parameter Number
            if (names != null && names[paramNumber] != null) {
                paramName = names[paramNumber];
                addMethodName = false;
            } else {
                paramName = flow + currentSize;
            }
        } else {
            paramName = flow;
        }

        paramName = addMethodName ? method.getName() + paramName : paramName;

        return paramName;
    }

    @Override
    public QName getOutputMessageName(OperationInfo op, Method method) {
        return new QName(op.getName().getNamespaceURI(), op.getName().getLocalPart() + "Response");
    }

    @Override
    public QName getInterfaceName() {
        return new QName(getServiceFactory().getServiceNamespace(), getServiceName() + "PortType");
    }

    @Override
    public QName getEndpointName() {
        return new QName(getServiceFactory().getServiceNamespace(), getServiceName() + "Port");
    }

    @Override
    public String getServiceName() {
        return getServiceFactory().getServiceClass().getSimpleName();
    }

    @Override
    public String getServiceNamespace() {
        String ret = super.getServiceNamespace();
        if (ret == null 
            && getServiceFactory() != null
            && getServiceFactory().getServiceClass() != null) {
            ret = ServiceUtils.makeNamespaceFromClassName(getServiceFactory().getServiceClass().getName(),
                "http");
        }
        return ret;
    }

    @Override
    public Boolean hasOutMessage(Method m) {
        return Boolean.TRUE;
    }

    @Override
    public Boolean isAsync(Method method) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean isHeader(Method method, int j) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean isInParam(Method method, int j) {
        if (j >= 0) {
            Class c = method.getParameterTypes()[j];
            if (Exchange.class.equals(c)) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public Boolean isOperation(Method method) {
        if (getServiceFactory().getIgnoredClasses().contains(method.getDeclaringClass().getName())) {
            return Boolean.FALSE;
        }

        // Don't do m.equals(method)
        for (Method m : getServiceFactory().getIgnoredMethods()) {
            if (m.getName().equals(method.getName()) 
                && Arrays.equals(method.getParameterTypes(), m.getParameterTypes())
                && m.getReturnType() == method.getReturnType()) {
                return Boolean.FALSE;
            }
        }
        
        final int modifiers = method.getModifiers();
        if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers) && !method.isSynthetic()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public Boolean isOutParam(Method method, int j) {
        if (j < 0) {
            return Boolean.TRUE;
        }
        Class<?> cls = method.getParameterTypes()[j];
        Type tp = method.getGenericParameterTypes()[j];
        
        return isHolder(cls, tp);
    }

    @Override
    public Boolean isWrapped(Method m) {
        return getServiceFactory().isWrapped();
    }
    
    @Override
    public Boolean isHolder(Class<?> cls, Type type) {
        if (cls.getSimpleName().equals("Holder")) {
            for (Field f : cls.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) { 
                    continue;
                }
                if (Modifier.isPublic(f.getModifiers())
                    && "value".equals(f.getName())) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
    
    @Override
    public Class<?> getHolderType(Class<?> cls, Type type) {
        
        if (isHolder(cls, type)) {
            if (type instanceof ParameterizedType) {
                //JAX-WS style using generics       
                ParameterizedType paramType = (ParameterizedType)type;
                cls = getHolderClass(paramType);
            } else {
                //JAXRPC style of code generated holder
                return cls.getDeclaredFields()[0].getType();
            }
        }

        return null;
    }   
    
    private static Class getHolderClass(ParameterizedType paramType) {
        Object rawType = paramType.getActualTypeArguments()[0];
        Class rawClass;
        if (rawType instanceof GenericArrayType) {
            rawClass = (Class)((GenericArrayType)rawType).getGenericComponentType();
            rawClass = Array.newInstance(rawClass, 0).getClass();
        } else {
            if (rawType instanceof ParameterizedType) {
                rawType = (Class)((ParameterizedType)rawType).getRawType();
            }
            rawClass = (Class)rawType;
        }
        return rawClass;
    }

    public Boolean isWrapperPartNillable(MessagePartInfo mpi) {
        return (Boolean)mpi.getProperty("nillable");
    }
    
    public Long getWrapperPartMaxOccurs(MessagePartInfo mpi) {
        String miString = (String)mpi.getProperty("maxOccurs");
        if (miString != null) {
            if ("unbounded".equals(miString)) {
                return Long.MAX_VALUE;
            } else {
                return Long.valueOf(miString, 10);
            }
        }
        // If no explicit spec and an array of bytes, default to unbounded.
        if (mpi.getTypeClass() != null && mpi.getTypeClass().isArray()
            && !Byte.TYPE.equals(mpi.getTypeClass().getComponentType())) {
            return Long.MAX_VALUE;
        }
        return null;
    }
    
    public Long getWrapperPartMinOccurs(MessagePartInfo mpi) {
        String miString = (String)mpi.getProperty("minOccurs");
        if (miString != null) {
            return Long.valueOf(miString, 10);
        }
        // If no explicit spec and not a primitive type (i.e. mappable to null)
        // set to 0.
        if (mpi.getTypeClass() != null && !mpi.getTypeClass().isPrimitive()) {
            return Long.valueOf(0);
        }
        return null;
    }
}
