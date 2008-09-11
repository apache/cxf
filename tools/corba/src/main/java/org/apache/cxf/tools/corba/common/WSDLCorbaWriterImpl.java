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

package org.apache.cxf.tools.corba.common;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.wsdl.Constants;
import com.ibm.wsdl.util.xml.DOM2Writer;
import com.ibm.wsdl.util.xml.DOMUtils;
import com.ibm.wsdl.xml.WSDLWriterImpl;

 /*
  * This class is extending the wsdl4j RI class to print out the 
  * extensibility elements at the top of a generated wsdl file.
  * 
  */
public class WSDLCorbaWriterImpl extends WSDLWriterImpl {

    public static final int DEFAULT_INDENT_LEVEL = 2;

    
    /**
     * Write the specified WSDL definition to the specified Writer.
     * 
     * @param wsdlDef the WSDL definition to be written.
     * @param sink the Writer to write the xml to.
     */
    public void writeWSDL(Definition wsdlDef, Writer sink) throws WSDLException {
        PrintWriter pw = new PrintWriter(sink);
        String javaEncoding = (sink instanceof OutputStreamWriter)
            ? ((OutputStreamWriter)sink).getEncoding() : null;

        String xmlEncoding = DOM2Writer.java2XMLEncoding(javaEncoding);

        if (xmlEncoding == null) {
            throw new WSDLException(WSDLException.CONFIGURATION_ERROR,
                                    "Unsupported Java encoding for writing " + "wsdl file: '" + javaEncoding
                                        + "'.");
        }

        pw.println(Constants.XML_DECL_START + xmlEncoding + Constants.XML_DECL_END);

        printDefinition(wsdlDef, pw);
    }
    
        
    protected void printDefinition(Definition def, PrintWriter pw) throws WSDLException {
        if (def == null) {
            return;
        }

        if (def.getPrefix(Constants.NS_URI_WSDL) == null) {
            String prefix = "wsdl";
            int subscript = 0;

            while (def.getNamespace(prefix) != null) {
                prefix = "wsdl" + subscript++;
            }

            def.addNamespace(prefix, Constants.NS_URI_WSDL);
        }

        String tagName = DOMUtils.getQualifiedValue(Constants.NS_URI_WSDL, Constants.ELEM_DEFINITIONS, def);

        pw.print('<' + tagName);

        QName name = def.getQName();
        String targetNamespace = def.getTargetNamespace();
        Map namespaces = def.getNamespaces();

        if (name != null) {
            DOMUtils.printAttribute(Constants.ATTR_NAME, name.getLocalPart(), pw);
        }

        DOMUtils.printAttribute(Constants.ATTR_TARGET_NAMESPACE, targetNamespace, pw);

        printExtensibilityAttributes(Definition.class, def, def, pw);

        printNamespaceDeclarations(namespaces, pw);

        pw.println('>');

        printDocumentation(def.getDocumentationElement(), def, pw);
        List extElements = def.getExtensibilityElements();
        printExtensibilityElements(Definition.class, extElements, def, pw);

        printImports(def.getImports(), def, pw);
        printTypes(def.getTypes(), def, pw);
        printMessages(def.getMessages(), def, pw);
        printPortTypes(def.getPortTypes(), def, pw);
        printBindings(def.getBindings(), def, pw);
        printServices(def.getServices(), def, pw);

        pw.println("</" + tagName + '>');

        pw.flush();        
    }
    
    

    public void printExtensibilityElements(Class class1,
                                           List list,
                                           Definition def,
                                           PrintWriter pw)
        throws WSDLException {
        if (list != null) {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                ExtensibilityElement extElement = (ExtensibilityElement) it.next();
                if (extElement instanceof Schema) {
                    Schema schemaElement = (Schema)extElement;
                    if (schemaElement.getElement() != null) {
                        printDOMElement(schemaElement.getElement(),
                                        pw,
                                        DEFAULT_INDENT_LEVEL + 2);
                    } else if (schemaElement.getImports() != null) {
                        printSchemaImports(extElement.getElementType(),
                                     schemaElement,
                                     pw,
                                     DEFAULT_INDENT_LEVEL + 2,
                                     def);                        
                    }
                    pw.println();
                } else {
                    super.printExtensibilityElements(class1, list, def, pw);
                }
            }
        }
    }

    private void printDOMElement(Element element, PrintWriter pw, int indentCount) {
        indent(pw, indentCount);
        pw.print("<" + element.getNodeName());
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);
            //REVISIT, should we normalize the attribute value?
            pw.print(" " + attr.getName() + "=\"" + attr.getValue() + "\"");
        }
        pw.print(">");
        Node node = element.getFirstChild();
        pw.println();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                printDOMElement((Element) node, pw, indentCount + 2);
                pw.println();
            }
            node = node.getNextSibling();
        }
        indent(pw, indentCount);
        pw.print("</" + element.getNodeName() + ">");
    }
    
    private void printSchemaImports(QName schemaName, Schema schemaElement, PrintWriter pw, int indentCount,
                              Definition def) throws WSDLException {
        Map imports = schemaElement.getImports();
        indent(pw, indentCount);
        pw.print("<xsd:" + schemaName.getLocalPart() + ">");                
        String tagName = "xsd:import";
                
        Iterator importListIterator = imports.values().iterator();
        while (importListIterator.hasNext()) {
            List importList = (List)importListIterator.next();
            Iterator importIterator = importList.iterator();

            while (importIterator.hasNext()) {
                SchemaImport schemaImport = (SchemaImport)importIterator.next();
                pw.println();
                indent(pw, indentCount + 2);
                pw.print("<" + tagName);
                DOMUtils.printAttribute(Constants.ATTR_NAMESPACE, 
                                        schemaImport.getNamespaceURI(), pw);
                DOMUtils.printAttribute("schemaLocation", 
                                        schemaImport.getSchemaLocationURI(), pw);
                pw.println("/>");
            }
        }
        indent(pw, indentCount);
        pw.print("</xsd:" + schemaName.getLocalPart() + ">");

    }

    public void indent(PrintWriter pw, int count) {
        for (int i = 0; i < count; i++) {
            pw.print(' ');
        }
    }        
    
}
