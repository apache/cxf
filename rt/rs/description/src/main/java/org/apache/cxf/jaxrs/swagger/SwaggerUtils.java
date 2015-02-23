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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.provider.json.JsonMapObjectReaderWriter;
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
    public static UserResource getUserResource(String loc) {
        return getUserResource(loc, BusFactory.getThreadDefaultBus());
    }
    public static UserResource getUserResource(String loc, Bus bus) {
        try {
            InputStream is = ResourceUtils.getResourceStream(loc, bus);
            if (is == null) {
                return null;
            }
            return getUserResourceFromJson(IOUtils.readStringFromStream(is));
        } catch (Exception ex) {
            LOG.warning("Problem with processing a user model at " + loc);
        }
        return null;
    }
    public static List<UserResource> getUserResourcesFromResourceObjects(List<String> jsonObjects) {
        List<UserResource> resources = new ArrayList<UserResource>();
        for (String json : jsonObjects) {
            resources.add(getUserResourceFromJson(json));
        }
        return resources;
    }
    public static UserResource getUserResourceFromJson(String json) {
        JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
        Map<String, Object> map = reader.fromJson(json);
        
        if (map.containsKey("swaggerVersion")) {
            return getUserResourceFromSwagger12(map);
        } else {
            return getUserResourceFromSwagger20(map);
        }
        
    }
    private static UserResource getUserResourceFromSwagger20(Map<String, Object> map) {
        UserResource ur = new UserResource();
        String relativePath = (String)map.get("basePath");
        ur.setPath(relativePath == null ? "/" : relativePath);
        
        List<String> resourceProduces = CastUtils.cast((List<?>)map.get("produces"));
        ur.setProduces(listToString(resourceProduces));
        
        List<String> resourceConsumes = CastUtils.cast((List<?>)map.get("consumes"));
        ur.setConsumes(listToString(resourceConsumes));
        
        List<UserOperation> userOps = new LinkedList<UserOperation>();
        Map<String, Map<String, Object>> paths = CastUtils.cast((Map<?, ?>)map.get("paths"));
        for (Map.Entry<String, Map<String, Object>> pathEntry : paths.entrySet()) {
            
            String operPath = pathEntry.getKey();
            
            Map<String, Object> operations = pathEntry.getValue();
            for (Map.Entry<String, Object> operEntry : operations.entrySet()) {
                UserOperation userOp = new UserOperation();
                userOp.setVerb(operEntry.getKey().toUpperCase());
                userOp.setPath(operPath);
                
                Map<String, Object> oper = CastUtils.cast((Map<?, ?>)operEntry.getValue());
                
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
                userOps.add(userOp);
            }
        }
        ur.setOperations(userOps);
        return ur;
    }
    private static UserResource getUserResourceFromSwagger12(Map<String, Object> map) {
        UserResource ur = new UserResource();
        String relativePath = (String)map.get("resourcePath");
        ur.setPath(relativePath == null ? "/" : relativePath);
        
        List<String> resourceProduces = CastUtils.cast((List<?>)map.get("produces"));
        ur.setProduces(listToString(resourceProduces));
        
        List<String> resourceConsumes = CastUtils.cast((List<?>)map.get("consumes"));
        ur.setConsumes(listToString(resourceConsumes));
        
        List<UserOperation> userOps = new LinkedList<UserOperation>();
        List<Map<String, Object>> apis = CastUtils.cast((List<?>)map.get("apis"));
        for (Map<String, Object> api : apis) {
            String operPath = (String)api.get("path");
            if (relativePath != null && operPath.startsWith(relativePath) 
                && operPath.length() > relativePath.length()) {
                // relative resource and operation paths overlap in Swagger 1.2
                operPath = operPath.substring(relativePath.length());
            }
            
            List<Map<String, Object>> operations = CastUtils.cast((List<?>)api.get("operations"));
            for (Map<String, Object> oper : operations) {
                UserOperation userOp = new UserOperation();
                userOp.setPath(operPath);
                userOp.setName((String)oper.get("nickname"));
                userOp.setVerb((String)oper.get("method"));
                
                List<String> opProduces = CastUtils.cast((List<?>)oper.get("produces"));
                userOp.setProduces(listToString(opProduces));
                
                List<String> opConsumes = CastUtils.cast((List<?>)oper.get("consumes"));
                userOp.setConsumes(listToString(opConsumes));
                
                List<Parameter> userOpParams = new LinkedList<Parameter>(); 
                List<Map<String, Object>> params = CastUtils.cast((List<?>)oper.get("parameters"));
                for (Map<String, Object> param : params) {
                    String name = (String)param.get("name");
                    //"path", "query", "body", "header", "form"
                    String paramType = (String)param.get("paramType");
                    ParameterType pType = "body".equals(paramType) 
                        ? ParameterType.REQUEST_BODY : ParameterType.valueOf(paramType.toUpperCase()); 
                    Parameter userParam = new Parameter(pType, name);
                    setJavaType(userParam, (String)param.get("type"));
                    
                    userOpParams.add(userParam);
                }   
                if (!userOpParams.isEmpty()) {
                    userOp.setParameters(userOpParams);
                }
                userOps.add(userOp);
            }
        }
        ur.setOperations(userOps);
        return ur;
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
