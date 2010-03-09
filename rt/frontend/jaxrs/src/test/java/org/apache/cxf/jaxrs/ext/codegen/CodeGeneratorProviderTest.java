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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.wadl.BookStore;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CodeGeneratorProviderTest extends Assert {

    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        control.makeThreadSafe(true);
    }
    
    @Test
    public void testBookStoreAsInterface() throws Exception {
        generateCodeAndCheck(true, false);
    }
    
    @Test
    public void testBookStoreAsClass() throws Exception {
        generateCodeAndCheck(false, false);
    }
    
    @Test
    public void testBookStoreTypesOnly() throws Exception {
        generateCodeAndCheck(false, true);
    }
    
    private void generateCodeAndCheck(boolean generateInterfaces, boolean typesOnly) throws Exception {
        CodeGeneratorProvider cgp = new CodeGeneratorProvider();
        cgp.setGenerateInterfaces(generateInterfaces);
        
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        
        String query = CodeGeneratorProvider.CODE_QUERY + "&_os=" + getOs();
        if (typesOnly) {
            query += "&_codeType=grammar";
        }
        Message m = mockMessage("http://localhost:8080/baz", "/bar", query, null);
        try {
            cgp.removeCode(cri);
            cgp.setUriInfo(new UriInfoImpl(m, null));
            cgp.handleRequest(m, cri);
            
            String tmpdir = System.getProperty("java.io.tmpdir");
            File classes = new File(tmpdir, cgp.getStem(cri, "classes"));
            if (!classes.mkdir()) {
                fail();
            }
            File unzippedSrc = new File(tmpdir, cgp.getStem(cri, "unzip"));
            if (!unzippedSrc.mkdir()) {
                fail();
            }
            File zipDir = new File(tmpdir, cgp.getStem(cri, "zip"));
            try {             
                compileSrc(classes, unzippedSrc, new FileInputStream(new File(zipDir, "src.zip")));
                verifyClasses(classes, typesOnly);
            } finally {
                FileUtils.removeDir(classes);
                FileUtils.removeDir(unzippedSrc);
            }
        } finally {
            cgp.removeCode(cri);
        }
    }
    
    private void verifyClasses(File classesDir, boolean typesOnly) {
        List<File> clsFiles = FileUtils.getFilesRecurse(classesDir, ".+\\.class$");
        assertEquals(typesOnly ? 5 : 7, clsFiles.size());
        assertTrue(checkContains(clsFiles, "superbooks.Book.class"));
        assertTrue(checkContains(clsFiles, "superbooks.Book2.class"));
        assertTrue(checkContains(clsFiles, "superbooks.Chapter.class"));
        assertTrue(checkContains(clsFiles, "superbooks.ObjectFactory.class"));
        assertTrue(checkContains(clsFiles, "superbooks.package-info.class"));
        if (!typesOnly) {
            assertTrue(checkContains(clsFiles, "org.apache.cxf.jaxrs.model.wadl.FormInterface.class"));
            assertTrue(checkContains(clsFiles, "org.apache.cxf.jaxrs.model.wadl.BookStore.class"));
        }
    }
    
    private boolean checkContains(List<File> clsFiles, String name) {
        
        for (File f : clsFiles) {
            if (f.getAbsolutePath().replace(getPathSep(), ".").endsWith(name)) {
                return true;
            }
        }
        return false;
    }
    private String getPathSep() {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().contains("win")) {
            return "\\";
        } else {
            return "/";
        }
    }
    
    private String getOs() {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().contains("win")) {
            return "win";
        } else {
            return "unix";
        }
    }
    
    private void compileSrc(File classes, File unzippedSrc, InputStream zipFile) throws Exception {
        unzip(zipFile, unzippedSrc);
        StringBuilder classPath = new StringBuilder();
        try {
            setupClasspath(classPath, this.getClass().getClassLoader());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        
        List<File> srcFiles = FileUtils.getFilesRecurse(unzippedSrc, ".+\\.java$"); 
        if (!compileJavaSrc(classPath.toString(), srcFiles, classes.toString())) {
            fail("Could not compile classes");
        }
    }

    private void unzip(InputStream fin, File unzippedSrc) throws Exception {
        ZipInputStream zin = new ZipInputStream(fin);
        ZipEntry ze = null;
        while ((ze = zin.getNextEntry()) != null) {
            String entryName = ze.getName();
            int index = entryName.lastIndexOf(System.getProperty("file.separator"));
            File packageDir = new File(unzippedSrc, entryName.substring(0, index));
            packageDir.mkdirs();
            FileOutputStream fout = new FileOutputStream(
                                        new File(packageDir, entryName.substring(index + 1)));
            for (int c = zin.read(); c != -1; c = zin.read()) {
                fout.write(c);
            }
            zin.closeEntry();
            fout.close();
        }
        zin.close();
    }
    
    protected boolean compileJavaSrc(String classPath, List<File> srcList, String dest) {
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
                        File file;
                        if (url.toURI().getPath() == null) {
                            continue;
                        }
                        try { 
                            file = new File(url.toURI().getPath()); 
                        } catch (URISyntaxException urise) { 
                            if (url.getPath() == null) {
                                continue;
                            }
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
    
    private Message mockMessage(String baseAddress, String pathInfo, String query,
                                List<ClassResourceInfo> cris) {
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        e.put(Service.class, new JAXRSServiceImpl(cris));
        
        m.setExchange(e);
        control.reset();
        ServletDestination d = control.createMock(ServletDestination.class);
        
        EndpointInfo epr = new EndpointInfo();
        epr.setAddress(baseAddress);
        
        epr.setProperty("org.apache.cxf.http.case_insensitive_queries", "false");
        epr.setProperty("org.apache.cxf.endpoint.private", "false");
        
        BindingInfo bi = control.createMock(BindingInfo.class);
        bi.getProperties();
        EasyMock.expectLastCall().andReturn(new HashMap<String, Object>()).anyTimes();
        epr.setBinding(bi);
        
        d.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(epr).anyTimes();
        e.setDestination(d);
        m.put(Message.REQUEST_URI, pathInfo);
        m.put(Message.QUERY_STRING, query);
        m.put(Message.HTTP_REQUEST_METHOD, "GET");
        Endpoint endpoint = control.createMock(Endpoint.class);
        e.put(Endpoint.class, endpoint);
        endpoint.get(ProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(ProviderFactory.getSharedInstance());
        endpoint.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(epr).anyTimes();
        control.replay();
        return m;
    }
    
}
