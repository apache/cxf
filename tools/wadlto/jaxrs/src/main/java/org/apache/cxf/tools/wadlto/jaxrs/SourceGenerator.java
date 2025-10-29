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
package org.apache.cxf.tools.wadlto.jaxrs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.catalog.OASISCatalogManagerHelper;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.jaxb.JAXBUtils.JCodeModel;
import org.apache.cxf.common.jaxb.JAXBUtils.S2JJAXBModel;
import org.apache.cxf.common.jaxb.JAXBUtils.SchemaCompiler;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.jaxrs.ext.Oneway;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.constants.Constants;

public class SourceGenerator {
    public static final String CODE_TYPE_GRAMMAR = "grammar";
    public static final String CODE_TYPE_PROXY = "proxy";
    public static final String CODE_TYPE_WEB = "web";
    public static final String LINE_SEP_PROPERTY = "line.separator";

    private static final Logger LOG = LogUtils.getL7dLogger(SourceGenerator.class);

    private static final String DEFAULT_PACKAGE_NAME = "application";
    private static final String DEFAULT_RESOURCE_NAME = "Resource";
    private static final String TAB = "    ";

    private static final Set<String> HTTP_OK_STATUSES =
        new HashSet<>(Arrays.asList("200", "201", "202", "203", "204"));

    private static final Set<Class<?>> OPTIONAL_PARAMS =
        new HashSet<>(Arrays.asList(QueryParam.class, HeaderParam.class, MatrixParam.class, FormParam.class));
    private static final Map<String, Class<?>> HTTP_METHOD_ANNOTATIONS = new HashMap<>();
    private static final Map<String, Class<?>> PARAM_ANNOTATIONS = new HashMap<>();
    private static final String PLAIN_PARAM_STYLE = "plain";
    private static final String BEAN_VALID_SIMPLE_NAME = "Valid";
    private static final String BEAN_VALID_FULL_NAME = "jakarta.validation." + BEAN_VALID_SIMPLE_NAME;
    private static final Set<String> RESOURCE_LEVEL_PARAMS = new HashSet<>(Arrays.asList("template", "matrix"));
    private static final Map<String, String> AUTOBOXED_PRIMITIVES_MAP = new HashMap<>();
    private static final Map<String, String> XSD_SPECIFIC_TYPE_MAP = new HashMap<>();

    static {
        HTTP_METHOD_ANNOTATIONS.put("get", GET.class);
        HTTP_METHOD_ANNOTATIONS.put("put", PUT.class);
        HTTP_METHOD_ANNOTATIONS.put("post", POST.class);
        HTTP_METHOD_ANNOTATIONS.put("delete", DELETE.class);
        HTTP_METHOD_ANNOTATIONS.put("head", HEAD.class);
        HTTP_METHOD_ANNOTATIONS.put("options", OPTIONS.class);
        HTTP_METHOD_ANNOTATIONS.put("patch", PATCH.class);

        PARAM_ANNOTATIONS.put("template", PathParam.class);
        PARAM_ANNOTATIONS.put("header", HeaderParam.class);
        PARAM_ANNOTATIONS.put("query", QueryParam.class);
        PARAM_ANNOTATIONS.put("matrix", MatrixParam.class);

        AUTOBOXED_PRIMITIVES_MAP.put(byte.class.getSimpleName(), Byte.class.getSimpleName());
        AUTOBOXED_PRIMITIVES_MAP.put(short.class.getSimpleName(), Short.class.getSimpleName());
        AUTOBOXED_PRIMITIVES_MAP.put(int.class.getSimpleName(), Integer.class.getSimpleName());
        AUTOBOXED_PRIMITIVES_MAP.put(long.class.getSimpleName(), Long.class.getSimpleName());
        AUTOBOXED_PRIMITIVES_MAP.put(float.class.getSimpleName(), Float.class.getSimpleName());
        AUTOBOXED_PRIMITIVES_MAP.put(double.class.getSimpleName(), Double.class.getSimpleName());
        AUTOBOXED_PRIMITIVES_MAP.put(boolean.class.getSimpleName(), Boolean.class.getSimpleName());

        XSD_SPECIFIC_TYPE_MAP.put("string", "String");
        XSD_SPECIFIC_TYPE_MAP.put("integer", "long");
        XSD_SPECIFIC_TYPE_MAP.put("float", "float");
        XSD_SPECIFIC_TYPE_MAP.put("double", "double");
        XSD_SPECIFIC_TYPE_MAP.put("int", "int");
        XSD_SPECIFIC_TYPE_MAP.put("long", "long");
        XSD_SPECIFIC_TYPE_MAP.put("byte", "byte");
        XSD_SPECIFIC_TYPE_MAP.put("boolean", "boolean");
        XSD_SPECIFIC_TYPE_MAP.put("unsignedInt", "long");
        XSD_SPECIFIC_TYPE_MAP.put("unsignedShort", "int");
        XSD_SPECIFIC_TYPE_MAP.put("unsignedByte", "short");
        XSD_SPECIFIC_TYPE_MAP.put("unsignedLong", "java.math.BigInteger");
        XSD_SPECIFIC_TYPE_MAP.put("decimal", "java.math.BigInteger");
        XSD_SPECIFIC_TYPE_MAP.put("positiveInteger", "java.math.BigInteger");
        XSD_SPECIFIC_TYPE_MAP.put("QName", "javax.xml.namespace.QName");
        XSD_SPECIFIC_TYPE_MAP.put("duration", "javax.xml.datatype.Duration");
        XSD_SPECIFIC_TYPE_MAP.put("date", "java.util.Date");
        XSD_SPECIFIC_TYPE_MAP.put("dateTime", "java.util.Date");
        XSD_SPECIFIC_TYPE_MAP.put("time", "java.util.Date");
        XSD_SPECIFIC_TYPE_MAP.put("anyType", "String");
        XSD_SPECIFIC_TYPE_MAP.put("anyURI", "java.net.URI");
    }

    private Comparator<String> importsComparator;
    private boolean generateInterfaces = true;
    private boolean generateImpl;
    private String resourcePackageName;
    private String resourceName;
    private String wadlPath;
    private String wadlNamespace = WadlGenerator.WADL_NS;
    private boolean generateEnums;
    private boolean skipSchemaGeneration;
    private boolean inheritResourceParams;
    private boolean inheritResourceParamsFirst;
    private boolean useVoidForEmptyResponses = true;
    private boolean generateResponseIfHeadersSet;

    private final String lineSeparator;

    private List<String> generatedServiceClasses = new ArrayList<>();
    private List<String> generatedTypeClasses = new ArrayList<>();
    private List<InputSource> bindingFiles = Collections.emptyList();
    private List<InputSource> schemaPackageFiles = Collections.emptyList();
    private List<String> compilerArgs = new ArrayList<>();
    private Set<String> suspendedAsyncMethods = Collections.emptySet();
    private Set<String> responseMethods = Collections.emptySet();
    private Set<String> onewayMethods = Collections.emptySet();
    private Map<String, String> schemaPackageMap = Collections.emptyMap();
    private Map<String, String> javaTypeMap = Collections.emptyMap();
    private Map<String, String> schemaTypeMap = Collections.emptyMap();
    private Map<String, String> mediaTypesMap = Collections.emptyMap();
    private Bus bus;
    private boolean supportMultipleRepsWithElements;
    private boolean supportBeanValidation;
    private boolean validateWadl;
    private SchemaCollection schemaCollection = new SchemaCollection();
    private String encoding;
    private String authentication;
    private boolean createJavaDocs;
    private String jaxbClassNameSuffix;
    private String rx;
    private ResponseWrapper responseWrapper;

    public SourceGenerator() {
        this(Collections.emptyMap());
    }

    public SourceGenerator(Map<String, String> properties) {
        String value = properties.get(LINE_SEP_PROPERTY);
        lineSeparator = value == null ? SystemPropertyAction.getProperty(LINE_SEP_PROPERTY) : value;
        responseWrapper = ResponseWrapper.create(rx);
    }

    public void setSupportMultipleXmlReps(boolean support) {
        supportMultipleRepsWithElements = support;
    }

    public void setWadlNamespace(String ns) {
        this.wadlNamespace = ns;
    }

    public void setUseVoidForEmptyResponses(boolean use) {
        this.useVoidForEmptyResponses = use;
    }

    public void setGenerateResponseIfHeadersSet(boolean set) {
        this.generateResponseIfHeadersSet = true;
    }

    public String getWadlNamespace() {
        return wadlNamespace;
    }

    public void setGenerateEnums(boolean generate) {
        this.generateEnums = generate;
    }

    public void setSkipSchemaGeneration(boolean skip) {
        this.skipSchemaGeneration = skip;
    }

    public void setSuspendedAsyncMethods(Set<String> asyncMethods) {
        this.suspendedAsyncMethods = asyncMethods;
    }

    public void setResponseMethods(Set<String> responseMethods) {
        this.responseMethods = responseMethods;
    }

    public void setOnewayMethods(Set<String> onewayMethods) {
        this.onewayMethods = onewayMethods;
    }

    private String getClassPackageName(String wadlPackageName) {
        if (resourcePackageName != null) {
            return resourcePackageName;
        } else if (wadlPackageName != null && !wadlPackageName.isEmpty()) {
            return wadlPackageName;
        } else {
            return DEFAULT_PACKAGE_NAME;
        }
    }

    public void generateSource(File srcDir, String codeType) {
        Application app = readWadl();
        Set<String> typeClassNames = new HashSet<>();
        GrammarInfo gInfo = generateSchemaCodeAndInfo(app, typeClassNames, srcDir);
        if (!CODE_TYPE_GRAMMAR.equals(codeType)) {
            generateResourceClasses(app, gInfo, typeClassNames, srcDir);
        }
    }

    private GrammarInfo generateSchemaCodeAndInfo(Application app, Set<String> typeClassNames,
                                                  File srcDir) {

        List<SchemaInfo> schemaElements = getSchemaElements(app);
        if (!skipSchemaGeneration && schemaElements != null && !schemaElements.isEmpty()) {
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
            throw new IllegalStateException("Single WADL resources element is expected");
        }

        List<Element> resourceEls = getWadlElements(resourcesEls.get(0), "resource");
        if (resourceEls.isEmpty()) {
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
        if (!type.isEmpty()) {
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
                Application refApp = new Application(readDocument(wadlRefPath), wadlRefPath);
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
        if (href.startsWith("#")) {
            return resolveLocalReference(wadlEl.getOwnerDocument().getDocumentElement(),
                                         wadlEl.getLocalName(), href);
        }
        return wadlEl;
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

        Map<String, String> nsMap = new HashMap<>();
        NamedNodeMap attrMap = app.getAppElement().getAttributes();
        for (int i = 0; i < attrMap.getLength(); i++) {
            Node node = attrMap.item(i);
            String nodeName = node.getNodeName();
            if (nodeName.startsWith("xmlns:")) {
                String nsValue = node.getNodeValue();
                nsMap.put(nodeName.substring(6), nsValue);
            }
        }
        Map<String, String> elementTypeMap = new HashMap<>();
        for (SchemaInfo schemaEl : schemaElements) {
            populateElementTypeMap(app, schemaEl.getElement(), schemaEl.getSystemId(), elementTypeMap);
        }
        boolean noTargetNamespace = schemaElements.size() == 1
            && schemaElements.get(0).getNamespaceURI().isEmpty();
        return new GrammarInfo(nsMap, elementTypeMap, noTargetNamespace);
    }

    private void populateElementTypeMap(Application app, Element schemaEl,
            String systemId, Map<String, String> elementTypeMap) {
        List<Element> elementEls = DOMUtils.getChildrenWithName(schemaEl,
                                                                Constants.URI_2001_SCHEMA_XSD, "element");
        for (Element el : elementEls) {
            String type = el.getAttribute("type");
            if (!type.isEmpty()) {
                elementTypeMap.put(el.getAttribute("name"), type);
            }
        }
        Element includeEl = DOMUtils.getFirstChildWithName(schemaEl,
                                                           Constants.URI_2001_SCHEMA_XSD, "include");
        if (includeEl != null) {
            int ind = systemId.lastIndexOf('/');
            if (ind != -1) {
                String schemaURI = systemId.substring(0, ind + 1) + includeEl.getAttribute("schemaLocation");
                populateElementTypeMap(app, readDocument(schemaURI), schemaURI, elementTypeMap);
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
        if (resourceId.isEmpty()) {
            String path = rElement.getAttribute("path");
            if (!path.isEmpty()) {
                path = path.replaceAll("[-\\{\\}_]*", "");
                String[] split = path.split("/");
                StringBuilder builder = new StringBuilder(resourceId);
                for (int i = 0; i < split.length; i++) {
                    if (!split[i].isEmpty()) {
                        builder.append(StringUtils.capitalize(split[i]));
                    }
                }
                resourceId = builder.toString();
                
            }
            resourceId += DEFAULT_RESOURCE_NAME;
        }

        boolean expandedQName = resourceId.startsWith("{");
        QName qname = convertToQName(resourceId, expandedQName);
        String namespaceURI = possiblyConvertNamespaceURI(qname.getNamespaceURI(), expandedQName);

        if (getSchemaClassName(namespaceURI, info.getGrammarInfo(), qname.getLocalPart(),
                              info.getTypeClassNames()) != null) {
            return;
        }

        final String className = getClassName(qname.getLocalPart(),
                info.isInterfaceGenerated(), info.getTypeClassNames()).replaceFirst("^[- 0-9]*", "");
        if (info.getResourceClassNames().contains(className)) {
            return;
        }
        info.getResourceClassNames().add(className);
        final String classPackage = getClassPackageName(namespaceURI.replaceFirst("^[- 0-9]*", ""));

        StringBuilder sbCode = new StringBuilder();
        Set<String> imports = createImports();


        boolean doCreateJavaDocs = isJavaDocNeeded(info);
        if (doCreateJavaDocs) {
            writeClassDocs(rElement, sbCode);
        }
        if (isRoot && writeAnnotations(info.isInterfaceGenerated())) {
            String path = rElement.getAttribute("path");
            writeAnnotation(sbCode, imports, Path.class, path)
                .append(lineSeparator);
        }

        sbCode.append("public ").append(getClassType(info.interfaceIsGenerated)).append(' ').append(className);
        writeImplementsInterface(sbCode, qname.getLocalPart(), info.isInterfaceGenerated());
        sbCode.append(" {").append(lineSeparator).append(lineSeparator);

        Map<String, Integer> methodNameMap = new HashMap<>();
        writeMethods(rElement, classPackage, imports, sbCode,
                     info, resourceId, isRoot, "",
                     methodNameMap);

        sbCode.append('}');

        createJavaSourceFile(info.getSrcDir(), classPackage, className, sbCode, imports, true);

        writeSubresourceClasses(rElement, info, isRoot, resourceId);
    }

    private void writeSubresourceClasses(Element rElement, ContextInfo info,
                                         boolean isRoot, String resourceId) {

        List<Element> childEls = getWadlElements(rElement, "resource");
        for (Element subEl : childEls) {
            String id = subEl.getAttribute("id");
            if (!id.isEmpty() && !resourceId.equals(id) && !id.startsWith("{java")
                && !id.startsWith("java")) {
                Element subElement = getResourceElement(info.getApp(), subEl, info.getGrammarInfo(),
                    info.getTypeClassNames(), subEl.getAttribute("type"), info.getSrcDir());
                writeResourceClass(subElement, info, false);
            }
            writeSubresourceClasses(subEl, info, false, id);
        }
    }

    private static QName convertToQName(String resourceId, boolean expandedQName) {
        final QName qname;
        if (expandedQName) {
            qname = JAXRSUtils.convertStringToQName(resourceId);
        } else {
            int lastIndex = resourceId.lastIndexOf('.');
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
        String name;
        if (interfaceIsGenerated) {
            name = clsName;
        } else {
            name = generateInterfaces ? clsName + "Impl" : clsName;
        }
        name = StringUtils.capitalize(name);
        for (String typeName : typeClassNames) {
            String localName = typeName.substring(typeName.lastIndexOf('.') + 1);
            if (name.equalsIgnoreCase(localName)) {
                name += "Resource";
            }
        }
        return name;
    }

    private boolean writeAnnotations(boolean interfaceIsGenerated) {
        if (interfaceIsGenerated) {
            return true;
        }
        return !generateInterfaces && generateImpl;
    }

    private void writeImplementsInterface(StringBuilder sb, String clsName,
                                             boolean interfaceIsGenerated) {
        if (generateInterfaces && !interfaceIsGenerated) {
            sb.append(" implements ").append(StringUtils.capitalize(clsName));
        }
    }

    private String getClassComment() {
        return "/**"
            + lineSeparator + " * Created by Apache CXF WadlToJava code generator"
            + lineSeparator + "**/";
    }
    //CHECKSTYLE:OFF: ParameterNumber
    private void writeMethods(Element rElement,
                              String classPackage,
                              Set<String> imports,
                              StringBuilder sbCode,
                              ContextInfo info,
                              String resourceId,
                              boolean isRoot,
                              String currentPath,
                              Map<String, Integer> methodNameMap) {
    //CHECKSTYLE:ON: ParameterNumber
        List<Element> methodEls = getWadlElements(rElement, "method");

        List<Element> currentInheritedParams = inheritResourceParams
            ? new ArrayList<Element>(info.getInheritedParams()) : Collections.emptyList();
        for (Element methodEl : methodEls) {
            writeResourceMethod(methodEl, classPackage, imports, sbCode, info, isRoot, currentPath, methodNameMap);
        }
        if (inheritResourceParams && methodEls.isEmpty()) {
            info.getInheritedParams().addAll(getWadlElements(rElement, "param"));
        }

        List<Element> childEls = getWadlElements(rElement, "resource");
        for (Element childEl : childEls) {
            String path = childEl.getAttribute("path");
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            String newPath = currentPath + path.replace("//", "/");
            String id = childEl.getAttribute("id");
            if (id.isEmpty()) {
                writeMethods(childEl, classPackage, imports, sbCode, info, id, false, newPath, methodNameMap);
            } else {
                writeResourceMethod(childEl, classPackage, imports, sbCode, info, false, newPath, methodNameMap);
            }
        }
        info.getInheritedParams().clear();
        info.getInheritedParams().addAll(currentInheritedParams);
    }

    private StringBuilder writeAnnotation(StringBuilder sbCode, Set<String> imports, Class<?> cls, String value) {
        if (value == null || !value.isEmpty()) {
            addImport(imports, cls.getName());
            sbCode.append('@').append(cls.getSimpleName());
            if (value != null) {
                sbCode.append("(\"").append(value).append("\")");
            }
        }
        return sbCode;
    }

    private void addImport(Set<String> imports, String clsName) {
        if (imports == null || clsName.startsWith("java.lang") || !clsName.contains(".")) {
            return;
        }
        imports.add(clsName);
    }

    //CHECKSTYLE:OFF: ParameterNumber
    private void writeResourceMethod(Element methodEl,
                                     String classPackage,
                                     Set<String> imports,
                                     StringBuilder sbCode,
                                     ContextInfo info,
                                     boolean isRoot,
                                     String currentPath,
                                     Map<String, Integer> methodNameMap) {
    //CHECKSTYLE:ON: ParameterNumber
        StringBuilder sbMethodCode = sbCode;
        StringBuilder sbMethodDocs = null;
        StringBuilder sbMethodRespDocs = null;

        boolean doCreateJavaDocs = isJavaDocNeeded(info);
        if (doCreateJavaDocs) {
            sbMethodCode = new StringBuilder();
            sbMethodDocs = startMethodDocs(methodEl);
            sbMethodRespDocs = new StringBuilder();
        }

        boolean isResourceElement = "resource".equals(methodEl.getLocalName());
        Element resourceEl = isResourceElement ? methodEl : (Element)methodEl.getParentNode();

        List<Element> responseEls = getWadlElements(methodEl, "response");
        List<Element> requestEls = getWadlElements(methodEl, "request");
        Element firstRequestEl = !requestEls.isEmpty() ? requestEls.get(0) : null;
        List<Element> allRequestReps = getWadlElements(firstRequestEl, "representation");
        List<Element> requestRepsWithElements = new ArrayList<>();
        boolean duplicatesAvailable =
            getRepsWithElements(allRequestReps, requestRepsWithElements, info.getGrammarInfo());

        String methodName = methodEl.getAttribute("name");
        final String methodNameLowerCase = methodName.toLowerCase();
        String idAttribute = methodEl.getAttribute("id");
        final String id = idAttribute.isEmpty() ? methodNameLowerCase : idAttribute;

        final boolean responseRequired = isMethodMatched(responseMethods, methodNameLowerCase, id);
        final boolean suspendedAsync = !responseRequired
            && isMethodMatched(suspendedAsyncMethods, methodNameLowerCase, id);
        final boolean oneway = isMethodMatched(onewayMethods, methodNameLowerCase, id);

        boolean jaxpSourceRequired = requestRepsWithElements.size() > 1 && !supportMultipleRepsWithElements;
        int numOfMethods = jaxpSourceRequired ? 1 : requestRepsWithElements.size();

        for (int i = 0; i < numOfMethods; i++) {

            List<Element> requestReps = allRequestReps;

            Element requestRepWithElement = requestRepsWithElements.get(i);
            String suffixName = "";
            if (supportMultipleRepsWithElements && requestRepWithElement != null
                && requestRepsWithElements.size() > 1) {
                String elementRef = requestRepWithElement.getAttribute("element");
                int index = elementRef.indexOf(':');
                suffixName = elementRef.substring(index + 1).replace("-", "");
                if (duplicatesAvailable) {
                    String mediaType = requestRepWithElement.getAttribute("mediaType");
                    if (!StringUtils.isEmpty(mediaType)) {
                        String subType = MediaType.valueOf(mediaType).getSubtype();
                        String[] parts = subType.split("\\+");
                        if (parts.length == 2) {
                            suffixName += StringUtils.capitalize(parts[1]);
                        } else {
                            suffixName += StringUtils.capitalize(parts[0].replaceAll("[\\.-]", ""));
                        }
                    }
                }
                requestReps = Collections.singletonList(requestRepWithElement);
            }
            if (writeAnnotations(info.isInterfaceGenerated())) {
                sbMethodCode.append(TAB);

                if (!methodNameLowerCase.isEmpty()) {
                    if (HTTP_METHOD_ANNOTATIONS.containsKey(methodNameLowerCase)) {
                        writeAnnotation(sbMethodCode, imports, HTTP_METHOD_ANNOTATIONS.get(methodNameLowerCase), null)
                            .append(lineSeparator).append(TAB);
                    } else {
                        writeCustomHttpMethod(info, classPackage, methodName, sbMethodCode, imports);
                    }
                    writeFormatAnnotations(requestReps, sbMethodCode, imports, true, null);
                    List<Element> responseReps = getWadlElements(getOKResponse(responseEls), "representation");
                    writeFormatAnnotations(responseReps,
                                           sbMethodCode, imports, false, requestRepWithElement);
                    if (supportBeanValidation && !responseRequired
                        && isRepWithElementAvailable(responseReps, info.getGrammarInfo())) {
                        addImport(imports, BEAN_VALID_FULL_NAME);
                        sbMethodCode.append('@').append(BEAN_VALID_SIMPLE_NAME).append(lineSeparator).append(TAB);
                    }
                    if (oneway) {
                        writeAnnotation(sbMethodCode, imports, Oneway.class, null)
                            .append(lineSeparator).append(TAB);
                    }
                }
                if (!isRoot && !"/".equals(currentPath)) {
                    writeAnnotation(sbMethodCode, imports, Path.class, currentPath)
                        .append(lineSeparator).append(TAB);
                }
            } else {
                sbMethodCode.append(lineSeparator).append(TAB);
            }

            if (!info.isInterfaceGenerated()) {
                sbMethodCode.append("public ");
            }
            boolean responseTypeAvailable = true;

            if (!methodNameLowerCase.isEmpty()) {
                responseTypeAvailable = writeResponseType(responseEls,
                                                          requestRepWithElement,
                                                          sbMethodCode,
                                                          sbMethodRespDocs,
                                                          imports,
                                                          info,
                                                          responseRequired,
                                                          suspendedAsync);

                String genMethodName = id + suffixName;
                if (methodNameLowerCase.equals(genMethodName) && idAttribute.isEmpty()) {
                    List<PathSegment> segments = JAXRSUtils.getPathSegments(currentPath, true, true);
                    StringBuilder sb = new StringBuilder();
                    for (PathSegment ps : segments) {
                        String pathSeg = ps.getPath().replaceAll("\\{", "").replaceAll("\\}", "");
                        int index = pathSeg.indexOf(':');
                        if (index > 0) {
                            pathSeg = pathSeg.substring(0, index);
                        }
                        sb.append(pathSeg);
                    }
                    genMethodName += StringUtils.capitalize(sb.toString());
                }
                genMethodName = genMethodName.replace("-", "");

                Integer value = methodNameMap.get(genMethodName);
                if (value == null) {
                    value = 0;
                }
                methodNameMap.put(genMethodName, ++value);
                if (value > 1) {
                    genMethodName = genMethodName + value.toString();
                }

                sbMethodCode.append(genMethodName);
            } else {
                writeSubresourceMethod(resourceEl, imports, sbMethodCode, info, id, suffixName);
            }

            sbMethodCode.append('(');

            List<Element> inParamElements = getParameters(resourceEl, info.getInheritedParams(),
                        !isRoot && !isResourceElement && !resourceEl.getAttribute("id").isEmpty());

            Element repElement = getActualRepElement(allRequestReps, requestRepWithElement);
            writeRequestTypes(firstRequestEl, classPackage, repElement, inParamElements,
                    jaxpSourceRequired, sbMethodCode, sbMethodDocs, imports, info, suspendedAsync);
            sbMethodCode.append(')');

            writeThrows(responseEls, sbMethodCode, sbMethodDocs, imports, info);

            if (info.isInterfaceGenerated()) {
                sbMethodCode.append(';');
            } else {
                generateEmptyMethodBody(sbMethodCode, responseTypeAvailable);
            }
            sbMethodCode.append(lineSeparator).append(lineSeparator);
        }
        finalizeMethodDocs(doCreateJavaDocs, sbCode, sbMethodDocs, sbMethodRespDocs, sbMethodCode);

    }

    private void finalizeMethodDocs(boolean doCreateJavaDocs, StringBuilder sbCode, StringBuilder sbJavaDocs,
                                    StringBuilder sbRespDocs, StringBuilder sbMethodCode) {
        if (doCreateJavaDocs) {
            sbJavaDocs.append(sbRespDocs);
            if (sbJavaDocs.length() > 0) {
                openJavaDocs(sbCode, true);
                sbCode.append(sbJavaDocs);
                closeJavaDocs(sbCode);
            }
            sbCode.append(sbMethodCode);
        }
    }

    private boolean isJavaDocNeeded(ContextInfo info) {
        return createJavaDocs && (generateInterfaces && !generateImpl
                || !generateInterfaces && generateImpl
                || generateImpl && info.isInterfaceGenerated());
    }

    private void openJavaDocs(StringBuilder sbDoc, boolean tab) {
        if (tab) {
            sbDoc.append(TAB);
        }
        sbDoc.append("/**").append(lineSeparator);
        if (tab) {
            sbDoc.append(TAB);
        }
    }

    private void closeJavaDocs(StringBuilder sbDoc) {
        sbDoc.append(" */").append(lineSeparator);
    }
    private void writeClassDocs(Element resourceEl, StringBuilder sbDoc) {
        String text = getDocText(resourceEl);
        if (text != null) {
            openJavaDocs(sbDoc, false);
            sbDoc.append(" * ").append(text).append(lineSeparator);
            closeJavaDocs(sbDoc);
        }
    }
    private StringBuilder startMethodDocs(Element methodEl) {
        StringBuilder sbDoc = new StringBuilder();
        String text = getDocText(methodEl);
        if (text != null) {
            sbDoc.append(" * ").append(text).append(lineSeparator).append(TAB);
        }
        return sbDoc;
    }

    private void writeMethodParamDocs(Element paramEl, String name, StringBuilder sbDoc) {
        String text = getDocText(paramEl);
        if (text != null) {
            sbDoc.append(" * @param ").append(name).append(' ').append(text)
                .append(lineSeparator).append(TAB);
        }
    }

    private void writeMethodThrowsDocs(Element paramEl, String name, StringBuilder sbDoc) {
        String text = getDocText(paramEl);
        if (text != null) {
            sbDoc.append(" * @throws ").append(name).append(' ').append(text).append(lineSeparator).append(TAB);
        }
    }

    private void writeMethodResponseDocs(Element responseEl, StringBuilder sbDoc) {
        String text = getDocText(responseEl);
        if (text != null) {
            sbDoc.append(" * @return ").append(text).append(lineSeparator).append(TAB);
        }
    }

    private String getDocText(Element el) {
        Element doc = DOMUtils.getFirstChildWithName(el, getWadlNamespace(), "doc");
        if (doc != null) {
            return DOMUtils.getContent(doc);
        }
        return null;
    }

    private void writeCustomHttpMethod(ContextInfo info,
                                       String classPackage,
                                       String methodName,
                                       StringBuilder mainCode,
                                       Set<String> mainImports) {

        mainCode.append('@').append(methodName);
        mainCode.append(lineSeparator);
        mainCode.append(TAB);

        final String className = methodName;
        if (info.getResourceClassNames().contains(className)) {
            return;
        }
        info.getResourceClassNames().add(className);

        StringBuilder sbMethodClassCode = new StringBuilder(256)
            .append("@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD})").append(lineSeparator)
            .append("@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)")
            .append(lineSeparator)
            .append("@jakarta.ws.rs.HttpMethod(\"").append(methodName).append("\")").append(lineSeparator)
            .append("public @interface ").append(methodName)
            .append(" {").append(lineSeparator).append(lineSeparator)
            .append('}');
        createJavaSourceFile(info.getSrcDir(), classPackage, className,
                             sbMethodClassCode, Collections.emptySet(), true);
    }

    private void writeSubresourceMethod(Element resourceEl,
                                        Set<String> imports,
                                        StringBuilder sbCode,
                                        ContextInfo info,
                                        String id,
                                        String suffixName) {
        boolean expandedQName = id.startsWith("{");
        QName qname = convertToQName(id, expandedQName);
        String packageName = possiblyConvertNamespaceURI(qname.getNamespaceURI(), expandedQName);

        String clsFullName = getSchemaClassName(packageName, info.getGrammarInfo(),
                qname.getLocalPart(), info.getTypeClassNames());
        int lastDotIndex = clsFullName == null ? -1 : clsFullName.lastIndexOf('.');
        String localName = clsFullName == null
            ? getClassName(qname.getLocalPart(), true, info.getTypeClassNames())
            : clsFullName.substring(lastDotIndex + 1);
        String subResponseNs = clsFullName == null ? getClassPackageName(packageName)
            : clsFullName.substring(0, lastDotIndex);
        Object parentNode = resourceEl.getParentNode();
        String parentId = parentNode instanceof Element
            ? ((Element)parentNode).getAttribute("id")
            : "";
        writeSubResponseType(id.equals(parentId), subResponseNs, localName,
                sbCode, imports);

        sbCode.append("get").append(localName).append(suffixName);
    }

    private static boolean isMethodMatched(Set<String> methodNames, String methodNameLowerCase, String id) {
        if (methodNames.isEmpty()) {
            return false;
        }
        return methodNames.contains(methodNameLowerCase)
            || !methodNameLowerCase.equals(id) && methodNames.contains(id.toLowerCase())
            || methodNames.size() == 1 && "*".equals(methodNames.iterator().next());
    }

    private boolean getRepsWithElements(List<Element> repElements,
                                              List<Element> requestRepsWithElements,
                                              GrammarInfo gInfo) {
        int duplicatesCount = 0;
        Set<String> elementRefs = new HashSet<>();
        for (Element el : repElements) {
            String value = el.getAttribute("element");
            if (!value.isEmpty()
                && (value.contains(":") || gInfo.isSchemaWithoutTargetNamespace())) {
                requestRepsWithElements.add(el);
                if (!elementRefs.add(value)) {
                    duplicatesCount++;
                }
            }
        }
        if (requestRepsWithElements.isEmpty()) {
            requestRepsWithElements.add(null);
        }
        return duplicatesCount > 0;
    }

    private boolean isRepWithElementAvailable(List<Element> repElements,
                                              GrammarInfo gInfo) {
        for (Element el : repElements) {
            String value = el.getAttribute("element");
            if (!value.isEmpty()
                && (value.contains(":") || gInfo.isSchemaWithoutTargetNamespace())) {
                return true;
            }
        }
        return false;
    }

    private List<Element> getParameters(Element resourceEl, List<Element> inheritedParams,
                                        boolean isSubresourceMethod) {
        List<Element> inParamElements = new ArrayList<>();
        List<Element> allParamElements = getWadlElements(resourceEl, "param");
        List<Element> newInheritedParams = inheritResourceParams ? new ArrayList<>() : Collections.emptyList();
        for (Element el : allParamElements) {
            boolean isResourceLevelParam = RESOURCE_LEVEL_PARAMS.contains(el.getAttribute("style"));
            if (isSubresourceMethod && isResourceLevelParam) {
                continue;
            }
            if (inheritResourceParams && isResourceLevelParam) {
                newInheritedParams.add(el);
            }
            inParamElements.add(el);
        }
        int inheritedCount = 0;
        for (Element inherited : inheritedParams) {
            boolean duplicate = false;
            for (Element in : inParamElements) {
                if (in.getAttribute("name").equals(inherited.getAttribute("name"))) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                if (inheritResourceParamsFirst && inheritedCount < inParamElements.size()) {
                    inParamElements.add(inheritedCount, inherited);
                } else {
                    inParamElements.add(inherited);
                }
                inheritedCount++;
            }
        }
        if (inheritResourceParamsFirst) {
            inheritedParams.addAll(newInheritedParams);
        } else {
            inheritedParams.addAll(0, newInheritedParams);
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
        sbCode.append(lineSeparator).append(TAB).append(TAB);
        sbCode.append("//TODO: implement").append(lineSeparator).append(TAB);
        if (responseTypeAvailable) {
            sbCode.append(TAB).append("return null;").append(lineSeparator).append(TAB);
        }
        sbCode.append('}');
    }

    private boolean addFormParameters(List<Element> inParamElements,
                                      Element requestEl,
                                      List<Element> repElements) {
        if (repElements.size() == 1) {
            String mediaType = repElements.get(0).getAttribute("mediaType");
            if (MediaType.APPLICATION_FORM_URLENCODED.equals(mediaType) || mediaType.startsWith("multipart/")) {
                if (!mediaTypesMap.containsKey(mediaType)) {
                    inParamElements.addAll(getWadlElements(repElements.get(0), "param"));
                }
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
        return repElements.isEmpty() ? null : repElements.get(0);
    }
    //CHECKSTYLE:OFF: ParameterNumber
    private boolean writeResponseType(List<Element> responseEls,
                                      Element requestRepWithElement,
                                      StringBuilder sbCode,
                                      StringBuilder sbRespDocs,
                                      Set<String> imports,
                                      ContextInfo info,
                                      boolean responseRequired,
                                      boolean suspendedAsync) {
    //CHECKSTYLE:ON: ParameterNumber
        Element okResponse = !suspendedAsync ? getOKResponse(responseEls) : null;

        final List<Element> repElements;
        if (okResponse != null) {
            if (sbRespDocs != null) {
                writeMethodResponseDocs(okResponse, sbRespDocs);
            }
            repElements = getWadlElements(okResponse, "representation");
        } else {
            repElements = Collections.emptyList();
        }
        if (!suspendedAsync && !responseRequired && responseEls.size() == 1 && generateResponseIfHeadersSet) {
            List<Element> outResponseParamElements =
                getParameters(responseEls.get(0), Collections.emptyList(), false);
            if (!outResponseParamElements.isEmpty()) {
                writeJaxrResponse(sbCode, imports);
                return true;
            }
        }
        if (repElements.isEmpty()) {
            if (useVoidForEmptyResponses && !responseRequired || suspendedAsync) {
                sbCode.append("void ");
                return false;
            }
            writeJaxrResponse(sbCode, imports);
            return true;
        }
        String elementType = null;
        if (!responseRequired) {
            List<Element> responseRepWithElements = new ArrayList<>();
            getRepsWithElements(repElements, responseRepWithElements, info.getGrammarInfo());

            Element responseRepWithElement = null;
            if (responseRepWithElements.size() == 1) {
                responseRepWithElement = responseRepWithElements.get(0);
            } else if (requestRepWithElement != null
                && supportMultipleRepsWithElements
                && responseRepWithElements.size() > 1) {
                String mediaType = requestRepWithElement.getAttribute("mediaType");
                for (Element el : responseRepWithElements) {
                    if (el.getAttribute("mediaType").equals(mediaType)) {
                        responseRepWithElement = el;
                        break;
                    }
                }
                if (responseRepWithElement == null) {
                    responseRepWithElement = responseRepWithElements.get(0);
                }
            }

            elementType = getElementRefName(
                               getActualRepElement(repElements, responseRepWithElement),
                               info, imports, true);
        }
        if (elementType != null) {
            sbCode.append(responseWrapper.wrap(elementType, imports)).append(' ');
        } else {
            writeJaxrResponse(sbCode, imports);
        }
        return true;
    }

    private void writeJaxrResponse(StringBuilder sbCode, Set<String> imports) {
        addImport(imports, Response.class.getName());
        sbCode.append(responseWrapper.wrap(Response.class, imports)).append(' ');
    }

    private static Element getOKResponse(List<Element> responseEls) {
        for (Element responseEl : responseEls) {
            String statusValue = responseEl.getAttribute("status");
            if (statusValue.isEmpty()) {
                return responseEl;
            }
            for (String status : statusValue.split("\\s")) {
                if (HTTP_OK_STATUSES.contains(status)) {
                    return responseEl;
                }
            }
        }
        return null;
    }

    private static List<Element> getErrorResponses(List<Element> responseEls) {
        final List<Element> result = new ArrayList<>();
        for (Element responseEl : responseEls) {
            if (responseEl.hasAttribute("status")) {
                for (String statusValue : responseEl.getAttribute("status").split("\\s")) {
                    if (400 <= Integer.parseInt(statusValue)) {
                        result.add(responseEl);
                        break;
                    }
                }
            }
        }
        return result;
    }

    private void writeSubResponseType(boolean recursive, String ns, String localName,
                                      StringBuilder sbCode, Set<String> imports) {
        if (!recursive && !ns.isEmpty()) {
            addImport(imports, ns + '.' + localName);
        }
        sbCode.append(localName).append(' ');
    }
    //CHECKSTYLE:OFF: ParameterNumber
    private void writeRequestTypes(Element requestEl,
                                   String classPackage,
                                   Element repElement,
                                   List<Element> inParamEls,
                                   boolean jaxpRequired,
                                   StringBuilder sbCode,
                                   StringBuilder sbMethodDocs,
                                   Set<String> imports,
                                   ContextInfo info,
                                   boolean suspendedAsync) {
    //CHECKSTYLE:ON: ParameterNumber
        boolean form = false;
        boolean multipart = false;
        boolean formOrMultipartParamsAvailable = false;
        String requestMediaType = null;
        int currentSize = 0;
        if (requestEl != null) {
            inParamEls.addAll(getWadlElements(requestEl, "param"));
            currentSize = inParamEls.size();
            List<Element> repElements = getWadlElements(requestEl, "representation");
            form = addFormParameters(inParamEls, requestEl, repElements);
            if (form) {
                formOrMultipartParamsAvailable = currentSize < inParamEls.size();
                requestMediaType = repElements.get(0).getAttribute("mediaType");
                multipart = requestMediaType.startsWith("multipart/");
            }
        }

        boolean writeAnnotations = writeAnnotations(info.isInterfaceGenerated());
        for (int i = 0; i < inParamEls.size(); i++) {

            Element paramEl = inParamEls.get(i);

            Class<?> paramAnn = getParamAnnotation(paramEl.getAttribute("style"));
            if (i >= currentSize && paramAnn == QueryParam.class && formOrMultipartParamsAvailable) {
                paramAnn = !multipart ? FormParam.class : Multipart.class;
            }
            String name = paramEl.getAttribute("name");
            boolean enumCreated = false;
            if (generateEnums) {
                List<Element> options =
                    DOMUtils.findAllElementsByTagNameNS(paramEl, getWadlNamespace(), "option");
                if (!options.isEmpty()) {
                    generateEnumClass(getTypicalClassName(name), options, info.getSrcDir(), classPackage);
                    enumCreated = true;
                }
            }
            if (writeAnnotations) {
                String required = paramEl.getAttribute("required");
                if (Multipart.class.equals(paramAnn) && "false".equals(required)) {
                    writeAnnotation(sbCode, imports, paramAnn, null)
                        .append("(value = \"").append(name).append("\", required = false").append(')');
                } else {
                    writeAnnotation(sbCode, imports, paramAnn, name);
                }
                sbCode.append(' ');
                String defaultVal = paramEl.getAttribute("default");
                if (!defaultVal.isEmpty()) {
                    writeAnnotation(sbCode, imports, DefaultValue.class, defaultVal)
                        .append(' ');
                }
            }
            boolean isRepeating = isRepeatingParam(paramEl);
            String type = enumCreated ? getTypicalClassName(name)
                : getPrimitiveType(paramEl, info, imports);
            if (OPTIONAL_PARAMS.contains(paramAnn)
                && (isRepeating || !Boolean.valueOf(paramEl.getAttribute("required")))
                && AUTOBOXED_PRIMITIVES_MAP.containsKey(type)) {
                type = AUTOBOXED_PRIMITIVES_MAP.get(type);
            }
            type = addListIfRepeating(type, isRepeating, imports);
            String paramName;
            if (JavaUtils.isJavaKeyword(name)) {
                paramName = name.concat("_arg");
            } else {
                paramName = name.replaceAll("[:\\.\\-]", "_");
            }
            String javaParamName = StringUtils.uncapitalize(paramName);
            sbCode.append(type).append(' ').append(javaParamName);
            if (i + 1 < inParamEls.size()) {
                sbCode.append(", ");
                if (i + 1 >= 4 && ((i + 1) % 4) == 0) {
                    sbCode.append(lineSeparator).append(TAB).append(TAB).append(TAB).append(TAB);
                }
            }
            if (sbMethodDocs != null) {
                writeMethodParamDocs(paramEl, javaParamName, sbMethodDocs);
            }
        }
        String elementParamType = null;
        String elementParamName = null;
        boolean writeBeanValidation = false;
        if (!form) {
            if (!jaxpRequired) {
                elementParamType = getElementRefName(repElement, info, imports, false);
                if (elementParamType != null) {
                    if (writeAnnotations && supportBeanValidation
                        && isRepWithElementAvailable(Collections.singletonList(repElement), info.getGrammarInfo())) {
                        writeBeanValidation = true;
                    }
                    int lastIndex = elementParamType.lastIndexOf('.');
                    if (lastIndex != -1) {
                        elementParamType = elementParamType.substring(lastIndex + 1);
                    }
                    elementParamName = elementParamType.toLowerCase();
                } else if (repElement != null) {
                    Element param = DOMUtils.getFirstChildWithName(repElement, getWadlNamespace(), "param");
                    if (param != null) {
                        elementParamType = getPrimitiveType(param, info, imports);
                        elementParamName = param.getAttribute("name");
                    }
                }
            } else {
                addImport(imports, Source.class.getName());
                elementParamType = Source.class.getSimpleName();
                elementParamName = "source";
            }
        } else if (!formOrMultipartParamsAvailable) {
            if (requestMediaType != null && mediaTypesMap.containsKey(requestMediaType)) {
                elementParamType = addImportsAndGetSimpleName(imports, mediaTypesMap.get(requestMediaType));
            } else {
                String fullClassName = !multipart ? MultivaluedMap.class.getName() : MultipartBody.class.getName();
                elementParamType = addImportsAndGetSimpleName(imports, fullClassName);
            }
            elementParamName = !multipart ? "map" : "body";
        }
        if (elementParamType != null) {
            if (!inParamEls.isEmpty()) {
                sbCode.append(", ");
            }
            if (writeBeanValidation) {
                addImport(imports, BEAN_VALID_FULL_NAME);
                sbCode.append('@').append(BEAN_VALID_SIMPLE_NAME).append(' ');
            }

            sbCode.append(elementParamType).append(' ').append(elementParamName);
        }
        if (sbMethodDocs != null && repElement != null) {
            writeMethodParamDocs(repElement, elementParamName, sbMethodDocs);
        }
        if (suspendedAsync) {
            if (!inParamEls.isEmpty() || elementParamType != null) {
                sbCode.append(", ");
            }
            if (writeAnnotations) {
                writeAnnotation(sbCode, imports, Suspended.class, null)
                    .append(' ');
            }
            addImport(imports, AsyncResponse.class.getName());
            sbCode.append(AsyncResponse.class.getSimpleName()).append(' ').append("async");
        }
    }

    private boolean isRepeatingParam(Element paramEl) {
        return Boolean.valueOf(paramEl.getAttribute("repeating"));
    }

    private String addListIfRepeating(String type, boolean isRepeating, Set<String> imports) {
        if (isRepeating) {
            addImport(imports, List.class.getName());
            type = "List<" + type + ">";
        }
        return type;
    }

    private Class<?> getParamAnnotation(String paramStyle) {
        Class<?> paramAnn = PARAM_ANNOTATIONS.get(paramStyle);
        if (paramAnn == null) {
            String error = "Unsupported parameter style: " + paramStyle;
            if (PLAIN_PARAM_STYLE.equals(paramStyle)) {
                error += ", plain style parameters have to be wrapped by representations";
            }
            throw new ValidationException(error);
        }
        return paramAnn;
    }

    private void generateEnumClass(String clsName, List<Element> options, File src, String classPackage) {
        StringBuilder sbCode = new StringBuilder(512);
        sbCode.append("public enum ").append(clsName);
        openBlock(sbCode);

        for (int i = 0; i < options.size(); i++) {
            String value = options.get(i).getAttribute("value");
            sbCode.append(TAB).append(value.toUpperCase().replaceAll("[\\,\\-]", "_"))
                .append("(\"").append(value).append("\")");
            if (i + 1 < options.size()) {
                sbCode.append(',');
            } else {
                sbCode.append(';');
            }
            sbCode.append(lineSeparator);
        }

        sbCode.append(TAB).append("private String value;").append(lineSeparator);
        sbCode.append(TAB).append("private ").append(clsName).append("(String v)");
        openBlock(sbCode);
        tab(sbCode, 2).append("this.value = v;").append(lineSeparator);
        tabCloseBlock(sbCode, 1);

        sbCode.append(TAB).append("public static ")
            .append(clsName).append(" fromString(String value)");
        openBlock(sbCode);
        tab(sbCode, 2);
        sbCode.append("if (").append("value").append(" != null)");
        openBlock(sbCode);
        tab(sbCode, 3);
        sbCode.append("for (").append(clsName).append(" v : ")
            .append(clsName).append(".values())");
        openBlock(sbCode);
        tab(sbCode, 4);
        sbCode.append("if (value.equalsIgnoreCase(v.value))");
        openBlock(sbCode);
        tab(sbCode, 5);
        sbCode.append("return v;").append(lineSeparator);
        tabCloseBlock(sbCode, 4);
        tabCloseBlock(sbCode, 3);
        tabCloseBlock(sbCode, 2);
        tab(sbCode, 2);
        sbCode.append("throw new IllegalArgumentException();").append(lineSeparator);
        tabCloseBlock(sbCode, 1);
        sbCode.append('}');
        createJavaSourceFile(src, classPackage, clsName, sbCode, Collections.emptySet(), false);
    }

    private static StringBuilder tab(StringBuilder sb, int count) {
        for (int i = 0; i < count; i++) {
            sb.append(TAB);
        }
        return sb;
    }

    private StringBuilder tabCloseBlock(StringBuilder sb, int count) {
        tab(sb, count).append('}').append(lineSeparator);
        return sb;
    }

    private StringBuilder openBlock(StringBuilder sb) {
        sb.append(" {").append(lineSeparator);
        return sb;
    }

    private static String getTypicalClassName(String name) {
        String theName = name.toUpperCase();
        if (theName.length() == 1) {
            return theName;
        }
        theName = theName.substring(0, 1) + theName.substring(1).toLowerCase();
        return theName.replaceAll("[\\.\\-]", "");
    }

    private List<Element> getWadlElements(Element parent, String name) {
        List<Element> elements = parent != null
            ? DOMUtils.getChildrenWithName(parent, getWadlNamespace(), name)
            : Collections.emptyList();
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

    private String getPrimitiveType(Element paramEl, ContextInfo info, Set<String> imports) {
        final String defaultValue = "String";
        String type = paramEl.getAttribute("type");
        if (type.isEmpty()) {
            return defaultValue;
        }

        String[] pair = type.split(":");
        if (pair.length == 2) {
            if (XSD_SPECIFIC_TYPE_MAP.containsKey(pair[1])) {
                String expandedName = "{" + Constants.URI_2001_SCHEMA_XSD + "}" + pair[1];
                if (schemaTypeMap.containsKey(expandedName)) {
                    return addImportsAndGetSimpleName(imports, schemaTypeMap.get(expandedName));
                }

                String xsdType = XSD_SPECIFIC_TYPE_MAP.get(pair[1]);
                return addImportsAndGetSimpleName(imports, xsdType);
            }

            String value = pair[1].replaceAll("[\\-\\_]", "");
            return convertRefToClassName(pair[0], value, defaultValue, info, imports);
        }
        return addImportsAndGetSimpleName(imports, type);

    }

    private String convertRefToClassName(String prefix,
                                         String actualValue,
                                         String defaultValue,
                                         ContextInfo info,
                                         Set<String> imports) {
        GrammarInfo gInfo = info.getGrammarInfo();
        if (gInfo != null) {
            String namespace = gInfo.getNsMap().get(prefix);
            if (namespace != null || prefix.isEmpty() && gInfo.isSchemaWithoutTargetNamespace()) {
                String theNs = namespace != null ? namespace : "";
                String packageName = getPackageFromNamespace(theNs);
                String clsName = getSchemaClassName(packageName, gInfo, actualValue,
                                                    info.getTypeClassNames());

                if (clsName == null) {
                    clsName = schemaTypeMap.get("{" + namespace + "}" + actualValue);
                }
                if (clsName != null) {
                    return addImportsAndGetSimpleName(imports, clsName);
                }

            }
        }
        return defaultValue;
    }

    private String addImportsAndGetSimpleName(Set<String> imports, String clsName) {
        String originalName = clsName;
        int typeIndex = clsName.lastIndexOf("..");
        if (typeIndex != -1) {
            clsName = clsName.substring(0, typeIndex);
        }
        addImport(imports, clsName);
        int index = clsName.lastIndexOf('.');

        if (index != -1) {
            clsName = clsName.substring(index + 1);
        }
        if (typeIndex != -1) {
            clsName = clsName + "<" + originalName.substring(typeIndex + 2) + ">";
        }
        return clsName;
    }

    private String checkGenericType(String clsName) {
        if (clsName != null) {
            int typeIndex = clsName.lastIndexOf("..");
            if (typeIndex != -1) {
                clsName = clsName.substring(0, typeIndex)
                    + "<"
                    + clsName.substring(typeIndex + 2)
                    + ">";
            }
        }
        return clsName;
    }

    private String getElementRefName(Element repElement,
                                     ContextInfo info,
                                     Set<String> imports,
                                     boolean checkPrimitive) {
        if (repElement == null) {
            return null;
        }
        String elementRef = repElement.getAttribute("element");

        if (!elementRef.isEmpty()) {
            String[] pair = elementRef.split(":");
            if (pair.length == 2
                || pair.length == 1 && info.getGrammarInfo().isSchemaWithoutTargetNamespace()) {
                String ns = pair.length == 1 ? "" : pair[0];
                String name = pair.length == 1 ? pair[0] : pair[1];
                return convertRefToClassName(ns, name, null, info, imports);
            }
        } else {
            // try mediaTypesMap first
            String mediaType = repElement.getAttribute("mediaType");
            if (mediaTypesMap.containsKey(mediaType)) {
                return addImportsAndGetSimpleName(imports, mediaTypesMap.get(mediaType));
            }
            if (checkPrimitive) {
                Element paramEl = DOMUtils.getFirstChildWithName(repElement, getWadlNamespace(), "param");
                if (paramEl != null) {
                    String type = getPrimitiveType(paramEl, info, imports);
                    return addListIfRepeating(type, isRepeatingParam(paramEl), imports);
                }
            }
        }
        return null;
    }

    private String getSchemaClassName(String packageName, GrammarInfo gInfo, String localName,
                                      Set <String> typeClassNames) {
        String clsName = matchClassName(typeClassNames, packageName, localName);
        if (clsName == null && gInfo != null) {
            String prefixedElementTypeName = gInfo.getElementTypeMap().get(localName);
            if (prefixedElementTypeName != null) {
                String[] pair = prefixedElementTypeName.split(":");
                String elementTypeName = pair.length == 2 ? pair[1] : pair[0];
                clsName = matchClassName(typeClassNames, packageName, elementTypeName);
                if (clsName == null && jaxbClassNameSuffix != null) {
                    clsName = matchClassName(typeClassNames, packageName,
                                             elementTypeName + jaxbClassNameSuffix);
                }
                if (clsName == null && elementTypeName.contains("_")) {
                    String elementTypeNameWithoutUnderscore = elementTypeName.replaceAll("_", "");
                    clsName = matchClassName(typeClassNames, packageName, elementTypeNameWithoutUnderscore);
                    if (clsName == null && jaxbClassNameSuffix != null) {
                        clsName = matchClassName(typeClassNames, packageName,
                                                 elementTypeNameWithoutUnderscore + jaxbClassNameSuffix);
                    }
                }
                if (clsName == null && pair.length == 2) {
                    String namespace = gInfo.getNsMap().get(pair[0]);
                    if (namespace != null) {
                        packageName = getPackageFromNamespace(namespace);
                        clsName = matchClassName(typeClassNames, packageName, elementTypeName);
                        //CHECKSTYLE:OFF: NestedIfDepth
                        if (clsName == null && jaxbClassNameSuffix != null) {
                            clsName = matchClassName(typeClassNames, packageName,
                                                     elementTypeName + jaxbClassNameSuffix);
                        }
                        //CHECKSTYLE:ON: NestedIfDepth
                    }
                }

            }
        }
        if (clsName == null && javaTypeMap != null) {
            clsName = checkGenericType(javaTypeMap.get(packageName + "." + localName));
        }
        return clsName;
    }

    private String matchClassName(Set<String> typeClassNames, String packageName, String localName) {
        if (localName == null) {
            return null;
        }
        String clsName = packageName + "." + localName.toLowerCase();
        for (String type : typeClassNames) {
            if (type.equalsIgnoreCase(clsName)) {
                return type;
            }
        }
        return null;
    }

    private void writeFormatAnnotations(List<Element> repElements, StringBuilder sbCode,
                                        Set<String> imports,
                                        boolean inRep,
                                        Element requestRepWithElement) {
        if (repElements.isEmpty()) {
            return;
        }
        Class<?> cls = inRep ? Consumes.class : Produces.class;
        addImport(imports, cls.getName());
        sbCode.append('@').append(cls.getSimpleName()).append('(');
        if (repElements.size() > 1) {
            sbCode.append('{');
        }
        boolean first = true;
        StringBuilder mediaTypes = new StringBuilder("");
        for (int i = 0; i < repElements.size(); i++) {
            String mediaType = repElements.get(i).getAttribute("mediaType");
            if (!StringUtils.isEmpty(mediaType) && mediaTypes.indexOf(mediaType) < 0) {
                if (!inRep
                    && supportMultipleRepsWithElements
                    && repElements.size() > 1
                    && requestRepWithElement != null
                    && !requestRepWithElement.getAttribute("mediaType").equals(mediaType)) {
                    continue;
                }

                if (!first) {
                    mediaTypes.append(", ");
                }
                first = false;
                mediaTypes.append('"').append(mediaType).append('"');
            }
        }
        sbCode.append(mediaTypes.toString());
        if (repElements.size() > 1) {
            sbCode.append(" }");
        }
        sbCode.append(')');
        sbCode.append(lineSeparator).append(TAB);
    }

    private void writeThrows(List<Element> responseEls, StringBuilder sbCode, StringBuilder sbMethodDocs,
            Set<String> imports, ContextInfo info) {
        final List<Element> throwsParamEls = new ArrayList<>();
        for (Element errorResp : getErrorResponses(responseEls)) {
            for (Element errorRep : getWadlElements(errorResp, "representation")) {
                throwsParamEls.addAll(getWadlElements(errorRep, "param"));
            }
        }
        if (!throwsParamEls.isEmpty()) {
            sbCode.append(" throws ");
            boolean comma = false;
            for (Element paramEl : throwsParamEls) {
                if (!comma) {
                    comma = true;
                } else {
                    sbCode.append(", ");
                }
                final String javaThrowsName = getPrimitiveType(paramEl, info, imports);
                sbCode.append(javaThrowsName);
                if (sbMethodDocs != null) {
                    writeMethodThrowsDocs(paramEl, javaThrowsName, sbMethodDocs);
                }
            }
        }
    }

    private void createJavaSourceFile(File src, String classPackage, String className, StringBuilder sbCode,
        Set<String> imports, boolean serviceClass) {
        if (serviceClass) {
            generatedServiceClasses.add(classPackage + '.' + className);
        }

        java.nio.file.Path currentDir = src.toPath().resolve(classPackage.replace('.', File.separatorChar));
        try (Writer writer = Files.newBufferedWriter(
            Files.createDirectories(currentDir).resolve(className + ".java"),
            encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding))) {

            writer.write(getClassComment());
            writer.write(lineSeparator);

            writer.write("package ");
            writer.write(classPackage);
            writer.write(';');
            writer.write(lineSeparator);
            writer.write(lineSeparator);

            for (String clsName : imports) {
                int index = clsName.lastIndexOf('.');
                if (index != -1 && clsName.substring(0, index).equals(classPackage)) {
                    continue;
                }
                writer.write("import ");
                writer.write(clsName);
                writer.write(';');
                writer.write(lineSeparator);
            }
            writer.write(lineSeparator);

            writer.write(sbCode.toString());
        } catch (java.nio.file.NoSuchFileException ex) {
            LOG.warning(ex.getMessage() + " is not found");
        } catch (IOException ex) {
            LOG.warning("Problem writing into " + ex.getMessage());
        }
    }

    private Application readWadl() {
        Element wadlElement = readDocument(wadlPath);
        if (validateWadl) {
            final WadlValidationErrorHandler errorHandler = new WadlValidationErrorHandler();
            try {
                Schema s = SchemaHandler.createSchema(
                        Arrays.asList("classpath:/schemas/wsdl/xml.xsd", "classpath:/schemas/wadl/wadl.xsd"), null,
                        bus);
                Validator v = s.newValidator();
                v.setErrorHandler(errorHandler);
                v.validate(new DOMSource(wadlElement));
            } catch (Exception ex) {
                throw new ValidationException("WADL document can not be validated", ex);
            }
            if (errorHandler.isValidationFailed()) {
                throw new ValidationException("WADL document is not valid.");
            }
        }
        return new Application(wadlElement, wadlPath);
    }

    private void generateClassesFromSchema(JCodeModel codeModel, File src) {
        try {
            Object writer = JAXBUtils.createFileCodeWriter(src, encoding == null
                ? StandardCharsets.UTF_8.name() : encoding);
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

        List<SchemaInfo> schemas = new ArrayList<>();
        List<Element> schemasEls = DOMUtils.getChildrenWithName(grammarEls.get(0),
                                                                Constants.URI_2001_SCHEMA_XSD, "schema");
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
                if (!URI.create(href).isAbsolute() && app.getWadlPath() != null) {
                    String baseWadlPath = getBaseWadlPath(app.getWadlPath());
                    if  (!href.startsWith("/") && !href.contains("..")) {
                        schemaURI = baseWadlPath + href;
                    } else {
                        try {
                            schemaURI = new URL(new URL(baseWadlPath), href).toString();
                        } catch (Exception ex) {
                            schemaURI = URI.create(baseWadlPath).resolve(href).toString();
                        }
                    }
                } else {
                    schemaURI = href;
                }
            }
            schemas.add(createSchemaInfo(readDocument(schemaURI), schemaURI));
        }
        return schemas;
    }

    private static String getBaseWadlPath(String docPath) {
        int lastSep = docPath.lastIndexOf('/');
        return lastSep != -1 ? docPath.substring(0, lastSep + 1) : docPath;
    }

    private SchemaInfo createSchemaInfo(Element schemaEl, String systemId) {
        SchemaInfo info = new SchemaInfo(schemaEl.getAttribute("targetNamespace"));

        info.setElement(schemaEl);
        info.setSystemId(systemId);
        // Lets try to read the schema to deal with the possible
        // eviction of the DOM element from the memory
        try {
            XmlSchema xmlSchema = schemaCollection.read(schemaEl, systemId);
            info.setSchema(xmlSchema);
        } catch (Exception ex) {
            // may be due to unsupported resolvers for protocols like
            // classpath: or not the valid schema definition, may not be critical
            // for the purpose of the schema compilation.
        }

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
        }
        return null;
    }

    private Element readDocument(String href) {
        try {
            InputStream is = null;
            if (!href.startsWith("http")) {
                is = ResourceUtils.getResourceStream(href, bus);
            }
            if (is == null) {
                URL url = new URL(href);
                if (href.startsWith("https") && authentication != null) {
                    is = SecureConnectionHelper.getStreamFromSecureConnection(url, authentication);
                } else {
                    is = url.openStream();
                }
            }
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return StaxUtils.read(new InputSource(reader)).getDocumentElement();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Resource " + href + " can not be read");
        }
    }

    private JCodeModel createCodeModel(List<SchemaInfo> schemaElements, Set<String> type) {

        SchemaCompiler compiler = createCompiler(type);
        Object elForRun = ReflectionInvokationHandler
            .createProxyWrapper(new InnerErrorListener(),
                            JAXBUtils.getParamClass(compiler, "setErrorListener"));
        compiler.setErrorListener(elForRun);
        compiler.setEntityResolver(OASISCatalogManager.getCatalogManager(bus)
                                       .getEntityResolver());
        if (!compilerArgs.isEmpty()) {
            compiler.getOptions().addGrammar(new InputSource("null"));
            compiler.getOptions().parseArguments(compilerArgs.toArray(new String[0]));
        }
        addSchemas(schemaElements, compiler);
        for (InputSource is : bindingFiles) {
            compiler.getOptions().addBindFile(is);
        }

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
                    URL url = ResourceUtils.getClasspathResourceURL(resource,
                                                                    SourceGenerator.class,
                                                                    bus);
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
        return new TreeSet<String>(importsComparator == null ? new DefaultImportsComparator() : importsComparator);
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

    public void setInheritResourceParams(boolean inherit) {
        this.inheritResourceParams = inherit;
    }

    public void setInheritResourceParamsFirst(boolean inherit) {
        this.inheritResourceParamsFirst = inherit;
    }

    public void setSchemaPackageMap(Map<String, String> map) {
        this.schemaPackageMap = map;
    }

    public void setJavaTypeMap(Map<String, String> map) {
        this.javaTypeMap = map;
    }

    public void setSchemaTypeMap(Map<String, String> map) {
        this.schemaTypeMap = map;
    }

    public void setMediaTypeMap(Map<String, String> map) {
        this.mediaTypesMap = map;
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

    public void setValidateWadl(boolean validateWadl) {
        this.validateWadl = validateWadl;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setCreateJavaDocs(boolean createJavaDocs) {
        this.createJavaDocs = createJavaDocs;
    }

    public void setSupportBeanValidation(boolean supportBeanValidation) {
        this.supportBeanValidation = supportBeanValidation;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public void setJaxbClassNameSuffix(String jaxbClassNameSuffix) {
        this.jaxbClassNameSuffix = jaxbClassNameSuffix;
    }

    public void setRx(String rx) {
        this.rx = rx;
        this.responseWrapper = ResponseWrapper.create(rx);
    }

    private static class GrammarInfo {
        private Map<String, String> nsMap = new HashMap<>();
        private Map<String, String> elementTypeMap = new HashMap<>();
        private boolean noTargetNamespace;
        GrammarInfo() {

        }

        GrammarInfo(Map<String, String> nsMap,
                    Map<String, String> elementTypeMap,
                    boolean noTargetNamespace) {
            this.nsMap = nsMap;
            this.elementTypeMap = elementTypeMap;
            this.noTargetNamespace = noTargetNamespace;
        }

        public Map<String, String> getNsMap() {
            return nsMap;
        }

        public Map<String, String> getElementTypeMap() {
            return elementTypeMap;
        }

        public boolean isSchemaWithoutTargetNamespace() {
            return noTargetNamespace;
        }
    }

    private static final class DefaultImportsComparator implements Comparator<String> {
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

    private static class Application {
        private Element appElement;
        private String wadlPath;
        Application(Element appElement, String wadlPath) {
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
        private Set<String> resourceClassNames = new HashSet<>();
        private Application rootApp;
        private File srcDir;
        private List<Element> inheritedParams = new ArrayList<>();

        ContextInfo(Application rootApp,
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
        public List<Element> getInheritedParams() {
            return inheritedParams;
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

    private static final class WadlValidationErrorHandler implements ErrorHandler {
        private int fatalErrors;
        private int errors;
        @Override
        public void warning(SAXParseException exception) throws SAXException {
            LOG.log(Level.WARNING, exception.toString());
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            LOG.log(Level.WARNING, exception.toString());
            errors++;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            LOG.log(Level.WARNING, exception.toString());
            fatalErrors++;
        }
        private boolean isValidationFailed() {
            return fatalErrors > 0 || errors > 0;
        }
    }
}
