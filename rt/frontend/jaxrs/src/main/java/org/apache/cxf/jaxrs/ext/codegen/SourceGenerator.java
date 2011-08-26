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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import org.apache.cxf.Bus;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.catalog.OASISCatalogManagerHelper;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.jaxb.JAXBUtils.JCodeModel;
import org.apache.cxf.jaxb.JAXBUtils.S2JJAXBModel;
import org.apache.cxf.jaxb.JAXBUtils.SchemaCompiler;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * TODO: This will need to be moved into a separate module
 */
public class SourceGenerator {
    public static final String CODE_TYPE_GRAMMAR = "grammar";
    public static final String CODE_TYPE_PROXY = "proxy";
    public static final String CODE_TYPE_WEB = "web";
    public static final String LINE_SEP_PROPERTY = "line.separator";
    public static final String FILE_SEP_PROPERTY = "file.separator";
    
    private static final Logger LOG = LogUtils.getL7dLogger(SourceGenerator.class);
    
    private static final String DEFAULT_PACKAGE_NAME = "application";
    private static final String DEFAULT_RESOURCE_NAME = "Resource";
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
        HTTP_METHOD_ANNOTATIONS.put("options", OPTIONS.class);
        
        PARAM_ANNOTATIONS = new HashMap<String, Class<?>>();
        PARAM_ANNOTATIONS.put("template", PathParam.class);
        PARAM_ANNOTATIONS.put("header", HeaderParam.class);
        PARAM_ANNOTATIONS.put("query", QueryParam.class);
        PARAM_ANNOTATIONS.put("matrix", MatrixParam.class);
    }

    private Comparator<String> importsComparator;
    private boolean generateInterfaces = true;
    private boolean generateImpl;
    private String resourcePackageName;
    private String resourceName;
    private String wadlPath;
    
    private Map<String, String> properties; 
    
    private List<String> generatedServiceClasses = new ArrayList<String>(); 
    private List<String> generatedTypeClasses = new ArrayList<String>();
    private List<InputSource> bindingFiles = Collections.emptyList();
    private List<InputSource> schemaPackageFiles = Collections.emptyList();
    private Map<String, String> schemaPackageMap = Collections.emptyMap();
    private Bus bus;
    
    public SourceGenerator() {
        this(Collections.<String, String>emptyMap());
    }
    
    public SourceGenerator(Map<String, String> properties) {
        this.properties = properties;
    }
    
    private String getClassPackageName(String wadlPackageName) {
        if (resourcePackageName != null) {
            return resourcePackageName;
        } else if (wadlPackageName != null && wadlPackageName.length() > 0) {
            return wadlPackageName;
        } else {
            return DEFAULT_PACKAGE_NAME;
        }
    }
    
    private String getLineSep() {
        String value = properties.get(LINE_SEP_PROPERTY);
        return value == null ? System.getProperty(LINE_SEP_PROPERTY) : value;
    }
    
    private String getFileSep() {
        String value = properties.get(FILE_SEP_PROPERTY);
        return value == null ? System.getProperty(FILE_SEP_PROPERTY) : value;
    }
    
    public void generateSource(String wadl, File srcDir, String codeType) {
        Application app = readWadl(wadl, wadlPath);
        
        Set<String> typeClassNames = new HashSet<String>();
        GrammarInfo gInfo = generateSchemaCodeAndInfo(app, typeClassNames, srcDir);
        if (!CODE_TYPE_GRAMMAR.equals(codeType)) {
            generateResourceClasses(app, gInfo, typeClassNames, srcDir);
        }
    }
    
    private GrammarInfo generateSchemaCodeAndInfo(Application app, Set<String> typeClassNames, 
                                                  File srcDir) {
        List<SchemaInfo> schemaElements = getSchemaElements(app);
        if (schemaElements != null && !schemaElements.isEmpty()) {
            // generate classes from schema
            JCodeModel codeModel = createCodeModel(schemaElements, typeClassNames);
            if (codeModel != null) {
                generateClassesFromSchema(codeModel, srcDir);
            }
        }
        return getGrammarInfo(app.getAppElement(), schemaElements);
    }
    
    private void generateResourceClasses(Application app, GrammarInfo gInfo, 
                                         Set<String> typeClassNames, File src) {
        Element appElement = app.getAppElement();
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
        
        for (int i = 0; i < resourceEls.size(); i++) {
            Element resource = getResourceElement(app, resourceEls.get(i), gInfo, typeClassNames, 
                                                  resourceEls.get(i).getAttribute("type"), src);
            writeResourceClass(app, resource, typeClassNames, gInfo, src, true, generateInterfaces);
            if (generateInterfaces && generateImpl) {
                writeResourceClass(app, resource, typeClassNames, gInfo, src, true, false);
            }
            if (resourceName != null) {
                break;
            }
        }
        
        generateMainClass(resourcesEls.get(0), src);
        
    }
    
    //TODO: similar procedure should work for representation, method and param
    // thus some of the code here will need to be moved into a sep function to be
    // reused by relevant handlers
    private Element getResourceElement(Application app, Element resElement,
                                       GrammarInfo gInfo, Set<String> typeClassNames,
                                       String type, File srcDir) {
        if (type.length() > 0) {
            if (type.startsWith("#")) {
                String refId = type.substring(1);
                List<Element> resourceTypes = 
                    DOMUtils.getChildrenWithName(app.getAppElement(), WadlGenerator.WADL_NS, 
                                                 "resource_type");
                for (Element resourceType : resourceTypes) {
                    if (refId.equals(resourceType.getAttribute("id"))) {
                        Element realElement = (Element)resourceType.cloneNode(true);
                        DOMUtils.setAttribute(realElement, "id", resElement.getAttribute("id"));
                        DOMUtils.setAttribute(realElement, "path", resElement.getAttribute("path"));
                        return realElement;
                    }
                }
            } else {
                URI wadlRef = URI.create(type);
                String wadlRefPath = app.getWadlPath() != null 
                    ? getBaseWadlPath(app.getWadlPath()) + wadlRef.getPath() : wadlRef.getPath();
                Application refApp = new Application(readIncludedDocument(wadlRefPath),
                                                     wadlRefPath);
                GrammarInfo gInfoBase = generateSchemaCodeAndInfo(refApp, typeClassNames, srcDir);
                if (gInfoBase != null) {
                    gInfo.getElementTypeMap().putAll(gInfoBase.getElementTypeMap());
                    gInfo.getNsMap().putAll(gInfo.getNsMap());
                }
                return getResourceElement(refApp, resElement, gInfo, typeClassNames, 
                                          "#" + wadlRef.getFragment(), srcDir);
            }
        } 
        return resElement;     
        
    }
    
    private GrammarInfo getGrammarInfo(Element appElement, List<SchemaInfo> schemaElements) {
        
        if (schemaElements == null || schemaElements.isEmpty()) {
            return new GrammarInfo();
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
        for (SchemaInfo schemaEl : schemaElements) {
            List<Element> elementEls = DOMUtils.getChildrenWithName(schemaEl.getElement(), 
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
    
    private void writeResourceClass(Application app, Element rElement, Set<String> typeClassNames, 
                                    GrammarInfo gInfo, File src, boolean isRoot,
                                    boolean interfaceIsGenerated) {
        String resourceId = resourceName != null 
            ? resourceName : rElement.getAttribute("id");
        if (resourceId.length() == 0) {
            resourceId = DEFAULT_RESOURCE_NAME;
        }
        boolean expandedQName = resourceId.startsWith("{") ? true : false;
        QName qname = convertToQName(resourceId, expandedQName);
        String namespaceURI = possiblyConvertNamespaceURI(qname.getNamespaceURI(), expandedQName);
        
        if (getSchemaClassName(namespaceURI, gInfo, qname.getLocalPart(), typeClassNames) != null) {
            return; 
        }
        
        StringBuilder sbImports = new StringBuilder();
        StringBuilder sbCode = new StringBuilder();
        Set<String> imports = createImports();
        
        final String classPackage = getClassPackageName(namespaceURI);
        final String className = getClassName(qname.getLocalPart(), interfaceIsGenerated);
        
        sbImports.append(getClassComment()).append(getLineSep());
        sbImports.append("package " + classPackage)
            .append(";").append(getLineSep()).append(getLineSep());
        
        if (isRoot && writeAnnotations(interfaceIsGenerated)) {
            String path = rElement.getAttribute("path");
            writeAnnotation(sbCode, imports, Path.class, path, true, false);
        }
                
        sbCode.append("public " + getClassType(interfaceIsGenerated) + " " + className);
        writeImplementsInterface(sbCode, qname.getLocalPart(), interfaceIsGenerated);              
        sbCode.append(" {" + getLineSep() + getLineSep());
        
        writeMethods(rElement, imports, sbCode, typeClassNames, gInfo, isRoot, interfaceIsGenerated);
        
        List<Element> childEls = DOMUtils.getChildrenWithName(rElement, 
            WadlGenerator.WADL_NS, "resource");
        for (Element childEl : childEls) {
            if (childEl.getAttribute("id").length() == 0) {
                writeMethods(childEl, imports, sbCode, typeClassNames, gInfo, false, interfaceIsGenerated);
            } else {
                writeResourceMethod(childEl, imports, sbCode, typeClassNames, gInfo, 
                                    false, interfaceIsGenerated);
            }
        }
        sbCode.append("}");
        writeImports(sbImports, imports, classPackage);
        
        createJavaSourceFile(src, new QName(classPackage, className), sbCode, sbImports);
        
        for (Element subEl : childEls) {
            String id = subEl.getAttribute("id");
            if (id.length() > 0 && !resourceId.equals(id) && !id.startsWith("{java")
                && !id.startsWith("java")) {
                writeResourceClass(app, 
                                   getResourceElement(app, subEl, gInfo, typeClassNames, 
                                                      subEl.getAttribute("type"), src), 
                                   typeClassNames, gInfo, src, false, interfaceIsGenerated);
            }
        }
    }
    
    private QName convertToQName(String resourceId, boolean expandedQName) {
        QName qname = null;
        if (expandedQName) {
            qname = JAXRSUtils.convertStringToQName(resourceId);
        } else {
            int lastIndex = resourceId.lastIndexOf(".");
            qname = lastIndex == -1 ? new QName(resourceId) 
                                    : new QName(resourceId.substring(0, lastIndex),
                                                resourceId.substring(lastIndex + 1));
        }
        return qname;
    }
    
    private String getClassType(boolean interfaceIsGenerated) {
        return interfaceIsGenerated ? "interface" : "class";
    }
    
    private String getClassName(String clsName, boolean interfaceIsGenerated) {
        if (interfaceIsGenerated) {
            return clsName;
        } else {
            return generateInterfaces ? clsName + "Impl" : clsName;
        }
        
    }
    
    private boolean writeAnnotations(boolean interfaceIsGenerated) {
        if (interfaceIsGenerated) {
            return true;
        } else {
            return !generateInterfaces && generateImpl;
        }
    }
    
    private void writeImplementsInterface(StringBuilder sb, String clsName, 
                                             boolean interfaceIsGenerated) {
        if (generateInterfaces && !interfaceIsGenerated) {
            sb.append(" implements " + clsName);
        }
    }
    
    private String getClassComment() {
        return "/**"
            + getLineSep() + " * Created by Apache CXF WadlToJava code generator"
            + getLineSep() + "**/";
    }
    
    private void writeMethods(Element rElement,  
                              Set<String> imports, StringBuilder sbCode, 
                              Set<String> typeClassNames, GrammarInfo gInfo,
                              boolean isRoot,
                              boolean interfaceIsGenerated) {
        List<Element> methodEls = DOMUtils.getChildrenWithName(rElement, 
            WadlGenerator.WADL_NS, "method");
       
        for (Element methodEl : methodEls) {
            writeResourceMethod(methodEl, imports, sbCode, typeClassNames, gInfo, 
                                isRoot, interfaceIsGenerated);    
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
    
    private void writeImports(StringBuilder sbImports, Set<String> imports, String classPackage) {
        for (String clsName : imports) {
            int index = clsName.lastIndexOf(".");
            if (index != -1 && clsName.substring(0, index).equals(classPackage)) {
                continue;
            }
            sbImports.append("import " + clsName).append(";").append(getLineSep());
        }
    }
    
    private void writeResourceMethod(Element methodEl, 
                                     Set<String> imports, StringBuilder sbCode, 
                                     Set<String> typeClassNames, GrammarInfo gInfo,
                                     boolean isRoot,
                                     boolean interfaceIsGenerated) {
        Element resourceEl = "resource".equals(methodEl.getLocalName()) 
            ? methodEl : (Element)methodEl.getParentNode();
        
        String methodName = methodEl.getAttribute("name");
        String methodNameLowerCase = methodName.toLowerCase();
        String id = methodEl.getAttribute("id");
        if (id.length() == 0) {
            id = methodNameLowerCase;
        }
        
        List<Element> responseEls = DOMUtils.getChildrenWithName(methodEl, 
                                                                 WadlGenerator.WADL_NS, "response");
        List<Element> requestEls = DOMUtils.getChildrenWithName(methodEl, 
                                                                WadlGenerator.WADL_NS, "request");
        
        
        if (writeAnnotations(interfaceIsGenerated)) {
            sbCode.append(TAB);
            
            if (methodNameLowerCase.length() > 0) {
                if (HTTP_METHOD_ANNOTATIONS.containsKey(methodNameLowerCase)) {
                    writeAnnotation(sbCode, imports, 
                                    HTTP_METHOD_ANNOTATIONS.get(methodNameLowerCase), null, true, true);
                } else {
                    // TODO : write a custom annotation class name based on HttpMethod    
                }
                writeFormatAnnotations(requestEls, sbCode, imports, true);
                writeFormatAnnotations(responseEls, sbCode, imports, false);
            }
            if (!isRoot) {
                String path = resourceEl.getAttribute("path");
                writeAnnotation(sbCode, imports, Path.class, path, true, true);
            }
        } else {
            sbCode.append(getLineSep()).append(TAB);
        }
        
        if (!interfaceIsGenerated) {
            sbCode.append("public ");
        }
        boolean responseTypeAvailable = true;
        if (methodNameLowerCase.length() > 0) {
            responseTypeAvailable = writeResponseType(responseEls, sbCode, imports, typeClassNames, gInfo);
            sbCode.append(id);
        } else {
            boolean expandedQName = id.startsWith("{");
            QName qname = convertToQName(id, expandedQName);
            String packageName = possiblyConvertNamespaceURI(qname.getNamespaceURI(), expandedQName);
            
            String clsSimpleName = getSchemaClassName(packageName, gInfo, qname.getLocalPart(), 
                                                      typeClassNames);
            String localName = clsSimpleName == null ? qname.getLocalPart() 
                : clsSimpleName.substring(packageName.length() + 1);
            String subResponseNs = clsSimpleName == null ? getClassPackageName(packageName) 
                : clsSimpleName.substring(0, packageName.length());
            String parentId = ((Element)resourceEl.getParentNode()).getAttribute("id");
            writeSubResponseType(id.equals(parentId), subResponseNs, localName, sbCode, imports);
            
            // TODO : we need to take care of multiple subresource locators with diff @Path
            // returning the same type; also we might have ids like "{org.apache.cxf}Book#getName" 
            
            sbCode.append("get" + localName);
        }
        
        sbCode.append("(");
        List<Element> inParamElements = new LinkedList<Element>();
        inParamElements.addAll(DOMUtils.getChildrenWithName(resourceEl, 
                                                            WadlGenerator.WADL_NS, "param"));
        writeRequestTypes(requestEls, inParamElements, sbCode, imports, typeClassNames, gInfo,
                          interfaceIsGenerated);
        sbCode.append(")");
        if (interfaceIsGenerated) {
            sbCode.append(";");
        } else {
            generateEmptyMethodBody(sbCode, responseTypeAvailable);
        }
        sbCode.append(getLineSep()).append(getLineSep());
    }

    private String possiblyConvertNamespaceURI(String nsURI, boolean expandedQName) {
        return expandedQName ? getPackageFromNamespace(nsURI) : nsURI;
    }
    
    private String getPackageFromNamespace(String nsURI) {
        return schemaPackageMap.containsKey(nsURI) ? schemaPackageMap.get(nsURI)
            : PackageUtils.getPackageNameByNameSpaceURI(nsURI);
    }
    
    private void generateEmptyMethodBody(StringBuilder sbCode, boolean responseTypeAvailable) {
        sbCode.append(" {");
        sbCode.append(getLineSep()).append(TAB).append(TAB);
        sbCode.append("//TODO: implement").append(getLineSep()).append(TAB);
        if (responseTypeAvailable) {
            sbCode.append(TAB).append("return null;").append(getLineSep()).append(TAB);
        }
        sbCode.append("}");
    }
    
    private boolean addFormParameters(List<Element> inParamElements, Element requestEl) {
        List<Element> repElements = 
            DOMUtils.getChildrenWithName(requestEl, WadlGenerator.WADL_NS, "representation");
 
        if (repElements.size() == 1) {
            String mediaType = repElements.get(0).getAttribute("mediaType");
            if (MediaType.APPLICATION_FORM_URLENCODED.equals(mediaType)) { 
                inParamElements.addAll(DOMUtils.getChildrenWithName(repElements.get(0), 
                                                                WadlGenerator.WADL_NS, "param"));
                return true;
            }
        }
        return false;
    }
    
    private boolean writeResponseType(List<Element> responseEls, StringBuilder sbCode,
                                   Set<String> imports, Set<String> typeClassNames, 
                                   GrammarInfo gInfo) {
        List<Element> repElements = null;
        if (responseEls.size() >= 1) {
            Element okResponse = null;
            if (responseEls.size() > 1) {
                for (int i = 0; i < responseEls.size(); i++) {
                    String statusValue = responseEls.get(i).getAttribute("status");
                    try {
                        int status = statusValue.length() == 0 ? 200 : Integer.valueOf(statusValue);
                        if (status == 200) {
                            okResponse = responseEls.get(i);
                            break;
                        }
                    } catch (NumberFormatException ex) {
                        // ignore
                    }
                }
            } else {
                okResponse = responseEls.get(0);
            }
            repElements = DOMUtils.getChildrenWithName(okResponse, WadlGenerator.WADL_NS, "representation");
        } else {
            repElements = CastUtils.cast(Collections.emptyList(), Element.class);
        }
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
                                   boolean interfaceIsGenerated) {
        
        boolean form = false;
        boolean formParamsAvailbale = false;
        if (requestEls.size() == 1 && inParamEls.size() == 0) {
            inParamEls.addAll(DOMUtils.getChildrenWithName(requestEls.get(0), 
                 WadlGenerator.WADL_NS, "param"));
            int currentSize = inParamEls.size();
            form = addFormParameters(inParamEls, requestEls.get(0));
            formParamsAvailbale = currentSize < inParamEls.size(); 
        }
                  
        if (form && !formParamsAvailbale) {
            addImport(imports, MultivaluedMap.class.getName());
            sbCode.append("MultivaluedMap map");
        } 
        for (int i = 0; i < inParamEls.size(); i++) {
    
            Element paramEl = inParamEls.get(i);

            String name = paramEl.getAttribute("name");
            if (writeAnnotations(interfaceIsGenerated)) {
                Class<?> paramAnnotation = form ? FormParam.class 
                    : PARAM_ANNOTATIONS.get(paramEl.getAttribute("style"));
                writeAnnotation(sbCode, imports, paramAnnotation, name, false, false);
                sbCode.append(" ");
            }
            String type = getPrimitiveType(paramEl);
            if (Boolean.valueOf(paramEl.getAttribute("repeating"))) {
                addImport(imports, List.class.getName());
                type = "List<" + type + ">";
            }
            sbCode.append(type).append(" ").append(name.replace('.', '_'));
            if (i + 1 < inParamEls.size()) {
                sbCode.append(", ");
                if (i + 1 >= 4 && ((i + 1) % 4) == 0) {
                    sbCode.append(getLineSep()).append(TAB).append(TAB).append(TAB).append(TAB);
                }
            }
        }
        if (!form) {
            String elementName = null;
            
            List<Element> repElements = requestEls.size() == 1 
                ? DOMUtils.getChildrenWithName(requestEls.get(0), WadlGenerator.WADL_NS, "representation")
                : CastUtils.cast(Collections.emptyList(), Element.class);
            if (repElements.size() > 0) {    
                elementName = getElementRefName(repElements, typeClassNames, gInfo, imports);
            }
            if (elementName != null) {
                if (inParamEls.size() > 0) {
                    sbCode.append(", ");
                }
                sbCode.append(elementName).append(" ").append(elementName.toLowerCase());
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
                String packageName = getPackageFromNamespace(namespace);
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
        String clsName = packageName + "." + localName.toLowerCase();
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
        
        String namespace = qname.getNamespaceURI();
        generatedServiceClasses.add(namespace + "." + qname.getLocalPart());
        
        namespace = namespace.replace(".", getFileSep());
        
        File currentDir = new File(src.getAbsolutePath(), namespace);
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
    
    private Application readWadl(String wadl, String docPath) {
        return new Application(readXmlDocument(new StringReader(wadl)), docPath);
    }
    
    private Element readXmlDocument(Reader reader) {
        try {
            return StaxUtils.read(new InputSource(reader)).getDocumentElement();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read wadl", ex);
        }
    }
    
    private void generateClassesFromSchema(JCodeModel codeModel, File src) {
        try {
            Object writer = JAXBUtils.createFileCodeWriter(src);
            codeModel.build(writer);
            generatedTypeClasses = JAXBUtils.getGeneratedClassNames(codeModel);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to write generated Java files for schemas: "
                                            + e.getMessage(), e);
        }
    }

    private List<SchemaInfo> getSchemaElements(Application app) {
        List<Element> grammarEls = DOMUtils.getChildrenWithName(app.getAppElement(), 
                                                                WadlGenerator.WADL_NS, "grammars");
        if (grammarEls.size() != 1) {
            return null;
        }
        
        List<SchemaInfo> schemas = new ArrayList<SchemaInfo>();
        List<Element> schemasEls = DOMUtils.getChildrenWithName(grammarEls.get(0), 
             XmlSchemaConstants.XSD_NAMESPACE_URI, "schema");
        for (int i = 0; i < schemasEls.size(); i++) {
            String systemId = app.getWadlPath();
            if (schemasEls.size() > 1) {
                systemId += "#grammar" + (i + 1);
            }
            schemas.add(createSchemaInfo(schemasEls.get(i), systemId));
        }
        List<Element> includeEls = DOMUtils.getChildrenWithName(grammarEls.get(0), 
             WadlGenerator.WADL_NS, "include");
        for (Element includeEl : includeEls) {
            String href = includeEl.getAttribute("href");
            
            String schemaURI = resolveLocationWithCatalog(href);
            if (schemaURI == null) {
                schemaURI = app.getWadlPath() != null ? getBaseWadlPath(app.getWadlPath()) + href : href;
            }
            schemas.add(createSchemaInfo(readIncludedDocument(schemaURI),
                                            schemaURI));
        }
        return schemas;
    }
    
    private static String getBaseWadlPath(String docPath) {
        int lastSep = docPath.lastIndexOf("/");
        return lastSep != -1 ? docPath.substring(0, lastSep + 1) : docPath;
    }
    
    private SchemaInfo createSchemaInfo(Element schemaEl, String systemId) { 
        SchemaInfo info = new SchemaInfo(schemaEl.getAttribute("targetNamespace"));
        info.setElement(schemaEl);
        info.setSystemId(systemId);
        return info;
    }
    
    private String resolveLocationWithCatalog(String href) {
        if (bus != null) {
            OASISCatalogManager catalogResolver = OASISCatalogManager.getCatalogManager(bus);
            try {
                return new OASISCatalogManagerHelper().resolve(catalogResolver, 
                                                               href, null);
            } catch (Exception e) {
                throw new RuntimeException("Catalog resolution failed", e);
            }
        } else {
            return null;
        }
    }
    
    private Element readIncludedDocument(String href) {
        
        try {
            InputStream is = null;
            if (!href.startsWith("http")) {
                is = ResourceUtils.getResourceStream(href, bus);
            }
            if (is == null) {
                is = URI.create(href).toURL().openStream();
            }
            return readXmlDocument(new InputStreamReader(is, "UTF-8"));
        } catch (Exception ex) {
            throw new RuntimeException("Resource " + href + " can not be read");
        }
    }
    
    private JCodeModel createCodeModel(List<SchemaInfo> schemaElements, Set<String> type) {
        

        SchemaCompiler compiler = createCompiler(type);
        addSchemas(schemaElements, compiler);
        for (InputSource is : bindingFiles) {
            compiler.getOptions().addBindFile(is);
        }
        
        Object elForRun = ReflectionInvokationHandler
            .createProxyWrapper(new InnerErrorListener(),
                            JAXBUtils.getParamClass(compiler, "setErrorListener"));
        
        compiler.setErrorListener(elForRun);
        S2JJAXBModel intermediateModel = compiler.bind();
        JCodeModel codeModel = intermediateModel.generateCode(null, elForRun);
        JAXBUtils.logGeneratedClassNames(LOG, codeModel);
        return codeModel;
    }

    private SchemaCompiler createCompiler(Set<String> typeClassNames) {
        return JAXBUtils.createSchemaCompilerWithDefaultAllocator(typeClassNames);
    }
    
    private void addSchemas(List<SchemaInfo> schemas, SchemaCompiler compiler) {
        // handle package customizations first
        for (int i = 0; i < schemaPackageFiles.size(); i++) {
            compiler.parseSchema(schemaPackageFiles.get(i));
        }
        
        for (int i = 0; i < schemas.size(); i++) {
            SchemaInfo schema = schemas.get(i);
            
            String key = schema.getSystemId();
            if (key != null) {
                // TODO: CXF code should have a better solution somewhere, we'll get back to it
                // when addressing the issue of retrieving WADLs with included schemas  
                if (key.startsWith("classpath:")) {
                    String resource = key.substring(10);
                    URL url = getClass().getResource(resource);
                    if (url != null) {
                        try {
                            key = url.toURI().toString();
                        } catch (Exception ex) {
                            // won't happen
                        }
                    }
                }
            } else {
                key = Integer.toString(i);
            }
            InputSource is = new InputSource((InputStream)null);
            is.setSystemId(key);
            is.setPublicId(key);
            compiler.getOptions().addGrammar(is);
            compiler.parseSchema(key, schema.getElement());
        }
    }
    
    public void setImportsComparator(Comparator<String> importsComparator) {
        this.importsComparator = importsComparator;
    }

    private Set<String> createImports() {
        return importsComparator == null ? new TreeSet<String>(new DefaultImportsComparator()) 
            : new TreeSet<String>(importsComparator);
    }

    public void setGenerateInterfaces(boolean generateInterfaces) {
        this.generateInterfaces = generateInterfaces;
    }
    
    public void setGenerateImplementation(boolean generate) {
        this.generateImpl = generate;
    }
    
    public void setPackageName(String name) {
        this.resourcePackageName = name;
    }
    
    public void setResourceName(String name) {
        this.resourceName = name;
    }
    
    public void setWadlPath(String name) {
        this.wadlPath = name;
    }
    
    public void setBindingFiles(List<InputSource> files) {
        this.bindingFiles = files;
    }
    
    public void setSchemaPackageFiles(List<InputSource> files) {
        this.schemaPackageFiles = files;
    }

    public void setSchemaPackageMap(Map<String, String> map) {
        this.schemaPackageMap = map;
    }
    
    public void setBus(Bus bus) {
        this.bus = bus;
    }
    
    public List<String> getGeneratedServiceClasses() {
        return generatedServiceClasses;    
    }
    
    public List<String> getGeneratedTypeClasses() {
        return generatedTypeClasses;    
    }
    
    private static class GrammarInfo {
        private Map<String, String> nsMap = new HashMap<String, String>();
        private Map<String, String> elementTypeMap = new HashMap<String, String>();
        
        public GrammarInfo() {
            
        }
        
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

    private static class DefaultImportsComparator implements Comparator<String> {
        private static final String JAVAX_PREFIX = "javax";
        public int compare(String s1, String s2) {
            boolean javax1 = s1.startsWith(JAVAX_PREFIX);
            boolean javax2 = s2.startsWith(JAVAX_PREFIX);
            if (javax1 && !javax2) {
                return -1;
            } else if (!javax1 && javax2) {
                return 1;
            } else { 
                return s1.compareTo(s2);
            }
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
    
    private class Application {
        private Element appElement;
        private String wadlPath;
        public Application(Element appElement, String wadlPath) {
            this.appElement = appElement;
            this.wadlPath = wadlPath;
        }
        
        public Element getAppElement() {
            return appElement;
        }
        
        public String getWadlPath() {
            return wadlPath;
        }
    }
}
