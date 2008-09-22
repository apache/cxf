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

package org.apache.cxf.maven_plugin.xml2fastinfoset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.sun.xml.fastinfoset.sax.SAXDocumentSerializer;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Compile XML resources to FastInfoset XML resources.
 * 
 * @goal xml2fastinfoset
 * @phase process-resources
 */
public class XML2FastInfosetCompilerMojo extends AbstractMojo {
    private static final String[] EMPTY_STRING_ARRAY = {};
    private static final String[] DEFAULT_INCLUDES = {"**/*.xml"};

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The resource directories containing the XML files to be compiled.
     * 
     * @parameter expression="${project.resources}"
     * @required
     * @readonly
     */
    private List resources;

    /**
     * A list of inclusion filters.
     * 
     * @parameter
     */
    private Set includes = new HashSet();

    /**
     * A list of exclusion filters.
     * 
     * @parameter
     */
    private Set excludes = new HashSet();

    /**
     * The directory for the results.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException {

        for (Iterator it = resources.iterator(); it.hasNext();) {
            Resource resource = (Resource)it.next();
            String targetPath = resource.getTargetPath();

            File resourceDirectory = new File(resource.getDirectory());
            if (!resourceDirectory.isAbsolute()) {
                resourceDirectory = new File(project.getBasedir(), resourceDirectory.getPath());
            }

            if (!resourceDirectory.exists()) {
                getLog().debug("Resource directory does not exist: " + resourceDirectory);
                continue;
            }


            DirectoryScanner scanner = new DirectoryScanner();

            scanner.setBasedir(resourceDirectory);
            if (includes != null && !includes.isEmpty()) {
                scanner.setIncludes((String[])includes.toArray(EMPTY_STRING_ARRAY));
            } else {
                scanner.setIncludes(DEFAULT_INCLUDES);
            }

            if (excludes != null && !excludes.isEmpty()) {
                scanner.setExcludes((String[])excludes.toArray(EMPTY_STRING_ARRAY));
            }

            scanner.addDefaultExcludes();
            scanner.scan();

            List includedFiles = Arrays.asList(scanner.getIncludedFiles());

            getLog().debug("FastInfosetting " + includedFiles.size() + " resource"
                              + (includedFiles.size() > 1 ? "s" : "")
                              + (targetPath == null ? "" : " to " + targetPath));

            if (includedFiles.size() > 0) {
                // this part is required in case the user specified "../something"
                // as destination
                // see MNG-1345
                if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                    throw new MojoExecutionException("Cannot create resource output directory: "
                                                     + outputDirectory);
                }
            }
            
            for (Iterator j = includedFiles.iterator(); j.hasNext();) {
                String name = (String)j.next();

                String destination = name.replaceFirst("\\.xml$", ".fixml");

                if (targetPath != null) {
                    destination = targetPath + "/" + name;
                }
                
                File source = new File(resourceDirectory, name);

                File destinationFile = new File(outputDirectory, destination);

                if (!destinationFile.getParentFile().exists()) {
                    destinationFile.getParentFile().mkdirs();
                }

                try {
                    compileFile(source, destinationFile);
                } catch (Exception e) {
                    throw new MojoExecutionException("Error copying resource " + source, e);
                }
            }

        }
    }

    private void compileFile(File sourceFile, File destinationFile) throws ParserConfigurationException,
        SAXException, IOException {

        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(sourceFile);
            fos = new FileOutputStream(destinationFile);
            dehydrate(fis, fos);
            fis.close();
            fos.close();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
                // nothing.
            }
        }
    }

    private void dehydrate(InputStream input, OutputStream output) throws ParserConfigurationException,
        SAXException, IOException {
        // Create Fast Infoset SAX serializer
        SAXDocumentSerializer saxDocumentSerializer = new SAXDocumentSerializer();
        // Set the output stream
        saxDocumentSerializer.setOutputStream(output);

        // Instantiate JAXP SAX parser factory
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        /*
         * Set parser to be namespace aware Very important to do otherwise
         * invalid FI documents will be created by the SAXDocumentSerializer
         */
        saxParserFactory.setNamespaceAware(true);
        // Instantiate the JAXP SAX parser
        SAXParser saxParser = saxParserFactory.newSAXParser();
        // Set the lexical handler
        saxParser.setProperty("http://xml.org/sax/properties/lexical-handler", saxDocumentSerializer);
        // Parse the XML document and convert to a fast infoset document
        saxParser.parse(input, saxDocumentSerializer);
    }

}
