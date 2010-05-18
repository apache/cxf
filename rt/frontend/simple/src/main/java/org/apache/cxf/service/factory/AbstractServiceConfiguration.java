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

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.xml.namespace.QName;

import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;

public abstract class AbstractServiceConfiguration {
    protected String serviceNamespace;
    
    private ReflectionServiceFactoryBean serviceFactory;
    
    public ReflectionServiceFactoryBean getServiceFactory() {
        return serviceFactory;
    }

    public void setServiceFactory(ReflectionServiceFactoryBean serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public String getWsdlURL() {
        return null;
    }
    
    public String getServiceName() {
        return null;
    }
    
    public String getServiceNamespace() {
        return serviceNamespace;
    }
    public void setServiceNamespace(String s) {
        serviceNamespace = s;
    }
    
    public Boolean isOperation(final Method method) {
        return null;
    }

    public String getStyle() {
        return null;
    }

    public Boolean isWrapped() {
        return null;
    }
    
    public Boolean isWrapped(Method m) { 
        return null;
    }
    
    public Boolean isOutParam(Method method, int j) {
        return null;
    }

    public Boolean isInParam(Method method, int j) {
        return null;
    }

    public QName getInputMessageName(final OperationInfo op, Method method) {
        return null;
    }

    public QName getOutputMessageName(final OperationInfo op, Method method) {
        return null;
    }

    public Boolean hasOutMessage(Method m) {
        return null;
    }

    public QName getFaultName(InterfaceInfo service, OperationInfo o, Class<?> exClass, Class<?> beanClass) {
        return null;
    }

    public String getAction(OperationInfo op, Method method) {
        return null;
    }

    public Boolean isHeader(Method method, int j) {
        return null;
    }

    /**
     * Creates a name for the operation from the method name. If an operation
     * with that name already exists, a name is create by appending an integer
     * to the end. I.e. if there is already two methods named
     * <code>doSomething</code>, the first one will have an operation name of
     * "doSomething" and the second "doSomething1".
     * 
     * @param service
     * @param method
     */
    public QName getOperationName(InterfaceInfo service, Method method) {
        return null;
    }

    public String getMEP(final Method method) {
        return null;
    }

    public Boolean isAsync(final Method method) {
        return null;
    }

    public QName getInParameterName(final OperationInfo op, final Method method,
                                    final int paramNumber) {
        return null;
    }

    public QName getOutParameterName(final OperationInfo op, final Method method,
                                      final int paramNumber) {
        return null;
    }

    public QName getInPartName(final OperationInfo op, final Method method, final int paramNumber) {
        return null;
    }

    public QName getOutPartName(final OperationInfo op, final Method method, final int paramNumber) {
        return null;
    }
    
    public QName getInterfaceName() {
        return null;
    }

    public QName getEndpointName() {
        return null;
    }
    
    public QName getRequestWrapperName(OperationInfo op, Method method) {
        return null;        
    }  
    
    public QName getResponseWrapperName(OperationInfo op, Method method) {
        return null;        
    }  
    public String getResponseWrapperPartName(OperationInfo op, Method method) {
        return null;        
    }  
    public String getRequestWrapperPartName(OperationInfo op, Method method) {
        return null;        
    }  
 
    public Class getResponseWrapper(Method selected) {
        return null;
    }
    
    public Class getRequestWrapper(Method selected) {
        return null;
    }
    public String getResponseWrapperClassName(Method selected) {
        return null;
    }
    public String getRequestWrapperClassName(Method selected) {
        return null;
    }
    
    public Boolean isRPC(Method selected) {
        return null;
    }
    
    public Boolean isHolder(Class<?> cls, Type type) {
        return null;
    }
    
    public Type getHolderType(Class<?> cls, Type type) {
        return null;
    }
    
    public Boolean isWrapperPartNillable(MessagePartInfo mpi) {
        return null;
    }
    
    public Boolean isWrapperPartQualified(MessagePartInfo mpi) {
        return null;
    }
    public Long getWrapperPartMaxOccurs(MessagePartInfo mpi) {
        return null;
    }
    public Long getWrapperPartMinOccurs(MessagePartInfo mpi) {
        return null;
    }

    public String getFaultMessageName(OperationInfo op, Class<?> exClass, Class<?> beanClass) {
        return null;
    }
}
