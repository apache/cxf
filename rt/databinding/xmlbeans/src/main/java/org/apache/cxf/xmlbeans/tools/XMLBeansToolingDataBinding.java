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

package org.apache.cxf.xmlbeans.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.DefaultValueWriter;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.SchemaTypeLoader;
import org.apache.xmlbeans.SchemaTypeSystem;
import org.apache.xmlbeans.SimpleValue;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlErrorCodes;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.impl.common.ResolverUtil;
import org.apache.xmlbeans.impl.common.XmlErrorWatcher;
import org.apache.xmlbeans.impl.config.BindingConfigImpl;
import org.apache.xmlbeans.impl.schema.PathResourceLoader;
import org.apache.xmlbeans.impl.schema.SchemaTypeLoaderImpl;
import org.apache.xmlbeans.impl.schema.SchemaTypeSystemCompiler;
import org.apache.xmlbeans.impl.schema.SchemaTypeSystemImpl;
import org.apache.xmlbeans.impl.schema.StscState;
import org.apache.xmlbeans.impl.tool.CodeGenUtil;
import org.apache.xmlbeans.impl.util.FilerImpl;
import org.apache.xmlbeans.impl.xb.substwsdl.TImport;
import org.apache.xmlbeans.impl.xb.xmlconfig.ConfigDocument;
import org.apache.xmlbeans.impl.xb.xmlconfig.Extensionconfig;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument.Schema;

/**
 * 
 */
public class XMLBeansToolingDataBinding implements DataBindingProfile {
    private static final String CONFIG_URI = "http://xml.apache.org/xmlbeans/2004/02/xbean/config";
    private static final String COMPATIBILITY_CONFIG_URI = "http://www.bea.com/2002/09/xbean/config";
    private static final Map<String, String> MAP_COMPATIBILITY_CONFIG_URIS;
    static {
        MAP_COMPATIBILITY_CONFIG_URIS = new HashMap<String, String>();
        MAP_COMPATIBILITY_CONFIG_URIS.put(COMPATIBILITY_CONFIG_URI, CONFIG_URI);
    }    
    
    SchemaTypeSystem typeSystem;
    Map<String, String> sourcesToCopyMap = new HashMap<String, String>();
    List<XmlError> errors = new LinkedList<XmlError>();
    XmlErrorWatcher errorListener = new XmlErrorWatcher(errors);
    PathResourceLoader cpResourceLoader = new PathResourceLoader(CodeGenUtil.systemClasspath());
    StscState state;
   
    
    public void initialize(ToolContext context) throws ToolException {
        context.put(ToolConstants.RUNTIME_DATABINDING_CLASS,
            "org.apache.cxf.xmlbeans.XmlBeansDataBinding.class");
        
        String wsdl = (String)context.get(ToolConstants.CFG_WSDLURL);
        String catalog = (String)context.get(ToolConstants.CFG_CATALOG);
              
        Object o = context.get(ToolConstants.CFG_BINDING);
        String bindingFiles[]; 
        if (o instanceof String) {
            bindingFiles = new String[] {o.toString()};
        } else {
            bindingFiles = (String[])o;
        }

        // build the in-memory type system
        state = StscState.start();
        // construct the state (have to initialize early in case of errors)
        state.setErrorListener(errorListener);

        EntityResolver cmdLineEntRes = ResolverUtil.resolverForCatalog(catalog);
        typeSystem = loadTypeSystem(wsdl, 
                                     bindingFiles, 
                                     null, 
                                     null, 
                                     null, 
                                     cmdLineEntRes);
        StscState.end();
    }

    public DefaultValueWriter createDefaultValueWriter(QName qn, boolean element) {
        return null;
    }

    public DefaultValueWriter createDefaultValueWriterForWrappedElement(QName wrapperElement, QName qn) {
        return null;
    }

    public String getType(QName qn, boolean element) {
        String ret;
        if (element) {
            ret = typeSystem.findDocumentType(qn).getFullJavaName();                  
            if (ret.contains("$")) {
                ret = ret.substring(0, ret.indexOf('$'));
            }
            return ret;
        }
        ret = typeSystem.findType(qn).getFullJavaName();
        return ret.replace('$', '.');
    }

    public String getWrappedElementType(QName wrapperElement, QName item) {        
        SchemaType st = typeSystem.findElement(wrapperElement).getType();
        SchemaType partType = st.getElementProperty(item).getType();        
        return XMLBeansSchemaTypeUtils.getNaturalJavaClassName(partType);        
    }

    public void generate(ToolContext context) throws ToolException {
        String srcd = (String)context.get(ToolConstants.CFG_OUTPUTDIR);
        String classesd = (String)context.get(ToolConstants.CFG_CLASSDIR);
        boolean verbose = context.optionSet(ToolConstants.CFG_VERBOSE);

        boolean result = true;
        if (errorListener.hasError()) {
            result = false;
        }
        
        File srcDir;
        File classesDir;
        if (srcd == null) {
            String wsdl = (String)context.get(ToolConstants.CFG_WSDLLOCATION);
            try {
                srcd = new File(new URI(wsdl)).getAbsolutePath();
            } catch (URISyntaxException e) {
                srcd = new File(".").getAbsolutePath();
            } 
        }
        srcDir = new File(srcd);
        srcDir.mkdirs();

        if (classesd == null) {
            classesDir = srcDir;
        } else {
            classesDir = new File(classesd);
            classesDir.mkdirs();
        }

        // now code generate and compile the JAR
        if (result) {
            // filer implementation writes binary .xsd and generated source to disk
            final ClassCollector classCollector = context.get(ClassCollector.class);

            FilerImpl filer = new FilerImpl(classesDir, srcDir,
                                            null, verbose, false) {

                public Writer createSourceFile(String typename) throws IOException {
                    String tn = typename;
                    if (tn.contains("$")) {
                        tn = tn.substring(0, tn.indexOf('$'));
                    }
                    String pkg = tn.substring(0, tn.lastIndexOf('.'));
                    tn = tn.substring(tn.lastIndexOf('.') + 1);
                    classCollector.addTypesClassName(pkg, tn, pkg + "." + tn);
                    return super.createSourceFile(typename);
                }
            };

            // currently just for schemaCodePrinter
            XmlOptions options = new XmlOptions();
            /*
            if (codePrinter != null) {
                options.setSchemaCodePrinter(codePrinter);
            }
            */
            options.setGenerateJavaVersion("1.5");

            // save .xsb files
            typeSystem.save(filer);

            // gen source files
            result &= SchemaTypeSystemCompiler.generateTypes(typeSystem, filer, options);
            /*
            for (String s : classCollector.getGeneratedFileInfo()) {
                System.out.println(s);
            }
            */
            for (Map.Entry<String, String> ent : sourcesToCopyMap.entrySet()) {
                try {
                    OutputStream out = filer.createBinaryFile("schema" 
                                                              + SchemaTypeSystemImpl.METADATA_PACKAGE_GEN
                                                              + "/src/" + ent.getValue());
                    URL url = new URL(ent.getKey());
                    InputStream in = url.openStream();
                    IOUtils.copy(in, out);
                    out.close();
                    in.close();
                } catch (Exception e) {
                    //probably not an issue
                }
            }
        }

        if (!result) {
            if (verbose) {
                System.out.println("BUILD FAILED");
            }
            StringBuilder sb = new StringBuilder("Error generating XMLBeans types\n");
            for (XmlError err : errors) {
                if (err.getSeverity() != XmlError.SEVERITY_INFO) {
                    sb.append(err.toString());
                    if (err.getLine() != -1) {
                        sb.append(": ").append(err.getSourceName());
                        sb.append('[').append(Integer.toString(err.getLine()))
                            .append(',').append(Integer.toString(err.getColumn()))
                            .append(']');
                    }
                    sb.append('\n');
                }
            }
            throw new ToolException(sb.toString());
        }

        if (cpResourceLoader != null) {
            cpResourceLoader.close();
        }
    }

    
    private SchemaTypeSystem loadTypeSystem(String wsdlFile, 
                                           String[] configFiles,
                                           Set mdefNamespaces, 
                                           File baseDir, 
                                           File schemasDir,
                                           EntityResolver entResolver) {


        SchemaTypeLoader loader = XmlBeans.typeLoaderForClassLoader(SchemaDocument.class.getClassLoader());

        // parse all the XSD files.
        List<SchemaDocument.Schema> scontentlist = new ArrayList<SchemaDocument.Schema>();
        try {
            URL url = new URL(wsdlFile);
            XmlOptions options = new XmlOptions();
            options.setLoadLineNumbers();
            options.setLoadSubstituteNamespaces(Collections
                .singletonMap("http://schemas.xmlsoap.org/wsdl/",
                              "http://www.apache.org/internal/xmlbeans/wsdlsubst"));
            options.setEntityResolver(entResolver);
            options.setGenerateJavaVersion(XmlOptions.GENERATE_JAVA_15);

            state.addSourceUri(wsdlFile, null);
            loadWSDLDoc(loader, url, options, scontentlist, errorListener);


        } catch (XmlException e) {
            errorListener.add(e.getError());
        } catch (Exception e) {
            StscState.addError(errorListener, XmlErrorCodes.CANNOT_LOAD_FILE, new Object[] {
                "url", wsdlFile, e.getMessage()
            }, (URL)null);
        }

        SchemaDocument.Schema[] sdocs = (SchemaDocument.Schema[])scontentlist
            .toArray(new SchemaDocument.Schema[scontentlist.size()]);
        
        // now the config files.
        List<ConfigDocument.Config> cdoclist = new ArrayList<ConfigDocument.Config>();
        List<File> javaFiles = new ArrayList<File>();
        if (configFiles != null) {
            for (int i = 0; i < configFiles.length; i++) {
                if (configFiles[i].endsWith(".java")) {
                    javaFiles.add(new File(configFiles[i]));
                    continue;
                }
                if (!configFiles[i].endsWith(".xsdconfig")) {
                    //jaxws/jaxb customization file or something else
                    continue;
                }
                try {
                    XmlOptions options = new XmlOptions();
                    options.put(XmlOptions.LOAD_LINE_NUMBERS);
                    options.setEntityResolver(entResolver);
                    options.setLoadSubstituteNamespaces(MAP_COMPATIBILITY_CONFIG_URIS);

                    URI uri = new URI(configFiles[i]);
                    XmlObject configdoc = null;
                    if ("file".equals(uri.getRawSchemeSpecificPart())) {
                        configdoc = loader.parse(new File(uri), null, options);                        
                    } else {
                        InputSource source = new InputSource(configFiles[i]);
                        Document doc = XMLUtils.parse(source);
                        configdoc = loader.parse(doc, null, options);                        
                    }
                    
                    if (!(configdoc instanceof ConfigDocument)) {
                        StscState.addError(errorListener, XmlErrorCodes.INVALID_DOCUMENT_TYPE, new Object[] {
                            configFiles[i], "xsd config"
                        }, configdoc);
                    } else {
                        StscState.addInfo(errorListener, "Loading config file " + configFiles[i]);
                        if (configdoc.validate(new XmlOptions().setErrorListener(errorListener))) {
                            ConfigDocument.Config config = ((ConfigDocument)configdoc).getConfig();
                            cdoclist.add(config);
                            config.setExtensionArray(new Extensionconfig[] {});
                        }
                    }
                } catch (XmlException e) {
                    errorListener.add(e.getError());
                } catch (Exception e) {
                    StscState.addError(errorListener, XmlErrorCodes.CANNOT_LOAD_FILE, new Object[] {
                        "xsd config", configFiles[i], e.getMessage()
                    }, new File(configFiles[i]));
                }
            }
        }
        ConfigDocument.Config[] cdocs = (ConfigDocument.Config[])cdoclist
            .toArray(new ConfigDocument.Config[cdoclist.size()]);


        SchemaTypeLoader linkTo = SchemaTypeLoaderImpl.build(null, cpResourceLoader, null);

        URI baseURI = null;
        if (baseDir != null) {
            baseURI = baseDir.toURI();
        }

        XmlOptions opts = new XmlOptions();
        opts.setCompileDownloadUrls();
        
        
        if (mdefNamespaces != null) {
            opts.setCompileMdefNamespaces(mdefNamespaces);
        }
        opts.setCompileNoValidation(); // already validated here
        opts.setEntityResolver(entResolver);
        opts.setGenerateJavaVersion("1.5");

        // now pass it to the main compile function
        SchemaTypeSystemCompiler.Parameters params = new SchemaTypeSystemCompiler.Parameters();
        params.setSchemas(sdocs);
        params.setConfig(BindingConfigImpl.forConfigDocuments(cdocs, 
                                                              javaFiles.toArray(new File[javaFiles.size()]), 
                                                              CodeGenUtil.systemClasspath()));
        params.setLinkTo(linkTo);
        params.setOptions(opts);
        params.setErrorListener(errorListener);
        params.setJavaize(true);
        params.setBaseURI(baseURI);
        params.setSourcesToCopyMap(sourcesToCopyMap);
        //params.setSchemasDir(schemasDir);
        return SchemaTypeSystemCompiler.compile(params);
    }


    private static void loadWSDLDoc(SchemaTypeLoader loader, URL url, 
                                    XmlOptions options, 
                                    List<Schema> scontentlist, 
                                    XmlErrorWatcher errorListener)
        throws XmlException, IOException {
        XmlObject urldoc = loader.parse(url, null, options);

        if (urldoc instanceof org.apache.xmlbeans.impl.xb.substwsdl.DefinitionsDocument) {
            org.apache.xmlbeans.impl.xb.substwsdl.DefinitionsDocument wsdldoc = 
                (org.apache.xmlbeans.impl.xb.substwsdl.DefinitionsDocument)urldoc;
            
            addWsdlSchemas(url.toString(),
                           wsdldoc,
                           errorListener,
                           scontentlist);
            
            for (TImport imp : wsdldoc.getDefinitions().getImportArray()) {
                URL url1 = new URL(url, imp.getLocation());
                if (imp.getLocation().toLowerCase().endsWith(".xsd")) {
                    XmlObject urldoc2 = loader.parse(url1, null, options);
                    addSchema(url1.toString(), (SchemaDocument)urldoc2, 
                              errorListener, false,
                              scontentlist);
                } else {
                    loadWSDLDoc(loader, url1, options, scontentlist, errorListener);
                }
            }

        } else if (urldoc instanceof SchemaDocument) {
            addSchema(url.toString(), (SchemaDocument)urldoc, errorListener, false,
                      scontentlist);
        } else {
            StscState.addError(errorListener, XmlErrorCodes.INVALID_DOCUMENT_TYPE, new Object[] {
                url, "wsdl or schema"
            }, urldoc);
        }        
    }
  

    private static void addSchema(String name, SchemaDocument schemadoc, XmlErrorWatcher errorListener,
                                  boolean noVDoc, List<SchemaDocument.Schema>  scontentlist) {
        StscState.addInfo(errorListener, "Loading schema file " + name);
        XmlOptions opts = new XmlOptions().setErrorListener(errorListener);
        if (noVDoc) {
            opts.setValidateTreatLaxAsSkip();
        }
        if (schemadoc.validate(opts)) {
            scontentlist.add(schemadoc.getSchema());
        }
    }

    private static void addWsdlSchemas(String name,
                                       org.apache.xmlbeans.impl.xb.substwsdl.DefinitionsDocument wsdldoc,
                                       XmlErrorWatcher errorListener,
                                       List<SchemaDocument.Schema> scontentlist) {
        if (wsdlContainsEncoded(wsdldoc)) {
            StscState
                .addWarning(
                            errorListener,
                            "The WSDL "
                                + name
                                + " uses SOAP encoding. SOAP encoding "
                                + "is not compatible with literal XML Schema.",
                            XmlErrorCodes.GENERIC_ERROR, wsdldoc);
        }
        StscState.addInfo(errorListener, "Loading wsdl file " + name);
        XmlOptions opts = new XmlOptions().setErrorListener(errorListener);
        XmlObject[] types = wsdldoc.getDefinitions().getTypesArray();
        
        int count = 0;
        for (int j = 0; j < types.length; j++) {
            XmlObject[] schemas = types[j]
                .selectPath("declare namespace xs=\"http://www.w3.org/2001/XMLSchema\" xs:schema");
            if (schemas.length == 0) {
                StscState
                    .addWarning(
                                errorListener,
                                "The WSDL "
                                    + name
                                    + " did not have any schema documents in "
                                    + "namespace 'http://www.w3.org/2001/XMLSchema'",
                                XmlErrorCodes.GENERIC_ERROR, wsdldoc);
                continue;
            }

            for (int k = 0; k < schemas.length; k++) {
                if (schemas[k] instanceof SchemaDocument.Schema && schemas[k].validate(opts)) {
                    count++;
                    scontentlist.add((SchemaDocument.Schema)schemas[k]);
                }
            }
        }
        StscState.addInfo(errorListener, "Processing " + count + " schema(s) in " + name);
    }

   

    private static boolean wsdlContainsEncoded(XmlObject wsdldoc) {
        // search for any <soap:body use="encoded"/> etc.
        XmlObject[] useAttrs = wsdldoc
            .selectPath("declare namespace soap='http://schemas.xmlsoap.org/wsdl/soap/' "
                        + ".//soap:body/@use|.//soap:header/@use|.//soap:fault/@use");
        for (int i = 0; i < useAttrs.length; i++) {
            if ("encoded".equals(((SimpleValue)useAttrs[i]).getStringValue())) {
                return true;
            }
        }
        return false;
    }


}
