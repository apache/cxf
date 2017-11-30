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
        verifyPetPath(paths);
    }

    private void verifyPetPath(JsonMapObject paths) {
        // /pet
        JsonMapObject pet = paths.getJsonMapProperty("/pet");
        assertEquals(2, pet.size());
        verifyPetPathPost(pet);
        verifyPetPathPut(pet);
    }
    
    private void verifyPetPathPost(JsonMapObject pet) {
        JsonMapObject petPost = pet.getJsonMapProperty("post");
        assertEquals(7, petPost.size());
        assertNotNull(petPost.getProperty("tags"));
        assertNotNull(petPost.getProperty("summary"));
        assertNotNull(petPost.getProperty("description"));
        assertNotNull(petPost.getProperty("security"));
        assertEquals("addPet", petPost.getStringProperty("operationId"));
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
        assertNotNull(petPut.getProperty("tags"));
        assertNotNull(petPut.getProperty("summary"));
        assertNotNull(petPut.getProperty("description"));
        assertNotNull(petPut.getProperty("security"));
        assertEquals("updatePet", petPut.getStringProperty("operationId"));
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

    private void verifyServersProperty(JsonMapObject sw3) {
        List<JsonMapObject> servers = sw3.getListJsonMapProperty("servers");
        assertEquals(1, servers.size());
        JsonMapObject server = servers.get(0);
        assertEquals(1, server.asMap().size());
        assertEquals("http://petstore.swagger.io/v2", server.getStringProperty("url"));
    }

    private void verifyInfoProperty(JsonMapObject sw3) {
        //TODO: check info properties as well, though it's only copied from the original doc
        assertNotNull(sw3.getJsonMapProperty("info"));
    }
    
    private void verifyTagsProperty(JsonMapObject sw3) {
        //TODO: check info properties as well, though it's only copied from the original doc
        assertNotNull(sw3.getListJsonMapProperty("tags"));
    }
    
    private void verifyComponentsProperty(JsonMapObject sw3) {
        JsonMapObject comps = sw3.getJsonMapProperty("components");
        assertEquals(3, comps.size());
        JsonMapObject requestBodies = comps.getJsonMapProperty("requestBodies");
        assertEquals(0, requestBodies.size());
        assertNotNull(comps.getJsonMapProperty("schemas"));
        assertNotNull(comps.getJsonMapProperty("securitySchemes"));
        
    }

}
