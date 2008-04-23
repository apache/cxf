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

import org.apache.cxf.binding.corba.wsdl.Anonarray;
import org.apache.cxf.binding.corba.wsdl.Array;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.tools.corba.common.ReferenceConstants;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaType;

public class ArrayDeferredAction implements SchemaDeferredAction {

    protected Array array;
    protected Anonarray anonarray;
    protected XmlSchemaElement element;
    
    
    public ArrayDeferredAction(Array arrayType,
                               Anonarray anonArrayType,
                               XmlSchemaElement elem) {                           
        array = arrayType;
        anonarray = anonArrayType;
        element = elem;        
    }
    
    public ArrayDeferredAction(Array arrayType) {
        array = arrayType;         
    }
    
    public ArrayDeferredAction(Anonarray anonarrayType) {
        anonarray = anonarrayType;         
    }
    
    public ArrayDeferredAction(XmlSchemaElement elem) {
        element = elem;               
    }
    
    public void execute(XmlSchemaType stype, CorbaTypeImpl ctype) {
        if (array != null) {
            array.setElemtype(ctype.getQName());
        }
        if (anonarray != null) {
            anonarray.setElemtype(ctype.getQName());
        }
        if (element != null) {
            element.setSchemaTypeName(stype.getQName());
            if (stype.getQName().equals(ReferenceConstants.WSADDRESSING_TYPE)) {
                element.setNillable(true);
            }
        }        
    }
        
}




