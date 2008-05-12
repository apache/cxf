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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.writer.FileCodeWriter;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.api.ErrorListener;
import com.sun.tools.xjc.api.S2JJAXBModel;
import com.sun.tools.xjc.api.SchemaCompiler;
import com.sun.tools.xjc.api.XJC;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
/**
 * 
 *
 */
public final class DynamicClientFactory {

    private static final Logger LOG = LogUtils.getL7dLogger(DynamicClientFactory.class);

    private Bus bus;

    private String tmpdir = System.getProperty("java.io.tmpdir");

    private boolean simpleBindingEnabled = true;
    
    private Map<String, Object> jaxbContextProperties;
    
    private DynamicClientFactory(Bus bus) {
        this.bus = bus;
    }

    public void setTemporaryDirectory(String dir) {
        tmpdir = dir;
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

    public Client createClient(String wsdlUrl, QName service) {
        return createClient(wsdlUrl, service, null);
    }

    public Client createClient(String wsdlUrl, QName service, QName port) {
        return createClient(wsdlUrl, service, null, port);
    }

    public Client createClient(String wsdlUrl, QName service, ClassLoader classLoader, QName port) {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        URL u = composeUrl(wsdlUrl);
        LOG.log(Level.FINE, "Creating client from URL " + u.toString());
        ClientImpl client = new ClientImpl(bus, u, service, port);

        Service svc = client.getEndpoint().getService();
        //all SI's should have the same schemas
        Collection<SchemaInfo> schemas = svc.getServiceInfos().get(0).getSchemas();

        SchemaCompiler compiler = XJC.createSchemaCompiler();
        ErrorListener elForRun = new InnerErrorListener(wsdlUrl);
        compiler.setErrorListener(elForRun);

        addSchemas(wsdlUrl, schemas, compiler);
        
        S2JJAXBModel intermediateModel = compiler.bind();
        JCodeModel codeModel = intermediateModel.generateCode(null, elForRun);
        StringBuilder sb = new StringBuilder();
        boolean firstnt = false;

        for (Iterator<JPackage> packages = codeModel.packages(); packages.hasNext();) {
            JPackage jpackage = packages.next();
            String name = jpackage.name();
            if ("org.w3._2001.xmlschema".equals(name)
                || !jpackage.classes().hasNext()) {
                continue;
            }
            if (firstnt) {
                sb.append(':');
            } else {
                firstnt = true;
            }
            sb.append(jpackage.name());
        }
        outputDebug(codeModel);
        
        String packageList = sb.toString();

        // our hashcode + timestamp ought to be enough.
        String stem = toString() + "-" + System.currentTimeMillis();
        File src = new File(tmpdir, stem + "-src");
        if (!src.mkdir()) {
            throw new IllegalStateException("Unable to create working directory " + src.getPath());
        }
        try {
            FileCodeWriter writer = new FileCodeWriter(src);
            codeModel.build(writer);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write generated Java files for schemas: "
                                            + e.getMessage(), e);
        }
        File classes = new File(tmpdir, stem + "-classes");
        if (!classes.mkdir()) {
            throw new IllegalStateException("Unable to create working directory " + src.getPath());
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
        Thread.currentThread().setContextClassLoader(cl);

        TypeClassInitializer visitor = new TypeClassInitializer(svcfo, intermediateModel);
        visitor.walk();
        // delete the classes files
        FileUtils.removeDir(classes);
        return client;
    }

    private void outputDebug(JCodeModel codeModel) {
        if (!LOG.isLoggable(Level.INFO)) {
            return;
        }
        
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (Iterator<JPackage> itr = codeModel.packages(); itr.hasNext();) {
            JPackage package1 = itr.next();
            
            for (Iterator<JDefinedClass> citr = package1.classes(); citr.hasNext();) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(citr.next().fullName());
            }
        }
        
        LOG.log(Level.INFO, "Created classes: " + sb.toString());
        
    }

    private void addSchemas(String wsdlUrl, Collection<SchemaInfo> schemas, SchemaCompiler compiler) {
        int num = 1;
        for (SchemaInfo schema : schemas) {
            Element el = schema.getElement();
            
            compiler.parseSchema(wsdlUrl + "#types" + num, el);
            num++;
        }
        
        if (simpleBindingEnabled && isJaxb21()) {
            String id = "/org/apache/cxf/endpoint/dynamic/simple-binding.xjb";
            LOG.info("Loading the JAXB 2.1 simple binding for client.");
            InputSource source = new InputSource(getClass().getResourceAsStream(id));
            source.setSystemId(id);
            compiler.parseSchema(source);
        }
    }
    
    private boolean isJaxb21() {
        String id = Options.getBuildID();
        StringTokenizer st = new StringTokenizer(id, ".");
        String minor = null;
        
        // major version
        if (st.hasMoreTokens()) {
            st.nextToken();
        }
        
        if (st.hasMoreTokens()) {
            minor = st.nextToken();
        }
        
        try {
            int i = Integer.valueOf(minor);
            if (i >= 1) {
                return true;
            }
        } catch (NumberFormatException e) {
            // do nothing;
        }
        
        return false;
    }

    public boolean isSimpleBindingEnabled() {
        return simpleBindingEnabled;
    }

    public void setSimpleBindingEnabled(boolean simpleBindingEnabled) {
        this.simpleBindingEnabled = simpleBindingEnabled;
    }

    static boolean compileJavaSrc(String classPath, List<File> srcList, String dest) {
        String[] javacCommand = new String[srcList.size() + 7];
        
        javacCommand[0] = "javac";
        javacCommand[1] = "-classpath";
        javacCommand[2] = classPath;        
        javacCommand[3] = "-d";
        javacCommand[4] = dest;
        javacCommand[5] = "-target";
        javacCommand[6] = "1.5";
        
        int i = 7;
        for (File f : srcList) {
            javacCommand[i++] = f.getAbsolutePath();            
        }
        org.apache.cxf.common.util.Compiler javaCompiler 
            = new org.apache.cxf.common.util.Compiler();
        
        return javaCompiler.internalCompile(javacCommand, 7); 
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
                    classPath.append(System.getProperty("path.separator"));
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
                for (URL url : urls) {
                    if (url.getProtocol().startsWith("file")) {
                        File file; 
                        try { 
                            file = new File(url.toURI().getPath()); 
                        } catch (URISyntaxException urise) { 
                            file = new File(url.getPath()); 
                        } 

                        if (file.exists()) { 
                            classPath.append(file.getAbsolutePath()) 
                                .append(System 
                                        .getProperty("path.separator")); 

                            if (file.getName().endsWith(".jar")) { 
                                addClasspathFromManifest(classPath, file); 
                            }                         
                        }     
                    }
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

    private class InnerErrorListener implements ErrorListener {

        private String url;

        InnerErrorListener(String url) {
            this.url = url;
        }

        public void error(SAXParseException arg0) {
            throw new RuntimeException("Error compiling schema from WSDL at {" + url + "}: "
                                       + arg0.getMessage(), arg0);
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
}
