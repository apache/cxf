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
package org.apache.cxf.jaxrs.swagger;

import java.util.Map;

import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserApplication;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;

import org.junit.Assert;
import org.junit.Test;

public class SwaggerUtilsTest extends Assert {

    @Test
    public void testConvertSwaggerPetShopToUserApp() {
        UserApplication ap = SwaggerUtils.getUserApplication("/swagger2petShop.json");
        assertNotNull(ap);
        assertEquals("/v2", ap.getBasePath());
        Map<String, UserResource> map = ap.getResourcesAsMap();
        assertEquals(3, map.size());
        
        UserResource pet = map.get("pet");
        verifyPetResource(pet);
        
        UserResource user = map.get("user");
        verifyPetUserResource(user);
        
        UserResource store = map.get("store");
        verifyPetStoreResource(store);
        
    }
    
    private void verifyPetResource(UserResource ur) {
        assertNotNull(ur);
    }
    //CHECKSTYLE:OFF
    private void verifyPetUserResource(UserResource ur) {
        assertNotNull(ur);
        assertEquals("/user", ur.getPath());
        assertEquals(8, ur.getOperations().size());
        //POST /user
        UserOperation createUser = ur.getOperations().get(0);
        assertEquals("createUser", createUser.getName());
        assertEquals("/", createUser.getPath());
        assertEquals("POST", createUser.getVerb());
        assertEquals("application/xml,application/json", createUser.getProduces());
        assertNull(createUser.getConsumes());
        assertEquals(1, createUser.getParameters().size());
        Parameter createUserParam1 = createUser.getParameters().get(0);
        assertEquals("body", createUserParam1.getName());
        assertEquals(ParameterType.REQUEST_BODY, createUserParam1.getType());
        //TODO: check that createUserParam1 refers to User model 
        //TODO: check default response type: JAX-RS Response ?
        
        //POST /user/createWithArray
        UserOperation createWithArray = ur.getOperations().get(1);
        assertEquals("createUsersWithArrayInput", createWithArray.getName());
        assertEquals("/createWithArray", createWithArray.getPath());
        assertEquals("POST", createUser.getVerb());
        assertEquals("application/xml,application/json", createUser.getProduces());
        assertNull(createWithArray.getConsumes());
        assertEquals(1, createUser.getParameters().size());
        Parameter createWithArrayParam1 = createUser.getParameters().get(0);
        assertEquals("body", createWithArrayParam1.getName());
        assertEquals(ParameterType.REQUEST_BODY, createWithArrayParam1.getType());
        //TODO: check that createUserParam1 refers to an array of User model 
        //TODO: check default response type: JAX-RS Response ?
        
        //POST /user/createWithList
        UserOperation createWithList = ur.getOperations().get(2);
        assertEquals("createUsersWithListInput", createWithList.getName());
        assertEquals("/createWithList", createWithList.getPath());
        assertEquals("POST", createWithList.getVerb());
        assertEquals("application/xml,application/json", createWithList.getProduces());
        assertNull(createWithList.getConsumes());
        assertEquals(1, createWithList.getParameters().size());
        Parameter createWithListParam1 = createWithList.getParameters().get(0);
        assertEquals("body", createWithListParam1.getName());
        assertEquals(ParameterType.REQUEST_BODY, createWithListParam1.getType());
        //TODO: check that createUserParam1 refers to an array of User model 
        //TODO: check default response type: JAX-RS Response ?
        
        //GET /user/login
        UserOperation loginUser = ur.getOperations().get(3);
        assertEquals("loginUser", loginUser.getName());
        assertEquals("/login", loginUser.getPath());
        assertEquals("GET", loginUser.getVerb());
        assertEquals("application/xml,application/json", loginUser.getProduces());
        assertNull(loginUser.getConsumes());
        assertEquals(2, loginUser.getParameters().size());
        Parameter loginUserParam1 = loginUser.getParameters().get(0);
        assertEquals("username", loginUserParam1.getName());
        assertEquals(ParameterType.QUERY, loginUserParam1.getType());
        assertEquals(String.class, loginUserParam1.getJavaType());
        Parameter loginUserParam2 = loginUser.getParameters().get(1);
        assertEquals("password", loginUserParam2.getName());
        assertEquals(ParameterType.QUERY, loginUserParam2.getType());
        assertEquals(String.class, loginUserParam2.getJavaType());
        //TODO: check 200 response type
        
        //GET /user/logout
        UserOperation logoutUser = ur.getOperations().get(4);
        assertEquals("logoutUser", logoutUser.getName());
        assertEquals("/logout", logoutUser.getPath());
        assertEquals("GET", loginUser.getVerb());
        assertEquals("application/xml,application/json", logoutUser.getProduces());
        assertNull(logoutUser.getConsumes());
        assertEquals(0, logoutUser.getParameters().size());
        //TODO: check 200 response type
        
        //GET /user/{username}
        UserOperation getUserByName = ur.getOperations().get(5);
        assertEquals("getUserByName", getUserByName.getName());
        assertEquals("/{username}", getUserByName.getPath());
        assertEquals("GET", getUserByName.getVerb());
        assertEquals("application/xml,application/json", getUserByName.getProduces());
        assertNull(getUserByName.getConsumes());
        assertEquals(1, getUserByName.getParameters().size());
        Parameter getUserByNameParam1 = getUserByName.getParameters().get(0);
        assertEquals("username", getUserByNameParam1.getName());
        assertEquals(ParameterType.PATH, getUserByNameParam1.getType());
        assertEquals(String.class, getUserByNameParam1.getJavaType());
        //TODO: check 200 response type
        
        //PUT /user/{username}
        UserOperation updateUser = ur.getOperations().get(6);
        assertEquals("updateUser", updateUser.getName());
        assertEquals("/{username}", updateUser.getPath());
        assertEquals("PUT", updateUser.getVerb());
        assertEquals("application/xml,application/json", updateUser.getProduces());
        assertNull(updateUser.getConsumes());
        assertEquals(2, updateUser.getParameters().size());
        Parameter updateUserParam1 = updateUser.getParameters().get(0);
        assertEquals("username", updateUserParam1.getName());
        assertEquals(ParameterType.PATH, updateUserParam1.getType());
        assertEquals(String.class, updateUserParam1.getJavaType());
        Parameter updateUserParam2 = updateUser.getParameters().get(1);
        assertEquals("body", updateUserParam2.getName());
        assertEquals(ParameterType.REQUEST_BODY, updateUserParam2.getType());
        //TODO: check that createUserParam1 refers to an array of User model
        //TODO: check 200 response type
        
        //DELETE /user/{username}
        UserOperation deleteUser = ur.getOperations().get(7);
        assertEquals("deleteUser", deleteUser.getName());
        assertEquals("/{username}", deleteUser.getPath());
        assertEquals("DELETE", deleteUser.getVerb());
        assertEquals("application/xml,application/json", deleteUser.getProduces());
        assertNull(deleteUser.getConsumes());
        assertEquals(1, deleteUser.getParameters().size());
        Parameter deleteUserParam1 = deleteUser.getParameters().get(0);
        assertEquals("username", deleteUserParam1.getName());
        assertEquals(ParameterType.PATH, deleteUserParam1.getType());
        assertEquals(String.class, deleteUserParam1.getJavaType());
        
    }
    //CHECKSTYLE:ON
    private void verifyPetStoreResource(UserResource ur) {
        assertNotNull(ur);
        assertEquals("/store", ur.getPath());
        assertEquals(4, ur.getOperations().size());
        //GET /store/inventory
        UserOperation getInv = ur.getOperations().get(0);
        assertEquals("getInventory", getInv.getName());
        assertEquals("/inventory", getInv.getPath());
        assertEquals("GET", getInv.getVerb());
        assertEquals("application/json", getInv.getProduces());
        assertNull(getInv.getConsumes());
        assertEquals(0, getInv.getParameters().size());
        //TODO: check 200 response type
        
        //POST /store/inventory
        UserOperation placeOrder = ur.getOperations().get(1);
        assertEquals("placeOrder", placeOrder.getName());
        assertEquals("/order", placeOrder.getPath());
        assertEquals("POST", placeOrder.getVerb());
        assertEquals("application/xml,application/json", placeOrder.getProduces());
        assertNull(placeOrder.getConsumes());
        assertEquals(1, placeOrder.getParameters().size());
        Parameter placeOrderParam1 = placeOrder.getParameters().get(0);
        assertEquals("body", placeOrderParam1.getName());
        assertEquals(ParameterType.REQUEST_BODY, placeOrderParam1.getType());
        //TODO: check that placeOrderParam1 refers to Order model 
        //TODO: check 200 response type
        
        //GET /store/order/{orderId}
        UserOperation getOrderById = ur.getOperations().get(2);
        assertEquals("getOrderById", getOrderById.getName());
        assertEquals("/order/{orderId}", getOrderById.getPath());
        assertEquals("GET", getOrderById.getVerb());
        assertEquals("application/xml,application/json", getOrderById.getProduces());
        assertNull(getOrderById.getConsumes());
        assertEquals(1, getOrderById.getParameters().size());
        Parameter getOrderByIdParam1 = getOrderById.getParameters().get(0);
        assertEquals("orderId", getOrderByIdParam1.getName());
        assertEquals(ParameterType.PATH, getOrderByIdParam1.getType());
        assertEquals(long.class, getOrderByIdParam1.getJavaType());
        //TODO: check 200 response type
        
        //DELETE /store/order/{orderId}
        UserOperation deleteOrder = ur.getOperations().get(3);
        assertEquals("deleteOrder", deleteOrder.getName());
        assertEquals("/order/{orderId}", deleteOrder.getPath());
        assertEquals("DELETE", deleteOrder.getVerb());
        assertEquals("application/xml,application/json", deleteOrder.getProduces());
        assertNull(deleteOrder.getConsumes());
        assertEquals(1, getOrderById.getParameters().size());
        Parameter deleteOrderParam1 = deleteOrder.getParameters().get(0);
        assertEquals("orderId", deleteOrderParam1.getName());
        assertEquals(ParameterType.PATH, deleteOrderParam1.getType());
        assertEquals(long.class, deleteOrderParam1.getJavaType());
        //TODO: check 200 response type
        
    }
    
    
    @Test
    public void testConvertSwaggerToUserApp() {
        UserApplication ap = SwaggerUtils.getUserApplication("/swagger20.json");
        assertNotNull(ap);
        assertEquals("/services/helloservice", ap.getBasePath());
        Map<String, UserResource> map = ap.getResourcesAsMap();
        assertEquals(2, map.size());

        UserResource ur = map.get("sayHello");
        assertNotNull(ur);
        assertEquals("/sayHello", ur.getPath());
        assertEquals(1, ur.getOperations().size());
        UserOperation op = ur.getOperations().get(0);
        assertEquals("sayHello", op.getName());
        assertEquals("/{a}", op.getPath());
        assertEquals("GET", op.getVerb());
        assertEquals("text/plain", op.getProduces());

        assertEquals(1, op.getParameters().size());
        Parameter param1 = op.getParameters().get(0);
        assertEquals("a", param1.getName());
        assertEquals(ParameterType.PATH, param1.getType());
        assertEquals(String.class, param1.getJavaType());

        UserResource ur2 = map.get("sayHello2");
        assertNotNull(ur2);
        assertEquals("/sayHello2", ur2.getPath());
        assertEquals(1, ur2.getOperations().size());
        UserOperation op2 = ur2.getOperations().get(0);
        assertEquals("sayHello", op2.getName());
        assertEquals("/{a}", op2.getPath());
        assertEquals("GET", op2.getVerb());
        assertEquals("text/plain", op2.getProduces());

        assertEquals(1, op2.getParameters().size());
        Parameter param2 = op.getParameters().get(0);
        assertEquals("a", param2.getName());
        assertEquals(ParameterType.PATH, param2.getType());
        assertEquals(String.class, param2.getJavaType());

    }
}
