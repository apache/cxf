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

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.container.ResourceContext;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;

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
        cri.getSubResource(subClass, cls, resource, true);
        return resource;
    }
}
