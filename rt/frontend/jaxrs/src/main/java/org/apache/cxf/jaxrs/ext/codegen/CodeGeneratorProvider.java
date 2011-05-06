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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class CodeGeneratorProvider implements RequestHandler {
    public static final String CODE_QUERY = "_code";
    public static final String LANGUAGE_QUERY = "_lang";
    public static final String OS_QUERY = "_os";
    public static final String SOURCE_QUERY = "_source";
    public static final String CODE_TYPE_QUERY = "_codeType";
    
    private static final Logger LOG = LogUtils.getL7dLogger(CodeGeneratorProvider.class);
    private static final Set<String> SUPPORTED_LANGUAGES = new HashSet<String>(
        Arrays.asList(new String[]{"java"}));
    
    private static final String TMPDIR = System.getProperty("java.io.tmpdir");
    
    
    private Comparator<String> importsComparator;
    private UriInfo ui;
    private boolean generateInterfaces = true;
    
    
    @Context
    public void setUriInfo(UriInfo uriInfo) {
        this.ui = uriInfo;
    }
    
    public Response handleRequest(Message m, ClassResourceInfo resourceClass) {
        
        if (!"GET".equals(m.get(Message.HTTP_REQUEST_METHOD))) {
            return null;
        }
        
        if (ui.getQueryParameters().containsKey(SOURCE_QUERY)) {
            synchronized (this) { 
                return getSource(new File(TMPDIR, getStem(resourceClass, "zip")));
            }
        }
        
        String codeQuery = ui.getQueryParameters().getFirst(CODE_QUERY);
        if (codeQuery == null) {
            return null;
        }
        
        String language = ui.getQueryParameters().getFirst(LANGUAGE_QUERY);
        if (language != null && !SUPPORTED_LANGUAGES.contains(language)) {
            return Response.noContent().entity("Unsupported language" + language).type("text/plain").build();
        }
        return doHandleRequest(m, resourceClass);
    }
    
    protected Response doHandleRequest(Message m, ClassResourceInfo resourceClass) { 
        synchronized (this) {
            File zipDir = new File(TMPDIR, getStem(resourceClass, "zip"));
            Response r = getLink(zipDir, m);
            if (r != null) {
                return r;
            }
            
            File srcDir = new File(TMPDIR, getStem(resourceClass, "src"));
            if (!srcDir.exists() && !srcDir.mkdir()) {
                throw new IllegalStateException("Unable to create working directory " + srcDir.getPath());
            }
            String codeType = ui.getQueryParameters().getFirst(CODE_TYPE_QUERY);
            try {
                String wadl = getWadl(m, resourceClass);
                if (wadl == null) {
                    LOG.warning("WADL for " 
                         + (resourceClass != null ? resourceClass.getServiceClass().getName() 
                             : "this service")
                         + " can not be loaded");
                    return Response.noContent().build();
                }
                
                Map<String, String> properties = getProperties();
                SourceGenerator sg = new SourceGenerator(properties);
                sg.setGenerateInterfaces(generateInterfaces);
                sg.setImportsComparator(importsComparator);
                sg.generateSource(wadl, srcDir, codeType);
                
                zipSource(srcDir, zipDir);
                return getLink(zipDir, m);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Code can not be generated for " 
                            + (resourceClass != null ? resourceClass.getServiceClass().getName() 
                                : "this service"), ex);
                FileUtils.removeDir(zipDir);
                return Response.noContent().build();
            } finally {
                FileUtils.removeDir(srcDir);
            }
        }
    }
    
    private Map<String, String> getProperties() {
        Map<String, String> map = new HashMap<String, String>();
        map.put(SourceGenerator.LINE_SEP_PROPERTY, getLineSep());
        map.put(SourceGenerator.FILE_SEP_PROPERTY, getFileSep());
        return map;
    }
    
    private void zipSource(File srcDir, File zipDir) throws Exception {
        if (!zipDir.exists()) {
            zipDir.mkdir();
        }
        File zipFile = new File(zipDir.getAbsolutePath(), "src.zip");
        zipFile.createNewFile();
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        List<File> srcFiles = FileUtils.getFilesRecurse(srcDir, ".+\\.java$");
        for (File f : srcFiles) {
            String entryName = f.getAbsolutePath().substring(srcDir.getAbsolutePath().length() + 1);
            zos.putNextEntry(new ZipEntry(entryName));
            IOUtils.copy(new FileInputStream(f), zos);
        }
        zos.close();
    }
    
    private String getLineSep() {
        String os = ui.getQueryParameters().getFirst(OS_QUERY);
        if (os == null) {
            return System.getProperty(SourceGenerator.LINE_SEP_PROPERTY);
        }
        return "unix".equals(os) ? "\r" : "\r\n";
    }
    
    private String getFileSep() {
        String os = ui.getQueryParameters().getFirst(OS_QUERY);
        if (os == null) {
            return System.getProperty(SourceGenerator.FILE_SEP_PROPERTY);
        }
        return "unix".equals(os) ? "/" : "\\";
    }
    
    private Response getSource(File zipDir) {
        if (zipDir.exists()) {
            File zipFile = new File(zipDir.getAbsolutePath(), "src.zip");
            if (zipFile.exists()) {
                try {
                    return Response.ok().type("application/zip").entity(new FileInputStream(zipFile)).build();
                } catch (FileNotFoundException ex) {
                    // should not happen given we've checked it exists
                    throw new WebApplicationException();
                }
            }
        } 
        return Response.noContent().build();
        
    }
    
    private Response getLink(File zipDir, Message m) {
        if (zipDir.exists() && new File(zipDir.getAbsolutePath(), "src.zip").exists()) {
            UriBuilder builder = ui.getAbsolutePathBuilder();
            String link = builder.queryParam(SOURCE_QUERY).build().toString();
            // TODO : move it into a resource template
            StringBuilder sb = new StringBuilder();
            sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
            sb.append("<head><title>Download the source</title></head>");
            sb.append("<body>");
            sb.append("<h1>Link:</h1><br/>");
            sb.append("<ul>" + "<a href=\"" + link + "\">" + link + "</a>" + "</ul>");
            sb.append("</body>");
            sb.append("</html>");
            m.getExchange().put(JAXRSUtils.IGNORE_MESSAGE_WRITERS, true);
            return Response.ok().type("application/xhtml+xml").entity(
                sb.toString()).build();
        }
        return null;
    }
    
    public void removeCode(ClassResourceInfo cri) {
        removeCode(new File(TMPDIR, getStem(cri, "src")));
        removeCode(new File(TMPDIR, getStem(cri, "zip")));
    }
    
    protected String getStem(ClassResourceInfo cri, String suffix) {
        if (cri == null) {
            return "cxf-jaxrs-" + suffix;
        } else {
            return "cxf-jaxrs-" + cri.getServiceClass().getName() + "-" + suffix; 
        }
    }
    
    private static void removeCode(File src) {
        if (src.exists()) {
            FileUtils.removeDir(src);
        }
    }
    
    protected String getWadl(Message m, ClassResourceInfo resourceClass) {
        m.put(Message.QUERY_STRING, WadlGenerator.WADL_QUERY);
        
        List<ProviderInfo<RequestHandler>> shs = ProviderFactory.getInstance(m).getRequestHandlers();
        // this is actually being tested by ProviderFactory unit tests but just in case
        // WadlGenerator, the custom or default one, must be the first one
        if (shs.size() > 0 && shs.get(0).getProvider() instanceof WadlGenerator) {
            WadlGenerator wg = (WadlGenerator)shs.get(0).getProvider();
            wg = new WadlGenerator(wg);
            wg.setAddResourceAndMethodIds(true);
            Response r = wg.handleRequest(m, resourceClass);
            return r == null ? null : (String)r.getEntity();
        }
        return null;
    }

    public void setImportsComparator(Comparator<String> importsComparator) {
        this.importsComparator = importsComparator;
    }

    public void setGenerateInterfaces(boolean generateInterfaces) {
        this.generateInterfaces = generateInterfaces;
    }
    
}
