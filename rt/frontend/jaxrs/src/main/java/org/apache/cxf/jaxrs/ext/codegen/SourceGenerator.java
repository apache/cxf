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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
import javax.xml.transform.Source;

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
    
    private static final List<String> HTTP_OK_STATUSES =
        Arrays.asList(new String[] {"200", "201", "202", "203", "204"});
    
    private static final Map<String, Class<?>> HTTP_METHOD_ANNOTATIONS;
    private static final Map<String, Class<?>> PARAM_ANNOTATIONS;
    private static final Set<String> RESOURCE_LEVEL_PARAMS;
    private static final Map<String, String> AUTOBOXED_PRIMITIVES_MAP;
    private static final Map<String, String> XSD_SPECIFIC_TYPE_MAP;
    
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
        
        RESOURCE_LEVEL_PARAMS = new HashSet<String>();
        RESOURCE_LEVEL_PARAMS.add("template");
        RESOURCE_LEVEL_PARAMS.add("matrix");
        
        AUTOBOXED_PRIMITIVES_MAP = new HashMap<String, String>();
        AUTOBOXED_PRIMITIVES_MAP.put(byte.class.getSimpleName(), Byte.class.getSimpleName());
        AUTOBOXED_PRIMITIVES_MAP.put(short.class.getSimpleName(), Short.class.getSimpleName());
        AUTOBOXED_PRIMITIVES_MAP.put(int.class.getSimpleName(), Integer.class.getSimpleName());
        AUTOBOXED_PRIMITIVES_MAP.put(long.class.getSimpleName(), Long.class.getSimpleName());
        AUTOBOXED_PRIMITIVES_MAP.put(float.class.getSimpleName(), Float.class.getSimpleName());
        AUTOBOXED_PRIMITIVES_MAP.put(double.class.getSimpleName(), Double.class.getSimpleName());
        AUTOBOXED_PRIMITIVES_MAP.put(boolean.class.getSimpleName(), Boolean.class.getSimpleName());
        
        XSD_SPECIFIC_TYPE_MAP = new HashMap<String, String>();
        XSD_SPECIFIC_TYPE_MAP.put("string", "String");
        XSD_SPECIFIC_TYPE_MAP.put("decimal", "java.math.BigInteger");
        XSD_SPECIFIC_TYPE_MAP.put("integer", "long");
    }

    private Comparator<String> importsComparator;
    private boolean generateInterfaces = true;
    private boolean generateImpl;
    private String resourcePackageName;
    private String resourceName;
    private String wadlPath;
    private String wadlNamespace = WadlGenerator.WADL_NS;
    private boolean generateEnums;
    
    private Map<String, String> properties; 
    
    private List<String> generatedServiceClasses = new ArrayList<String>(); 
    private List<String> generatedTypeClasses = new ArrayList<String>();
    private List<InputSource> bindingFiles = Collections.emptyList();
    private List<InputSource> schemaPackageFiles = Collections.emptyList();
    private List<String> compilerArgs = new ArrayList<String>();
    private Map<String, String> schemaPackageMap = Collections.emptyMap();
    private Map<String, String> schemaTypesMap = Collections.emptyMap();
    private Bus bus;
    private boolean supportMultipleXmlReps;
    
    public SourceGenerator() {
        this(Collections.<String, String>emptyMap());
    }
    
    public SourceGenerator(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public void setSupportMultipleXmlReps(boolean support) {
        supportMultipleXmlReps = support;
    }
    
    public void setWadlNamespace(String ns) {
        this.wadlNamespace = ns;
    }
    
    public String getWadlNamespace() {
        return wadlNamespace;
    }
    
    public void setGenerateEnums(boolean generate) {
        this.generateEnums = generate;
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
        return getGrammarInfo(app, schemaElements);
    }
    
    private void generateResourceClasses(Application app, GrammarInfo gInfo, 
                                         Set<String> typeClassNames, File src) {
        Element appElement = app.getAppElement();
        List<Element> resourcesEls = getWadlElements(appElement, "resources");
        if (resourcesEls.size() != 1) {
            throw new IllegalStateException("WADL resources element is missing");
        }
        
        List<Element> resourceEls = getWadlElements(resourcesEls.get(0), "resource");
        if (resourceEls.size() == 0) {
            throw new IllegalStateException("WADL has no resource elements");
        }
        
        for (int i = 0; i < resourceEls.size(); i++) {
            Element resource = getResourceElement(app, resourceEls.get(i), gInfo, typeClassNames, 
                                                  resourceEls.get(i).getAttribute("type"), src);
            writeResourceClass(resource, 
                               new ContextInfo(app, src, typeClassNames, gInfo, generateInterfaces), 
                               true);
            if (generateInterfaces && generateImpl) {
                writeResourceClass(resource, 
                                   new ContextInfo(app, src, typeClassNames, gInfo, false), 
                                   true);
            }
            if (resourceName != null) {
                break;
            }
        }
        
        generateMainClass(resourcesEls.get(0), src);
        
    }
    
    private Element getResourceElement(Application app, Element resElement,
                                       GrammarInfo gInfo, Set<String> typeClassNames,
                                       String type, File srcDir) {
        if (type.length() > 0) {
            if (type.startsWith("#")) {
                Element resourceType = resolveLocalReference(app.getAppElement(), "resource_type", type);
                if (resourceType != null) {
                    Element realElement = (Element)resourceType.cloneNode(true);
                    DOMUtils.setAttribute(realElement, "id", resElement.getAttribute("id"));
                    DOMUtils.setAttribute(realElement, "path", resElement.getAttribute("path"));
                    return realElement;
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
                    gInfo.getNsMap().putAll(gInfoBase.getNsMap());
                }
                return getResourceElement(refApp, resElement, gInfo, typeClassNames, 
                                          "#" + wadlRef.getFragment(), srcDir);
            }
        } 
        return resElement;     
        
    }
    
    private Element getWadlElement(Element wadlEl) {
        String href = wadlEl.getAttribute("href");
        if (href.length() > 0 && href.startsWith("#")) {
            return resolveLocalReference(wadlEl.getOwnerDocument().getDocumentElement(), 
                                         wadlEl.getLocalName(), href);
        } else { 
            return wadlEl;
        }
    }
    
    private Element resolveLocalReference(Element appEl, String elementName, String localRef) {
        String refId = localRef.substring(1);
        List<Element> resourceTypes = getWadlElements(appEl, elementName);
        for (Element resourceType : resourceTypes) {
            if (refId.equals(resourceType.getAttribute("id"))) {
                return resourceType;
            }
        }
        return null;
    }
    
    private GrammarInfo getGrammarInfo(Application app, List<SchemaInfo> schemaElements) {
        
        if (schemaElements == null || schemaElements.isEmpty()) {
            return new GrammarInfo();
        }
        
        Map<String, String> nsMap = new HashMap<String, String>();
        NamedNodeMap attrMap = app.getAppElement().getAttributes();
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
            populateElementTypeMap(app, schemaEl.getElement(), schemaEl.getSystemId(), elementTypeMap);
        }
        return new GrammarInfo(nsMap, elementTypeMap);
    }
    
    private void populateElementTypeMap(Application app, Element schemaEl, 
            String systemId, Map<String, String> elementTypeMap) {
        List<Element> elementEls = DOMUtils.getChildrenWithName(schemaEl, 
                XmlSchemaConstants.XSD_NAMESPACE_URI, "element");
        for (Element el : elementEls) {
            String type = el.getAttribute("type");
            if (type.length() > 0) {
                String[] pair = type.split(":");
                elementTypeMap.put(el.getAttribute("name"), pair.length == 1 ? pair[0] : pair[1]);
            }
        }
        Element includeEl = DOMUtils.getFirstChildWithName(schemaEl, 
                XmlSchemaConstants.XSD_NAMESPACE_URI, "include");
        if (includeEl != null) {
            int ind = systemId.lastIndexOf("/");
            if (ind != -1) {
                String schemaURI = systemId.substring(0, ind + 1) + includeEl.getAttribute("schemaLocation");
                populateElementTypeMap(app, readIncludedDocument(schemaURI), schemaURI, elementTypeMap);
            }
        }
    }
    
    public void generateMainClass(Element resourcesEl, File src) {
        
    }
    
    private void writeResourceClass(Element rElement,
                                    ContextInfo info, 
                                    boolean isRoot) {
        String resourceId = resourceName != null 
            ? resourceName : rElement.getAttribute("id");
        if (resourceId.length() == 0) {
            String path = rElement.getAttribute("path");
            if (path.length() > 0) {
                path = path.replaceAll("[\\{\\}_]*", "");
                String[] split = path.split("/");
                for (int i = 0; i < split.length; i++) {
                    if (split[i].length() > 0) {
                        resourceId += split[i].toUpperCase().charAt(0) + split[i].substring(1);
                    }
                }
            }
            resourceId += DEFAULT_RESOURCE_NAME;    
        }
        
        boolean expandedQName = resourceId.startsWith("{") ? true : false;
        QName qname = convertToQName(resourceId, expandedQName);
        String namespaceURI = possiblyConvertNamespaceURI(qname.getNamespaceURI(), expandedQName);
        
        if (getSchemaClassName(namespaceURI, info.getGrammarInfo(), qname.getLocalPart(), 
                              info.getTypeClassNames()) != null) {
            return; 
        }
        
        
        final String className = getClassName(qname.getLocalPart(), 
                info.isInterfaceGenerated(), info.getTypeClassNames());
        if (info.getResourceClassNames().contains(className)) {
            return;
        }
        info.getResourceClassNames().add(className);
        final String classPackage = getClassPackageName(namespaceURI);
        
        StringBuilder sbImports = new StringBuilder();
        StringBuilder sbCode = new StringBuilder();
        Set<String> imports = createImports();
        
        
        sbImports.append(getClassComment()).append(getLineSep());
        sbImports.append("package " + classPackage)
            .append(";").append(getLineSep()).append(getLineSep());
        
        if (isRoot && writeAnnotations(info.isInterfaceGenerated())) {
            String path = rElement.getAttribute("path");
            writeAnnotation(sbCode, imports, Path.class, path, true, false);
        }
                
        sbCode.append("public " + getClassType(info.interfaceIsGenerated) + " " + className);
        writeImplementsInterface(sbCode, qname.getLocalPart(), info.isInterfaceGenerated());              
        sbCode.append(" {" + getLineSep() + getLineSep());
        
        writeMethods(rElement, classPackage, imports, sbCode, info, resourceId, isRoot, "");
        
        sbCode.append("}");
        writeImports(sbImports, imports, classPackage);
        
        createJavaSourceFile(info.getSrcDir(), new QName(classPackage, className), sbCode, sbImports, true);
        
        writeSubresourceClasses(rElement, info, isRoot, resourceId);
    }
    
    private void writeSubresourceClasses(Element rElement, ContextInfo info, 
                                         boolean isRoot, String resourceId) {

        List<Element> childEls = getWadlElements(rElement, "resource");
        for (Element subEl : childEls) {
            String id = subEl.getAttribute("id");
            if (id.length() > 0 && !resourceId.equals(id) && !id.startsWith("{java")
                && !id.startsWith("java")) {
                Element subElement = getResourceElement(info.getApp(), subEl, info.getGrammarInfo(), 
                    info.getTypeClassNames(), subEl.getAttribute("type"), info.getSrcDir());
                writeResourceClass(subElement, info, false);
            }
            writeSubresourceClasses(subEl, info, false, id);
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
    
    private String getClassName(String clsName, boolean interfaceIsGenerated, Set<String> typeClassNames) {
        String name = null;
        if (interfaceIsGenerated) {
            name = clsName;
        } else {
            name = generateInterfaces ? clsName + "Impl" : clsName;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(name.charAt(0)));
        name = name.length() > 1 ? sb.append(name.substring(1)).toString() : sb.toString();
        for (String typeName : typeClassNames) {
            String localName = typeName.contains(".") 
                ? typeName.substring(typeName.lastIndexOf('.') + 1) : typeName;
            if (name.equalsIgnoreCase(localName)) {
                name += "Resource";
            }
        }
        return name;
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
    //CHECKSTYLE:OFF
    private void writeMethods(Element rElement,
                              String classPackage,
                              Set<String> imports, 
                              StringBuilder sbCode, 
                              ContextInfo info,
                              String resourceId,
                              boolean isRoot,
                              String currentPath) {
    //CHECKSTYLE:ON    
        List<Element> methodEls = getWadlElements(rElement, "method");
       
        for (Element methodEl : methodEls) {
            writeResourceMethod(methodEl, classPackage, imports, sbCode, info, isRoot, currentPath);    
        }
        
        List<Element> childEls = getWadlElements(rElement, "resource");
        for (Element childEl : childEls) {
            String path = childEl.getAttribute("path");
            String newPath = (currentPath + path).replace("//", "/");
            String id = childEl.getAttribute("id");
            if (id.length() == 0) {
                writeMethods(childEl, classPackage, imports, sbCode, info, id, false, newPath);
            } else {
                writeResourceMethod(childEl, classPackage, imports, sbCode, info, false, newPath);
            }
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
                                     String classPackage,
                                     Set<String> imports,
                                     StringBuilder sbCode,
                                     ContextInfo info,
                                     boolean isRoot,
                                     String currentPath) {
        boolean isResourceElement = "resource".equals(methodEl.getLocalName());
        Element resourceEl = isResourceElement ? methodEl : (Element)methodEl.getParentNode();
        
        List<Element> responseEls = getWadlElements(methodEl, "response");
        List<Element> requestEls = getWadlElements(methodEl, "request");
        Element firstRequestEl = requestEls.size() >= 1 ? requestEls.get(0) : null;
        List<Element> allRequestReps = getWadlElements(firstRequestEl, "representation");
        List<Element> xmlRequestReps = getXmlReps(allRequestReps);
        
        boolean jaxpSourceRequired = xmlRequestReps.size() > 1 && !supportMultipleXmlReps;
        int numOfMethods = jaxpSourceRequired ? 1 : xmlRequestReps.size(); 
        for (int i = 0; i < numOfMethods; i++) {
            
            Element inXmlRep = xmlRequestReps.get(i);
                        
            String methodNameLowerCase = methodEl.getAttribute("name").toLowerCase();
            String id = methodEl.getAttribute("id");
            if (id.length() == 0) {
                id = methodNameLowerCase;
            }
            String suffixName = "";
            if (!jaxpSourceRequired && inXmlRep != null && xmlRequestReps.size() > 1) {
                String value = inXmlRep.getAttribute("element");
                int index = value.indexOf(":");
                suffixName = value.substring(index + 1);
            }
            if (writeAnnotations(info.isInterfaceGenerated())) {
                sbCode.append(TAB);
                
                if (methodNameLowerCase.length() > 0) {
                    if (HTTP_METHOD_ANNOTATIONS.containsKey(methodNameLowerCase)) {
                        writeAnnotation(sbCode, imports, 
                                        HTTP_METHOD_ANNOTATIONS.get(methodNameLowerCase), null, true, true);
                    } else {
                        // TODO : write a custom annotation class name based on HttpMethod    
                    }
                    writeFormatAnnotations(allRequestReps, sbCode, imports, true);
                    writeFormatAnnotations(getWadlElements(getOKResponse(responseEls), "representation"),
                            sbCode, imports, false);
                }
                if (!isRoot && !"/".equals(currentPath)) {
                    writeAnnotation(sbCode, imports, Path.class, currentPath, true, true);
                }
            } else {
                sbCode.append(getLineSep()).append(TAB);
            }
            
            if (!info.isInterfaceGenerated()) {
                sbCode.append("public ");
            }
            boolean responseTypeAvailable = true;
            if (methodNameLowerCase.length() > 0) {
                responseTypeAvailable = writeResponseType(responseEls, sbCode, imports,
                        info.getTypeClassNames(), info.getGrammarInfo());
                sbCode.append(id + suffixName);
            } else {
                boolean expandedQName = id.startsWith("{");
                QName qname = convertToQName(id, expandedQName);
                String packageName = possiblyConvertNamespaceURI(qname.getNamespaceURI(), expandedQName);
                
                String clsSimpleName = getSchemaClassName(packageName, info.getGrammarInfo(), 
                        qname.getLocalPart(), info.getTypeClassNames());
                String localName = clsSimpleName == null 
                    ? getClassName(qname.getLocalPart(), true, info.getTypeClassNames()) 
                    : clsSimpleName.substring(packageName.length() + 1);
                String subResponseNs = clsSimpleName == null ? getClassPackageName(packageName) 
                    : clsSimpleName.substring(0, packageName.length());
                String parentId = ((Element)resourceEl.getParentNode()).getAttribute("id");
                writeSubResponseType(id.equals(parentId), subResponseNs, localName, 
                        sbCode, imports);
                
                sbCode.append("get" + localName + suffixName);
            }
            
            sbCode.append("(");
            
            List<Element> inParamElements = getParameters(resourceEl, 
                        !isRoot && !isResourceElement && resourceEl.getAttribute("id").length() > 0);
            
            Element repElement = getActualRepElement(allRequestReps, inXmlRep); 
            writeRequestTypes(firstRequestEl, classPackage, repElement, inParamElements, 
                    jaxpSourceRequired, sbCode, imports, info);
            sbCode.append(")");
            if (info.isInterfaceGenerated()) {
                sbCode.append(";");
            } else {
                generateEmptyMethodBody(sbCode, responseTypeAvailable);
            }
            sbCode.append(getLineSep()).append(getLineSep());
        }
    }

    private List<Element> getXmlReps(List<Element> repElements) {
        Set<String> values = new HashSet<String>(repElements.size());
        List<Element> xmlReps = new ArrayList<Element>();
        for (Element el : repElements) {
            String value = el.getAttribute("element");
            if (value.length() > 0 && value.contains(":") && !values.contains(value)) {
                xmlReps.add(el);
                values.add(value);
            }
        }
        if (xmlReps.isEmpty()) {
            xmlReps.add(null);
        }
        return xmlReps;
    }
    
    private List<Element> getParameters(Element resourceEl, boolean isSubresourceMethod) {
        List<Element> inParamElements = new LinkedList<Element>();
        List<Element> allParamElements = getWadlElements(resourceEl, "param");
        for (Element el : allParamElements) {
            if (isSubresourceMethod && RESOURCE_LEVEL_PARAMS.contains(el.getAttribute("style"))) {
                continue;
            }
            inParamElements.add(el);
        }
        return inParamElements;
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
        List<Element> repElements = getWadlElements(requestEl, "representation");
 
        if (repElements.size() == 1) {
            String mediaType = repElements.get(0).getAttribute("mediaType");
            if (MediaType.APPLICATION_FORM_URLENCODED.equals(mediaType)) { 
                inParamElements.addAll(getWadlElements(repElements.get(0), "param"));
                return true;
            }
        }
        return false;
    }
    
    private Element getActualRepElement(List<Element> repElements, Element xmlElement) {
        if (xmlElement != null) {
            return xmlElement;
        }
        for (Element el : repElements) {
            Element param = DOMUtils.getFirstChildWithName(el, getWadlNamespace(), "param");
            if (param != null) {
                return el;
            }
        }
        return null;
    }
    
    private boolean writeResponseType(List<Element> responseEls,
                                      StringBuilder sbCode,
                                      Set<String> imports, Set<String> typeClassNames, 
                                      GrammarInfo gInfo) {
        
        Element okResponse = getOKResponse(responseEls);
        
        List<Element> repElements = null;
        if (okResponse != null) {
            repElements = getWadlElements(okResponse, "representation");    
        } else {
            repElements = CastUtils.cast(Collections.emptyList(), Element.class);
        }
        
        if (repElements.size() == 0) {    
            sbCode.append("void ");
            return false;
        }
        String elementName = getElementRefName(
                getActualRepElement(repElements, getXmlReps(repElements).get(0)),
                typeClassNames, gInfo, imports);
        if (elementName != null) {
            sbCode.append(elementName + " ");
        } else {
            addImport(imports, Response.class.getName());
            sbCode.append("Response ");
        }
        return true;
    }
    
    private Element getOKResponse(List<Element> responseEls) {
        for (int i = 0; i < responseEls.size(); i++) {
            String statusValue = responseEls.get(i).getAttribute("status");
            if (statusValue.length() == 0) {
                return responseEls.get(i);
            }
            String[] statuses = statusValue.split("\\s");
            for (String status : statuses) {
                if (HTTP_OK_STATUSES.contains(status)) {
                    return responseEls.get(i);
                }
            }
        }
        return null;
    }
    
    private void writeSubResponseType(boolean recursive, String ns, String localName,
                                      StringBuilder sbCode, Set<String> imports) {
        if (!recursive && ns.length() > 0) {
            addImport(imports, ns + "." + localName);
        }
        sbCode.append(localName).append(" ");
    }
    //CHECKSTYLE:OFF    
    private void writeRequestTypes(Element requestEl,
                                   String classPackage,
                                   Element repElement,
                                   List<Element> inParamEls, 
                                   boolean jaxpRequired,
                                   StringBuilder sbCode, 
                                   Set<String> imports, 
                                   ContextInfo info) {
    //CHECKSTYLE:ON    
        boolean form = false;
        boolean formParamsAvailable = false;
        if (requestEl != null) {
            inParamEls.addAll(getWadlElements(requestEl, "param"));
            int currentSize = inParamEls.size();
            form = addFormParameters(inParamEls, requestEl);
            formParamsAvailable = currentSize < inParamEls.size(); 
        }
                  
        for (int i = 0; i < inParamEls.size(); i++) {
    
            Element paramEl = inParamEls.get(i);
            Class<?> paramAnn = PARAM_ANNOTATIONS.get(paramEl.getAttribute("style"));
            if (paramAnn == QueryParam.class && form) {
                paramAnn = FormParam.class; 
            } 
            String name = paramEl.getAttribute("name");
            boolean enumCreated = false;
            if (generateEnums) {
                List<Element> options =
                    DOMUtils.findAllElementsByTagNameNS(paramEl, getWadlNamespace(), "option");
                if (options.size() > 0) {
                    generateEnumClass(getTypicalClassName(name), options, info.getSrcDir(), classPackage);
                    enumCreated = true;
                }
            }
            if (writeAnnotations(info.isInterfaceGenerated())) {
                writeAnnotation(sbCode, imports, paramAnn, name, false, false);
                sbCode.append(" ");
                String defaultVal = paramEl.getAttribute("default");
                if (defaultVal.length() > 0) {
                    writeAnnotation(sbCode, imports, DefaultValue.class, defaultVal, false, false);
                    sbCode.append(" ");    
                }
            }
            boolean isRepeating = Boolean.valueOf(paramEl.getAttribute("repeating"));
            String type = getPrimitiveType(paramEl);
            if (paramAnn == QueryParam.class
                && (isRepeating || !Boolean.valueOf(paramEl.getAttribute("required")))    
                && AUTOBOXED_PRIMITIVES_MAP.containsKey(type)) {
                type = AUTOBOXED_PRIMITIVES_MAP.get(type);
            }
            if (isRepeating) {
                addImport(imports, List.class.getName());
                type = "List<" + type + ">";
            }
            String paramName = enumCreated ? getTypicalClassName(name) 
                                           : name.replaceAll("[\\.\\-]", "_");
            sbCode.append(type).append(" ").append(paramName);
            if (i + 1 < inParamEls.size()) {
                sbCode.append(", ");
                if (i + 1 >= 4 && ((i + 1) % 4) == 0) {
                    sbCode.append(getLineSep()).append(TAB).append(TAB).append(TAB).append(TAB);
                }
            }
        }
        String elementParamType = null;
        String elementParamName = null;
        if (!form) {
            if (!jaxpRequired) {    
                elementParamType = getElementRefName(repElement, 
                        info.getTypeClassNames(), info.getGrammarInfo(), imports);
                if (elementParamType != null) {
                    elementParamName = elementParamType.toLowerCase();
                }
            } else {
                addImport(imports, Source.class.getName());
                elementParamType = Source.class.getSimpleName();
                elementParamName = "source";
            }
        } else if (!formParamsAvailable) {
            addImport(imports, MultivaluedMap.class.getName());
            elementParamType = MultivaluedMap.class.getSimpleName();
            elementParamName = "map";
        }
        if (elementParamType != null) {
            if (inParamEls.size() > 0) {
                sbCode.append(", ");
            }
            sbCode.append(elementParamType).append(" ").append(elementParamName);
        }
    }
    
    private void generateEnumClass(String clsName, List<Element> options, File src, String classPackage) {
        StringBuilder sbImports = new StringBuilder();
        StringBuilder sbCode = new StringBuilder();
        sbImports.append(getClassComment()).append(getLineSep());
        sbImports.append("package " + classPackage)
            .append(";").append(getLineSep()).append(getLineSep());
        
        sbCode.append("public enum " + clsName);
        sbCode.append(" {" + getLineSep());
        
        for (int i = 0; i < options.size(); i++) {
            String value = options.get(i).getAttribute("value");
            sbCode.append(TAB).append(value.toUpperCase());
            if (i + 1 < options.size()) {
                sbCode.append(",");
            }
            sbCode.append(getLineSep());
        }
        sbCode.append("}");
        createJavaSourceFile(src, new QName(classPackage, clsName), sbCode, sbImports, false);
    }
    
    private String getTypicalClassName(String name) { 
        String theName = name.toUpperCase();
        if (theName.length() == 1) {
            return theName;
        } else {
            return theName.substring(0, 1) + theName.substring(1).toLowerCase();
        }
    }
    
    private List<Element> getWadlElements(Element parent, String name) {
        List<Element> elements = parent != null 
            ? DOMUtils.getChildrenWithName(parent, getWadlNamespace(), name)
            : CastUtils.cast(Collections.emptyList(), Element.class);
        if (!"resource".equals(name)) {    
            for (int i = 0; i < elements.size(); i++) {
                Element el = elements.get(i);
                Element realEl = getWadlElement(el);
                if (el != realEl) {
                    elements.set(i, realEl);
                }
            }
        }
        return elements;
    }
    
    private String getPrimitiveType(Element paramEl) {
        String type = paramEl.getAttribute("type");
        if (type.length() == 0) {
            return "String";
        }
        String[] pair = type.split(":");
        String value = pair.length == 2 ? pair[1] : type;
        if (XSD_SPECIFIC_TYPE_MAP.containsKey(value)) {
            return XSD_SPECIFIC_TYPE_MAP.get(value);
        } else {
            return value;
        }
    }
    
    private String getElementRefName(Element repElement,
                                     Set<String> typeClassNames,
                                     GrammarInfo gInfo, 
                                     Set<String> imports) {
        if (repElement == null) {
            return null;
        }
        String elementRef = repElement.getAttribute("element");
        
        if (elementRef.length() > 0) {
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
                    int index = clsName.lastIndexOf(".");
                    if (index != -1) {
                        return clsName.substring(index + 1);
                    } else {
                        return clsName;
                    }
                }
            }
        } else {
            Element param = DOMUtils.getFirstChildWithName(repElement, getWadlNamespace(), "param");
            if (param != null) {
                return getPrimitiveType(param);
            }
        }
        return null;
    }
    
    private String getSchemaClassName(String packageName, GrammarInfo gInfo, String localName,
                                      Set <String> typeClassNames) {
        String clsName = matchClassName(typeClassNames, packageName, localName);
        if (clsName == null && gInfo != null) {
            String elementTypeName = gInfo.getElementTypeMap().get(localName);
            clsName = matchClassName(typeClassNames, packageName, elementTypeName);
            if (clsName == null && elementTypeName != null && elementTypeName.contains("_")) {
                clsName = matchClassName(typeClassNames, packageName, elementTypeName.replaceAll("_", ""));
            }
        }
        if (clsName == null && schemaTypesMap != null) {
            clsName = schemaTypesMap.get(packageName + "." + localName);
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
    
    
    
    private void writeFormatAnnotations(List<Element> repElements, StringBuilder sbCode, 
                                        Set<String> imports, boolean inRep) {
        if (repElements.size() == 0) {    
            return;
        }
        Class<?> cls = inRep ? Consumes.class : Produces.class;
        addImport(imports, cls.getName());
        sbCode.append("@").append(cls.getSimpleName()).append("(");
        if (repElements.size() > 1) {
            sbCode.append("{");
        }
        String mediaTypes = "";
        boolean first = true;
        for (int i = 0; i < repElements.size(); i++) {
            String mediaType = repElements.get(i).getAttribute("mediaType");
            if (mediaType != null && !mediaTypes.contains(mediaType)) {
                if (!first) { 
                    mediaTypes += ", ";
                }
                first = false;
                mediaTypes += "\"" + mediaType + "\"";
            }
        }
        sbCode.append(mediaTypes);
        if (repElements.size() > 1) {
            sbCode.append(" }");
        }
        sbCode.append(")");
        sbCode.append(getLineSep()).append(TAB);
    }
    
    private void createJavaSourceFile(File src, QName qname, StringBuilder sbCode, StringBuilder sbImports,
                                      boolean serviceClass) {
        String content = sbImports.toString() + getLineSep() + sbCode.toString();
        
        String namespace = qname.getNamespaceURI();
        if (serviceClass) {
            generatedServiceClasses.add(namespace + "." + qname.getLocalPart());
        }
        
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
        List<Element> grammarEls = getWadlElements(app.getAppElement(), "grammars");
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
        List<Element> includeEls = getWadlElements(grammarEls.get(0), "include");
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
        compiler.setEntityResolver(OASISCatalogManager.getCatalogManager(bus)
                                       .getEntityResolver());
        if (compilerArgs.size() > 0) {
            compiler.getOptions().addGrammar(new InputSource("null"));
            compiler.getOptions().parseArguments(compilerArgs.toArray(new String[] {}));
        }
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
    
    public void setCompilerArgs(List<String> args) {
        this.compilerArgs = args;
    }

    public void setSchemaPackageMap(Map<String, String> map) {
        this.schemaPackageMap = map;
    }
    
    public void setSchemaTypesMap(Map<String, String> map) {
        this.schemaTypesMap = map;
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
    
    private static class ContextInfo {
        private boolean interfaceIsGenerated;
        private Set<String> typeClassNames;
        private GrammarInfo gInfo;
        private Set<String> resourceClassNames = new HashSet<String>();
        private Application rootApp;
        private File srcDir;
        
        public ContextInfo(Application rootApp,
                           File srcDir,
                           Set<String> typeClassNames, 
                           GrammarInfo gInfo, 
                           boolean interfaceIsGenerated) {
            this.interfaceIsGenerated = interfaceIsGenerated;
            this.typeClassNames = typeClassNames;
            this.gInfo = gInfo;
            this.rootApp = rootApp;
            this.srcDir = srcDir;
        }
        public Application getApp() {
            return rootApp;
        }
        public File getSrcDir() {
            return srcDir;
        }
        public boolean isInterfaceGenerated() {
            return interfaceIsGenerated;
        }
        public Set<String> getTypeClassNames() {
            return typeClassNames;
        }
        public GrammarInfo getGrammarInfo() {
            return gInfo;
        }
        public Set<String> getResourceClassNames() {
            return resourceClassNames;
        }
        
        
    }
}
