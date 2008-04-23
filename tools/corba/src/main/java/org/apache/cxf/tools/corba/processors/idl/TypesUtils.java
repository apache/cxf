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

import javax.xml.namespace.QName;

import antlr.collections.AST;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaType;

public final class TypesUtils {

    private TypesUtils() {
        //complete
    }
    
    /** Returns node corresponding to the name of the CORBA primitive type node.
     * 
     * @param node
     * @return
     */
    public static AST getCorbaTypeNameNode(AST node) {
        AST currentNode = node;
        if (currentNode.getType() == IDLTokenTypes.LITERAL_unsigned) {
            currentNode = currentNode.getNextSibling();
        }
        if (currentNode.getType() == IDLTokenTypes.LITERAL_long
            && (currentNode.getNextSibling() != null)
            && (currentNode.getNextSibling().getType() == IDLTokenTypes.LITERAL_long)) {
            currentNode = currentNode.getNextSibling();
        }
        return currentNode.getNextSibling();
    }
    
    public static boolean isValidIdentifier(String id) {
        boolean result = true;
        // From the CORBA IDL spec (section 3.2.3):
        //   An identifier is an arbitrarily long sequence of ASCII alphabetic, digit,
        //   and underscore ("_") characters. The first character must be an ASCII 
        //   alphabetic character. All characters are significant.
        //
        // See section 3.2.3.1 for escaped identifiers (that start with a "_")
        //
        if (!Character.isLetter(id.charAt(0))) {
            result = false;
        }
        if (id.charAt(0) == '_') {
            result = false;
        }
        int index = 1;
        while (result && index < id.length()) {
            char cur = id.charAt(index);
            if (!Character.isLetterOrDigit(cur)
                || cur == '_') {
                result = false;
            }
            index++;
        }
        return result;
    }
    
    public static Scope generateAnonymousScopedName(Scope scope, XmlSchema schema) {
        Scope scopedName = null;
        XmlSchemaType anonSchemaType = null;
        Integer id = 0;
        do {
            id++;
            StringBuffer name = new StringBuffer();
            name.append("_");
            name.append("Anon" + id.toString());
            name.append("_");
            name.append(scope.tail());
            scopedName = new Scope(scope.getParent(), name.toString());
            QName scopedQName = new QName(schema.getTargetNamespace(), scopedName.toString());
            anonSchemaType = schema.getTypeByName(scopedQName);
        } while (anonSchemaType != null);
        
        return scopedName;
    }

}
