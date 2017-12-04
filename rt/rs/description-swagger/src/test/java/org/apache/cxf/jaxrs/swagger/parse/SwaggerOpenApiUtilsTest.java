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

import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;

import org.junit.Assert;
import org.junit.Test;

public class SwaggerOpenApiUtilsTest extends Assert {

    @Test
    public void testConvertFromSwaggerToOpenApi() {
        
        String s = SwaggerOpenApiUtils.getOpenApiFromSwaggerLoc("/swagger2petShop.json");
        JsonMapObjectReaderWriter readerWriter = new JsonMapObjectReaderWriter();
        JsonMapObject sw3 = readerWriter.fromJsonToJsonObject(s);
        assertEquals("3.0.0", sw3.getStringProperty("openapi"));
        verifyServersProperty(sw3);
        verifyInfoProperty(sw3);
        verifyTagsProperty(sw3);
        verifyPathsProperty(sw3);
        verifyComponentsProperty(sw3);
    }

    private void verifyPathsProperty(JsonMapObject sw3) {
        JsonMapObject paths = sw3.getJsonMapProperty("paths");
        assertEquals(14, paths.size());
        // /pet
        verifyPetPath(paths);
        // /pet/findByStatus
        verifyPetFindByStatusPath(paths);
        // /pet/findByTags
        verifyPetFindByTagsPath(paths);
        // /pet/{petId}
        verifyPetIdPath(paths);
        // "/pet/{petId}/uploadImage"
        verifyPetIdUploadImagePath(paths);
    }

    private void verifyPetPath(JsonMapObject paths) {
        // /pet
        JsonMapObject pet = paths.getJsonMapProperty("/pet");
        assertEquals(2, pet.size());
        verifyPetPathPost(pet);
        verifyPetPathPut(pet);
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
        assertTrue(param.getBooleanProperty("required"));
        JsonMapObject schema = param.getJsonMapProperty("schema");
        assertEquals(2, schema.size());
        assertEquals("integer", schema.getProperty("type"));
        assertEquals("int64", schema.getProperty("format"));
    }

    private void verifyPetPathPost(JsonMapObject pet) {
        JsonMapObject petPost = pet.getJsonMapProperty("post");
        assertEquals(7, petPost.size());
        testCommonVerbProps(petPost, "addPet");
        assertNull(petPost.getProperty("parameters"));
        JsonMapObject requestBody = petPost.getJsonMapProperty("requestBody");
        assertEquals(3, requestBody.size());
        assertNotNull(requestBody.getProperty("description"));
        assertTrue(requestBody.getBooleanProperty("required"));
        JsonMapObject content = requestBody.getJsonMapProperty("content");
        assertEquals(2, content.size());
        verifySimpleContent(content, "application/json", "Pet");
        verifySimpleContent(content, "application/xml", "Pet");
        JsonMapObject responses = petPost.getJsonMapProperty("responses");
        assertEquals(1, responses.size());
        assertNotNull(responses.getProperty("405"));
    }
    private void verifyPetPathPut(JsonMapObject pet) {
        JsonMapObject petPut = pet.getJsonMapProperty("put");
        assertEquals(7, petPut.size());
        testCommonVerbProps(petPut, "updatePet");
        assertNull(petPut.getProperty("parameters"));
        JsonMapObject requestBody = petPut.getJsonMapProperty("requestBody");
        assertEquals(3, requestBody.size());
        assertNotNull(requestBody.getProperty("description"));
        assertTrue(requestBody.getBooleanProperty("required"));
        JsonMapObject content = requestBody.getJsonMapProperty("content");
        assertEquals(2, content.size());
        verifySimpleContent(content, "application/json", "Pet");
        verifySimpleContent(content, "application/xml", "Pet");
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
    
    private void verifyComponentsProperty(JsonMapObject sw3) {
        JsonMapObject comps = sw3.getJsonMapProperty("components");
        assertEquals(3, comps.size());
        JsonMapObject requestBodies = comps.getJsonMapProperty("requestBodies");
        assertEquals(0, requestBodies.size());
        assertNotNull(comps.getJsonMapProperty("schemas"));
        assertNotNull(comps.getJsonMapProperty("securitySchemes"));
        
    }

    private void testCommonVerbProps(JsonMapObject method, String opId) {
        assertNotNull(method.getProperty("tags"));
        assertNotNull(method.getProperty("summary"));
        assertNotNull(method.getProperty("description"));
        assertNotNull(method.getProperty("security"));
        assertEquals(opId, method.getStringProperty("operationId"));
        assertNull(method.getProperty("produces"));
        assertNull(method.getProperty("consumes"));
        
    }
}
