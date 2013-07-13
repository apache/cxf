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

package org.apache.cxf.tools.corba.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.corba.wsdl.BindingType;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.cxf.wsdl.JAXBExtensionHelper;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;

import org.junit.Assert;

public class WSDLGenerationTester {

    private XmlSchemaCollection schemaCol = new XmlSchemaCollection();

    public WSDLGenerationTester() {
    }

    public void compare(XMLStreamReader orig, XMLStreamReader actual)
        throws Exception {

        boolean origEnd = false;
        boolean actualEnd = false;
        QName elName = null;
        while (orig.hasNext() || actual.hasNext()) {
            int origTag = orig.next();
            while (!orig.isStartElement() && !orig.isEndElement() && !orig.isCharacters()) {
                if (orig.hasNext()) {
                    origTag = orig.next();
                } else {
                    origEnd = true;
                    break;
                }
            }
            int actualTag = actual.next();
            while (!actual.isStartElement() && !actual.isEndElement() && !actual.isCharacters()) {
                if (actual.hasNext()) {
                    actualTag = actual.next();
                } else {
                    actualEnd = true;
                    break;
                }
            }
            if (!origEnd && !actualEnd) {
                Assert.assertEquals("XML mismatch", origTag, actualTag);
                if (orig.isStartElement()) {
                    elName = orig.getName();
                    compareStartElement(orig, actual);
                } else if (orig.isEndElement()) {
                    compareEndElement(orig, actual);
                } else if (orig.isCharacters()) {
                    compareCharacters(elName, orig, actual);
                }
            } else {
                break;
            }
        }
    }

    private void compareStartElement(XMLStreamReader orig, XMLStreamReader actual)
        throws Exception {
        Assert.assertEquals("Start element is not matched", orig.getName(), actual.getName());
        int origAttrCount = orig.getAttributeCount();
        int actualAttrCount = actual.getAttributeCount();
        for (int i = 0; i < origAttrCount; i++) {
            QName origAttrName = orig.getAttributeName(i);
            if ((origAttrName.getLocalPart().equals("location"))
                || (origAttrName.getLocalPart().equals("schemaLocation"))) {
                //skip this atribute
                origAttrCount--;
            } else {
                String s1 = orig.getAttributeValue(origAttrName.getNamespaceURI(),
                                                   origAttrName.getLocalPart());
                String s2 = actual.getAttributeValue(origAttrName.getNamespaceURI(),
                                                     origAttrName.getLocalPart());
                
                if (!s1.equals(s2)
                    && (s1.contains(":") || s2.contains(":"))) {
                    s1 = mapToQName(orig, s1);
                    s2 = mapToQName(actual, s2);
                }
                
                Assert.assertEquals("Attribute " + origAttrName + " not found or value not matching",
                                    s1, s2);
            }
        }
        for (int i = 0; i < actualAttrCount; i++) {
            QName actualAttrName = actual.getAttributeName(i);
            if ((actualAttrName.getLocalPart().equals("location"))
                || (actualAttrName.getLocalPart().equals("schemaLocation"))) {
                //skip this atribute
                actualAttrCount--;
            }
        }
        Assert.assertEquals("Attribute count is not matched for element " + orig.getName(),
                            origAttrCount,
                            actualAttrCount);
    }

    private String mapToQName(XMLStreamReader reader, String s2) {
        int idx = s2.indexOf(':');
        String ns = null;
        if (idx == -1) {
            ns = reader.getNamespaceURI("");
        } else {
            ns = reader.getNamespaceURI(s2.substring(0, idx));
            if (ns == null) {
                ns = reader.getNamespaceURI("");
            } else {
                s2 = s2.substring(idx + 1);
            }
        }
        return new QName(ns, s2).toString();
    }

    private void compareEndElement(XMLStreamReader orig, XMLStreamReader actual)
        throws Exception {
        Assert.assertEquals("End element is not matched", orig.getName(), actual.getName());
    }

    private void compareCharacters(QName elName, XMLStreamReader orig, XMLStreamReader actual)
        throws Exception {
        Assert.assertEquals("Element Characters not matched " + elName,
                            orig.getText().trim(), actual.getText().trim());
    }

    public File writeDefinition(File targetDir, File defnFile) throws Exception {
        File bkFile = new File(targetDir, "bk_" + defnFile.getName());
        FileWriter writer = new FileWriter(bkFile);
        WSDLFactory factory
            = WSDLFactory.newInstance("org.apache.cxf.tools.corba.utils.TestWSDLCorbaFactoryImpl");
        WSDLReader reader = factory.newWSDLReader();
        reader.setFeature("javax.wsdl.importDocuments", false);
        ExtensionRegistry extReg = new ExtensionRegistry();
        addExtensions(extReg);
        reader.setExtensionRegistry(extReg);
        Definition wsdlDefn = reader.readWSDL(defnFile.toString());
        WSDLWriter wsdlWriter = factory.newWSDLWriter();
        wsdlWriter.writeWSDL(wsdlDefn, writer);
        writer.close();
        writer = null;
        reader = null;
        return bkFile;
    }

    public File writeSchema(File targetDir, File schemaFile) throws Exception {
        File bkFile = new File(targetDir, "bk_" + schemaFile.getName());
        FileWriter writer = new FileWriter(bkFile);
        FileReader reader = new FileReader(schemaFile);
        XmlSchema schema = schemaCol.read(reader);
        schema.write(writer);
        reader.close();
        writer.close();
        writer = null;
        reader = null;
        return bkFile;
    }

    private void addExtensions(ExtensionRegistry extReg) throws Exception {
        JAXBExtensionHelper.addExtensions(extReg, Binding.class, BindingType.class);
        JAXBExtensionHelper.addExtensions(extReg, BindingOperation.class,
                                          org.apache.cxf.binding.corba.wsdl.OperationType.class);
        JAXBExtensionHelper.addExtensions(extReg, Definition.class, TypeMappingType.class);
        JAXBExtensionHelper.addExtensions(extReg, Port.class,
                                          org.apache.cxf.binding.corba.wsdl.AddressType.class);

        extReg.mapExtensionTypes(Binding.class, CorbaConstants.NE_CORBA_BINDING, BindingType.class);
        extReg.mapExtensionTypes(BindingOperation.class, CorbaConstants.NE_CORBA_OPERATION,
                                 org.apache.cxf.binding.corba.wsdl.OperationType.class);
        extReg.mapExtensionTypes(Definition.class, CorbaConstants.NE_CORBA_TYPEMAPPING,
                                 TypeMappingType.class);
        extReg.mapExtensionTypes(Port.class, CorbaConstants.NE_CORBA_ADDRESS,
                                 org.apache.cxf.binding.corba.wsdl.AddressType.class);
    }

}
