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

package org.apache.cxf.tools.java2wsdl.generator.wsdl11;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;

import org.w3c.dom.Element;

import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.java2wsdl.generator.AbstractGenerator;
import org.apache.cxf.tools.util.FileWriterUtil;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.apache.cxf.wsdl11.WSDLDefinitionBuilder;

public class WSDL11Generator extends AbstractGenerator<Definition> {

    public Definition generate(final File dir) {
        File file = getOutputBase();
        if (file == null && dir != null) {
            if (dir.isDirectory()) {
                file = new File(dir, getServiceModel().getName().getLocalPart() + ".wsdl");
            } else {
                file = dir;
            }
        } else if (dir == null) {
            file = new File(getServiceModel().getName().getLocalPart() + ".wsdl");
        }

        File outputdir = createOutputDir(file);
        Definition def = null;
        try {
            Writer os = FileWriterUtil.getWriter(file);
            WSDLWriter wsdlWriter = WSDLFactory.newInstance().newWSDLWriter();

            ServiceWSDLBuilder builder = new ServiceWSDLBuilder(getBus(), getServiceModel());
            builder.setUseSchemaImports(this.allowImports());

            String name = file.getName();
            if (name.endsWith(".wsdl")) {
                name = name.substring(0, name.lastIndexOf(".wsdl"));
            }
            builder.setBaseFileName(name);
            Map<String, SchemaInfo> imports = new HashMap<String, SchemaInfo>();
            def = builder.build(imports);
            wsdlWriter.writeWSDL(def, os);
            os.close();

            if (def.getImports().size() > 0) {
                for (Import wsdlImport : WSDLDefinitionBuilder.getImports(def)) {
                    Definition wsdlDef = wsdlImport.getDefinition();
                    File wsdlFile = null;
                    if (!StringUtils.isEmpty(wsdlImport.getLocationURI())) {
                        wsdlFile = new File(outputdir,  wsdlImport.getLocationURI());
                    } else {
                        wsdlFile = new File(outputdir, wsdlDef.getQName().getLocalPart() + ".wsdl");
                    }
                    OutputStream wsdlOs = new BufferedOutputStream(new FileOutputStream(wsdlFile));
                    wsdlWriter.writeWSDL(wsdlDef, wsdlOs);
                    wsdlOs.close();
                }
            }

            for (Map.Entry<String, SchemaInfo> imp : imports.entrySet()) {
                File impfile = new File(file.getParentFile(), imp.getKey());
                Element el = imp.getValue().getElement();
                updateImports(el, imports);
                os = FileWriterUtil.getWriter(impfile);
                XMLUtils.writeTo(el, os, 2);
                os.close();
            }

            customizing(outputdir, name, imports.keySet());
        } catch (WSDLException wex) {
            wex.printStackTrace();
        } catch (FileNotFoundException fnfe) {
            throw new ToolException("Output file " + file + " not found", fnfe);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return def;
    }

    private void updateImports(Element el, Map<String, SchemaInfo> imports) {
        List<Element> imps = DOMUtils.getChildrenWithName(el,
                                                          WSDLConstants.NS_SCHEMA_XSD, 
                                                          "import");
        for (Element e : imps) {
            String ns = e.getAttribute("namespace");
            for (Map.Entry<String, SchemaInfo> ent : imports.entrySet()) {
                if (ent.getValue().getNamespaceURI().equals(ns)) {
                    e.setAttribute("schemaLocation", ent.getKey());
                }
            }
        }
    }

    private void customizing(final File outputdir,
                             final String wsdlName,
                             final Set<String> imports) {
        DateTypeCustomGenerator generator = new DateTypeCustomGenerator();

        generator.setWSDLName(wsdlName);
        generator.setServiceModel(getServiceModel());
        generator.setAllowImports(allowImports());
        generator.addSchemaFiles(imports);

        generator.generate(outputdir);
    }
}

