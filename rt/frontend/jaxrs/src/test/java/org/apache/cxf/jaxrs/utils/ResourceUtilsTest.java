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
package org.apache.cxf.jaxrs.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.Customer;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.resources.Book;
import org.apache.cxf.jaxrs.resources.BookInterface;
import org.apache.cxf.jaxrs.resources.Chapter;

import org.junit.Assert;
import org.junit.Test;

public class ResourceUtilsTest extends Assert {
    
    @Test
    public void testFindResourceConstructor() {
        Constructor c = ResourceUtils.findResourceConstructor(Customer.class, true); 
        assertNotNull(c);
        assertEquals(2, c.getParameterTypes().length);
        assertEquals(UriInfo.class, c.getParameterTypes()[0]);
        assertEquals(String.class, c.getParameterTypes()[1]);
    }

    @Test
    public void testClassResourceInfoUserResource() throws Exception {
        UserResource ur = new UserResource();
        ur.setName(HashMap.class.getName());
        ur.setPath("/hashmap");
        UserOperation op = new UserOperation();
        op.setPath("/key/{id}");
        op.setName("get");
        op.setVerb("POST");
        op.setParameters(Collections.singletonList(new Parameter(ParameterType.PATH, "id")));
        ur.setOperations(Collections.singletonList(op));
        
        Map<String, UserResource> resources = new HashMap<String, UserResource>();
        resources.put(ur.getName(), ur);
        ClassResourceInfo cri = ResourceUtils.createClassResourceInfo(resources, ur, true);
        assertNotNull(cri);
        assertEquals("/hashmap", cri.getURITemplate().getValue());
        Method method = 
            HashMap.class.getMethod("get", new Class[]{Object.class});
        OperationResourceInfo ori = cri.getMethodDispatcher().getOperationResourceInfo(method);
        assertNotNull(ori);
        assertEquals("/key/{id}", ori.getURITemplate().getValue());
        List<Parameter> params = ori.getParameters();
        assertNotNull(params);
        Parameter p = params.get(0);
        assertEquals("id", p.getName());
    }
    
    @Test
    public void testGetAllJaxbClasses() {
        ClassResourceInfo cri1 = 
            ResourceUtils.createClassResourceInfo(BookInterface.class, BookInterface.class, true, true);
        Map<Class<?>, Type> types = 
            ResourceUtils.getAllRequestResponseTypes(Collections.singletonList(cri1), true);
        assertEquals(2, types.size());
        assertTrue(types.containsKey(Book.class));
        assertTrue(types.containsKey(Chapter.class));
    }
}
