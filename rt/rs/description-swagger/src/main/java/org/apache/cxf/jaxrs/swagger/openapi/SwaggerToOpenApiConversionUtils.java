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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

public final class SwaggerToOpenApiConversionUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(SwaggerToOpenApiConversionUtils.class);
    
    private static final List<String> SIMPLE_TYPE_RELATED_PROPS =
        Arrays.asList("format", "minimum", "maximum", "default"); 
    
    private SwaggerToOpenApiConversionUtils() {
        
    }
    
    public static String getOpenApiFromSwaggerLoc(String loc) {
        return getOpenApiFromSwaggerLoc(loc, null);
    }
    
    public static String getOpenApiFromSwaggerLoc(String loc, OpenApiConfiguration cfg) {
        return getOpenApiFromSwaggerLoc(loc, cfg, BusFactory.getThreadDefaultBus());
    }
    
    public static String getOpenApiFromSwaggerLoc(String loc, OpenApiConfiguration cfg, Bus bus) {
        try {
            InputStream is = ResourceUtils.getResourceStream(loc, bus);
            if (is == null) {
                return null;
            }
            return getOpenApiFromSwaggerStream(is, cfg);
        } catch (Exception ex) {
            LOG.warning("Problem with processing a user model at " + loc + ", exception: "
                + ExceptionUtils.getStackTrace(ex));
        }
        return null;
    }
    
    public static String getOpenApiFromSwaggerStream(InputStream is) throws IOException {
        return getOpenApiFromSwaggerStream(is, null);
    }
    
    public static String getOpenApiFromSwaggerStream(InputStream is, OpenApiConfiguration cfg) throws IOException {
        return getOpenApiFromSwaggerJson(null, IOUtils.readStringFromStream(is), cfg);
    }
    
    public static String getOpenApiFromSwaggerJson(String json) throws IOException {
        return getOpenApiFromSwaggerJson(null, json, null);
    }
    
    public static String getOpenApiFromSwaggerJson(
            MessageContext ctx, String json, OpenApiConfiguration cfg) throws IOException {

        JsonMapObjectReaderWriter readerWriter = new JsonMapObjectReaderWriter();
        JsonMapObject sw2 = readerWriter.fromJsonToJsonObject(json);
        JsonMapObject sw3 = new JsonMapObject();
        
        // "openapi"
        sw3.setProperty("openapi", "3.0.0");
        
        // "servers"
        setServersProperty(ctx, sw2, sw3);
        
        // "info"
        JsonMapObject infoObject = sw2.getJsonMapProperty("info");
        if (infoObject != null) {
            sw3.setProperty("info", infoObject);
        }
        
        // "tags"
        List<Map<String, Object>> tagsObject = sw2.getListMapProperty("tags");
        if (tagsObject != null) {
            sw3.setProperty("tags", tagsObject);
        }
        
        // paths
        Map<String, JsonMapObject> requestBodies = cfg != null && cfg.isCreateRequestBodies() 
            ? new LinkedHashMap<>() : null;
        setPathsProperty(sw2, sw3, requestBodies);
        
        // components
        setComponentsProperty(sw2, sw3, requestBodies);
        
        // externalDocs
        Object externalDocsObject = sw2.getProperty("externalDocs");
        if (externalDocsObject != null) {
            sw3.setProperty("externalDocs", externalDocsObject);
        }
        
        return readerWriter.toJson(sw3).replace("#/definitions/", "#/components/schemas/");
    }
    
    private static void setComponentsProperty(JsonMapObject sw2, JsonMapObject sw3,
                                              Map<String, JsonMapObject> requestBodies) {
        JsonMapObject comps = new JsonMapObject();
        JsonMapObject requestBodiesObj = new JsonMapObject(); 
        if (requestBodies != null) {
            for (Map.Entry<String, JsonMapObject> entry : requestBodies.entrySet()) {
                requestBodiesObj.setProperty(entry.getKey(), entry.getValue());
            }
        }
        comps.setProperty("requestBodies", requestBodiesObj);
        
        Object s2Defs = sw2.getProperty("definitions");
        if (s2Defs != null) {
            comps.setProperty("schemas", s2Defs);
        }
        JsonMapObject s2SecurityDefs = sw2.getJsonMapProperty("securityDefinitions");
        if (s2SecurityDefs != null) {
            comps.setProperty("securitySchemes", s2SecurityDefs);

            for (String property : s2SecurityDefs.asMap().keySet()) {
                JsonMapObject securityScheme = s2SecurityDefs.getJsonMapProperty(property);
                if ("basic".equals(securityScheme.getStringProperty("type"))) {
                    securityScheme.setProperty("type", "http");
                    securityScheme.setProperty("scheme", "basic");
                }
            }
        }
        
        sw3.setProperty("components", comps);
    }
    
    private static void setPathsProperty(JsonMapObject sw2, JsonMapObject sw3,
                                         Map<String, JsonMapObject> requestBodies) {
        JsonMapObject sw2Paths = sw2.getJsonMapProperty("paths");
        for (Map.Entry<String, Object> sw2PathEntries : sw2Paths.asMap().entrySet()) {
            JsonMapObject sw2PathVerbs = new JsonMapObject(CastUtils.cast((Map<?, ?>)sw2PathEntries.getValue()));
            for (Map.Entry<String, Object> sw2PathVerbEntries : sw2PathVerbs.asMap().entrySet()) {
                JsonMapObject sw2PathVerbProps =
                    new JsonMapObject(CastUtils.cast((Map<?, ?>)sw2PathVerbEntries.getValue()));
                
                prepareRequestBody(sw2PathVerbProps, requestBodies);
                prepareResponses(sw2PathVerbProps);
                
            }
        }
        
        sw3.setProperty("paths", sw2Paths);
    }
    
    private static void prepareResponses(JsonMapObject sw2PathVerbProps) {
        List<String> sw2PathVerbProduces =
            CastUtils.cast((List<?>)sw2PathVerbProps.removeProperty("produces"));
        
        JsonMapObject sw3PathVerbResps = null;
        JsonMapObject sw2PathVerbResps = sw2PathVerbProps.getJsonMapProperty("responses");
        if (sw2PathVerbResps != null) {
            sw3PathVerbResps = new JsonMapObject();
            
            JsonMapObject okResp = null;
            if (sw2PathVerbResps.containsProperty("200")) {
                okResp = new JsonMapObject(CastUtils.cast((Map<?, ?>)sw2PathVerbResps.removeProperty("200")));
                JsonMapObject newOkResp = new JsonMapObject();
                String description = okResp.getStringProperty("description");
                if (description != null) {
                    newOkResp.setProperty("description", description);
                }
                JsonMapObject schema = okResp.getJsonMapProperty("schema");
                if (schema != null) {
                    JsonMapObject content = prepareContentFromSchema(schema, sw2PathVerbProduces, false);
                    if (content != null) {
                        newOkResp.setProperty("content", content);
                    }
                    
                }
                JsonMapObject headers = okResp.getJsonMapProperty("headers");
                if (headers != null) {
                    newOkResp.setProperty("headers", headers);
                }
                sw3PathVerbResps.setProperty("200", newOkResp);
            }
            for (Map.Entry<String, Object> entry : sw2PathVerbResps.asMap().entrySet()) {
                sw3PathVerbResps.setProperty(entry.getKey(), entry.getValue());
            }
            sw2PathVerbProps.setProperty("responses", sw3PathVerbResps);
        }
    }

    private static void prepareRequestBody(JsonMapObject sw2PathVerbProps, 
                                           Map<String, JsonMapObject> requestBodies) {
        List<String> sw2PathVerbConsumes =
            CastUtils.cast((List<?>)sw2PathVerbProps.removeProperty("consumes"));
        
        JsonMapObject sw3RequestBody = null;
        List<JsonMapObject> sw3formBody = null;
        List<Map<String, Object>> sw2PathVerbParamsList = sw2PathVerbProps.getListMapProperty("parameters");
        if (sw2PathVerbParamsList != null) {
            for (Iterator<Map<String, Object>> it = sw2PathVerbParamsList.iterator(); it.hasNext();) {
                JsonMapObject sw2PathVerbParamMap = new JsonMapObject(it.next());
                if ("body".equals(sw2PathVerbParamMap.getStringProperty("in"))) {
                    it.remove();
                    
                    sw3RequestBody = new JsonMapObject();
                    String description = sw2PathVerbParamMap.getStringProperty("description");
                    if (description != null) {
                        sw3RequestBody.setProperty("description", description);
                    }
                    Boolean required = sw2PathVerbParamMap.getBooleanProperty("required");
                    if (required != null) {
                        sw3RequestBody.setProperty("required", required);
                    }
                    JsonMapObject schema = sw2PathVerbParamMap.getJsonMapProperty("schema");
                    if (schema != null) {
                        JsonMapObject content = prepareContentFromSchema(schema, sw2PathVerbConsumes,
                                                                         requestBodies != null);
                        if (content != null) {
                            sw3RequestBody.setProperty("content", content);
                        }
                        
                    }
                } else if ("formData".equals(sw2PathVerbParamMap.getStringProperty("in"))) {
                    it.remove();
                    if (sw3formBody == null) {
                        sw3formBody = new LinkedList<>();
                        sw3RequestBody = new JsonMapObject();
                    }
                    sw2PathVerbParamMap.removeProperty("in");
                    sw2PathVerbParamMap.removeProperty("required");
                    sw3formBody.add(sw2PathVerbParamMap);
                } else if ("array".equals(sw2PathVerbParamMap.getStringProperty("type"))) {
                    sw2PathVerbParamMap.removeProperty("type");
                    sw2PathVerbParamMap.removeProperty("collectionFormat");
                    sw2PathVerbParamMap.setProperty("explode", true);
                    JsonMapObject items = sw2PathVerbParamMap.getJsonMapProperty("items");
                    sw2PathVerbParamMap.removeProperty("items");
                    JsonMapObject schema = new JsonMapObject();
                    schema.setProperty("type", "array");
                    schema.setProperty("items", items);
                    sw2PathVerbParamMap.setProperty("schema", schema);
                } else {
                    if ("matrix".equals(sw2PathVerbParamMap.getStringProperty("in"))) {
                        sw2PathVerbParamMap.removeProperty("in");
                        sw2PathVerbParamMap.setProperty("in", "path");
                        sw2PathVerbParamMap.setProperty("style", "matrix");
                    }

                    String type = (String)sw2PathVerbParamMap.removeProperty("type");
                    Object enumK = sw2PathVerbParamMap.removeProperty("enum");
                    if (type != null) {
                        JsonMapObject schema = new JsonMapObject();
                        schema.setProperty("type", type);
                        if (enumK != null) {
                            schema.setProperty("enum", enumK);
                        }
                        for (String prop : SIMPLE_TYPE_RELATED_PROPS) {
                            Object value = sw2PathVerbParamMap.removeProperty(prop);
                            if (value != null) {
                                schema.setProperty(prop, value);
                            }
                        }
                        if ("password".equals(sw2PathVerbParamMap.getProperty("name"))) {
                            schema.setProperty("format", "password");
                        }
                        sw2PathVerbParamMap.setProperty("schema", schema);
                    }
                }
            }
        }
        if (sw2PathVerbParamsList.isEmpty()) {
            sw2PathVerbProps.removeProperty("parameters");
        }
        if (sw3formBody != null) {
            sw3RequestBody.setProperty("content", prepareFormContent(sw3formBody, sw2PathVerbConsumes));
        }
        if (sw3RequestBody != null) {
            if (requestBodies == null || sw3formBody != null) {
                sw2PathVerbProps.setProperty("requestBody", sw3RequestBody);
            } else {
                JsonMapObject content = sw3RequestBody.getJsonMapProperty("content");
                if (content != null) {
                    String requestBodyName = (String)content.removeProperty("requestBodyName");
                    if (requestBodyName != null) {
                        requestBodies.put(requestBodyName, sw3RequestBody);
                        String ref = "#components/requestBodies/" + requestBodyName;
                        sw2PathVerbProps.setProperty("requestBody",
                                                     Collections.singletonMap("$ref", ref));
                    }
                }
            }
        }
    }

    private static JsonMapObject prepareFormContent(List<JsonMapObject> formList, List<String> mediaTypes) {
        String mediaType = StringUtils.isEmpty(mediaTypes)
            ? "application/x-www-form-urlencoded" : mediaTypes.get(0); 
        JsonMapObject content = new JsonMapObject();
        JsonMapObject formType = new JsonMapObject();
        JsonMapObject schema = new JsonMapObject();
        schema.setProperty("type", "object");
        JsonMapObject props = new JsonMapObject();
        for (JsonMapObject prop : formList) {
            String name = (String)prop.removeProperty("name");
            props.setProperty(name, prop);
            if ("file".equals(prop.getProperty("type"))) {
                prop.setProperty("type", "string");
                if (!prop.containsProperty("format")) {
                    prop.setProperty("format", "binary");
                }
            }
        }
        schema.setProperty("properties", props);
        formType.setProperty("schema", schema);
        content.setProperty(mediaType, formType);
        return content;
    }

    private static JsonMapObject prepareContentFromSchema(JsonMapObject schema,
                                                          List<String> mediaTypes,
                                                          boolean storeModelName) {
        String type = schema.getStringProperty("type");
        String modelName = null;
        boolean isArray = false;
        if (!"object".equals(type) || !"string".equals(type)) {
            String ref = null;
            JsonMapObject items = null;
            if ("array".equals(type)) {
                isArray = true;
                items = schema.getJsonMapProperty("items");
                ref = (String)items.getProperty("$ref");
            } else {
                ref = schema.getStringProperty("$ref");
            }
            if (ref != null) {
                int index = ref.lastIndexOf('/');
                modelName = ref.substring(index + 1);
                if (items == null) {
                    schema.setProperty("$ref", "#components/schemas/" + modelName);
                } else {
                    items.setProperty("$ref", "#components/schemas/" + modelName);
                }
            }
        }
        JsonMapObject content = new JsonMapObject();
        
        List<String> mediaTypesList = mediaTypes == null 
            ? Collections.singletonList("application/json") : mediaTypes;
        
        for (String mediaType : mediaTypesList) {
            content.setProperty(mediaType, 
                        Collections.singletonMap("schema", schema));
            
        }
        if (modelName != null && storeModelName) {
            content.setProperty("requestBodyName", isArray ? modelName + "Array" : modelName);
        }
        // pass the model name via the content object
        return content;
    }

    private static void setServersProperty(MessageContext ctx, JsonMapObject sw2, JsonMapObject sw3) {
        URI requestURI = ctx == null ? null : URI.create(ctx.getHttpServletRequest().getRequestURL().toString());

        List<String> sw2Schemes = sw2.getListStringProperty("schemes");
        String sw2Scheme;
        if (StringUtils.isEmpty(sw2Schemes)) {
            if (requestURI == null) {
                sw2Scheme = "https";
            } else {
                sw2Scheme = requestURI.getScheme();
            }
        } else {
            sw2Scheme = sw2Schemes.get(0);
        }

        String sw2Host = sw2.getStringProperty("host");
        if (sw2Host == null && requestURI != null) {
            sw2Host = requestURI.getHost() + ":" + requestURI.getPort();
        }

        String sw2BasePath = sw2.getStringProperty("basePath");

        String sw3ServerUrl = sw2Scheme + "://" + sw2Host + sw2BasePath;
        sw3.setProperty("servers", Collections.singletonList(Collections.singletonMap("url", sw3ServerUrl)));
    }

}
