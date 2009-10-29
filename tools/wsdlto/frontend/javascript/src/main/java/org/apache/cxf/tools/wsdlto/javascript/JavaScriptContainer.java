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

package org.apache.cxf.tools.wsdlto.javascript;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import javax.wsdl.Definition;

import org.apache.cxf.Bus;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.util.PropertiesLoaderUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.common.toolspec.parser.BadUsageException;
import org.apache.cxf.tools.common.toolspec.parser.CommandDocument;
import org.apache.cxf.tools.common.toolspec.parser.ErrorVisitor;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.util.URIParserUtil;
import org.apache.cxf.tools.validator.ServiceValidator;
import org.apache.cxf.tools.wsdlto.WSDLToJavaContainer;
import org.apache.cxf.tools.wsdlto.core.AbstractWSDLBuilder;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.tools.wsdlto.core.PluginLoader;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;

public class JavaScriptContainer extends WSDLToJavaContainer {

    private static final String TOOL_NAME = "wsdl2js";
    private static final String SERVICE_VALIDATOR = "META-INF/tools.service.validator.xml";

    public JavaScriptContainer(ToolSpec toolspec) throws Exception {
        super(TOOL_NAME, toolspec);
    }

    public Set<String> getArrayKeys() {
        Set<String> set = new HashSet<String>();
        set.add(ToolConstants.CFG_JSPACKAGEPREFIX);
        set.add(ToolConstants.CFG_BEAN_CONFIG);
        return set;
    }

    public WSDLConstants.WSDLVersion getWSDLVersion() {
        String version = (String)context.get(ToolConstants.CFG_WSDL_VERSION);
        return WSDLConstants.getVersion(version);
    }

    @SuppressWarnings("unchecked")
    public void execute() throws ToolException {
        buildToolContext();
        validate(context);

        WSDLConstants.WSDLVersion version = getWSDLVersion();

        String wsdlURL = (String)context.get(ToolConstants.CFG_WSDLURL);
        List<ServiceInfo> serviceList = (List<ServiceInfo>)context.get(ToolConstants.SERVICE_LIST);
        if (serviceList == null) {
            serviceList = new ArrayList<ServiceInfo>();

            PluginLoader pluginLoader = PluginLoader.getInstance();
            // for JavaScript generation, we always use JAX-WS.
            FrontEndProfile frontend = pluginLoader.getFrontEndProfile("jaxws");

            // Build the ServiceModel from the WSDLModel
            if (version == WSDLConstants.WSDLVersion.WSDL11) {
                AbstractWSDLBuilder<Definition> builder = frontend.getWSDLBuilder();
                builder.setContext(context);
                builder.setBus(getBus());
                context.put(Bus.class, getBus());
                builder.build(URIParserUtil.getAbsoluteURI(wsdlURL));
                builder.customize();
                Definition definition = builder.getWSDLModel();

                context.put(Definition.class, definition);

                builder.validate(definition);

                WSDLServiceBuilder serviceBuilder = new WSDLServiceBuilder(getBus());
                String serviceName = (String)context.get(ToolConstants.CFG_SERVICENAME);

                if (serviceName != null) {
                    List<ServiceInfo> services = serviceBuilder.buildServices(definition,
                                                                              getServiceQName(definition));
                    serviceList.addAll(services);
                } else if (definition.getServices().size() > 0) {
                    serviceList = serviceBuilder.buildServices(definition);
                } else {
                    serviceList = serviceBuilder.buildMockServices(definition);
                }

            } else {
                // TODO: wsdl2.0 support
            }
        }
        Map<String, InterfaceInfo> interfaces = new LinkedHashMap<String, InterfaceInfo>();
        
        ServiceInfo service0 = serviceList.get(0);
        SchemaCollection schemaCollection = service0.getXmlSchemaCollection();
        context.put(ToolConstants.XML_SCHEMA_COLLECTION, schemaCollection);
        context.put(ToolConstants.PORTTYPE_MAP, interfaces);
        context.put(ClassCollector.class, new ClassCollector());
        WSDLToJavaScriptProcessor processor = new WSDLToJavaScriptProcessor();

        for (ServiceInfo service : serviceList) {

            context.put(ServiceInfo.class, service);

            validate(service);

            processor.setEnvironment(context);
            processor.process();
        }
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

    public void setNamespaceJavascriptPrefixes(ToolContext env) {
        Map<String, String> nsPrefixMap = new HashMap<String, String>();
        if (env.get(ToolConstants.CFG_JSPACKAGEPREFIX) != null) {
            String[] pns = null;
            try {
                pns = (String[])env.get(ToolConstants.CFG_JSPACKAGEPREFIX);
            } catch (ClassCastException e) {
                Message msg = new Message("INVALID_PREFIX_MAPPING", LOG, env
                    .get(ToolConstants.CFG_JSPACKAGEPREFIX));
                throw new ToolException(msg);
            }
            for (String jsprefix : pns) {
                int pos = jsprefix.indexOf("=");
                if (pos != -1) {
                    String ns = jsprefix.substring(0, pos);
                    jsprefix = jsprefix.substring(pos + 1);
                    nsPrefixMap.put(ns, jsprefix);
                }
            }
            env.put(ToolConstants.CFG_JSPREFIXMAP, nsPrefixMap);
        }
    }

    public void validate(ToolContext env) throws ToolException {
        String outdir = (String)env.get(ToolConstants.CFG_OUTPUTDIR);
        if (outdir != null) {
            File dir = new File(outdir);
            if (!dir.exists() && !dir.mkdirs()) {
                Message msg = new Message("DIRECTORY_COULD_NOT_BE_CREATED", LOG, outdir);
                throw new ToolException(msg);
            }
            if (!dir.isDirectory()) {
                Message msg = new Message("NOT_A_DIRECTORY", LOG, outdir);
                throw new ToolException(msg);
            }
        }

        if (env.optionSet(ToolConstants.CFG_COMPILE)) {
            String clsdir = (String)env.get(ToolConstants.CFG_CLASSDIR);
            if (clsdir != null) {
                File dir = new File(clsdir);
                if (!dir.exists() && !dir.mkdirs()) {
                    Message msg = new Message("DIRECTORY_COULD_NOT_BE_CREATED", LOG, clsdir);
                    throw new ToolException(msg);
                }
            }
        }

        String wsdl = (String)env.get(ToolConstants.CFG_WSDLURL);
        if (StringUtils.isEmpty(wsdl)) {
            Message msg = new Message("NO_WSDL_URL", LOG);
            throw new ToolException(msg);
        }

        env.put(ToolConstants.CFG_WSDLURL, URIParserUtil.normalize(wsdl));

        String[] bindingFiles;
        try {
            bindingFiles = (String[])env.get(ToolConstants.CFG_BINDING);
            if (bindingFiles == null) {
                return;
            }
        } catch (ClassCastException e) {
            bindingFiles = new String[1];
            bindingFiles[0] = (String)env.get(ToolConstants.CFG_BINDING);
        }

        for (int i = 0; i < bindingFiles.length; i++) {
            bindingFiles[i] = URIParserUtil.getAbsoluteURI(bindingFiles[i]);
        }

        env.put(ToolConstants.CFG_BINDING, bindingFiles);
    }

    public void buildToolContext() {
        context = getContext();
        context.addParameters(getParametersMap(getArrayKeys()));

        if (context.get(ToolConstants.CFG_OUTPUTDIR) == null) {
            context.put(ToolConstants.CFG_OUTPUTDIR, ".");
        }

        if (!context.containsKey(ToolConstants.CFG_WSDL_VERSION)) {
            context.put(ToolConstants.CFG_WSDL_VERSION, WSDLConstants.WSDL11);
        }

        context.put(ToolConstants.CFG_SUPPRESS_WARNINGS, true);
        setNamespaceJavascriptPrefixes(context);
    }

    public void checkParams(ErrorVisitor errors) throws ToolException {
        CommandDocument doc = super.getCommandDocument();

        if (!doc.hasParameter("wsdlurl")) {
            errors.add(new ErrorVisitor.UserError("WSDL/SCHEMA URL has to be specified"));
        }
        if (errors.getErrors().size() > 0) {
            Message msg = new Message("PARAMETER_MISSING", LOG);
            throw new ToolException(msg, new BadUsageException(getUsage(), errors));
        }
    }

    public void validate(final ServiceInfo service) throws ToolException {
        for (ServiceValidator validator : getServiceValidators()) {
            service.setProperty(ToolContext.class.getName(), context);
            validator.setService(service);
            if (!validator.isValid()) {
                throw new ToolException(validator.getErrorMessage());
            }
        }
    }

    public List<ServiceValidator> getServiceValidators() {
        List<ServiceValidator> validators = new ArrayList<ServiceValidator>();

        Properties initialExtensions = null;
        try {
            initialExtensions = PropertiesLoaderUtils.loadAllProperties(SERVICE_VALIDATOR, Thread
                .currentThread().getContextClassLoader());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        for (Object element : initialExtensions.values()) {
            String validatorClass = (String)element;
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Found service validator : " + validatorClass);
                }
                ServiceValidator validator = (ServiceValidator)Class.forName(
                                                                             validatorClass,
                                                                             true,
                                                                             Thread.currentThread()
                                                                                 .getContextClassLoader())
                    .newInstance();
                validators.add(validator);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "EXTENSION_ADD_FAILED_MSG", ex);
            }
        }
        return validators;
    }
}
