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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.XmlSchemaPrimitiveUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxb.JAXBBeanInfo;
import org.apache.cxf.jaxb.JAXBContextProxy;
import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.ext.Description;
import org.apache.cxf.jaxrs.ext.Oneway;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.ext.xml.XMLName;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.commons.schema.XmlSchema;

public class WadlGenerator implements RequestHandler {

    public static final String WADL_QUERY = "_wadl"; 
    public static final MediaType WADL_TYPE = MediaType.valueOf("application/vnd.sun.wadl+xml"); 
    public static final String WADL_NS = "http://research.sun.com/wadl/2006/10";    
    
    private static final Logger LOG = LogUtils.getL7dLogger(WadlGenerator.class);
    private static final String JAXB_DEFAULT_NAMESPACE = "##default";
    private static final String JAXB_DEFAULT_NAME = "##default";
    private static final String CLASSPATH_PREFIX = "classpath:";
    
    private String wadlNamespace;
    private boolean ignoreMessageWriters = true;
    private boolean singleResourceMultipleMethods = true;
    private boolean useSingleSlashResource;
    private boolean addResourceAndMethodIds;
    
    private boolean useJaxbContextForQnames = true;
    
    private List<String> externalSchemasCache;
    private List<URI> externalSchemaLinks;
    private Map<String, List<String>> externalQnamesMap; 
    private ElementQNameResolver resolver;
    private List<String> privateAddresses;
    
    public WadlGenerator() {
        
    }
    
    public WadlGenerator(WadlGenerator other) {
        this.wadlNamespace = other.wadlNamespace;
        this.externalQnamesMap = other.externalQnamesMap;
        this.externalSchemaLinks = other.externalSchemaLinks;
        this.externalSchemasCache = other.externalSchemasCache;
        this.ignoreMessageWriters = other.ignoreMessageWriters;
        this.privateAddresses = other.privateAddresses;
        this.resolver = other.resolver;
        this.addResourceAndMethodIds = other.addResourceAndMethodIds;
        this.singleResourceMultipleMethods = other.singleResourceMultipleMethods;
        this.useJaxbContextForQnames = other.useJaxbContextForQnames;
        this.useSingleSlashResource = other.useSingleSlashResource;
    }
    
    public Response handleRequest(Message m, ClassResourceInfo resource) {
        
        if (!"GET".equals(m.get(Message.HTTP_REQUEST_METHOD))) {
            return null;
        }

        UriInfo ui = new UriInfoImpl(m);
        if (!ui.getQueryParameters().containsKey(WADL_QUERY)) {
            return null;
        }
        
        if (isPrivate(m)) {
            return Response.status(401).build();
        }
        
        StringBuilder sbMain = new StringBuilder();
        sbMain.append("<application xmlns=\"").append(getNamespace())
              .append("\" xmlns:xs=\"").append(XmlSchemaConstants.XSD_NAMESPACE_URI).append("\"");
        StringBuilder sbGrammars = new StringBuilder();
        sbGrammars.append("<grammars>");
        
        StringBuilder sbResources = new StringBuilder();
        sbResources.append("<resources base=\"").append(ui.getBaseUri().toString()).append("\">");
        
        List<ClassResourceInfo> cris = getResourcesList(m, resource);
        
        Set<Class<?>> allTypes = 
            ResourceUtils.getAllRequestResponseTypes(cris, useJaxbContextForQnames).keySet();
        
        JAXBContext context = createJaxbContext(allTypes);
        SchemaWriter schemaWriter = createSchemaWriter(context, ui);
        ElementQNameResolver qnameResolver = 
            schemaWriter == null ? null : createElementQNameResolver(context);
        
        Map<Class<?>, QName> clsMap = new IdentityHashMap<Class<?>, QName>();
        Set<ClassResourceInfo> visitedResources = new HashSet<ClassResourceInfo>();
        for (ClassResourceInfo cri : cris) {
            startResourceTag(sbResources, cri.getServiceClass(), cri.getURITemplate().getValue());
            handleDocs(cri.getServiceClass().getAnnotations(), sbResources);
            handleResource(sbResources, allTypes, qnameResolver, clsMap, cri, visitedResources);
            sbResources.append("</resource>");
        }
        sbResources.append("</resources>");
        
        handleGrammars(sbMain, sbGrammars, schemaWriter, clsMap);
        
        sbGrammars.append("</grammars>");
        sbMain.append(">");
        sbMain.append(sbGrammars.toString());
        sbMain.append(sbResources.toString());
        sbMain.append("</application>");
        
        m.getExchange().put(JAXRSUtils.IGNORE_MESSAGE_WRITERS, ignoreMessageWriters);
        
        HttpHeaders headers = new HttpHeadersImpl(m);
        MediaType type = headers.getAcceptableMediaTypes().contains(MediaType.APPLICATION_XML_TYPE)
                      ? MediaType.APPLICATION_XML_TYPE : WADL_TYPE;  
        return Response.ok().type(type).entity(sbMain.toString()).build();
    }

    private void handleGrammars(StringBuilder sbApp, StringBuilder sbGrammars, 
                                SchemaWriter writer, Map<Class<?>, QName> clsMap) {
        if (writer == null) {
            return;
        }
        
        Map<String, String> map = new HashMap<String, String>();
        for (QName qname : clsMap.values()) {
            map.put(qname.getPrefix(), qname.getNamespaceURI());
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sbApp.append(" xmlns:").append(entry.getKey()).append("=\"")
                 .append(entry.getValue()).append("\"");
        }
        
        writer.write(sbGrammars);
    }
    
    
    private void handleResource(StringBuilder sb, Set<Class<?>> jaxbTypes, 
                                ElementQNameResolver qnameResolver,
                                Map<Class<?>, QName> clsMap, ClassResourceInfo cri, 
                                Set<ClassResourceInfo> visitedResources) {
        visitedResources.add(cri);
        List<OperationResourceInfo> sortedOps = sortOperationsByPath(
            cri.getMethodDispatcher().getOperationResourceInfos());
        
        boolean resourceTagOpened = false;
        for (int i = 0; i < sortedOps.size(); i++) {
            OperationResourceInfo ori = sortedOps.get(i); 
            
            if (ori.getHttpMethod() == null) {
                Class<?> cls = ori.getMethodToInvoke().getReturnType();
                ClassResourceInfo subcri = cri.findResource(cls, cls);
                if (subcri != null && !visitedResources.contains(subcri)) {
                    startResourceTag(sb, subcri.getServiceClass(), ori.getURITemplate().getValue());
                    handleDocs(subcri.getServiceClass().getAnnotations(), sb);
                    handlePathAndMatrixParams(sb, ori);
                    handleResource(sb, jaxbTypes, qnameResolver, clsMap, subcri, 
                                   visitedResources);
                    sb.append("</resource>");
                } else {
                    handleDynamicSubresource(sb, jaxbTypes, qnameResolver, clsMap, ori, subcri);
                }
                continue;
            }
            OperationResourceInfo nextOp = i + 1 < sortedOps.size() ? sortedOps.get(i + 1) : null;
            resourceTagOpened = handleOperation(sb, jaxbTypes, qnameResolver, clsMap, ori, nextOp, 
                                                resourceTagOpened, i);
        }
    }
    
    private void startResourceTag(StringBuilder sb, Class<?> serviceClass, String path) {
        sb.append("<resource path=\"").append(path).append("\"");
        if (addResourceAndMethodIds) {
            QName jaxbQname = null;
            if (useJaxbContextForQnames) {
                jaxbQname = getJaxbQName(null, serviceClass, new HashMap<Class<?>, QName>(0));
            }
            String pName = jaxbQname == null ? PackageUtils.getPackageName(serviceClass) 
                : jaxbQname.getNamespaceURI();
            String localName = jaxbQname == null ? serviceClass.getSimpleName() 
                : jaxbQname.getLocalPart();
            sb.append(" id=\"").append("{" + pName + "}" + localName).append("\"");
        }
        sb.append(">");
    }
    
    private void startMethodTag(StringBuilder sb, OperationResourceInfo ori) {
        sb.append("<method name=\"").append(ori.getHttpMethod()).append("\"");
        if (addResourceAndMethodIds) {
            sb.append(" id=\"").append(ori.getMethodToInvoke().getName()).append("\"");
        }
        sb.append(">");
    }
    
    //CHECKSTYLE:OFF
    private boolean handleOperation(StringBuilder sb, Set<Class<?>> jaxbTypes, 
                                 ElementQNameResolver qnameResolver, 
                                 Map<Class<?>, QName> clsMap, 
                                 OperationResourceInfo ori,
                                 OperationResourceInfo nextOp,
                                 boolean resourceTagOpened,
                                 int index) {
    //CHECKSTYLE:ON    
        boolean samePathOperationFollows = singleResourceMultipleMethods && compareOperations(ori, nextOp);
        
        String path = ori.getURITemplate().getValue();
        if (!resourceTagOpened && openResource(path)) {
            resourceTagOpened = true;
            URITemplate template = ori.getClassResourceInfo().getURITemplate();
            if (template != null) {
                String parentPath = template.getValue();
                if (parentPath.endsWith("/") && path.startsWith("/") && path.length() > 1) {
                    path = path.substring(1); 
                }
            }
            sb.append("<resource path=\"").append(path).append("\">");
            handlePathAndMatrixParams(sb, ori);
        } else if (index == 0) {
            handlePathAndMatrixParams(sb, ori);
        }
        
        startMethodTag(sb, ori);
        handleDocs(ori.getAnnotatedMethod().getAnnotations(), sb);
        if (ori.getMethodToInvoke().getParameterTypes().length != 0) {
            sb.append("<request>");
            if (isFormRequest(ori)) {
                handleRepresentation(sb, jaxbTypes, qnameResolver, clsMap, ori, null, false);
            } else {
                for (Parameter p : ori.getParameters()) {        
                    handleParameter(sb, jaxbTypes, qnameResolver, clsMap, ori, p);             
                }
            }
            sb.append("</request>");
        }
        sb.append("<response");
        boolean isVoid = void.class == ori.getMethodToInvoke().getReturnType();
        if (isVoid) {
            boolean oneway = ori.getMethodToInvoke().getAnnotation(Oneway.class) != null;
            sb.append(" status=\"" + (oneway ? 202 : 204) + "\"");
        }
        sb.append(">");
        if (void.class != ori.getMethodToInvoke().getReturnType()) {
            handleRepresentation(sb, jaxbTypes, qnameResolver, clsMap, ori,
                                 ori.getMethodToInvoke().getReturnType(), false);
        }
        sb.append("</response>");
        
        sb.append("</method>");
        
        if (resourceTagOpened && !samePathOperationFollows) {
            sb.append("</resource>");
            resourceTagOpened = false;
        }
        return resourceTagOpened;
    }
    
    protected boolean compareOperations(OperationResourceInfo ori1, OperationResourceInfo ori2) {
        if (ori1 == null || ori2 == null
            || !ori1.getURITemplate().getValue().equals(ori2.getURITemplate().getValue())) {
            return false;
        }
        int ori1PathParams = 0;
        int ori1MatrixParams = 0;
        for (Parameter p : ori1.getParameters()) {
            if (p.getType() == ParameterType.PATH) {
                ori1PathParams++;
            } else if (p.getType() == ParameterType.MATRIX) {
                ori1MatrixParams++;
            }
        }
        
        int ori2PathParams = 0;
        int ori2MatrixParams = 0;
        for (Parameter p : ori2.getParameters()) {
            if (p.getType() == ParameterType.PATH) {
                ori2PathParams++;
            } else if (p.getType() == ParameterType.MATRIX) {
                ori2MatrixParams++;
            }
        }
        
        return ori1PathParams == ori2PathParams && ori1MatrixParams == ori2MatrixParams;
    }
    
    private boolean openResource(String path) {
        if ("/".equals(path)) {
            return useSingleSlashResource;
        }
        return true;
    }
    
    protected void handleDynamicSubresource(StringBuilder sb, Set<Class<?>> jaxbTypes, 
                 ElementQNameResolver qnameResolver, Map<Class<?>, QName> clsMap, OperationResourceInfo ori,
                 ClassResourceInfo subcri) {
        
        if (subcri != null) {
            sb.append("<!-- Recursive subresource -->");
        } else {
            sb.append("<!-- Dynamic subresource -->");    
        }
        startResourceTag(sb, subcri != null ? subcri.getServiceClass() : Object.class, 
            ori.getURITemplate().getValue());
        handlePathAndMatrixParams(sb, ori);
        sb.append("</resource>");
    }
    
    private void handlePathAndMatrixParams(StringBuilder sb, OperationResourceInfo ori) {
        handleParams(sb, ori, ParameterType.PATH);
        handleParams(sb, ori, ParameterType.MATRIX);
    }
    
    
    private void handleParameter(StringBuilder sb, Set<Class<?>> jaxbTypes, 
                                 ElementQNameResolver qnameResolver, 
                                 Map<Class<?>, QName> clsMap, OperationResourceInfo ori, Parameter pm) {
        Class<?> cls = ori.getMethodToInvoke().getParameterTypes()[pm.getIndex()];
        if (pm.getType() == ParameterType.REQUEST_BODY) {
            handleRepresentation(sb, jaxbTypes, qnameResolver, clsMap, ori, cls, true);
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
    
    private Annotation[] getBodyAnnotations(OperationResourceInfo ori, boolean inbound) {
        if (inbound) {
            for (Parameter pm : ori.getParameters()) {
                if (pm.getType() == ParameterType.REQUEST_BODY) {
                    return ori.getAnnotatedMethod().getParameterAnnotations()[pm.getIndex()];
                }
            }
            return new Annotation[]{};
        } else {
            return ori.getAnnotatedMethod().getDeclaredAnnotations();
        }
    }
    
    private void writeParam(StringBuilder sb, Parameter pm, OperationResourceInfo ori) {
        Class<?> type = ori.getMethodToInvoke().getParameterTypes()[pm.getIndex()];
        if (!"".equals(pm.getName())) {
            doWriteParam(sb, pm, type, pm.getName());
        } else {
            doWriteBeanParam(sb, type, pm, null);
        }
    }
    
    private void doWriteBeanParam(StringBuilder sb, Class<?> type, Parameter pm, String parentName) {
        Map<Parameter, Class<?>> pms = InjectionUtils.getParametersFromBeanClass(type, pm.getType(), true);
        for (Map.Entry<Parameter, Class<?>> entry : pms.entrySet()) {
            String name = entry.getKey().getName();
            if (parentName != null) {
                name = parentName + "." + name;
            }
            if (InjectionUtils.isPrimitive(entry.getValue())) {
                doWriteParam(sb, entry.getKey(), entry.getValue(), name);
            } else {
                doWriteBeanParam(sb, entry.getValue(), entry.getKey(), name);
            }
        }
    }
    
    protected void doWriteParam(StringBuilder sb, Parameter pm, Class<?> type, String paramName) {
        if (ParameterType.REQUEST_BODY == pm.getType()) {
            return;
        }
        sb.append("<param name=\"").append(paramName).append("\" ");
        String style = ParameterType.PATH == pm.getType() ? "template" 
                       : ParameterType.FORM == pm.getType() ? "query"
                       : pm.getType().toString().toLowerCase();
        sb.append("style=\"").append(style).append("\"");
        if (pm.getDefaultValue() != null) {
            sb.append(" default=\"").append(pm.getDefaultValue()).append("\"");
        }
        String value = XmlSchemaPrimitiveUtils.getSchemaRepresentation(type);
        if (value != null) {
            sb.append(" type=\"").append(value).append("\"");
        }
        sb.append("/>");
    }
    
    private void handleRepresentation(StringBuilder sb, Set<Class<?>> jaxbTypes, 
                                      ElementQNameResolver qnameResolver,
                                      Map<Class<?>, QName> clsMap, OperationResourceInfo ori, 
                                      Class<?> type, boolean inbound) {
        List<MediaType> types = inbound ? ori.getConsumeTypes() : ori.getProduceTypes();
        if (types.size() == 1 && types.get(0).equals(MediaType.WILDCARD_TYPE)
            && (type == null || MultivaluedMap.class.isAssignableFrom(type))) {
            types = Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        }
        if (type != null) {
            for (MediaType mt : types) {
                if (InjectionUtils.isPrimitive(type)) {
                    String rep = XmlSchemaPrimitiveUtils.getSchemaRepresentation(type);
                    String value = rep == null ? type.getSimpleName() : rep;
                    sb.append("<!-- Primitive type : " + value + " -->");
                }
                sb.append("<representation");
                sb.append(" mediaType=\"").append(mt.toString()).append("\"");
                if (qnameResolver != null && mt.getSubtype().contains("xml") && jaxbTypes.contains(type)) {
                    generateQName(sb, qnameResolver, clsMap, type,
                                  getBodyAnnotations(ori, inbound));
                }
                sb.append("/>");
            }
        } else { 
            sb.append("<representation");
            sb.append(" mediaType=\"").append(types.get(0).toString()).append("\">");
            for (Parameter pm : ori.getParameters()) {
                writeParam(sb, pm, ori);
            }
            sb.append("</representation>");
        }
    }
    
    protected List<OperationResourceInfo> sortOperationsByPath(Set<OperationResourceInfo> ops) {
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
                int result = ut1.getValue().compareTo(ut2.getValue());
                if (result == 0 && !(sub1 && sub2)) {
                    result = op1.getHttpMethod().compareTo(op2.getHttpMethod());
                }
                return result;
            }
            
        });        
        return opsWithSamePath;
    }
    
    public List<ClassResourceInfo> getResourcesList(Message m, ClassResourceInfo cri) {
        return cri != null ? Collections.singletonList(cri)
               : ((JAXRSServiceImpl)m.getExchange().get(Service.class)).getClassResourceInfos();
    }
    

    private void generateQName(StringBuilder sb,
                               ElementQNameResolver qnameResolver,
                               Map<Class<?>, QName> clsMap, 
                               Class<?> type,
                               Annotation[] annotations) {
        
        QName typeQName = clsMap.get(type);
        if (typeQName != null) {
            writeQName(sb, typeQName);
            return;
        }
        
        QName qname = qnameResolver.resolve(type, annotations,
                                            Collections.unmodifiableMap(clsMap));
        
        if (qname != null) {
            writeQName(sb, qname);
            clsMap.put(type, qname);
        }
    }    

    private void writeQName(StringBuilder sb, QName qname) {
        sb.append(" element=\"").append(qname.getPrefix()).append(':')
            .append(qname.getLocalPart()).append("\"");
    }
    
    private SchemaCollection getSchemaCollection(JAXBContext context) {
        if (context == null) {
            return null;
        }
        SchemaCollection xmlSchemaCollection = new SchemaCollection();
        Collection<DOMSource> schemas = new HashSet<DOMSource>();
        try {
            for (DOMResult r : JAXBUtils.generateJaxbSchemas(context, 
                                    CastUtils.cast(Collections.emptyMap(), String.class, DOMResult.class))) {
                schemas.add(new DOMSource(r.getNode(), r.getSystemId()));
            }
        } catch (IOException e) {
            LOG.fine("No schema can be generated");
            return null;
        }
        
        boolean hackAroundEmptyNamespaceIssue = false;
        for (DOMSource r : schemas) {
            hackAroundEmptyNamespaceIssue = 
                              addSchemaDocument(xmlSchemaCollection, 
                             (Document)r.getNode(),
                              r.getSystemId(),
                              hackAroundEmptyNamespaceIssue);
        }
        return xmlSchemaCollection;
    }
    
    private QName getJaxbQName(JAXBContextProxy jaxbProxy, Class<?> type, Map<Class<?>, QName> clsMap) {
        
        XmlRootElement root = type.getAnnotation(XmlRootElement.class);
        if (root != null) {
            QName qname = getQNameFromParts(root.name(), root.namespace(), clsMap);
            if (qname != null) {
                return qname;
            }
            String ns = JAXBUtils.getPackageNamespace(type);
            if (ns != null) {
                return getQNameFromParts(root.name(), ns, clsMap);
            } else {
                return null;
            }
        }
        
        try {
            JAXBBeanInfo jaxbInfo = jaxbProxy == null ? null : JAXBUtils.getBeanInfo(jaxbProxy, type);
            if (jaxbInfo == null) {
                return null;
            }
            Object instance = type.newInstance();
            return getQNameFromParts(jaxbInfo.getElementLocalName(instance), 
                                     jaxbInfo.getElementNamespaceURI(instance),
                                     clsMap);
        } catch (Exception ex) {
            // ignore    
        }
        return null;
    }
    
    private String getPrefix(String ns, Map<Class<?>, QName> clsMap) {
        String prefix = null;
        for (QName name : clsMap.values()) {
            if (name.getNamespaceURI().equals(ns)) {
                prefix = name.getPrefix();
                break;
            }
        }    
        if (prefix == null) {
            int size = new HashSet<QName>(clsMap.values()).size();
            prefix = "prefix" + (size + 1);
        }
        return prefix;
    }
    
    private JAXBContext createJaxbContext(Set<Class<?>> classes) {
        if (!useJaxbContextForQnames || classes.isEmpty()) {
            return null;
        }
        Set<Class<?>> classesToBeBound = new HashSet<Class<?>>(classes);
        JAXBUtils.scanPackages(classesToBeBound, null);

        JAXBContext ctx;
        try {
            ctx = JAXBContext.newInstance(classesToBeBound.toArray(new Class[classes.size()]));
            return ctx;
        } catch (JAXBException ex) {
            LOG.fine("No JAXB context can be created");
        }
        return null;
    }
    
    private boolean isFormRequest(OperationResourceInfo ori) {
        for (Parameter p : ori.getParameters()) {
            if (p.getType() == ParameterType.FORM) {
                return true;
            }
        }
        return false;
    }
    
    // TODO : can we reuse this block with JAXBBinding somehow ?
    public boolean addSchemaDocument(SchemaCollection col, 
                                     Document d,
                                     String systemId,
                                     boolean hackAroundEmptyNamespaceIssue) {
        String ns = d.getDocumentElement().getAttribute("targetNamespace");
        
        if (StringUtils.isEmpty(ns)) {
            if (DOMUtils.getFirstElement(d.getDocumentElement()) == null) {
                hackAroundEmptyNamespaceIssue = true;
                return hackAroundEmptyNamespaceIssue;
            }
            //create a copy of the dom so we 
            //can modify it.
            d = copy(d);
            ns = "";
            d.getDocumentElement().setAttribute("targetNamespace", ns);
        }
        
        if (hackAroundEmptyNamespaceIssue) {
            d = doEmptyNamespaceHack(d);            
        }
        
        Node n = d.getDocumentElement().getFirstChild();
        while (n != null) { 
            if (n instanceof Element) {
                Element e = (Element)n;
                if (e.getLocalName().equals("import")) {
                    e.removeAttribute("schemaLocation");
                }
            }
            n = n.getNextSibling();
        }
         
        synchronized (d) {
            col.read(d, systemId, null);
        }
        return hackAroundEmptyNamespaceIssue;
    }
 
    private Document doEmptyNamespaceHack(Document d) {
        boolean hasStuffToRemove = false;
        Element el = DOMUtils.getFirstElement(d.getDocumentElement());
        while (el != null) {
            if ("import".equals(el.getLocalName())
                && StringUtils.isEmpty(el.getAttribute("targetNamespace"))) {
                hasStuffToRemove = true;
                break;
            }
            el = DOMUtils.getNextElement(el);
        }
        if (hasStuffToRemove) {
            //create a copy of the dom so we 
            //can modify it.
            d = copy(d);
            el = DOMUtils.getFirstElement(d.getDocumentElement());
            while (el != null) {
                if ("import".equals(el.getLocalName())
                    && StringUtils.isEmpty(el.getAttribute("targetNamespace"))) {
                    d.getDocumentElement().removeChild(el);
                    el = DOMUtils.getFirstElement(d.getDocumentElement());
                } else {
                    el = DOMUtils.getNextElement(el);
                }
            }
        }
            
        return d;
    }

    private Document copy(Document doc) {
        try {
            return StaxUtils.copy(doc);
        } catch (XMLStreamException e) {
            //ignore
        } catch (ParserConfigurationException e) {
            //ignore
        }
        return doc;
    }
    
    
    private QName getQNameFromParts(String name, String namespace, Map<Class<?>, QName> clsMap) {
        if (name == null || JAXB_DEFAULT_NAME.equals(name) || name.length() == 0) {
            return null; 
        }
        if (namespace == null || JAXB_DEFAULT_NAMESPACE.equals(namespace) || namespace.length() == 0) {
            return null;
        }
        
        String prefix = getPrefix(namespace, clsMap);
        return new QName(namespace, name, prefix);
    }

    public void setIgnoreMessageWriters(boolean ignoreMessageWriters) {
        this.ignoreMessageWriters = ignoreMessageWriters;
    }

    private void handleDocs(Annotation[] anns, StringBuilder sb) {
        for (Annotation a : anns) {
            if (a.annotationType() == Description.class) {
                Description d = (Description)a;

                sb.append("<doc");
                if (d.lang().length() > 0) {
                    sb.append(" xml:lang=\"" + d.lang() + "\"");
                }
                if (d.title().length() > 0) {
                    sb.append(" title=\"" + d.title() + "\"");
                }
                sb.append(">");
                if (d.value().length() > 0) {
                    sb.append(d.value());
                } else if (d.docuri().length() > 0) {
                    InputStream is = null;
                    if (d.docuri().startsWith(CLASSPATH_PREFIX)) {
                        String path = d.docuri().substring(CLASSPATH_PREFIX.length());
                        is = ResourceUtils.getClasspathResourceStream(path, SchemaHandler.class,
                            BusFactory.getDefaultBus());
                        if (is != null) {
                            try { 
                                sb.append(IOUtils.toString(is));
                            } catch (IOException ex) {
                                // ignore
                            }
                        }
                    }
                }
                sb.append("</doc>");
            }
        }
    }

    private String getNamespace() {
        return wadlNamespace != null ? wadlNamespace : WADL_NS;
    }
    
    public void setWadlNamespace(String namespace) {
        this.wadlNamespace = namespace;
    }

    public void setSingleResourceMultipleMethods(boolean singleResourceMultipleMethods) {
        this.singleResourceMultipleMethods = singleResourceMultipleMethods;
    }

    public void setUseSingleSlashResource(boolean useSingleSlashResource) {
        this.useSingleSlashResource = useSingleSlashResource;
    }

    public void setSchemaLocations(List<String> locations) {
        
        externalQnamesMap = new HashMap<String, List<String>>();
        externalSchemasCache = new ArrayList<String>(locations.size()); 
        for (int i = 0; i < locations.size(); i++) {
            String loc = locations.get(i);
            try {
                InputStream is = ResourceUtils.getResourceStream(loc, BusFactory.getDefaultBus());
                if (is == null) {
                    return;
                }
                ByteArrayInputStream bis = IOUtils.loadIntoBAIS(is);
                XMLSource source = new XMLSource(bis);
                source.setBuffering(true);
                String targetNs = source.getValue("/*/@targetNamespace");
                
                Map<String, String> nsMap = 
                    Collections.singletonMap("xs", XmlSchemaConstants.XSD_NAMESPACE_URI);
                String[] elementNames = source.getValues("/*/xs:element/@name", nsMap);
                externalQnamesMap.put(targetNs, Arrays.asList(elementNames));
                String schemaValue = source.getNode("/xs:schema", nsMap, String.class);
                externalSchemasCache.add(schemaValue);
            } catch (Exception ex) {
                LOG.warning("No schema resource " + loc + " can be loaded : " + ex.getMessage());
                externalSchemasCache = null;
                externalQnamesMap = null;
                return;
            }
        }
         
    }

    public void setUseJaxbContextForQnames(boolean checkJaxbOnly) {
        this.useJaxbContextForQnames = checkJaxbOnly;
    }
    
    protected ElementQNameResolver createElementQNameResolver(JAXBContext context) {
        if (resolver != null) {
            return resolver;
        }
        if (useJaxbContextForQnames) {
            if (context != null) {
                JAXBContextProxy proxy = 
                    ReflectionInvokationHandler.createProxyWrapper(context, JAXBContextProxy.class);
                return new JaxbContextQNameResolver(proxy);
            } else {
                return null;
            }
        } else if (externalQnamesMap != null) {
            return new SchemaQNameResolver(externalQnamesMap);
        } else {
            return new XMLNameQNameResolver();
        }
    }
    
    protected SchemaWriter createSchemaWriter(JAXBContext context, UriInfo ui) {
        // if neither externalSchemaLinks nor externalSchemasCache is set
        // then JAXBContext will be used to generate the schema
        if (externalSchemaLinks != null && externalSchemasCache == null) {
            return new ExternalSchemaWriter(externalSchemaLinks, ui);
        } else if (externalSchemasCache != null) {
            return new StringSchemaWriter(externalSchemasCache, externalSchemaLinks, ui);
        } else if (context != null) {
            SchemaCollection coll = getSchemaCollection(context);
            if (coll != null) {
                return new SchemaCollectionWriter(coll);
            }
        }
        return null;
    }
    
    public void setExternalLinks(List<String> externalLinks) {
        externalSchemaLinks = new LinkedList<URI>();
        for (String s : externalLinks) {
            try {
                externalSchemaLinks.add(URI.create(s));
            } catch (Exception ex) {
                LOG.warning("Not a valid URI : " + s);
                externalSchemaLinks = null;
                break;
            }
        }
    }

    private static interface SchemaWriter {
        void write(StringBuilder sb);
    }
    
    private class StringSchemaWriter implements SchemaWriter {
        
        private List<String> theSchemas;
        
        public StringSchemaWriter(List<String> schemas, List<URI> links, UriInfo ui) {
            
            this.theSchemas = new LinkedList<String>();
            // we'll need to do the proper schema caching eventually 
            for (String s : schemas) {
                XMLSource source = new XMLSource(new ByteArrayInputStream(s.getBytes()));
                source.setBuffering(true);
                Map<String, String> locs = getLocationsMap(source, "import", links, ui);
                String actualSchema = !locs.isEmpty() ? transformSchema(s, locs) : s;
                theSchemas.add(actualSchema);
            }
        }

        private Map<String, String> getLocationsMap(XMLSource source, String elementName,
                                                    List<URI> links, UriInfo ui) {
            Map<String, String> nsMap = 
                Collections.singletonMap("xs", XmlSchemaConstants.XSD_NAMESPACE_URI);
            String[] locations = source.getValues("/*/xs:" + elementName + "/@schemaLocation", nsMap);
            if (locations == null) {
                return Collections.emptyMap();
            }
            
            Map<String, String> locs = new HashMap<String, String>();
            for (String loc : locations) {
                try {
                    URI uri = URI.create(loc);
                    if (!uri.isAbsolute()) {
                        if (links != null) {
                            for (URI overwriteURI : links) {
                                if (overwriteURI.toString().endsWith(loc)) {
                                    if (overwriteURI.isAbsolute()) {
                                        locs.put(loc, overwriteURI.toString());
                                    } else {
                                        locs.put(loc, ui.getBaseUriBuilder().path(
                                            overwriteURI.toString()).build().toString());
                                    }
                                    break;
                                }
                            }
                        }
                        if (!locs.containsKey(loc)) {
                            locs.put(loc, ui.getBaseUriBuilder().path(
                                 loc.toString()).build().toString());
                        }
                    }
                } catch (Exception ex) {
                    // continue
                }
            }
            return locs;
        }
        
        private String transformSchema(String schema, Map<String, String> locs) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            SchemaConverter sc = new SchemaConverter(StaxUtils.createXMLStreamWriter(bos), locs);
            try {
                StaxUtils.copy(new StreamSource(new StringReader(schema)), sc);
                sc.flush();
                sc.close();
                return bos.toString();
            } catch (Exception ex) {
                return schema;
            }
            
        }
        
        public void write(StringBuilder sb) {
            for (String s : theSchemas) { 
                sb.append(s);
            }
        }
    }
    
    private class SchemaCollectionWriter implements SchemaWriter {
        
        private SchemaCollection coll;
        
        public SchemaCollectionWriter(SchemaCollection coll) {
            this.coll = coll;
        }
        
        public void write(StringBuilder sb) {
            for (XmlSchema xs : coll.getXmlSchemas()) {
                if (xs.getItems().getCount() == 0) {
                    continue;
                }
                StringWriter writer = new StringWriter();
                xs.write(writer);
                sb.append(writer.toString());
            }
        }
    }
    
    private class ExternalSchemaWriter implements SchemaWriter {
        
        private List<URI> links;
        private UriInfo uriInfo; 
        
        public ExternalSchemaWriter(List<URI> links, UriInfo ui) {
            this.links = links;
            this.uriInfo = ui;
        }
        
        public void write(StringBuilder sb) {
            for (URI link : links) {
                try {
                    URI value = link.isAbsolute() ? link 
                        : uriInfo.getBaseUriBuilder().path(link.toString()).build(); 
                    sb.append("<include href=\"").append(value.toString()).append("\"/>");
                } catch (Exception ex) {
                    LOG.warning("WADL grammar section will be incomplete, this link is not a valid URI : "
                                + link.toString());
                }
            }
        }
    }
    
    private class JaxbContextQNameResolver implements ElementQNameResolver {

        private JAXBContextProxy proxy;
        
        public JaxbContextQNameResolver(JAXBContextProxy proxy) {
            this.proxy = proxy;
        }
        
        public QName resolve(Class<?> type, Annotation[] annotations, Map<Class<?>, QName> clsMap) {
            return getJaxbQName(proxy, type, clsMap);
        }
        
    }
    
    private class XMLNameQNameResolver implements ElementQNameResolver {

        public QName resolve(Class<?> type, Annotation[] annotations, Map<Class<?>, QName> clsMap) {
            XMLName name = AnnotationUtils.getAnnotation(annotations, XMLName.class);
            if (name == null) {
                name = type.getAnnotation(XMLName.class);
            }
            if (name != null) {
                QName qname = JAXRSUtils.convertStringToQName(name.value(), name.prefix());
                if (qname.getPrefix().length() > 0) {
                    return qname;
                } else {
                    return getQNameFromParts(qname.getLocalPart(),
                                             qname.getNamespaceURI(), clsMap);                    
                }
            }
            return null;
        }
        
    }
    
    private class SchemaQNameResolver implements ElementQNameResolver {

        private Map<String, List<String>> map;
        
        public SchemaQNameResolver(Map<String, List<String>> map) {
            this.map = map;
        }
        
        public QName resolve(Class<?> type, Annotation[] annotations, Map<Class<?>, QName> clsMap) {
            String name = type.getSimpleName();
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                String elementName = null;  
                if (entry.getValue().contains(name)) { 
                    elementName = name;                    
                } else if (entry.getValue().contains(name.toLowerCase())) { 
                    elementName = name.toLowerCase();                    
                }  
                if (elementName != null) {
                    return getQNameFromParts(elementName, entry.getKey(), clsMap);
                }
            }
            return null;
        }
        
    }
    
    public void setResolver(ElementQNameResolver resolver) {
        this.resolver = resolver;
    }
    
    public void setPrivateAddresses(List<String> privateAddresses) {
        this.privateAddresses = privateAddresses;
    }
    
    public List<String> getPrivateAddresses() {
        return privateAddresses;
    }

    private boolean isPrivate(Message m) {
        return MessageUtils.isTrue(m.getContextualProperty("org.apache.cxf.endpoint.private")); 
    }
    
    public void setAddResourceAndMethodIds(boolean addResourceAndMethodIds) {
        this.addResourceAndMethodIds = addResourceAndMethodIds;
    }

    private static class SchemaConverter extends DelegatingXMLStreamWriter {
        private static final String SCHEMA_LOCATION = "schemaLocation";
        private Map<String, String> locsMap;    
        public SchemaConverter(XMLStreamWriter writer, Map<String, String> locsMap) {
            super(writer);
            this.locsMap = locsMap;
        }
    
        public void writeAttribute(String local, String value) throws XMLStreamException {
            if (SCHEMA_LOCATION.equals(local) && locsMap.containsKey(value)) {
                value = locsMap.get(value);
            }
            super.writeAttribute(local, value);
        }
    }        
        
}
