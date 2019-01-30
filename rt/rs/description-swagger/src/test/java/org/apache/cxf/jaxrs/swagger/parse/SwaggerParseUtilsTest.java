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
package org.apache.cxf.jaxrs.swagger.parse;

import java.io.InputStream;
import java.util.Map;

import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserApplication;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SwaggerParseUtilsTest {

    @Test
    public void testConvertPetShopDocToUserApp() {
        UserApplication ap = SwaggerParseUtils.getUserApplication("/swagger2petShop.json");
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
    //CHECKSTYLE:OFF
    private void verifyPetResource(UserResource ur) {
        assertNotNull(ur);
        assertEquals("/", ur.getPath());
        assertEquals(8, ur.getOperations().size());
        //POST /pet
        UserOperation addPet = ur.getOperations().get(0);
        assertEquals("addPet", addPet.getName());
        assertEquals("/pet", addPet.getPath());
        assertEquals("POST", addPet.getVerb());
        assertEquals("application/xml,application/json", addPet.getProduces());
        assertEquals("application/json,application/xml", addPet.getConsumes());
        assertEquals(1, addPet.getParameters().size());
        Parameter addPetParam1 = addPet.getParameters().get(0);
        assertEquals("body", addPetParam1.getName());
        assertEquals(ParameterType.REQUEST_BODY, addPetParam1.getType());
        //TODO: check that addPetParam1 refers to Pet model 
        //TODO: check default response type: JAX-RS Response ?
        
        //PUT /pet
        UserOperation putPet = ur.getOperations().get(1);
        assertEquals("updatePet", putPet.getName());
        assertEquals("/pet", putPet.getPath());
        assertEquals("PUT", putPet.getVerb());
        assertEquals("application/xml,application/json", addPet.getProduces());
        assertEquals("application/json,application/xml", addPet.getConsumes());
        assertEquals(1, putPet.getParameters().size());
        Parameter putPetParam1 = putPet.getParameters().get(0);
        assertEquals("body", putPetParam1.getName());
        assertEquals(ParameterType.REQUEST_BODY, putPetParam1.getType());
        //TODO: check that putPetParam1 refers to Pet model 
        //TODO: check default response type: JAX-RS Response ?
        
        //GET /pet/findByStatus
        UserOperation findPetsByStatus = ur.getOperations().get(2);
        assertEquals("findPetsByStatus", findPetsByStatus.getName());
        assertEquals("/pet/findByStatus", findPetsByStatus.getPath());
        assertEquals("GET", findPetsByStatus.getVerb());
        assertEquals("application/xml,application/json", findPetsByStatus.getProduces());
        assertNull(findPetsByStatus.getConsumes());
        assertEquals(1, findPetsByStatus.getParameters().size());
        Parameter findPetsByStatusParam1 = findPetsByStatus.getParameters().get(0);
        assertEquals("status", findPetsByStatusParam1.getName());
        assertEquals(ParameterType.QUERY, findPetsByStatusParam1.getType());
        //TODO: check that findPetsByStatusParam1 refers to List of String
        //TODO: check that response refers to List of Pets
        
        //GET /pet/findByTag
        UserOperation findPetsByTags = ur.getOperations().get(3);
        assertEquals("findPetsByTags", findPetsByTags.getName());
        assertEquals("/pet/findByTags", findPetsByTags.getPath());
        assertEquals("GET", findPetsByTags.getVerb());
        assertEquals("application/xml,application/json", findPetsByTags.getProduces());
        assertNull(findPetsByTags.getConsumes());
        assertEquals(1, findPetsByTags.getParameters().size());
        Parameter findPetsByTagsParam1 = findPetsByTags.getParameters().get(0);
        assertEquals("tags", findPetsByTagsParam1.getName());
        assertEquals(ParameterType.QUERY, findPetsByTagsParam1.getType());
        //TODO: check that findPetsByTagsParam1 refers to List of String
        //TODO: check that response refers to List of Pets
        
        //GET /pet/{petId}
        UserOperation getPetById = ur.getOperations().get(4);
        assertEquals("getPetById", getPetById.getName());
        assertEquals("/pet/{petId}", getPetById.getPath());
        assertEquals("GET", getPetById.getVerb());
        assertEquals("application/xml,application/json", getPetById.getProduces());
        assertNull(getPetById.getConsumes());
        assertEquals(1, getPetById.getParameters().size());
        Parameter getPetByIdParam1 = getPetById.getParameters().get(0);
        assertEquals("petId", getPetByIdParam1.getName());
        assertEquals(ParameterType.PATH, getPetByIdParam1.getType());
        assertEquals(long.class, getPetByIdParam1.getJavaType());
        
        //POST /pet/{petId}
        UserOperation updatePetWithForm = ur.getOperations().get(5);
        assertEquals("updatePetWithForm", updatePetWithForm.getName());
        assertEquals("/pet/{petId}", updatePetWithForm.getPath());
        assertEquals("POST", updatePetWithForm.getVerb());
        assertEquals("application/xml,application/json", updatePetWithForm.getProduces());
        assertEquals("application/x-www-form-urlencoded", updatePetWithForm.getConsumes());
        assertEquals(3, updatePetWithForm.getParameters().size());
        Parameter updatePetWithFormParam1 = updatePetWithForm.getParameters().get(0);
        assertEquals("petId", updatePetWithFormParam1.getName());
        assertEquals(ParameterType.PATH, updatePetWithFormParam1.getType());
        assertEquals(long.class, updatePetWithFormParam1.getJavaType());
        Parameter updatePetWithFormParam2 = updatePetWithForm.getParameters().get(1);
        assertEquals("name", updatePetWithFormParam2.getName());
        assertEquals(ParameterType.FORM, updatePetWithFormParam2.getType());
        assertEquals(String.class, updatePetWithFormParam2.getJavaType());
        Parameter updatePetWithFormParam3 = updatePetWithForm.getParameters().get(2);
        assertEquals("status", updatePetWithFormParam3.getName());
        assertEquals(ParameterType.FORM, updatePetWithFormParam3.getType());
        assertEquals(String.class, updatePetWithFormParam3.getJavaType());
        
        //DELETE /pet/{petId}
        UserOperation deletePet = ur.getOperations().get(6);
        assertEquals("deletePet", deletePet.getName());
        assertEquals("/pet/{petId}", deletePet.getPath());
        assertEquals("DELETE", deletePet.getVerb());
        assertEquals("application/xml,application/json", deletePet.getProduces());
        assertNull(deletePet.getConsumes());
        assertEquals(2, deletePet.getParameters().size());
        Parameter deletePetParam1 = deletePet.getParameters().get(0);
        assertEquals("api_key", deletePetParam1.getName());
        assertEquals(ParameterType.HEADER, deletePetParam1.getType());
        assertEquals(String.class, deletePetParam1.getJavaType());
        Parameter deletePetParam2 = deletePet.getParameters().get(1);
        assertEquals("petId", deletePetParam2.getName());
        assertEquals(ParameterType.PATH, deletePetParam2.getType());
        assertEquals(long.class, deletePetParam2.getJavaType());
        
        //POST /pet/{petId}/uploadImage
        UserOperation uploadFile = ur.getOperations().get(7);
        assertEquals("uploadFile", uploadFile.getName());
        assertEquals("/pet/{petId}/uploadImage", uploadFile.getPath());
        assertEquals("POST", uploadFile.getVerb());
        assertEquals("application/json", uploadFile.getProduces());
        assertEquals("multipart/form-data", uploadFile.getConsumes());
        assertEquals(3, uploadFile.getParameters().size());
        Parameter uploadFileParam1 = uploadFile.getParameters().get(0);
        assertEquals("petId", uploadFileParam1.getName());
        assertEquals(ParameterType.PATH, uploadFileParam1.getType());
        assertEquals(long.class, uploadFileParam1.getJavaType());
        Parameter uploadFileParam2 = uploadFile.getParameters().get(1);
        assertEquals("additionalMetadata", uploadFileParam2.getName());
        assertEquals(ParameterType.FORM, uploadFileParam2.getType());
        assertEquals(String.class, uploadFileParam2.getJavaType());
        Parameter uploadFileParam3 = uploadFile.getParameters().get(2);
        assertEquals("file", uploadFileParam3.getName());
        assertEquals(ParameterType.FORM, uploadFileParam3.getType());
        assertEquals(InputStream.class, uploadFileParam3.getJavaType());
        
    }
    //CHECKSTYLE:ON
    //CHECKSTYLE:OFF
    private void verifyPetUserResource(UserResource ur) {
        assertNotNull(ur);
        assertEquals("/", ur.getPath());
        assertEquals(8, ur.getOperations().size());
        //POST /user
        UserOperation createUser = ur.getOperations().get(0);
        assertEquals("createUser", createUser.getName());
        assertEquals("/user", createUser.getPath());
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
        assertEquals("/user/createWithArray", createWithArray.getPath());
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
        assertEquals("/user/createWithList", createWithList.getPath());
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
        assertEquals("/user/login", loginUser.getPath());
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
        assertEquals("/user/logout", logoutUser.getPath());
        assertEquals("GET", loginUser.getVerb());
        assertEquals("application/xml,application/json", logoutUser.getProduces());
        assertNull(logoutUser.getConsumes());
        assertEquals(0, logoutUser.getParameters().size());
        //TODO: check 200 response type
        
        //GET /user/{username}
        UserOperation getUserByName = ur.getOperations().get(5);
        assertEquals("getUserByName", getUserByName.getName());
        assertEquals("/user/{username}", getUserByName.getPath());
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
        assertEquals("/user/{username}", updateUser.getPath());
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
        assertEquals("/user/{username}", deleteUser.getPath());
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
        assertEquals("/", ur.getPath());
        assertEquals(4, ur.getOperations().size());
        //GET /store/inventory
        UserOperation getInv = ur.getOperations().get(0);
        assertEquals("getInventory", getInv.getName());
        assertEquals("/store/inventory", getInv.getPath());
        assertEquals("GET", getInv.getVerb());
        assertEquals("application/json", getInv.getProduces());
        assertNull(getInv.getConsumes());
        assertEquals(0, getInv.getParameters().size());
        //TODO: check 200 response type
        
        //POST /store/inventory
        UserOperation placeOrder = ur.getOperations().get(1);
        assertEquals("placeOrder", placeOrder.getName());
        assertEquals("/store/order", placeOrder.getPath());
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
        assertEquals("/store/order/{orderId}", getOrderById.getPath());
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
        assertEquals("/store/order/{orderId}", deleteOrder.getPath());
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
    public void testConvertSimpleDocToUserApp() {
        UserApplication ap = SwaggerParseUtils.getUserApplication("/swagger20.json");
        assertNotNull(ap);
        assertEquals("/services/helloservice", ap.getBasePath());
        Map<String, UserResource> map = ap.getResourcesAsMap();
        assertEquals(2, map.size());

        UserResource ur = map.get("sayHello");
        assertNotNull(ur);
        assertEquals("/", ur.getPath());
        assertEquals(1, ur.getOperations().size());
        UserOperation op = ur.getOperations().get(0);
        assertEquals("sayHello", op.getName());
        assertEquals("/sayHello/{a}", op.getPath());
        assertEquals("GET", op.getVerb());
        assertEquals("text/plain", op.getProduces());

        assertEquals(1, op.getParameters().size());
        Parameter param1 = op.getParameters().get(0);
        assertEquals("a", param1.getName());
        assertEquals(ParameterType.PATH, param1.getType());
        assertEquals(String.class, param1.getJavaType());

        UserResource ur2 = map.get("sayHello2");
        assertNotNull(ur2);
        assertEquals("/", ur2.getPath());
        assertEquals(1, ur2.getOperations().size());
        UserOperation op2 = ur2.getOperations().get(0);
        assertEquals("sayHello", op2.getName());
        assertEquals("/sayHello2/{a}", op2.getPath());
        assertEquals("GET", op2.getVerb());
        assertEquals("text/plain", op2.getProduces());

        assertEquals(1, op2.getParameters().size());
        Parameter param2 = op.getParameters().get(0);
        assertEquals("a", param2.getName());
        assertEquals(ParameterType.PATH, param2.getType());
        assertEquals(String.class, param2.getJavaType());

    }
    
    @Test
    public void testConvertSimpleDocTagAndPathMismatchToUserApp() {
        UserApplication ap = SwaggerParseUtils.getUserApplication("/swagger20TagsPathsMismatch.json");
        assertNotNull(ap);
        assertEquals("/services/helloservice", ap.getBasePath());
        Map<String, UserResource> map = ap.getResourcesAsMap();
        assertEquals(2, map.size());

        UserResource ur = map.get("sayHello");
        assertNotNull(ur);
        assertEquals("/", ur.getPath());
        assertEquals(1, ur.getOperations().size());
        UserOperation op = ur.getOperations().get(0);
        assertEquals("sayHello", op.getName());
        assertEquals("/sayHi/{a}", op.getPath());
        assertEquals("GET", op.getVerb());
        assertEquals("text/plain", op.getProduces());

        assertEquals(1, op.getParameters().size());
        Parameter param1 = op.getParameters().get(0);
        assertEquals("a", param1.getName());
        assertEquals(ParameterType.PATH, param1.getType());
        assertEquals(String.class, param1.getJavaType());

        UserResource ur2 = map.get("sayHello2");
        assertNotNull(ur2);
        assertEquals("/", ur2.getPath());
        assertEquals(1, ur2.getOperations().size());
        UserOperation op2 = ur2.getOperations().get(0);
        assertEquals("sayHello", op2.getName());
        assertEquals("/sayHi2/{a}", op2.getPath());
        assertEquals("GET", op2.getVerb());
        assertEquals("text/plain", op2.getProduces());

        assertEquals(1, op2.getParameters().size());
        Parameter param2 = op.getParameters().get(0);
        assertEquals("a", param2.getName());
        assertEquals(ParameterType.PATH, param2.getType());
        assertEquals(String.class, param2.getJavaType());

    }
    
    @Test
    public void testConvertSimpleDocNoTagsToUserApp() {
        UserApplication ap = SwaggerParseUtils.getUserApplication("/swagger20NoTags.json");
        assertNotNull(ap);
        assertEquals("/services/helloservice", ap.getBasePath());
        Map<String, UserResource> map = ap.getResourcesAsMap();
        assertEquals(1, map.size());

        UserResource ur = map.get("");
        assertNotNull(ur);
        assertEquals("/", ur.getPath());
        assertEquals(2, ur.getOperations().size());
        UserOperation op = ur.getOperations().get(0);
        assertEquals("sayHello", op.getName());
        assertEquals("/sayHello/{a}", op.getPath());
        assertEquals("GET", op.getVerb());
        assertEquals("text/plain", op.getProduces());

        assertEquals(1, op.getParameters().size());
        Parameter param1 = op.getParameters().get(0);
        assertEquals("a", param1.getName());
        assertEquals(ParameterType.PATH, param1.getType());
        assertEquals(String.class, param1.getJavaType());

        UserOperation op2 = ur.getOperations().get(1);
        assertEquals("sayHello2", op2.getName());
        assertEquals("/sayHello2/{a}", op2.getPath());
        assertEquals("GET", op2.getVerb());
        assertEquals("text/plain", op2.getProduces());

        assertEquals(1, op2.getParameters().size());
        Parameter param2 = op.getParameters().get(0);
        assertEquals("a", param2.getName());
        assertEquals(ParameterType.PATH, param2.getType());
        assertEquals(String.class, param2.getJavaType());

    }

    @Test
    public void testConvertSwaggerWithNullValuesForOperations() {
        UserApplication ap = SwaggerParseUtils.getUserApplication("/swagger2petShopWithNullOperations.json");
        assertNotNull(ap);
    }
}