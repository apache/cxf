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
package org.apache.cxf.jaxrs.swagger.openapi;

import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SwaggerToOpenApiConversionUtilsTest {

    @Test
    public void testConvertFromSwaggerToOpenApi() {
        OpenApiConfiguration cfg = new OpenApiConfiguration();
        String s = SwaggerToOpenApiConversionUtils.getOpenApiFromSwaggerLoc("/swagger2petShop.json");
        JsonMapObjectReaderWriter readerWriter = new JsonMapObjectReaderWriter();
        JsonMapObject sw3 = readerWriter.fromJsonToJsonObject(s);
        assertEquals("3.0.0", sw3.getStringProperty("openapi"));
        verifyServersProperty(sw3);
        verifyInfoProperty(sw3);
        verifyTagsProperty(sw3);
        verifyPathsProperty(sw3, cfg);
        verifyComponentsProperty(sw3, cfg);
    }
    
    @Test
    public void testConvertFromSwaggerToOpenApiWithRequestBodies() {
        OpenApiConfiguration cfg = new OpenApiConfiguration();
        cfg.setCreateRequestBodies(true);
        String s = SwaggerToOpenApiConversionUtils.getOpenApiFromSwaggerLoc("/swagger2petShop.json", cfg);
        JsonMapObjectReaderWriter readerWriter = new JsonMapObjectReaderWriter();
        JsonMapObject sw3 = readerWriter.fromJsonToJsonObject(s);
        assertEquals("3.0.0", sw3.getStringProperty("openapi"));
        verifyServersProperty(sw3);
        verifyInfoProperty(sw3);
        verifyTagsProperty(sw3);
        verifyPathsProperty(sw3, cfg);
        verifyComponentsProperty(sw3, cfg);
    }

    private void verifyPathsProperty(JsonMapObject sw3, OpenApiConfiguration cfg) {
        JsonMapObject paths = sw3.getJsonMapProperty("paths");
        assertEquals(14, paths.size());
        // /pet
        verifyPetPath(paths, cfg);
        // /pet/findByStatus
        verifyPetFindByStatusPath(paths);
        // /pet/findByTags
        verifyPetFindByTagsPath(paths);
        // /pet/{petId}
        verifyPetIdPath(paths);
        // "/pet/{petId}/uploadImage"
        verifyPetIdUploadImagePath(paths);
        // "/store/inventory"
        verifyStoreInventoryPath(paths);
        // "/store/order"
        verifyStoreOrderPath(paths, cfg);
        // "/store/order/{orderId}"
        verifyStoreOrderIdPath(paths);
        // "/user"
        verifyUserPath(paths, cfg);
        // "/user/createWithArray"
        verifyUserCreateWithArrayPath(paths, cfg);
        // "/user/createWithList"        
        verifyUserCreateWithListPath(paths, cfg);
        // "/user/login"        
        verifyUserLoginPath(paths);
        // "/user/logout"        
        verifyUserLogoutPath(paths);
        // "/user/{username}"        
        verifyUserUsernamePath(paths, cfg);
    }

    private void verifyPetPath(JsonMapObject paths, OpenApiConfiguration cfg) {
        // /pet
        JsonMapObject pet = paths.getJsonMapProperty("/pet");
        assertEquals(2, pet.size());
        verifyPetPathPost(pet, cfg);
        verifyPetPathPut(pet, cfg);
    }
    
    private void verifyPetFindByStatusPath(JsonMapObject paths) {
        // /pet/findByStatus
        JsonMapObject pet = paths.getJsonMapProperty("/pet/findByStatus");
        assertEquals(1, pet.size());
        verifyPetFindByStatusOrTags(pet, "findPetsByStatus");
        
    }
    private void verifyPetFindByTagsPath(JsonMapObject paths) {
        // /pet/findByTags
        JsonMapObject pet = paths.getJsonMapProperty("/pet/findByTags");
        assertEquals(1, pet.size());
        verifyPetFindByStatusOrTags(pet, "findPetsByTags");
    }
    
    private void verifyPetIdPath(JsonMapObject paths) {
        // /pet/{petId}
        JsonMapObject pet = paths.getJsonMapProperty("/pet/{petId}");
        assertEquals(3, pet.size());
        verifyPetIdPathPost(pet);
        verifyPetIdPathGet(pet);
        verifyPetIdPathDelete(pet);
    }
    
    private void verifyPetIdUploadImagePath(JsonMapObject paths) {
        // /pet/{petId}/uploadImage
        JsonMapObject pet = paths.getJsonMapProperty("/pet/{petId}/uploadImage");
        assertEquals(1, pet.size());
        verifyPetIdUploadImagePathPost(pet);
    }
    
    private void verifyStoreInventoryPath(JsonMapObject paths) {
        // /store/inventory
        JsonMapObject store = paths.getJsonMapProperty("/store/inventory");
        assertEquals(1, store.size());
        verifyStoreInventoryPathGet(store);
    }
    
    private void verifyStoreOrderPath(JsonMapObject paths, OpenApiConfiguration cfg) {
        // /store/order
        JsonMapObject store = paths.getJsonMapProperty("/store/order");
        assertEquals(1, store.size());
        verifyStoreOrderPathPost(store, cfg);
    }
    
    private void verifyStoreOrderIdPath(JsonMapObject paths) {
        // /store/order/{orderId}
        JsonMapObject store = paths.getJsonMapProperty("/store/order/{orderId}");
        assertEquals(2, store.size());
        verifyStoreOrderIdPathGet(store);
        verifyStoreOrderIdPathDelete(store);
    }
    
    private void verifyUserPath(JsonMapObject paths, OpenApiConfiguration cfg) {
        // /user
        JsonMapObject user = paths.getJsonMapProperty("/user");
        assertEquals(1, user.size());
        verifyUserPathPost(user, cfg);
    }
    
    private void verifyUserCreateWithArrayPath(JsonMapObject paths, OpenApiConfiguration cfg) {
        // /user/createWithArray
        JsonMapObject user = paths.getJsonMapProperty("/user/createWithArray");
        assertEquals(1, user.size());
        verifyUserCreateWithArrayPathPost(user, cfg);
    }
    
    private void verifyUserCreateWithListPath(JsonMapObject paths, OpenApiConfiguration cfg) {
        // /user/createWithList
        JsonMapObject user = paths.getJsonMapProperty("/user/createWithList");
        assertEquals(1, user.size());
        verifyUserCreateWithListPathPost(user, cfg);
    }
    
    private void verifyUserLoginPath(JsonMapObject paths) {
        // /user/login
        JsonMapObject user = paths.getJsonMapProperty("/user/login");
        assertEquals(1, user.size());
        verifyUserLoginPathPost(user);
    }
    
    private void verifyUserLogoutPath(JsonMapObject paths) {
        // /user/logout
        JsonMapObject user = paths.getJsonMapProperty("/user/logout");
        assertEquals(1, user.size());
        verifyUserLogoutPathGet(user);
    }
    
    private void verifyUserUsernamePath(JsonMapObject paths, OpenApiConfiguration cfg) {
        // /user/{username}
        JsonMapObject user = paths.getJsonMapProperty("/user/{username}");
        assertEquals(3, user.size());
        verifyUserUsernamePathGet(user);
        verifyUserUsernamePathPut(user, cfg);
        verifyUserUsernamePathDelete(user);
    }
    
    private void verifyUserLoginPathPost(JsonMapObject user) {
        JsonMapObject userPost = user.getJsonMapProperty("get");
        assertEquals(6, userPost.size());
        testCommonVerbPropsExceptSec(userPost, "loginUser");
        assertNull(userPost.getProperty("requestBody"));
        List<Map<String, Object>> parameters = userPost.getListMapProperty("parameters");
        assertEquals(2, parameters.size());
        verifyUserNameParameter(new JsonMapObject(parameters.get(0)), "username", "query");
        verifyPasswordParameter(new JsonMapObject(parameters.get(1)), "password");
        JsonMapObject responses = userPost.getJsonMapProperty("responses");
        assertEquals(2, responses.size());
        assertNotNull(responses.getJsonMapProperty("400"));
        JsonMapObject okResp = responses.getJsonMapProperty("200");
        assertEquals(3, okResp.size());
        assertNotNull(okResp.getProperty("description"));
        assertNotNull(okResp.getProperty("headers"));
        JsonMapObject content = okResp.getJsonMapProperty("content");
        assertEquals(2, content.size());
        verifySimpleStringContent(content, "application/json");
        verifySimpleStringContent(content, "application/xml");
    }
    
    private void verifyUserLogoutPathGet(JsonMapObject user) {
        JsonMapObject userGet = user.getJsonMapProperty("get");
        assertEquals(5, userGet.size());
        testCommonVerbPropsExceptSec(userGet, "logoutUser");
        assertNull(userGet.getListMapProperty("parameters"));
        assertNull(userGet.getProperty("requestBody"));
        JsonMapObject responses = userGet.getJsonMapProperty("responses");
        assertEquals(1, responses.size());
        testDefaultResponse(userGet);
    }
    
    private void verifyUserUsernamePathGet(JsonMapObject user) {
        JsonMapObject userGet = user.getJsonMapProperty("get");
        assertEquals(6, userGet.size());
        testCommonVerbPropsExceptSec(userGet, "getUserByName");
        List<Map<String, Object>> parameters = userGet.getListMapProperty("parameters");
        assertEquals(1, parameters.size());
        JsonMapObject userParam = new JsonMapObject(parameters.get(0));
        verifyUserNameParameter(userParam, "username", "path");
        assertNull(userGet.getProperty("requestBody"));
        JsonMapObject responses = userGet.getJsonMapProperty("responses");
        assertEquals(3, responses.size());
        assertNotNull(responses.getProperty("400"));
        assertNotNull(responses.getProperty("404"));
        JsonMapObject okResp = responses.getJsonMapProperty("200");
        assertEquals(2, okResp.size());
        assertNotNull(okResp.getProperty("description"));
        JsonMapObject content = okResp.getJsonMapProperty("content");
        assertEquals(2, content.size());
        verifySimpleContent(content, "application/json", "User");
        verifySimpleContent(content, "application/xml", "User");
    }
    
    private void verifyUserUsernamePathPut(JsonMapObject user, OpenApiConfiguration cfg) {
        JsonMapObject userPut = user.getJsonMapProperty("put");
        assertEquals(7, userPut.size());
        testCommonVerbPropsExceptSec(userPut, "updateUser");
        List<Map<String, Object>> parameters = userPut.getListMapProperty("parameters");
        assertEquals(1, parameters.size());
        JsonMapObject userParam = new JsonMapObject(parameters.get(0));
        verifyUserNameParameter(userParam, "username", "path");
        if (cfg.isCreateRequestBodies()) {
            verifyRequestBodyRef(userPut, "User");
        } else {
            JsonMapObject requestBody = userPut.getJsonMapProperty("requestBody");
            assertEquals(3, requestBody.size());
            assertNotNull(requestBody.getProperty("description"));
            assertTrue(requestBody.getBooleanProperty("required"));
            JsonMapObject content = requestBody.getJsonMapProperty("content");
            assertEquals(1, content.size());
            verifySimpleContent(content, "application/json", "User");
        }
        
        JsonMapObject responses = userPut.getJsonMapProperty("responses");
        assertEquals(2, responses.size());
        assertNotNull(responses.getProperty("400"));
        assertNotNull(responses.getProperty("404"));
    }
    
    private void verifyUserUsernamePathDelete(JsonMapObject user) {
        JsonMapObject usetDelete = user.getJsonMapProperty("delete");
        assertEquals(6, usetDelete.size());
        testCommonVerbPropsExceptSec(usetDelete, "deleteUser");
        List<Map<String, Object>> parameters = usetDelete.getListMapProperty("parameters");
        assertEquals(1, parameters.size());
        JsonMapObject userParam = new JsonMapObject(parameters.get(0));
        verifyUserNameParameter(userParam, "username", "path");
        assertNull(usetDelete.getJsonMapProperty("requestBody"));
        JsonMapObject responses = usetDelete.getJsonMapProperty("responses");
        assertEquals(2, responses.size());
        assertNotNull(responses.getProperty("400"));
        assertNotNull(responses.getProperty("404"));
        
        
    }
    
    private void verifyStoreOrderIdPathGet(JsonMapObject store) {
        JsonMapObject storeGet = store.getJsonMapProperty("get");
        assertEquals(6, storeGet.size());
        testCommonVerbPropsExceptSec(storeGet, "getOrderById");
        List<Map<String, Object>> parameters = storeGet.getListMapProperty("parameters");
        assertEquals(1, parameters.size());
        JsonMapObject orderIdParam = new JsonMapObject(parameters.get(0));
        verifyOrderIdParameter(orderIdParam, false);
        assertNull(storeGet.getJsonMapProperty("requestBody"));
        JsonMapObject responses = storeGet.getJsonMapProperty("responses");
        assertEquals(3, responses.size());
        assertNotNull(responses.getProperty("400"));
        assertNotNull(responses.getProperty("404"));
        JsonMapObject okResp = responses.getJsonMapProperty("200");
        assertEquals(2, okResp.size());
        assertNotNull(okResp.getProperty("description"));
        JsonMapObject content = okResp.getJsonMapProperty("content");
        assertEquals(2, content.size());
        verifySimpleContent(content, "application/json", "Order");
        verifySimpleContent(content, "application/xml", "Order");
    }
    
    private void verifyStoreOrderIdPathDelete(JsonMapObject store) {
        JsonMapObject storeGet = store.getJsonMapProperty("delete");
        assertEquals(6, storeGet.size());
        testCommonVerbPropsExceptSec(storeGet, "deleteOrder");
        List<Map<String, Object>> parameters = storeGet.getListMapProperty("parameters");
        assertEquals(1, parameters.size());
        JsonMapObject orderIdParam = new JsonMapObject(parameters.get(0));
        verifyOrderIdParameter(orderIdParam, true);
        assertNull(storeGet.getJsonMapProperty("requestBody"));
        JsonMapObject responses = storeGet.getJsonMapProperty("responses");
        assertEquals(2, responses.size());
        assertNotNull(responses.getProperty("400"));
        assertNotNull(responses.getProperty("404"));
    }
    
    private void verifyStoreOrderPathPost(JsonMapObject store, OpenApiConfiguration cfg) {
        JsonMapObject storePost = store.getJsonMapProperty("post");
        assertEquals(6, storePost.size());
        testCommonVerbPropsExceptSec(storePost, "placeOrder");
        assertNull(storePost.getProperty("parameters"));
        
        if (cfg.isCreateRequestBodies()) {
            verifyRequestBodyRef(storePost, "Order");
        } else {
            JsonMapObject contentIn = verifyRequestBodyContent(storePost);
            assertEquals(1, contentIn.size());
            verifySimpleContent(contentIn, "application/json", "Order");
        }
        JsonMapObject responses = storePost.getJsonMapProperty("responses");
        assertEquals(2, responses.size());
        assertNotNull(responses.getJsonMapProperty("400"));
        JsonMapObject okResp = responses.getJsonMapProperty("200");
        assertEquals(2, okResp.size());
        assertNotNull(okResp.getProperty("description"));
        JsonMapObject contentOut = okResp.getJsonMapProperty("content");
        assertEquals(2, contentOut.size());
        verifySimpleContent(contentOut, "application/json", "Order");
        verifySimpleContent(contentOut, "application/xml", "Order");
    }
    
    private void verifyUserPathPost(JsonMapObject store, OpenApiConfiguration cfg) {
        JsonMapObject userPost = store.getJsonMapProperty("post");
        assertEquals(6, userPost.size());
        testCommonVerbPropsExceptSec(userPost, "createUser");
        assertNull(userPost.getProperty("parameters"));
        if (cfg.isCreateRequestBodies()) {
            verifyRequestBodyRef(userPost, "User");
        } else {
            JsonMapObject contentIn = verifyRequestBodyContent(userPost);
            assertEquals(1, contentIn.size());
            verifySimpleContent(contentIn, "application/json", "User");
        }
        testDefaultResponse(userPost);
    }
    
    private void testDefaultResponse(JsonMapObject json) {
        JsonMapObject responses = json.getJsonMapProperty("responses");
        assertEquals(1, responses.size());
        assertNotNull(responses.getProperty("default"));
        
    }

    private void verifyUserCreateWithArrayPathPost(JsonMapObject store, OpenApiConfiguration cfg) {
        verifyUserCreateWithListOrArrayPathPost(store, "createUsersWithArrayInput", cfg);
    }
    
    private void verifyUserCreateWithListPathPost(JsonMapObject store, OpenApiConfiguration cfg) {
        verifyUserCreateWithListOrArrayPathPost(store, "createUsersWithListInput", cfg);
    }
    
    private void verifyUserCreateWithListOrArrayPathPost(JsonMapObject store, String opId,
                                                         OpenApiConfiguration cfg) {
        JsonMapObject userPost = store.getJsonMapProperty("post");
        assertEquals(6, userPost.size());
        testCommonVerbPropsExceptSec(userPost, opId);
        assertNull(userPost.getProperty("parameters"));
        if (cfg.isCreateRequestBodies()) {
            verifyRequestBodyRef(userPost, "UserArray");
        } else {
            JsonMapObject contentIn = verifyRequestBodyContent(userPost);
            assertEquals(1, contentIn.size());
            verifyArrayContent(contentIn, "application/json", "User");
        }
        testDefaultResponse(userPost);
    }
    
    private void verifyStoreInventoryPathGet(JsonMapObject store) {
        JsonMapObject storeGet = store.getJsonMapProperty("get");
        assertEquals(6, storeGet.size());
        testCommonVerbProps(storeGet, "getInventory");
        assertNull(storeGet.getProperty("parameters"));
        assertNull(storeGet.getJsonMapProperty("requestBody"));
        JsonMapObject responses = storeGet.getJsonMapProperty("responses");
        assertEquals(1, responses.size());
        JsonMapObject okResp = responses.getJsonMapProperty("200");
        assertEquals(2, okResp.size());
        assertNotNull(okResp.getProperty("description"));
        JsonMapObject content = okResp.getJsonMapProperty("content");
        assertEquals(1, content.size());
        verifyMapContent(content, "application/json", "integer", "int32");
    }
    
    private void verifyPetFindByStatusOrTags(JsonMapObject pet, String opId) {
        JsonMapObject petGet = pet.getJsonMapProperty("get");
        boolean findByStatus = "findPetsByStatus".equals(opId);
        
        assertEquals(findByStatus ? 7 : 8, petGet.size());
        testCommonVerbProps(petGet, opId);
        assertNull(petGet.getJsonMapProperty("requestBody"));
        List<Map<String, Object>> parameters = petGet.getListMapProperty("parameters");
        assertEquals(1, parameters.size());
        JsonMapObject param = new JsonMapObject(parameters.get(0));
        assertEquals(findByStatus ? "status" : "tags", param.getProperty("name"));
        assertEquals("query", param.getProperty("in"));
        assertNull(param.getProperty("type"));
        assertNull(param.getProperty("items"));
        assertNull(param.getProperty("collectionFormat"));
        assertTrue(param.getBooleanProperty("explode"));
        assertTrue(param.getBooleanProperty("required"));
        JsonMapObject schema = param.getJsonMapProperty("schema");
        assertEquals(2, schema.size());
        assertEquals("array", schema.getStringProperty("type"));
        assertNotNull(schema.getProperty("items"));
        assertNull(schema.getProperty("requestBody"));
        JsonMapObject responses = petGet.getJsonMapProperty("responses");
        assertEquals(2, responses.size());
        assertNotNull(responses.getProperty("400"));
        JsonMapObject okResp = responses.getJsonMapProperty("200");
        assertEquals(2, okResp.size());
        assertNotNull(okResp.getProperty("description"));
        JsonMapObject content = okResp.getJsonMapProperty("content");
        assertEquals(2, content.size());
        verifyArrayContent(content, "application/json", "Pet");
        verifyArrayContent(content, "application/xml", "Pet");
        if (findByStatus) {
            assertNull(petGet.getProperty("deprecated"));
        } else {
            assertTrue(petGet.getBooleanProperty("deprecated"));
        }
    }
    private void verifyPetIdUploadImagePathPost(JsonMapObject pet) {
        JsonMapObject petPost = pet.getJsonMapProperty("post");
        assertEquals(8, petPost.size());
        testCommonVerbProps(petPost, "uploadFile");
        List<Map<String, Object>> parameters = petPost.getListMapProperty("parameters");
        assertEquals(1, parameters.size());
        JsonMapObject pathIdParam = new JsonMapObject(parameters.get(0));
        verifyPetIdParameter(pathIdParam);
        
        verifyPetFormContent(petPost, "multipart/form-data",
                             "additionalMetadata", "file");
        JsonMapObject responses = petPost.getJsonMapProperty("responses");
        assertEquals(1, responses.size());
        JsonMapObject okResp = responses.getJsonMapProperty("200");
        assertEquals(2, okResp.size());
        assertNotNull(okResp.getProperty("description"));
        JsonMapObject content = okResp.getJsonMapProperty("content");
        assertEquals(1, content.size());
        verifySimpleContent(content, "application/json", "ApiResponse");
        
    }
    private void verifyPetIdPathPost(JsonMapObject pet) {
        JsonMapObject petPost = pet.getJsonMapProperty("post");
        assertEquals(8, petPost.size());
        testCommonVerbProps(petPost, "updatePetWithForm");
        List<Map<String, Object>> parameters = petPost.getListMapProperty("parameters");
        assertEquals(1, parameters.size());
        JsonMapObject pathIdParam = new JsonMapObject(parameters.get(0));
        verifyPetIdParameter(pathIdParam);
        
        verifyPetFormContent(petPost, "application/x-www-form-urlencoded",
                             "name", "status");
        JsonMapObject responses = petPost.getJsonMapProperty("responses");
        assertEquals(1, responses.size());
        assertNotNull(responses.getProperty("405"));
        
    }
    private void verifyPetIdPathGet(JsonMapObject pet) {
        JsonMapObject petGet = pet.getJsonMapProperty("get");
        assertEquals(7, petGet.size());
        testCommonVerbProps(petGet, "getPetById");
        List<Map<String, Object>> parameters = petGet.getListMapProperty("parameters");
        assertEquals(1, parameters.size());
        JsonMapObject pathIdParam = new JsonMapObject(parameters.get(0));
        verifyPetIdParameter(pathIdParam);
        
        assertNull(petGet.getJsonMapProperty("requestBody"));
        JsonMapObject responses = petGet.getJsonMapProperty("responses");
        assertEquals(3, responses.size());
        JsonMapObject okResp = responses.getJsonMapProperty("200");
        assertEquals(2, okResp.size());
        JsonMapObject content = okResp.getJsonMapProperty("content");
        assertEquals(2, content.size());
        verifySimpleContent(content, "application/json", "Pet");
        verifySimpleContent(content, "application/xml", "Pet");
        assertNotNull(responses.getProperty("400"));
        assertNotNull(responses.getProperty("404"));
        
    }
    
    private void verifyPetIdPathDelete(JsonMapObject pet) {
        JsonMapObject petDel = pet.getJsonMapProperty("delete");
        assertEquals(7, petDel.size());
        testCommonVerbProps(petDel, "deletePet");
        List<Map<String, Object>> parameters = petDel.getListMapProperty("parameters");
        assertEquals(2, parameters.size());
        JsonMapObject apiKeyParam = new JsonMapObject(parameters.get(0));
        assertEquals("api_key", apiKeyParam.getProperty("name"));
        assertEquals("header", apiKeyParam.getProperty("in"));
        assertNull(apiKeyParam.getProperty("type"));
        assertNull(apiKeyParam.getProperty("format"));
        assertFalse(apiKeyParam.getBooleanProperty("required"));
        JsonMapObject schema = apiKeyParam.getJsonMapProperty("schema");
        assertEquals(1, schema.size());
        assertEquals("string", schema.getProperty("type"));
        JsonMapObject pathIdParam = new JsonMapObject(parameters.get(1));
        verifyPetIdParameter(pathIdParam);
        assertNull(petDel.getJsonMapProperty("requestBody"));
        JsonMapObject responses = petDel.getJsonMapProperty("responses");
        assertEquals(2, responses.size());
        assertNotNull(responses.getProperty("400"));
        assertNotNull(responses.getProperty("404"));
        
    }
    
    private void verifyPetIdParameter(JsonMapObject param) {
        assertEquals("petId", param.getProperty("name"));
        assertEquals("path", param.getProperty("in"));
        assertNull(param.getProperty("type"));
        assertNull(param.getProperty("format"));
        assertNotNull(param.getProperty("description"));
        assertTrue(param.getBooleanProperty("required"));
        JsonMapObject schema = param.getJsonMapProperty("schema");
        assertEquals(2, schema.size());
        assertEquals("integer", schema.getProperty("type"));
        assertEquals("int64", schema.getProperty("format"));
    }
    
    private void verifyUserNameParameter(JsonMapObject param, String name, String inType) {
        verifyStringParameter(param, "username", inType);
        
    }
    
    private void verifyPasswordParameter(JsonMapObject param, String name) {
        verifyStringParameter(param, "password", "query");
        assertEquals("password", 
                     param.getJsonMapProperty("schema").getProperty("format"));    
    }
    
    private void verifyStringParameter(JsonMapObject param, String name, String inType) {
        assertEquals(name, param.getProperty("name"));
        assertEquals(inType, param.getProperty("in"));
        assertNull(param.getProperty("type"));
        assertNull(param.getProperty("format"));
        assertNotNull(param.getProperty("description"));
        assertTrue(param.getBooleanProperty("required"));
        JsonMapObject schema = param.getJsonMapProperty("schema");
        assertEquals("password".equals(name) ? 2 : 1, schema.size());
        assertEquals("string", schema.getProperty("type"));
    }
    
    private void verifyOrderIdParameter(JsonMapObject param, boolean minOnly) {
        assertEquals("orderId", param.getProperty("name"));
        assertEquals("path", param.getProperty("in"));
        assertNull(param.getProperty("type"));
        assertNull(param.getProperty("format"));
        assertNotNull(param.getProperty("description"));
        assertTrue(param.getBooleanProperty("required"));
        JsonMapObject schema = param.getJsonMapProperty("schema");
        
        assertEquals(minOnly ? 3 : 4, schema.size());
        assertEquals("integer", schema.getProperty("type"));
        assertNotNull(schema.getProperty("minimum"));
        if (!minOnly) {
            assertNotNull(schema.getProperty("maximum"));
        }
    }

    private void verifyPetPathPost(JsonMapObject pet, OpenApiConfiguration cfg) {
        JsonMapObject petPost = pet.getJsonMapProperty("post");
        assertEquals(7, petPost.size());
        testCommonVerbProps(petPost, "addPet");
        assertNull(petPost.getProperty("parameters"));
        if (cfg.isCreateRequestBodies()) {
            verifyRequestBodyRef(petPost, "Pet");
        } else {
            JsonMapObject content = verifyRequestBodyContent(petPost);
            assertEquals(2, content.size());
            verifySimpleContent(content, "application/json", "Pet");
            verifySimpleContent(content, "application/xml", "Pet");
        }
        JsonMapObject responses = petPost.getJsonMapProperty("responses");
        assertEquals(1, responses.size());
        assertNotNull(responses.getProperty("405"));
    }
    private JsonMapObject verifyRequestBodyContent(JsonMapObject json) {
        return verifyRequestBodyContent(json, "requestBody");
    }
    private JsonMapObject verifyRequestBodyContent(JsonMapObject json, String propName) {
        JsonMapObject requestBody = json.getJsonMapProperty(propName);
        assertEquals(3, requestBody.size());
        assertNotNull(requestBody.getProperty("description"));
        assertTrue(requestBody.getBooleanProperty("required"));
        return requestBody.getJsonMapProperty("content");
    }
    private void verifyRequestBodyRef(JsonMapObject json, String refName) {
        JsonMapObject requestBody = json.getJsonMapProperty("requestBody");
        assertEquals(1, requestBody.size());
        assertEquals("#components/requestBodies/" + refName,
                     requestBody.getProperty("$ref"));
    }

    private void verifyPetPathPut(JsonMapObject pet, OpenApiConfiguration cfg) {
        JsonMapObject petPut = pet.getJsonMapProperty("put");
        assertEquals(7, petPut.size());
        testCommonVerbProps(petPut, "updatePet");
        assertNull(petPut.getProperty("parameters"));
        if (cfg.isCreateRequestBodies()) {
            verifyRequestBodyRef(petPut, "Pet");
        } else {
            JsonMapObject content = verifyRequestBodyContent(petPut);
            assertEquals(2, content.size());
            verifySimpleContent(content, "application/json", "Pet");
            verifySimpleContent(content, "application/xml", "Pet");
        }
        JsonMapObject responses = petPut.getJsonMapProperty("responses");
        assertEquals(3, responses.size());
        assertNotNull(responses.getProperty("400"));
        assertNotNull(responses.getProperty("404"));
        assertNotNull(responses.getProperty("405"));
    }
    

    private void verifySimpleContent(JsonMapObject contentMap, String mediaType, String modelName) {
        JsonMapObject content = contentMap.getJsonMapProperty(mediaType);
        assertEquals(1, content.size());
        JsonMapObject schema = content.getJsonMapProperty("schema");
        assertEquals(1, schema.size());
        assertEquals("#components/schemas/" + modelName, schema.getStringProperty("$ref"));
    }
    private void verifySimpleStringContent(JsonMapObject contentMap, String mediaType) {
        JsonMapObject content = contentMap.getJsonMapProperty(mediaType);
        assertEquals(1, content.size());
        JsonMapObject schema = content.getJsonMapProperty("schema");
        assertEquals(1, schema.size());
        assertEquals("string", schema.getStringProperty("type"));
    }
    private void verifyMapContent(JsonMapObject contentMap, String mediaType, String type, String format) {
        JsonMapObject content = contentMap.getJsonMapProperty(mediaType);
        assertEquals(1, content.size());
        JsonMapObject schema = content.getJsonMapProperty("schema");
        assertEquals(2, schema.size());
        assertEquals("object", schema.getStringProperty("type"));
        JsonMapObject additionalProps = schema.getJsonMapProperty("additionalProperties");
        assertEquals(type, additionalProps.getStringProperty("type"));
        assertEquals(format, additionalProps.getStringProperty("format"));
    }
    private void verifyPetFormContent(JsonMapObject petPost,
                                      String mediaType,
                                      String firstPropName,
                                      String secondPropName) {
        JsonMapObject requestBody = petPost.getJsonMapProperty("requestBody");
        assertEquals(1, requestBody.size());
        JsonMapObject contentMap = requestBody.getJsonMapProperty("content");
        assertEquals(1, contentMap.size());
               
        
        JsonMapObject content = contentMap.getJsonMapProperty(mediaType);
        assertEquals(1, content.size());
        JsonMapObject schema = content.getJsonMapProperty("schema");
        assertEquals(2, schema.size());
        assertEquals("object", schema.getStringProperty("type"));
        JsonMapObject props = schema.getJsonMapProperty("properties");
        assertEquals(2, props.size());
        JsonMapObject firstProp = props.getJsonMapProperty(firstPropName);
        assertNotNull(firstProp.getProperty("description"));
        assertEquals("string", firstProp.getProperty("type"));
        JsonMapObject secondProp = props.getJsonMapProperty(secondPropName);
        assertNotNull(secondProp.getProperty("description"));
        assertEquals("string", secondProp.getProperty("type"));
        if ("file".equals(secondPropName)) {
            assertEquals("binary", secondProp.getProperty("format"));
        }
    }
    private void verifyArrayContent(JsonMapObject contentMap, String mediaType, String modelName) {
        JsonMapObject content = contentMap.getJsonMapProperty(mediaType);
        assertEquals(1, content.size());
        JsonMapObject schema = content.getJsonMapProperty("schema");
        assertEquals(2, schema.size());
        assertEquals("array", schema.getStringProperty("type"));
        JsonMapObject items = schema.getJsonMapProperty("items");
        assertEquals(1, items.size());
        assertEquals("#components/schemas/" + modelName, items.getStringProperty("$ref"));
    }

    private void verifyServersProperty(JsonMapObject sw3) {
        List<Map<String, Object>> servers = sw3.getListMapProperty("servers");
        assertEquals(1, servers.size());
        JsonMapObject server = new JsonMapObject(servers.get(0));
        assertEquals(1, server.size());
        assertEquals("http://petstore.swagger.io/v2", server.getStringProperty("url"));
    }

    private void verifyInfoProperty(JsonMapObject sw3) {
        //TODO: check info properties as well, though it's only copied from the original doc
        assertNotNull(sw3.getJsonMapProperty("info"));
    }
    
    private void verifyTagsProperty(JsonMapObject sw3) {
        //TODO: check info properties as well, though it's only copied from the original doc
        assertNotNull(sw3.getListMapProperty("tags"));
    }
    
    private void verifyComponentsProperty(JsonMapObject sw3, OpenApiConfiguration cfg) {
        JsonMapObject comps = sw3.getJsonMapProperty("components");
        assertEquals(3, comps.size());
        JsonMapObject requestBodies = comps.getJsonMapProperty("requestBodies");
        if (cfg.isCreateRequestBodies()) {
            assertEquals(4, requestBodies.size());
            // UserArray
            JsonMapObject userArrayContent = verifyRequestBodyContent(requestBodies, "UserArray");
            assertEquals(1, userArrayContent.size());
            verifyArrayContent(userArrayContent, "application/json", "User");
            // Pet
            JsonMapObject petContent = verifyRequestBodyContent(requestBodies, "Pet");
            assertEquals(2, petContent.size());
            verifySimpleContent(petContent, "application/json", "Pet");
            verifySimpleContent(petContent, "application/xml", "Pet");
            // User
            JsonMapObject userContent = verifyRequestBodyContent(requestBodies, "User");
            assertEquals(1, userContent.size());
            verifySimpleContent(userContent, "application/json", "User");
            // Order
            JsonMapObject orderContent = verifyRequestBodyContent(requestBodies, "Order");
            assertEquals(1, orderContent.size());
            verifySimpleContent(orderContent, "application/json", "Order");
        } else {
            assertEquals(0, requestBodies.size());
        }
        assertNotNull(comps.getJsonMapProperty("schemas"));
        assertNotNull(comps.getJsonMapProperty("securitySchemes"));
        
    }

    private void testCommonVerbProps(JsonMapObject method, String opId) {
        testCommonVerbPropsExceptSec(method, opId);
        assertNotNull(method.getProperty("security"));
    }
    private void testCommonVerbPropsExceptSec(JsonMapObject method, String opId) {
        assertNotNull(method.getProperty("tags"));
        assertNotNull(method.getProperty("summary"));
        assertNotNull(method.getProperty("description"));
        assertEquals(opId, method.getStringProperty("operationId"));
        assertNull(method.getProperty("produces"));
        assertNull(method.getProperty("consumes"));
        
    }
}