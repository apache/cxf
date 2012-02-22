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
package org.apache.cxf.endpoint.dynamic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.XMLFilterImpl;

import com.sun.tools.xjc.reader.internalizer.AbstractReferenceFinderImpl;
import com.sun.tools.xjc.reader.internalizer.DOMForest;
import com.sun.tools.xjc.reader.internalizer.InternalizationLogic;
//import com.sun.tools.xjc.reader.xmlschema.parser.XMLSchemaInternalizationLogic;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.catalog.OASISCatalogManagerHelper;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.EndpointImplFactory;
import org.apache.cxf.endpoint.SimpleEndpointImplFactory;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.jaxb.JAXBUtils.JCodeModel;
import org.apache.cxf.jaxb.JAXBUtils.JDefinedClass;
import org.apache.cxf.jaxb.JAXBUtils.JPackage;
import org.apache.cxf.jaxb.JAXBUtils.Options;
import org.apache.cxf.jaxb.JAXBUtils.S2JJAXBModel;
import org.apache.cxf.jaxb.JAXBUtils.SchemaCompiler;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaSerializer;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
/**
 * This class reads a WSDL and creates a dynamic client from it.
 * 
 * Use {@link #newInstance} to obtain an instance, and then
 * {@link #createClient(String)} (or other overloads) to create a client.
 * 
 * It uses the JAXB data binding. It does not set up complex interceptors for 
 * features such as attachments. 
 * See {@link org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory}
 * for an alternative that sets up JAX-WS endpoints.
 *
 * This class may be subclassed to allow for other endpoints or behaviors.
 */
public class DynamicClientFactory {

    private static final Logger LOG = LogUtils.getL7dLogger(DynamicClientFactory.class);

    private Bus bus;

    private String tmpdir = SystemPropertyAction.getProperty("java.io.tmpdir");

    private boolean simpleBindingEnabled = true;
    private boolean allowRefs;
    
    private Map<String, Object> jaxbContextProperties;
    
    protected DynamicClientFactory(Bus bus) {
        this.bus = bus;
    }
    
    protected EndpointImplFactory getEndpointImplFactory() {
        return SimpleEndpointImplFactory.getSingleton();
    }

    public void setTemporaryDirectory(String dir) {
        tmpdir = dir;
    }
    
    public void setAllowElementReferences(boolean b) {
        allowRefs = b;
    }

    /**
     * Create a new instance using a specific <tt>Bus</tt>.
     * 
     * @param b the <tt>Bus</tt> to use in subsequent operations with the
     *            instance
     * @return the new instance
     */
    public static DynamicClientFactory newInstance(Bus b) {
        return new DynamicClientFactory(b);
    }

    /**
     * Create a new instance using a default <tt>Bus</tt>.
     * 
     * @return the new instance
     * @see CXFBusFactory#getDefaultBus()
     */
    public static DynamicClientFactory newInstance() {
        Bus bus = CXFBusFactory.getThreadDefaultBus();
        return new DynamicClientFactory(bus);
    }

    /**
     * Create a new <code>Client</code> instance using the WSDL to be loaded
     * from the specified URL and using the current classloading context.
     * 
     * @param wsdlURL the URL to load
     * @return
     */
    public Client createClient(String wsdlUrl) {
        return createClient(wsdlUrl, (QName)null, (QName)null);
    }
    public Client createClient(String wsdlUrl, List<String> bindingFiles) {
        return createClient(wsdlUrl, (QName)null, (QName)null, bindingFiles);
    }
    
    
    /**
     * Create a new <code>Client</code> instance using the WSDL to be loaded
     * from the specified URL and using the current classloading context.
     * 
     * @param wsdlURL the URL to load
     * @return
     */
    public Client createClient(URL wsdlUrl) {
        return createClient(wsdlUrl, (QName)null, (QName)null);
    }
    public Client createClient(URL wsdlUrl, List<String> bindingFiles) {
        return createClient(wsdlUrl, (QName)null, (QName)null, bindingFiles);
    }

    /**
     * Create a new <code>Client</code> instance using the WSDL to be loaded
     * from the specified URL and with the specified <code>ClassLoader</code>
     * as parent.
     * 
     * @param wsdlUrl
     * @param classLoader
     * @return
     */
    public Client createClient(String wsdlUrl, ClassLoader classLoader) {
        return createClient(wsdlUrl, null, classLoader, null);
    }
    public Client createClient(String wsdlUrl, ClassLoader classLoader, List<String> bindingFiles) {
        return createClient(wsdlUrl, null, classLoader, null, bindingFiles);
    }

    public Client createClient(String wsdlUrl, QName service) {
        return createClient(wsdlUrl, service, (QName)null);
    }
    public Client createClient(String wsdlUrl, QName service, List<String> bindingFiles) {
        return createClient(wsdlUrl, service, null, bindingFiles);
    }

    public Client createClient(String wsdlUrl, QName service, QName port) {
        return createClient(wsdlUrl, service, null, port);
    }
    public Client createClient(String wsdlUrl, QName service, QName port, List<String> bindingFiles) {
        return createClient(wsdlUrl, service, null, port, bindingFiles);
    }

    public Client createClient(String wsdlUrl, QName service, ClassLoader classLoader, QName port) {
        return createClient(wsdlUrl, service, classLoader, port, null);
    }

    
    /**
     * Create a new <code>Client</code> instance using the WSDL to be loaded
     * from the specified URL and with the specified <code>ClassLoader</code>
     * as parent.
     * 
     * @param wsdlUrl
     * @param classLoader
     * @return
     */
    public Client createClient(URL wsdlUrl, ClassLoader classLoader) {
        return createClient(wsdlUrl, null, classLoader, null);
    }
    public Client createClient(URL wsdlUrl, ClassLoader classLoader, List<String> bindingFiles) {
        return createClient(wsdlUrl.toString(), null, classLoader, null, bindingFiles);
    }

    public Client createClient(URL wsdlUrl, QName service) {
        return createClient(wsdlUrl, service, (QName)null);
    }
    public Client createClient(URL wsdlUrl, QName service, List<String> bindingFiles) {
        return createClient(wsdlUrl, service, null, bindingFiles);
    }

    public Client createClient(URL wsdlUrl, QName service, QName port) {
        return createClient(wsdlUrl, service, null, port);
    }
    public Client createClient(URL wsdlUrl, QName service, QName port, List<String> bindingFiles) {
        return createClient(wsdlUrl.toString(), service, null, port, bindingFiles);
    }

    public Client createClient(URL wsdlUrl, QName service, ClassLoader classLoader, QName port) {
        return createClient(wsdlUrl.toString(), service, classLoader, port, null);
    }
    
    public Client createClient(URL wsdlUrl, 
                               QName service, 
                               ClassLoader classLoader, 
                               QName port, 
                               List<String> bindingFiles) {
        return createClient(wsdlUrl.toString(), service, classLoader, port, bindingFiles);
    }
    
    
    
    public Client createClient(String wsdlUrl, QName service,
                               ClassLoader classLoader, QName port,
                               List<String> bindingFiles) {
            
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        LOG.log(Level.FINE, "Creating client from WSDL " + wsdlUrl);

        WSDLServiceFactory sf = (service == null)
            ? (new WSDLServiceFactory(bus, wsdlUrl)) : (new WSDLServiceFactory(bus, wsdlUrl, service));
        sf.setAllowElementRefs(allowRefs);
        Service svc = sf.create();

        ClientImpl client = new ClientImpl(bus, svc, port,
                                           getEndpointImplFactory());

        //all SI's should have the same schemas
        SchemaCollection schemas = svc.getServiceInfos().get(0).getXmlSchemaCollection();

        SchemaCompiler compiler = 
            JAXBUtils.createSchemaCompilerWithDefaultAllocator(new HashSet<String>());
        
        InnerErrorListener listener = new InnerErrorListener(wsdlUrl);
        Object elForRun = ReflectionInvokationHandler
            .createProxyWrapper(listener,
                                JAXBUtils.getParamClass(compiler, "setErrorListener"));
        
        compiler.setErrorListener(elForRun);
        
        OASISCatalogManager catalog = bus.getExtension(OASISCatalogManager.class);
        hackInNewInternalizationLogic(compiler, catalog);

        addSchemas(compiler.getOptions(), compiler, svc.getServiceInfos(), schemas);
        addBindingFiles(bindingFiles, compiler);
        S2JJAXBModel intermediateModel = compiler.bind();
        
        listener.throwException();
        
        JCodeModel codeModel = intermediateModel.generateCode(null, elForRun);
        StringBuilder sb = new StringBuilder();
        boolean firstnt = false;

        for (Iterator<JPackage> packages = codeModel.packages(); packages.hasNext();) {
            JPackage jpackage = packages.next();
            if (!isValidPackage(jpackage)) {
                continue;
            }
            if (firstnt) {
                sb.append(':');
            } else {
                firstnt = true;
            }
            sb.append(jpackage.name());
        }
        JAXBUtils.logGeneratedClassNames(LOG, codeModel);
        
        String packageList = sb.toString();

        // our hashcode + timestamp ought to be enough.
        String stem = toString() + "-" + System.currentTimeMillis();
        File src = new File(tmpdir, stem + "-src");
        if (!src.mkdir()) {
            throw new IllegalStateException("Unable to create working directory " + src.getPath());
        }
        try {
            Object writer = JAXBUtils.createFileCodeWriter(src);
            codeModel.build(writer);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to write generated Java files for schemas: "
                                            + e.getMessage(), e);
        }
        File classes = new File(tmpdir, stem + "-classes");
        if (!classes.mkdir()) {
            throw new IllegalStateException("Unable to create working directory " + classes.getPath());
        }
        StringBuilder classPath = new StringBuilder();
        try {
            setupClasspath(classPath, classLoader);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        
        List<File> srcFiles = FileUtils.getFilesRecurse(src, ".+\\.java$"); 
        if (!compileJavaSrc(classPath.toString(), srcFiles, classes.toString())) {
            LOG.log(Level.SEVERE , new Message("COULD_NOT_COMPILE_SRC", LOG, wsdlUrl).toString());
        }
        FileUtils.removeDir(src);
        URLClassLoader cl;
        try {
            cl = new URLClassLoader(new URL[] {classes.toURI().toURL()}, classLoader);
        } catch (MalformedURLException mue) {
            throw new IllegalStateException("Internal error; a directory returns a malformed URL: "
                                            + mue.getMessage(), mue);
        }

        JAXBContext context;
        Map<String, Object> contextProperties = jaxbContextProperties;
        
        if (contextProperties == null) {
            contextProperties = Collections.emptyMap();
        }
        
        try {
            if (StringUtils.isEmpty(packageList)) {
                context = JAXBContext.newInstance(new Class[0], contextProperties);
            } else {
                context = JAXBContext.newInstance(packageList, cl, contextProperties);
            }
        } catch (JAXBException jbe) {
            throw new IllegalStateException("Unable to create JAXBContext for generated packages: "
                                            + jbe.getMessage(), jbe);
        }
         
        JAXBDataBinding databinding = new JAXBDataBinding();
        databinding.setContext(context);
        svc.setDataBinding(databinding);

        ServiceInfo svcfo = client.getEndpoint().getEndpointInfo().getService();

        // Setup the new classloader!
        ClassLoaderUtils.setThreadContextClassloader(cl);

        TypeClassInitializer visitor = new TypeClassInitializer(svcfo, 
                                                                intermediateModel,
                                                                allowWrapperOps());
        visitor.walk();
        // delete the classes files
        FileUtils.removeDir(classes);
        return client;
    }
    protected boolean allowWrapperOps() {
        return false;
    }
    
    private void addBindingFiles(List<String> bindingFiles, SchemaCompiler compiler) {
        if (bindingFiles != null) {
            for (String s : bindingFiles) {
                URL url = composeUrl(s);
                try {
                    InputStream ins = url.openStream();
                    InputSource is = new InputSource(ins);
                    is.setSystemId(url.toString());
                    is.setPublicId(url.toString());
                    compiler.getOptions().addBindFile(is);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private boolean isValidPackage(JPackage jpackage) {
        if (jpackage == null) {
            return false;
        }
        String name = jpackage.name();
        if ("org.w3._2001.xmlschema".equals(name)
            || "java.lang".equals(name)
            || "java.io".equals(name)
            || "generated".equals(name)) {
            return false;
        }
        Iterator<JDefinedClass> i = jpackage.classes();
        while (i.hasNext()) {
            JDefinedClass current = i.next();
            if ("ObjectFactory".equals(current.name())) { 
                return true;
            }
        }
        return false;
    }

    
    private void addSchemas(Options opts,
                            SchemaCompiler schemaCompiler,
                            List<ServiceInfo> serviceList,
                            SchemaCollection schemaCollection) {

        Map<String, Element> done = new HashMap<String, Element>();
        Map<String, Element> notDone = new HashMap<String, Element>();
        OASISCatalogManager catalog = bus.getExtension(OASISCatalogManager.class);
        for (XmlSchema schema : schemaCollection.getXmlSchemas()) {
            if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(schema.getTargetNamespace())) {
                continue;
            }
            String key = schema.getSourceURI();
            if (done.containsKey(key)) {
                continue;
            }
            if (!key.startsWith("file:") && !key.startsWith("jar:")) {
                XmlSchemaSerializer xser = new XmlSchemaSerializer();
                xser.setExtReg(schemaCollection.getExtReg());
                Document[] docs;
                try {
                    docs = xser.serializeSchema(schema, false);
                } catch (XmlSchemaSerializerException e) {
                    throw new RuntimeException(e);
                }
                Element ele = docs[0].getDocumentElement();
                ele = removeImportElement(ele, key, catalog, done, notDone);
                try {
                    docs[0].setDocumentURI(key);
                } catch (Throwable t) {
                    //ignore - DOM level 3
                }
                if (ele != null) {
                    InputSource is = new InputSource((InputStream)null);
                    //key = key.replaceFirst("#types[0-9]+$", "");
                    is.setSystemId(key);
                    is.setPublicId(key);
                    opts.addGrammar(is);
                    schemaCompiler.parseSchema(key, ele);
                }
            }
        }
        for (XmlSchema schema : schemaCollection.getXmlSchemas()) {
            if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(schema.getTargetNamespace())) {
                continue;
            }
            String key = schema.getSourceURI();
            if (done.containsKey(key)) {
                continue;
            }
            if (key.startsWith("file:") || key.startsWith("jar:")) {
                InputStream in = null;
                try {
                    if (key.contains("#")) {
                        for (ServiceInfo si : serviceList) {
                            for (SchemaInfo sci : si.getSchemas()) {
                                if (key.equals(sci.getSystemId())) {
                                    key = null;
                                }
                            }
                        }
                       
                    }
                    if (key == null) {
                        continue;
                    }
                    if (key.startsWith("file:")) {
                        in = new FileInputStream(new File(new URI(key)));
                    } else {
                        in = new URL(key).openStream();
                    }

                    XMLStreamReader reader = StaxUtils.createXMLStreamReader(key, in);
                    reader = new LocationFilterReader(reader, catalog);
                    InputSource is = new InputSource(key);
                    opts.addGrammar(is);
                    schemaCompiler.parseSchema(key, reader);
                    reader.close();
                } catch (RuntimeException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            //ignore
                        }
                    }
                }
            }
        }
        for (ServiceInfo si : serviceList) {
            for (SchemaInfo sci : si.getSchemas()) {
                String key = sci.getSystemId();
                if (done.containsKey(key)) {
                    continue;
                }
                Element ele = sci.getElement();
                ele = removeImportElement(ele, key, catalog, done, notDone);
                if (ele != null) {
                    InputSource is = new InputSource((InputStream)null);
                    //key = key.replaceFirst("#types[0-9]+$", "");
                    is.setSystemId(key);
                    is.setPublicId(key);
                    opts.addGrammar(is);
                    schemaCompiler.parseSchema(key, StaxUtils.createXMLStreamReader(ele, key));
                }
            }
        }
        for (Map.Entry<String, Element> el : notDone.entrySet()) {
            InputSource is = new InputSource((InputStream)null);
            //key = key.replaceFirst("#types[0-9]+$", "");
            is.setSystemId(el.getKey());
            is.setPublicId(el.getKey());
            opts.addGrammar(is);
            schemaCompiler.parseSchema(el.getKey(),
                                       StaxUtils.createXMLStreamReader(el.getValue(),
                                                                       el.getKey()));

        }

    }
    
    public boolean isSimpleBindingEnabled() {
        return simpleBindingEnabled;
    }

    public void setSimpleBindingEnabled(boolean simpleBindingEnabled) {
        this.simpleBindingEnabled = simpleBindingEnabled;
    }

    protected boolean compileJavaSrc(String classPath, List<File> srcList, String dest) {
        org.apache.cxf.common.util.Compiler javaCompiler 
            = new org.apache.cxf.common.util.Compiler();
        
        javaCompiler.setClassPath(classPath);
        javaCompiler.setOutputDir(dest);
        javaCompiler.setTarget("1.5");
        
        return javaCompiler.compileFiles(srcList); 
    }
    
    static void addClasspathFromManifest(StringBuilder classPath, File file) 
        throws URISyntaxException, IOException {
        
        JarFile jar = new JarFile(file);
        Attributes attr = null;
        if (jar.getManifest() != null) {
            attr = jar.getManifest().getMainAttributes();
        }
        if (attr != null) {
            String cp = attr.getValue("Class-Path");
            while (cp != null) {
                String fileName = cp;
                int idx = fileName.indexOf(' ');
                if (idx != -1) {
                    fileName = fileName.substring(0, idx);
                    cp =  cp.substring(idx + 1).trim();
                } else {
                    cp = null;
                }
                URI uri = new URI(fileName);
                File f2;
                if (uri.isAbsolute()) {
                    f2 = new File(uri);
                } else {
                    f2 = new File(file, fileName);
                }
                if (f2.exists()) {
                    classPath.append(f2.getAbsolutePath());
                    classPath.append(File.pathSeparator);
                }
            }
        }         
    }

    static void setupClasspath(StringBuilder classPath, ClassLoader classLoader)
        throws URISyntaxException, IOException {
        
        ClassLoader scl = ClassLoader.getSystemClassLoader();        
        ClassLoader tcl = classLoader;
        do {
            if (tcl instanceof URLClassLoader) {
                URL[] urls = ((URLClassLoader)tcl).getURLs();
                if (urls == null) {
                    urls = new URL[0];
                }
                for (URL url : urls) {
                    if (url.getProtocol().startsWith("file")) {
                        File file = null;
                        // CXF-3884 use url-decoder to get the decoded file path from the url
                        try {
                            if (url.getPath() == null) {
                                continue;
                            }
                            file = new File(URLDecoder.decode(url.getPath(), "utf-8")); 
                        } catch (UnsupportedEncodingException uee) {
                            // ignored as utf-8 is supported
                        } 

                        if (null != file && file.exists()) { 
                            classPath.append(file.getAbsolutePath()) 
                                .append(System 
                                        .getProperty("path.separator")); 

                            if (file.getName().endsWith(".jar")) { 
                                addClasspathFromManifest(classPath, file); 
                            }                         
                        }     
                    }
                }
            } else if (tcl.getClass().getName().contains("weblogic")) {
                // CXF-2549: Wrong classpath for dynamic client compilation in Weblogic
                try {
                    Method method = tcl.getClass().getMethod("getClassPath");
                    Object weblogicClassPath = method.invoke(tcl);
                    classPath.append(weblogicClassPath)
                        .append(File.pathSeparator); 
                } catch (Exception e) {
                    LOG.log(Level.FINE, "unsuccessfully tried getClassPath method", e);
                }
            }
            tcl = tcl.getParent();
            if (null == tcl) {
                break;
            }
        } while(!tcl.equals(scl.getParent()));
    }

    private URL composeUrl(String s) {
        try {
            URIResolver resolver = new URIResolver(null, s, getClass());

            if (resolver.isResolved()) {
                return resolver.getURI().toURL();
            } else {
                throw new ServiceConstructionException(new Message("COULD_NOT_RESOLVE_URL", LOG, s));
            }
        } catch (IOException e) {
            throw new ServiceConstructionException(new Message("COULD_NOT_RESOLVE_URL", LOG, s), e);
        }
    }

    class InnerErrorListener {

        private String url;
        private StringBuilder errors = new StringBuilder();
        private Exception ex;

        InnerErrorListener(String url) {
            this.url = url;
        }

        public void throwException() {
            if (errors.length() > 0) {
                throw new RuntimeException(errors.toString(), ex);
            }
        }
        public void error(SAXParseException arg0) {
            if (ex == null) {
                ex = arg0;
            }
            if (errors.length() == 0) {
                errors.append("Error compiling schema from WSDL at {").append(url).append("}: \n");
            } else {
                errors.append("\n");
            }
            if (arg0.getLineNumber() > 0) {
                errors.append(arg0.getLocalizedMessage() + "\n"
                    + " at line " + arg0.getLineNumber()
                    + " column " + arg0.getColumnNumber()
                    + " of schema " + arg0.getSystemId()
                    + "\n");
            } else {
                errors.append(arg0.getMessage());
                errors.append("\n");
            }
        }

        public void fatalError(SAXParseException arg0) {
            throw new RuntimeException("Fatal error compiling schema from WSDL at {" + url + "}: "
                                       + arg0.getMessage(), arg0);
        }

        public void info(SAXParseException arg0) {
            // ignore
        }

        public void warning(SAXParseException arg0) {
            // ignore
        }
    }

    // sorry, but yuck. Try a file first?!?
    static class RelativeEntityResolver implements EntityResolver {
        private String baseURI;

        public RelativeEntityResolver(String baseURI) {
            super();
            this.baseURI = baseURI;
        }

        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            // the system id is null if the entity is in the wsdl.
            if (systemId != null) {
                File file = new File(baseURI, systemId);
                if (file.exists()) {
                    return new InputSource(new FileInputStream(file));
                } else {
                    return new InputSource(systemId);
                }
            }
            return null;
        }
    }

    /**
     * Return the map of JAXB context properties used at the time that we create new contexts.
     * @return the map
     */
    public Map<String, Object> getJaxbContextProperties() {
        return jaxbContextProperties;
    }

    /**
     * Set the map of JAXB context properties used at the time that we create new contexts.
     * @param jaxbContextProperties
     */
    public void setJaxbContextProperties(Map<String, Object> jaxbContextProperties) {
        this.jaxbContextProperties = jaxbContextProperties;
    }
    
    
    private void hackInNewInternalizationLogic(SchemaCompiler schemaCompiler,
                                               final OASISCatalogManager catalog) {
        try {
            Object o = ((ReflectionInvokationHandler)Proxy.getInvocationHandler(schemaCompiler)).getTarget();
            Field f = o.getClass().getDeclaredField("forest");
            Object forest = ReflectionUtil.setAccessible(f).get(o);
            // Set the error handler
            for (Method m : forest.getClass().getMethods()) {
                if ("setErrorHandler".equals(m.getName())) {
                    m.invoke(forest, o);
                }
            }
            
            f = forest.getClass().getDeclaredField("logic");
            Object xil = ReflectionUtil.setAccessible(f).get(forest);
            if (xil.getClass().getName().contains(".internal.")) {
                xil = createWrapperLogic(xil, catalog);
                ReflectionUtil.setAccessible(f).set(forest, xil);
            }
        } catch (Throwable ex)  {
            //ignore
            ex.printStackTrace();
        }
    }
    private Object createWrapperLogic(final Object xil, final OASISCatalogManager catalog) {
        try {
            return new InternalizationLogic() {
                public XMLFilterImpl createExternalReferenceFinder(DOMForest parent) {
                    return new ReferenceFinder(parent, catalog);
                }
                public boolean checkIfValidTargetNode(DOMForest parent, Element bindings, Element target) {
                    return ((InternalizationLogic)xil).checkIfValidTargetNode(parent, bindings, target);
                }
                public Element refineTarget(Element target) {
                    return ((InternalizationLogic)xil).refineTarget(target);
                }
            };
        } catch (Throwable ex) {
            //ignore
            return xil;
        }
    }

    private static final class ReferenceFinder extends AbstractReferenceFinderImpl {
        private Locator locator;
        private OASISCatalogManager catalog;

        ReferenceFinder(DOMForest parent, OASISCatalogManager cat) {
            super(parent);
            catalog = cat;
        }

        public void setDocumentLocator(Locator loc) {
            super.setDocumentLocator(loc);
            this.locator = loc;
        }
        protected String findExternalResource(String nsURI, String localName, org.xml.sax.Attributes atts) {
            if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(nsURI)
                && ("import".equals(localName)
                    || "include".equals(localName))) {
                String s = atts.getValue("schemaLocation");
                if (!StringUtils.isEmpty(s)) {
                    s = mapSchemaLocation(s, locator.getSystemId(), catalog);
                }
                return s;
            }
            return null;
        }
    }
    static String mapSchemaLocation(String target, String base, OASISCatalogManager catalog) {
        try {
            String resolvedLocation = new OASISCatalogManagerHelper().resolve(catalog, 
                                                                              target, base);
            if (resolvedLocation != null) {
                return resolvedLocation;
            }

        } catch (Exception ex) {
            //ignore
        }
        

        try {
            URIResolver resolver = new URIResolver(base, target);
            if (resolver.isResolved()) {
                target = resolver.getURI().toString();
            }
        } catch (Exception ex) {
            //ignore
        }
        return target;
    }
    private Element removeImportElement(Element element,
                                        String sysId,
                                        OASISCatalogManager catalog,
                                        Map<String, Element> done,
                                        Map<String, Element> notDone) {
        List<Element> impElemList = DOMUtils.findAllElementsByTagNameNS(element,
                                                                        "http://www.w3.org/2001/XMLSchema",
                                                                     "import");
        List<Element> incElemList = DOMUtils.findAllElementsByTagNameNS(element,
                                                                        "http://www.w3.org/2001/XMLSchema",
                                                                     "include");
        if (impElemList.size() == 0 && incElemList.size() == 0) {
            return element;
        }
        element = (Element)cloneNode(element.getOwnerDocument(), element, true);
        List<Node> ns = new ArrayList<Node>();

        impElemList = DOMUtils.findAllElementsByTagNameNS(element,
                                                          "http://www.w3.org/2001/XMLSchema",
                                                           "import");
        for (Element elem : impElemList) {
            Node importNode = elem;
            ns.add(importNode);
        }
        for (Node item : ns) {
            Node schemaNode = item.getParentNode();
            schemaNode.removeChild(item);
        }

        incElemList = DOMUtils.findAllElementsByTagNameNS(element,
                                                          "http://www.w3.org/2001/XMLSchema",
                                                          "include");
        
        boolean addedToNotDone = false;
        for (Element elem : incElemList) {
            Attr val = elem.getAttributeNode("schemaLocation");
            String s = mapSchemaLocation(val.getNodeValue(), sysId, catalog);
            val.setNodeValue(s);
            if (!done.containsKey(s)) {
                notDone.put(sysId, element);
                addedToNotDone = true;
            }
        }

        return addedToNotDone ? null : element;
    }

    public Node cloneNode(Document document, Node node, boolean deep) throws DOMException {
        if (document == null || node == null) {
            return null;
        }
        int type = node.getNodeType();

        if (node.getOwnerDocument() == document) {
            return node.cloneNode(deep);
        }
        Node clone;
        switch (type) {
        case Node.CDATA_SECTION_NODE:
            clone = document.createCDATASection(node.getNodeValue());
            break;
        case Node.COMMENT_NODE:
            clone = document.createComment(node.getNodeValue());
            break;
        case Node.ENTITY_REFERENCE_NODE:
            clone = document.createEntityReference(node.getNodeName());
            break;
        case Node.ELEMENT_NODE:
            clone = document.createElement(node.getNodeName());
            NamedNodeMap attributes = node.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                ((Element)clone).setAttribute(attributes.item(i).getNodeName(), attributes.item(i)
                    .getNodeValue());
            }
            break;

        case Node.TEXT_NODE:
            clone = document.createTextNode(node.getNodeValue());
            break;
        default:
            return null;
        }
        if (deep && type == Node.ELEMENT_NODE) {
            Node child = node.getFirstChild();
            while (child != null) {
                clone.appendChild(cloneNode(document, child, true));
                child = child.getNextSibling();
            }
        }
        return clone;
    }
    public class LocationFilterReader extends StreamReaderDelegate implements XMLStreamReader {
        boolean isImport;
        boolean isInclude;
        int locIdx = -1;
        OASISCatalogManager catalog;

        LocationFilterReader(XMLStreamReader read, OASISCatalogManager catalog) {
            super(read);
            this.catalog = catalog;
        }

        public int next() throws XMLStreamException {
            int i = super.next();
            if (i == XMLStreamReader.START_ELEMENT) {
                QName qn = super.getName();
                isInclude = qn.equals(WSDLConstants.QNAME_SCHEMA_INCLUDE);
                isImport = qn.equals(WSDLConstants.QNAME_SCHEMA_IMPORT);
                if (isImport) {
                    findLocation();
                } else {
                    locIdx = -1;
                }
            } else {
                isImport = false;
                locIdx = -1;
            }
            return i;
        }

        public int nextTag() throws XMLStreamException {
            int i = super.nextTag();
            if (i == XMLStreamReader.START_ELEMENT) {
                QName qn = super.getName();
                isInclude = qn.equals(WSDLConstants.QNAME_SCHEMA_INCLUDE);
                isImport = qn.equals(WSDLConstants.QNAME_SCHEMA_IMPORT);
                if (isImport) {
                    findLocation();
                } else {
                    locIdx = -1;
                }
            } else {
                isImport = false;
                locIdx = -1;
            }
            return i;
        }
        private void findLocation() {
            locIdx = -1;
            for (int x = super.getAttributeCount(); x > 0; --x) {
                String nm = super.getAttributeLocalName(x - 1);
                if ("schemaLocation".equals(nm)) {
                    locIdx = x - 1;
                }
            }
        }
        public int getAttributeCount() {
            int i = super.getAttributeCount();
            if (locIdx != -1) {
                --i;
            }
            return i;
        }
        private int mapIdx(int index) {
            if (locIdx != -1
                && index >= locIdx) {
                ++index;
            }
            return index;
        }

        private String mapSchemaLocation(String target) {
            return DynamicClientFactory.mapSchemaLocation(target, this.getLocation().getSystemId(), catalog);
        }

        public String getAttributeValue(String namespaceURI, String localName) {
            if (isInclude && "schemaLocation".equals(localName)) {
                return mapSchemaLocation(super.getAttributeValue(namespaceURI, localName));
            }
            return super.getAttributeValue(namespaceURI, localName);
        }
        public String getAttributeValue(int index) {
            if (isInclude) {
                String n = getAttributeLocalName(index);
                if ("schemaLocation".equals(n)) {
                    return mapSchemaLocation(super.getAttributeValue(index));
                }
            }
            return super.getAttributeValue(mapIdx(index));
        }

        public QName getAttributeName(int index) {
            return super.getAttributeName(mapIdx(index));
        }

        public String getAttributePrefix(int index) {
            return super.getAttributePrefix(mapIdx(index));
        }

        public String getAttributeNamespace(int index) {
            return super.getAttributeNamespace(mapIdx(index));
        }

        public String getAttributeLocalName(int index) {
            return super.getAttributeLocalName(mapIdx(index));
        }

        public String getAttributeType(int index) {
            return super.getAttributeType(mapIdx(index));
        }


        public boolean isAttributeSpecified(int index) {
            return super.isAttributeSpecified(mapIdx(index));
        }
    }

}
