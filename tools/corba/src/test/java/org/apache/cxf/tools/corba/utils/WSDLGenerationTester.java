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
import java.io.Writer;
import java.nio.file.Files;

import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.CatalogWSDLLocator;

import org.junit.Assert;

public class WSDLGenerationTester {

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
            if ("location".equals(origAttrName.getLocalPart())
                || "schemaLocation".equals(origAttrName.getLocalPart())) {
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
            if ("location".equals(actualAttrName.getLocalPart())
                || "schemaLocation".equals(actualAttrName.getLocalPart())) {
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
        String ns;
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
        WSDLManager wm = BusFactory.getThreadDefaultBus().getExtension(WSDLManager.class);
        WSDLFactory factory
            = WSDLFactory.newInstance("org.apache.cxf.tools.corba.utils.TestWSDLCorbaFactoryImpl");
        WSDLReader reader = factory.newWSDLReader();
        reader.setFeature("javax.wsdl.importDocuments", false);
        reader.setExtensionRegistry(wm.getExtensionRegistry());
        final String url = defnFile.toString();
        CatalogWSDLLocator locator = new CatalogWSDLLocator(url, (Bus)null);

        Definition wsdlDefn = reader.readWSDL(locator);

        File bkFile = new File(targetDir, "bk_" + defnFile.getName());
        try (Writer writer = Files.newBufferedWriter(bkFile.toPath())) {
            factory.newWSDLWriter().writeWSDL(wsdlDefn, writer);
        }
        return bkFile;
    }

}
