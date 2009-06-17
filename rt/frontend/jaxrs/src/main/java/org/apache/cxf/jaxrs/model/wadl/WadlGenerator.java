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
package org.apache.cxf.jaxrs.model.wadl;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.message.Message;

// TODO :
// 1. extract JavaDocs and put them into XML comments
// 2. if _type = html -> convert the XML buil here using MH's stylesheet
// 3. generate grammars 

public class WadlGenerator implements RequestHandler {

    public static final String WADL_QUERY = "_wadl"; 
    public static final MediaType WADL_TYPE = MediaType.valueOf("application/vnd.sun.wadl+xml"); 
    public static final String WADL_NS = "http://research.sun.com/wadl/2006/10";    
    
    public Response handleRequest(Message m, ClassResourceInfo resource) {
        
        if (!"GET".equals(m.get(Message.HTTP_REQUEST_METHOD))) {
            return null;
        }
        
        UriInfo ui = new UriInfoImpl(m);
        if (!ui.getQueryParameters().containsKey(WADL_QUERY)) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<application xmlns=\"").append(WADL_NS).append("\">");
        
        sb.append("<resources base=\"").append(ui.getBaseUri().toString()).append("\">");
        handleResource(sb, resource, resource.getURITemplate().getValue(),
                       resource.getURITemplate().getVariables());
        sb.append("</resources>");
        sb.append("</application>");
        
        return Response.ok().type(WADL_TYPE).entity(sb.toString()).build();
    }

    private void handleResource(StringBuilder sb, ClassResourceInfo cri, String path,
                                List<String> templateVars) {
        sb.append("<resource path=\"").append(path).append("\">");
        handleTemplateParams(sb, templateVars);
        
        List<OperationResourceInfo> sortedOps = sortOperationsByPath(
            cri.getMethodDispatcher().getOperationResourceInfos());
        List<OperationResourceInfo> opsWithSamePath = new LinkedList<OperationResourceInfo>();
        for (int i = 0; i < sortedOps.size(); i++) {
            
            if (sortedOps.get(i).getHttpMethod() == null) {
                Class<?> cls = sortedOps.get(i).getMethodToInvoke().getReturnType();
                ClassResourceInfo subcri = cri.findResource(cls, cls);
                if (subcri != null) {
                    handleResource(sb, subcri, sortedOps.get(i).getURITemplate().getValue(), 
                                   sortedOps.get(i).getURITemplate().getVariables());
                    opsWithSamePath.clear();
                    continue;
                } else {
                    handleDynamicSubresource(sb, sortedOps.get(i));
                }
            }
            if (opsWithSamePath.size() == 0) {
                opsWithSamePath.add(sortedOps.get(i));
            } else if (i > 0 && sortedOps.get(i - 1).getURITemplate().getValue()
                .equals(sortedOps.get(i).getURITemplate().getValue())) {
                opsWithSamePath.add(sortedOps.get(i));
            } else {
                handleOperation(sb, opsWithSamePath);
                opsWithSamePath.clear();
                opsWithSamePath.add(sortedOps.get(i));
            }
        }
        handleOperation(sb, opsWithSamePath);
        sb.append("</resource>");
    }
    
    private void handleOperation(StringBuilder sb, List<OperationResourceInfo> oris) {
        if (oris.size() == 0) {
            return;
        }
        String path = oris.get(0).getURITemplate().getValue();
        boolean isSlash =  "/".equals(path);
        if (!isSlash) {
            sb.append("<resource path=\"").append(path).append("\">");
        }
        for (OperationResourceInfo ori : oris) {
            handleTemplateParams(sb, ori.getURITemplate().getVariables());
            handleMatrixParams(sb, ori);
        }
        for (OperationResourceInfo ori : oris) {
            sb.append("<method name=\"").append(ori.getHttpMethod()).append("\">");
            if (ori.getMethodToInvoke().getParameterTypes().length != 0) {
                sb.append("<request>");
                for (Parameter p : ori.getParameters()) {        
                    handleParameter(sb, ori, p);             
                }
                sb.append("</request>");
            }
            if (Void.class != ori.getMethodToInvoke().getReturnType()) {
                sb.append("<response>");
                handleRepresentation(sb, ori);
                sb.append("</response>");
            }
            sb.append("</method>");
        }
        if (!isSlash) {
            sb.append("</resource>");
        }
    }
    
    private void handleDynamicSubresource(StringBuilder sb, OperationResourceInfo ori) {
        
        sb.append("<!-- Dynamic subresource -->");
        sb.append("<resource path=\"").append(ori.getURITemplate().getValue()).append("\">");
        if (ori.getMethodToInvoke().getParameterTypes().length != 0) {
            sb.append("<request>");
            for (Parameter p : ori.getParameters()) {        
                handleParameter(sb, ori, p);             
            }
            sb.append("</request>");
        }
        sb.append("</resource>");
    }
    
    
    private void handleParameter(StringBuilder sb, OperationResourceInfo ori, Parameter pm) {
        if (pm.getType() == ParameterType.REQUEST_BODY) {
            handleRepresentation(sb, ori);
            return;
        }
        if (pm.getType() == ParameterType.PATH || pm.getType() == ParameterType.MATRIX) {
            return;
        }
        if (pm.getType() == ParameterType.HEADER || pm.getType() == ParameterType.QUERY) {
            writeParam(sb, pm.getName(), pm.getType().toString().toLowerCase(), pm.getDefaultValue());
        }
        
    }
    
    private void handleMatrixParams(StringBuilder sb, OperationResourceInfo ori) {
        for (Parameter pm : ori.getParameters()) {
            if (pm.getType() == ParameterType.MATRIX) {
                writeParam(sb, pm.getName(), "matrix", pm.getDefaultValue());
            }
        }
    }
    
    private void writeParam(StringBuilder sb, String name, String style, String dValue) {
        sb.append("<param name=\"").append(name).append("\" ");
        sb.append("style=\"").append(style).append("\"");
        if (dValue != null) {
            sb.append(" default=\"").append(dValue).append("\"");
        }
        sb.append("/>");
    }
    
    private void handleTemplateParams(StringBuilder sb, List<String> vars) {
        for (String var : vars) {
            writeParam(sb, var, "template", null);
        }
    }
    
    private void handleRepresentation(StringBuilder sb, OperationResourceInfo ori) {
        sb.append("<representation>");
        sb.append("</representation>");
    }
    
    private List<OperationResourceInfo> sortOperationsByPath(Set<OperationResourceInfo> ops) {
        List<OperationResourceInfo> opsWithSamePath = new LinkedList<OperationResourceInfo>(ops);
        Collections.sort(opsWithSamePath, new Comparator<OperationResourceInfo>() {

            public int compare(OperationResourceInfo op1, OperationResourceInfo op2) {
                String path1 = op1.getURITemplate().getValue();
                String path2 = op2.getURITemplate().getValue();
                return path1.compareTo(path2);
            } 
        
        });
        return opsWithSamePath;
    }
}
