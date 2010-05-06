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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class PluginTransformer implements ResourceTransformer {
    public static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    String resource;
    Document doc;

    public PluginTransformer() {
        super();
    }

    public boolean canTransformResource(String r) {
        r = r.toLowerCase();

        if (resource != null && resource.toLowerCase().equals(r)) {
            return true;
        }

        return false;
    }

    public void processResource(String resource, InputStream is, List relocators) throws IOException {
        processResource(is);
    }
    public void processResource(InputStream is) throws IOException {
        Document r;
        try {
            r = new SAXBuilder().build(is);
        } catch (JDOMException e) {
            throw new RuntimeException(e);
        }

        if (doc == null) {
            doc = r;
            
            Element el = doc.getRootElement();
            el.setAttribute("name", "default");
            el.setAttribute("provider", "cxf.apache.org");
        } else {
            Element root = r.getRootElement();

            for (Iterator itr = root.getChildren().iterator(); itr.hasNext();) {
                Content n = (Content)itr.next();
                itr.remove();

                doc.getRootElement().addContent(n);
            }
        }
    }

    public boolean hasTransformedResource() {
        return doc != null;
    }

    public void modifyOutputStream(JarOutputStream jos) throws IOException {
        jos.putNextEntry(new JarEntry(resource));

        new XMLOutputter(Format.getPrettyFormat()).output(doc, jos);
        doc = null;
    }

}
