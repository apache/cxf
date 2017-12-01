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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
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
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

public final class SwaggerOpenApiUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(SwaggerOpenApiUtils.class);
    private SwaggerOpenApiUtils() {
        
    }
    
    public static String getOpenApiFromSwaggerLoc(String loc) {
        return getOpenApiFromSwaggerLoc(loc, BusFactory.getThreadDefaultBus());
    }
    public static String getOpenApiFromSwaggerLoc(String loc, Bus bus) {
        try {
            InputStream is = ResourceUtils.getResourceStream(loc, bus);
            if (is == null) {
                return null;
            }
            return getOpenApiFromSwaggerStream(is);
        } catch (Exception ex) {
            LOG.warning("Problem with processing a user model at " + loc);
        }
        return null;
    }
    
    public static String getOpenApiFromSwaggerStream(InputStream is) throws IOException {
        return getOpenApiFromSwaggerJson(IOUtils.readStringFromStream(is));
    }
    
    public static String getOpenApiFromSwaggerJson(String json) throws IOException {
        JsonMapObjectReaderWriter readerWriter = new JsonMapObjectReaderWriter();
        JsonMapObject sw2 = readerWriter.fromJsonToJsonObject(json);
        JsonMapObject sw3 = new JsonMapObject();
        
        // "openapi"
        sw3.setProperty("openapi", "3.0.0");
        
        // "servers"
        setServersProperty(sw2, sw3);
        
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
        setPathsProperty(sw2, sw3);
        
        // components
        setComponentsProperty(sw2, sw3);
        
        // externalDocs
        Object externalDocsObject = sw2.getProperty("externalDocs");
        if (externalDocsObject != null) {
            sw3.setProperty("externalDocs", externalDocsObject);
        }
        
        return readerWriter.toJson(sw3);
    }
    
    private static void setComponentsProperty(JsonMapObject sw2, JsonMapObject sw3) {
        JsonMapObject comps = new JsonMapObject();
        comps.setProperty("requestBodies", Collections.emptyMap());
        Object s2Defs = sw2.getProperty("definitions");
        if (s2Defs != null) {
            comps.setProperty("schemas", s2Defs);
        }
        Object s2SecurityDefs = sw2.getProperty("securityDefinitions");
        if (s2SecurityDefs != null) {
            comps.setProperty("securitySchemes", s2SecurityDefs);
        }
        
        sw3.setProperty("components", comps);
        
    }
    private static void setPathsProperty(JsonMapObject sw2, JsonMapObject sw3) {
        JsonMapObject sw2Paths = sw2.getJsonMapProperty("paths");
        for (Map.Entry<String, Object> sw2PathEntries : sw2Paths.asMap().entrySet()) {
            JsonMapObject sw2PathVerbs = new JsonMapObject(CastUtils.cast((Map<?, ?>)sw2PathEntries.getValue()));
            for (Map.Entry<String, Object> sw2PathVerbEntries : sw2PathVerbs.asMap().entrySet()) {
                JsonMapObject sw2PathVerbProps =
                    new JsonMapObject(CastUtils.cast((Map<?, ?>)sw2PathVerbEntries.getValue()));
                
                List<String> sw2PathVerbConsumes =
                    CastUtils.cast((List<?>)sw2PathVerbProps.removeProperty("consumes"));
                List<String> sw2PathVerbProduces =
                    CastUtils.cast((List<?>)sw2PathVerbProps.removeProperty("produces"));
                
                JsonMapObject sw3RequestBody = null;
                List<JsonMapObject> sw3formBody = null;
                List<Map<String, Object>> sw2PathVerbParamsList =
                    sw2PathVerbProps.getListMapProperty("parameters");
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
                                JsonMapObject content = prepareContentFromSchema(schema, sw2PathVerbConsumes);
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
                            String type = (String)sw2PathVerbParamMap.removeProperty("type");
                            String format = (String)sw2PathVerbParamMap.removeProperty("format");
                            JsonMapObject schema = new JsonMapObject();
                            schema.setProperty("type", type);
                            schema.setProperty("format", format);
                            sw2PathVerbParamMap.setProperty("schema", schema);
                        }
                    }
                }
                if (sw2PathVerbParamsList.isEmpty()) {
                    sw2PathVerbProps.removeProperty("parameters");
                }
                if (sw3formBody != null) {
                    sw3RequestBody.setProperty("content", prepareFormContent(sw3formBody));
                }
                if (sw3RequestBody != null) {
                    // Inline for now, or the map of requestBodies can be created instead 
                    // and added to the /components
                    sw2PathVerbProps.setProperty("requestBody", sw3RequestBody);
                }
                
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
                            JsonMapObject content = prepareContentFromSchema(schema, sw2PathVerbProduces);
                            if (content != null) {
                                newOkResp.setProperty("content", content);
                            }
                            
                        }
                        sw3PathVerbResps.setProperty("200", newOkResp);
                    }
                    for (Map.Entry<String, Object> entry : sw2PathVerbResps.asMap().entrySet()) {
                        sw3PathVerbResps.setProperty(entry.getKey(), entry.getValue());
                    }
                    sw2PathVerbProps.setProperty("responses", sw3PathVerbResps);
                }
            }
        }
        
        sw3.setProperty("paths", sw2Paths);
        
        
    }
    
    private static JsonMapObject prepareFormContent(List<JsonMapObject> formList) {
        JsonMapObject content = new JsonMapObject();
        JsonMapObject formType = new JsonMapObject();
        JsonMapObject schema = new JsonMapObject();
        schema.setProperty("type", "object");
        JsonMapObject props = new JsonMapObject();
        for (JsonMapObject prop : formList) {
            String name = (String)prop.removeProperty("name");
            props.setProperty(name, prop);
        }
        schema.setProperty("properties", props);
        formType.setProperty("schema", schema);
        content.setProperty("application/x-www-form-urlencoded", formType);
        return content;
    }
    private static JsonMapObject prepareContentFromSchema(JsonMapObject schema,
                                                          List<String> mediaTypes) {
        JsonMapObject content = null;
        String type = schema.getStringProperty("type");
        String ref = null;
        JsonMapObject items = null;
        if ("array".equals(type)) {
            items = schema.getJsonMapProperty("items");
            ref = (String)items.getProperty("$ref");
        } else {
            ref = schema.getStringProperty("$ref");
        }
        if (ref != null) {
            content = new JsonMapObject();
            
            int index = ref.lastIndexOf("/");
            String modelName = ref.substring(index + 1);
            if (items == null) {
                schema.setProperty("$ref", "#components/schemas/" + modelName);
            } else {
                items.setProperty("$ref", "#components/schemas/" + modelName);
            }
            
            List<String> mediaTypesList = mediaTypes == null 
                ? Collections.singletonList("*/*") : mediaTypes;
            
            for (String mediaType : mediaTypesList) {
                content.setProperty(mediaType, 
                            Collections.singletonMap("schema", schema));
                
            }
        }
        return content;
    }

    private static void setServersProperty(JsonMapObject sw2, JsonMapObject sw3) {
        String sw2Host = sw2.getStringProperty("host");
        String sw2BasePath = sw2.getStringProperty("basePath");
        String sw2Scheme = null;
        List<String> sw2Schemes = sw2.getListStringProperty("schemes");
        if (StringUtils.isEmpty(sw2Schemes)) {
            sw2Scheme = "https";
        } else {
            sw2Scheme = sw2Schemes.get(0);
        }
        String sw3ServerUrl = sw2Scheme + "://" + sw2Host + sw2BasePath;
        sw3.setProperty("servers", 
            Collections.singletonList(
                Collections.singletonMap("url", sw3ServerUrl)));
        
    }

    
}
