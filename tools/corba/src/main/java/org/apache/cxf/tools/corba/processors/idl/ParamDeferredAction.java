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

package org.apache.cxf.tools.corba.processors.idl;

import java.util.Iterator;

import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.ParamType;
import org.apache.cxf.tools.corba.common.ReferenceConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaType;

public class ParamDeferredAction implements SchemaDeferredAction {

    protected ParamType param;
    protected XmlSchemaElement element;
    protected XmlSchema schema;
    protected XmlSchemaCollection schemas;
    protected Scope typeScope;
    protected WSDLSchemaManager manager;
    protected ModuleToNSMapper mapper;
    
    public ParamDeferredAction(ParamType defParam, XmlSchemaElement elem) {
        param = defParam;
        element = elem;        
    }
    
    public ParamDeferredAction(ParamType defParam) {
        param = defParam;         
    }
    
    public ParamDeferredAction(XmlSchemaElement elem) {
        element = elem;               
    }
    
    public ParamDeferredAction(XmlSchemaElement elem, 
                               Scope ts,
                               XmlSchema xmlSchema,
                               XmlSchemaCollection xmlSchemas,
                               WSDLSchemaManager man,
                               ModuleToNSMapper map) {
        element = elem;
        schema = xmlSchema;
        schemas = xmlSchemas;
        typeScope = ts;
        manager = man;
        mapper = map;
    }
    
    public void execute(XmlSchemaType stype, CorbaTypeImpl ctype) {
        if (param != null) {
            param.setIdltype(ctype.getQName());
        }
        if (element != null) {
            element.setSchemaTypeName(stype.getQName());
            if (stype.getQName().equals(ReferenceConstants.WSADDRESSING_TYPE)) {
                element.setNillable(true);
            }
     
            if (manager == null) {
                return;
            }
            
            // Now we need to make sure we are importing any types we need
            XmlSchema importedSchema = null;
            if (stype.getQName().getNamespaceURI().equals(ReferenceConstants.WSADDRESSING_NAMESPACE)) {
                boolean alreadyImported = false;
                for (Iterator i = schema.getIncludes().getIterator(); i.hasNext();) {
                    java.lang.Object o = i.next();
                    if (o instanceof XmlSchemaImport) {
                        XmlSchemaImport schemaImport = (XmlSchemaImport)o;
                        if (schemaImport.getNamespace().equals(ReferenceConstants.WSADDRESSING_NAMESPACE)) {
                            alreadyImported = true;
                            break;
                        }
                    }
                }
                    
                if (!alreadyImported) {
                    // We need to add an import statement to include the WS addressing types
                    XmlSchemaImport wsaImport = new XmlSchemaImport();
                    wsaImport.setNamespace(ReferenceConstants.WSADDRESSING_NAMESPACE);
                    wsaImport.setSchemaLocation(ReferenceConstants.WSADDRESSING_LOCATION);
                    schema.getItems().add(wsaImport);
                    schema.getIncludes().add(wsaImport);
                }
            } else if (!stype.getQName().getNamespaceURI().equals(schema.getTargetNamespace())) {
                importedSchema = manager.getXmlSchema(mapper.map(typeScope));
                manager.addXmlSchemaImport(schema, importedSchema, typeScope.toString("_"));
            }
        }        
    }
       
}




