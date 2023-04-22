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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xml.sax.InputSource;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.URIParserUtil;
import org.apache.cxf.tools.common.AbstractCXFToolContainer;
import org.apache.cxf.tools.common.ClassUtils;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.common.toolspec.parser.BadUsageException;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.wadlto.WadlToolConstants;
import org.apache.cxf.tools.wadlto.jaxb.CustomizationParser;

public class JAXRSContainer extends AbstractCXFToolContainer {
    private static final String TOOL_NAME = "wadl2java";
    private static final String EPR_TYPE_KEY = "org.w3._2005._08.addressing.EndpointReference";
    private static final Map<String, String> DEFAULT_JAVA_TYPE_MAP = Collections.singletonMap(EPR_TYPE_KEY,
        "jakarta.xml.ws.wsaddressing.W3CEndpointReference");

    public JAXRSContainer(ToolSpec toolspec) throws Exception {
        super(TOOL_NAME, toolspec);
    }

    public void execute() throws ToolException {
        if (hasInfoOption()) {
            return;
        }

        buildToolContext();

        processWadl();

    }

    public void execute(boolean exitOnFinish) throws ToolException {
        try {
            if (getArgument() != null) {
                super.execute(exitOnFinish);
            }
            execute();

        } catch (ToolException ex) {
            if (ex.getCause() instanceof BadUsageException) {
                printUsageException(TOOL_NAME, (BadUsageException)ex.getCause());
            }
            throw ex;
        } catch (Exception ex) {
            throw new ToolException(ex);
        } finally {
            tearDown();
        }
    }

    public void buildToolContext() {
        getContext();
        context.addParameters(getParametersMap(getArrayKeys()));
        if (context.get(WadlToolConstants.CFG_OUTPUTDIR) == null) {
            context.put(WadlToolConstants.CFG_OUTPUTDIR, ".");
        }
        setPackageAndNamespaces();
    }

    public Set<String> getArrayKeys() {
        Set<String> set = new HashSet<>();
        set.add(WadlToolConstants.CFG_BINDING);
        set.add(WadlToolConstants.CFG_SCHEMA_PACKAGENAME);
        set.add(WadlToolConstants.CFG_SCHEMA_TYPE_MAP);
        set.add(WadlToolConstants.CFG_MEDIA_TYPE_MAP);
        set.add(WadlToolConstants.CFG_XJC_ARGS);
        return set;
    }

    private void processWadl() {
        File outDir = new File((String)context.get(WadlToolConstants.CFG_OUTPUTDIR));
        String wadlURL = getAbsoluteWadlURL();
        String authentication = (String)context.get(WadlToolConstants.CFG_AUTHENTICATION);

        SourceGenerator sg = new SourceGenerator();
        sg.setBus(getBus());

        boolean generateImpl = context.optionSet(WadlToolConstants.CFG_IMPL);
        sg.setGenerateImplementation(generateImpl);
        if (generateImpl) {
            sg.setGenerateInterfaces(context.optionSet(WadlToolConstants.CFG_INTERFACE));
        }
        sg.setPackageName((String)context.get(WadlToolConstants.CFG_PACKAGENAME));
        sg.setResourceName((String)context.get(WadlToolConstants.CFG_RESOURCENAME));
        sg.setEncoding((String)context.get(WadlToolConstants.CFG_ENCODING));
        sg.setAuthentication(authentication);

        String wadlNs = (String)context.get(WadlToolConstants.CFG_WADL_NAMESPACE);
        if (wadlNs != null) {
            sg.setWadlNamespace(wadlNs);
        }
        sg.setJaxbClassNameSuffix((String)context.get(WadlToolConstants.CFG_JAXB_CLASS_NAME_SUFFIX));

        sg.setSupportMultipleXmlReps(context.optionSet(WadlToolConstants.CFG_MULTIPLE_XML_REPS));
        sg.setSupportBeanValidation(context.optionSet(WadlToolConstants.CFG_BEAN_VALIDATION));
        sg.setCreateJavaDocs(context.optionSet(WadlToolConstants.CFG_CREATE_JAVA_DOCS));
        // set the base path
        sg.setWadlPath(wadlURL);

        CustomizationParser parser = new CustomizationParser(context);
        parser.parse(context);

        List<InputSource> bindingFiles = parser.getJaxbBindings();
        sg.setBindingFiles(bindingFiles);

        sg.setCompilerArgs(parser.getCompilerArgs());

        List<InputSource> schemaPackageFiles = parser.getSchemaPackageFiles();
        sg.setSchemaPackageFiles(schemaPackageFiles);
        sg.setSchemaPackageMap(context.getNamespacePackageMap());

        sg.setJavaTypeMap(DEFAULT_JAVA_TYPE_MAP);
        sg.setSchemaTypeMap(getSchemaTypeMap());
        sg.setMediaTypeMap(getMediaTypeMap());

        sg.setSuspendedAsyncMethods(getSuspendedAsyncMethods());
        sg.setResponseMethods(getResponseMethods());
        sg.setOnewayMethods(getOnewayMethods());

        sg.setGenerateEnums(context.optionSet(WadlToolConstants.CFG_GENERATE_ENUMS));
        sg.setValidateWadl(context.optionSet(WadlToolConstants.CFG_VALIDATE_WADL));
        sg.setRx(context.get(WadlToolConstants.CFG_RX, String.class));
        boolean inheritResourceParams = context.optionSet(WadlToolConstants.CFG_INHERIT_PARAMS);
        sg.setInheritResourceParams(inheritResourceParams);
        if (inheritResourceParams) {
            sg.setInheritResourceParamsFirst(isInheritResourceParamsFirst());
        }
        sg.setSkipSchemaGeneration(context.optionSet(WadlToolConstants.CFG_NO_TYPES));

        boolean noVoidForEmptyResponses = context.optionSet(WadlToolConstants.CFG_NO_VOID_FOR_EMPTY_RESPONSES);
        if (noVoidForEmptyResponses) {
            sg.setUseVoidForEmptyResponses(false);
        }

        sg.setGenerateResponseIfHeadersSet(context.optionSet(WadlToolConstants.CFG_GENERATE_RESPONSE_IF_HEADERS_SET));

        // generate
        String codeType = context.optionSet(WadlToolConstants.CFG_TYPES)
            ? SourceGenerator.CODE_TYPE_GRAMMAR : SourceGenerator.CODE_TYPE_PROXY;
        sg.generateSource(outDir, codeType);

        // compile
        if (context.optionSet(WadlToolConstants.CFG_COMPILE)) {
            ClassCollector collector = createClassCollector();
            List<String> generatedServiceClasses = sg.getGeneratedServiceClasses();
            for (String className : generatedServiceClasses) {
                int index = className.lastIndexOf('.');
                collector.addServiceClassName(className.substring(0, index),
                                              className.substring(index + 1),
                                              className);
            }

            List<String> generatedTypeClasses = sg.getGeneratedTypeClasses();
            for (String className : generatedTypeClasses) {
                int index = className.lastIndexOf('.');
                collector.addTypesClassName(className.substring(0, index),
                                              className.substring(index + 1),
                                              className);
            }

            context.put(ClassCollector.class, collector);
            new ClassUtils().compile(context);
        }

    }

    protected String getAbsoluteWadlURL() {
        String wadlURL = (String)context.get(WadlToolConstants.CFG_WADLURL);
        String absoluteWadlURL = URIParserUtil.getAbsoluteURI(wadlURL);
        context.put(WadlToolConstants.CFG_WADLURL, absoluteWadlURL);
        return absoluteWadlURL;
    }

    public Set<String> getSuspendedAsyncMethods() {
        return parseMethodList(WadlToolConstants.CFG_SUSPENDED_ASYNC);
    }

    public Set<String> getResponseMethods() {
        return parseMethodList(WadlToolConstants.CFG_GENERATE_RESPONSE_FOR_METHODS);
    }

    public Set<String> getOnewayMethods() {
        return parseMethodList(WadlToolConstants.CFG_ONEWAY);
    }

    private Set<String> parseMethodList(String paramName) {
        Object value = context.get(paramName);
        if (value != null) {
            Set<String> methods = new HashSet<>();
            String[] values = value.toString().split(",");
            for (String s : values) {
                String actual = s.trim();
                if (!actual.isEmpty()) {
                    methods.add(actual.toLowerCase());
                }
            }
            if (methods.isEmpty()) {
                methods.add("*");
            }
            return methods;
        }
        return Collections.emptySet();
    }
    private boolean isInheritResourceParamsFirst() {
        Object value = context.get(WadlToolConstants.CFG_INHERIT_PARAMS);
        if (StringUtils.isEmpty((String)value)) {
            return true;
        }
        return "first".equals(value.toString().trim());
    }

    //TODO: this belongs to JAXB Databinding, should we just reuse
    // org.apache.cxf.tools.wsdlto.databinding.jaxb ?
    private void setPackageAndNamespaces() {
        String[] schemaPackageNamespaces = new String[]{};
        Object value = context.get(WadlToolConstants.CFG_SCHEMA_PACKAGENAME);
        if (value != null) {
            schemaPackageNamespaces = value instanceof String ? new String[]{(String)value}
                                                   : (String[])value;
        }
        for (int i = 0; i < schemaPackageNamespaces.length; i++) {
            int pos = schemaPackageNamespaces[i].indexOf('=');
            String packagename = schemaPackageNamespaces[i];
            if (pos != -1) {
                String ns = schemaPackageNamespaces[i].substring(0, pos);
                packagename = schemaPackageNamespaces[i].substring(pos + 1);
                context.addNamespacePackageMap(ns, packagename);
            } else {
                // this is the default schema package name
                // if CFG_PACKAGENAME is set then it's only used for JAX-RS resource
                // classes
                context.put(WadlToolConstants.CFG_SCHEMA_PACKAGENAME, packagename);
            }
        }

    }

    private Map<String, String> getSchemaTypeMap() {
        return getMap(WadlToolConstants.CFG_SCHEMA_TYPE_MAP);
    }

    private Map<String, String> getMediaTypeMap() {
        return getMap(WadlToolConstants.CFG_MEDIA_TYPE_MAP);
    }

    private Map<String, String> getMap(String parameterName) {
        String[] typeToClasses = new String[]{};
        Object value = context.get(parameterName);
        if (value != null) {
            typeToClasses = value instanceof String ? new String[]{(String)value}
                                                   : (String[])value;
        }
        Map<String, String> typeMap = new HashMap<>();
        for (int i = 0; i < typeToClasses.length; i++) {
            int pos = typeToClasses[i].indexOf('=');
            if (pos != -1) {
                String type = typeToClasses[i].substring(0, pos);
                if (type.contains("%3D")) {
                    type = type.replace("%3D", "=");
                }
                String clsName = typeToClasses[i].substring(pos + 1);
                typeMap.put(type, clsName);
            }
        }
        return typeMap;
    }
}
