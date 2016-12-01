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

import java.io.IOException;
import java.io.InputStream;
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

public final class SwaggerUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(ResourceUtils.class);
    private static final Map<String, String> SWAGGER_TYPE_MAP; 
    static {
        SWAGGER_TYPE_MAP = new HashMap<String, String>();
        SWAGGER_TYPE_MAP.put("string", "String");
        SWAGGER_TYPE_MAP.put("integer", "long");
        SWAGGER_TYPE_MAP.put("float", "float");
        SWAGGER_TYPE_MAP.put("double", "double");
        SWAGGER_TYPE_MAP.put("int", "int");
        SWAGGER_TYPE_MAP.put("long", "long");
        SWAGGER_TYPE_MAP.put("byte", "byte");
        SWAGGER_TYPE_MAP.put("boolean", "boolean");
        SWAGGER_TYPE_MAP.put("date", "java.util.Date");
        SWAGGER_TYPE_MAP.put("dateTime", "java.util.Date");
        SWAGGER_TYPE_MAP.put("File", "java.io.InputStream");
        SWAGGER_TYPE_MAP.put("file", "java.io.InputStream");
    }
    private SwaggerUtils() {
        
    }
    public static UserApplication getUserApplication(String loc) {
        return getUserApplication(loc, BusFactory.getThreadDefaultBus());
    }
    public static UserApplication getUserApplication(String loc, Bus bus) {
        try {
            InputStream is = ResourceUtils.getResourceStream(loc, bus);
            if (is == null) {
                return null;
            }
            return getUserApplicationFromStream(is);
        } catch (Exception ex) {
            LOG.warning("Problem with processing a user model at " + loc);
        }
        return null;
    }
    public static UserApplication getUserApplicationFromStream(InputStream is) throws IOException {
        return getUserApplicationFromJson(IOUtils.readStringFromStream(is));
    }
    public static UserApplication getUserApplicationFromJson(String json) {
        JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
        Map<String, Object> map = reader.fromJson(json);
    
        UserApplication app = new UserApplication();
        String relativePath = (String)map.get("basePath");
        app.setBasePath(StringUtils.isEmpty(relativePath) ? "/" : relativePath);
        
        
        Map<String, List<UserOperation>> userOpsMap = new LinkedHashMap<String, List<UserOperation>>();
        Set<String> tags = new HashSet<String>();
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
                List<String> opTags = CastUtils.cast((List<?>)oper.get("tags"));
                String opTag = opTags == null ? "" : opTags.get(0);
                
                String realOpPath = operPath.equals("/" + opTag) ? "/" : operPath.substring(opTag.length() + 1);
                userOp.setPath(realOpPath);
                
                userOp.setName((String)oper.get("operationId"));
                List<String> opProduces = CastUtils.cast((List<?>)oper.get("produces"));
                userOp.setProduces(listToString(opProduces));
                
                List<String> opConsumes = CastUtils.cast((List<?>)oper.get("consumes"));
                userOp.setConsumes(listToString(opConsumes));
                
                List<Parameter> userOpParams = new LinkedList<Parameter>(); 
                List<Map<String, Object>> params = CastUtils.cast((List<?>)oper.get("parameters"));
                for (Map<String, Object> param : params) {
                    String name = (String)param.get("name");
                    //"query", "header", "path", "formData" or "body"
                    String paramType = (String)param.get("in");
                    ParameterType pType = "body".equals(paramType) ? ParameterType.REQUEST_BODY 
                        : "formData".equals(paramType) 
                        ? ParameterType.FORM : ParameterType.valueOf(paramType.toUpperCase()); 
                    Parameter userParam = new Parameter(pType, name);
                    
                    setJavaType(userParam, (String)param.get("type"));
                    userOpParams.add(userParam);
                }   
                if (!userOpParams.isEmpty()) {
                    userOp.setParameters(userOpParams);
                }
                userOpsMap.get(opTag).add(userOp);    
                
            }
        }
        
        List<UserResource> resources = new LinkedList<UserResource>();
        
        for (Map.Entry<String, List<UserOperation>> entry : userOpsMap.entrySet()) {
            UserResource ur = new UserResource();
            ur.setPath("/" + entry.getKey());
            ur.setOperations(entry.getValue());
            ur.setName(entry.getKey());
            resources.add(ur);
        }
        
        app.setResources(resources);
        return app;
    }
    
    private static void setJavaType(Parameter userParam, String typeName) {
        String javaTypeName = SWAGGER_TYPE_MAP.get(typeName);
        if (javaTypeName != null) {
            try {
                userParam.setJavaType(ClassLoaderUtils.loadClass(javaTypeName, SwaggerUtils.class));
            } catch (Throwable t) {
                // ignore - can be a reference to a JSON model class, etc
            }
        }
        
    }
    private static String listToString(List<String> list) {
        if (list != null) {
            StringBuilder sb = new StringBuilder();
            for (String s : list) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(s);
            }
            return sb.toString();
        } else {
            return null;
        }
    }
}
