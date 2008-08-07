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
package org.apache.cxf.binding.corba.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.CorbaStreamable;
import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.wsdl.Alias;
import org.apache.cxf.binding.corba.wsdl.Anonarray;
import org.apache.cxf.binding.corba.wsdl.Anonfixed;
import org.apache.cxf.binding.corba.wsdl.Anonsequence;
import org.apache.cxf.binding.corba.wsdl.Anonstring;
import org.apache.cxf.binding.corba.wsdl.Anonwstring;
import org.apache.cxf.binding.corba.wsdl.Array;
import org.apache.cxf.binding.corba.wsdl.CaseType;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaType;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.Enum;
import org.apache.cxf.binding.corba.wsdl.Enumerator;
import org.apache.cxf.binding.corba.wsdl.Exception;
import org.apache.cxf.binding.corba.wsdl.Fixed;
import org.apache.cxf.binding.corba.wsdl.MemberType;
import org.apache.cxf.binding.corba.wsdl.Sequence;
import org.apache.cxf.binding.corba.wsdl.Struct;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.cxf.binding.corba.wsdl.Union;
import org.apache.cxf.binding.corba.wsdl.Unionbranch;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.omg.CORBA.Any;
import org.omg.CORBA.NVList;
import org.omg.CORBA.ORB;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.UnionMember;

public final class CorbaUtils {

    static final QName EMPTY_QNAME = new QName("", "");
    static final Map<QName, TCKind> PRIMITIVE_TYPECODES = new HashMap<QName, TCKind>();

    private static final Logger LOG = LogUtils.getL7dLogger(CorbaUtils.class);
    private static final class LastExport {
        private final String ior;
        private final org.omg.CORBA.Object ref;
        LastExport(String iors, org.omg.CORBA.Object oref) {
            ior = iors;
            ref = oref;
        }
        String getIor() {
            return ior;
        }
        org.omg.CORBA.Object getRef() {
            return ref;
        }
    }
    private static final ThreadLocal<LastExport> LAST_EXPORT_CACHE = 
        new ThreadLocal<LastExport>();

    private CorbaUtils() {
        //utility class
    }
    
    
    public static QName getEmptyQName() {
        return EMPTY_QNAME;
    }

    public static TypeCode getTypeCode(ORB orb, QName type, CorbaTypeMap typeMap) {
        Stack<QName> seenTypes = new Stack<QName>();
        return getTypeCode(orb, type, null, typeMap, seenTypes);
    }

    public static TypeCode getTypeCode(ORB orb, 
                                       QName type, 
                                       CorbaTypeMap typeMap,
                                       Stack<QName> seenTypes) {
        return getTypeCode(orb, type, null, typeMap, seenTypes);
    }

    public static TypeCode getTypeCode(ORB orb,
                                       QName type,
                                       CorbaType obj,
                                       CorbaTypeMap typeMap) {
        Stack<QName> seenTypes = new Stack<QName>();
        return getTypeCode(orb, type, obj, typeMap, seenTypes);
    }

    public static TypeCode getTypeCode(ORB orb, 
                                       QName type, 
                                       CorbaType obj, 
                                       CorbaTypeMap typeMap,
                                       Stack<QName> seenTypes) {
        if (type == null) {
            throw new CorbaBindingException("corba:typemap type or elemtype information required" 
                    + (obj == null ? "" : " for " + obj)  
                    + (seenTypes.empty() ? "" : ", Enclosing type: " + seenTypes.elementAt(0))); 
        }       
        
        TypeCode tc = null;
        // first see if it is a primitive
        tc = getPrimitiveTypeCode(orb, type);
        if (tc == null && type.equals(CorbaConstants.NT_CORBA_ANY)) {
            // Anys are handled in a special way
            tc = orb.get_primitive_tc(TCKind.from_int(TCKind._tk_any));
        } else if (tc == null) {
            if (typeMap == null) {
                throw new CorbaBindingException("Unable to locate typemap for namespace \"" 
                                                + type.getNamespaceURI() + "\"");
            }
            
            tc = typeMap.getTypeCode(type); 

            if (tc == null) {
                if (obj == null) {
                    obj = typeMap.getType(type.getLocalPart());
                    if (obj == null) {
                        throw new CorbaBindingException("Unable to locate object definition");
                    }
                }
                tc = getComplexTypeCode(orb, type, obj, typeMap, seenTypes);
                if (tc != null) {
                    typeMap.addTypeCode(type, tc);
                }
            }
        }
        if (tc == null) {
            throw new CorbaBindingException("Corba type node with qname " + type + " is not supported");
        }       
        return tc;
    }

    public static TypeCode getPrimitiveTypeCode(ORB orb, QName type) {
        TCKind kind = PRIMITIVE_TYPECODES.get(type);
        if (kind != null) {
            return orb.get_primitive_tc(kind);
        }

        // There is a possiblitity that the idl type will not have its namespace URI set if it has
        // been read directly from the WSDL file as a string. Try with the standard corba namespace URI.
        if (type.getNamespaceURI() == null) {
            QName uriIdltype = new QName(CorbaConstants.NU_WSDL_CORBA, type.getLocalPart(), type.getPrefix());

            kind = PRIMITIVE_TYPECODES.get(uriIdltype);
            if (kind != null) {
                return orb.get_primitive_tc(kind);
            }
        }
        return null;
    }

    public static TypeCode getComplexTypeCode(ORB orb, 
                                              QName type, 
                                              Object obj, 
                                              CorbaTypeMap typeMap, 
                                              Stack<QName> seenTypes) {
        TypeCode tc = getAnonTypeCode(orb, type, obj, typeMap, seenTypes);
        
        if (tc == null) {
            if (obj instanceof Alias) {
                Alias aliasType = (Alias)obj;
                tc = orb.create_alias_tc(aliasType.getRepositoryID(), 
                                         getTypeCodeName(aliasType.getName()), 
                                         getTypeCode(orb, aliasType.getBasetype(), typeMap, seenTypes)); 
            } else if (obj instanceof Array) {
                Array arrayType = (Array)obj;
                tc = orb.create_array_tc((int) arrayType.getBound(), 
                                         getTypeCode(orb, arrayType.getElemtype(), typeMap, seenTypes));
            } else if (obj instanceof Enum) {
                Enum enumType = (Enum)obj;
                String name = enumType.getName();
                List enums = enumType.getEnumerator();
                String[] members = new String[enums.size()];
                
                for (int i = 0; i < members.length; ++i) {
                    members[i] = ((Enumerator) enums.get(i)).getValue();
                }
                name = getTypeCodeName(name);
                tc = orb.create_enum_tc(enumType.getRepositoryID(), name, members);
            } else if (obj instanceof Exception) {
                Exception exceptType = (Exception)obj;
                
                // TODO: check to see if this is a recursive type.
                List list = exceptType.getMember();
                StructMember[] members = new StructMember[list.size()];
                for (int i = 0; i < members.length; ++i) {
                    MemberType member = (MemberType) list.get(i);
                    members[i] = new StructMember(member.getName(), 
                                                  getTypeCode(orb, member.getIdltype(), typeMap, seenTypes), 
                                                  null);
                }
                String name = getTypeCodeName(exceptType.getName());
                tc = orb.create_exception_tc(exceptType.getRepositoryID(), name, members);
            } else if (obj instanceof Fixed) {
                Fixed fixedType = (Fixed) obj;
                tc = orb.create_fixed_tc((short) fixedType.getDigits(), (short) fixedType.getScale());
            } else if (obj instanceof org.apache.cxf.binding.corba.wsdl.Object) {
                org.apache.cxf.binding.corba.wsdl.Object objType =
                    (org.apache.cxf.binding.corba.wsdl.Object)obj;
                if (objType.getName().equals("CORBA.Object")) {
                    tc = orb.create_interface_tc(objType.getRepositoryID(), "Object");
                } else {
                    tc = orb.create_interface_tc(objType.getRepositoryID(),
                                                 getTypeCodeName(objType.getName()));
                }
            } else if (obj instanceof Sequence) {
                Sequence seqType = (Sequence)obj;
                tc = orb.create_sequence_tc((int) seqType.getBound(), 
                                            getTypeCode(orb, seqType.getElemtype(), typeMap, seenTypes));
            } else if (obj instanceof Struct) {
                Struct structType = (Struct)obj;
                
                // TODO: check to see if this is a recursive type.
                if (seenTypes.contains(new QName(structType.getName()))) {
                    tc = orb.create_recursive_tc(structType.getRepositoryID());
                } else {
                    seenTypes.push(new QName(structType.getName()));
                    List list = structType.getMember();
                    StructMember[] members = new StructMember[list.size()];
                    for (int i = 0; i < members.length; ++i) {
                        MemberType member = (MemberType) list.get(i);
                        members[i] = new StructMember(member.getName(), 
                                                 getTypeCode(orb, member.getIdltype(), typeMap, seenTypes),
                                                 null);
                    }
                    String name = getTypeCodeName(structType.getName());
                    tc = orb.create_struct_tc(structType.getRepositoryID(), name, members);
                    seenTypes.pop();
                }
            } else if (obj instanceof Union) {
                tc = getUnionTypeCode(orb, obj, typeMap, seenTypes);
            }
        }
        return tc;
    }
    
    private static TypeCode getAnonTypeCode(ORB orb, 
                                            QName type, 
                                            Object obj, 
                                            CorbaTypeMap typeMap,
                                            Stack<QName> seenTypes) {
        TypeCode tc = null;
        if (obj instanceof Anonarray) {
            Anonarray anonArrayType = (Anonarray)obj;
            tc = orb.create_array_tc((int) anonArrayType.getBound(), 
                                     getTypeCode(orb, anonArrayType.getElemtype(), typeMap, seenTypes));
        } else if (obj instanceof Anonfixed) {
            Anonfixed anonFixedType = (Anonfixed) obj;
            tc = orb.create_fixed_tc((short) anonFixedType.getDigits(), (short) anonFixedType.getScale());
        } else if (obj instanceof Anonsequence) {
            Anonsequence anonSeqType = (Anonsequence)obj;
            tc = orb.create_sequence_tc((int) anonSeqType.getBound(), 
                                        getTypeCode(orb, anonSeqType.getElemtype(), typeMap, seenTypes));
        } else if (obj instanceof Anonstring) {
            Anonstring anonStringType = (Anonstring)obj;
            tc = orb.create_string_tc((int)anonStringType.getBound());
        } else if (obj instanceof Anonwstring) {
            Anonwstring anonWStringType = (Anonwstring)obj;
            tc = orb.create_wstring_tc((int)anonWStringType.getBound());
        }
        return tc;
    }

    public static TypeCode getUnionTypeCode(ORB orb, 
                                            Object obj, 
                                            CorbaTypeMap typeMap,
                                            Stack<QName> seenTypes) {
        Union unionType = (Union)obj;

        if (seenTypes.contains(new QName(unionType.getName()))) {
            return orb.create_recursive_tc(unionType.getRepositoryID());
        } else {
            seenTypes.push(new QName(unionType.getName()));
        
            TypeCode discTC = getTypeCode(orb, unionType.getDiscriminator(), typeMap, seenTypes);
            Map<String, UnionMember> members = new LinkedHashMap<String, UnionMember>();
            List<Unionbranch> branches = unionType.getUnionbranch();
            for (Iterator<Unionbranch> branchIter = branches.iterator(); branchIter.hasNext();) {
                Unionbranch branch = branchIter.next();
                List<CaseType> cases = branch.getCase();
                for (Iterator<CaseType> caseIter = cases.iterator(); caseIter.hasNext();) {
                    CaseType cs = caseIter.next();
                    if (!members.containsKey(cs.getLabel())) {
                        UnionMember member = new UnionMember();
                        member.name = branch.getName();
                        member.type = getTypeCode(orb, branch.getIdltype(), typeMap, seenTypes);
                        member.label = orb.create_any();
                        // We need to insert the labels in a way that depends on the type of the 
                        // discriminator.  According to the CORBA specification, the following types 
                        // are permissable as discriminator types:
                        //    * signed & unsigned short
                        //    * signed & unsigned long
                        //    * signed & unsigned long long
                        //    * char
                        //    * boolean
                        //    * enum
                        switch (discTC.kind().value()) {
                        case TCKind._tk_short:
                            member.label.insert_short(Short.parseShort(cs.getLabel()));
                            break;
                        case TCKind._tk_ushort:
                            member.label.insert_ushort(Short.parseShort(cs.getLabel()));
                            break;
                        case TCKind._tk_long:
                            member.label.insert_long(Integer.parseInt(cs.getLabel()));
                            break;
                        case TCKind._tk_ulong:
                            member.label.insert_ulong(Integer.parseInt(cs.getLabel()));
                            break;
                        case TCKind._tk_longlong:
                            member.label.insert_longlong(Long.parseLong(cs.getLabel()));
                            break;
                        case TCKind._tk_ulonglong:
                            member.label.insert_ulonglong(Long.parseLong(cs.getLabel()));
                            break;
                        case TCKind._tk_char:
                            member.label.insert_char(cs.getLabel().charAt(0));
                            break;
                        case TCKind._tk_boolean:
                            member.label.insert_boolean(Boolean.parseBoolean(cs.getLabel()));
                            break;
                        case TCKind._tk_enum:
                            org.omg.CORBA.portable.OutputStream out = 
                                member.label.create_output_stream();
                            Enum enumVal = (Enum)getCorbaType(unionType.getDiscriminator(), typeMap);
                            List<Enumerator> enumerators = enumVal.getEnumerator();
                            for (int i = 0; i < enumerators.size(); ++i) {
                                Enumerator e = enumerators.get(i);
                                if (e.getValue().equals(cs.getLabel())) {
                                    out.write_long(i);
                                }
                            }
                            member.label.read_value(out.create_input_stream(), discTC);
                            break;
                        default:
                            throw new CorbaBindingException("Unsupported discriminator type");
                        }
                        // Some orbs are strict on how the case labels are stored for 
                        // each member.  So we can't
                        // simply insert the labels as strings 
                        members.put(cs.getLabel(), member);
                    }
                }
            }
            seenTypes.pop();
            return orb.create_union_tc(unionType.getRepositoryID(), 
                                       getTypeCodeName(unionType.getName()), 
                                       discTC, 
                                       (UnionMember[])members.values().toArray(
                                           new UnionMember[members.size()]));
        }
    }

    public static String getTypeCodeName(String name) {
        int pos = name.lastIndexOf(".");
        if (pos != -1) {
            name = name.substring(pos + 1);
        }
        return name;
    }

    public static boolean isPrimitiveIdlType(QName idltype) {
        TCKind kind = PRIMITIVE_TYPECODES.get(idltype);
        if (kind != null) {
            return true;
        }

        // There is a possiblitity that the idl type will not have its namespace URI set if it has
        // been read directly from the WSDL file as a string. Try with the standard corba namespace URI.
        if (idltype.getNamespaceURI() == null) {
            QName uriIdltype = new QName(CorbaConstants.NU_WSDL_CORBA, idltype.getLocalPart(), 
                                         idltype.getPrefix());
            kind = PRIMITIVE_TYPECODES.get(uriIdltype);
            if (kind != null) {
                return true;
            }
        }

        return false;
    }

    public static boolean isPrimitiveTypeCode(TypeCode tc) {
        return PRIMITIVE_TYPECODES.values().contains(tc.kind());
    }

    public static CorbaTypeImpl getCorbaType(QName idlType, CorbaTypeMap typeMap) {
        if (!isPrimitiveIdlType(idlType) && (typeMap != null)) {
            return (CorbaTypeImpl) typeMap.getType(idlType.getLocalPart());
        }
        return null;
    }

    public static CorbaTypeMap createCorbaTypeMap(List<TypeMappingType> tmTypes) {
        CorbaTypeMap map = null;
        if (tmTypes != null) {
            //Currently, only one type map
            TypeMappingType tmType = tmTypes.get(0);
            map = new CorbaTypeMap(tmType.getTargetNamespace());

            List<CorbaTypeImpl> types = tmType.getStructOrExceptionOrUnion();
            LOG.fine("Found " + types.size() + " types defined in the typemap");
            for (Iterator<CorbaTypeImpl> it = types.iterator(); it.hasNext();) {
                CorbaTypeImpl corbaType = it.next();
                String name = corbaType.getName();
                // There can be some instances where a prefix is added to the name by the tool
                // (e.g. Object Reference Names).  Since the name is read as a string, this
                // prefix is added to the types name.  Remove this as it is not needed.
                int pos = name.lastIndexOf(":");
                if (pos != -1) {
                    name = name.substring(pos + 1);
                    corbaType.setName(name);
                }
                    
                map.addType(name, corbaType);
                LOG.fine("Adding type " + name);
            }
        }
        return map;
    }
    
    
    
    public static boolean isValidURL(String url) {
        if ((url.startsWith("ior:")) || (url.startsWith("IOR:"))) {
            return true;
        } else if (url.startsWith("file:")) {
            return true;
        } else if (url.startsWith("corbaloc:")) {
            return true;
        } else if (url.startsWith("corbaname:")) {
            return true;
        }
        return false;
    }

    public static String getUniquePOAName(QName serviceName, String portName, String poaName) {
        //Create a unique name of the form
        //"{namespace_uri}service_name#port_name#user_provided_name"
        //The last part is only appended if it is non empty
        //
        String result = "{" + serviceName.getNamespaceURI() + "}"
            + serviceName.getLocalPart() + "#" + portName;

        if (poaName != null) {
            result += "#" + poaName;
        }

        return result;
    }
    
    public static boolean isIOR(String location) {
        if ((location.startsWith("ior:")) || (location.startsWith("IOR:"))) {
            return true;
        }
        return false;
    }


    public static String exportObjectReference(org.omg.CORBA.Object obj, ORB orb) {
        String ior = orb.object_to_string(obj);
        LAST_EXPORT_CACHE.set(new LastExport(ior, obj));
        return ior;
    }



    public static org.omg.CORBA.Object importObjectReference(ORB orb,
                                                             String url) {
        org.omg.CORBA.Object result;

        if (url.startsWith("file:")) {
            return importObjectReferenceFromFile(orb, url.substring(5));
        } else if ("IOR:".equalsIgnoreCase(url)) {
            throw new RuntimeException("Proxy not initialized. URL contains a invalid ior");
        }
    
        String trimmedUrl = url.trim();
        LastExport last = LAST_EXPORT_CACHE.get();
        if (last != null
            && trimmedUrl.equals(last.getIor())) {
            return last.getRef();
        }
        try {
            result = orb.string_to_object(trimmedUrl);
        } catch (java.lang.Exception ex) {
            throw new RuntimeException(ex);
        }

        return result;
    }

    public static org.omg.CORBA.Object importObjectReferenceFromFile(ORB orb,
                                                                     String url) {
        org.omg.CORBA.Object result;

        try {
            java.io.File file = new java.io.File(url);
            if (!file.exists()) {
                throw new RuntimeException("Could not find file " + url + " to read the object reference");
            }
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
            String ior = reader.readLine();
            if (ior == null) {
                throw new RuntimeException("Invalid object reference found in file " + url);
            }            
            result = orb.string_to_object(ior.trim());
            reader.close();            
        } catch (java.io.IOException ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }

    public static XmlSchemaType getXmlSchemaType(ServiceInfo serviceInfo, QName name) {
        XmlSchemaType result = null;
        if ((name != null) && (serviceInfo != null)) {
            SchemaCollection col = serviceInfo.getXmlSchemaCollection();
            result = col.getTypeByQName(name);
            if (result == null) {
                //check the name, if it is an element
                XmlSchemaElement el = col.getElementByQName(name);
                if (el != null) {
                    result = el.getSchemaType();
                }
            }
        }
        return result;
    }

    //Change this method to access the XmlSchemaCollection.
    public static boolean isElementFormQualified(ServiceInfo serviceInfo, String uri) {
        if (uri != null) {
            SchemaInfo schemaInfo = serviceInfo.getSchema(uri);
            if (schemaInfo != null) {
                return schemaInfo.isElementFormQualified();
            }
            Iterator<SchemaInfo> it = serviceInfo.getSchemas().iterator();
            while (it.hasNext()) {
                XmlSchema schema = it.next().getSchema();
                return isElementFormQualified(schema, uri);
            }
        }
        return false;
    }

    //Change this method to access the XmlSchemaCollection.
    private static boolean isElementFormQualified(XmlSchema schema, String uri) {
        if (uri.equals(schema.getTargetNamespace())) {
            return schema.getElementFormDefault().getValue().equals(XmlSchemaForm.QUALIFIED);
        }
        Iterator it = schema.getIncludes().getIterator();
        while (it.hasNext()) {
            XmlSchemaExternal extSchema = (XmlSchemaExternal) it.next();
            return isElementFormQualified(extSchema.getSchema(), uri);
        }
        return false;
    }

    //Change this method to access the XmlSchemaCollection.
    public static boolean isAttributeFormQualified(ServiceInfo serviceInfo, String uri) {
        if (uri != null) {
            SchemaInfo schemaInfo = serviceInfo.getSchema(uri);
            if (schemaInfo != null) {
                return schemaInfo.isAttributeFormQualified();
            }
            Iterator<SchemaInfo> it = serviceInfo.getSchemas().iterator();
            while (it.hasNext()) {
                XmlSchema schema = it.next().getSchema();
                return isAttributeFormQualified(schema, uri);
            }
        }
        return false;
    }

    //Change this method to access the XmlSchemaCollection.
    private static boolean isAttributeFormQualified(XmlSchema schema, String uri) {
        if (uri.equals(schema.getTargetNamespace())) {
            return schema.getAttributeFormDefault().getValue().equals(XmlSchemaForm.QUALIFIED);
        }
        Iterator it = schema.getIncludes().getIterator();
        while (it.hasNext()) {
            XmlSchemaExternal extSchema = (XmlSchemaExternal) it.next();
            return isAttributeFormQualified(extSchema.getSchema(), uri);
        }
        return false;
    }


    public static QName processQName(QName qname, ServiceInfo serviceInfo) {
        QName result = qname;
        if ((qname.getNamespaceURI() != null)
            && (!qname.getNamespaceURI().equals(""))
            && (!isElementFormQualified(serviceInfo, qname.getNamespaceURI()))) {
            result = new QName("", qname.getLocalPart());
        }
        return result;
    }

    public static NVList nvListFromStreamables(ORB orb, CorbaStreamable[] streamables) {
        NVList list = null;
        if (streamables != null && streamables.length > 0) {
            list = orb.create_list(streamables.length);
            for (int i = 0; i < streamables.length; ++i) {
                Any value = orb.create_any();
                value.insert_Streamable(streamables[i]);
                list.add_value(streamables[i].getName(), value, streamables[i].getMode());
            }
        } else {
            list = orb.create_list(0);
        }
        return list; 
    }
             

    static {
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_BOOLEAN, TCKind.from_int(TCKind._tk_boolean));
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_CHAR, TCKind.from_int(TCKind._tk_char));
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_WCHAR, TCKind.from_int(TCKind._tk_wchar));
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_OCTET, TCKind.from_int(TCKind._tk_octet));
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_USHORT, TCKind.from_int(TCKind._tk_ushort));
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_SHORT, TCKind.from_int(TCKind._tk_short));
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_LONG, TCKind.from_int(TCKind._tk_long));
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_ULONG, TCKind.from_int(TCKind._tk_ulong));
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_LONGLONG, TCKind.from_int(TCKind._tk_longlong));
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_ULONGLONG, TCKind.from_int(TCKind._tk_ulonglong));
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_FLOAT, TCKind.from_int(TCKind._tk_float));
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_DOUBLE, TCKind.from_int(TCKind._tk_double));
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_STRING, TCKind.from_int(TCKind._tk_string));
        PRIMITIVE_TYPECODES.put(CorbaConstants.NT_CORBA_WSTRING, TCKind.from_int(TCKind._tk_wstring));
    }
}
