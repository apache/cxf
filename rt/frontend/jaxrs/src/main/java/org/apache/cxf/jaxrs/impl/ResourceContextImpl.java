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

import java.net.URI;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

public class ResourceContextImpl implements ResourceContext {

    private ClassResourceInfo cri;
    private Class<?> subClass;
    
    public ResourceContextImpl(OperationResourceInfo ori) {
        this.cri = ori.getClassResourceInfo();
        this.subClass = ori.getMethodToInvoke().getReturnType();
    }
    
    @Override
    public <T> T getResource(Class<T> cls) {
        T resource = null;
        try {
            resource = cls.newInstance();
        } catch (Throwable ex) {
            throw new InternalServerErrorException(ex);
        }
        return doInitResource(cls, resource);
    }
    
    public <T> T initResource(T resource) {
        return doInitResource(resource.getClass(), resource);
    }

    private <T> T doInitResource(Class<?> cls, T resource) {
        ClassResourceInfo sub = cri.getSubResource(subClass, cls, resource, true);
        Message m = PhaseInterceptorChain.getCurrentMessage();
        if (m != null) {
            sub.initBeanParamInfo(ProviderFactory.getInstance(m));
        } 
        return resource;
    }

    @Override
    public Object matchResource(URI arg0) throws NullPointerException, IllegalArgumentException {
        return null;
    }

    @Override
    public <T> T matchResource(URI arg0, Class<T> arg1) throws NullPointerException,
        IllegalArgumentException, ClassCastException {
        return null;
    }

    @Override
    public UriInfo matchUriInfo(URI arg0) throws NullPointerException, IllegalArgumentException {
        return null;
    }

}
