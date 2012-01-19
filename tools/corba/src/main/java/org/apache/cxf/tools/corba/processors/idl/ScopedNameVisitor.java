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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import antlr.collections.AST;

import org.apache.cxf.binding.corba.wsdl.Alias;
import org.apache.cxf.binding.corba.wsdl.Anonarray;
import org.apache.cxf.binding.corba.wsdl.Anonsequence;
import org.apache.cxf.binding.corba.wsdl.Array;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.Sequence;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.cxf.tools.corba.common.XmlSchemaPrimitiveMap;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;

public class ScopedNameVisitor extends VisitorBase {        
    private static boolean exceptionMode;
    private static XmlSchemaPrimitiveMap xmlSchemaPrimitiveMap = new XmlSchemaPrimitiveMap();          

    public ScopedNameVisitor(Scope scope,
                             Definition defn,
                             XmlSchema schemaRef,
                             WSDLASTVisitor wsdlVisitor) {
        super(scope, defn, schemaRef, wsdlVisitor);             
    }
    
    public void setExceptionMode(boolean value) {
        exceptionMode = value;
    }
    
    public static boolean accept(Scope scope,
                                 Definition defn,
                                 XmlSchema schemaRef,
                                 AST node,                                 
                                 WSDLASTVisitor wsdlVisitor) {
        boolean result = false;
        if (PrimitiveTypesVisitor.accept(node)) {
            result = true; 
        } else if (isforwardDeclared(scope, node, wsdlVisitor)) {
            result = true;          
        } else if (ObjectReferenceVisitor.accept(scope,
                                                 schemaRef,
                                                 defn,
                                                 node,
                                                 wsdlVisitor)) {
            result = true;
        } else if (findSchemaType(scope, defn, schemaRef, node, wsdlVisitor, null)) {
            result = true;
        }
        return result;
    }

    public void visit(AST node) {
        // <scoped_name> ::= <identifier>
        //                 | :: <identifier>
        //                 | <scoped_name> "::" <identifier>

        XmlSchemaType stype = null;
        CorbaTypeImpl ctype = null;        
        if (PrimitiveTypesVisitor.accept(node)) {
            // primitive type            
            PrimitiveTypesVisitor primitiveVisitor =
                new PrimitiveTypesVisitor(null, definition, schema, schemas);
            primitiveVisitor.visit(node);
            
            stype = primitiveVisitor.getSchemaType();
            ctype = primitiveVisitor.getCorbaType();            
        } else if (isforwardDeclared(getScope(), node, wsdlVisitor)) {
            // forward declaration
            Scope scope = forwardDeclared(getScope(),
                                          definition,
                                          schema,
                                          node,
                                          wsdlVisitor);
            setFullyQualifiedName(scope);
            // how will we create the corbatype ????
        } else if (ObjectReferenceVisitor.accept(getScope(), schema, definition, node, wsdlVisitor)) {
            ObjectReferenceVisitor objRefVisitor = new ObjectReferenceVisitor(getScope(),
                                                                              definition,
                                                                              schema,
                                                                              wsdlVisitor);
            objRefVisitor.visit(node);

            stype = objRefVisitor.getSchemaType();
            ctype = objRefVisitor.getCorbaType();           
        } else {
            VisitorTypeHolder holder = new VisitorTypeHolder();
            boolean found = findSchemaType(getScope(),
                                           definition,
                                           schema,
                                           node,
                                           wsdlVisitor,
                                           holder);
            if (found) {
                ctype = holder.getCorbaType();
                stype = holder.getSchemaType();
            } else {
                Scope scopedName = new Scope(getScope(), node);
                QName qname = new QName(schema.getTargetNamespace(), scopedName.toString());
                throw new RuntimeException("[ScopedNameVisitor:  Corba type "
                                           + qname
                                           + " not found in typeMap]");
            }
        }       
        
        setSchemaType(stype);
        setCorbaType(ctype);        
        
    }

    private static CorbaTypeImpl getCorbaSchemaType(XmlSchema xmlSchema,
                                                    TypeMappingType typeMap,
                                                    XmlSchemaType stype,
                                                    Scope scopedName) {       
        CorbaTypeImpl ctype = null;
        if (stype.getQName().equals(Constants.XSD_STRING)) {
            ctype = new CorbaTypeImpl();
            ctype.setName(CorbaConstants.NT_CORBA_STRING.getLocalPart());
            ctype.setQName(CorbaConstants.NT_CORBA_STRING);
            ctype.setType(Constants.XSD_STRING);
        } else {                    
            QName qname = stype.getQName();
            ctype = findCorbaTypeForSchemaType(typeMap, qname, scopedName);
        }
        return ctype;
    }
         
    protected static boolean isforwardDeclared(Scope scope, AST node, WSDLASTVisitor wsdlVisitor) {
        boolean isForward = false;
        Scope currentScope = scope;

        ScopeNameCollection scopedNames = wsdlVisitor.getScopedNames();

        // Check for forward declaration from local scope outwards
        if ((node.getFirstChild() == null)
            || (node.getFirstChild() != null && node.getFirstChild().getType() != IDLTokenTypes.SCOPEOP)) {
            while (!isForward && currentScope != currentScope.getParent()) {
                Scope scopedName = null;
                if (isFullyScopedName(node)) {
                    scopedName = getFullyScopedName(currentScope, node);
                } else {
                    scopedName = new Scope(currentScope, node);
                }
                if (scopedNames.getScope(scopedName) != null) {
                    isForward = true;
                }
                currentScope = currentScope.getParent();
            }
        }
        // Check for forward declaration in global scope
        if (!isForward) {
            Scope scopedName = null;
            if (isFullyScopedName(node)) {
                scopedName = getFullyScopedName(new Scope(), node);
            } else {                
                scopedName = new Scope(new Scope(), node);
            }
                        
            if (scopedNames.getScope(scopedName) != null) {
                isForward = true;
            }
        }

        return isForward;
    }
     
    
    protected static Scope forwardDeclared(Scope scope,
                                           Definition defn,
                                           XmlSchema schemaRef,
                                           AST node,
                                           WSDLASTVisitor wsdlVisitor) {
        //XmlSchemaType result = null;
        Scope result = null;
        Scope currentScope = scope;
        ScopeNameCollection scopedNames = wsdlVisitor.getScopedNames();

        // Check for forward declaration from local scope outwards
        if ((node.getFirstChild() == null)
            || (node.getFirstChild() != null && node.getFirstChild().getType() != IDLTokenTypes.SCOPEOP)) {
            while (result == null && currentScope != currentScope.getParent()) {
                Scope scopedName = null;
                if (isFullyScopedName(node)) {
                    scopedName = getFullyScopedName(currentScope, node);
                } else {
                    scopedName = new Scope(currentScope, node);
                }

                if (scopedNames.getScope(scopedName) != null) {
                    XmlSchema xmlSchema = schemaRef;
                    String tns = wsdlVisitor.getModuleToNSMapper().map(scopedName.getParent());
                    if (tns != null) {
                        xmlSchema = wsdlVisitor.getManager().getXmlSchema(tns);
                    }
                    if (ObjectReferenceVisitor.accept(scope, xmlSchema, defn, node, wsdlVisitor)) {
                        // checks if its a forward
                        Visitor visitor = new ObjectReferenceVisitor(scope, defn, xmlSchema, wsdlVisitor);
                        visitor.visit(node);                    
                    }
                    result = scopedName;
                }
                currentScope = currentScope.getParent();
            }
        }
        // Check for forward declaration in global scope
        if (result == null) {
            Scope scopedName = null;
            if (isFullyScopedName(node)) {
                scopedName = getFullyScopedName(new Scope(), node);
            } else {
                scopedName = new Scope(new Scope(), node);
            }
            if (scopedNames.getScope(scopedName) != null) {
                XmlSchema xmlSchema = schemaRef;
                String tns = wsdlVisitor.getModuleToNSMapper().map(scopedName.getParent());
                if (tns != null) {
                    xmlSchema = wsdlVisitor.getManager().getXmlSchema(tns);
                }
                if (ObjectReferenceVisitor.accept(scope, xmlSchema, defn, node, wsdlVisitor)) {
                    // checks if an object ref
                    Visitor visitor = new ObjectReferenceVisitor(scope, defn, xmlSchema, wsdlVisitor);
                    visitor.visit(node);
                }
                result = scopedName;
            }
        }
        return result;
    }
    
    
    protected static boolean findSchemaType(Scope scope,
                                            Definition defn,
                                            XmlSchema schemaRef,
                                            AST node,
                                            WSDLASTVisitor wsdlVisitor,
                                            VisitorTypeHolder holder) {
                                                
        boolean result = false;
        Scope currentScope = scope;        
        
        // checks from innermost local scope outwards
        if ((node.getFirstChild() == null)
            || (node.getFirstChild() != null && node.getFirstChild().getType() != IDLTokenTypes.SCOPEOP)) {
            while (!result && currentScope != currentScope.getParent()) {
                // A name can be used in an unqualified form within a particular
                // scope;
                // it will be resolved by successvely n searching farther out in
                // enclosing scopes, while taking into consideration 
                // inheritance relationships among interfaces.
                Scope scopedName = null;
                if (isFullyScopedName(node)) {
                    scopedName = getFullyScopedName(currentScope, node);
                } else {
                    scopedName = new Scope(currentScope, node);
                }
                result = findScopeSchemaType(scopedName, schemaRef, wsdlVisitor, holder);

                // Search inherited scopes for the type        
                if (!result) {
                    result = findSchemaTypeInInheritedScope(scope, defn, schemaRef,
                                                            node, wsdlVisitor, holder);
                    
                }
                currentScope = currentScope.getParent();
            }
        }
        
        
        
        if (!result) {
            // Global scope is our last chance to resolve the node
            result = findSchemaTypeInGlobalScope(scope,
                                                 defn,
                                                 schemaRef,
                                                 node,
                                                 wsdlVisitor,
                                                 holder);
        }
        return result;
    }
    
    private static boolean findSchemaTypeInGlobalScope(Scope scope,
                                                       Definition defn,
                                                       XmlSchema currentSchema,
                                                       AST node,
                                                       WSDLASTVisitor wsdlVisitor,
                                                       VisitorTypeHolder holder) {
        XmlSchemaCollection schemas = wsdlVisitor.getSchemas();
        TypeMappingType typeMap = wsdlVisitor.getTypeMap();
        ModuleToNSMapper mapper = wsdlVisitor.getModuleToNSMapper();
        WSDLSchemaManager manager = wsdlVisitor.getManager();
        
        Scope scopedName = scope;
        String name = node.toString();
        if (isFullyScopedName(node)) {
            scopedName = getFullyScopedName(new Scope(), node);
            name = scopedName.toString();
        }
        boolean result = findNonSchemaType(name, wsdlVisitor, holder);
        if (!result) {
            XmlSchema xmlSchema = currentSchema;
            QName qname = null;
            String tns = mapper.map(scopedName.getParent());
            if (tns != null) {
                xmlSchema = manager.getXmlSchema(tns);
                if (xmlSchema != null) {
                    qname = new QName(xmlSchema.getTargetNamespace(), scopedName.tail());
                }
            } else {
                qname = new QName(xmlSchema.getTargetNamespace(), name);
            }
            XmlSchemaType stype = null;
            if (qname != null) {
                // Exceptions are treated as a special case as above
                if (exceptionMode) {
                    qname = new QName(xmlSchema.getTargetNamespace(), qname.getLocalPart() + "Type");
                }
                stype = xmlSchema.getTypeByName(qname);
                if (stype == null) {
                    stype = schemas.getTypeByQName(qname);
                }
            }
            if (stype != null) {
                result = true;
                if (holder != null) {
                    holder.setSchemaType(stype);
                    holder.setCorbaType(getCorbaSchemaType(xmlSchema, typeMap, stype, scopedName));
                    //add a xmlschema import
                    if (!currentSchema.getTargetNamespace().equals(xmlSchema.getTargetNamespace())) {
                        String importFile = wsdlVisitor.getOutputDir()
                            + System.getProperty("file.separator")
                            + scopedName.getParent().toString("_");
                        manager.addXmlSchemaImport(currentSchema, xmlSchema, importFile);
                    }
                }
            }
        }
        return result;
    }

    
    // Searches all the inherited interfaces for the type.
    private static boolean findSchemaTypeInInheritedScope(Scope scope, Definition defn, XmlSchema schemaRef,
                                                          AST node, WSDLASTVisitor wsdlVisitor,
                                                          VisitorTypeHolder holder) {

        boolean result = false;                
        List<Scope> baseScopes = (List<Scope>)wsdlVisitor.getInheritedScopeMap().get(scope);
        if (baseScopes != null) {
            List<Scope> scopeList = new ArrayList<Scope>();
            for (Scope scopeName : baseScopes) {
                scopeList.add(scopeName);
            }
            result = findSchemaTypeInBaseScope(scopeList, scope, defn, 
                                               schemaRef, node, wsdlVisitor, holder);
        }
        return result;
    }
    
    // Does a breath depth search first.
    public static boolean findSchemaTypeInBaseScope(List<Scope> scopeList, Scope scope, 
                                                    Definition defn, XmlSchema schemaRef,
                                                    AST node, WSDLASTVisitor wsdlVisitor,
                                                    VisitorTypeHolder holder) {
        List<Scope> inheritedList = new ArrayList<Scope>();
        boolean result = false;
        for (Scope scopeName : scopeList) {
            inheritedList.add(scopeName);
        }        
        
        if (scopeList != null) {            
            Iterator iterator = scopeList.iterator();
            while (iterator.hasNext()) {
                Scope inheritScope = (Scope)iterator.next();

                Scope scopedName = new Scope(inheritScope, node);
                result = findScopeSchemaType(scopedName, schemaRef, wsdlVisitor, holder);
                if (!result) {
                    inheritedList.remove(inheritScope);
                    List<Scope> scopes = wsdlVisitor.getInheritedScopeMap().get(inheritScope);
                    if (scopes != null) {
                        for (Scope scopeName : scopes) {
                            inheritedList.add(scopeName);
                        }
                    }
                } else {
                    return result;
                }
            }

            if (!inheritedList.isEmpty()) {
                List<Scope> baseList = new ArrayList<Scope>();
                for (Scope scopeName : inheritedList) {
                    baseList.add(scopeName);
                }

                result = findSchemaTypeInBaseScope(baseList, scope, defn, schemaRef, node, wsdlVisitor,
                                                   holder);
            }
        }
        return result;
    }
    
    // Searches this scope for the schema type.
    private static boolean findScopeSchemaType(Scope scopedName, XmlSchema schemaRef, 
                                           WSDLASTVisitor wsdlVisitor, 
                                           VisitorTypeHolder holder) {
        
        XmlSchemaCollection schemas = wsdlVisitor.getSchemas();
        TypeMappingType typeMap = wsdlVisitor.getTypeMap();
        ModuleToNSMapper mapper = wsdlVisitor.getModuleToNSMapper();
        WSDLSchemaManager manager = wsdlVisitor.getManager();

        boolean result = findNonSchemaType(scopedName.toString(), wsdlVisitor, holder);
        if (!result) {
            QName qname = null;
            XmlSchema xmlSchema = schemaRef;
            String tns = wsdlVisitor.getModuleToNSMapper().map(scopedName.getParent());
            if (tns != null) {
                xmlSchema = wsdlVisitor.getManager().getXmlSchema(tns);
            }
            XmlSchemaType stype = null;
            if (xmlSchema != null) {
                // Exceptions are treated as a special case as for the
                // doc/literal style
                // in the schema we will have an element and a complextype
                // so the name
                // and the typename will be different.

                String scopedNameString = null;
                if (mapper.isDefaultMapping()) {
                    scopedNameString = scopedName.toString();
                } else {
                    scopedNameString = scopedName.tail();
                }

                if (exceptionMode) {
                    qname = new QName(xmlSchema.getTargetNamespace(), scopedNameString + "Type");
                } else {
                    qname = new QName(xmlSchema.getTargetNamespace(), scopedNameString);
                }

                stype = xmlSchema.getTypeByName(qname);
                if (stype == null) {
                    stype = schemas.getTypeByQName(qname);
                }
            }
            if (stype != null) {
                result = true;
            }
            if (result && holder != null) {
                holder.setSchemaType(stype);
                holder.setCorbaType(getCorbaSchemaType(xmlSchema, typeMap, stype, scopedName));
                // add a xmlschema import
                if (!schemaRef.getTargetNamespace().equals(xmlSchema.getTargetNamespace())) {
                    String importFile = wsdlVisitor.getOutputDir() + System.getProperty("file.separator")
                                        + scopedName.getParent().toString("_");
                    manager.addXmlSchemaImport(schemaRef, xmlSchema, importFile);
                }
            }
        }
        return result;
    }
    
    
    
    public static CorbaTypeImpl findCorbaTypeForSchemaType(TypeMappingType typeMap, 
                                                           QName schemaTypeName,
                                                           Scope scopedName) {
        CorbaTypeImpl result = null;
        Iterator corbaTypes = typeMap.getStructOrExceptionOrUnion().iterator();
        while (corbaTypes.hasNext()) {
            CorbaTypeImpl type = (CorbaTypeImpl) corbaTypes.next();         
            if ((type instanceof Sequence)
                || (type instanceof Array)
                || (type.getType() == null)
                || (type instanceof Anonsequence)
                || (type instanceof Anonarray)) {
                //REVISIT, cannot compare the type because they are incorrect
                if (type.getQName().getLocalPart().equals(schemaTypeName.getLocalPart())) {
                    result = type;
                    break;
                }               
                
                // If we are using the module to ns mapping, then the name of the type in schema
                // and in the typemap are actually different.  We should then compare with the scoped
                // name that we are given.
                if (type.getQName().getLocalPart().equals(scopedName.toString())) {
                    result = type;
                    break;
                }

            } else if (schemaTypeName.equals(type.getType())) {
                result = type;
                break;
            }
        }
        return result;
    }  

    public static CorbaTypeImpl findCorbaType(TypeMappingType typeMap, QName typeName) {
        CorbaTypeImpl result = null;
        Iterator corbaTypes = typeMap.getStructOrExceptionOrUnion().iterator();
        while (corbaTypes.hasNext()) {
            CorbaTypeImpl type = (CorbaTypeImpl) corbaTypes.next();
            if (type.getQName().equals(typeName)) {
                result = type;
                break;
            }
        }
        return result;
    }     
    
    protected static boolean isFullyScopedName(AST node) {
        if (node.getType() == IDLTokenTypes.IDENT
            && node.getFirstChild() != null
            && ((node.getFirstChild().getType() == IDLTokenTypes.SCOPEOP)
                || (node.getFirstChild().getType() == IDLTokenTypes.IDENT))) {
            return true;
        }
        return false;
    }
    
    protected static Scope getFullyScopedName(Scope currentScope, AST node) {
        Scope scopedName = new Scope();
        if (!currentScope.toString().equals(node.getText())) {
            scopedName = new Scope(currentScope);
        }
        scopedName = new Scope(scopedName, node);
        AST scopeNode = node.getFirstChild();
        if (node.getFirstChild().getType() == IDLTokenTypes.IDENT) {
            scopedName = new Scope(scopedName, scopeNode);
        }
        while (scopeNode.getNextSibling() != null) {
            scopeNode = scopeNode.getNextSibling(); 
            scopedName = new Scope(scopedName, scopeNode);
        }
        return scopedName;
    }

    protected static boolean findNonSchemaType(String name,
                                               WSDLASTVisitor wsdlVisitor,
                                               VisitorTypeHolder holder) {
        boolean result = false;
        TypeMappingType typeMap = wsdlVisitor.getTypeMap();
        XmlSchemaCollection schemas = wsdlVisitor.getSchemas();

        QName qname = new QName(typeMap.getTargetNamespace(), name);
        CorbaTypeImpl corbaType = findCorbaType(typeMap, qname);
        if (corbaType != null) {
            if (corbaType instanceof Alias) {
                result = true;
                if (holder != null) {
                    populateAliasSchemaType(corbaType, wsdlVisitor, holder);
                }
            } else if (((corbaType instanceof Sequence) || (corbaType instanceof Anonsequence))
                       && ((corbaType.getType().equals(Constants.XSD_BASE64))
                           || (corbaType.getType().equals(Constants.XSD_BASE64)))) {
                //special case of sequence of octets
                result = true;
                if (holder != null) {
                    holder.setCorbaType(corbaType);
                    holder.setSchemaType(schemas.getTypeByQName(corbaType.getType()));
                }
            }
        }
        return result;
    }

    protected static void populateAliasSchemaType(CorbaTypeImpl corbaType,
                                                  WSDLASTVisitor wsdlVisitor,
                                                  VisitorTypeHolder holder) {
        XmlSchemaCollection schemas = wsdlVisitor.getSchemas();
        TypeMappingType typeMap = wsdlVisitor.getTypeMap();
        holder.setCorbaType(corbaType);
        Alias alias = (Alias) corbaType;
        //loop through alias base types, till you get a non-alias corba type
        CorbaTypeImpl type = findCorbaType(typeMap, alias.getBasetype());
        while ((type != null) && (type instanceof Alias)) {
            alias = (Alias) type;
            type = findCorbaType(typeMap, alias.getBasetype());
        }
        QName tname;
        if (type == null) {
            //it must be a primitive type
            tname = xmlSchemaPrimitiveMap.get(alias.getBasetype());
        } else {
            tname = type.getType();
        }
        XmlSchemaType stype = schemas.getTypeByQName(tname);
        if (stype == null) {
            XmlSchema xmlSchema = wsdlVisitor.getManager().getXmlSchema(tname.getNamespaceURI());
            if (xmlSchema != null) {
                stype = xmlSchema.getTypeByName(tname);
            } else {
                stype = wsdlVisitor.getSchema().getTypeByName(tname);
            }
        }
        holder.setSchemaType(stype);
    }
        
}
