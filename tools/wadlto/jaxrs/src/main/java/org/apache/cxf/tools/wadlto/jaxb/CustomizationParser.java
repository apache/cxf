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
package org.apache.cxf.tools.wadlto.jaxb;

import java.io.File;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Element;

import org.xml.sax.InputSource;

import org.apache.cxf.Bus;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.catalog.OASISCatalogManagerHelper;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.URIParserUtil;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.util.JAXBUtils;
import org.apache.cxf.tools.wadlto.WadlToolConstants;

public final class CustomizationParser {
    private static final Logger LOG = LogUtils.getL7dLogger(CustomizationParser.class);
    private final List<InputSource> jaxbBindings = new ArrayList<>();
    private final List<InputSource> packageFiles = new ArrayList<>();
    private final List<String> compilerArgs = new ArrayList<>();

    private Bus bus;
    private String wadlPath;

    public CustomizationParser(ToolContext env) {
        bus = env.get(Bus.class);
        wadlPath = (String)env.get(WadlToolConstants.CFG_WADLURL);
    }

    public void parse(ToolContext env) {
        // JAXB schema customizations
        for (String bindingFile : getBindingFiles(env)) {
            try {
                addBinding(bindingFile);
            } catch (XMLStreamException xse) {
                Message msg = new Message("STAX_PARSER_ERROR", LOG);
                throw new ToolException(msg, xse);
            }
        }

        if (env.get(WadlToolConstants.CFG_NO_ADDRESS_BINDING) == null) {
            //hard code to enable jaxb extensions
            compilerArgs.add("-extension");
            String name = "/org/apache/cxf/tools/common/jaxb/W3CEPRJaxbBinding.xml";
            if (org.apache.cxf.common.jaxb.JAXBUtils.isJAXB22()) {
                name = "/org/apache/cxf/tools/common/jaxb/W3CEPRJaxbBinding_jaxb22.xml";
            }
            URL bindingFileUrl = getClass().getResource(name);
            InputSource ins = new InputSource(bindingFileUrl.toString());
            jaxbBindings.add(ins);
        }

        //pass additional JAXB compiler arguments
        Object jaxbCompilerArgs = env.get(WadlToolConstants.CFG_XJC_ARGS);
        if (jaxbCompilerArgs != null) {

            String[] jaxbArgs = jaxbCompilerArgs instanceof String
                    ? new String[]{(String)jaxbCompilerArgs} : (String[])jaxbCompilerArgs;
            List<String> jaxbArgsList = new LinkedList<>();
            for (String jaxbArg : jaxbArgs) {
                String[] allArgs = jaxbArg.split(" ");
                for (String arg : allArgs) {
                    String s = arg.trim();
                    if (!StringUtils.isEmpty(s)) {
                        jaxbArgsList.add(s);
                    }
                }
            }
            compilerArgs.addAll(jaxbArgsList);
        }

        // Schema Namespace to Package customizations
        for (String ns : env.getNamespacePackageMap().keySet()) {
            packageFiles.add(JAXBUtils.getPackageMappingSchemaBinding(ns, env.mapPackageName(ns)));
        }
    }

    private void addBinding(String bindingFile) throws XMLStreamException {

        final Element root;
        try (URIResolver resolver = new URIResolver(bindingFile)) {
            root = StaxUtils.read(resolver.getInputStream()).getDocumentElement();
        } catch (Exception e1) {
            Message msg = new Message("CAN_NOT_READ_AS_ELEMENT", LOG, new Object[] {bindingFile});
            throw new ToolException(msg, e1);
        }
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(root);
        StaxUtils.toNextTag(reader);
        if (isValidJaxbBindingFile(reader)) {
            String schemaLocation = root.getAttribute("schemaLocation");
            String resolvedSchemaLocation = resolveByCatalog(schemaLocation);
            if (resolvedSchemaLocation == null) {
                resolvedSchemaLocation = schemaLocation.isEmpty()
                    ? wadlPath : getBaseWadlPath() + schemaLocation;
            }
            try {
                jaxbBindings.add(convertToTmpInputSource(root, resolvedSchemaLocation));
            } catch (Exception e1) {
                Message msg = new Message("FAILED_TO_ADD_SCHEMALOCATION", LOG, bindingFile);
                throw new ToolException(msg, e1);
            }
        }
    }

    private String getBaseWadlPath() {
        int lastSep = wadlPath.lastIndexOf('/');
        return lastSep != -1 ? wadlPath.substring(0, lastSep + 1) : wadlPath;
    }

    private static InputSource convertToTmpInputSource(Element ele, String schemaLoc) throws Exception {
        ele.setAttribute("schemaLocation", schemaLoc);
        File tmpFile = FileUtils.createTempFile("jaxbbinding", ".xml");
        try (Writer w = Files.newBufferedWriter(tmpFile.toPath())) {
            StaxUtils.writeTo(ele, w);
        }
        tmpFile.deleteOnExit();
        return new InputSource(URIParserUtil.getAbsoluteURI(tmpFile.getAbsolutePath()));
    }

    private String resolveByCatalog(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        OASISCatalogManager catalogResolver = OASISCatalogManager.getCatalogManager(bus);

        try {
            return new OASISCatalogManagerHelper().resolve(catalogResolver,
                                                           url, null);
        } catch (Exception e1) {
            Message msg = new Message("FAILED_RESOLVE_CATALOG", LOG, url);
            throw new ToolException(msg, e1);
        }
    }

    private boolean isValidJaxbBindingFile(XMLStreamReader reader) {
        return ToolConstants.JAXB_BINDINGS.equals(reader.getName());
    }

    private String[] getBindingFiles(ToolContext env) {
        Object files = env.get(WadlToolConstants.CFG_BINDING);
        if (files != null) {
            return files instanceof String ? new String[]{(String)files}
                                                   : (String[])files;
        }
        return new String[] {};
    }

    public List<InputSource> getJaxbBindings() {
        return this.jaxbBindings;
    }

    public List<InputSource> getSchemaPackageFiles() {
        return this.packageFiles;
    }

    public List<String> getCompilerArgs() {
        return this.compilerArgs;
    }
}
