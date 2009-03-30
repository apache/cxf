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

package org.apache.cxf.tools.validator.internal;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.util.URIParserUtil;
import org.apache.cxf.tools.validator.AbstractValidator;
import org.apache.cxf.wsdl.WSDLManager;

public class WSDL11Validator extends AbstractDefinitionValidator {
    protected static final Logger LOG = LogUtils.getL7dLogger(SchemaValidator.class);
    private final List<AbstractValidator> validators = new ArrayList<AbstractValidator>();

    public WSDL11Validator(final Definition definition) {
        this(definition, null);
    }

    public WSDL11Validator(final Definition definition, final ToolContext pe) {
        this(definition, pe, BusFactory.getDefaultBus());
    }

    public WSDL11Validator(final Definition definition, final ToolContext pe, final Bus b) {
        super(definition, pe, b);
    }

    private Document getWSDLDoc(String wsdl) {
        LOG.log(Level.FINE, new Message("VALIDATE_WSDL", LOG, wsdl).toString());
        try {
            OASISCatalogManager catalogResolver = OASISCatalogManager.getCatalogManager(this.getBus());

            String nw = catalogResolver.resolveSystem(wsdl);
            if (nw == null) {
                nw = catalogResolver.resolveURI(wsdl);
            }
            if (nw == null) {
                nw = catalogResolver.resolvePublic(wsdl, null);
            }
            if (nw == null) {
                nw = wsdl;
            }
            return new Stax2DOM().getDocument(URIParserUtil.getAbsoluteURI(nw));
        } catch (FileNotFoundException fe) {
            LOG.log(Level.WARNING, "Cannot find the wsdl " + wsdl + "to validate");
            return null;
        } catch (Exception e) {
            throw new ToolException(e);
        }
    }
    
    public boolean isValid() throws ToolException {
        //boolean isValid = true;
        String schemaDir = getSchemaDir();
        SchemaValidator schemaValidator = null;
        String[] schemas = (String[])env.get(ToolConstants.CFG_SCHEMA_URL);
        // Tool will use the following sequence to find the schema files
        // 1.ToolConstants.CFG_SCHEMA_DIR from ToolContext
        // 2.ToolConstants.CXF_SCHEMA_DIR from System property
        // 3.If 1 and 2 is null , then load these schema files from jar file
        String wsdl = (String)env.get(ToolConstants.CFG_WSDLURL);

        Document doc = getWSDLDoc(wsdl);
        if (doc == null) {
            return true;
        }
        if (this.def == null) {
            try {
                this.def = getBus().getExtension(WSDLManager.class).getDefinition(wsdl);
            } catch (WSDLException e) {
                throw new ToolException(e);
            }
        }
        
        WSDLRefValidator wsdlRefValidator = new WSDLRefValidator(this.def, doc, getBus());
        wsdlRefValidator.setSuppressWarnings(env.optionSet(ToolConstants.CFG_SUPPRESS_WARNINGS));        
        validators.add(wsdlRefValidator);
        
        if (env.optionSet(ToolConstants.CFG_VALIDATE_WSDL)) {
            validators.add(new UniqueBodyPartsValidator(this.def));
            validators.add(new WSIBPValidator(this.def));
            validators.add(new MIMEBindingValidator(this.def));
        }

        boolean notValid = false;
        for (AbstractValidator validator : validators) {
            if (!validator.isValid()) {
                notValid = true;
                addErrorMessage(validator.getErrorMessage());                
            }
        }
        if (notValid) {
            throw new ToolException(this.getErrorMessage());            
        }

        // By default just use WsdlRefValidator
        if (!env.optionSet(ToolConstants.CFG_VALIDATE_WSDL)) {
            return true;
        }

        if (!StringUtils.isEmpty(schemaDir)) {
            schemaValidator = new SchemaValidator(schemaDir, wsdl, schemas);
        } else {
            try {
                schemaValidator = new SchemaValidator(getDefaultSchemas(), wsdl, schemas);
            } catch (IOException e) {
                throw new ToolException("Schemas can not be loaded before validating wsdl", e);
            }

        }
        if (!schemaValidator.isValid()) {
            this.addErrorMessage(schemaValidator.getErrorMessage());            
            throw new ToolException(this.getErrorMessage());

        }
        
        return true;
    }

    public String getSchemaDir() {
        String dir = "";
        if (env.get(ToolConstants.CFG_SCHEMA_DIR) == null) {
            dir = System.getProperty(ToolConstants.CXF_SCHEMA_DIR);
        } else {
            dir = (String)env.get(ToolConstants.CFG_SCHEMA_DIR);
        }
        return dir;
    }

    protected List<InputSource> getDefaultSchemas() throws IOException {
        List<InputSource> xsdList = new ArrayList<InputSource>();
        ClassLoader clzLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> urls = clzLoader.getResources(ToolConstants.CXF_SCHEMAS_DIR_INJAR);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            //from jar files 
            if (url.toString().startsWith("jar")) {
                
                JarURLConnection jarConnection = (JarURLConnection)url.openConnection();
                
                JarFile jarFile = jarConnection.getJarFile();
                
                Enumeration<JarEntry> entry = jarFile.entries();
                
                while (entry.hasMoreElements()) {
                    JarEntry ele = (JarEntry)entry.nextElement();
                    if (ele.getName().endsWith(".xsd")
                        && ele.getName().indexOf(ToolConstants.CXF_SCHEMAS_DIR_INJAR) > -1) {
                        
                        URIResolver resolver = new URIResolver(ele.getName());
                        if (resolver.isResolved()) {
                            InputSource is = new InputSource(resolver.getInputStream());
                            // Use the resolved URI of the schema if available.
                            // The ibm jdk won't resolve the schema if we set
                            // the id to the relative path.
                            if (resolver.getURI() != null) {
                                is.setSystemId(resolver.getURI().toString());
                            } else {
                                is.setSystemId(ele.getName());
                            }
                            xsdList.add(is);
                        }
                    }
                }
                //from class path direcotry
            } else if (url.toString().startsWith("file")) {
                URI loc = null;
                try {
                    loc = url.toURI();
                } catch (URISyntaxException e) {
                    //
                }
                java.io.File file = new java.io.File(loc);
                if (file.exists()) {
                    File[] files = file.listFiles(new FileFilter() {
                            public boolean accept(File pathname) {
                                if (pathname.getAbsolutePath().endsWith(".xsd")) {
                                    return true;
                                }
                                return false;
                            }
                        });
                    for (int i = 0; i < files.length; i++) {
                        InputSource is = new InputSource(files[i].toURI().toURL().openStream());
                        is.setSystemId(files[i].toURI().toURL().toString());
                        xsdList.add(is);
                    }
                }
                
            }
        }
        
        sort(xsdList);
        return xsdList;
    }

    private void sort(List<InputSource> list) {
        Collections.sort(list, new Comparator<InputSource>() {
            public int compare(InputSource i1, InputSource i2) {
                if (i1 == null && i2 == null) {
                    return -1;
                }
                return i1.getSystemId().compareTo(i2.getSystemId());
            }
        });
    }
}
