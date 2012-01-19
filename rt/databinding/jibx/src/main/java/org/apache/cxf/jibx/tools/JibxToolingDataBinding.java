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

package org.apache.cxf.jibx.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.BusFactory;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.common.ClassUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.DefaultValueWriter;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.util.URIParserUtil;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.jibx.binding.Compile;
import org.jibx.binding.Utility;
import org.jibx.binding.model.BindingElement;
import org.jibx.binding.model.BindingHolder;
import org.jibx.binding.model.BindingOrganizer;
import org.jibx.binding.model.BindingUtils;
import org.jibx.binding.model.MappingElement;
import org.jibx.binding.model.ModelVisitor;
import org.jibx.binding.model.NamespaceElement;
import org.jibx.binding.model.TreeContext;
import org.jibx.binding.model.ValueElement;
import org.jibx.runtime.JiBXException;
import org.jibx.schema.codegen.CodeGen;
import org.jibx.schema.codegen.PackageHolder;
import org.jibx.schema.codegen.PackageOrganizer;
import org.jibx.schema.codegen.StringObjectPair;
import org.jibx.schema.codegen.custom.SchemaCustom;
import org.jibx.schema.codegen.custom.SchemasetCustom;
import org.jibx.schema.validation.ProblemMultiHandler;
import org.jibx.util.DummyClassLocator;

public class JibxToolingDataBinding implements DataBindingProfile {

    private JibxToolingProblemHandler problemHandler = new JibxToolingProblemHandler();
    private Map<String, Element> schemaMap = new HashMap<String, Element>();
    private List<JibxSchemaResolver> resolvers = new ArrayList<JibxSchemaResolver>();

    private Map<org.jibx.runtime.QName, MappingElement> types = new HashMap<org.jibx.runtime.QName,
                                                                            MappingElement>();
    private Map<org.jibx.runtime.QName, MappingElement> elements = new HashMap<org.jibx.runtime.QName,
                                                                            MappingElement>();

    public DefaultValueWriter createDefaultValueWriter(QName qn, boolean element) {
        return null;
    }

    public DefaultValueWriter createDefaultValueWriterForWrappedElement(QName wrapperElement, QName qn) {
        return null;
    }

    public void generate(ToolContext context) throws ToolException {
        try {
            JiBXCodeGen codegen = new JiBXCodeGen();

            ProblemMultiHandler handler = new ProblemMultiHandler();
            handler.addHandler(problemHandler);
            codegen.setProblemHandler(handler);

            // Setting the source (or the output) directory
            String sourcePath = (String)context.get(ToolConstants.CFG_OUTPUTDIR);
            if (sourcePath == null) {
                sourcePath = (new File(".")).getAbsolutePath();
            }
            File generatePath = new File(sourcePath);
            if (!generatePath.exists()) {
                generatePath.mkdir();
            }
            codegen.setGeneratePath(generatePath);
            String wsdlUrl = URIParserUtil.getAbsoluteURI((String)context.get(ToolConstants.CFG_WSDLURL));
            if (wsdlUrl.contains("/")) {
                wsdlUrl = wsdlUrl.substring(wsdlUrl.lastIndexOf('/'));
            }
            if (wsdlUrl.toLowerCase().endsWith(".wsdl")) {
                wsdlUrl = wsdlUrl.substring(0, wsdlUrl.length() - 5);
            }
            wsdlUrl += ".xml";
            File jibxDir = new File(generatePath, "jibx_bindings/");
            jibxDir.mkdirs();
            codegen.setBindingName("jibx_bindings/" +  wsdlUrl);

            String classPath = (String)context.get(ToolConstants.CFG_CLASSDIR);
            if (classPath == null) {
                classPath = (new File(".")).getAbsolutePath();
            }
            File compilePath = new File(classPath);
            if (!compilePath.exists()) {
                compilePath.mkdir();
            }
            codegen.setCompilePath(compilePath);

            // Set schema resolver list

            codegen.setFileset(resolvers);

            // Set Customization
            String[] bindingFiles = (String[])context.get(ToolConstants.CFG_BINDING);
            SchemasetCustom customRoot;
            if (bindingFiles == null || bindingFiles.length == 0) {
                customRoot = defaultSchemasetCustom(schemaMap);
            } else {
                customRoot = SchemasetCustom.loadCustomizations(bindingFiles[0], handler);
            }
            // force to retrain types information in the generated binding model
            forceTypes(customRoot);
            codegen.setCustomRoot(customRoot);

            codegen.generate();

            if (Boolean.valueOf((String)context.get(ToolConstants.CFG_COMPILE))) {
                if (context.get(ToolConstants.CFG_SOURCEDIR) == null) {
                    context.put(ToolConstants.CFG_SOURCEDIR, generatePath.getAbsolutePath());
                }
                if (context.get(ToolConstants.CFG_CLASSDIR) == null) {
                    context.put(ToolConstants.CFG_CLASSDIR, compilePath.getAbsolutePath());
                }

                ClassCollector collector = new ClassCollector();
                addGeneratedSourceFiles(codegen.getPackageOrganizer(), collector);
                context.put(ClassCollector.class, collector);

                // compile generated source files
                (new ClassUtils()).compile(context);

                // jibx binding compiler
                codegen.compile();
            }

            Map<QName, Object> formats = new HashMap<QName, Object>();
            BindingUtils.getDefinitions(codegen.getRootBinding(), types, elements, formats);
            
            Iterator it = codegen.getBindingOrganizer().iterateBindings();
            while (it.hasNext()) {
                BindingHolder o = (BindingHolder)it.next();
                if (o != null) {
                    getDefinitions(o, types, elements);
                }
            }

        } catch (Exception e) {
            problemHandler.handleSevere("", e);
        }
    }
    public static void getDefinitions(final BindingHolder holder, 
                                      final Map<org.jibx.runtime.QName, MappingElement> types, 
                                      final Map<org.jibx.runtime.QName, MappingElement> elems) {
        TreeContext ctx = new TreeContext(new DummyClassLocator());
        ModelVisitor visitor = new ModelVisitor() {
            public boolean visit(MappingElement mapping) {
                org.jibx.runtime.QName qname = mapping.getTypeQName();
                if (qname != null) {
                    types.put(qname, mapping);
                }
                String name = mapping.getName();
                if (name != null) {
                    NamespaceElement ns = mapping.getNamespace();
                    if (ns == null) {
                        qname = new org.jibx.runtime.QName(holder.getElementDefaultNamespace(), name);
                    } else {
                        qname = new org.jibx.runtime.QName(mapping.getNamespace().getUri(), name);
                    }
                    elems.put(qname, mapping);
                }
                return false;
            }
        };
        ctx.tourTree(holder.getBinding(), visitor);
    }
    public String getType(QName qn, boolean element) {
        MappingElement mappingElement = element ? elements.get(jibxQName(qn)) : types.get(jibxQName(qn));
        return (mappingElement == null) ? null : mappingElement.getClassName();
    }

    public String getWrappedElementType(QName wrapperElement, QName item) {
        MappingElement mappingElement = elements.get(jibxQName(wrapperElement));
        return (mappingElement == null) ? null : itemType(mappingElement, item);
    }

    public void initialize(ToolContext context) throws ToolException {
        context.put(ToolConstants.RUNTIME_DATABINDING_CLASS,
                    "org.apache.cxf.jibx.JibxDataBinding.class");
        
        String wsdlUrl = (String)context.get(ToolConstants.CFG_WSDLURL);
        initializeJiBXCodeGenerator(wsdlUrl);
    }

    private void initializeJiBXCodeGenerator(String wsdlUrl) {

        try {
            loadWsdl(wsdlUrl, this.schemaMap, this.resolvers);
        } catch (WSDLException e) {
            problemHandler.handleSevere("Error in loading wsdl file at :" + wsdlUrl, e);
        }
    }

    private static void loadWsdl(String wsdlUrl, Map<String, Element> schemaMap,
                                 List<JibxSchemaResolver> resolvers) throws WSDLException {
        WSDLFactory factory = WSDLFactory.newInstance();
        WSDLReader reader = factory.newWSDLReader();
        Definition parentDef = reader.readWSDL(wsdlUrl);

        JibxSchemaHelper util = new JibxSchemaHelper(BusFactory.getDefaultBus(), schemaMap);
        util.getSchemas(parentDef, new SchemaCollection(), resolvers);
    }

    private static org.jibx.runtime.QName jibxQName(QName qname) {
        return new org.jibx.runtime.QName(qname.getNamespaceURI(), qname.getLocalPart());
    }

    private static String itemType(MappingElement mappingElement, QName qName) {
        String localPart = qName.getLocalPart();
        for (Iterator childIterator = mappingElement.childIterator(); childIterator.hasNext();) {
            Object child = childIterator.next();
            if (child instanceof ValueElement) {
                ValueElement valueElement = (ValueElement)child;
                if (localPart.equals(valueElement.getName())) {
                    return valueElement.getDeclaredType();
                }
            }
            // TODO
            /*
             * else if (child instanceof ) { .. } else if () { .. }
             */
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private SchemasetCustom defaultSchemasetCustom(Map<String, Element> smap) {
        SchemasetCustom customRoot = new SchemasetCustom((SchemasetCustom)null);
        Set<String> schemaIds = smap.keySet();
        for (String schemaId : schemaIds) {
            SchemaCustom schemaCustom = new SchemaCustom(customRoot);
            schemaCustom.setName(schemaId);
            schemaCustom.setForceTypes(Boolean.TRUE);
            schemaCustom.setNamespace(smap.get(schemaId).getAttribute("targetNamespace"));
            customRoot.getChildren().add(schemaCustom);
        }
        for (JibxSchemaResolver r : resolvers) {
            if (!schemaIds.contains(r.getId())) {
                SchemaCustom schemaCustom = new SchemaCustom(customRoot);
                schemaCustom.setName(r.getName());
                schemaCustom.setNamespace(r.getElement().getAttribute("targetNamespace"));
                schemaCustom.setForceTypes(Boolean.TRUE);
                customRoot.getChildren().add(schemaCustom);
            }
        }
        return customRoot;
    }

    private static void forceTypes(SchemasetCustom customRoot) {
        List<?> children = customRoot.getChildren();
        for (Object child : children) {
            SchemaCustom schemaCustom = (SchemaCustom)child;
            schemaCustom.setForceTypes(Boolean.TRUE);
        }
    }

    private static void addGeneratedSourceFiles(PackageOrganizer o, ClassCollector collector) {
        List<PackageHolder> packages = CastUtils.cast(o.getPackages());
        for (PackageHolder pkgHolder : packages) {
            if (pkgHolder.getTopClassCount() > 0) {
                String pkgName = pkgHolder.getName();
                StringObjectPair[] classFields = pkgHolder.getClassFields();
                for (int i = 0; i < classFields.length; i++) {
                    String fullname = classFields[i].getKey();
                    if (fullname.contains("$")) { // CHECK
                        continue;
                    }
                    collector.addTypesClassName(pkgName, fullname.replace(pkgName, ""), fullname);
                }
            }
        }
    }

    /**
     * A helper class to manage JiBX specific code generation parameters and initiate code generation. Every
     * member variable is a parameter for JiBX code generator and carries a default value in case it is not
     * set by CXF code generator framework.
     */
    static class JiBXCodeGen {
        private ProblemMultiHandler problemHandler;
        private SchemasetCustom customRoot;
        private URL schemaRoot;
        private File generatePath;
        private boolean verbose;
        private String usingNamespace;
        private String nonamespacePackage;
        private String bindingName = "binding";
        private List fileset;
        private List includePaths = new ArrayList();
        private File modelFile;
        private BindingElement rootBinding;
        private File compilePath;
        private PackageOrganizer packageOrganizer;
        private BindingOrganizer bindingOrganizer;

        public void setProblemHandler(ProblemMultiHandler problemHandler) {
            this.problemHandler = problemHandler;
        }

        public BindingOrganizer getBindingOrganizer() {
            return bindingOrganizer;
        }

        public void setCustomRoot(SchemasetCustom customRoot) {
            this.customRoot = customRoot;
        }

        public void setSchemaRoot(URL schemaRoot) {
            this.schemaRoot = schemaRoot;
        }

        public void setGeneratePath(File generatePath) {
            this.generatePath = generatePath;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        public void setUsingNamespace(String usingNamespace) {
            this.usingNamespace = usingNamespace;
        }

        public void setNonamespacePackage(String nonamespacePackage) {
            this.nonamespacePackage = nonamespacePackage;
        }

        public void setBindingName(String bindingName) {
            this.bindingName = bindingName;
        }

        public List getFileset() {
            return fileset;
        }

        public void setFileset(List fileset) {
            this.fileset = fileset;
        }

        public void setIncludePaths(List includePaths) {
            this.includePaths = includePaths;
        }

        public void setModelFile(File modelFile) {
            this.modelFile = modelFile;
        }

        /**
         * Returns the {@link BindingElement} instance that contains binding information of generated code.
         * Hence it is <strong>only meaningful<strong> after executing {@link #generate()} method.
         * 
         * @return the binding element instance that contains binding info of generated code
         */
        public BindingElement getRootBinding() {
            return rootBinding;
        }

        public PackageOrganizer getPackageOrganizer() {
            return packageOrganizer;
        }

        public void setCompilePath(File compilePath) {
            this.compilePath = compilePath;
        }

        /**
         * Generates code based on parameters set. Once the code is generated {@link #rootBinding} is set
         * which can be retrieved by {@link #getRootBinding()}
         * 
         * @throws JiBXException if thrown by JiBX code generator
         * @throws IOException if thrown by JiBX code generator
         */
        public void generate() throws JiBXException, IOException {
            CodeGen codegen = new CodeGen(customRoot, schemaRoot, generatePath);
            
            codegen.generate(verbose, usingNamespace, nonamespacePackage, bindingName, fileset, includePaths,
                             modelFile, problemHandler);
            setPostGenerateInfo(codegen);
        }

        public void compile() throws JiBXException {
            Compile compiler = new Compile();
            String path = generatePath.getAbsolutePath();
            if (!path.endsWith(File.separator)) {
                path = path + File.separator;
            }

            List<String> clsPath = new ArrayList<String>();
            clsPath.add(compilePath.getAbsolutePath());
            clsPath.addAll(Arrays.asList(Utility.getClassPaths()));

            String[] clsPathSet = clsPath.toArray(new String[clsPath.size()]);
            String[] bindingSet = new String[] {
                path + bindingName + ".xml"
            };

            compiler.compile(clsPathSet, bindingSet);
        }

        private void setPostGenerateInfo(CodeGen codegen) {
            this.bindingOrganizer = codegen.getBindingDirectory();
            this.rootBinding = codegen.getRootBinding();
            this.packageOrganizer = codegen.getPackageDirectory();
        }

    }
}
