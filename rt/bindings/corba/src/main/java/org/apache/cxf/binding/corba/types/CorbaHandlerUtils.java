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
package org.apache.cxf.binding.corba.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.utils.CorbaUtils;
import org.apache.cxf.binding.corba.wsdl.Alias;
import org.apache.cxf.binding.corba.wsdl.Anonarray;
import org.apache.cxf.binding.corba.wsdl.Anonsequence;
import org.apache.cxf.binding.corba.wsdl.Array;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.Exception;
import org.apache.cxf.binding.corba.wsdl.MemberType;
import org.apache.cxf.binding.corba.wsdl.Sequence;
import org.apache.cxf.binding.corba.wsdl.Struct;
import org.apache.cxf.binding.corba.wsdl.Union;
import org.apache.cxf.binding.corba.wsdl.Unionbranch;
import org.apache.cxf.binding.corba.wsdl.W3CConstants;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroupBase;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

public final class CorbaHandlerUtils {
    private CorbaHandlerUtils() {
        //utility class
    }

    public static CorbaObjectHandler createTypeHandler(ORB orb,
                                                       QName name, 
                                                       QName idlType,
                                                       CorbaTypeMap typeMap) {
        CorbaObjectHandler handler = null;        
        TypeCode tc = CorbaUtils.getTypeCode(orb, idlType, typeMap);
        try {
            while (tc.kind().value() == TCKind._tk_alias) {
                Alias alias = (Alias) CorbaUtils.getCorbaType(idlType, typeMap);
                if (alias == null) {
                    throw new CorbaBindingException("Couldn't find corba alias type: " + idlType);
                }
                tc = tc.content_type();
                idlType = alias.getBasetype();
            }
        } catch (Throwable ex) {
            throw new CorbaBindingException(ex);
        }
        if (CorbaUtils.isPrimitiveIdlType(idlType)) {
            handler = new CorbaPrimitiveHandler(name, idlType, tc, null);
        } else if (tc.kind().value() == TCKind._tk_any) {
            // Any is a special kind of primitive so it gets its own handler
            handler = new CorbaAnyHandler(name, idlType, tc, null);
            ((CorbaAnyHandler)handler).setTypeMap(typeMap);
        } else {
            CorbaTypeImpl type = CorbaUtils.getCorbaType(idlType, typeMap);
            switch (tc.kind().value()) {
            case TCKind._tk_array:
                handler = new CorbaArrayHandler(name, idlType, tc, type);
                break;
            case TCKind._tk_enum:
                handler = new CorbaEnumHandler(name, idlType, tc, type);
                break;
            case TCKind._tk_except:
                handler = new CorbaExceptionHandler(name, idlType, tc, type);
                break;
            case TCKind._tk_fixed:
                handler = new CorbaFixedHandler(name, idlType, tc, type);
                break;
            case TCKind._tk_sequence:
                if (isOctets(type)) {
                    handler = new CorbaOctetSequenceHandler(name, idlType, tc, type);
                } else {
                    handler = new CorbaSequenceHandler(name, idlType, tc, type);
                }
                break;
            case TCKind._tk_struct:
                handler = new CorbaStructHandler(name, idlType, tc, type);
                break;
            case TCKind._tk_union:
                handler = new CorbaUnionHandler(name, idlType, tc, type);
                break;
            case TCKind._tk_string:
            case TCKind._tk_wstring:
                // These need to be here to catch the anonymous string types.
                handler = new CorbaPrimitiveHandler(name, idlType, tc, type);
                break;
            case TCKind._tk_objref:
                handler = new CorbaObjectReferenceHandler(name, idlType, tc, type);
                break;                
            default:
                handler = new CorbaObjectHandler(name, idlType, tc, type);                
            }
        }

        return handler;
    }

   
    public static CorbaObjectHandler initializeObjectHandler(ORB orb,
                                                             QName name, 
                                                             QName idlType,
                                                             CorbaTypeMap typeMap,
                                                             ServiceInfo serviceInfo) {
        Map<QName, CorbaObjectHandler> seenTypes = new HashMap<QName, CorbaObjectHandler>();
        return initializeObjectHandler(orb, name, idlType, typeMap, serviceInfo, seenTypes);
    }

    public static CorbaObjectHandler initializeObjectHandler(ORB orb,
                                                             QName name, 
                                                             QName idlType,
                                                             CorbaTypeMap typeMap,
                                                             ServiceInfo serviceInfo,
                                                             Map<QName, CorbaObjectHandler> seenTypes) {
        CorbaObjectHandler obj = createTypeHandler(orb, name, idlType, typeMap);
        if (!CorbaUtils.isPrimitiveIdlType(idlType)) {
            switch (obj.getTypeCode().kind().value()) {
            case TCKind._tk_any:
                ((CorbaAnyHandler)obj).setValue(orb.create_any());
                break;
            case TCKind._tk_array:
                initializeArrayHandler(orb, obj, typeMap, serviceInfo, seenTypes);
                break;
            case TCKind._tk_except:
                initializeExceptionHandler(orb, obj, typeMap, serviceInfo, seenTypes);
                break;
            case TCKind._tk_sequence:
                if (!isOctets(obj.getType())) {
                    initializeSequenceHandler(orb, obj, typeMap, serviceInfo, seenTypes);
                }
                break;
            case TCKind._tk_struct:
                initializeStructHandler(orb, obj, typeMap, serviceInfo, seenTypes);
                break;
            case TCKind._tk_union:
                initializeUnionHandler(orb, obj, typeMap, serviceInfo, seenTypes);
                break;

            default:
               // TODO: Should we raise an exception or log?
            }
        }
        return obj;
    }
    
    public static void initializeArrayHandler(ORB orb,
                                              CorbaObjectHandler obj, 
                                              CorbaTypeMap typeMap,
                                              ServiceInfo serviceInfo,
                                              Map<QName, CorbaObjectHandler> seenTypes) {
        QName arrayElementType = null;
        long arrayBound = 0;
        CorbaTypeImpl baseType = obj.getType();
        QName elementName;
        if (baseType instanceof Array) {
            Array arrayType = (Array)baseType;
            arrayElementType = arrayType.getElemtype();
            arrayBound = arrayType.getBound();
            elementName = arrayType.getElemname();
        } else {
            Anonarray anonArrayType = (Anonarray)baseType;
            arrayElementType = anonArrayType.getElemtype();
            arrayBound = anonArrayType.getBound();
            elementName = anonArrayType.getElemname();
        }
        for (int i = 0; i < arrayBound; ++i) {
            CorbaObjectHandler elementObj = 
                initializeObjectHandler(orb, elementName, arrayElementType, typeMap, serviceInfo,
                                        seenTypes);
            ((CorbaArrayHandler)obj).addElement(elementObj);
        }
    }
    
    public static void initializeExceptionHandler(ORB orb,
                                                  CorbaObjectHandler obj, 
                                                  CorbaTypeMap typeMap,
                                                  ServiceInfo serviceInfo,
                                                  Map<QName, CorbaObjectHandler> seenTypes) {
        Exception exceptType = (Exception)obj.getType();
        List<MemberType> exceptMembers = exceptType.getMember();
        QName typeName = exceptType.getType();
        for (int i = 0; i < exceptMembers.size(); ++i) {
            MemberType member = exceptMembers.get(i);
            QName memberName;
            if (member.isSetQualified() && member.isQualified() && (typeName != null)) {
                memberName = new QName(typeName.getNamespaceURI(), member.getName());
            } else {
                memberName = new QName("", member.getName());
            }
            QName memberType = member.getIdltype();
            CorbaObjectHandler memberObj = initializeObjectHandler(orb,
                                                                   memberName,
                                                                   memberType,
                                                                   typeMap,
                                                                   serviceInfo,
                                                                   seenTypes);
            ((CorbaExceptionHandler)obj).addMember(memberObj);
        }
    }
    
    public static void initializeSequenceHandler(ORB orb,
                                                 CorbaObjectHandler obj, 
                                                 CorbaTypeMap typeMap,
                                                 ServiceInfo serviceInfo,
                                                 Map<QName, CorbaObjectHandler> seenTypes) {
        QName seqElementType = null;
        long seqBound = 0;
        CorbaTypeImpl baseType = obj.getType();
        QName elementName;
        if (baseType instanceof Sequence) {
            Sequence seqType = (Sequence)baseType;
            seqElementType = seqType.getElemtype();
            seqBound = seqType.getBound();
            elementName = seqType.getElemname();
        } else {
            Anonsequence seqType = (Anonsequence)baseType;
            seqElementType = seqType.getElemtype();
            seqBound = seqType.getBound();
            elementName = seqType.getElemname();
        }
        if (seqBound == 0) {
            // This is an unbounded sequence.  Store a 'template' object that we can use to create
            // new objects as needed
            CorbaObjectHandler elementObj = null;
            
            // Check for a recursive type
            if (seenTypes.get(seqElementType) != null) {
                elementObj = seenTypes.get(seqElementType);
                elementObj.setRecursive(true);
                ((CorbaSequenceHandler)obj).hasRecursiveTypeElement(true);
            } else {
                elementObj = 
                    initializeObjectHandler(orb, elementName, seqElementType, typeMap, 
                                            serviceInfo, seenTypes);
            }
            ((CorbaSequenceHandler)obj).setTemplateElement(elementObj);
        }
        for (int i = 0; i < seqBound; ++i) {
            CorbaObjectHandler elementObj = null;
            
            // Check for a recursive type
            if (seenTypes.get(seqElementType) != null) {
                // Even though this is bounded, if we have a recursive type, we'll still use the
                // template object so that we don't overwrite the value of the element.
                elementObj = seenTypes.get(seqElementType);
                elementObj.setRecursive(true);
                ((CorbaSequenceHandler)obj).hasRecursiveTypeElement(true);
                ((CorbaSequenceHandler)obj).setTemplateElement(elementObj);
            } else {
                elementObj = 
                    initializeObjectHandler(orb, elementName, seqElementType, typeMap, serviceInfo,
                                            seenTypes);
                ((CorbaSequenceHandler)obj).addElement(elementObj);
            }
        }
    }
    
    public static void initializeStructHandler(ORB orb,
                                               CorbaObjectHandler obj, 
                                               CorbaTypeMap typeMap,
                                               ServiceInfo serviceInfo,
                                               Map<QName, CorbaObjectHandler> seenTypes) {
        Struct structType = (Struct)obj.getType();
        List<MemberType> structMembers = structType.getMember();
        QName typeName = structType.getType();

        seenTypes.put(obj.getIdlType(), obj);

        for (int i = 0; i < structMembers.size(); ++i) {
            MemberType member = structMembers.get(i);
            QName memberName;
            if (member.isSetQualified() && member.isQualified() && (typeName != null)) {
                memberName = new QName(typeName.getNamespaceURI(), member.getName());
            } else {
                memberName = new QName("", member.getName());
            }
            QName memberType = member.getIdltype();
            CorbaObjectHandler memberObj = initializeObjectHandler(orb,
                                                                   memberName,
                                                                   memberType,
                                                                   typeMap,
                                                                   serviceInfo,
                                                                   seenTypes);
            if (member.isSetAnonschematype() && member.isAnonschematype()) {
                memberObj.setAnonymousType(true);
            }
            ((CorbaStructHandler)obj).addMember(memberObj);
        }

        seenTypes.remove(obj.getIdlType());
    }
    
    public static void initializeUnionHandler(ORB orb,
                                              CorbaObjectHandler obj, 
                                              CorbaTypeMap typeMap,
                                              ServiceInfo serviceInfo, 
                                              Map<QName, CorbaObjectHandler> seenTypes) {
        Union unionType = (Union)obj.getType();
        // First handle the discriminator
        CorbaObjectHandler discObj = initializeObjectHandler(orb, 
                                                             new QName("discriminator"),
                                                             unionType.getDiscriminator(),
                                                             typeMap,
                                                             serviceInfo,
                                                             seenTypes);
        ((CorbaUnionHandler)obj).setDiscriminator(discObj);
        QName typeName = unionType.getType();

        seenTypes.put(obj.getIdlType(), obj);

        // Now handle all of the branches
        List<Unionbranch> unionBranches = unionType.getUnionbranch();
        for (int i = 0; i < unionBranches.size(); i++) {
            Unionbranch branch = unionBranches.get(i);
            QName branchName;
            if (branch.isSetQualified() && branch.isQualified() && (typeName != null)) {
                branchName = new QName(typeName.getNamespaceURI(), branch.getName());
            } else {
                branchName = new QName("", branch.getName());
            }
            QName branchIdlType = branch.getIdltype();
            CorbaObjectHandler branchObj = 
                initializeObjectHandler(orb, branchName, branchIdlType, typeMap, serviceInfo,
                                        seenTypes);
            ((CorbaUnionHandler)obj).addCase(branchObj);
        }

        seenTypes.remove(obj.getIdlType());
    }

    //Change this method to access the XmlSchemaCollection.
    public static String getNamespaceURI(ServiceInfo serviceInfo, QName typeName) {
        String nsUri = "";
        if ((typeName != null)
            && (CorbaUtils.isElementFormQualified(serviceInfo, typeName.getNamespaceURI()))) {
            nsUri = typeName.getNamespaceURI();
        }
        return nsUri;
    }

    public static XmlSchemaElement getXmlSchemaSequenceElement(XmlSchemaObject schemaType,
                                                               ServiceInfo serviceInfo) {
        XmlSchemaObject stype = schemaType;
        XmlSchemaElement el = null;
        if (schemaType instanceof XmlSchemaElement) {
            stype = ((XmlSchemaElement) schemaType).getSchemaType();
            if (stype == null) {
                stype = CorbaUtils.getXmlSchemaType(serviceInfo, 
                                                    ((XmlSchemaElement) schemaType).getRefName());
            }
        }
        
        if (stype instanceof XmlSchemaComplexType) {
            //only one element inside the XmlSchemaComplexType
            XmlSchemaComplexType ctype = (XmlSchemaComplexType) stype;
            XmlSchemaGroupBase group = (XmlSchemaGroupBase) ctype.getParticle();
            el = (XmlSchemaElement) group.getItems().getItem(0);
        } else {
            el = (XmlSchemaElement) schemaType;
        }
        return el;
    }

    public static CorbaTypeListener getTypeListener(QName name,
                                                    QName idlType,
                                                    CorbaTypeMap typeMap,
                                                    ORB orb, ServiceInfo serviceInfo)
        throws CorbaBindingException {
        CorbaObjectHandler handler = null;
        TypeCode tc = CorbaUtils.getTypeCode(orb, idlType, typeMap);
        try {
            while (tc.kind().value() == TCKind._tk_alias) {
                Alias alias = (Alias) CorbaUtils.getCorbaType(idlType, typeMap);
                if (alias == null) {
                    throw new CorbaBindingException("Couldn't find corba alias type: " + idlType);
                }
                tc = tc.content_type();
                idlType = alias.getBasetype();
            }
        } catch (Throwable ex) {
            throw new CorbaBindingException(ex);
        }
        CorbaTypeListener result = null;
        if (CorbaUtils.isPrimitiveIdlType(idlType)) {
            handler = new CorbaPrimitiveHandler(name, idlType, tc, null);
            result = new CorbaPrimitiveListener(handler);
        } else {
            CorbaTypeImpl type = CorbaUtils.getCorbaType(idlType, typeMap);
            switch (tc.kind().value()) {
            case TCKind._tk_any:
                handler = new CorbaAnyHandler(name, idlType, tc, type);
                ((CorbaAnyHandler)handler).setTypeMap(typeMap);
                result = new CorbaAnyListener(handler, typeMap, orb, serviceInfo);
                break;
            case TCKind._tk_array:
                handler = new CorbaArrayHandler(name, idlType, tc, type);
                result = new CorbaArrayListener(handler, typeMap, orb, serviceInfo);
                break;
            case TCKind._tk_enum:
                handler = new CorbaEnumHandler(name, idlType, tc, type);
                result = new CorbaEnumListener(handler);
                break;
            case TCKind._tk_except:
                handler = new CorbaExceptionHandler(name, idlType, tc, type);
                result = new CorbaExceptionListener(handler, typeMap, orb, serviceInfo);
                break;
            case TCKind._tk_fixed:
                handler = new CorbaFixedHandler(name, idlType, tc, type);
                result = new CorbaFixedListener(handler);
                break;
            case TCKind._tk_sequence:
                if (isOctets(type)) {
                    handler = new CorbaOctetSequenceHandler(name, idlType, tc, type);
                    result = new CorbaOctetSequenceListener(handler);
                } else {
                    handler = new CorbaSequenceHandler(name, idlType, tc, type);
                    result = new CorbaSequenceListener(handler, typeMap, orb, serviceInfo);
                }
                break;
            case TCKind._tk_string:
            case TCKind._tk_wstring:
                // These can be handled just like regular strings
                handler = new CorbaPrimitiveHandler(name, idlType, tc, type);
                result = new CorbaPrimitiveListener(handler);
                break;
            case TCKind._tk_struct:
                handler = new CorbaStructHandler(name, idlType, tc, type);
                result = new CorbaStructListener(handler, typeMap, orb, serviceInfo);
                break;
            case TCKind._tk_union:            
                handler = new CorbaUnionHandler(name, idlType, tc, type);
                result = new CorbaUnionListener(handler, typeMap, orb, serviceInfo);
                break;
            case TCKind._tk_objref:
                handler =
                    new CorbaObjectReferenceHandler(name, idlType, tc, type);
                result = new CorbaObjectReferenceListener(handler, orb);
                break;
            default:
                throw new CorbaBindingException("Unsupported complex type " + idlType);
            }
        }
        return result;
    }
    
    public static CorbaTypeEventProducer getTypeEventProducer(CorbaObjectHandler handler,
                                                              ServiceInfo serviceInfo,
                                                              ORB orb)
        throws CorbaBindingException {        
        QName idlType = handler.getIdlType();
        TypeCode tc = handler.getTypeCode();
        CorbaTypeEventProducer result = null;
        if (CorbaUtils.isPrimitiveIdlType(idlType)) {
            result = new CorbaPrimitiveTypeEventProducer(handler);
        } else {
            switch (tc.kind().value()) {
            case TCKind._tk_any:
                result = new CorbaAnyEventProducer(handler, serviceInfo, orb);
                break;
            case TCKind._tk_array:
                result = new CorbaArrayEventProducer(handler, serviceInfo, orb);
                break;
            case TCKind._tk_enum:
                result = new CorbaEnumEventProducer(handler);
                break;
            case TCKind._tk_except:
                result = new CorbaExceptionEventProducer(handler, serviceInfo, orb);
                break;
            case TCKind._tk_fixed:
                result = new CorbaFixedEventProducer(handler);
                break;
            case TCKind._tk_sequence:
                if (isOctets(handler.getType())) {
                    result = new CorbaOctetSequenceEventProducer(handler);
                } else {
                    result = new CorbaSequenceEventProducer(handler, serviceInfo, orb);
                }
                break;
            case TCKind._tk_string:
            case TCKind._tk_wstring:
                // These can be handled just like regular strings
                result = new CorbaPrimitiveTypeEventProducer(handler);
                break;
            case TCKind._tk_struct:
                if (handler.isAnonymousType()) {
                    result = new CorbaAnonStructEventProducer(handler, serviceInfo, orb);
                } else {
                    result = new CorbaStructEventProducer(handler, serviceInfo, orb);
                }
                break;
            case TCKind._tk_union:
                result = new CorbaUnionEventProducer(handler, serviceInfo, orb);
                break;
            case TCKind._tk_objref:
                result = new CorbaObjectReferenceEventProducer(handler, serviceInfo, orb);
                break;
            default:
                throw new CorbaBindingException("Unsupported complex type "
                                                + idlType);
            }
        }
        return result;
    }

    public static boolean isPrimitiveIDLTypeSequence(CorbaObjectHandler handler) {
        CorbaTypeImpl seqType = handler.getType();
        QName seqElementType;
        if (seqType instanceof Anonsequence) {
            Anonsequence anonSeqType = (Anonsequence) seqType;
            seqElementType = anonSeqType.getElemtype();
        } else {
            Sequence type = (Sequence) seqType;
            seqElementType = type.getElemtype();
        }
        return CorbaUtils.isPrimitiveIdlType(seqElementType);
    }

    public static boolean isAnonType(XmlSchemaObject schemaObj) {
        boolean result = false;        
        if ((schemaObj != null) && !(schemaObj instanceof XmlSchemaElement)
             && !(schemaObj instanceof XmlSchemaComplexType)) {
            result = true;
        }
        return result;
    }

    public static boolean isOctets(CorbaTypeImpl baseType) {
        return baseType.getType().equals(W3CConstants.NT_SCHEMA_BASE64)
            || baseType.getType().equals(W3CConstants.NT_SCHEMA_HBIN);
    }
}
