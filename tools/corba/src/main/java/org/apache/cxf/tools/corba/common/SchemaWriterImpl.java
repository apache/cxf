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

import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.wsdl.Constants;
import com.ibm.wsdl.util.xml.DOM2Writer;
import com.ibm.wsdl.xml.WSDLWriterImpl;


 /*
  * This class is extending the wsdl4j RI class to print out the 
  * extensibility elements of the schema into a separate file.
  * 
  */
public class SchemaWriterImpl extends WSDLWriterImpl {

    public static final int DEFAULT_INDENT_LEVEL = 0;

    
    /**
     * Write the specified schema of the WSDL definition 
     * to the specified Writer.
     * 
     * @param wsdlDef contains the schema to be written.
     * @param sink the Writer to write the xml to.
     */
    public void writeWSDL(Definition wsdlDef, Writer sink) throws WSDLException {
        PrintWriter pw = new PrintWriter(sink);
        String javaEncoding = (sink instanceof OutputStreamWriter)
            ? ((OutputStreamWriter)sink).getEncoding() : null;

        String xmlEncoding = DOM2Writer.java2XMLEncoding(javaEncoding);

        if (xmlEncoding == null) {
            throw new WSDLException(WSDLException.CONFIGURATION_ERROR,
                                    "Unsupported Java encoding for writing " + "schema file: '" + javaEncoding
                                        + "'.");
        }

        pw.println(Constants.XML_DECL_START + xmlEncoding + Constants.XML_DECL_END);

        printSchema(wsdlDef, pw);
    }
    
        
    protected void printSchema(Definition def, PrintWriter pw) throws WSDLException {
        if (def == null) {
            return;
        }
            
        Types types = def.getTypes();          
        if (types != null) {
            List extElements = types.getExtensibilityElements();
            printExtensibilityElements(Types.class, extElements, def, pw);
        }
    
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
                    printDOMElement(((Schema) extElement).getElement(),
                                    pw,
                                    DEFAULT_INDENT_LEVEL);
                    pw.println();
                } else {
                    super.printExtensibilityElements(class1, list, def, pw);
                }
            }
        }
    }

    private void printDOMElement(Element element, PrintWriter pw, int indentCount) {
        indent(pw, indentCount);        
        if (element.getLocalName().equals("schema")) {
            pw.print("<xs:" + element.getLocalName());
        } else {
            pw.print("<" + element.getNodeName());
        }
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr attr = (Attr) attrs.item(i);                  
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
        if (element.getLocalName().equals("schema")) {
            pw.print("</xs:" + element.getLocalName() + ">");
        } else {
            pw.print("</" + element.getNodeName() + ">");
        }
    }

    public void indent(PrintWriter pw, int count) {
        for (int i = 0; i < count; i++) {
            pw.print(' ');
        }
    }       
    
}
