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
import java.lang.annotation.Annotation;
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
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.XmlSchemaPrimitiveUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxb.JAXBBeanInfo;
import org.apache.cxf.jaxb.JAXBContextProxy;
import org.apache.cxf.jaxb.JAXBUtils;
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
import org.apache.ws.commons.schema.XmlSchema;

public class WadlGenerator implements RequestHandler {

    public static final String WADL_QUERY = "_wadl"; 
    public static final MediaType WADL_TYPE = MediaType.valueOf("application/vnd.sun.wadl+xml"); 
    public static final String WADL_NS = "http://research.sun.com/wadl/2006/10";    
    
    private static final Logger LOG = LogUtils.getL7dLogger(WadlGenerator.class);
    private static final String JAXB_DEFAULT_NAMESPACE = "##default";
    private static final String JAXB_DEFAULT_NAME = "##default";
    
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
              .append("\" xmlns:xs=\"").append(XmlSchemaConstants.XSD_NAMESPACE_URI).append("\"");
        StringBuilder sbGrammars = new StringBuilder();
        sbGrammars.append("<grammars>");
        
        StringBuilder sbResources = new StringBuilder();
        sbResources.append("<resources base=\"").append(ui.getBaseUri().toString()).append("\">");
        
        List<ClassResourceInfo> cris = getResourcesList(m, resource);
        
        Set<Class<?>> jaxbTypes = getAllJaxbTypes(cris);
        JAXBContext context = createJaxbContext(jaxbTypes);
        SchemaCollection coll = getSchemaCollection(context);
        JAXBContextProxy proxy = null;
        if (coll != null) {
            proxy = ReflectionInvokationHandler.createProxyWrapper(context, JAXBContextProxy.class);
        }
        Map<Class<?>, QName> clsMap = new IdentityHashMap<Class<?>, QName>();
        
        for (ClassResourceInfo cri : cris) {
            handleResource(sbResources, jaxbTypes, proxy, clsMap,
                           cri, cri.getURITemplate().getValue());
        }
        sbResources.append("</resources>");
        
        handleGrammars(sbMain, sbGrammars, coll, clsMap);
        
        sbGrammars.append("</grammars>");
        sbMain.append(">");
        sbMain.append(sbGrammars.toString());
        sbMain.append(sbResources.toString());
        sbMain.append("</application>");
        
        HttpHeaders headers = new HttpHeadersImpl(m);
        MediaType type = headers.getAcceptableMediaTypes().contains(MediaType.APPLICATION_XML_TYPE)
                      ? MediaType.APPLICATION_XML_TYPE : WADL_TYPE;  
        return Response.ok().type(type).entity(sbMain.toString()).build();
    }

    private void handleGrammars(StringBuilder sbApp, StringBuilder sbGrammars, 
                                SchemaCollection coll, Map<Class<?>, QName> clsMap) {
        Map<String, String> map = new HashMap<String, String>();
        for (QName qname : clsMap.values()) {
            map.put(qname.getPrefix(), qname.getNamespaceURI());
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sbApp.append(" xmlns:").append(entry.getKey()).append("=\"")
                 .append(entry.getValue()).append("\"");
        }
        
        
        writeSchemas(sbGrammars, coll);
    }
    
    private void writeSchemas(StringBuilder sb, SchemaCollection coll) {
        if (coll == null) {
            return;
        }
        for (XmlSchema xs : coll.getXmlSchemas()) {
            if (xs.getItems().getCount() == 0) {
                continue;
            }
            StringWriter writer = new StringWriter();
            xs.write(writer);
            sb.append(writer.toString());
        }
    }
    
    private void handleResource(StringBuilder sb, Set<Class<?>> jaxbTypes, JAXBContextProxy jaxbProxy,
                                Map<Class<?>, QName> clsMap, ClassResourceInfo cri, String path) {
        sb.append("<resource path=\"").append(path).append("\">");
        
        List<OperationResourceInfo> sortedOps = sortOperationsByPath(
            cri.getMethodDispatcher().getOperationResourceInfos());
        
        for (OperationResourceInfo ori : sortedOps) {
            
            if (ori.getHttpMethod() == null) {
                Class<?> cls = ori.getMethodToInvoke().getReturnType();
                ClassResourceInfo subcri = cri.findResource(cls, cls);
                if (subcri != null) {
                    handleResource(sb, jaxbTypes, jaxbProxy, clsMap, subcri, 
                                   ori.getURITemplate().getValue());
                } else {
                    handleDynamicSubresource(sb, jaxbTypes, jaxbProxy, clsMap, ori);
                }
                continue;
            }
            handleOperation(sb, jaxbTypes, jaxbProxy, clsMap, ori);
        }
        sb.append("</resource>");
    }
    
    private void handleOperation(StringBuilder sb, Set<Class<?>> jaxbTypes, JAXBContextProxy jaxbProxy, 
                                 Map<Class<?>, QName> clsMap, OperationResourceInfo ori) {
        
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
            if (isFormRequest(ori)) {
                handleRepresentation(sb, jaxbTypes, jaxbProxy, clsMap, ori, null, false);
            } else {
                for (Parameter p : ori.getParameters()) {        
                    handleParameter(sb, jaxbTypes, jaxbProxy, clsMap, ori, p);             
                }
            }
            sb.append("</request>");
        }
        boolean isVoid = void.class == ori.getMethodToInvoke().getReturnType();
        if (isVoid) {
            sb.append("<!-- Only status code is returned -->");
        }
        sb.append("<response>");
        if (void.class != ori.getMethodToInvoke().getReturnType()) {
            handleRepresentation(sb, jaxbTypes, jaxbProxy, clsMap, ori,
                                 ori.getMethodToInvoke().getReturnType(), false);
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
    
    private void handleDynamicSubresource(StringBuilder sb, Set<Class<?>> jaxbTypes, 
                 JAXBContextProxy jaxbProxy, Map<Class<?>, QName> clsMap, OperationResourceInfo ori) {
        
        sb.append("<!-- Dynamic subresource -->");
        sb.append("<resource path=\"").append(ori.getURITemplate().getValue()).append("\">");
        if (ori.getMethodToInvoke().getParameterTypes().length != 0) {
            sb.append("<request>");
            for (Parameter p : ori.getParameters()) {        
                handleParameter(sb, jaxbTypes, jaxbProxy, clsMap, ori, p);             
            }
            sb.append("</request>");
        }
        sb.append("</resource>");
    }
    
    
    private void handleParameter(StringBuilder sb, Set<Class<?>> jaxbTypes, JAXBContextProxy jaxbProxy, 
                                 Map<Class<?>, QName> clsMap, OperationResourceInfo ori, Parameter pm) {
        Class<?> cls = ori.getMethodToInvoke().getParameterTypes()[pm.getIndex()];
        if (pm.getType() == ParameterType.REQUEST_BODY) {
            handleRepresentation(sb, jaxbTypes, jaxbProxy, clsMap, ori, cls, true);
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
                       : ParameterType.FORM == pm.getType() ? "query"
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
    
    private void handleRepresentation(StringBuilder sb, Set<Class<?>> jaxbTypes, JAXBContextProxy jaxbProxy,
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
                if (!mt.isWildcardType()) {
                    sb.append(" mediaType=\"").append(mt.toString()).append("\"");
                }
                if (jaxbProxy != null && mt.getSubtype().contains("xml") && jaxbTypes.contains(type)) {
                    generateQName(sb, jaxbProxy, clsMap, type);
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
    

    private void generateQName(StringBuilder sb,
                               JAXBContextProxy jaxbProxy,
                               Map<Class<?>, QName> clsMap, 
                               Class<?> type) {
        
        QName typeQName = clsMap.get(type);
        if (typeQName != null) {
            writeQName(sb, typeQName);
            return;
        }
        
        QName qname = getQName(jaxbProxy, type, clsMap);
        
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
    
    private QName getQName(JAXBContextProxy jaxbProxy, Class<?> type, Map<Class<?>, QName> clsMap) {
        
        XmlRootElement root = type.getAnnotation(XmlRootElement.class);
        if (root != null) {
            return getQNameFromParts(root.name(), root.namespace(), clsMap);
        }
        
        try {
            JAXBBeanInfo jaxbInfo = JAXBUtils.getBeanInfo(jaxbProxy, type);
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
    
    private Set<Class<?>> getAllJaxbTypes(List<ClassResourceInfo> cris) {
        Set<Class<?>> types = new HashSet<Class<?>>();
        for (ClassResourceInfo root : cris) {
            for (OperationResourceInfo ori : root.getMethodDispatcher().getOperationResourceInfos()) {
                checkJaxbType(ori.getMethodToInvoke().getReturnType(), types);
                for (Parameter pm : ori.getParameters()) {
                    if (pm.getType() == ParameterType.REQUEST_BODY) {
                        checkJaxbType(ori.getMethodToInvoke().getParameterTypes()[pm.getIndex()], types);
                    }
                }
            }
        }
        
        return types;
    }

    private void checkJaxbType(Class<?> type, Set<Class<?>> types) {
        JAXBElementProvider provider = new JAXBElementProvider();
        if (!InjectionUtils.isPrimitive(type) 
            && !JAXBElement.class.isAssignableFrom(type)
            && provider.isReadable(type, type, new Annotation[0], MediaType.APPLICATION_XML_TYPE)) {
            types.add(type);
        }        
    }
    
    private JAXBContext createJaxbContext(Set<Class<?>> classes) {
        if (classes.isEmpty()) {
            return null;
        }
        JAXBUtils.scanPackages(classes, null);
        JAXBContext ctx;
        try {
            ctx = JAXBContext.newInstance(classes.toArray(new Class[classes.size()]));
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
    
    
}
