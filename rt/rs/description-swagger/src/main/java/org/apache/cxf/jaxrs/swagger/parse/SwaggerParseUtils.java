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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserApplication;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

public final class SwaggerParseUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(ResourceUtils.class);
    private static final Map<String, Class<?>> SWAGGER_TYPE_MAP;
    static {
        SWAGGER_TYPE_MAP = new HashMap<>();
        SWAGGER_TYPE_MAP.put("string", String.class);
        SWAGGER_TYPE_MAP.put("integer", long.class);
        SWAGGER_TYPE_MAP.put("float", float.class);
        SWAGGER_TYPE_MAP.put("double", double.class);
        SWAGGER_TYPE_MAP.put("int", int.class);
        SWAGGER_TYPE_MAP.put("long", long.class);
        SWAGGER_TYPE_MAP.put("byte", byte.class);
        SWAGGER_TYPE_MAP.put("boolean", boolean.class);
        SWAGGER_TYPE_MAP.put("date", java.util.Date.class);
        SWAGGER_TYPE_MAP.put("dateTime", java.util.Date.class);
        SWAGGER_TYPE_MAP.put("File", java.io.InputStream.class);
        SWAGGER_TYPE_MAP.put("file", java.io.InputStream.class);
    }
    private SwaggerParseUtils() {

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
    public static UserApplication getUserApplicationFromJson(String json,
                                                             ParseConfiguration cfg) {
        JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
        Map<String, Object> map = reader.fromJson(json);

        UserApplication app = new UserApplication();
        String relativePath = (String)map.get("basePath");
        app.setBasePath(StringUtils.isEmpty(relativePath) ? "/" : relativePath);


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
                // The operation could be null, the particular HTTP verb may not contain any
                // operations but Swagger may still include it.
                if (oper != null) {
                    userOp.setPath(operPath);

                    userOp.setName((String) oper.get("operationId"));
                    List<String> opProduces = CastUtils.cast((List<?>) oper.get("produces"));
                    userOp.setProduces(listToString(opProduces));

                    List<String> opConsumes = CastUtils.cast((List<?>) oper.get("consumes"));
                    userOp.setConsumes(listToString(opConsumes));

                    List<Parameter> userOpParams = new LinkedList<>();
                    List<Map<String, Object>> params = CastUtils.cast((List<?>) oper.get("parameters"));
                    for (Map<String, Object> param : params) {
                        String name = (String) param.get("name");
                        //"query", "header", "path", "formData" or "body"
                        String paramType = (String) param.get("in");
                        ParameterType pType = "body".equals(paramType) ? ParameterType.REQUEST_BODY
                                : "formData".equals(paramType)
                                ? ParameterType.FORM : ParameterType.valueOf(paramType.toUpperCase());
                        Parameter userParam = new Parameter(pType, name);

                        setJavaType(userParam, (String) param.get("type"));
                        userOpParams.add(userParam);
                    }
                    if (!userOpParams.isEmpty()) {
                        userOp.setParameters(userOpParams);
                    }
                    List<String> opTags = CastUtils.cast((List<?>) oper.get("tags"));
                    if (opTags == null) {
                        opTags = Collections.singletonList("");
                    }
                    for (String opTag : opTags) {
                        userOpsMap.get(opTag).add(userOp);
                    }
                }
            }
        }

        List<UserResource> resources = new LinkedList<>();

        for (Map.Entry<String, List<UserOperation>> entry : userOpsMap.entrySet()) {
            UserResource ur = new UserResource();
            ur.setPath("/");
            ur.setOperations(entry.getValue());
            ur.setName(entry.getKey());
            resources.add(ur);
        }

        app.setResources(resources);
        return app;
    }

    private static void setJavaType(Parameter userParam, String typeName) {
        Class<?> javaType = SWAGGER_TYPE_MAP.get(typeName);
        if (javaType == null) {
            try {
                // May work if the model has already been compiled
                // TODO: need to know the package name
                javaType = ClassLoaderUtils.loadClass(typeName, SwaggerParseUtils.class); 
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
