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

import java.io.IOException;
import java.io.Writer;

import java.util.Iterator;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.xml.WSDLWriter;

import javax.xml.namespace.QName;

import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.tools.util.FileWriterUtil;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaForm;

public final class WSDLUtils {

    private WSDLUtils() {
        //complete
    }

    public static boolean isElementFormQualified(SchemaCollection schemas, QName type) {
        if (type != null) {   
            XmlSchema sch = schemas.getSchemaByTargetNamespace(type.getNamespaceURI());
            if (sch != null) {
                return sch.getElementFormDefault().getValue().equals(XmlSchemaForm.QUALIFIED);
            }
        }
        return false;
    }

    public static boolean isElementFormQualified(XmlSchema schema, QName type) {
        if (type != null) {     
            String uri = type.getNamespaceURI();
            if (uri.equals(schema.getTargetNamespace())) {
                return schema.getElementFormDefault().getValue().equals(XmlSchemaForm.QUALIFIED);
            }
            Iterator it = schema.getIncludes().getIterator();
            while (it.hasNext()) {
                XmlSchemaExternal extSchema = (XmlSchemaExternal) it.next();
                return isElementFormQualified(extSchema.getSchema(), type);
            }
        }
        return false;
    }

    public static void writeWSDL(Definition def, String outputdir, String wsdlOutput)
        throws WSDLException, IOException {     
        FileWriterUtil fw = new FileWriterUtil(outputdir);
        Writer outputWriter = fw.getWriter("", wsdlOutput);

        writeWSDL(def, outputWriter);   
    }

    public static void writeWSDL(Definition def, Writer outputWriter)
        throws WSDLException, IOException {     
        WSDLCorbaFactory wsdlfactory = WSDLCorbaFactory
            .newInstance("org.apache.cxf.tools.corba.common.WSDLCorbaFactoryImpl");
        WSDLWriter writer = wsdlfactory.newWSDLWriter();        
        writer.writeWSDL(def, outputWriter);
        
        outputWriter.flush();
        outputWriter.close();
    }

    
    public static void writeSchema(Definition def, Writer outputWriter) throws WSDLException, IOException {
        SchemaFactory sfactory = SchemaFactory
            .newInstance("org.apache.cxf.tools.corba.common.SchemaFactoryImpl");
        WSDLWriter swriter = sfactory.newWSDLWriter();
        swriter.writeWSDL(def, outputWriter);

        outputWriter.flush();
        outputWriter.close();
    }
}
