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

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;

public class ContainerResponseContextImpl extends AbstractResponseContextImpl 
    implements ContainerResponseContext {

    private OperationResourceInfo ori;
    public ContainerResponseContextImpl(Response r, 
                                        Message m,
                                        OperationResourceInfo ori) {
        super(r, m);
        this.ori = ori;
    }
    
    @Override
    public Annotation[] getEntityAnnotations() {
        Annotation[] anns = super.getResponseEntityAnnotations();
        if (anns == null) {
            Method method = ori == null ? null : ori.getAnnotatedMethod();
            anns = method == null ? new Annotation[]{} : method.getAnnotations();
        }
        return anns;
    }

    @Override
    public Class<?> getEntityClass() {
        return ori == null ? getResponseEntityClass() : ori.getMethodToInvoke().getReturnType();
    }

    @Override
    public Type getEntityType() {
        return ori == null ? getResponseEntityClass() : ori.getMethodToInvoke().getGenericReturnType();
    }
    
    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return r.getMetadata();
    }
    
    @Override
    public OutputStream getEntityStream() {
        return m.get(OutputStream.class);
    }

    @Override
    public void setEntityStream(OutputStream os) {
        m.put(OutputStream.class, os);

    }


}
