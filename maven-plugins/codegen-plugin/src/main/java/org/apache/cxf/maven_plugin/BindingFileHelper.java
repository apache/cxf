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
package org.apache.cxf.maven_plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public final class BindingFileHelper {
    static final String LOCATION_ATTR_NAME = "wsdlLocation";
    
    private BindingFileHelper() {
    }

    static Document readDocument(InputStream is) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(is);
        doc.getDocumentElement().normalize();
        return doc;
    }

    /**
     * Reads bindingFile from given stream, sets the attribute wsdlLocation on the top element bindings to the
     * given wsdlLocation and writes the resulting binding file to outBindingFile
     * 
     * @param bindingFileStream
     * @param wsdlLocation
     * @param outBindingFile
     * @throws Exception
     */
    public static boolean setWsdlLocationAndWrite(InputStream bindingFileStream, URI wsdlLocation,
                                               File outBindingFile) throws Exception {
        Document doc = readDocument(bindingFileStream);

        // Find and set wsdlLocation
        Element bindings = doc.getDocumentElement();
        String oldLocation = bindings.getAttribute(LOCATION_ATTR_NAME);
        if (oldLocation != null && !"".equals(oldLocation)) {
            return false;
        }
        bindings.setAttribute(LOCATION_ATTR_NAME, wsdlLocation.toURL().toExternalForm());

        // Save to outBindingFile
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);

        if (!outBindingFile.exists()) {
            outBindingFile.getParentFile().mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(outBindingFile);
        StreamResult result = new StreamResult(fos);
        transformer.transform(source, result);
        return true;
    }

    static void setWsdlLocationInBindingsIfNotSet(File baseDir, File outDir, WsdlOption o, Log log) 
        throws MojoExecutionException {
        try {
            String[] bindingFiles = o.getBindingFiles();
            for (int c = 0; c < bindingFiles.length; c++) {
                String bindingFilePath = bindingFiles[c];
                File bindingFile = new File(bindingFilePath);
                File outFile =  new File(outDir, "" + c + "-" + bindingFile.getName());
                URI wsdlLocation = o.getWsdlURI(baseDir.toURI());
                FileInputStream is = new FileInputStream(bindingFile);
                boolean wasSet = setWsdlLocationAndWrite(is, wsdlLocation, outFile);
                if (log != null) {
                    log.info("Checked binding file " + bindingFilePath + " " + wasSet);
                }
                if (wasSet) {
                    bindingFiles[c] = outFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error setting wsdlLocation in binding file", e);
        }
    }

}
