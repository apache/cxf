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

package org.apache.cxf.tools.wsdlto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertiesLoaderUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.URIParserUtil;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.AbstractCXFToolContainer;
import org.apache.cxf.tools.common.ClassNameProcessor;
import org.apache.cxf.tools.common.ClassUtils;
import org.apache.cxf.tools.common.FrontEndGenerator;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.apache.cxf.tools.common.toolspec.parser.BadUsageException;
import org.apache.cxf.tools.common.toolspec.parser.CommandDocument;
import org.apache.cxf.tools.common.toolspec.parser.ErrorVisitor;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.util.FileWriterUtil;
import org.apache.cxf.tools.util.OutputStreamCreator;
import org.apache.cxf.tools.validator.ServiceValidator;
import org.apache.cxf.tools.wsdlto.core.AbstractWSDLBuilder;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;
import org.apache.ws.commons.schema.XmlSchema;

public class WSDLToJavaContainer extends AbstractCXFToolContainer {

    protected static final Logger LOG = LogUtils.getL7dLogger(WSDLToJavaContainer.class);
    private static final String DEFAULT_NS2PACKAGE = "http://www.w3.org/2005/08/addressing";
    private static final String SERVICE_VALIDATOR = "META-INF/tools.service.validator.xml";
    String toolName;

    public WSDLToJavaContainer(String name, ToolSpec toolspec) throws Exception {
        super(name, toolspec);
        this.toolName = name;
    }

    public Set<String> getArrayKeys() {
        Set<String> set = new HashSet<>();
        set.add(ToolConstants.CFG_PACKAGENAME);
        set.add(ToolConstants.CFG_NEXCLUDE);
        set.add(ToolConstants.CFG_XJC_ARGS);
        return set;
    }

    public WSDLConstants.WSDLVersion getWSDLVersion() {
        String version = (String)context.get(ToolConstants.CFG_WSDL_VERSION);
        return WSDLConstants.getVersion(version);
    }

    public void execute() throws ToolException {
        if (hasInfoOption()) {
            return;
        }

        buildToolContext();

        boolean isWsdlList = context.optionSet(ToolConstants.CFG_WSDLLIST);

        if (isWsdlList) {
            try {
                ToolContext initialContextState = context.makeCopy();
                String wsdlURL = (String)context.get(ToolConstants.CFG_WSDLURL);
                wsdlURL = URIParserUtil.getAbsoluteURI(wsdlURL);

                URL url = new URL(wsdlURL);
                InputStream is = (InputStream)url.getContent();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String tempLine;
                    while ((tempLine = reader.readLine()) != null) {
                        ToolContext freshContext = initialContextState.makeCopy();
                        freshContext.put(ToolConstants.CFG_WSDLURL, tempLine);
                        setContext(freshContext);
                        buildToolContext();

                        processWsdl();
                    }
                    if (context.getErrorListener().getErrorCount() > 0) {
                        context.getErrorListener().throwToolException();
                    }
                }
            } catch (IOException e) {
                throw new ToolException(e);
            }
        } else {
            processWsdl();
            if (context.getErrorListener().getErrorCount() > 0) {
                context.getErrorListener().throwToolException();
            }
        }
    }

    private void processWsdl() {
        validate(context);
        FrontEndProfile frontend = context.get(FrontEndProfile.class);

        if (frontend == null) {
            throw new ToolException(new Message("FOUND_NO_FRONTEND", LOG));
        }

        WSDLConstants.WSDLVersion version = getWSDLVersion();

        String wsdlURL = (String)context.get(ToolConstants.CFG_WSDLURL);

        @SuppressWarnings("unchecked")
        List<ServiceInfo> serviceList = (List<ServiceInfo>)context.get(ToolConstants.SERVICE_LIST);
        if (serviceList == null) {
            serviceList = new ArrayList<>();

            // Build the ServiceModel from the WSDLModel
            if (version == WSDLConstants.WSDLVersion.WSDL11) {
                AbstractWSDLBuilder builder = frontend.getWSDLBuilder();
                builder.setContext(context);
                builder.setBus(getBus());
                context.put(Bus.class, getBus());
                wsdlURL = URIParserUtil.getAbsoluteURI(wsdlURL);
                builder.build(wsdlURL);
                builder.customize();
                Definition definition = builder.getWSDLModel();

                context.put(Definition.class, definition);

                builder.validate(definition);

                WSDLServiceBuilder serviceBuilder = new WSDLServiceBuilder(getBus());
                if (context.isVerbose()) {
                    serviceBuilder.setUnwrapLogLevel(Level.INFO);
                }
                serviceBuilder.setIgnoreUnknownBindings(true);
                String allowRefs = (String)context.get(ToolConstants.CFG_ALLOW_ELEMENT_REFS);
                if (!StringUtils.isEmpty(allowRefs)
                    || context.optionSet(ToolConstants.CFG_ALLOW_ELEMENT_REFS)) {
                    if (allowRefs.length() > 0 && allowRefs.charAt(0) == '=') {
                        allowRefs = allowRefs.substring(1);
                    }
                    if (StringUtils.isEmpty(allowRefs)) {
                        allowRefs = "true";
                    }
                    serviceBuilder.setAllowElementRefs(Boolean.valueOf(allowRefs));
                }
                String serviceName = (String)context.get(ToolConstants.CFG_SERVICENAME);

                if (serviceName != null) {
                    List<ServiceInfo> services = serviceBuilder
                        .buildServices(definition, getServiceQName(definition));
                    serviceList.addAll(services);
                } else if (definition.getServices().size() > 0) {
                    serviceList = serviceBuilder.buildServices(definition);
                } else {
                    serviceList = serviceBuilder.buildMockServices(definition);
                }
                //remove definition from cache so that won't fail when encounter same wsdl file
                //name but different wsdl content(CXF-3340)
                getBus().getExtension(WSDLManager.class).removeDefinition(definition);
            } else {
                // TODO: wsdl2.0 support
            }
        }
        context.put(ToolConstants.SERVICE_LIST, serviceList);

        Map<String, InterfaceInfo> interfaces = new LinkedHashMap<>();

        ServiceInfo service0 = serviceList.get(0);
        SchemaCollection schemaCollection = service0.getXmlSchemaCollection();
        context.put(ToolConstants.XML_SCHEMA_COLLECTION, schemaCollection);

        context.put(ToolConstants.PORTTYPE_MAP, interfaces);

        context.put(ClassCollector.class, createClassCollector());
        Processor processor = frontend.getProcessor();
        if (processor instanceof ClassNameProcessor) {
            processor.setEnvironment(context);
            for (ServiceInfo service : serviceList) {

                context.put(ServiceInfo.class, service);

                ((ClassNameProcessor)processor).processClassNames();

                context.put(ServiceInfo.class, null);
            }
        }

        if (context.optionSet(ToolConstants.CFG_NO_TYPES)) {
            context.remove(ToolConstants.CFG_TYPES);
            context.remove(ToolConstants.CFG_ALL);
            context.remove(ToolConstants.CFG_COMPILE);
        }

        generateTypes();
        if (context.getErrorListener().getErrorCount() > 0) {
            return;
        }

        for (ServiceInfo service : serviceList) {
            context.put(ServiceInfo.class, service);

            if (context.basicValidateWSDL()) {
                validate(service);
            }

            if (context.getErrorListener().getErrorCount() == 0) {
                // Build the JavaModel from the ServiceModel
                processor.setEnvironment(context);
                processor.process();
            }
        }
        if (context.getErrorListener().getErrorCount() > 0) {
            return;
        }

        if (context.optionSet(ToolConstants.CFG_CLIENT_JAR)) {
            enforceWSDLLocation(context);
        }

        if (!isSuppressCodeGen()) {
            // Generate artifacts
            for (FrontEndGenerator generator : frontend.getGenerators()) {
                generator.generate(context);
            }
        }
        context.remove(ToolConstants.SERVICE_LIST);

        // Build projects: compile classes and copy resources etc.
        if (context.optionSet(ToolConstants.CFG_COMPILE)) {
            new ClassUtils().compile(context);
        }

        if (context.isExcludeNamespaceEnabled()) {
            try {
                removeExcludeFiles();
            } catch (IOException e) {
                throw new ToolException(e);
            }
        }
        if (context.optionSet(ToolConstants.CFG_CLIENT_JAR)) {
            processClientJar(context);
        }
    }

    private void enforceWSDLLocation(ToolContext context) {
        String wsdlURL = (String)context.get(ToolConstants.CFG_WSDLURL);
        @SuppressWarnings("unchecked")
        List<ServiceInfo> serviceList = (List<ServiceInfo>)context.get(ToolConstants.SERVICE_LIST);
        int slashIndex = wsdlURL.lastIndexOf('/');
        int dotIndex = wsdlURL.indexOf('.', slashIndex);
        String wsdlLocation = null;
        if (slashIndex > -1 && dotIndex > -1) {
            wsdlLocation = wsdlURL.substring(slashIndex + 1, dotIndex) + ".wsdl";
        }
        if (wsdlLocation == null) {
            wsdlLocation = serviceList.get(0).getName().getLocalPart() + ".wsdl";
        }
        context.put(ToolConstants.CFG_WSDLLOCATION, wsdlLocation);
    }

    private void processClientJar(ToolContext context) {
        ClassCollector oldCollector = context.get(ClassCollector.class);
        ClassCollector newCollector = new ClassCollector();
        String oldClassDir = (String)context.get(ToolConstants.CFG_CLASSDIR);
        File tmpDir = FileUtils.createTmpDir();
        context.put(ToolConstants.CFG_CLASSDIR, tmpDir.getAbsolutePath());

        newCollector.setTypesClassNames(oldCollector.getTypesClassNames());
        newCollector.setSeiClassNames(oldCollector.getSeiClassNames());
        newCollector.setExceptionClassNames(oldCollector.getExceptionClassNames());
        newCollector.setServiceClassNames(oldCollector.getServiceClassNames());
        context.put(ClassCollector.class, newCollector);
        new ClassUtils().compile(context);

        generateLocalWSDL(context);


        File clientJarFile = new File((String)context.get(ToolConstants.CFG_OUTPUTDIR),
                                      (String)context.get(ToolConstants.CFG_CLIENT_JAR));
        try (JarOutputStream jarout = new JarOutputStream(
            Files.newOutputStream(clientJarFile.toPath()), new Manifest())) {
            createClientJar(tmpDir, jarout);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "FAILED_TO_CREAT_CLIENTJAR", e);
            Message msg = new Message("FAILED_TO_CREAT_CLIENTJAR", LOG);
            throw new ToolException(msg, e);
        }
        context.put(ToolConstants.CFG_CLASSDIR, oldClassDir);
        context.put(ClassCollector.class, oldCollector);
    }

    private void createClientJar(File tmpDirectory, JarOutputStream jarout) {
        try {
            URI parentFile = new File((String)context.get(ToolConstants.CFG_CLASSDIR)).toURI();
            File[] files = tmpDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    URI relativePath = parentFile.relativize(file.toURI());
                    String name = relativePath.toString();
                    if (file.isDirectory()) {
                        if (!StringUtils.isEmpty(name)) {
                            if (!name.endsWith("/")) {
                                name += "/";
                            }
                            JarEntry entry = new JarEntry(name);
                            entry.setTime(file.lastModified());
                            jarout.putNextEntry(entry);
                            jarout.closeEntry();
                        }
                        createClientJar(file, jarout);
                        continue;
                    }
                    JarEntry entry = new JarEntry(name);
                    entry.setTime(file.lastModified());
                    jarout.putNextEntry(entry);
                    InputStream input = new BufferedInputStream(Files.newInputStream(file.toPath()));
                    IOUtils.copy(input, jarout);
                    input.close();
                    jarout.closeEntry();
                }
            }
        } catch (Exception e) {
            Message msg = new Message("FAILED_ADD_JARENTRY", LOG);
            throw new ToolException(msg, e);
        }
    }

    private boolean isSuppressCodeGen() {
        return context.optionSet(ToolConstants.CFG_SUPPRESS_GEN);
    }

    public void execute(boolean exitOnFinish) throws ToolException {
        try {
            if (getArgument() != null) {
                super.execute(exitOnFinish);
            }
            execute();

        } catch (ToolException ex) {
            if (ex.getCause() instanceof BadUsageException) {
                printUsageException(toolName, (BadUsageException)ex.getCause());
            }
            throw ex;
        } catch (Exception ex) {
            throw new ToolException(ex);
        } finally {
            tearDown();
        }
    }

    @SuppressWarnings("unchecked")
    public QName getServiceQName(Definition def) {
        List<Definition> defs = new ArrayList<>();
        defs.add(def);
        Iterator<?> ite1 = def.getImports().values().iterator();
        while (ite1.hasNext()) {
            List<javax.wsdl.Import> defList = CastUtils.cast((List<?>)ite1.next());
            for (javax.wsdl.Import importDef : defList) {
                defs.add(importDef.getDefinition());
            }
        }
        String serviceName = (String)context.get(ToolConstants.CFG_SERVICENAME);
        for (Definition definition : defs) {
            if (serviceName != null) {
                for (Iterator<QName> ite = definition.getServices().keySet().iterator(); ite.hasNext();) {
                    QName qn = ite.next();
                    if (qn.getLocalPart().equalsIgnoreCase(serviceName)) {
                        return qn;
                    }
                }
            }
        }

        Message msg = new Message("SERVICE_NOT_FOUND", LOG, new Object[] {serviceName});
        throw new ToolException(msg);
    }

    public void loadDefaultNSPackageMapping(ToolContext env) {
        if (!env.hasExcludeNamespace(DEFAULT_NS2PACKAGE)
            && env.getBooleanValue(ToolConstants.CFG_DEFAULT_NS, "true")
            && env.get(ToolConstants.CFG_NO_ADDRESS_BINDING) != null) {
            // currently namespace2pacakge.cfg only contains wsadressing mapping
            env.loadDefaultNS2Pck(getResourceAsStream("namespace2package.cfg"));
        }
        if (env.getBooleanValue(ToolConstants.CFG_DEFAULT_EX, "true")) {
            env.loadDefaultExcludes(getResourceAsStream("wsdltojavaexclude.cfg"));
        }
    }

    public void setExcludePackageAndNamespaces(ToolContext env) {
        if (env.get(ToolConstants.CFG_NEXCLUDE) != null) {
            String[] pns;
            try {
                pns = (String[])env.get(ToolConstants.CFG_NEXCLUDE);
            } catch (ClassCastException e) {
                pns = new String[] {(String)env.get(ToolConstants.CFG_NEXCLUDE)};
            }

            for (int j = 0; j < pns.length; j++) {
                int pos = pns[j].indexOf('=');
                if (pos != -1) {
                    String ns = pns[j].substring(0, pos);
                    if (ns.equals(ToolConstants.WSA_NAMESPACE_URI)) {
                        env.put(ToolConstants.CFG_NO_ADDRESS_BINDING, ToolConstants.CFG_NO_ADDRESS_BINDING);
                    }
                    String excludePackagename = pns[j].substring(pos + 1);
                    env.addExcludeNamespacePackageMap(ns, excludePackagename);
                    env.addNamespacePackageMap(ns, excludePackagename);
                } else {
                    env.addExcludeNamespacePackageMap(pns[j], env.mapPackageName(pns[j]));
                }
            }
        }
    }

    public void setPackageAndNamespaces(ToolContext env) {
        if (env.get(ToolConstants.CFG_PACKAGENAME) != null) {
            String[] pns;
            try {
                pns = (String[])env.get(ToolConstants.CFG_PACKAGENAME);
            } catch (ClassCastException e) {
                pns = new String[] {(String)env.get(ToolConstants.CFG_PACKAGENAME)};
            }
            for (int j = 0; j < pns.length; j++) {
                int pos = pns[j].indexOf('=');
                String packagename = pns[j];
                if (pos != -1) {
                    String ns = pns[j].substring(0, pos);
                    if (ns.equals(ToolConstants.WSA_NAMESPACE_URI)) {
                        env.put(ToolConstants.CFG_NO_ADDRESS_BINDING, ToolConstants.CFG_NO_ADDRESS_BINDING);
                    }
                    packagename = pns[j].substring(pos + 1);
                    env.addNamespacePackageMap(ns, packagename);
                } else {
                    env.setPackageName(packagename);
                }
            }
        }

    }

    public void validate(ToolContext env) throws ToolException {
        String outdir = (String)env.get(ToolConstants.CFG_OUTPUTDIR);
        if (!isSuppressCodeGen()) {
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
        }

        String wsdl = (String)env.get(ToolConstants.CFG_WSDLURL);
        if (StringUtils.isEmpty(wsdl)) {
            Message msg = new Message("NO_WSDL_URL", LOG);
            throw new ToolException(msg);
        }

        env.put(ToolConstants.CFG_WSDLURL, URIParserUtil.getAbsoluteURI(wsdl));
        if (!env.containsKey(ToolConstants.CFG_WSDLLOCATION)) {
            //make sure the "raw" form is used for the wsdlLocation
            //instead of the absolute URI that normalize may return
            boolean assumeFileURI = false;
            try {
                URI uri = new URI(wsdl);

                String uriScheme = uri.getScheme();
                if (uriScheme == null) {
                    assumeFileURI = true;
                }

                wsdl = uri.toString();
            } catch (Exception e) {
                //not a URL, assume file
                assumeFileURI = true;
            }

            if (assumeFileURI) {
                if (wsdl.indexOf(':') != -1 && !wsdl.startsWith("/")) {
                    wsdl = "file:/" + wsdl;
                } else {
                    wsdl = "file:" + wsdl;
                }
                try {
                    URI uri = new URI(wsdl);
                    wsdl = uri.toString();
                } catch (Exception e1) {
                    //ignore...
                }
            }

            wsdl = wsdl.replace("\\", "/");

            env.put(ToolConstants.CFG_WSDLLOCATION, wsdl);
        }


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

    public void setAntProperties(ToolContext env) {
        String installDir = System.getProperty("install.dir");
        if (installDir != null) {
            env.put(ToolConstants.CFG_INSTALL_DIR, installDir);
        } else {
            env.put(ToolConstants.CFG_INSTALL_DIR, ".");
        }
    }

    protected void setLibraryReferences(ToolContext env) {
        Properties props = loadProperties(getResourceAsStream("wsdltojavalib.properties"));
        if (props != null) {
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                env.put((String)entry.getKey(), entry.getValue());
            }
        }
        env.put(ToolConstants.CFG_ANT_PROP, props);
    }

    public void buildToolContext() {
        context = getContext();
        context.addParameters(getParametersMap(getArrayKeys()));

        if (context.get(ToolConstants.CFG_OUTPUTDIR) == null) {
            context.put(ToolConstants.CFG_OUTPUTDIR, ".");
        }

        if (context.containsKey(ToolConstants.CFG_ANT)) {
            setAntProperties(context);
            setLibraryReferences(context);
        }

        if (!context.containsKey(ToolConstants.CFG_WSDL_VERSION)) {
            context.put(ToolConstants.CFG_WSDL_VERSION, WSDLConstants.WSDL11);
        }

        context.put(ToolConstants.CFG_SUPPRESS_WARNINGS, true);
        loadDefaultNSPackageMapping(context);
        setPackageAndNamespaces(context);
        setExcludePackageAndNamespaces(context);
    }

    protected static InputStream getResourceAsStream(String file) {
        return WSDLToJavaContainer.class.getResourceAsStream(file);
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

    public void removeExcludeFiles() throws IOException {
        List<String> excludeGenFiles = context.getExcludeFileList();
        if (excludeGenFiles == null) {
            return;
        }
        String outPutDir = (String)context.get(ToolConstants.CFG_OUTPUTDIR);
        for (int i = 0; i < excludeGenFiles.size(); i++) {
            String excludeFile = excludeGenFiles.get(i);
            File file = new File(outPutDir, excludeFile);
            file.delete();
            File tmpFile = file.getParentFile();
            while (tmpFile != null && !tmpFile.getCanonicalPath().equalsIgnoreCase(outPutDir)) {
                if (tmpFile.isDirectory() && tmpFile.list() != null && tmpFile.list().length == 0) {
                    tmpFile.delete();
                }
                tmpFile = tmpFile.getParentFile();
            }

            if (context.get(ToolConstants.CFG_COMPILE) != null) {
                String classDir = context.get(ToolConstants.CFG_CLASSDIR) == null
                    ? outPutDir : (String)context.get(ToolConstants.CFG_CLASSDIR);
                File classFile = new File(classDir, excludeFile.substring(0, excludeFile.indexOf(".java"))
                                                    + ".class");
                classFile.delete();
                File tmpClzFile = classFile.getParentFile();
                while (tmpClzFile != null && !tmpClzFile.getCanonicalPath().equalsIgnoreCase(outPutDir)) {
                    if (tmpClzFile.isDirectory() && tmpClzFile.list() != null
                        && tmpClzFile.list().length == 0) {
                        tmpClzFile.delete();
                    }
                    tmpClzFile = tmpClzFile.getParentFile();
                }
            }
        }
    }

    public boolean passthrough() {
        if (context.optionSet(ToolConstants.CFG_GEN_TYPES) || context.optionSet(ToolConstants.CFG_ALL)) {
            return false;
        }
        if (context.optionSet(ToolConstants.CFG_GEN_ANT) || context.optionSet(ToolConstants.CFG_GEN_CLIENT)
            || context.optionSet(ToolConstants.CFG_GEN_IMPL) || context.optionSet(ToolConstants.CFG_GEN_SEI)
            || context.optionSet(ToolConstants.CFG_GEN_SERVER)
            || context.optionSet(ToolConstants.CFG_GEN_SERVICE)
            || context.optionSet(ToolConstants.CFG_GEN_FAULT)) {
            return true;
        }
        return context.optionSet(ToolConstants.CFG_NO_TYPES);
    }

    public void generateTypes() throws ToolException {
        DataBindingProfile dataBindingProfile = context.get(DataBindingProfile.class);
        if (dataBindingProfile == null) {
            Message msg = new Message("FOUND_NO_DATABINDING", LOG);
            throw new ToolException(msg);
        }
        dataBindingProfile.initialize(context);
        if (passthrough()) {
            return;
        }
        dataBindingProfile.generate(context);
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
        List<ServiceValidator> validators = new ArrayList<>();

        final Properties initialExtensions;
        try {
            initialExtensions = PropertiesLoaderUtils.loadAllProperties(SERVICE_VALIDATOR, Thread
                .currentThread().getContextClassLoader());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        for (Iterator<?> it = initialExtensions.values().iterator(); it.hasNext();) {
            String validatorClass = (String)it.next();
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Found service validator : " + validatorClass);
                }
                ServiceValidator validator = (ServiceValidator)Class.forName(
                                                                             validatorClass,
                                                                             true,
                                                                             Thread.currentThread()
                                                                                 .getContextClassLoader())
                    .getDeclaredConstructor().newInstance();
                validators.add(validator);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "EXTENSION_ADD_FAILED_MSG", ex);
            }
        }
        return validators;
    }

    @SuppressWarnings("unchecked")
    private void generateLocalWSDL(ToolContext context) {
        String outputdir = (String)context.get(ToolConstants.CFG_CLASSDIR);
        File wsdlFile = new File(outputdir, (String)context.get(ToolConstants.CFG_WSDLLOCATION));
        Definition def = context.get(Definition.class);
        try {
            //get imported schemas
            int xsdCount = 0;
            SchemaCollection schemas = (SchemaCollection) context.get(ToolConstants.XML_SCHEMA_COLLECTION);
            Map<String, String> sourceMap = new HashMap<>();
            for (XmlSchema imp : schemas.getXmlSchemas()) {
                if (imp.getSourceURI() != null && !imp.getSourceURI().contains(".wsdl#")) {
                    String schemaFileName = "schema" + (++xsdCount) + ".xsd";
                    File sourceFile = new File(imp.getSourceURI());
                    sourceMap.put(createSchemaFileKey(imp.getTargetNamespace(), sourceFile.getName()), schemaFileName);
                }
            }

            //get imported wsdls
            int wsdlImportCount = 0;
            List<Definition> defs = (List<Definition>)context.get(ToolConstants.IMPORTED_DEFINITION);
            Map<String, String> importWSDLMap = new HashMap<>();
            for (Definition importDef : defs) {
                File importedWsdlFile;
                if (!StringUtils.isEmpty(importDef.getDocumentBaseURI())) {
                    importedWsdlFile = new File(importDef.getDocumentBaseURI());
                } else {
                    importedWsdlFile = new File(importDef.getQName().getLocalPart() + ".wsdl");
                }
                if (!FileUtils.isValidFileName(importedWsdlFile.getName())) {
                    importedWsdlFile = new File("import" + (++wsdlImportCount) + ".wsdl");
                }
                importWSDLMap.put(importDef.getTargetNamespace(), importedWsdlFile.getName());
            }

            final OutputStreamCreator outputStreamCreator;
            if (context.get(OutputStreamCreator.class) != null) {
                outputStreamCreator = context.get(OutputStreamCreator.class);
            } else {
                outputStreamCreator = new OutputStreamCreator();
                context.put(OutputStreamCreator.class, outputStreamCreator);
            }

            for (XmlSchema imp : schemas.getXmlSchemas()) {
                if (imp.getSourceURI() != null && !imp.getSourceURI().contains(".wsdl#")) {
                    File sourceFile = new File(imp.getSourceURI());
                    String schemaKey = createSchemaFileKey(imp.getTargetNamespace(), sourceFile.getName());
                    String schemaFileName = sourceMap.get(schemaKey);
                    File impfile = new File(wsdlFile.getParentFile(), schemaFileName);
                    Element el = imp.getSchemaDocument().getDocumentElement();
                    updateImports(el, sourceMap);
                    updateIncludes(el, sourceMap);
                    try (Writer os = new FileWriterUtil(impfile.getParent(), outputStreamCreator)
                        .getWriter(impfile, StandardCharsets.UTF_8.name())) {
                        StaxUtils.writeTo(el, os, 2);
                    }
                }
            }

            WSDLWriter wsdlWriter = WSDLFactory.newInstance().newWSDLWriter();

            //change the import location in wsdl file
            try (OutputStream wsdloutput = new BufferedOutputStream(Files.newOutputStream(wsdlFile.toPath()))) {
                LoadingByteArrayOutputStream bout = new LoadingByteArrayOutputStream();
                wsdlWriter.writeWSDL(def, bout);
                Element defEle = StaxUtils.read(bout.createInputStream()).getDocumentElement();
                List<Element> xsdElements = DOMUtils.findAllElementsByTagNameNS(defEle,
                                                                                WSDLConstants.NS_SCHEMA_XSD,
                                                                                "schema");
                for (Element xsdEle : xsdElements) {
                    updateImports(xsdEle, sourceMap);
                    updateIncludes(xsdEle, sourceMap);
                }
                updateWSDLImports(defEle, importWSDLMap);
                StaxUtils.writeTo(defEle, wsdloutput);
            }

            for (Definition importDef : defs) {
                File importWsdlFile = new File(outputdir, importWSDLMap.get(importDef.getTargetNamespace()));
                try (OutputStream wsdlOs = new BufferedOutputStream(Files.newOutputStream(importWsdlFile.toPath()))) {
                    LoadingByteArrayOutputStream bout = new LoadingByteArrayOutputStream();
                    wsdlWriter.writeWSDL(importDef, bout);
                    Element importEle = StaxUtils.read(bout.createInputStream()).getDocumentElement();

                    List<Element> xsdElements = DOMUtils.findAllElementsByTagNameNS(importEle,
                        WSDLConstants.NS_SCHEMA_XSD, "schema");
                    for (Element xsdEle : xsdElements) {
                        updateImports(xsdEle, sourceMap);
                        updateIncludes(xsdEle, sourceMap);
                    }
                    updateWSDLImports(importEle, importWSDLMap);
                    StaxUtils.writeTo(importEle, wsdlOs);
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "FAILED_TO_GEN_LOCAL_WSDL", ex);
            Message msg = new Message("FAILED_TO_GEN_LOCAL_WSDL", LOG);
            throw new ToolException(msg, ex);
        }
    }

    private static String createSchemaFileKey(String targetNamespace, String fileName) {
        return targetNamespace + "_" + fileName;
    }

    private void updateImports(Element el, Map<String, String> sourceMap) {
        List<Element> imps = DOMUtils.getChildrenWithName(el,
                                                          WSDLConstants.NS_SCHEMA_XSD,
                                                          "import");
        for (Element e : imps) {
            String ns = e.getAttribute("namespace");
            updateSchemaLocation(sourceMap, ns, e);
        }
    }

    private void updateIncludes(Element el, Map<String, String> sourceMap) {
        List<Element> imps = DOMUtils.getChildrenWithName(el,
                                                        WSDLConstants.NS_SCHEMA_XSD,
                                                        "include");
        String ns =  el.getAttribute("targetNamespace");
        for (Element e : imps) {
            updateSchemaLocation(sourceMap, ns, e);
        }
    }

    private static void updateSchemaLocation(Map<String, String> sourceMap, String namespace, Element e) {
        File sourceFile = new File(e.getAttribute("schemaLocation"));
        String schemaKey = createSchemaFileKey(namespace, sourceFile.getName());
        e.setAttribute("schemaLocation", sourceMap.get(schemaKey));
    }

    private void updateWSDLImports(Element el, Map<String, String> wsdlSourceMap) {
        List<Element> imps = DOMUtils.getChildrenWithName(el,
                                                          WSDLConstants.QNAME_IMPORT.getNamespaceURI(),
                                                          "import");
        for (Element e : imps) {
            String ns = e.getAttribute("namespace");
            e.setAttribute("location", wsdlSourceMap.get(ns));
        }
    }
}
