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
package org.apache.cxf.jaxrs.provider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.ext.xml.XMLSource;

@Provider
@Consumes({"text/xml", "application/xml", "application/*+xml" })
public class XPathProvider implements MessageBodyReader<Object> {

    private List<String> consumeMediaTypes;
    private Map<String, String> classExpressions;
    private String globalExpression;
    private String className;
    private Map<String, String> globalNamespaces = 
        Collections.emptyMap();
    
    public void setConsumeMediaTypes(List<String> types) {
        consumeMediaTypes = types;
    }
    
    public List<String> getConsumeMediaTypes() {
        return consumeMediaTypes;    
    }

    public void setExpression(String expr) {
        globalExpression = expr;
    }
    
    public void setClassName(String name) {
        className = name;
    }
    
    public void setExpressions(Map<String, String> expressions) {
        classExpressions = expressions;
    }
    
    public void setNamespaces(Map<String, String> nsMap) {
        globalNamespaces = nsMap;
    }
    
    public boolean isReadable(Class cls, Type genericType, Annotation[] annotations, MediaType mediaType) {
        
        return globalExpression != null && (className == null 
            || className != null && className.equals(cls.getName()))  
            || classExpressions != null && classExpressions.containsKey(cls.getName());
    }

    public Object readFrom(Class<Object> cls, Type type, Annotation[] anns, MediaType mt, 
        MultivaluedMap<String, String> hrs, InputStream is) throws IOException, WebApplicationException {
        String expression = globalExpression != null ? globalExpression
            : classExpressions.get(cls.getName());
        if (expression == null) {
            // must not happen if isReadable() returned true
            throw new WebApplicationException(500);
        }
        XMLSource source = new XMLSource(is);
        return source.getNode(expression, globalNamespaces, cls);
    }

}
