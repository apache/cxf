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

package org.apache.cxf.tools.corba.processors.wsdl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Part;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.wsdl.Abstractanonsequence;
import org.apache.cxf.binding.corba.wsdl.Abstractsequence;
import org.apache.cxf.binding.corba.wsdl.CaseType;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaType;
import org.apache.cxf.binding.corba.wsdl.Enum;
import org.apache.cxf.binding.corba.wsdl.Enumerator;
import org.apache.cxf.binding.corba.wsdl.MemberType;
import org.apache.cxf.binding.corba.wsdl.Struct;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.cxf.binding.corba.wsdl.Union;
import org.apache.cxf.binding.corba.wsdl.Unionbranch;
import org.apache.cxf.binding.corba.wsdl.W3CConstants;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.corba.common.CorbaPrimitiveMap;
import org.apache.cxf.tools.corba.common.ReferenceConstants;
import org.apache.cxf.tools.corba.common.WSDLUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeOrGroupRef;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaMaxLengthFacet;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.XmlSchemaUse;
import org.apache.ws.commons.schema.utils.XmlSchemaObjectBase;

public class WSDLToCorbaHelper {

    public static final String REPO_STRING = "IDL:";
    public static final String IDL_VERSION = ":1.0";

    protected static final Logger LOG = LogUtils.getL7dLogger(WSDLToCorbaHelper.class);
    protected static final String[] DISCRIMINATORTYPES
        = new String[] {"long", "short", "boolean", "char"};
    protected static final Set<String> SUPPORTEDDISTYPES =
        new TreeSet<>(Arrays.asList(DISCRIMINATORTYPES));

    protected static final CorbaPrimitiveMap CORBAPRIMITIVEMAP = new CorbaPrimitiveMap();

    String idlNamespace;
    SchemaCollection xmlSchemaList;
    TypeMappingType typeMappingType;
    Definition def;
    Map<QName, CorbaType> recursionMap = new HashMap<>();

    public void setTypeMap(TypeMappingType map) {
        typeMappingType = map;
    }

    public void setIdlNamespace(String ns) {
        idlNamespace = ns;
    }

    public String getIdlNamespace() {
        return idlNamespace;
    }

    public void setXMLSchemaList(SchemaCollection list) {
        xmlSchemaList = list;
    }

    public SchemaCollection getXMLSchemaList() {
        return xmlSchemaList;
    }

    public void setWsdlDefinition(Definition defn) {
        def = defn;
    }

    public CorbaType convertSchemaToCorbaType(XmlSchemaType stype,
                                                  QName defaultName,
                                                  XmlSchemaType parent,
                                                  XmlSchemaAnnotation annotation,
                                                  boolean anonymous)
        throws Exception {
        CorbaType corbaTypeImpl = null;
        if (!isAddressingNamespace(stype.getQName())) {
            // need to determine if its a primitive type.
            if (stype instanceof XmlSchemaComplexType) {
                corbaTypeImpl = processComplexType((XmlSchemaComplexType)stype,
                                                              defaultName, annotation, anonymous);
            } else if (stype instanceof XmlSchemaSimpleType) {
                corbaTypeImpl = processSimpleType((XmlSchemaSimpleType)stype,
                                                  defaultName, anonymous);
            }  else if (xmlSchemaList.getElementByQName(stype.getQName()) != null) {
                XmlSchemaElement el = xmlSchemaList.getElementByQName(stype.getQName());
                //REVISIT, passing ns uri because of a bug in XmlSchema (Bug: WSCOMMONS-69)
                corbaTypeImpl = processElementType(el,
                                                   defaultName,
                                                   stype.getQName().getNamespaceURI());
            } else {
                throw new Exception("Couldn't convert schema " + stype.getQName() + " to corba type");
            }
        }

        if (corbaTypeImpl != null && !isDuplicate(corbaTypeImpl)) {
            typeMappingType.getStructOrExceptionOrUnion().add(corbaTypeImpl);
        }

        return corbaTypeImpl;
    }

    protected List<MemberType> processContainerAsMembers(XmlSchemaParticle particle,
                                             QName defaultName,
                                             QName schemaTypeName)
        throws Exception {
        List<MemberType> members = new ArrayList<>();

        Iterator<? extends XmlSchemaObjectBase> iterL = null;
        if (particle instanceof XmlSchemaSequence) {
            XmlSchemaSequence scontainer = (XmlSchemaSequence)particle;
            iterL = scontainer.getItems().iterator();
        } else if (particle instanceof XmlSchemaChoice) {
            XmlSchemaChoice scontainer = (XmlSchemaChoice)particle;
            iterL = scontainer.getItems().iterator();
        } else if (particle instanceof XmlSchemaAll) {
            XmlSchemaAll acontainer = (XmlSchemaAll)particle;
            iterL = acontainer.getItems().iterator();
        } else {
            LOG.warning("Unknown particle type " + particle.getClass().getName());
            iterL = new ArrayList<XmlSchemaObjectBase>().iterator();
        }

        while (iterL.hasNext()) {
            XmlSchemaParticle container = (XmlSchemaParticle)iterL.next();

            if (container instanceof XmlSchemaSequence) {
                XmlSchemaSequence sequence = (XmlSchemaSequence)container;
                CorbaType memberType =
                    processSequenceType(sequence, defaultName, schemaTypeName);
                QName typeName = memberType.getQName();
                if (memberType instanceof Struct
                    && !isDuplicate(memberType)) {
                    typeMappingType.getStructOrExceptionOrUnion().add(memberType);
                }
                MemberType member = new MemberType();
                member.setName(memberType.getName() + "_f");
                member.setIdltype(typeName);
                member.setAnonschematype(true);
                if (memberType.isSetQualified() && memberType.isQualified()) {
                    member.setQualified(true);
                }
                members.add(member);
            } else if (container instanceof XmlSchemaChoice) {
                XmlSchemaChoice choice = (XmlSchemaChoice)container;
                MemberType member = processChoiceMember(choice, defaultName,
                                                        schemaTypeName);
                member.setAnonschematype(true);
                members.add(member);
            } else if (container instanceof XmlSchemaAll) {
                XmlSchemaAll all = (XmlSchemaAll)container;
                MemberType member = processAllMember(all, defaultName,
                                                     schemaTypeName);
                member.setAnonschematype(true);
                members.add(member);
            } else if (container instanceof XmlSchemaElement) {
                XmlSchemaElement element = (XmlSchemaElement)container;

                CorbaType corbatype = processLocalElement(defaultName, element, schemaTypeName.getNamespaceURI());
                QName elName = element.getQName();
                if (elName == null) {
                    elName = element.getRef().getTargetQName();
                }
                if (corbatype != null) {
                    MemberType member;
                    String memberName = elName.getLocalPart();
                    member = new MemberType();
                    member.setName(memberName);
                    member.setIdltype(corbatype.getQName());
                    if (corbatype.isSetQualified() && corbatype.isQualified()) {
                        member.setQualified(true);
                    }
                    members.add(member);
                } else {
                    LOG.log(Level.WARNING, "Unsupported Element Found in CORBA Binding Generation:"
                            + elName);
                }
            }
        }
        return members;
    }

    private MemberType processChoiceMember(XmlSchemaChoice choice, QName defaultName,
        QName schemaTypeName) throws Exception {

        CorbaType corbatype = processChoice(choice, defaultName, schemaTypeName);
        MemberType member = new MemberType();
        member.setName(corbatype.getQName().getLocalPart());
        member.setIdltype(corbatype.getQName());
        if (corbatype.isSetQualified() && corbatype.isQualified()) {
            member.setQualified(true);
        }
        return member;
    }

    private MemberType processAllMember(XmlSchemaAll all, QName defaultName,
        QName schemaTypeName) throws Exception {

        CorbaType corbatype = processAllType(all, defaultName, schemaTypeName);
        MemberType member = new MemberType();
        member.setName(corbatype.getQName().getLocalPart());
        member.setIdltype(corbatype.getQName());
        if (corbatype.isSetQualified() && corbatype.isQualified()) {
            member.setQualified(true);
        }
        return member;
    }


    private CorbaType processChoice(XmlSchemaChoice choice,
                                        QName defaultName,
                                        QName schemaTypeName)
        throws Exception {
        QName choicename = null;

        if (schemaTypeName == null) {
            choicename = createQNameCorbaNamespace(defaultName.getLocalPart());
        } else {
            choicename = createQNameCorbaNamespace(schemaTypeName.getLocalPart());
        }
        choicename = checkPrefix(choicename);

        CorbaType corbatype = createUnion(choicename, choice, defaultName, schemaTypeName);
        String repoId = REPO_STRING + corbatype.getQName().getLocalPart().replace('.', '/')
            + IDL_VERSION;
        ((Union)corbatype).setRepositoryID(repoId);

        if (choice.getMaxOccurs() != 1 || choice.getMinOccurs() != 1) {
            QName name = createQNameTargetNamespace(corbatype.getQName().getLocalPart() + "Array");
            CorbaType arrayType =
                createArray(name, corbatype.getQName(), corbatype.getQName(),
                            choice.getMaxOccurs(), choice.getMinOccurs(), false);

            if (arrayType != null
                && !isDuplicate(arrayType)) {
                typeMappingType.getStructOrExceptionOrUnion().add(arrayType);
            }
        }
        return corbatype;
    }

    private CorbaType processLocalElement(QName containingTypeName,
                                              XmlSchemaElement element, String uri) throws Exception {
        CorbaType membertype = new CorbaType();

        XmlSchemaType schemaType = element.getSchemaType();
        QName schemaName = element.getQName();
        if (schemaType == null) {
            if (element.getRef().getTarget() != null) {
                schemaType = findSchemaType(element.getRef().getTarget().getSchemaTypeName());
                schemaName = element.getRef().getTargetQName();
            } else {
                schemaType = findSchemaType(element.getSchemaTypeName());
            }
        }
        if (schemaName.getNamespaceURI().isEmpty()) {
            schemaName = new QName(uri, schemaName.getLocalPart());
        }
        QName elemName = schemaName;
        boolean elementQualified = getElementQualification(element, uri);
        if (!elementQualified) {
            elemName = new QName("", elemName.getLocalPart());
        }

        QName memName = null;
        if (element.isNillable()) {
            CorbaType elemtype = convertSchemaToCorbaType(schemaType, elemName,
                                                              schemaType, null, true);
            QName name = createQNameTargetNamespace(elemtype.getQName().getLocalPart() + "_nil");
            QName elName = checkPrefix(elemName);
            if (elName ==  null) {
                elName = createQNameTargetNamespace(elemName.getLocalPart());
            }
            CorbaType memtype = createNillableUnion(elName,
                                          name,
                                          elemtype.getQName(),
                                          elementQualified);
            memName = createQNameCorbaNamespace(memtype.getQName().getLocalPart());

            if (!isDuplicate(memtype)) {
                typeMappingType.getStructOrExceptionOrUnion().add(memtype);
            }
            membertype.setQName(memName);
            membertype.setName(memtype.getName());
            membertype.setType(memtype.getType());
        } else if (schemaType != null) {
            XmlSchemaType st = schemaType;
            boolean anonymous = WSDLTypes.isAnonymous(st.getName());
            QName typeName = null;
            if (anonymous) {
                typeName = new QName(elemName.getNamespaceURI(),
                                     containingTypeName.getLocalPart() + "." + elemName.getLocalPart());
            } else {
                typeName = st.getQName();
            }
            membertype = convertSchemaToCorbaType(st, typeName, st, null, anonymous);
        } else if (element.getSchemaTypeName() != null) {
            QName name = checkPrefix(element.getSchemaTypeName());
            membertype = getLocalType(name);
        }
        if (membertype == null) {
            return null;
        }

        if (element.getMaxOccurs() != 1 || element.getMinOccurs() != 1) {
            QName name = createQNameCorbaNamespace(getModulePrefix(membertype)
                                                    + elemName.getLocalPart() + "Array");
            CorbaType arraytype = null;
            if (memName != null) {
                arraytype = createArray(name, /*schemaName*/name, memName, elemName,
                                        element.getMaxOccurs(), element.getMinOccurs(), false);
            } else {
                arraytype = createArray(name, /*schemaName*/name, membertype.getQName(), elemName,
                                        element.getMaxOccurs(), element.getMinOccurs(), false);
            }

            if (arraytype != null) {
                if (arraytype instanceof Abstractsequence) {
                    ((Abstractsequence)arraytype).setWrapped(false);
                }
                if (arraytype instanceof Abstractanonsequence) {
                    ((Abstractanonsequence)arraytype).setWrapped(false);
                }
                // we don't change a type which is already added to typeMappingType.getStructOrExceptionOrUnion()!
//                membertype.setName(arraytype.getName());
//                membertype.setQName(arraytype.getQName());
//                membertype.setType(arraytype.getType());

                if (!isDuplicate(arraytype)) {
                    typeMappingType.getStructOrExceptionOrUnion().add(arraytype);
                }
                // the local element with maxOccurs != 1 or minOccurs != 1 becomes the just created array
                membertype = arraytype;
            }
        }
        membertype.setQualified(elementQualified);
        return membertype;
    }


    public XmlSchemaType getSchemaType(QName name) throws Exception {
        XmlSchemaType type = null;
        for (XmlSchema xmlSchema : xmlSchemaList.getXmlSchemas()) {
            String nspace = name.getNamespaceURI();
            if (nspace == null) {
                nspace = xmlSchema.getTargetNamespace();
            }
            //QName tname = createQName(nspace, name.getLocalPart(), "xsd");
            QName tname = createQName(nspace, name.getLocalPart(), "");
            type = findSchemaType(tname);
            if (type != null) {
                break;
            }
        }
        return type;
    }

    private String getModulePrefix(CorbaType type) {
        String name = type.getQName().getLocalPart();
        int dotPos = name.lastIndexOf('.');
        return dotPos == -1 ? "" : name.substring(0, dotPos + 1);
    }


    protected CorbaType processSequenceType(XmlSchemaSequence seq,
                                                QName defaultName, QName schemaTypeName)
        throws Exception {
        CorbaType type = null;
        QName seqName = null;
        if (schemaTypeName == null) {
            seqName = createQNameCorbaNamespace(defaultName.getLocalPart() + "SequenceStruct");
        } else {
            seqName = createQNameCorbaNamespace(schemaTypeName.getLocalPart() + "SequenceStruct");
        }

        schemaTypeName = checkPrefix(schemaTypeName);
        Struct struct = new Struct();
        struct.setName(seqName.getLocalPart());
        struct.setQName(seqName);
        struct.setRepositoryID(REPO_STRING + seqName.getLocalPart().replace('.', '/') + IDL_VERSION);
        struct.setType(schemaTypeName);

        List<MemberType> members = processContainerAsMembers(seq, defaultName, schemaTypeName);
        for (MemberType memberType : members) {
            struct.getMember().add(memberType);
        }

        type = struct;

        if (seq.getMaxOccurs() != 1 || seq.getMinOccurs() != 1) {
            QName name = createQNameTargetNamespace(type.getQName().getLocalPart() + "Array");
            CorbaType atype = createArray(name, type.getQName(), type.getQName(),
                                              seq.getMaxOccurs(), seq.getMinOccurs(), false);

            if (atype != null
                && !isDuplicate(atype)) {
                typeMappingType.getStructOrExceptionOrUnion().add(atype);
            }
        }

        if (struct.getMember().isEmpty()) {
            String msgStr = "Cannot create CORBA Struct" + struct.getName()
                            + "from container with no members";
            org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(
                                      msgStr, LOG);
            throw new Exception(msg.toString());
        }

        return type;
    }


    protected CorbaType processAllType(XmlSchemaAll seq, QName defaultName,
                                           QName schematypeName) throws Exception {
        QName allName = null;
        Struct type = null;

        if (schematypeName == null) {
            allName = createQNameCorbaNamespace(defaultName.getLocalPart() + "AllStruct");
        } else {
            allName = createQNameCorbaNamespace(schematypeName.getLocalPart() + "AllStruct");
        }

        type = new Struct();
        type.setName(allName.getLocalPart());
        type.setQName(allName);
        type.setType(schematypeName);

        List<MemberType> members = processContainerAsMembers(seq, defaultName, schematypeName);
        for (MemberType memberType : members) {
            type.getMember().add(memberType);
        }

        String repoId = REPO_STRING + type.getQName().getLocalPart().replace('.', '/') + IDL_VERSION;
        type.setRepositoryID(repoId);
        return type;
    }

    private CorbaType processPrimitiveType(QName typeName) {
        QName qName = createQNameXmlSchemaNamespace(typeName.getLocalPart());
        CorbaType corbatype = (CorbaType)CORBAPRIMITIVEMAP.get(qName);
        if (corbatype == null) {
            //REVISIT, bravi, not an ideal way to add the fixed & octet type to the typemap.
            CorbaType type = null;
            if (typeName.equals(W3CConstants.NT_SCHEMA_DECIMAL)) {
                QName name = new QName(idlNamespace, "fixed_1");
                type = WSDLTypes.getFixedCorbaType(name, typeName, 31, 6);
                corbatype = WSDLTypes.getFixedCorbaType(name, typeName, 31, 6);
            } else if (typeName.equals(W3CConstants.NT_SCHEMA_BASE64)
                       || typeName.equals(W3CConstants.NT_SCHEMA_HBIN)) {
                QName name = new QName(idlNamespace, typeName.getLocalPart() + "Seq");
                type = WSDLTypes.getOctetCorbaType(name, typeName, 0);
                corbatype = WSDLTypes.getOctetCorbaType(name, typeName, 0);
            }
            if (type != null
                && !isDuplicate(type)) {
                typeMappingType.getStructOrExceptionOrUnion().add(type);
            }
        }
        return corbatype;
    }

    protected List<MemberType> processAttributesAsMembers(List<XmlSchemaAttributeOrGroupRef> list,
                                                          String uri) throws Exception {
        QName memName = null;
        List <MemberType>members = new ArrayList<>();

        for (XmlSchemaAttributeOrGroupRef aog : list) {
            if (!(aog instanceof XmlSchemaAttribute)) {
                LOG.warning(aog.getClass() + " not supported in CORBA binding.  Skipping.");
                continue;
            }
            XmlSchemaAttribute attribute = (XmlSchemaAttribute) aog;
            QName attrName = attribute.getQName();
            if (attrName.getNamespaceURI().isEmpty()) {
                attrName = new QName(uri, attrName.getLocalPart());
            }
            CorbaType membertype = null;
            boolean attrQualified = getAttributeQualification(attribute, uri);
            if (attribute.getUse() == XmlSchemaUse.NONE
                || attribute.getUse() == XmlSchemaUse.OPTIONAL) {
                CorbaType attType = null;
                if (attribute.getSchemaType() != null) {
                    // REVISIT, edell bug in XmlSchema 1.2.
                    // https://issues.apache.org/jira/browse/WSCOMMONS-208
                    attType = convertSchemaToCorbaType(attribute.getSchemaType(),
                                                       checkPrefix(attrName),
                                                       attribute.getSchemaType(),
                                                       null, true);
                    if (attType != null) {
                        QName typeName = attType.getQName();
                        if (!isDuplicate(attType)) {
                            typeMappingType.getStructOrExceptionOrUnion().add(attType);
                        }
                        QName name = createQNameTargetNamespace(typeName.getLocalPart() + "_nil");
                        membertype = createNillableUnion(name,
                                                         checkPrefix(attrName),
                                                         createQNameCorbaNamespace(typeName.getLocalPart()),
                                                         attrQualified);
                    }
                } else {
                    attType = processPrimitiveType(attribute.getSchemaTypeName());
                    //REVISIT, bravi, attType is null for the wsaddr type
                    //{http://www.w3.org/2005/08/addressing}RelationshipTypeOpenEnum
                    if (attType != null) {
                        QName name = createQNameTargetNamespace(attType.getQName().getLocalPart() + "_nil");
                        //REVISIT, Edell - bug in Xmlschema 1.2
                        // https://issues.apache.org/jira/browse/WSCOMMONS-208
                        membertype = createNillableUnion(name,
                                                         checkPrefix(attrName),
                                                         attType.getQName(),
                                                         attrQualified);
                    }

                }
                if (membertype != null) {
                    memName = createQNameCorbaNamespace(membertype.getQName().getLocalPart());
                    if (!isDuplicate(membertype)) {
                        typeMappingType.getStructOrExceptionOrUnion().add(membertype);
                    }
                }
            } else {
                if (attribute.getSchemaType() != null) {
                    membertype = convertSchemaToCorbaType(attribute.getSchemaType(), attrName,
                                                          attribute.getSchemaType(), null, false);
                } else {
                    membertype = processPrimitiveType(attribute.getSchemaTypeName());
                }
            }

            if (membertype != null) {
                MemberType member;
                String memberName = attrName.getLocalPart();

                member = new MemberType();
                member.setName(memberName);
                if (memName != null) {
                    member.setIdltype(memName);
                } else {
                    member.setIdltype(membertype.getQName());
                }
                if (attrQualified) {
                    member.setQualified(true);
                }
                members.add(member);
            } else {
                String msg = "Unsupported Attribute Found in CORBA Binding Generation:"
                    + attrName;
                LOG.log(Level.WARNING, msg);
            }
        }

        return members;
    }

    private CorbaType processElementType(XmlSchemaElement stype, QName defaultName, String uri)
        throws Exception {

        String name = null;
        QName schemaTypeName = null;
        XmlSchemaType schemaType = stype.getSchemaType();

        if (stype.getQName() == null) {
            if (stype.getRef().getTargetQName() == null) {
                schemaTypeName = defaultName;
            } else {
                name = stype.getRef().getTargetQName().getLocalPart();
                schemaType = findSchemaType(stype.getRef().getTargetQName());
            }
        } else {
            name = stype.getQName().getLocalPart();
        }
        if (schemaTypeName == null) {
            schemaTypeName = createQNameTargetNamespace(name);
        }
        CorbaType result = convertSchemaToCorbaType(schemaType, schemaTypeName,
                                                        schemaType, null, false);

        result.setQualified(getElementQualification(stype, uri));
        return result;
    }

    private CorbaType processSimpleType(XmlSchemaSimpleType stype, QName defaultName,
                                            boolean anonymous)
        throws Exception {

        CorbaType corbaTypeImpl = null;
        QName name;
        QName schematypeName = null;

        if (stype.getQName() == null) {
            schematypeName = defaultName;
            name = createQNameTargetNamespace(defaultName.getLocalPart() + "Type");
        } else {
            schematypeName = checkPrefix(stype.getQName());
            if (schematypeName == null) {
                schematypeName = stype.getQName();
            }
            name = createQNameCorbaNamespace(schematypeName.getLocalPart());
        }

        if (stype.getParent().getTargetNamespace().equals(W3CConstants.NU_SCHEMA_XSD)) {
            // built in types
            QName stypeName = createQNameXmlSchemaNamespace(stype.getName());
            corbaTypeImpl = getLocalType(stypeName);
        } else if (stype.getContent() instanceof XmlSchemaSimpleTypeRestriction) {
            corbaTypeImpl = processSimpleRestrictionType(stype, name, schematypeName, anonymous);
        } else if (stype.getContent() instanceof XmlSchemaSimpleTypeList) {
            XmlSchemaSimpleTypeList ltype = (XmlSchemaSimpleTypeList)stype.getContent();
            CorbaType itemType = null;
            if (ltype.getItemType() != null) {
                itemType = convertSchemaToCorbaType(ltype.getItemType(), name, stype, null, false);
                if (itemType != null) {
                    return WSDLTypes.mapToSequence(name, checkPrefix(schematypeName),
                                                   itemType.getQName(), null, 0, false);
                }
                return itemType;
            }
            QName ltypeName = createQNameXmlSchemaNamespace(ltype.getItemTypeName().getLocalPart());
            itemType = processPrimitiveType(ltypeName);
            if (itemType != null) {
                return WSDLTypes.mapToSequence(name, checkPrefix(schematypeName),
                                           itemType.getQName(), null, 0, false);
            }
            // if the type of the simpleContent is a list with another simple type.
            XmlSchemaType base = getSchemaType(ltype.getItemTypeName());
            itemType = convertSchemaToCorbaType(base, base.getQName(), base, null, false);
            if (itemType != null) {
                return WSDLTypes.mapToSequence(name, checkPrefix(schematypeName),
                                               itemType.getQName(), null, 0, false);
            }
        } else if (stype.getContent() == null) {
            // elements primitive type
            QName stypeName = createQNameXmlSchemaNamespace(stype.getName());
            corbaTypeImpl = getLocalType(stypeName);
        } else {
            System.out.println("SimpleType Union Not Supported in CORBA Binding");
        }
        return corbaTypeImpl;

    }

    private CorbaType processSimpleRestrictionType(XmlSchemaSimpleType stype,
                                                       QName name, QName schematypeName,
                                                       boolean anonymous)
        throws Exception {
        CorbaType corbaTypeImpl = null;

        // checks if enumeration
        XmlSchemaSimpleTypeRestriction restrictionType = (XmlSchemaSimpleTypeRestriction)stype
            .getContent();

        QName baseName = checkPrefix(restrictionType.getBaseTypeName());

        String maxLength = null;
        String length = null;

        for (XmlSchemaFacet val : restrictionType.getFacets()) {
            if (val instanceof XmlSchemaMaxLengthFacet) {
                maxLength = val.getValue().toString();
            }
            if (val instanceof XmlSchemaLengthFacet) {
                length = val.getValue().toString();
            }
        }

        if (isEnumeration(restrictionType)) {
            corbaTypeImpl = createCorbaEnum(restrictionType, name, schematypeName);
        } else {
            if (restrictionType.getBaseType() != null) {
                corbaTypeImpl = convertSchemaToCorbaType(restrictionType.getBaseType(), schematypeName,
                                                         stype, null, false);
            } else {
                corbaTypeImpl = processPrimitiveType(baseName);
                if (corbaTypeImpl == null) {
                    XmlSchemaType schematype = findSchemaType(baseName);
                    corbaTypeImpl = convertSchemaToCorbaType(schematype, schematypeName,
                                                             schematype, null, false);
                }
            }

            if (corbaTypeImpl != null) {
                if (corbaTypeImpl.getType().equals(W3CConstants.NT_SCHEMA_STRING)
                    || (baseName.equals(W3CConstants.NT_SCHEMA_STRING))) {
                    corbaTypeImpl =
                        WSDLTypes.processStringType(corbaTypeImpl, name, maxLength, length);
                } else if (corbaTypeImpl.getType().equals(W3CConstants.NT_SCHEMA_DECIMAL)
                    || (baseName.equals(W3CConstants.NT_SCHEMA_DECIMAL))) {
                    corbaTypeImpl = WSDLTypes.processDecimalType(restrictionType, name,
                                                             corbaTypeImpl, anonymous);
                } else if ((corbaTypeImpl.getType().equals(W3CConstants.NT_SCHEMA_BASE64))
                    || (baseName.equals(W3CConstants.NT_SCHEMA_BASE64))
                    || (corbaTypeImpl.getType().equals(W3CConstants.NT_SCHEMA_HBIN))) {
                    corbaTypeImpl = WSDLTypes.processBase64Type(corbaTypeImpl,
                                                                name, maxLength, length);
                }
            }
        }

        return corbaTypeImpl;
    }

    private CorbaType getLocalType(QName qname) {
        return processPrimitiveType(qname);
    }

    private Enum createCorbaEnum(XmlSchemaSimpleTypeRestriction restrictionType, QName name,
                                 QName schematypeName) {
        Enum corbaEnum = new Enum();
        corbaEnum.setType(schematypeName);
        corbaEnum.setName(name.getLocalPart());
        corbaEnum.setQName(name);

        corbaEnum.setRepositoryID(REPO_STRING + name.getLocalPart().replace('.', '/') + IDL_VERSION);

        for (XmlSchemaFacet f : restrictionType.getFacets()) {
            XmlSchemaEnumerationFacet val = (XmlSchemaEnumerationFacet)f;
            Enumerator enumerator = new Enumerator();
            enumerator.setValue(val.getValue().toString());
            corbaEnum.getEnumerator().add(enumerator);
        }
        return corbaEnum;
    }

    private boolean isEnumeration(XmlSchemaSimpleTypeRestriction restriction) {

        if ((restriction == null) || (restriction.getFacets().isEmpty())
            || (restriction.getBaseTypeName() == null)) {
            return false;
        }


        for (XmlSchemaFacet facet : restriction.getFacets()) {
            if (facet instanceof XmlSchemaEnumerationFacet) {
                return true;
            }
        }
        return false;
    }


    protected XmlSchemaType lookUpType(Part part) {
        XmlSchemaType schemaType = null;
        for (XmlSchema xmlSchema : xmlSchemaList.getXmlSchemas()) {
            if (part.getElementName() != null) {
                XmlSchemaElement schemaElement = xmlSchema.getElementByName(part.getElementName());
                if (schemaElement != null) {
                    schemaType = schemaElement.getSchemaType();
                }
            } else {
                if (part.getTypeName() != null) {
                    schemaType = xmlSchema.getTypeByName(part.getTypeName());
                }
            }
            if (schemaType != null) {
                return schemaType;
            }
        }

        return schemaType;
    }

    private XmlSchemaType findSchemaType(QName typeName) {
        for (XmlSchema xmlSchema : xmlSchemaList.getXmlSchemas()) {
            // if the schema includes other schemas need to search there.
            XmlSchemaType schemaType = findTypeInSchema(xmlSchema, typeName);
            if (schemaType != null) {
                return schemaType;
            }
        }
        return null;
    }

    private XmlSchemaType findTypeInSchema(XmlSchema xmlSchema, QName typeName) {
        XmlSchemaType schemaType = null;

        if (xmlSchema.getElementByName(typeName) != null) {
            XmlSchemaElement schemaElement = xmlSchema.getElementByName(typeName);
            schemaType = schemaElement.getSchemaType();
        } else if (xmlSchema.getTypeByName(typeName) != null) {
            schemaType = xmlSchema.getTypeByName(typeName);
        }
        if (schemaType != null) {
            return schemaType;
        }
        for (XmlSchemaExternal extSchema : xmlSchema.getExternals()) {
            if (!(extSchema instanceof XmlSchemaImport)) {
                schemaType = findTypeInSchema(extSchema.getSchema(), typeName);
                if (schemaType != null) {
                    return schemaType;
                }
            }
        }

        return null;
    }

    protected boolean isSchemaTypeException(XmlSchemaType stype) {
        boolean exception = false;
        XmlSchemaComplexType complex = null;

        if (stype instanceof XmlSchemaComplexType) {
            complex = (XmlSchemaComplexType)stype;

            if (!isLiteralArray(complex)
                && !WSDLTypes.isOMGUnion(complex)
                && !WSDLTypes.isUnion(complex)) {
                exception = true;
            }
        }
        return exception;
    }


    public boolean isLiteralArray(XmlSchemaComplexType type) {
        boolean array = false;

        if ((type.getAttributes().isEmpty())
            && (type.getParticle() instanceof XmlSchemaSequence)) {
            XmlSchemaSequence stype = (XmlSchemaSequence)type.getParticle();

            if ((stype.getItems().size() == 1)
                && (stype.getItems().get(0) instanceof XmlSchemaElement)) {
                XmlSchemaElement el = (XmlSchemaElement)stype.getItems().get(0);
                if (el.getMaxOccurs() != 1) {
                    // it's a literal array
                    array = true;
                }
                if (el.getMaxOccurs() == 1
                    && el.getMinOccurs() == 1
                    && type.getName() != null
                    &&  WSDLTypes.isAnonymous(type.getName())) {
                    array = true;
                }
            }
        }
        return array;
    }

    /**
     * Create a CORBA Array or Sequence based on min and max Occurs If minOccurs ==
     * maxOccurs == 1 then log warning and return null. Else if minOccurs is
     * equal to maxOccurs then create an Array. Else create a Sequence
     */
    protected CorbaType createArray(QName name, QName schematypeName, QName arrayType,
                                        Long maxOccurs, Long minOccurs, boolean anonymous) {
        return createArray(name, schematypeName, arrayType, null, maxOccurs, minOccurs, anonymous);
    }
    /**
     * Create a CORBA Array or Sequence based on min and max Occurs If minOccurs ==
     * maxOccurs == 1 then log warning and return null. Else if minOccurs is
     * equal to maxOccurs then create an Array. Else create a Sequence
     */
    protected CorbaType createArray(QName name, QName schematypeName, QName arrayType, QName elName,
                                        Long maxOccurs, Long minOccurs, boolean anonymous) {

        int max = maxOccurs.intValue();
        if (max == -1) {
            return WSDLTypes.mapToSequence(name, schematypeName, arrayType, elName, 0, anonymous);
        }

        int min = minOccurs.intValue();

        if (min == max) {
            if (max == 1) {
                if (!anonymous) {
                    String msg = "Couldn't Map to Array:" + name + ":minOccurs="
                        + minOccurs + ":maxOccurs=" + maxOccurs;
                    LOG.log(Level.WARNING, msg);
                    return null;
                }
                return WSDLTypes.mapToArray(name, checkPrefix(schematypeName), arrayType,
                                            elName, max, anonymous);
            }
            return WSDLTypes.mapToArray(name, checkPrefix(schematypeName), arrayType,
                                        elName, max, anonymous);
        }
        return WSDLTypes.mapToSequence(name, checkPrefix(schematypeName), arrayType,
                                       elName, max, anonymous);
    }


    private CorbaType processComplexType(XmlSchemaComplexType complex, QName defaultName,
                                             XmlSchemaAnnotation annotation,
                                             boolean anonymous) throws Exception {
        CorbaType corbatype = null;
        if (isLiteralArray(complex)) {
            corbatype = processLiteralArray(complex, defaultName, anonymous);
        } else if (WSDLTypes.isOMGUnion(complex)) {
            corbatype = processOMGUnion(complex, defaultName);
        } else if (WSDLTypes.isUnion(complex)) {
            corbatype = processRegularUnion(complex, defaultName);
        } else if (complex.getQName() != null && isIDLObjectType(complex.getQName())) {
            // process it.
            corbatype = WSDLTypes.processObject(def, complex, annotation, checkPrefix(complex.getQName()),
                                                defaultName, idlNamespace);
        } else {
            // Deal the ComplexType as Struct
            corbatype = processStruct(complex, defaultName);
        }
        return corbatype;
    }


    private CorbaType processStruct(XmlSchemaComplexType complex, QName defaultName)
        throws Exception {
        QName name;
        Struct corbaStruct = null;
        QName schematypeName = checkPrefix(complex.getQName());
        if (schematypeName == null) {
            schematypeName = createQNameTargetNamespace(defaultName.getLocalPart());
            if (defaultName.getNamespaceURI().isEmpty()) {
                schematypeName = checkPrefix(schematypeName);
            } else {
                schematypeName = checkPrefix(defaultName);
            }
            name = checkPrefix(createQNameCorbaNamespace(defaultName.getLocalPart()));
        } else {
            name = checkPrefix(createQNameCorbaNamespace(schematypeName.getLocalPart()));
        }

        corbaStruct = (Struct)recursionMap.get(name);
        if (corbaStruct != null) {
            return corbaStruct;
        }

        corbaStruct = new Struct();
        corbaStruct.setName(name.getLocalPart());
        corbaStruct.setQName(name);
        String repoId = REPO_STRING + name.getLocalPart().replace('.', '/') + IDL_VERSION;
        corbaStruct.setRepositoryID(repoId);
        corbaStruct.setType(schematypeName);


        recursionMap.put(name, corbaStruct);

        if (complex.getContentModel() instanceof XmlSchemaSimpleContent) {
            corbaStruct = processSimpleContentStruct((XmlSchemaSimpleContent)complex.getContentModel(),
                                                     defaultName, corbaStruct, schematypeName);
        } else if (complex.getContentModel() instanceof XmlSchemaComplexContent) {
            corbaStruct = processComplexContentStruct((XmlSchemaComplexContent)complex.getContentModel(),
                                                      defaultName, corbaStruct, schematypeName);
        }

        // Process attributes at ComplexType level
        if (!complex.getAttributes().isEmpty()) {
            String uri;
            if (schematypeName != null) {
                uri = schematypeName.getNamespaceURI();
            } else {
                uri = defaultName.getNamespaceURI();
            }
            List<MemberType> attlist2 = processAttributesAsMembers(complex.getAttributes(), uri);
            for (int i = 0; i < attlist2.size(); i++) {
                MemberType member = attlist2.get(i);
                corbaStruct.getMember().add(member);
            }
        }

        if (complex.getParticle() != null) {
            List<MemberType> members = processContainerAsMembers(complex.getParticle(),
                                                                 defaultName, schematypeName);

            for (MemberType memberType : members) {
                corbaStruct.getMember().add(memberType);
            }
        }

        recursionMap.remove(name);

        return corbaStruct;
    }

    protected Struct processSimpleContentStruct(XmlSchemaSimpleContent simpleContent,
                                                QName defaultName, Struct corbaStruct, QName schematypeName)
        throws Exception {
        XmlSchemaType base = null;
        List<MemberType> attrMembers = null;
        CorbaType basetype = null;

        String uri;
        if (schematypeName != null) {
            uri = schematypeName.getNamespaceURI();
        } else {
            uri = defaultName.getNamespaceURI();
        }

        if (simpleContent.getContent() instanceof XmlSchemaSimpleContentExtension) {
            XmlSchemaSimpleContentExtension ext =
                (XmlSchemaSimpleContentExtension)simpleContent.getContent();

            if (ext.getBaseTypeName() != null) {
                basetype = processPrimitiveType(ext.getBaseTypeName());
            }

            if (basetype == null) {
                base = getSchemaType(ext.getBaseTypeName());
                basetype = convertSchemaToCorbaType(base, base.getQName(), base, null, false);
            }
            if (basetype == null) {
                return null;
            }
            // process ext types ????
            MemberType basemember = new MemberType();
            basemember.setName("_simpleTypeValue");
            QName baseTypeName = checkPrefix(basetype.getQName());
            basemember.setIdltype(baseTypeName);
            corbaStruct.getMember().add(basemember);
            if (!isDuplicate(basetype)) {
                typeMappingType.getStructOrExceptionOrUnion().add(basetype);
            }
            attrMembers = processAttributesAsMembers(ext.getAttributes(), uri);
        } else if (simpleContent.getContent() instanceof XmlSchemaSimpleContentRestriction) {
            XmlSchemaSimpleContentRestriction restrict
                = (XmlSchemaSimpleContentRestriction)simpleContent.getContent();

            base = restrict.getBaseType();

            if (restrict.getBaseTypeName() != null) {
                basetype = processPrimitiveType(restrict.getBaseTypeName());
            }

            if (basetype == null) {
                base = getSchemaType(restrict.getBaseTypeName());
                basetype = convertSchemaToCorbaType(base, base.getQName(), base, null, false);
            }

            MemberType basemember = new MemberType();
            basemember.setName("_simpleTypeValue");
            QName baseTypeName = checkPrefix(basetype.getQName());
            basemember.setIdltype(baseTypeName);
            corbaStruct.getMember().add(basemember);
            if (!isDuplicate(basetype)) {
                typeMappingType.getStructOrExceptionOrUnion().add(basetype);
            }
            attrMembers = processAttributesAsMembers(restrict.getAttributes(), uri);
        }

        //Deal with Attributes defined in Extension
        if (attrMembers != null) {
            for (int i = 0; i < attrMembers.size(); i++) {
                MemberType member = attrMembers.get(i);
                corbaStruct.getMember().add(member);
            }
        }

        return corbaStruct;
    }

    protected Struct processComplexContentStruct(XmlSchemaComplexContent complex, QName defaultName,
                                                 Struct corbaStruct, QName schematypeName)
        throws Exception {

        if (complex.getContent() instanceof XmlSchemaComplexContentExtension) {
            XmlSchemaComplexContentExtension extype
                = (XmlSchemaComplexContentExtension)complex.getContent();
            QName extName = extype.getBaseTypeName();
            corbaStruct = processComplexContentStructParticle(extype.getParticle(), defaultName, corbaStruct,
                                                         schematypeName, extName, extype.getAttributes());
        } else {
            if (complex.getContent() instanceof XmlSchemaComplexContentRestriction) {
                XmlSchemaComplexContentRestriction extype
                    = (XmlSchemaComplexContentRestriction)complex.getContent();
                QName extName = extype.getBaseTypeName();
                corbaStruct =
                    processComplexContentStructParticle(extype.getParticle(), defaultName,
                                                   corbaStruct, schematypeName,
                                                   extName, extype.getAttributes());
            }
        }
        return corbaStruct;
    }

    private Struct processComplexContentStructParticle(XmlSchemaParticle extype,
                                                  QName defaultName, Struct corbaStruct,
                                                  QName schematypeName, QName extName,
                                                  List<XmlSchemaAttributeOrGroupRef> list)
        throws Exception {

        String uri;
        if (schematypeName != null) {
            uri = schematypeName.getNamespaceURI();
        } else {
            uri = defaultName.getNamespaceURI();
        }

        // Add base as a member of this struct
        MemberType memberType = new MemberType();
        memberType.setName(extName.getLocalPart() + "_f");
        if ("anyType".equals(extName.getLocalPart())) {
            memberType.setIdltype(processPrimitiveType(extName).getQName());
        } else {
            memberType.setIdltype(createQNameCorbaNamespace(extName.getLocalPart()));
        }
        corbaStruct.getMember().add(memberType);

        // process attributes at complexContent level
        List<MemberType> attlist1 = processAttributesAsMembers(list, uri);
        for (int i = 0; i < attlist1.size(); i++) {
            MemberType member = attlist1.get(i);
            corbaStruct.getMember().add(member);
        }

        // Process members of Current Type
        if (extype instanceof XmlSchemaChoice) {
            XmlSchemaChoice choice = (XmlSchemaChoice)extype;
            MemberType choicemem = processComplexContentStructChoice(choice, schematypeName, defaultName);
            choicemem.setAnonschematype(true);
            corbaStruct.getMember().add(choicemem);
        } else if (extype instanceof  XmlSchemaSequence) {
            XmlSchemaSequence seq = (XmlSchemaSequence)extype;
            corbaStruct = processComplexContentStructSequence(corbaStruct, seq, defaultName, schematypeName);
        } else if (extype instanceof  XmlSchemaAll) {
            XmlSchemaAll all = (XmlSchemaAll)extype;
            corbaStruct = processComplexContentStructSchemaAll(corbaStruct, all,
                                                         defaultName, schematypeName);
        }
        return corbaStruct;
    }

    private Struct processComplexContentStructSequence(Struct corbaStruct, XmlSchemaSequence seq,
                                                 QName defaultName, QName schematypeName)
        throws Exception {

        CorbaType seqtype =
            processSequenceType(seq, defaultName, schematypeName);

        MemberType seqmem = new MemberType();
        seqmem.setName(seqtype.getQName().getLocalPart() + "_f");
        QName type = createQNameCorbaNamespace(seqtype.getQName().getLocalPart());
        seqmem.setIdltype(type);
        seqmem.setAnonschematype(true);
        if (seqtype.isSetQualified() && seqtype.isQualified()) {
            seqmem.setQualified(true);
        }
        corbaStruct.getMember().add(seqmem);
        if (!isDuplicate(seqtype)) {
            typeMappingType.getStructOrExceptionOrUnion().add(seqtype);
        }

        return corbaStruct;
    }

    private Struct processComplexContentStructSchemaAll(Struct corbaStruct, XmlSchemaAll all,
                                                  QName defaultName, QName schematypeName)
        throws Exception {

        CorbaType alltype = processAllType(all, defaultName, schematypeName);
        MemberType allmem = new MemberType();
        allmem.setName(alltype.getQName().getLocalPart() + "_f");
        allmem.setIdltype(alltype.getQName());
        allmem.setAnonschematype(true);
        if (alltype.isSetQualified() && alltype.isQualified()) {
            allmem.setQualified(true);
        }
        corbaStruct.getMember().add(allmem);
        if (!isDuplicate(alltype)) {
            typeMappingType.getStructOrExceptionOrUnion().add(alltype);
        }

        return corbaStruct;
    }


    protected MemberType processComplexContentStructChoice(XmlSchemaChoice choice,
                                                     QName schematypeName, QName defaultName)
        throws Exception {
        QName choicename = createQNameTargetNamespace(schematypeName.getLocalPart() + "ChoiceType");
        Union choiceunion = createUnion(choicename, choice,
                                        defaultName, schematypeName);

        MemberType choicemem = new MemberType();
        if (choiceunion != null) {
            String repoId = REPO_STRING + choiceunion.getQName().getLocalPart().replace('.', '/')
                + IDL_VERSION;
            choiceunion.setRepositoryID(repoId);

            choicemem.setName(choiceunion.getQName().getLocalPart() + "_f");
            choicemem.setIdltype(createQNameCorbaNamespace(choiceunion.getQName().getLocalPart()));

            if (!isDuplicate(choiceunion)) {
                typeMappingType.getStructOrExceptionOrUnion().add(choiceunion);
            }
        }

        return choicemem;
    }

    protected CorbaType createNillableUnion(QName name,
                                                QName schemaType,
                                                QName membertype,
                                                boolean isQualified) {

        Union nilUnion = new Union();
        nilUnion.setName(name.getLocalPart());
        nilUnion.setType(schemaType);
        nilUnion.setQName(name);
        nilUnion.setDiscriminator(CorbaConstants.NT_CORBA_BOOLEAN);
        String id = REPO_STRING + nilUnion.getQName().getLocalPart().replace('.', '/') + IDL_VERSION;
        nilUnion.setRepositoryID(id);

        Unionbranch branch = new Unionbranch();
        branch.setName("value");
        branch.setIdltype(membertype);
        branch.setDefault(false);
        if (isQualified) {
            branch.setQualified(true);
        }
        CaseType caseType = new CaseType();
        caseType.setLabel("TRUE");
        branch.getCase().add(caseType);
        nilUnion.getUnionbranch().add(branch);
        nilUnion.setNillable(true);
        return nilUnion;
    }

    private CorbaType processLiteralArray(XmlSchemaComplexType complex, QName defaultName,
                                              boolean anonymous) throws Exception {
        // NEED TO DO
        QName name;
        QName typeName = null;

        QName schematypeName = checkPrefix(complex.getQName());

        if (schematypeName == null) {
            schematypeName = defaultName;
            name = createQNameCorbaNamespace(defaultName.getLocalPart() + "Type");
            schematypeName = checkPrefix(schematypeName);
            name = checkPrefix(name);
        } else {
            name = createQNameCorbaNamespace(schematypeName.getLocalPart());
            name = checkPrefix(name);
        }

        CorbaType arrayType = null;
        XmlSchemaElement arrayEl = null;
        QName elName = null;
        if (complex.getParticle() instanceof XmlSchemaSequence) {
            XmlSchemaSequence seq = (XmlSchemaSequence)complex.getParticle();

            Iterator<XmlSchemaSequenceMember> iterator = seq.getItems().iterator();
            Iterator<XmlSchemaSequenceMember> iter = seq.getItems().iterator();
            while (iterator.hasNext()) {
                if (iter.next() instanceof XmlSchemaElement) {
                    arrayEl = (XmlSchemaElement)iterator.next();
                    elName = arrayEl.getQName();
                    XmlSchemaType atype = arrayEl.getSchemaType();
                    if (elName == null) {
                        elName = arrayEl.getRef().getTargetQName();
                        /*
                         * TODO: why are we looking up an element name with findSchemaType?
                         */
                        atype = findSchemaType(elName);
                    }
                    String uri = defaultName.getNamespaceURI();
                    if (complex.getQName() != null) {
                        uri = complex.getQName().getNamespaceURI();
                    }
                    if (elName.getNamespaceURI().isEmpty()) {
                        elName = new QName(uri, elName.getLocalPart());
                    }
                    QName arrayTypeName = elName;
                    if (anonymous) {
                        arrayTypeName = new QName(elName.getNamespaceURI(),
                                                  defaultName.getLocalPart() + "." + elName.getLocalPart());
                    }
                    arrayType = convertSchemaToCorbaType(atype, arrayTypeName, atype, null, true);
                    boolean isQualified = getElementQualification(arrayEl, uri);
                    if (isQualified) {
                        arrayType.setQualified(isQualified);
                    } else {
                        elName = new QName("", elName.getLocalPart());
                    }
                    typeName = arrayType.getQName();
                }
            }
        }

        Long maxOccurs = null;
        Long minOccurs = null;
        if (arrayEl != null) {
            if (arrayEl.isNillable()) {
                QName nilunionname = createQNameTargetNamespace(arrayType.getQName().getLocalPart() + "_nil");
                boolean isQualified = arrayType.isSetQualified() && arrayType.isQualified();
                arrayType = createNillableUnion(nilunionname,
                                            elName,
                                            arrayType.getQName(),
                                            isQualified);
                typeName = createQNameCorbaNamespace(arrayType.getQName().getLocalPart());
                if (arrayType != null
                    && !isDuplicate(arrayType)) {
                    typeMappingType.getStructOrExceptionOrUnion().add(arrayType);
                }
            }

            maxOccurs = arrayEl.getMaxOccurs();
            minOccurs = arrayEl.getMinOccurs();
        }

        return createArray(name, schematypeName, checkPrefix(typeName), elName,
                           maxOccurs, minOccurs, anonymous);
    }

    private CorbaType processOMGUnion(XmlSchemaComplexType complex, QName defaultName) throws Exception {
        QName name;
        Union corbaUnion = null;
        QName schematypeName = checkPrefix(complex.getQName());

        if (schematypeName == null) {
            schematypeName = defaultName;
            name = createQNameCorbaNamespace(defaultName.getLocalPart() + "Type");
        } else {
            name = createQNameCorbaNamespace(schematypeName.getLocalPart());
        }

        corbaUnion = new Union();
        corbaUnion.setName(name.getLocalPart());
        corbaUnion.setQName(name);
        String id = REPO_STRING + name.getLocalPart().replace('.', '/') + IDL_VERSION;
        corbaUnion.setRepositoryID(id);
        corbaUnion.setType(schematypeName);

        XmlSchemaSequence stype = (XmlSchemaSequence)complex.getParticle();
        Iterator<XmlSchemaSequenceMember> it = stype.getItems().iterator();
        XmlSchemaParticle st1 = (XmlSchemaParticle)it.next();
        XmlSchemaParticle st2 = (XmlSchemaParticle)it.next();
        XmlSchemaElement discEl = null;
        XmlSchemaChoice choice = null;

        if (st1 instanceof XmlSchemaElement) {
            discEl = (XmlSchemaElement)st1;
            choice = (XmlSchemaChoice)st2;
        } else {
            discEl = (XmlSchemaElement)st2;
            choice = (XmlSchemaChoice)st1;
        }

        CorbaType disctype = convertSchemaToCorbaType(discEl.getSchemaType(), discEl.getQName(), discEl
            .getSchemaType(), null, false);
        corbaUnion.setDiscriminator(disctype.getQName());

        List<MemberType> fields = processContainerAsMembers(choice, defaultName, schematypeName);

        List<String> caselist = new ArrayList<>();

        if (disctype instanceof Enum) {
            Enum corbaenum = (Enum)disctype;
            Iterator<Enumerator> iterator = corbaenum.getEnumerator().iterator();

            while (iterator.hasNext()) {
                Enumerator enumerator = iterator.next();
                caselist.add(enumerator.getValue());
            }
        } else if (SUPPORTEDDISTYPES.contains(disctype.getQName().getLocalPart())) {
            if ("long".equals(disctype.getQName().getLocalPart())
                || "short".equals(disctype.getQName().getLocalPart())) {
                for (int i = 0; i < fields.size(); i++) {
                    caselist.add(Integer.toString(i));
                }
            } else if ("char".equals(disctype.getQName().getLocalPart())) {
                for (int i = 0; i < fields.size(); i++) {
                    caselist.add(Integer.toString(i));
                }
            } else if ("boolean".equals(disctype.getQName().getLocalPart())) {
                if (fields.size() == 2) {
                    caselist.add("TRUE");
                    caselist.add("FALSE");
                } else if (fields.size() == 1) {
                    caselist.add("TRUE");
                } else {
                    String msg = "Discriminator Type doesnt match number of Choices in Union:" + name;
                    LOG.log(Level.WARNING, msg);
                }
            }
        }

        WSDLTypes.processUnionBranches(corbaUnion, fields, caselist);

        return corbaUnion;
    }


    private CorbaType processRegularUnion(XmlSchemaComplexType complex,
                                              QName defaultName) throws Exception {
        //NEED TO DO
        QName name = null;
        QName schematypeName = complex.getQName();

        if (schematypeName == null) {
            schematypeName = defaultName;
            name = createQNameCorbaNamespace(defaultName.getLocalPart() + "Type");
        } else {
            name = createQNameCorbaNamespace(schematypeName.getLocalPart());
        }

        return createUnion(name, (XmlSchemaChoice)complex.getParticle(), defaultName, schematypeName);
    }

    protected Union createUnion(QName name, XmlSchemaChoice choice, QName defaultName,
                                QName schematypeName)
        throws Exception {

        Union corbaUnion = null;
        if (recursionMap.get(name) instanceof Union) {
            corbaUnion = (Union)recursionMap.get(name);
            if (corbaUnion != null) {
                return corbaUnion;
            }
        }

        corbaUnion = new Union();
        corbaUnion.setName(name.getLocalPart());
        corbaUnion.setQName(name);
        corbaUnion.setType(schematypeName);
        String id = REPO_STRING + name.getLocalPart().replace('.', '/') + IDL_VERSION;
        corbaUnion.setRepositoryID(id);

        //Set Integer as Discriminator
        corbaUnion.setDiscriminator(CorbaConstants.NT_CORBA_LONG);

        // add to the list of possible recursive types
        recursionMap.put(name, corbaUnion);

        List<MemberType> fields = processContainerAsMembers(choice, defaultName, schematypeName);

        //Choose an Integer as a Discriminator
        List<String> caselist = new ArrayList<>();

        for (int i = 0; i < fields.size(); i++) {
            caselist.add(Integer.toString(i));
        }

        corbaUnion = WSDLTypes.processUnionBranches(corbaUnion, fields, caselist);

        recursionMap.remove(name);
        if (!isDuplicate(corbaUnion)) {
            typeMappingType.getStructOrExceptionOrUnion().add(corbaUnion);
        }
        return corbaUnion;
    }

    protected boolean isDuplicate(CorbaType corbaTypeImpl) {
        String corbaName = corbaTypeImpl.getName();
        QName corbaType = corbaTypeImpl.getType();

        QName primName = createQNameXmlSchemaNamespace(corbaName);
        if ((CorbaType)CORBAPRIMITIVEMAP.get(primName) != null) {
            return true;
        }
        if (!typeMappingType.getStructOrExceptionOrUnion().isEmpty()) {
            Iterator<CorbaType> i = typeMappingType.getStructOrExceptionOrUnion().iterator();
            while (i.hasNext()) {
                CorbaType type = i.next();
                if ((corbaName != null) && type.getType() != null && corbaType != null
                    && (corbaName.equals(type.getName()))
                    && (corbaType.getLocalPart().equals(type.getType().getLocalPart()))
                    && (corbaTypeImpl.getClass().getName().equals(type.getClass().getName()))) {
                    return true;
                }
            }
        }
        return false;
    }


    protected CorbaType isDuplicateException(CorbaType corbaTypeImpl) {
        CorbaType duplicate = null;
        String corbaName = corbaTypeImpl.getName();
        String corbaType = corbaTypeImpl.getType().getLocalPart();
        if (!typeMappingType.getStructOrExceptionOrUnion().isEmpty()) {
            Iterator<CorbaType> i = typeMappingType.getStructOrExceptionOrUnion().iterator();
            while (i.hasNext()) {
                CorbaType type = i.next();
                if (corbaName.equals(type.getName())
                    && corbaType.equals(type.getType().getLocalPart())
                    && type instanceof Struct) {
                    return type;
                }
            }
        }
        return duplicate;
    }

    protected QName checkPrefix(QName schematypeName) {
        QName name = schematypeName;
        if ((name != null) && (name.getPrefix() == null || name.getPrefix().isEmpty())) {
            if (StringUtils.isEmpty(name.getNamespaceURI())) {
                return name;
            }
            String prefix = def.getPrefix(name.getNamespaceURI());
            if (prefix == null) {
                prefix = xmlSchemaList.getSchemaByTargetNamespace(name.getNamespaceURI())
                    .getNamespaceContext().getPrefix(name.getNamespaceURI());
            }
            if (prefix != null) {
                return new QName(name.getNamespaceURI(),
                                 name.getLocalPart(),
                                 prefix);
            }
            return null;
        }

        return name;
    }

    public QName createQNameTargetNamespace(String name) {
        return new QName(def.getTargetNamespace(), name, def.getPrefix(def.getTargetNamespace()));
    }

    public QName createQNameCorbaNamespace(String name) {
        return new QName(getIdlNamespace(), name, def.getPrefix(getIdlNamespace()));
    }

    public QName createQName(String name, String namespaceName, String prefix) {
        return new QName(name, namespaceName, prefix);
    }

    public QName createQNameXmlSchemaNamespace(String name) {
        return new QName(W3CConstants.NU_SCHEMA_XSD, name, W3CConstants.NP_SCHEMA_XSD);
    }

    private boolean isIDLObjectType(QName typeName) {
        return typeName.equals(ReferenceConstants.WSADDRESSING_TYPE);
    }

    private boolean isAddressingNamespace(QName typeName) {
        return (typeName != null)
                && (!isIDLObjectType(typeName))
                && (typeName.getNamespaceURI().equals(ReferenceConstants.WSADDRESSING_NAMESPACE));
    }

    protected static boolean queryBinding(Definition definition, QName bqname) {
        Collection<Binding> bindings = CastUtils.cast(definition.getBindings().values());
        for (Binding binding : bindings) {
            if (binding.getQName().getLocalPart().equals(bqname.getLocalPart())) {
                return true;
            }
        }
        return false;
    }

    private boolean getElementQualification(XmlSchemaElement element, String uri) {
        QName schemaName = element.getQName();
        if (element.isRef()) {
            schemaName = element.getRef().getTargetQName();
        }

        if (schemaName.getNamespaceURI().isEmpty()) {
            schemaName = new QName(uri, schemaName.getLocalPart());
        }
        boolean qualified = false;
        if (element.getForm() == XmlSchemaForm.QUALIFIED) {
            qualified = true;
        } else {
            qualified = WSDLUtils.isElementFormQualified(xmlSchemaList, schemaName);
        }
        return qualified;
    }

    private boolean getAttributeQualification(XmlSchemaAttribute attr, String uri) {
        QName schemaName = attr.getQName();

        boolean qualified = false;
        if (attr.getForm() == XmlSchemaForm.QUALIFIED) {
            qualified = true;
        } else {
            qualified = WSDLUtils.isElementFormQualified(xmlSchemaList, schemaName);
        }
        return qualified;
    }

}
