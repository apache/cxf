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

package org.apache.cxf.tools.java2js.processor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.BindingType;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.javascript.BasicNameManager;
import org.apache.cxf.javascript.JavascriptGetInterceptor;
import org.apache.cxf.javascript.NamespacePrefixAccumulator;
import org.apache.cxf.javascript.service.ServiceJavascriptBuilder;
import org.apache.cxf.javascript.types.SchemaJavascriptBuilder;
import org.apache.cxf.service.ServiceBuilder;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.java2wsdl.processor.internal.ServiceBuilderFactory;
import org.apache.cxf.tools.util.AnnotationUtil;
import org.apache.cxf.wsdl.WSDLConstants;

import static java.nio.charset.StandardCharsets.UTF_8;

public class JavaToJSProcessor implements Processor {
    private static final Logger LOG = LogUtils.getL7dLogger(JavaToJSProcessor.class);
    private static final String JAVA_CLASS_PATH = "java.class.path";
    private ToolContext context;

    public void process() throws ToolException {
        String oldClassPath = System.getProperty(JAVA_CLASS_PATH);
        LOG.log(Level.INFO, "OLD_CP", oldClassPath);
        if (context.get(ToolConstants.CFG_CLASSPATH) != null) {
            String newCp = (String)context.get(ToolConstants.CFG_CLASSPATH);
            System.setProperty(JAVA_CLASS_PATH, newCp + File.pathSeparator + oldClassPath);
            LOG.log(Level.INFO, "NEW_CP", newCp);
        }

        // check for command line specification of data binding.

        ServiceBuilder builder = getServiceBuilder();
        ServiceInfo serviceInfo = builder.createService();

        File jsFile = getOutputFile(builder.getOutputFile(), serviceInfo.getName().getLocalPart() + ".js");

        BasicNameManager nameManager = BasicNameManager.newNameManager(serviceInfo, null);
        NamespacePrefixAccumulator prefixManager = new NamespacePrefixAccumulator(serviceInfo
            .getXmlSchemaCollection());
        Collection<SchemaInfo> schemata = serviceInfo.getSchemas();
        try {
            OutputStream outputStream = Files.newOutputStream(jsFile.toPath());
            if (null != context.get(ToolConstants.CFG_JAVASCRIPT_UTILS)) {
                JavascriptGetInterceptor.writeUtilsToResponseStream(JavaToJSProcessor.class, outputStream);
            }

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, UTF_8);
            try (BufferedWriter writer = new BufferedWriter(outputStreamWriter)) {

                for (SchemaInfo schema : schemata) {
                    SchemaJavascriptBuilder jsBuilder = new SchemaJavascriptBuilder(serviceInfo
                        .getXmlSchemaCollection(), prefixManager, nameManager);
                    String allThatJavascript = jsBuilder.generateCodeForSchema(schema.getSchema());
                    writer.append(allThatJavascript);
                }

                ServiceJavascriptBuilder serviceBuilder = new ServiceJavascriptBuilder(serviceInfo, null,
                                                                                     prefixManager, nameManager);
                serviceBuilder.walk();
                String serviceJavascript = serviceBuilder.getCode();
                writer.append(serviceJavascript);
            }
        } catch (IOException e) {
            throw new ToolException(e);
        }

        System.setProperty(JAVA_CLASS_PATH, oldClassPath);
        LOG.log(Level.INFO, "RESUME_CP", oldClassPath);
    }

    @SuppressWarnings("unchecked")
    public ServiceBuilder getServiceBuilder() throws ToolException {
        Object beanFilesParameter = context.get(ToolConstants.CFG_BEAN_CONFIG);
        List<String> beanDefinitions = new ArrayList<>();
        if (beanFilesParameter != null) {
            if (beanFilesParameter instanceof String) {
                beanDefinitions.add((String)beanFilesParameter);
            } else if (beanFilesParameter instanceof List) {
                // is there a better way to avoid the warning?
                beanDefinitions.addAll((List<String>)beanFilesParameter);
            } else {
                String[] list = (String[])beanFilesParameter;
                for (String b : list) {
                    beanDefinitions.add(b);
                }
            }
        }


        ServiceBuilderFactory builderFactory
            = ServiceBuilderFactory.getInstance(beanDefinitions,
                                                getDataBindingName());
        Class<?> clz = getServiceClass();
        context.put(Class.class, clz);
        if (clz.isInterface()) {
            context.put(ToolConstants.GEN_FROM_SEI, Boolean.TRUE);
            context.put(ToolConstants.SEI_CLASS, clz.getName());
        } else {
            context.put(ToolConstants.IMPL_CLASS, clz.getName());
            if (clz.getInterfaces().length == 1) {
                context.put(ToolConstants.SEI_CLASS, clz.getInterfaces()[0].getName());
            }
            context.put(ToolConstants.GEN_FROM_SEI, Boolean.FALSE);
        }
        builderFactory.setServiceClass(clz);
        builderFactory.setDatabindingName(getDataBindingName());
        // The service class determines the frontend, so no need to pass it in
        // twice.
        ServiceBuilder builder = builderFactory.newBuilder();

        builder.validate();

        builder.setTransportId(getTransportId());
        builder.setBus(getBus());
        builder.setBindingId(getBindingId());

        return builder;
    }

    protected String getTransportId() {
        if (isSOAP12()) {
            return WSDLConstants.NS_SOAP12;
        }
        return WSDLConstants.NS_SOAP11;
    }

    protected String getBindingId() {
        if (isSOAP12()) {
            return WSDLConstants.NS_SOAP12;
        }
        return WSDLConstants.NS_SOAP11;
    }

    protected boolean isSOAP12() {
        if (!this.context.optionSet(ToolConstants.CFG_SOAP12)) {
            BindingType bType = getServiceClass().getAnnotation(BindingType.class);
            if (bType != null) {
                return SOAPBinding.SOAP12HTTP_BINDING.equals(bType.value());
            }
            return false;
        }
        return true;
    }

    protected File getOutputDir(File wsdlLocation) {
        String dir = (String)context.get(ToolConstants.CFG_OUTPUTDIR);
        if (dir == null) {
            if (wsdlLocation == null || wsdlLocation.getParentFile() == null
                || !wsdlLocation.getParentFile().exists()) {
                dir = "./";
            } else {
                dir = wsdlLocation.getParent();
            }
        }
        return new File(dir);
    }

    protected File getOutputFile(File nameFromClz, String defaultOutputFile) {
        String output = (String)context.get(ToolConstants.CFG_OUTPUTFILE);
        String dir = (String)context.get(ToolConstants.CFG_OUTPUTDIR);
        if (dir == null) {
            dir = "./";
        }

        File result;
        if (output != null) {
            result = new File(output);
            if (!result.isAbsolute()) {
                result = new File(new File(dir), output);
            }
        } else {
            result = new File(new File(dir), defaultOutputFile);
        }
        if (nameFromClz != null) {
            result = nameFromClz;
        }

        // rename the exising wsdl file
        if (result.exists() && !result.renameTo(new File(result.getParent(), result.getName()))) {
            throw new ToolException(new Message("OUTFILE_EXISTS", LOG));
        }
        return result;
    }

    public Class<?> getServiceClass() {
        return AnnotationUtil.loadClass((String)context.get(ToolConstants.CFG_CLASSNAME), Thread
            .currentThread().getContextClassLoader());
    }

    public WSDLConstants.WSDLVersion getWSDLVersion() {
        String version = (String)context.get(ToolConstants.CFG_WSDL_VERSION);
        WSDLConstants.WSDLVersion wsVersion = WSDLConstants.getVersion(version);
        if (wsVersion == WSDLConstants.WSDLVersion.UNKNOWN) {
            wsVersion = WSDLConstants.WSDLVersion.WSDL11;
        }
        return wsVersion;
    }

    public String getServiceName() {
        return (String)this.context.get(ToolConstants.CFG_SERVICENAME);
    }

    File getSourceDir() {
        String dir = (String)this.context.get(ToolConstants.CFG_SOURCEDIR);
        if (StringUtils.isEmpty(dir)) {
            return null;
        }
        return new File(dir);
    }

    File getClassesDir() {
        String dir = (String)this.context.get(ToolConstants.CFG_CLASSDIR);
        if (StringUtils.isEmpty(dir)) {
            return null;
        }
        return new File(dir);
    }

    public Bus getBus() {
        return BusFactory.getThreadDefaultBus();
    }

    public void setEnvironment(ToolContext env) {
        this.context = env;
    }

    public ToolContext getEnvironment() {
        return this.context;
    }

    public String getDataBindingName() {
        String databindingName = (String)context.get(ToolConstants.CFG_DATABINDING);
        if (databindingName == null) {
            databindingName = ToolConstants.DEFAULT_DATA_BINDING_NAME;
        }
        return databindingName;
    }
}
