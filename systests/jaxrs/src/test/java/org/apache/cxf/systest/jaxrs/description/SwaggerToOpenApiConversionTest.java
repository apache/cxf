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
package org.apache.cxf.systest.jaxrs.description;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.swagger.Swagger2Feature;
import org.apache.cxf.jaxrs.swagger.openapi.SwaggerToOpenApiConversionFilter;
import org.apache.cxf.systest.jaxrs.description.AbstractSwagger2ServiceDescriptionTest.XForwarded;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SwaggerToOpenApiConversionTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(SwaggerToOpenApiConversionTest.class);
    static final String SECURITY_DEFINITION_NAME = "basicAuth";

    private static final String CONTACT = "cxf@apache.org";
    private static final String TITLE = "CXF unittest";
    private static final String DESCRIPTION = "API Description";
    private static final String LICENSE = "API License";
    private static final String LICENSE_URL = "API License URL";

    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        @Override
        protected void run() {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStoreSwagger2.class);
            sf.setResourceProvider(BookStoreSwagger2.class,
                new SingletonResourceProvider(new BookStoreSwagger2()));
            sf.setProvider(new SwaggerToOpenApiConversionFilter());
            final Swagger2Feature feature = createSwagger2Feature();
            sf.setFeatures(Arrays.asList(feature));
            sf.setAddress("http://localhost:" + PORT + "/");
            sf.create();
        }

        protected Swagger2Feature createSwagger2Feature() {
            final Swagger2Feature feature = new Swagger2Feature();
            feature.setContact(CONTACT);
            feature.setTitle(TITLE);
            feature.setDescription(DESCRIPTION);
            feature.setLicense(LICENSE);
            feature.setLicenseUrl(LICENSE_URL);
            feature.setSecurityDefinitions(Collections.singletonMap(SECURITY_DEFINITION_NAME,
               new io.swagger.models.auth.BasicAuthDefinition()));
            return feature;
        }

        protected static void start(final Server s) {
            try {
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, true));
        createStaticBus();
    }


    @Test
    public void testOpenApiJSON() throws Exception {
        final WebClient client = createWebClient("/openapi.json");
        String openApiResponse = client.get(String.class);
        JsonMapObject openApiJson = new JsonMapObjectReaderWriter().fromJsonToJsonObject(openApiResponse);
        assertEquals(6, openApiJson.size());
        // version
        assertEquals("3.0.0", openApiJson.getProperty("openapi"));
        // servers
        List<Map<String, Object>> servers = openApiJson.getListMapProperty("servers");
        assertEquals(1, servers.size());
        assertEquals(1, servers.get(0).size());
        assertEquals("http://localhost:" + PORT, servers.get(0).get("url"));
        // info
        JsonMapObject info = openApiJson.getJsonMapProperty("info");
        assertEquals(4, info.size());
        assertEquals(DESCRIPTION, info.getProperty("description"));
        assertEquals(TITLE, info.getProperty("title"));
        JsonMapObject contact = info.getJsonMapProperty("contact");
        assertEquals(1, contact.size());
        assertEquals(CONTACT, contact.getProperty("name"));
        JsonMapObject license = info.getJsonMapProperty("license");
        assertEquals(2, license.size());
        assertEquals(LICENSE, license.getProperty("name"));
        assertEquals(LICENSE_URL, license.getProperty("url"));
        // tags
        List<Map<String, Object>> tags = openApiJson.getListMapProperty("tags");
        assertEquals(1, tags.size());
        assertEquals(1, tags.get(0).size());
        assertEquals("bookstore", tags.get(0).get("name"));
        // paths
        JsonMapObject paths = openApiJson.getJsonMapProperty("paths");
        assertEquals(2, paths.size());
        //1: bookstore
        JsonMapObject bookstore = paths.getJsonMapProperty("/bookstore");
        assertEquals(1, bookstore.size());
        // get
        verifyBookStoreGet(bookstore);
        //2: bookstore/{id}
        JsonMapObject bookstoreId = paths.getJsonMapProperty("/bookstore/{id}");
        assertEquals(2, bookstoreId.size());
        // get
        verifyBookStoreIdGet(bookstoreId);
        // delete
        verifyBookStoreIdDelete(bookstoreId);

        // components
        JsonMapObject comps = openApiJson.getJsonMapProperty("components");
        assertEquals(3, comps.size());
        JsonMapObject requestBodies = comps.getJsonMapProperty("requestBodies");
        assertEquals(0, requestBodies.size());
        JsonMapObject schemas = comps.getJsonMapProperty("schemas");
        assertEquals(1, schemas.size());
        JsonMapObject secSchemes = comps.getJsonMapProperty("securitySchemes");
        assertEquals(1, secSchemes.size());

        // Finally check swagger.json can still be generated.
        doTestSwagger2JSON();
    }
    private void verifyBookStoreGet(JsonMapObject bookstore) {
        JsonMapObject bookstoreGet = bookstore.getJsonMapProperty("get");
        assertEquals(6, bookstoreGet.size());
        List<String> bookstoreGetTags = bookstoreGet.getListStringProperty("tags");
        assertEquals(1, bookstoreGetTags.size());
        assertEquals("bookstore", bookstoreGetTags.get(0));
        assertEquals("Get books", bookstoreGet.getProperty("summary"));
        assertEquals("Get books", bookstoreGet.getProperty("description"));
        assertEquals("getBooks", bookstoreGet.getProperty("operationId"));
        List<Map<String, Object>> bookstoreGetParams = bookstoreGet.getListMapProperty("parameters");
        assertEquals(1, bookstoreGetParams.size());
        JsonMapObject firstParam = new JsonMapObject(bookstoreGetParams.get(0));
        assertEquals(5, firstParam.size());
        assertEquals("page", firstParam.getProperty("name"));
        assertEquals("query", firstParam.getProperty("in"));
        assertEquals("Page to fetch", firstParam.getProperty("description"));
        assertTrue(firstParam.getBooleanProperty("required"));
        JsonMapObject schema = firstParam.getJsonMapProperty("schema");
        assertEquals(3, schema.size());
        assertEquals("integer", schema.getProperty("type"));
        assertEquals("int32", schema.getProperty("format"));
        assertEquals(Integer.valueOf(1), schema.getIntegerProperty("default"));
        JsonMapObject responses = bookstoreGet.getJsonMapProperty("responses");
        assertEquals(1, responses.size());
        JsonMapObject okResp = responses.getJsonMapProperty("200");
        assertEquals(2, okResp.size());
        assertEquals("successful operation", okResp.getProperty("description"));
        JsonMapObject content = okResp.getJsonMapProperty("content");
        assertEquals(1, content.size());
        JsonMapObject jsonResp = content.getJsonMapProperty("application/json");
        assertEquals(1, jsonResp.size());
        JsonMapObject jsonRespSchema = jsonResp.getJsonMapProperty("schema");
        assertEquals(2, jsonRespSchema.size());
        assertEquals("array", jsonRespSchema.getProperty("type"));
        JsonMapObject jsonRespSchemaItems = jsonRespSchema.getJsonMapProperty("items");
        assertEquals(1, jsonRespSchemaItems.size());
        assertEquals("#components/schemas/Book", jsonRespSchemaItems.getProperty("$ref"));
    }
    private void verifyBookStoreIdGet(JsonMapObject bookstore) {
        JsonMapObject bookstoreGet = bookstore.getJsonMapProperty("get");
        assertEquals(6, bookstoreGet.size());
        List<String> bookstoreGetTags = bookstoreGet.getListStringProperty("tags");
        assertEquals(1, bookstoreGetTags.size());
        assertEquals("bookstore", bookstoreGetTags.get(0));
        assertEquals("Get book by Id", bookstoreGet.getProperty("summary"));
        assertEquals("Get book by Id", bookstoreGet.getProperty("description"));
        assertEquals("getBook", bookstoreGet.getProperty("operationId"));
        List<Map<String, Object>> bookstoreGetParams = bookstoreGet.getListMapProperty("parameters");
        assertEquals(1, bookstoreGetParams.size());
        JsonMapObject firstParam = new JsonMapObject(bookstoreGetParams.get(0));
        assertEquals(5, firstParam.size());
        assertEquals("id", firstParam.getProperty("name"));
        assertEquals("path", firstParam.getProperty("in"));
        assertEquals("id", firstParam.getProperty("description"));
        assertTrue(firstParam.getBooleanProperty("required"));
        JsonMapObject schema = firstParam.getJsonMapProperty("schema");
        assertEquals(2, schema.size());
        assertEquals("integer", schema.getProperty("type"));
        assertEquals("int64", schema.getProperty("format"));
        JsonMapObject responses = bookstoreGet.getJsonMapProperty("responses");
        assertEquals(1, responses.size());
        JsonMapObject okResp = responses.getJsonMapProperty("200");
        assertEquals(2, okResp.size());
        assertEquals("successful operation", okResp.getProperty("description"));
        JsonMapObject content = okResp.getJsonMapProperty("content");
        assertEquals(1, content.size());
        JsonMapObject jsonResp = content.getJsonMapProperty("application/json");
        assertEquals(1, jsonResp.size());
        JsonMapObject jsonRespSchema = jsonResp.getJsonMapProperty("schema");
        assertEquals(1, jsonRespSchema.size());
        assertEquals("#components/schemas/Book", jsonRespSchema.getProperty("$ref"));
    }
    private void verifyBookStoreIdDelete(JsonMapObject bookstore) {
        JsonMapObject bookstoreDel = bookstore.getJsonMapProperty("delete");
        assertEquals(6, bookstoreDel.size());
        List<String> bookstoreDelTags = bookstoreDel.getListStringProperty("tags");
        assertEquals(1, bookstoreDelTags.size());
        assertEquals("bookstore", bookstoreDelTags.get(0));
        assertEquals("Delete book", bookstoreDel.getProperty("summary"));
        assertEquals("Delete book", bookstoreDel.getProperty("description"));
        assertEquals("delete", bookstoreDel.getProperty("operationId"));
        List<Map<String, Object>> bookstoreDelParams = bookstoreDel.getListMapProperty("parameters");
        assertEquals(1, bookstoreDelParams.size());
        JsonMapObject firstParam = new JsonMapObject(bookstoreDelParams.get(0));
        assertEquals(5, firstParam.size());
        assertEquals("id", firstParam.getProperty("name"));
        assertEquals("path", firstParam.getProperty("in"));
        assertEquals("id", firstParam.getProperty("description"));
        assertTrue(firstParam.getBooleanProperty("required"));
        JsonMapObject schema = firstParam.getJsonMapProperty("schema");
        assertEquals(1, schema.size());
        assertEquals("string", schema.getProperty("type"));
        JsonMapObject responses = bookstoreDel.getJsonMapProperty("responses");
        assertEquals(1, responses.size());
        JsonMapObject okResp = responses.getJsonMapProperty("default");
        assertEquals(1, okResp.size());
        assertEquals("successful operation", okResp.getProperty("description"));
    }

    protected WebClient createWebClient(final String url) {
        WebClient wc = WebClient.create("http://localhost:" + PORT + url)
            .accept(MediaType.APPLICATION_JSON);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        return wc;
    }

    private void doTestSwagger2JSON() throws Exception {
        final WebClient client = createWebClient("/swagger.json");
        AbstractSwagger2ServiceDescriptionTest.doTestApiListingIsProperlyReturnedJSON(client, XForwarded.NONE);
    }
}
