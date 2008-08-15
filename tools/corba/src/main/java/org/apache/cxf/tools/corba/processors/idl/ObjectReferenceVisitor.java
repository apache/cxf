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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import antlr.collections.AST;

import org.apache.cxf.binding.corba.wsdl.BindingType;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.Object;
import org.apache.cxf.tools.corba.common.ReferenceConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.utils.NamespaceMap;

public class ObjectReferenceVisitor extends VisitorBase {
    
    private WSDLASTVisitor objRefWsdlVisitor;
    
    public ObjectReferenceVisitor(Scope scope,
                                  Definition defn,
                                  XmlSchema schemaRef,
                                  WSDLASTVisitor wsdlVisitor) {
        super(scope, defn, schemaRef, wsdlVisitor);
        objRefWsdlVisitor = wsdlVisitor;
        
    }

    public static boolean accept(Scope scope, XmlSchema s, 
                                 Definition def, AST node, WSDLASTVisitor wsdlVisitor) {
        boolean result = false;
        if (node.getType() == IDLTokenTypes.LITERAL_Object) {
            result = true;
        } else if (node.getType() == IDLTokenTypes.IDENT && hasBinding(scope, s, def, node, wsdlVisitor)) {
            result = true;
        }
        return result;
    }
    
    public void visit(AST node) {
        if (!objRefWsdlVisitor.getDeclaredWSAImport()) {
            addWSAddressingImport(schema);
        }
        objRefWsdlVisitor.setDeclaredWSAImport(true);
        
        // There are two types of object references we can encounter.  Each one 
        // requires us to do something differnt so we'll have methods for each
        // type. Also need to check if its a forward declared object reference.
        if (isForwardDeclaredReference(getScope(), schema, node, scopedNames, wsdlVisitor)) {
            visitForwardDeclaredObjectReference(getScope(), schemas, schema, 
                                                node, scopedNames, wsdlVisitor);             
        } else if (node.getType() == IDLTokenTypes.LITERAL_Object) {
            visitDefaultTypeObjectReference(node);
        } else {
            // This should be of type IDENT
            visitCustomTypeObjectReference(node);
        }
    }
    
    private void visitDefaultTypeObjectReference(AST node) {
        // Even though we don't need to add a schema definition for a default endpoint
        // type, we still need to create a schema type so that the visitor knows what
        // kind of parameter this is.  For a default endpoint, we'll just provide a
        // reference to a WS addressing EndpointReferenceType.
        XmlSchema scs[] = schemas.getXmlSchema(ReferenceConstants.WSADDRESSING_NAMESPACE);
        XmlSchema wsaSchema = null;
        if (scs != null) {
            for (XmlSchema sc : scs) {
                if (ReferenceConstants.WSADDRESSING_NAMESPACE.equals(sc.getTargetNamespace())) {
                    wsaSchema = sc;
                    break;
                }
            }
        }
        if (wsaSchema == null) {
            wsaSchema = new XmlSchema(ReferenceConstants.WSADDRESSING_NAMESPACE, schemas);
        }
        XmlSchemaType objectType = new XmlSchemaType(wsaSchema);
        objectType.setName(ReferenceConstants.WSADDRESSING_LOCAL_NAME);
        setSchemaType(objectType);
        
        // Build and assign the corba:object to the visitor
        Object corbaObject = new Object();
        corbaObject.setQName(new QName(typeMap.getTargetNamespace(), "CORBA.Object"));
        corbaObject.setRepositoryID("IDL:omg.org/CORBA/Object/1.0");
        corbaObject.setType(objectType.getQName());
        setCorbaType(corbaObject);
        
        // Add the object definition to the typemap.  We only need to add the default
        // type once.
        if (!isReferenceCORBATypeDefined(corbaObject.getQName())) {
            typeMap.getStructOrExceptionOrUnion().add(corbaObject);
        } 
    }
    
    private void visitCustomTypeObjectReference(AST node) {
        QName bindingName = null;
        QName referenceName = null;
        String repositoryID = null;
        Scope currentScope = getScope();
        Scope customScope = null;
        if ((node.getFirstChild() == null)
            || (node.getFirstChild() != null && node.getFirstChild().getType() != IDLTokenTypes.SCOPEOP)) {
            while (bindingName == null && currentScope != currentScope.getParent()) {
                if (ScopedNameVisitor.isFullyScopedName(node)) {
                    customScope = ScopedNameVisitor.getFullyScopedName(currentScope, node);
                } else {
                    customScope = new Scope(currentScope, node);
                }

                if (mapper.isDefaultMapping()) {
                    referenceName = new QName(schema.getTargetNamespace(), customScope.toString() + "Ref");
                } else {
                    String tns = mapper.map(customScope.getParent());
                    referenceName = new QName(tns, customScope.tail() + "Ref");
                }

                repositoryID = customScope.toIDLRepositoryID();
                bindingName = getBindingQNameByID(definition, repositoryID, objRefWsdlVisitor);
                currentScope = currentScope.getParent();

            }
        }
                
        if (bindingName == null) {

           // Global scope is our last chance to resolve the node
            if (ScopedNameVisitor.isFullyScopedName(node)) {
                customScope = ScopedNameVisitor.getFullyScopedName(new Scope(), node);
                if (mapper.isDefaultMapping()) {
                    referenceName = new QName(schema.getTargetNamespace(),
                                              customScope.toString() + "Ref");
                } else {
                    String tns = mapper.map(customScope.getParent());
                    referenceName = new QName(tns, customScope.tail() + "Ref");
                }
            } else {
                //customScope = currentScope;
                customScope = new Scope(new Scope(), node);
                if (mapper.isDefaultMapping()) {
                    referenceName = new QName(schema.getTargetNamespace(),
                                              customScope.toString() + "Ref");
                } else {
                    String tns = mapper.map(customScope.getParent());
                    referenceName = new QName(tns, customScope.tail() + "Ref");
                }
            }
            repositoryID = customScope.toIDLRepositoryID();
            bindingName = getBindingQNameByID(definition, repositoryID, objRefWsdlVisitor);
        }
        
        if (bindingName == null) {
            // We need to have a binding for this kind of object reference to work
            throw new RuntimeException("[ObjectReferenceVisitor: No binding available for endpoint]");
        }

        // Create a schema namespace for WS addressing and use it to create an endpoint 
        // reference type.  This will be used as the type for our endpoint reference.
        XmlSchema scs[] = schemas.getXmlSchema(ReferenceConstants.WSADDRESSING_NAMESPACE);
        XmlSchema wsaSchema = null;
        if (scs != null) {
            for (XmlSchema sc : scs) {
                if (ReferenceConstants.WSADDRESSING_NAMESPACE.equals(sc.getTargetNamespace())) {
                    wsaSchema = sc;
                    break;
                }
            }
        }
        if (wsaSchema == null) {
            wsaSchema = new XmlSchema(ReferenceConstants.WSADDRESSING_NAMESPACE, schemas);
        }
        XmlSchemaType wsaType = new XmlSchemaType(wsaSchema);
        wsaType.setName(ReferenceConstants.WSADDRESSING_LOCAL_NAME);
        
        // Check to see if we have already defined an element for this reference type.  If
        // we have, then there is no need to add it to the schema again.
        isDuplicateReference(referenceName, bindingName, customScope, wsaType, node);        

        setSchemaType(wsaType);
        
        // Build and assign the corba:object to the visitor
        Object corbaObject = new Object();
        corbaObject.setBinding(bindingName);
        corbaObject.setQName(new QName(typeMap.getTargetNamespace(), customScope.toString()));
        corbaObject.setRepositoryID(repositoryID);
        corbaObject.setType(wsaType.getQName());
        setCorbaType(corbaObject);
        
        // Add the object definition to the typemap.  We only need to add the default
        // type once.
        if (!isReferenceCORBATypeDefined(corbaObject.getQName())) {
            typeMap.getStructOrExceptionOrUnion().add(corbaObject);
        } 
    }
    
    private void isDuplicateReference(QName referenceName, QName bindingName, Scope refScope, 
                                      XmlSchemaType wsaType, AST node) {
        XmlSchema refSchema = null;
        if (!mapper.isDefaultMapping()) {
            String tns = mapper.map(refScope.getParent());
            String refSchemaFileName = getWsdlVisitor().getOutputDir()
                + System.getProperty("file.separator")
                + refScope.getParent().toString("_") + ".xsd";
            refSchema = manager.getXmlSchema(tns);
            if (refSchema == null) {
                refSchema = manager.createXmlSchema(tns, wsdlVisitor.getSchemas());
            }
            addWSAddressingImport(refSchema);
            manager.addXmlSchemaImport(schema, refSchema, refSchemaFileName);
        } else {
            refSchema = schema;
        }
        
        // Check to see if we have already defined an element for this reference type.  If
        // we have, then there is no need to add it to the schema again.
        if (!isReferenceSchemaTypeDefined(referenceName, refSchema)) {
            // We need to add a new element definition to the schema section of our WSDL.
            // For custom endpoint types, this should contain an annotation which points
            // to the binding which will be used for this endpoint type.
            XmlSchemaElement refElement = new XmlSchemaElement();
            refElement.setQName(referenceName);
            refElement.setName(referenceName.getLocalPart());
            refElement.setSchemaType(wsaType);
            refElement.setSchemaTypeName(wsaType.getQName());
      
            // Create an annotation which contains the CORBA binding for the element
            XmlSchemaAnnotation annotation = new XmlSchemaAnnotation();
            XmlSchemaAppInfo appInfo = new XmlSchemaAppInfo();
            try {
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = db.newDocument();
                Element el = doc.createElement("appinfo");
                el.setTextContent("corba:binding=" + bindingName.getLocalPart());
                // TODO: This is correct but the appinfo markup is never added to the 
                // schema.  Investigate.
                appInfo.setMarkup(el.getChildNodes());
            } catch (ParserConfigurationException ex) {
                throw new RuntimeException("[ObjectReferenceVisitor: error creating endpoint schema]");
            }
            
            annotation.getItems().add(appInfo);
            
            refElement.setAnnotation(annotation);

            refSchema.getElements().add(referenceName, refElement);
            refSchema.getItems().add(refElement);
        }        
    }
    
    private boolean isReferenceCORBATypeDefined(QName objectReferenceName) {
        // Get the list of all corba types already defined and look for the provided
        // QName.  If we have defined this type, we don't need to add it to the typemap
        // again.
        List<CorbaTypeImpl> allTypes = typeMap.getStructOrExceptionOrUnion();
        for (Iterator<CorbaTypeImpl> iter = allTypes.iterator(); iter.hasNext();) {
            CorbaTypeImpl impl = iter.next();
            if (impl.getQName().equals(objectReferenceName))  {
                return true;
            }
        }
        return false;
    }

    private boolean isReferenceSchemaTypeDefined(QName objectReferenceName,
                                                 XmlSchema refSchema) {
        XmlSchemaObjectCollection schemaObjects = refSchema.getItems();

        for (Iterator iter = schemaObjects.getIterator(); iter.hasNext();) {
            java.lang.Object schemaObj = iter.next();
            
            if (schemaObj instanceof XmlSchemaElement) {
                XmlSchemaElement el = (XmlSchemaElement)schemaObj;
                
                if (el.getName().equals(objectReferenceName.getLocalPart())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void addWSAddressingImport(XmlSchema s) {
        boolean alreadyImported = false;
        for (Iterator i = s.getIncludes().getIterator(); i.hasNext();) {
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
            s.getItems().add(wsaImport);
            s.getIncludes().add(wsaImport);
        }
        
        // Add the addressing namespace to the WSDLs list of namespaces.
        definition.addNamespace(ReferenceConstants.WSADDRESSING_PREFIX,
                                ReferenceConstants.WSADDRESSING_NAMESPACE);
        
        try {
            // This is used to get the correct prefix in the schema section of
            // the wsdl.  If we don't have this, then this namespace gets an 
            // arbitrary prefix (e.g. ns5 instead of wsa).
            NamespaceMap nsMap = (NamespaceMap)s.getNamespaceContext();
            if (nsMap == null) {
                nsMap = new NamespaceMap();
                nsMap.add(ReferenceConstants.WSADDRESSING_PREFIX, 
                          ReferenceConstants.WSADDRESSING_NAMESPACE);
                s.setNamespaceContext(nsMap);
            } else {
                nsMap.add(ReferenceConstants.WSADDRESSING_PREFIX, 
                          ReferenceConstants.WSADDRESSING_NAMESPACE);
            }
        } catch (ClassCastException ex) {
            // Consume the exception.  It is still OK with the default prefix, 
            // just not as clear.
        }
        
    }
    
    private static QName getBindingQNameByID(Definition wsdlDef, String repositoryID, 
                                             WSDLASTVisitor wsdlVisitor) {
        // We need to find the binding which corresponds with the given repository ID.
        // This is specified in the schema definition for a custom endpoint 
        // reference type.
        Collection bindings = wsdlDef.getBindings().values();
        if (bindings.isEmpty() && !wsdlVisitor.getModuleToNSMapper().isDefaultMapping()) {
            // If we are not using the default mapping, then the binding definitions are not 
            // located in the current Definition object, but nistead in the root Definition 
            bindings = wsdlVisitor.getDefinition().getBindings().values();
        }
        
        for (Iterator iter = bindings.iterator(); iter.hasNext();) {
            Binding b = (Binding)iter.next();
            List extElements = b.getExtensibilityElements();
            
            for (Iterator extIter = extElements.iterator(); extIter.hasNext();) {
                java.lang.Object element = extIter.next();
                
                if (element instanceof BindingType) {
                    BindingType bt = (BindingType)element;                    
                    if (bt.getRepositoryID().equals(repositoryID)) {
                        if (wsdlVisitor.getSupportPolymorphicFactories()) {
                            return new QName(b.getQName().getNamespaceURI(),
                                             "InferFromTypeId",
                                             b.getQName().getPrefix());
            
                        } else {
                            return b.getQName();
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    protected static boolean isForwardDeclaredReference(Scope scope, XmlSchema schema, AST node,
                                               ScopeNameCollection scopedNames, WSDLASTVisitor wsdlVisitor) {
        boolean isForward = false;
        Scope currentScope = scope;
        
        // Check for forward declaration from local scope outwards
        if ((node.getFirstChild() == null)
            || (node.getFirstChild() != null && node.getFirstChild().getType() != IDLTokenTypes.SCOPEOP)) {
            while (!isForward && currentScope != currentScope.getParent()) {
                Scope scopedName = null;
                if (ScopedNameVisitor.isFullyScopedName(node)) {
                    scopedName = ScopedNameVisitor.getFullyScopedName(currentScope, node);
                } else {
                    scopedName = new Scope(currentScope, node);
                }

                if (scopedNames.getScope(scopedName) != null) {
                    isForward = true;
                }
                currentScope = currentScope.getParent();
                //fqName = scopedName;
            }            
        }
        // Check for forward declaration in global scope
        if (!isForward) {
            Scope scopedName = null;
            if (ScopedNameVisitor.isFullyScopedName(node)) {
                scopedName = ScopedNameVisitor.getFullyScopedName(new Scope(), node);
            } else {
                //scopedName = scope;
                scopedName = new Scope(new Scope(), node);
            }
            if (scopedNames.getScope(scopedName) != null) {                
                isForward = true;
            }
            //fqName = scopedName;
        }                

        return isForward;
    }
          
    protected void visitForwardDeclaredObjectReference(Scope scope, 
                                                       XmlSchemaCollection schemas, 
                                                       XmlSchema schema, AST node,
                                                       ScopeNameCollection scopedNames, 
                                                       WSDLASTVisitor wsdlVisitor) {
     
        XmlSchemaType result = null; 
        Scope currentScope = scope;
        
        // checks from innermost local scope outwards
        if ((node.getFirstChild() == null)
            || (node.getFirstChild() != null && node.getFirstChild().getType() != IDLTokenTypes.SCOPEOP)) {
            while (result == null && currentScope != currentScope.getParent()) {                
                Scope scopedName = null;
                if (ScopedNameVisitor.isFullyScopedName(node)) {
                    scopedName = ScopedNameVisitor.getFullyScopedName(currentScope, node);
                } else {
                    scopedName = new Scope(currentScope, node);
                }
                if (scopedNames.getScope(scopedName) != null) {
                    XmlSchema wsaSchema = null;
                    // check whether a schema with the required namespace has already been created
                    XmlSchema[] existingSchemas = schemas.getXmlSchemas();
                    for (XmlSchema xs : existingSchemas) {
                        if (xs.getTargetNamespace().equals(ReferenceConstants.WSADDRESSING_NAMESPACE)) {
                            // if it has been created, reuse it
                            wsaSchema = xs;
                            result = wsaSchema.getTypeByName(ReferenceConstants.WSADDRESSING_LOCAL_NAME);
                            break;
                        }
                    }
                    // if not, create a new one and create the WS-Addressing EndpointReferenceType
                    if (wsaSchema == null) {
                        wsaSchema = new XmlSchema(ReferenceConstants.WSADDRESSING_NAMESPACE, schemas);
                        XmlSchemaType wsaType = new XmlSchemaType(wsaSchema);
                        wsaType.setName(ReferenceConstants.WSADDRESSING_LOCAL_NAME);
                        result = wsaType;
                    }
                }                
                currentScope = currentScope.getParent();
            }
        }
        if (result == null) {
            Scope scopedName = null;
            if (ScopedNameVisitor.isFullyScopedName(node)) {
                scopedName = ScopedNameVisitor.getFullyScopedName(new Scope(), node);
            } else {
                scopedName = scope;
            }
            if (scopedNames.getScope(scopedName) != null) {
                XmlSchema wsaSchema = null;
                // check whether a schema with the required namespace has already been created
                XmlSchema[] existingSchemas = schemas.getXmlSchemas();
                for (XmlSchema xs : existingSchemas) {
                    if (xs.getTargetNamespace().equals(ReferenceConstants.WSADDRESSING_NAMESPACE)) {
                        // if it has been created, reuse it
                        wsaSchema = xs;
                        result = wsaSchema.getTypeByName(ReferenceConstants.WSADDRESSING_LOCAL_NAME);
                        break;
                    }
                }
                // if not, create a new one and create the WS-Addressing EndpointReferenceType
                if (wsaSchema == null) {
                    wsaSchema = new XmlSchema(ReferenceConstants.WSADDRESSING_NAMESPACE, schemas);
                    XmlSchemaType wsaType = new XmlSchemaType(wsaSchema);
                    wsaType.setName(ReferenceConstants.WSADDRESSING_LOCAL_NAME);
                    result = wsaType;
                }
            }
        }
        
        setSchemaType(result);
    }

    private static boolean hasBinding(Scope scope, XmlSchema s, Definition def, 
                                      AST node, WSDLASTVisitor wsdlVisitor) {
        boolean result = false;
        QName bindingName = null;
        String repositoryID = null;
        Scope currentScope = scope;
        Scope customScope = null;
        if ((node.getFirstChild() == null) || (node.getFirstChild() != null 
            && node.getFirstChild().getType() != IDLTokenTypes.SCOPEOP)) {        
            while (bindingName == null
                && currentScope != currentScope.getParent()) {                
                if (ScopedNameVisitor.isFullyScopedName(node)) {
                    customScope = ScopedNameVisitor.getFullyScopedName(currentScope, node);                
                } else {
                    customScope = new Scope(currentScope, node);                    
                }
                repositoryID = customScope.toIDLRepositoryID();
                bindingName = getBindingQNameByID(def, repositoryID, wsdlVisitor);
                currentScope = currentScope.getParent();
            }
        }
                
        if (bindingName == null) {
            // Global scope is our last chance to resolve the node               
            if (ScopedNameVisitor.isFullyScopedName(node)) {
                customScope = ScopedNameVisitor.getFullyScopedName(new Scope(), node);
            } else {                
                customScope = new Scope(new Scope(), node);
            }
            repositoryID = customScope.toIDLRepositoryID();
            bindingName = getBindingQNameByID(def, repositoryID, wsdlVisitor);
        }
        
        if (bindingName != null) {
            result = true;
        }

        return result;
    }
}
