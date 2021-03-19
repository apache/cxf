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
package org.apache.cxf.jaxrs.openapi.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserApplication;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

public final class OpenApiParseUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(ResourceUtils.class);
    private static final Map<String, Class<?>> OPENAPI_TYPE_MAP;
    static {
        OPENAPI_TYPE_MAP = new HashMap<>();
        OPENAPI_TYPE_MAP.put("string", String.class);
        OPENAPI_TYPE_MAP.put("integer", int.class);
        OPENAPI_TYPE_MAP.put("float", float.class);
        OPENAPI_TYPE_MAP.put("double", double.class);
        OPENAPI_TYPE_MAP.put("int", int.class);
        OPENAPI_TYPE_MAP.put("long", long.class);
        OPENAPI_TYPE_MAP.put("byte", byte.class);
        OPENAPI_TYPE_MAP.put("boolean", boolean.class);
        OPENAPI_TYPE_MAP.put("date", java.util.Date.class);
        OPENAPI_TYPE_MAP.put("dateTime", java.util.Date.class);
        OPENAPI_TYPE_MAP.put("password", String.class);
        OPENAPI_TYPE_MAP.put("binary", java.io.InputStream.class);
    }
    private OpenApiParseUtils() {

    }
    public static UserApplication getUserApplication(String loc) {
        return getUserApplication(loc, BusFactory.getThreadDefaultBus());
    }
    public static UserApplication getUserApplication(String loc, Bus bus) {
        return getUserApplication(loc, bus, new ParseConfiguration());
    }    
    public static UserApplication getUserApplication(String loc, Bus bus, ParseConfiguration cfg) {    
        try {
            InputStream is = ResourceUtils.getResourceStream(loc, bus);
            if (is == null) {
                return null;
            }
            return getUserApplicationFromStream(is, cfg);
        } catch (Exception ex) {
            LOG.warning("Problem with processing a user model at " + loc);
        }
        return null;
    }
    public static UserApplication getUserApplicationFromStream(InputStream is) throws IOException {
        return getUserApplicationFromStream(is, new ParseConfiguration());
    }
    public static UserApplication getUserApplicationFromStream(InputStream is,
                                                               ParseConfiguration cfg) throws IOException {
        return getUserApplicationFromJson(IOUtils.readStringFromStream(is), cfg);
    }
    public static UserApplication getUserApplicationFromJson(String json) {
        return getUserApplicationFromJson(json, new ParseConfiguration());
    }
    public static UserApplication getUserApplicationFromJson(String json, ParseConfiguration cfg) {
        JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
        Map<String, Object> map = reader.fromJson(json);

        UserApplication app = new UserApplication();
        app.setBasePath("/");
        
        List<Map<String, Object>> servers = CastUtils.cast((List<?>)map.get("servers"));
        if (servers != null && !servers.isEmpty()) {
            final String url = (String)servers.get(0).get("url");
            if (url != null) {
                app.setBasePath(url);
            }
        }

        Map<String, List<UserOperation>> userOpsMap = new LinkedHashMap<>();
        Set<String> tags = new HashSet<>();
        List<Map<String, Object>> tagsProp = CastUtils.cast((List<?>)map.get("tags"));
        if (tagsProp != null) {
            for (Map<String, Object> tagProp : tagsProp) {
                tags.add((String)tagProp.get("name"));
            }
        } else {
            tags.add("");
        }
        
        for (String tag : tags) {
            userOpsMap.put(tag, new LinkedList<UserOperation>());
        }


        Map<String, Map<String, Object>> paths = CastUtils.cast((Map<?, ?>)map.get("paths"));
        for (Map.Entry<String, Map<String, Object>> pathEntry : paths.entrySet()) {
            String operPath = pathEntry.getKey();

            Map<String, Object> operations = pathEntry.getValue();
            for (Map.Entry<String, Object> operEntry : operations.entrySet()) {

                UserOperation userOp = new UserOperation();
                userOp.setVerb(operEntry.getKey().toUpperCase());

                Map<String, Object> oper = CastUtils.cast((Map<?, ?>)operEntry.getValue());
                
                userOp.setPath(operPath);

                userOp.setName((String)oper.get("operationId"));
                Map<String, Object> responses = CastUtils.cast((Map<?, ?>)oper.get("responses"));
                if (responses != null) {
                    userOp.setProduces(listToString(
                        responses
                            .entrySet()
                            .stream()
                            .map(entry -> CastUtils.cast((Map<?, ?>)entry.getValue()))
                            .map(value -> CastUtils.cast((Map<?, ?>)value.get("content")))
                            .filter(Objects::nonNull)
                            .flatMap(content -> content.keySet().stream().map(type -> (String)type))
                            .collect(Collectors.toList())
                    ));
                }

                Map<String, Object> payloads = CastUtils.cast((Map<?, ?>)oper.get("requestBody"));
                if (payloads != null) {
                    userOp.setConsumes(listToString(
                        payloads
                            .entrySet()
                            .stream()
                            .map(entry -> CastUtils.cast((Map<?, ?>)entry.getValue()))
                            .map(value -> CastUtils.cast((Map<?, ?>)value.get("content")))
                            .filter(Objects::nonNull)
                            .flatMap(content -> content.keySet().stream().map(type -> (String)type))
                            .collect(Collectors.toList())
                    ));
                }

                List<Parameter> userOpParams = new LinkedList<>();
                List<Map<String, Object>> params = CastUtils.cast((List<?>)oper.get("parameters"));
                if (params != null) {
                    for (Map<String, Object> param : params) {
                        String name = (String)param.get("name");
                        //"query", "header", "path" or "cookie".
                        String paramType = (String)param.get("in");
                        final ParameterType pType;
                        
                        if ("query".equals(paramType)) {
                            pType = ParameterType.QUERY;
                        } else if ("header".equals(paramType)) {
                            pType = ParameterType.HEADER;
                        } else if ("path".equals(paramType)) {
                            pType = ParameterType.PATH;
                        } else if ("cookie".equals(paramType)) {
                            pType = ParameterType.COOKIE;
                        } else {
                            pType = ParameterType.REQUEST_BODY;
                        }
                        
                        Parameter userParam = new Parameter(pType, name);
                        setJavaType(userParam, (String)param.get("type"));
                        userOpParams.add(userParam);
                    }
                }
                if (!userOpParams.isEmpty()) {
                    userOp.setParameters(userOpParams);
                }
                List<String> opTags = CastUtils.cast((List<?>)oper.get("tags"));
                if (opTags == null) {
                    opTags = Collections.singletonList("");
                }
                for (String opTag : opTags) {
                    userOpsMap.putIfAbsent(opTag, new LinkedList<UserOperation>());
                    userOpsMap.get(opTag).add(userOp);
                }

            }
        }

        List<UserResource> resources = new LinkedList<>();

        for (Map.Entry<String, List<UserOperation>> entry : userOpsMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                UserResource ur = new UserResource();
                ur.setPath("/");
                ur.setOperations(entry.getValue());
                ur.setName(entry.getKey());
                resources.add(ur);
            }
        }

        app.setResources(resources);
        return app;
    }

    private static void setJavaType(Parameter userParam, String typeName) {
        Class<?> javaType = OPENAPI_TYPE_MAP.get(typeName);
        if (javaType == null) {
            try {
                // May work if the model has already been compiled
                // TODO: need to know the package name
                javaType = ClassLoaderUtils.loadClass(typeName, OpenApiParseUtils.class); 
            } catch (Throwable t) {
                // ignore
            }
        }

        userParam.setJavaType(javaType);
    }
    
    private static String listToString(List<String> list) {
        if (list != null) {
            return String.join(",", list);
        }
        return null;
    }
}
