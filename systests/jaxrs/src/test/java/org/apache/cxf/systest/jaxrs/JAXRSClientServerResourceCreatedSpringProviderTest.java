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

package org.apache.cxf.systest.jaxrs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.codegen.CodeGeneratorProvider;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSClientServerResourceCreatedSpringProviderTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerResourceCreatedSpringProviders.class));
    }
    
    @Test
    public void testMultipleRootsWadl() throws Exception {
        List<Element> resourceEls = getWadlResourcesInfo("http://localhost:9080/webapp/",
                                                         "http://localhost:9080/webapp/", 2);
        String path1 = resourceEls.get(0).getAttribute("path");
        int bookStoreInd = path1.contains("/bookstore") ? 0 : 1;
        int petStoreInd = bookStoreInd == 0 ? 1 : 0;
        checkBookStoreInfo(resourceEls.get(bookStoreInd));
        checkPetStoreInfo(resourceEls.get(petStoreInd));
    }
    
    @Test
    public void testBookStoreWadl() throws Exception {
        List<Element> resourceEls = getWadlResourcesInfo("http://localhost:9080/webapp/",
                                                         "http://localhost:9080/webapp/bookstore", 1);
        checkBookStoreInfo(resourceEls.get(0));
    }
    
    @Test
    public void testPetStoreWadl() throws Exception {
        List<Element> resourceEls = getWadlResourcesInfo("http://localhost:9080/webapp/",
                                                         "http://localhost:9080/webapp/petstore", 1);
        checkPetStoreInfo(resourceEls.get(0));
    }
    
    @Test
    public void testPetStoreSource() throws Exception {
        try {
            WebClient wc = WebClient.create("http://localhost:9080/webapp/petstore");
            HTTPConduit conduit = WebClient.getConfig(wc).getHttpConduit();
            conduit.getClient().setReceiveTimeout(1000000);
            conduit.getClient().setConnectionTimeout(1000000);
            XMLSource source = wc.query("_code", "").query("_os", getOs()).get(XMLSource.class);
            String link = source.getValue("ns:html/ns:body/ns:ul/ns:a/@href", 
                                          Collections.singletonMap("ns", "http://www.w3.org/1999/xhtml"));
            WebClient wc2 = WebClient.create(link);
            HTTPConduit conduit2 = WebClient.getConfig(wc2).getHttpConduit();
            conduit2.getClient().setReceiveTimeout(1000000);
            conduit2.getClient().setConnectionTimeout(1000000);
            InputStream is = wc2.accept("application/zip").get(InputStream.class);
            String tmpdir = System.getProperty("java.io.tmpdir");
            File classes = new File(tmpdir, "cxf-jaxrs-test-compiled-src");
            if (!classes.mkdir()) {
                fail();
            }
            File unzippedSrc = new File(tmpdir, "cxf-jaxrs-test-unzipped-src");
            if (!unzippedSrc.mkdir()) {
                fail();
            }
            try {             
                compileSrc(classes, unzippedSrc, is);
                verifyClasses(classes);
            } finally {
                FileUtils.removeDir(classes);
                FileUtils.removeDir(unzippedSrc);
            }
        } finally {
            ClassResourceInfo cri = 
                ResourceUtils.createClassResourceInfo(PetStore.class, PetStore.class, true, true);
            new CodeGeneratorProvider().removeCode(cri);
        }
    }
 
    private void verifyClasses(File classesDir) {
        List<File> clsFiles = FileUtils.getFilesRecurse(classesDir, ".+\\.class$");
        assertEquals(1, clsFiles.size());
        assertTrue(checkContains(clsFiles, "org.apache.cxf.systest.jaxrs.PetStore.class"));
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
    
    private void checkBookStoreInfo(Element resource) {
        assertEquals("/bookstore", resource.getAttribute("path"));
    }
    
    private void checkPetStoreInfo(Element resource) {
        assertEquals("/petstore/", resource.getAttribute("path"));
    }
    
    private List<Element> getWadlResourcesInfo(String baseURI, String requestURI, int size) throws Exception {
        WebClient client = WebClient.create(requestURI + "?_wadl&_type=xml");
        Document doc = DOMUtils.readXml(new InputStreamReader(client.get(InputStream.class), "UTF-8"));
        Element root = doc.getDocumentElement();
        assertEquals(WadlGenerator.WADL_NS, root.getNamespaceURI());
        assertEquals("application", root.getLocalName());
        List<Element> resourcesEls = DOMUtils.getChildrenWithName(root, 
                                                                  WadlGenerator.WADL_NS, "resources");
        assertEquals(1, resourcesEls.size());
        Element resourcesEl =  resourcesEls.get(0);
        assertEquals(baseURI, resourcesEl.getAttribute("base"));
        List<Element> resourceEls = 
            DOMUtils.getChildrenWithName(resourcesEl, 
                                         WadlGenerator.WADL_NS, "resource");
        assertEquals(size, resourceEls.size());
        return resourceEls;
    }
    
    @Test
    public void testGetBook123() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/webapp/bookstore/books/123"; 
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/json");
        connect.addRequestProperty("Content-Language", "badgerFishLanguage");
        InputStream in = connect.getInputStream();
        assertNotNull(in);           

        //Ensure BadgerFish output as this should have replaced the standard JSONProvider
        InputStream expected = getClass()
            .getResourceAsStream("resources/expected_get_book123badgerfish.txt");

        assertEquals("BadgerFish output not correct", 
                     getStringFromInputStream(expected).trim(),
                     getStringFromInputStream(in).trim());
    }
    
    @Test
    public void testGetBookNotFound() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/webapp/bookstore/books/12345"; 
        URL url = new URL(endpointAddress);
        HttpURLConnection connect = (HttpURLConnection)url.openConnection();
        connect.addRequestProperty("Accept", "text/plain,application/xml");
        assertEquals(500, connect.getResponseCode());
        InputStream in = connect.getErrorStream();
        assertNotNull(in);           

        InputStream expected = getClass()
            .getResourceAsStream("resources/expected_get_book_notfound_mapped.txt");

        assertEquals("Exception is not mapped correctly", 
                     getStringFromInputStream(expected).trim(),
                     getStringFromInputStream(in).trim());
    }
    
    @Test
    public void testGetBookNotExistent() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/webapp/bookstore/nonexistent"; 
        URL url = new URL(endpointAddress);
        HttpURLConnection connect = (HttpURLConnection)url.openConnection();
        connect.addRequestProperty("Accept", "application/xml");
        assertEquals(405, connect.getResponseCode());
        InputStream in = connect.getErrorStream();
        assertNotNull(in);           

        assertEquals("Exception is not mapped correctly", 
                     "Nonexistent method",
                     getStringFromInputStream(in).trim());
    }
    
    @Test
    public void testPostPetStatus() throws Exception {
        
        String endpointAddress =
            "http://localhost:9080/webapp/petstore/pets";

        URL url = new URL(endpointAddress);   
        HttpURLConnection httpUrlConnection = (HttpURLConnection)url.openConnection();  
             
        httpUrlConnection.setUseCaches(false);   
        httpUrlConnection.setDefaultUseCaches(false);   
        httpUrlConnection.setDoOutput(true);   
        httpUrlConnection.setDoInput(true);   
        httpUrlConnection.setRequestMethod("POST");   
        httpUrlConnection.setRequestProperty("Accept",   "text/xml");   
        httpUrlConnection.setRequestProperty("Content-type",   "application/x-www-form-urlencoded");   
        httpUrlConnection.setRequestProperty("Connection",   "close");   

        OutputStream outputstream = httpUrlConnection.getOutputStream();
        File inputFile = new File(getClass().getResource("resources/singleValPostBody.txt").toURI());         
         
        byte[] tmp = new byte[4096];
        int i = 0;
        InputStream is = new FileInputStream(inputFile);
        try {
            while ((i = is.read(tmp)) >= 0) {
                outputstream.write(tmp, 0, i);
            }
        } finally {
            is.close();
        }

        outputstream.flush();

        int responseCode = httpUrlConnection.getResponseCode();   
        assertEquals(200, responseCode); 
        assertEquals("Wrong status returned", "open", getStringFromInputStream(httpUrlConnection
            .getInputStream()));  
        httpUrlConnection.disconnect();
    }
    
    @Test
    public void testPostPetStatus2() throws Exception {
        
        
        Socket s = new Socket("localhost", 9080);
        IOUtils.copyAndCloseInput(getClass().getResource("resources/formRequest.txt").openStream(), 
                                  s.getOutputStream());

        s.getOutputStream().flush();
        try {
            assertTrue("Wrong status returned", getStringFromInputStream(s.getInputStream())
                       .contains("open"));  
        } finally {
            s.close();
        }
    }
    
    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
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
    
    private String getOs() {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().contains("win")) {
            return "win";
        } else {
            return "unix";
        }
    }
}
