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
package org.apache.cxf.jaxrs.ext.codegen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.jaxb.JAXBUtils.JCodeModel;
import org.apache.cxf.jaxb.JAXBUtils.S2JJAXBModel;
import org.apache.cxf.jaxb.JAXBUtils.SchemaCompiler;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class CodeGeneratorProvider implements RequestHandler {
    public static final String CODE_QUERY = "_code";
    public static final String LANGUAGE_QUERY = "_lang";
    public static final String OS_QUERY = "_os";
    public static final String SOURCE_QUERY = "_source";
    public static final String CODE_TYPE_QUERY = "_codeType";
    public static final String CODE_TYPE_GRAMMAR = "grammar";
    public static final String CODE_TYPE_PROXY = "proxy";
    public static final String CODE_TYPE_WEB = "web";
    
    private static final Logger LOG = LogUtils.getL7dLogger(CodeGeneratorProvider.class);
    private static final Set<String> SUPPORTED_LANGUAGES = new HashSet<String>(
        Arrays.asList(new String[]{"java"}));
    
    private static final String TMPDIR = System.getProperty("java.io.tmpdir");
    private static final String TAB = "    "; 
    
    private static final Map<String, Class<?>> HTTP_METHOD_ANNOTATIONS;
    private static final Map<String, Class<?>> PARAM_ANNOTATIONS;
    
    static {
        HTTP_METHOD_ANNOTATIONS = new HashMap<String, Class<?>>();
        HTTP_METHOD_ANNOTATIONS.put("get", GET.class);
        HTTP_METHOD_ANNOTATIONS.put("put", PUT.class);
        HTTP_METHOD_ANNOTATIONS.put("post", POST.class);
        HTTP_METHOD_ANNOTATIONS.put("delete", DELETE.class);
        HTTP_METHOD_ANNOTATIONS.put("head", HEAD.class);
        
        PARAM_ANNOTATIONS = new HashMap<String, Class<?>>();
        PARAM_ANNOTATIONS.put("template", PathParam.class);
        PARAM_ANNOTATIONS.put("header", HeaderParam.class);
        PARAM_ANNOTATIONS.put("query", QueryParam.class);
        PARAM_ANNOTATIONS.put("matrix", MatrixParam.class);
    }

    private Comparator<String> importsComparator;
    private UriInfo ui;
    private boolean generateInterfaces = true;
    
    
    @Context
    public void setUriInfo(UriInfo uriInfo) {
        this.ui = uriInfo;
    }
    
    public Response handleRequest(Message m, ClassResourceInfo resourceClass) {
        
        if (!"GET".equals(m.get(Message.HTTP_REQUEST_METHOD))) {
            return null;
        }
        
        if (ui.getQueryParameters().containsKey(SOURCE_QUERY)) {
            synchronized (this) { 
                return getSource(new File(TMPDIR, getStem(resourceClass, "zip")));
            }
        }
        
        String codeQuery = ui.getQueryParameters().getFirst(CODE_QUERY);
        if (codeQuery == null) {
            return null;
        }
        
        String language = ui.getQueryParameters().getFirst(LANGUAGE_QUERY);
        if (language != null && !SUPPORTED_LANGUAGES.contains(language)) {
            return Response.noContent().entity("Unsupported language" + language).type("text/plain").build();
        }
        return doHandleRequest(m, resourceClass);
    }
    
    protected Response doHandleRequest(Message m, ClassResourceInfo resourceClass) { 
        synchronized (this) {
            File zipDir = new File(TMPDIR, getStem(resourceClass, "zip"));
            Response r = getLink(zipDir, m);
            if (r != null) {
                return r;
            }
            
            File srcDir = new File(TMPDIR, getStem(resourceClass, "src"));
            if (!srcDir.exists() && !srcDir.mkdir()) {
                throw new IllegalStateException("Unable to create working directory " + srcDir.getPath());
            }
            String codeType = ui.getQueryParameters().getFirst(CODE_TYPE_QUERY);
            try {
                String wadl = getWadl(m, resourceClass);
                if (wadl == null) {
                    LOG.warning("WADL for " 
                         + (resourceClass != null ? resourceClass.getServiceClass().getName() 
                             : "this service")
                         + " can not be loaded");
                    return Response.noContent().build();
                }
                generateSource(wadl, srcDir, codeType);
                zipSource(srcDir, zipDir);
                return getLink(zipDir, m);
            } catch (Exception ex) {
                LOG.warning("Code can not be generated for " 
                            + resourceClass != null ? resourceClass.getServiceClass().getName() 
                                : "this service");
                FileUtils.removeDir(zipDir);
                return Response.noContent().build();
            } finally {
                FileUtils.removeDir(srcDir);
            }
        }
    }
    
    private void zipSource(File srcDir, File zipDir) throws Exception {
        if (!zipDir.exists()) {
            zipDir.mkdir();
        }
        String pathSep = getPathSep();
        File zipFile = new File(zipDir.getAbsolutePath(), "src.zip");
        zipFile.createNewFile();
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        List<File> srcFiles = FileUtils.getFilesRecurse(srcDir, ".+\\.java$");
        for (File f : srcFiles) {
            String entryName = f.getAbsolutePath().substring(srcDir.getAbsolutePath().length() + 1);
            entryName = entryName.replace(".", pathSep).replace(pathSep + "java", ".java");
            zos.putNextEntry(new ZipEntry(entryName));
            IOUtils.copy(new FileInputStream(f), zos);
        }
        zos.close();
    }
    
    private String getLineSep() {
        String os = ui.getQueryParameters().getFirst(OS_QUERY);
        if (os == null) {
            return "\r\n";
        }
        return "unix".equals(os) ? "\r" : "\r\n";
    }
    
    protected String getPathSep() {
        String os = ui.getQueryParameters().getFirst(OS_QUERY);
        if (os == null) {
            return "\\";
        }
        return "unix".equals(os) ? "/" : "\\";
    }
    
    private Response getSource(File zipDir) {
        if (zipDir.exists()) {
            File zipFile = new File(zipDir.getAbsolutePath(), "src.zip");
            if (zipFile.exists()) {
                try {
                    return Response.ok().type("application/zip").entity(new FileInputStream(zipFile)).build();
                } catch (FileNotFoundException ex) {
                    // should not happen given we've checked it exists
                    throw new WebApplicationException();
                }
            }
        } 
        return Response.noContent().build();
        
    }
    
    private Response getLink(File zipDir, Message m) {
        if (zipDir.exists() && new File(zipDir.getAbsolutePath(), "src.zip").exists()) {
            UriBuilder builder = ui.getAbsolutePathBuilder();
            String link = builder.queryParam(SOURCE_QUERY).build().toString();
            // TODO : move it into a resource template
            StringBuilder sb = new StringBuilder();
            sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
            sb.append("<head><title>Download the source</title></head>");
            sb.append("<body>");
            sb.append("<h1>Link:</h1><br/>");
            sb.append("<ul>" + "<a href=\"" + link + "\">" + link + "</a>" + "</ul>");
            sb.append("</body>");
            sb.append("</html>");
            m.getExchange().put(JAXRSUtils.IGNORE_MESSAGE_WRITERS, true);
            return Response.ok().type("application/xhtml+xml").entity(
                sb.toString()).build();
        }
        return null;
    }
    
    public void removeCode(ClassResourceInfo cri) {
        removeCode(new File(TMPDIR, getStem(cri, "src")));
        removeCode(new File(TMPDIR, getStem(cri, "zip")));
    }
    
    protected String getStem(ClassResourceInfo cri, String suffix) {
        if (cri == null) {
            return "cxf-jaxrs-" + suffix;
        } else {
            return "cxf-jaxrs-" + cri.getServiceClass().getName() + "-" + suffix; 
        }
    }
    
    private static void removeCode(File src) {
        if (src.exists()) {
            FileUtils.removeDir(src);
        }
    }
    
    protected String getWadl(Message m, ClassResourceInfo resourceClass) {
        m.put(Message.QUERY_STRING, WadlGenerator.WADL_QUERY);
        
        List<ProviderInfo<RequestHandler>> shs = ProviderFactory.getInstance(m).getRequestHandlers();
        // this is actually being tested by ProviderFactory unit tests but just in case
        // WadlGenerator, the custom or default one, must be the first one
        if (shs.size() > 0 && shs.get(0).getProvider() instanceof WadlGenerator) {
            WadlGenerator wg = (WadlGenerator)shs.get(0).getProvider();
            wg = new WadlGenerator(wg);
            wg.setAddResourceAndMethodIds(true);
            Response r = wg.handleRequest(m, resourceClass);
            return r == null ? null : (String)r.getEntity();
        }
        return null;
    }

    protected void generateSource(String wadl, File srcDir, String codeType) {
        Element appElement = readWadl(wadl);
        
        Set<String> typeClassNames = new HashSet<String>();
        List<Element> schemaElements = getSchemaElements(appElement);
        if (!schemaElements.isEmpty()) {
            // generate classes from schema
            JCodeModel codeModel = createCodeModel(schemaElements, typeClassNames);
            if (codeModel != null) {
                generateClassesFromSchema(codeModel, srcDir);
            }
        }
        
        if (!CODE_TYPE_GRAMMAR.equals(codeType)) {
            generateResourceClasses(appElement, schemaElements, typeClassNames, srcDir);
        }
    }
    
    private void generateResourceClasses(Element appElement, List<Element> schemaElements, 
                                         Set<String> typeClassNames, File src) {
        List<Element> resourcesEls = DOMUtils.getChildrenWithName(appElement, 
            WadlGenerator.WADL_NS, "resources");
        if (resourcesEls.size() != 1) {
            throw new IllegalStateException("WADL resources element is missing");
        }
        
        List<Element> resourceEls = DOMUtils.getChildrenWithName(resourcesEls.get(0), 
            WadlGenerator.WADL_NS, "resource");
        if (resourceEls.size() == 0) {
            throw new IllegalStateException("WADL has no resource elements");
        }
        
        GrammarInfo gInfo = getGrammarInfo(appElement, schemaElements);
        for (Element resource : resourceEls) {
            writeResourceClass(resource, typeClassNames, gInfo, src);
        }
        
        generateMainClass(resourcesEls.get(0), src);
        
    }
    
    private GrammarInfo getGrammarInfo(Element appElement, List<Element> schemaElements) {
        
        if (schemaElements.isEmpty()) {
            return null;
        }
        
        Map<String, String> nsMap = new HashMap<String, String>();
        NamedNodeMap attrMap = appElement.getAttributes();
        for (int i = 0; i < attrMap.getLength(); i++) {
            Node node = attrMap.item(i);
            String nodeName = node.getNodeName();
            if (nodeName.startsWith("xmlns:")) {
                String nsValue = node.getNodeValue();
                nsMap.put(nodeName.substring(6), nsValue);
            }
        }
        Map<String, String> elementTypeMap = new HashMap<String, String>();
        for (Element schemaEl : schemaElements) {
            List<Element> elementEls = DOMUtils.getChildrenWithName(schemaEl, 
                 XmlSchemaConstants.XSD_NAMESPACE_URI, "element");
            for (Element el : elementEls) {
                String type = el.getAttribute("type");
                if (type.length() > 0) {
                    String[] pair = type.split(":");
                    elementTypeMap.put(el.getAttribute("name"), pair.length == 1 ? pair[0] : pair[1]);
                }
            }
        }
        return new GrammarInfo(nsMap, elementTypeMap);
    }
    
    public void generateMainClass(Element resourcesEl, File src) {
        
    }
    
    private void writeResourceClass(Element rElement, Set<String> typeClassNames, 
                                    GrammarInfo gInfo, File src) {
        String resourceId = rElement.getAttribute("id");
        String path = rElement.getAttribute("path");
        if (resourceId.length() == 0) {
            LOG.warning("Resource with path " + path + " can not be mapped to a class");
            return;
        }
        
        
        QName qname = JAXRSUtils.convertStringToQName(resourceId);
        if (getSchemaClassName(PackageUtils.getPackageNameByNameSpaceURI(qname.getNamespaceURI()), 
                               gInfo, qname.getLocalPart(), typeClassNames) != null) {
            return; 
        }
        
        StringBuilder sbImports = new StringBuilder();
        StringBuilder sbCode = new StringBuilder();
        Set<String> imports = createImports();
        
        sbImports.append(getClassComment()).append(getLineSep());
        sbImports.append("package " + qname.getNamespaceURI())
            .append(";").append(getLineSep()).append(getLineSep());
        
        writeAnnotation(sbCode, imports, Path.class, path, true, false);
        sbCode.append("public " + getClassType() + " " + qname.getLocalPart() 
                                       + " {" + getLineSep() + getLineSep());
        
        writeMethods(rElement, imports, sbCode, typeClassNames, gInfo);
        
        List<Element> childEls = DOMUtils.getChildrenWithName(rElement, 
            WadlGenerator.WADL_NS, "resource");
        for (Element childEl : childEls) {
            if (childEl.getAttribute("id").length() == 0) {
                writeMethods(childEl, imports, sbCode, typeClassNames, gInfo);
            } else {
                writeResourceMethod(childEl, childEl, imports, sbCode, typeClassNames, gInfo);
            }
        }
        sbCode.append("}");
        writeImports(sbImports, imports);
        
        createJavaSourceFile(src, qname, sbCode, sbImports);
        
        for (Element subEl : childEls) {
            String id = subEl.getAttribute("id");
            if (id.length() > 0 && !resourceId.equals(id) && !id.startsWith("{java")) {
                writeResourceClass(subEl, typeClassNames, gInfo, src);
            }
        }
    }
    
    private String getClassType() {
        return generateInterfaces ? "interface" : "class";
    }
    
    private String getClassComment() {
        return "/**"
            + getLineSep() + " * Generated by Apache CXF"
            + getLineSep() + "**/";
    }
    
    private void writeMethods(Element rElement,  
                              Set<String> imports, StringBuilder sbCode, 
                              Set<String> typeClassNames, GrammarInfo gInfo) {
        List<Element> methodEls = DOMUtils.getChildrenWithName(rElement, 
            WadlGenerator.WADL_NS, "method");
       
        for (Element methodEl : methodEls) {
            writeResourceMethod(rElement, methodEl, imports, sbCode, typeClassNames, gInfo);    
        }
    }
    
    private void writeAnnotation(StringBuilder sbCode, Set<String> imports,
                                 Class<?> cls, String value, boolean nextLine, boolean addTab) {
        if (value != null && value.length() == 0) {
            return;
        }
        addImport(imports, cls.getName());
        sbCode.append("@").append(cls.getSimpleName());
        if (value != null) {
            sbCode.append("(\"" + value + "\")");
        }
        if (nextLine) {
            sbCode.append(getLineSep());
            if (addTab) {
                sbCode.append(TAB);
            }
        }
    }
    
    private void addImport(Set<String> imports, String clsName) {
        if (imports == null || clsName.startsWith("java.lang")) {
            return;
        }
        if (!imports.contains(clsName)) {
            imports.add(clsName);
        }
    }
    
    private void writeImports(StringBuilder sbImports, Set<String> imports) {
        for (String clsName : imports) {
            sbImports.append("import " + clsName).append(";").append(getLineSep());
        }
    }
    
    private void writeResourceMethod(Element resourceEl, Element methodEl, 
                                     Set<String> imports, StringBuilder sbCode, 
                                     Set<String> typeClassNames, GrammarInfo gInfo) {
        String id = methodEl.getAttribute("id");
        String methodName = methodEl.getAttribute("name");
        String path = resourceEl.getAttribute("path");
        if (id.length() == 0) {
            LOG.warning("Method with path " + path + " can not be mapped to a class method");
            return;
        }
        
        sbCode.append(TAB);
        writeAnnotation(sbCode, imports, Path.class, path, true, true);
        if (methodName.length() > 0) {
            if (HTTP_METHOD_ANNOTATIONS.containsKey(methodName.toLowerCase())) {
                writeAnnotation(sbCode, imports, 
                                HTTP_METHOD_ANNOTATIONS.get(methodName.toLowerCase()), null, true, true);
            } else {
                // TODO : write a custom annotation class based on HttpMethod    
            }
        }
        
        List<Element> responseEls = DOMUtils.getChildrenWithName(methodEl, 
                                                                 WadlGenerator.WADL_NS, "response");
        List<Element> requestEls = DOMUtils.getChildrenWithName(methodEl, 
                                                                WadlGenerator.WADL_NS, "request");
        
        if (methodName.length() > 0) {
            writeFormatAnnotations(requestEls, sbCode, imports, true);
            writeFormatAnnotations(responseEls, sbCode, imports, false);
        }
        if (!generateInterfaces) {
            sbCode.append("public ");
        }
        boolean responseTypeAvailable = true;
        if (methodName.length() > 0) {
            responseTypeAvailable = writeResponseType(responseEls, sbCode, imports, typeClassNames, gInfo);
            sbCode.append(id);
        } else {
            QName qname = JAXRSUtils.convertStringToQName(id);
            String packageName = PackageUtils.getPackageNameByNameSpaceURI(qname.getNamespaceURI());
            String clsSimpleName = getSchemaClassName(packageName, gInfo, qname.getLocalPart(), 
                                                      typeClassNames);
            String localName = clsSimpleName == null ? qname.getLocalPart() 
                : clsSimpleName.substring(packageName.length() + 1);
            String parentId = ((Element)resourceEl.getParentNode()).getAttribute("id");
            writeSubResponseType(id.equals(parentId), clsSimpleName == null ? qname.getNamespaceURI() 
                : clsSimpleName.substring(0, packageName.length()), localName, sbCode, imports);
            // TODO : we need to take care of multiple subresource locators with diff @Path
            // returning the same type; also we might have ids like "{org.apache.cxf}Book#getName" 
            sbCode.append("get" + localName);
        }
        
        sbCode.append("(");
        List<Element> inParamElements = new LinkedList<Element>();
        inParamElements.addAll(DOMUtils.getChildrenWithName(resourceEl, 
                                                            WadlGenerator.WADL_NS, "param"));
        boolean form = false;
        if (requestEls.size() == 1 && inParamElements.size() == 0) {
            inParamElements.addAll(DOMUtils.getChildrenWithName(requestEls.get(0), 
                 WadlGenerator.WADL_NS, "param"));
            addFormParameters(inParamElements, requestEls.get(0));
            form = true;
        }
        writeRequestTypes(requestEls, inParamElements, sbCode, imports, typeClassNames, gInfo,
                          form);
        sbCode.append(")");
        if (generateInterfaces) {
            sbCode.append(";");
        } else {
            generateEmptyMethodBody(sbCode, responseTypeAvailable);
        }
        sbCode.append(getLineSep()).append(getLineSep());
    }

    protected void generateEmptyMethodBody(StringBuilder sbCode, boolean responseTypeAvailable) {
        sbCode.append(" {");
        sbCode.append(getLineSep()).append(TAB).append(TAB);
        sbCode.append("//TODO: implement").append(getLineSep()).append(TAB);
        if (responseTypeAvailable) {
            sbCode.append(TAB).append("return null;").append(getLineSep()).append(TAB);
        }
        sbCode.append("}");
    }
    
    private void addFormParameters(List<Element> inParamElements, Element requestEl) {
        List<Element> repElements = 
            DOMUtils.getChildrenWithName(requestEl, WadlGenerator.WADL_NS, "representation");
 
        if (repElements.size() == 1) {
            String mediaType = repElements.get(0).getAttribute("mediaType");
            if (MediaType.APPLICATION_FORM_URLENCODED.equals(mediaType)) { 
                inParamElements.addAll(DOMUtils.getChildrenWithName(repElements.get(0), 
                                                                WadlGenerator.WADL_NS, "param"));
            }
        }
    }
    
    private boolean writeResponseType(List<Element> responseEls, StringBuilder sbCode,
                                   Set<String> imports, Set<String> typeClassNames, 
                                   GrammarInfo gInfo) {
        List<Element> repElements = responseEls.size() == 1 
            ? DOMUtils.getChildrenWithName(responseEls.get(0), WadlGenerator.WADL_NS, "representation")
            : CastUtils.cast(Collections.emptyList(), Element.class);
        if (repElements.size() == 0) {    
            sbCode.append("void ");
            return false;
        }
        String elementName = getElementRefName(repElements, typeClassNames, gInfo, imports);
        if (elementName != null) {
            sbCode.append(elementName + " ");
        } else {
            addImport(imports, Response.class.getName());
            sbCode.append("Response ");
        }
        return true;
    }
    
    private void writeSubResponseType(boolean recursive, String ns, String localName,
                                      StringBuilder sbCode, Set<String> imports) {
        if (!recursive && ns.length() > 0) {
            addImport(imports, ns + "." + localName);
        }
        sbCode.append(localName).append(" ");
    }
    
    private void writeRequestTypes(List<Element> requestEls,
                                   List<Element> inParamEls, 
                                   StringBuilder sbCode, 
                                   Set<String> imports, 
                                   Set<String> typeClassNames, 
                                   GrammarInfo gInfo,
                                   boolean form) {
        
        String elementName = null;
        
        List<Element> repElements = requestEls.size() == 1 
            ? DOMUtils.getChildrenWithName(requestEls.get(0), WadlGenerator.WADL_NS, "representation")
            : CastUtils.cast(Collections.emptyList(), Element.class);
        if (repElements.size() > 0) {    
            elementName = getElementRefName(repElements, typeClassNames, gInfo, imports);
        }
        if (elementName != null) {
            sbCode.append(elementName).append(" ").append(elementName.toLowerCase());
            if (inParamEls.size() > 0) {
                sbCode.append(", ");
            }
        } else if (inParamEls.size() == 0) {
            if (form) {
                addImport(imports, MultivaluedMap.class.getName());
                sbCode.append("MultivaluedMap map");
            }
            return;
        }
        for (int i = 0; i < inParamEls.size(); i++) {
            Element paramEl = inParamEls.get(i);

            String name = paramEl.getAttribute("name");
            Class<?> paramAnnotation = form ? FormParam.class 
                : PARAM_ANNOTATIONS.get(paramEl.getAttribute("style"));
            writeAnnotation(sbCode, imports, paramAnnotation, name, false, false);
            String type = getPrimitiveType(paramEl);
            sbCode.append(" ").append(type).append(" ").append(name.replace('.', '_'));
            if (i + 1 < inParamEls.size()) {
                sbCode.append(", ");
                if (i + 1 >= 4 && ((i + 1) % 4) == 0) {
                    sbCode.append(getLineSep()).append(TAB).append(TAB).append(TAB).append(TAB);
                }
            }
        }
    }
    
    private String getPrimitiveType(Element paramEl) {
        String type = paramEl.getAttribute("type");
        if (type == null) {
            return "String";
        }
        String[] pair = type.split(":");
        String value = pair.length == 2 ? pair[1] : type;
        return "string".equals(value) ? "String" : value;
    }
    
    private String getElementRefName(List<Element> repElements, Set<String> typeClassNames,
                                     GrammarInfo gInfo, Set<String> imports) {
        String elementRef = null;
        for (Element el : repElements) {
            String value = el.getAttribute("element");
            if (value.length() > 0) {
                elementRef = value;
                break;
            }
        }
        if (elementRef != null) {
            String[] pair = elementRef.split(":");
            if (pair.length == 2) {
                String namespace = gInfo != null ? gInfo.getNsMap().get(pair[0]) : null;
                if (namespace == null) {
                    return null;
                }
                String packageName = PackageUtils.getPackageNameByNameSpaceURI(namespace);
                String clsName = getSchemaClassName(packageName, gInfo, pair[1], typeClassNames);
                if (clsName != null) {
                    addImport(imports, clsName);
                    return clsName.substring(packageName.length() + 1);
                }
            }
        }
        return null;
    }
    
    private String getSchemaClassName(String packageName, GrammarInfo gInfo, String localName,
                                      Set <String> typeClassNames) {
        String clsName = matchClassName(typeClassNames, packageName, localName);
        if (clsName == null && gInfo != null) {
            clsName = matchClassName(typeClassNames, packageName, 
                                   gInfo.getElementTypeMap().get(localName));
        }
        return clsName;
    }
    
    private String matchClassName(Set<String> typeClassNames, String packageName, String localName) {
        if (localName == null) {
            return null;
        }
        String clsName = packageName + "." + localName;
        for (String type : typeClassNames) {
            if (type.toLowerCase().equals(clsName)) {
                return type;
            }
        }
        return null;
    }
    
    
    
    private void writeFormatAnnotations(List<Element> parentEls, StringBuilder sbCode, 
                                        Set<String> imports, boolean inRep) {
        List<Element> repElements = parentEls.size() == 1 
            ? DOMUtils.getChildrenWithName(parentEls.get(0), WadlGenerator.WADL_NS, "representation")
            : CastUtils.cast(Collections.emptyList(), Element.class);
        if (repElements.size() == 0) {    
            return;
        }
        Class<?> cls = inRep ? Consumes.class : Produces.class;
        addImport(imports, cls.getName());
        sbCode.append("@").append(cls.getSimpleName()).append("(");
        if (repElements.size() > 1) {
            sbCode.append("{");
        }
        for (int i = 0; i < repElements.size(); i++) {
            String mediaType = repElements.get(i).getAttribute("mediaType");
            if (mediaType != null) {
                sbCode.append("\"" + mediaType + "\"");
                if (i + 1 < repElements.size()) { 
                    sbCode.append(", ");
                }
            }
        }
        if (repElements.size() > 1) {
            sbCode.append(" }");
        }
        sbCode.append(")");
        sbCode.append(getLineSep()).append(TAB);
    }
    
    private void createJavaSourceFile(File src, QName qname, StringBuilder sbCode, StringBuilder sbImports) {
        String content = sbImports.toString() + getLineSep() + sbCode.toString();
        File currentDir = new File(src.getAbsolutePath(), qname.getNamespaceURI());
        currentDir.mkdirs();
        File file = new File(currentDir.getAbsolutePath(), qname.getLocalPart() + ".java");
        
        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.close();
        } catch (FileNotFoundException ex) {
            LOG.warning(file.getAbsolutePath() + " is not found");
        } catch (IOException ex) {
            LOG.warning("Problem writing into " + file.getAbsolutePath());
        }
    }
    
    private Element readWadl(String wadl) {
        try {
            Document doc = DOMUtils.readXml(new StringReader(wadl));
            return doc.getDocumentElement();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read wadl", ex);
        }
    }
    
    private void generateClassesFromSchema(JCodeModel codeModel, File src) {
        try {
            Object writer = JAXBUtils.createFileCodeWriter(src);
            codeModel.build(writer);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to write generated Java files for schemas: "
                                            + e.getMessage(), e);
        }
    }

    private List<Element> getSchemaElements(Element appElement) {
        List<Element> grammarEls = DOMUtils.getChildrenWithName(appElement, 
                                                                WadlGenerator.WADL_NS, "grammars");
        if (grammarEls.size() != 1) {
            return null;
        }
        
        List<Element> schemasEls = DOMUtils.getChildrenWithName(grammarEls.get(0), 
             XmlSchemaConstants.XSD_NAMESPACE_URI, "schema");
        //TODO : check remote referencs if size() == 0
        return schemasEls;
    }
    
    private JCodeModel createCodeModel(List<Element> schemaElements, Set<String> type) {
        
        SchemaCompiler compiler = createCompiler(type);

        addSchemas(schemaElements, compiler);
        
        S2JJAXBModel intermediateModel = compiler.bind();
        
        Object elForRun = ReflectionInvokationHandler
            .createProxyWrapper(new InnerErrorListener(),
                            JAXBUtils.getParamClass(compiler, "setErrorListener"));
        
        JCodeModel codeModel = intermediateModel.generateCode(null, elForRun);
        JAXBUtils.logGeneratedClassNames(LOG, codeModel);
        return codeModel;
    }
    
    private SchemaCompiler createCompiler(Set<String> typeClassNames) {
        return JAXBUtils.createSchemaCompilerWithDefaultAllocator(typeClassNames);
    }
    
    private void addSchemas(List<Element> schemaElements, SchemaCompiler compiler) {
        
        for (int i = 0; i < schemaElements.size(); i++) {
            String key = Integer.toString(i);
            //For JAXB 2.1.8
            InputSource is = new InputSource((InputStream)null);
            is.setSystemId(key);
            is.setPublicId(key);
            compiler.getOptions().addGrammar(is);
    
            compiler.parseSchema(key, schemaElements.get(i));
        }
    }
    
    public void setImportsComparator(Comparator<String> importsComparator) {
        this.importsComparator = importsComparator;
    }

    private Set<String> createImports() {
        return importsComparator == null ? new TreeSet<String>() : new TreeSet<String>(importsComparator);
    }

    public void setGenerateInterfaces(boolean generateInterfaces) {
        this.generateInterfaces = generateInterfaces;
    }
    
    private static class GrammarInfo {
        private Map<String, String> nsMap;
        private Map<String, String> elementTypeMap;
        
        public GrammarInfo(Map<String, String> nsMap, Map<String, String> elementTypeMap) {
            this.nsMap = nsMap;
            this.elementTypeMap = elementTypeMap; 
        }

        public Map<String, String> getNsMap() {
            return nsMap;
        }
        
        public Map<String, String> getElementTypeMap() {
            return elementTypeMap;
        }
    }

    static class InnerErrorListener {

        public void error(SAXParseException ex) {
            throw new RuntimeException("Error compiling schema from WADL : "
                                       + ex.getMessage(), ex);
        }

        public void fatalError(SAXParseException ex) {
            throw new RuntimeException("Fatal error compiling schema from WADL : "
                                       + ex.getMessage(), ex);
        }

        public void info(SAXParseException ex) {
            // ignore
        }

        public void warning(SAXParseException ex) {
            // ignore
        }
    }
}
