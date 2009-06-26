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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfoComparator;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.StreamWriterContentHandler;

// TODO :
// 1. extract JavaDocs and put them into XML comments
// 2. if _type = html -> convert the XML buil here using MH's stylesheet
// 3. generate grammars 

public class WadlGenerator implements RequestHandler {

    public static final String WADL_QUERY = "_wadl"; 
    public static final MediaType WADL_TYPE = MediaType.valueOf("application/vnd.sun.wadl+xml"); 
    public static final String WADL_NS = "http://research.sun.com/wadl/2006/10";    
    
    private static final Logger LOG = LogUtils.getL7dLogger(WadlGenerator.class);
    
    public Response handleRequest(Message m, ClassResourceInfo resource) {
        
        if (!"GET".equals(m.get(Message.HTTP_REQUEST_METHOD))) {
            return null;
        }

        UriInfo ui = new UriInfoImpl(m);
        if (!ui.getQueryParameters().containsKey(WADL_QUERY)) {
            return null;
        }
        
        StringBuilder sbMain = new StringBuilder();
        sbMain.append("<application xmlns=\"").append(WADL_NS).append("\">");
        StringBuilder sbGrammars = new StringBuilder();
        sbGrammars.append("<grammars>");
        StringBuilder sbResources = new StringBuilder();
        sbResources.append("<resources base=\"").append(ui.getBaseUri().toString()).append("\">");
        handleResource(sbResources, resource, resource.getURITemplate().getValue(),
                       resource.getURITemplate().getVariables());
        sbResources.append("</resources>");
        sbGrammars.append("</grammars>");
        sbMain.append(sbGrammars.toString());
        sbMain.append(sbResources.toString());
        sbMain.append("</application>");
        
        HttpHeaders headers = new HttpHeadersImpl(m);
        MediaType type = headers.getAcceptableMediaTypes().contains(MediaType.APPLICATION_XML_TYPE)
                      ? MediaType.APPLICATION_XML_TYPE : WADL_TYPE;  
        return Response.ok().type(type).entity(sbMain.toString()).build();
    }

    private void handleResource(StringBuilder sb, ClassResourceInfo cri, String path,
                                List<String> templateVars) {
        sb.append("<resource path=\"").append(path).append("\">");
        handleTemplateParams(sb, templateVars);
        
        List<OperationResourceInfo> sortedOps = sortOperationsByPath(
            cri.getMethodDispatcher().getOperationResourceInfos());
        
        for (OperationResourceInfo ori : sortedOps) {
            
            if (ori.getHttpMethod() == null) {
                Class<?> cls = ori.getMethodToInvoke().getReturnType();
                ClassResourceInfo subcri = cri.findResource(cls, cls);
                if (subcri != null) {
                    handleResource(sb, subcri, ori.getURITemplate().getValue(), 
                                   ori.getURITemplate().getVariables());
                } else {
                    handleDynamicSubresource(sb, ori);
                }
                continue;
            }
            handleOperation(sb, ori);
        }
        sb.append("</resource>");
    }
    
    private void handleOperation(StringBuilder sb, OperationResourceInfo ori) {
        
        String path = ori.getURITemplate().getValue();
        boolean isSlash =  "/".equals(path);
        if (!isSlash) {
            sb.append("<resource path=\"").append(path).append("\">");
        }
        handleTemplateParams(sb, ori.getURITemplate().getVariables());
        handleMatrixParams(sb, ori);
        
        sb.append("<method name=\"").append(ori.getHttpMethod()).append("\">");
        if (ori.getMethodToInvoke().getParameterTypes().length != 0) {
            sb.append("<request>");
            for (Parameter p : ori.getParameters()) {        
                handleParameter(sb, ori, p);             
            }
            sb.append("</request>");
        }
        boolean isVoid = void.class == ori.getMethodToInvoke().getReturnType();
        if (isVoid) {
            sb.append("<!-- Only status code is returned -->");
        }
        sb.append("<response>");
        if (void.class != ori.getMethodToInvoke().getReturnType()) {
            handleRepresentation(sb, ori, ori.getMethodToInvoke().getReturnType(), false);
        }
        sb.append("</response>");
        
        sb.append("</method>");
        
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
            handleRepresentation(sb, ori, ori.getMethodToInvoke().getParameterTypes()[pm.getIndex()],
                                 true);
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
    
    private void handleRepresentation(StringBuilder sb, OperationResourceInfo ori, 
                                      Class<?> type, boolean inbound) {
        if (InjectionUtils.isPrimitive(type)) {
            sb.append("<!-- Primitive type : " + type.getSimpleName() + " -->");
        }
        sb.append("<representation");
        
        List<MediaType> types = inbound ? ori.getConsumeTypes() : ori.getProduceTypes();
        boolean wildcardOnly = true; 
        for (MediaType mt : types) {
            if (!mt.isWildcardType()) {
                wildcardOnly = false;
                break;
            }
        }
        if (!wildcardOnly) {
            sb.append(" mediaType=\"");
            for (int i = 0; i < types.size(); i++) {
                sb.append(types.get(i).toString());
                if (i + 1 < types.size()) {
                    sb.append(',');
                }
            }
            if (types.size() > 0) {
                sb.append("\"");
            }
        }
        sb.append(">");
        
        if (!type.isPrimitive()) {
            // try to use JAXB
            // TODO : reuse JaxbDatabinding code
            JAXBElementProvider jaxb = new JAXBElementProvider();
            try {
                JAXBContext context = jaxb.getPackageContext(type);
                if (context == null) {
                    context = jaxb.getClassContext(type);
                }
                if (context != null) {
                    StringWriter writer = new StringWriter();
                    XMLStreamWriter streamWriter = StaxUtils.createXMLStreamWriter(writer);
                    final StreamWriterContentHandler handler = new StreamWriterContentHandler(streamWriter);
                    context.generateSchema(new SchemaOutputResolver() {
                        @Override
                        public Result createOutput(String ns, String file) throws IOException {
                            SAXResult result = new SAXResult(handler);
                            result.setSystemId(file);
                            return result;
                        }
                    });
                    streamWriter.flush();
                    sb.append(writer.toString());
                }
            } catch (Exception ex) {
                LOG.fine("No schema can be generated from " + type.getName());
            }
        }
        
        sb.append("</representation>");
    }
    
    private List<OperationResourceInfo> sortOperationsByPath(Set<OperationResourceInfo> ops) {
        List<OperationResourceInfo> opsWithSamePath = new LinkedList<OperationResourceInfo>(ops);
        Collections.sort(opsWithSamePath, new OperationResourceInfoComparator());        
        return opsWithSamePath;
    }
    
    
}
