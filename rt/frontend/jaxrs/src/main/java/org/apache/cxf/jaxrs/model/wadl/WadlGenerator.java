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
import java.util.Comparator;
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
import org.apache.cxf.common.util.XmlSchemaPrimitiveUtils;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.StreamWriterContentHandler;

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
        sbMain.append("<application xmlns=\"").append(WADL_NS)
              .append("\" xmlns:xs=\"").append(XmlSchemaConstants.XSD_NAMESPACE_URI).append("\">");
        StringBuilder sbGrammars = new StringBuilder();
        sbGrammars.append("<grammars>");
        
        StringBuilder sbResources = new StringBuilder();
        sbResources.append("<resources base=\"").append(ui.getBaseUri().toString()).append("\">");
        
        List<ClassResourceInfo> cris = getResourcesList(m, resource);
        for (ClassResourceInfo cri : cris) {
            handleResource(sbResources, cri, cri.getURITemplate().getValue());
        }
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

    private void handleResource(StringBuilder sb, ClassResourceInfo cri, String path) {
        sb.append("<resource path=\"").append(path).append("\">");
        
        List<OperationResourceInfo> sortedOps = sortOperationsByPath(
            cri.getMethodDispatcher().getOperationResourceInfos());
        
        for (OperationResourceInfo ori : sortedOps) {
            
            if (ori.getHttpMethod() == null) {
                Class<?> cls = ori.getMethodToInvoke().getReturnType();
                ClassResourceInfo subcri = cri.findResource(cls, cls);
                if (subcri != null) {
                    handleResource(sb, subcri, ori.getURITemplate().getValue());
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
        boolean useResource = useResource(ori);
        if (useResource) {
            sb.append("<resource path=\"").append(path).append("\">");
        }
        handleParams(sb, ori, ParameterType.PATH);
        handleParams(sb, ori, ParameterType.MATRIX);
        
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
        
        if (useResource) {
            sb.append("</resource>");
        }
    }
    
    private boolean useResource(OperationResourceInfo ori) {
        String path = ori.getURITemplate().getValue();
        if ("/".equals(path)) {
            for (Parameter pm : ori.getParameters()) {
                if (pm.getType() == ParameterType.PATH || pm.getType() == ParameterType.MATRIX) {
                    return true;
                }
            }
            return false;
        }
        return true;
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
        Class<?> cls = ori.getMethodToInvoke().getParameterTypes()[pm.getIndex()];
        if (pm.getType() == ParameterType.REQUEST_BODY) {
            handleRepresentation(sb, ori, cls, true);
            return;
        }
        if (pm.getType() == ParameterType.PATH || pm.getType() == ParameterType.MATRIX) {
            return;
        }
        if (pm.getType() == ParameterType.HEADER || pm.getType() == ParameterType.QUERY) {
            writeParam(sb, pm, ori);
        }
        
    }
    
    private void handleParams(StringBuilder sb, OperationResourceInfo ori, ParameterType type) {
        for (Parameter pm : ori.getParameters()) {
            if (pm.getType() == type) {
                writeParam(sb, pm, ori);
            }
        }
    }
    
    private void writeParam(StringBuilder sb, Parameter pm, OperationResourceInfo ori) {
        sb.append("<param name=\"").append(pm.getName()).append("\" ");
        String style = ParameterType.PATH == pm.getType() ? "template" 
                                                          : pm.getType().toString().toLowerCase();
        sb.append("style=\"").append(style).append("\"");
        if (pm.getDefaultValue() != null) {
            sb.append(" default=\"").append(pm.getDefaultValue()).append("\"");
        }
        Class<?> type = ori.getMethodToInvoke().getParameterTypes()[pm.getIndex()];
        String value = XmlSchemaPrimitiveUtils.getSchemaRepresentation(type);
        if (value != null) {
            sb.append(" type=\"").append(value).append("\"");
        }
        sb.append("/>");
    }
    
    private void handleRepresentation(StringBuilder sb, OperationResourceInfo ori, 
                                      Class<?> type, boolean inbound) {
        List<MediaType> types = inbound ? ori.getConsumeTypes() : ori.getProduceTypes();
        if (types.size() == 0) {
            types = Collections.singletonList(MediaType.APPLICATION_ATOM_XML_TYPE);
        }
        for (MediaType mt : types) {
            if (InjectionUtils.isPrimitive(type)) {
                String rep = XmlSchemaPrimitiveUtils.getSchemaRepresentation(type);
                String value = rep == null ? type.getSimpleName() : rep;
                sb.append("<!-- Primitive type : " + value + " -->");
            }
            sb.append("<representation");
            if (!mt.isWildcardType()) {
                sb.append(" mediaType=\"").append(mt.toString()).append("\"");
            }
            sb.append(">");
            if (!InjectionUtils.isPrimitive(type) && mt.getSubtype().contains("xml")) {
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
                        final StreamWriterContentHandler handler = 
                            new StreamWriterContentHandler(streamWriter);
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
    }
    
    private List<OperationResourceInfo> sortOperationsByPath(Set<OperationResourceInfo> ops) {
        List<OperationResourceInfo> opsWithSamePath = new LinkedList<OperationResourceInfo>(ops);
        Collections.sort(opsWithSamePath, new Comparator<OperationResourceInfo>() {

            public int compare(OperationResourceInfo op1, OperationResourceInfo op2) {
                boolean sub1 = op1.getHttpMethod() == null;
                boolean sub2 = op2.getHttpMethod() == null;
                if (sub1 && !sub2) {
                    return 1;
                } else if (!sub1 && sub2) {
                    return -1;
                }
                URITemplate ut1 = op1.getURITemplate();
                URITemplate ut2 = op2.getURITemplate();
                return ut1.getValue().compareTo(ut2.getValue());
            }
            
        });        
        return opsWithSamePath;
    }
    
    public List<ClassResourceInfo> getResourcesList(Message m, ClassResourceInfo cri) {
        return cri != null ? Collections.singletonList(cri)
               : ((JAXRSServiceImpl)m.getExchange().get(Service.class)).getClassResourceInfos();
    }
    
}
