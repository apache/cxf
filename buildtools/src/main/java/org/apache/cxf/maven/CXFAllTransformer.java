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
package org.apache.cxf.maven;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class CXFAllTransformer implements ResourceTransformer {
    
    byte buffer[] = new byte[1024];
    Map<String, ByteArrayOutputStream> extensions 
        = new LinkedHashMap<String, ByteArrayOutputStream>();
    String lastResource;

    public CXFAllTransformer() {
        super();
    }

    public boolean canTransformResource(String r) {
        if (r.startsWith("META-INF/cxf/cxf-extension-")
            && r.endsWith(".xml")) {
            lastResource = r;
            return true;
        }
        return false;
    }
    public boolean hasTransformedResource() {
        return !extensions.isEmpty();
    }

    public void processResource(String resource, InputStream is, List relocators) throws IOException {
        processResource(is);
    }
    public void processResource(InputStream is) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);
        int i = is.read(buffer);
        while (i != -1) {
            bout.write(buffer, 0, i);
            i = is.read(buffer);
        }
        extensions.put(lastResource, bout);
    }
    public void modifyOutputStream(JarOutputStream jos) throws IOException {
        List<String> imps = new ArrayList<String>(extensions.keySet());
        for (Map.Entry<String, ByteArrayOutputStream> ent : extensions.entrySet()) {
            jos.putNextEntry(new JarEntry(ent.getKey()));
            ent.getValue().writeTo(jos);
            try {
                Document r = new SAXBuilder()
                    .build(new ByteArrayInputStream(ent.getValue().toByteArray()));
                
                Element root = r.getRootElement();
                for (Iterator itr = root.getChildren().iterator(); itr.hasNext();) {
                    Content n = (Content)itr.next();
                    if (n instanceof Element) {
                        Element e = (Element)n;
                        if ("import".equals(e.getName())
                            && "http://www.springframework.org/schema/beans".equals(e.getNamespaceURI())) {
                            
                            //remove stuff that is imported from other extensions to
                            //keep them from being loaded twice. (it's not an issue
                            //to load them twice, there just is a performance 
                            //penalty in doing so
                            String loc = e.getAttributeValue("resource");
                            if (loc.startsWith("classpath:META-INF/cxf/cxf")) {
                                
                                loc = loc.substring(10);
                                imps.remove(loc);
                            }
                        }
                    }
                }
            } catch (JDOMException e) {
                throw new RuntimeException(e);
            }
        }
        
        if (imps.size() > 0) {
            jos.putNextEntry(new JarEntry("META-INF/cxf/cxf-all.xml"));
            Writer writer = new OutputStreamWriter(jos, "UTF-8");
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.append("<beans xmlns=\"http://www.springframework.org/schema/beans\"\n");
            writer.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            writer.append("    xsi:schemaLocation=\"");
            writer.append("http://www.springframework.org/schema/beans ");
            writer.append("http://www.springframework.org/schema/beans/spring-beans.xsd\">\n");
            writer.append("    <import resource=\"classpath:META-INF/cxf/cxf.xml\"/>\n");
            for (String res : imps) {
                writer.append("    <import resource=\"classpath:");
                writer.append(res);
                writer.append("\"/>\n");
            }
            writer.append("</beans>");
            writer.flush();
        }
    }
}

