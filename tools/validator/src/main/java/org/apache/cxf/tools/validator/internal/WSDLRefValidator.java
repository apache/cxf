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

package org.apache.cxf.tools.validator.internal;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Import;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceSchemaInfo;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.validator.internal.model.FailureLocation;
import org.apache.cxf.tools.validator.internal.model.XBinding;
import org.apache.cxf.tools.validator.internal.model.XDef;
import org.apache.cxf.tools.validator.internal.model.XFault;
import org.apache.cxf.tools.validator.internal.model.XInput;
import org.apache.cxf.tools.validator.internal.model.XMessage;
import org.apache.cxf.tools.validator.internal.model.XNode;
import org.apache.cxf.tools.validator.internal.model.XOperation;
import org.apache.cxf.tools.validator.internal.model.XOutput;
import org.apache.cxf.tools.validator.internal.model.XPort;
import org.apache.cxf.tools.validator.internal.model.XPortType;
import org.apache.cxf.tools.validator.internal.model.XService;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.SchemaUtil;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaType;

public class WSDLRefValidator extends AbstractDefinitionValidator {
    protected static final Logger LOG = LogUtils.getL7dLogger(WSDLRefValidator.class);
    protected List<XNode> vNodes = new ArrayList<XNode>();

    private Set<QName> portTypeRefNames = new HashSet<QName>();
    private Set<QName> messageRefNames = new HashSet<QName>();
    private Map<QName, Service> services = new HashMap<QName, Service>();

    private ValidationResult vResults = new ValidationResult();

    private Definition definition;
    private Document baseDoc;

    private List<Definition> importedDefinitions;
    private SchemaCollection schemaCollection = new SchemaCollection();

    private boolean suppressWarnings;

    public WSDLRefValidator(Definition wsdl, Document doc) {
        this(wsdl, doc, BusFactory.getDefaultBus());
    }

    public WSDLRefValidator(Definition wsdl, Document doc, Bus bus) {
        this.definition = wsdl;
        baseDoc = doc;
        importedDefinitions = new ArrayList<Definition>();
        parseImports(wsdl);
        processSchemas(bus);
    }
    private void getSchemas(Bus bus) {
        Map<String, Element> schemaList = new HashMap<String, Element>();
        SchemaUtil schemaUtil = new SchemaUtil(bus, schemaList);
        List<SchemaInfo> si = new ArrayList<SchemaInfo>();
        schemaUtil.getSchemas(definition, schemaCollection, si);
        ServiceSchemaInfo ssi = new ServiceSchemaInfo();
        ssi.setSchemaCollection(schemaCollection);
        ssi.setSchemaInfoList(si);
        ssi.setSchemaElementList(schemaList);
        bus.getExtension(WSDLManager.class).putSchemasForDefinition(definition, ssi);
    }
    private void processSchemas(Bus bus) {
        try {
            ServiceSchemaInfo info = bus.getExtension(WSDLManager.class)
                .getSchemasForDefinition(definition);
            if (info == null) {
                getSchemas(bus);
            } else {
                schemaCollection = info.getSchemaCollection();                
            }
            checkTargetNamespace(this.definition.getTargetNamespace());
        } catch (Exception ex) {
            throw new ToolException(ex);
        }        
    }
    private Collection<Import> getImports(final Definition wsdlDef) {
        Collection<Import> importList = new ArrayList<Import>();
        Map imports = wsdlDef.getImports();
        for (Iterator iter = imports.keySet().iterator(); iter.hasNext();) {
            String uri = (String)iter.next();
            List<Import> lst = CastUtils.cast((List)imports.get(uri));
            importList.addAll(lst);
        }
        return importList;
    }
    private void parseImports(Definition def) {
        for (Import impt : getImports(def)) {
            if (!importedDefinitions.contains(impt.getDefinition())) {
                importedDefinitions.add(impt.getDefinition());
                parseImports(impt.getDefinition());
            }
        }
    }

    
    private void checkTargetNamespace(String path) {
        try {
            if (new URL(path).getPath().indexOf(":") != -1) {
                throw new ToolException(": is not a valid char in the targetNamespace");
            }
        } catch (MalformedURLException e) {
            // do nothing
        }
    }

    public void setSuppressWarnings(boolean s) {
        this.suppressWarnings = s;
    }

    public ValidationResult getValidationResults() {
        return this.vResults;
    }

    private Document getWSDLDocument(final String wsdl) throws URISyntaxException {
        return new Stax2DOM().getDocument(wsdl);
    }

    private Document getWSDLDocument() throws Exception {
        if (baseDoc != null) {
            return baseDoc;
        }
        return getWSDLDocument(this.definition.getDocumentBaseURI());
    }

    private List<Document> getWSDLDocuments() {
        List<Document> docs = new ArrayList<Document>();
        try {
            docs.add(getWSDLDocument());

            if (null != importedDefinitions) {
                for (Definition d : importedDefinitions) {
                    docs.add(getWSDLDocument(d.getDocumentBaseURI()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // ignore
        }

        return docs;
    }

    private boolean isExist(List<Document> docs, XNode vNode) {
        for (Document doc : docs) {
            if (vNode.matches(doc)) {
                return true;
            }
        }
        return false;
    }

    private FailureLocation getFailureLocation(List<Document> docs, XNode fNode) {
        if (fNode == null) {
            return null;
        }

        XPathUtils xpather = new XPathUtils(fNode.getNSMap());
        for (Document doc : docs) {
            Node node = (Node) xpather.getValue(fNode.toString(), doc, XPathConstants.NODE);
            if (null != node) {
                try {
                    return new FailureLocation((Location)node.getUserData("location"),
                                               doc.getDocumentURI());
                } catch (Exception ex) {
                    //ignore, probably not DOM level 3
                }
            }
        }
        return null;
    }

    public boolean isValid() {
        try {
            loadServices();

            collectValidationPoints();

            List<Document> wsdlDocs = getWSDLDocuments();
            for (XNode vNode : vNodes) {
                if (!isExist(wsdlDocs, vNode)) {
                    //System.out.println("Fail: " + vNode.getXPath());
                    FailureLocation loc = getFailureLocation(wsdlDocs, vNode.getFailurePoint());

                    vResults.addError(new Message("FAILED_AT_POINT",
                                                  LOG,
                                                  loc.getLocation().getLineNumber(),
                                                  loc.getLocation().getColumnNumber(),
                                                  loc.getDocumentURI(),
                                                  vNode.getPlainText()));
                }
            }
        } catch (Exception e) {
            this.vResults.addError(e.getMessage());
            return false;
        }
        return vResults.isSuccessful();
    }

    private void addServices(final Definition wsdlDef) {
        Iterator sNames = wsdlDef.getServices().keySet().iterator();
        while (sNames.hasNext()) {
            QName sName = (QName) sNames.next();
            services.put(sName, definition.getService(sName));
        }
    }

    private void loadServices() {
        addServices(this.definition);
        if (importedDefinitions != null) {
            for (Definition d : importedDefinitions) {
                addServices(d);
            }
        }
    }

    private Map<QName, XNode> getBindings(Service service) {
        Map<QName, XNode> bindings = new HashMap<QName, XNode>();

        if (service.getPorts().values().size() == 0) {
            throw new ToolException("Service " + service.getQName() + " does not contain any usable ports");
        }
        Iterator portIte = service.getPorts().values().iterator();
        while (portIte.hasNext()) {
            Port port = (Port)portIte.next();
            Binding binding = port.getBinding();
            bindings.put(binding.getQName(), getXNode(service, port));
            if (WSDLConstants.NS_WSDL11.equals(binding.getQName().getNamespaceURI())) {
                throw new ToolException("Binding "
                                        + binding.getQName().getLocalPart()
                                        + " namespace set improperly.");
            }
        }

        return bindings;
    }

    private Map<QName, Operation> getOperations(PortType portType) {
        Map<QName, Operation> operations = new HashMap<QName, Operation>();
        for (Iterator iter = portType.getOperations().iterator(); iter.hasNext();) {
            Operation op = (Operation) iter.next();
            operations.put(new QName(portType.getQName().getNamespaceURI(), op.getName()), op);
        }
        return operations;
    }

    private XNode getXNode(Service service, Port port) {
        XNode vService = getXNode(service);

        XPort pNode = new XPort();
        pNode.setName(port.getName());
        pNode.setParentNode(vService);
        return pNode;
    }

    private XNode getXNode(Service service) {
        XDef xdef = new XDef();
        xdef.setTargetNamespace(service.getQName().getNamespaceURI());

        XService sNode = new XService();
        sNode.setName(service.getQName().getLocalPart());
        sNode.setParentNode(xdef);
        return sNode;
    }

    private XNode getXNode(Binding binding) {
        XDef xdef = new XDef();
        xdef.setTargetNamespace(binding.getQName().getNamespaceURI());

        XBinding bNode = new XBinding();
        bNode.setName(binding.getQName().getLocalPart());
        bNode.setParentNode(xdef);
        return bNode;
    }

    private XNode getXNode(PortType portType) {
        XDef xdef = new XDef();
        xdef.setTargetNamespace(portType.getQName().getNamespaceURI());

        XPortType pNode = new XPortType();
        pNode.setName(portType.getQName().getLocalPart());
        pNode.setParentNode(xdef);
        return pNode;
    }

    private XNode getOperationXNode(XNode pNode, String opName) {
        XOperation node = new XOperation();
        node.setName(opName);
        node.setParentNode(pNode);
        return node;
    }

    private XNode getInputXNode(XNode opVNode, String name) {
        XInput oNode = new XInput();
        oNode.setName(name);
        oNode.setParentNode(opVNode);
        
        if (name != null && name.equals(opVNode.getAttributeValue() + "Request")) {
            oNode.setDefaultAttributeValue(true);
        }
        return oNode;
    }

    private XNode getOutputXNode(XNode opVNode, String name) {
        XOutput oNode = new XOutput();
        oNode.setName(name);
        oNode.setParentNode(opVNode);
        if (name != null && name.equals(opVNode.getAttributeValue() + "Response")) {
            oNode.setDefaultAttributeValue(true);
        }
        return oNode;
    }

    private XNode getFaultXNode(XNode opVNode, String name) {
        XFault oNode = new XFault();
        oNode.setName(name);
        oNode.setParentNode(opVNode);
        return oNode;
    }

    private XNode getXNode(javax.wsdl.Message msg) {
        XDef xdef = new XDef();
        xdef.setTargetNamespace(msg.getQName().getNamespaceURI());

        XMessage mNode = new XMessage();
        mNode.setName(msg.getQName().getLocalPart());
        mNode.setParentNode(xdef);
        return mNode;
    }

    private void addWarning(String warningMsg) {
        if (suppressWarnings) {
            return;
        }
        vResults.addWarning(warningMsg);
    }

    private void collectValidationPoints() throws Exception {
        if (services.size() == 0) {
            LOG.log(Level.WARNING, "WSDL document " 
                    + this.definition.getDocumentBaseURI() + " does not define any services");
            //addWarning("WSDL document does not define any services");
            Collection<QName> ports = CastUtils.cast(this.definition.getAllPortTypes().keySet());
            portTypeRefNames.addAll(ports);
        } else {
            collectValidationPointsForBindings();
        }

        collectValidationPointsForPortTypes();
        collectValidationPointsForMessages();
    }

    private void collectValidationPointsForBindings() throws Exception {
        Map<QName, XNode> vBindingNodes = new HashMap<QName, XNode>();
        for (Service service : services.values()) {
            vBindingNodes.putAll(getBindings(service));
        }

        for (QName bName : vBindingNodes.keySet()) {
            Binding binding = this.definition.getBinding(bName);
            if (binding == null) {
                LOG.log(Level.SEVERE, bName.toString() 
                        + " is not correct, please check that the correct namespace is being used");
                throw new Exception(bName.toString() 
                        + " is not correct, please check that the correct namespace is being used");
            }
            XNode vBindingNode = getXNode(binding);
            vBindingNode.setFailurePoint(vBindingNodes.get(bName));
            vNodes.add(vBindingNode);

            if (binding.getPortType() == null) {
                continue;
            }
            portTypeRefNames.add(binding.getPortType().getQName());

            XNode vPortTypeNode = getXNode(binding.getPortType());
            vPortTypeNode.setFailurePoint(vBindingNode);
            vNodes.add(vPortTypeNode);
            for (Iterator iter = binding.getBindingOperations().iterator(); iter.hasNext();) {
                BindingOperation bop = (BindingOperation) iter.next();
                XNode vOpNode = getOperationXNode(vPortTypeNode, bop.getName());
                XNode vBopNode = getOperationXNode(vBindingNode, bop.getName());
                vOpNode.setFailurePoint(vBopNode);
                vNodes.add(vOpNode);
                if (bop.getBindingInput() != null) {
                    String inName = bop.getBindingInput().getName();
                    if (!StringUtils.isEmpty(inName)) {
                        XNode vInputNode = getInputXNode(vOpNode, inName);
                        vInputNode.setFailurePoint(getInputXNode(vBopNode, inName));
                        vNodes.add(vInputNode);
                    }
                }
                if (bop.getBindingOutput() != null) {
                    String outName = bop.getBindingOutput().getName();
                    if (!StringUtils.isEmpty(outName)) {
                        XNode vOutputNode = getOutputXNode(vOpNode, outName);
                        vOutputNode.setFailurePoint(getOutputXNode(vBopNode, outName));
                        vNodes.add(vOutputNode);
                    }
                }
                for (Iterator iter1 = bop.getBindingFaults().keySet().iterator(); iter1.hasNext();) {
                    String faultName = (String) iter1.next();
                    XNode vFaultNode = getFaultXNode(vOpNode, faultName);
                    vFaultNode.setFailurePoint(getFaultXNode(vBopNode, faultName));
                    vNodes.add(vFaultNode);
                }
            }
        }
    }

    private javax.wsdl.Message getMessage(QName msgName) {
        javax.wsdl.Message message = this.definition.getMessage(msgName);
        if (message == null) {
            for (Definition d : importedDefinitions) {
                message = d.getMessage(msgName);
                if (message != null) {
                    break;
                }
            }
        }
        return message;
    }

    private void collectValidationPointsForMessages() {
        for (QName msgName : messageRefNames) {
            javax.wsdl.Message message = getMessage(msgName);
            for (Iterator iter = message.getParts().values().iterator(); iter.hasNext();) {
                Part part = (Part) iter.next();
                QName elementName = part.getElementName();
                QName typeName = part.getTypeName();

                if (elementName == null && typeName == null) {
                    vResults.addError(new Message("PART_NO_TYPES", LOG));
                    continue;
                }

                if (elementName != null && typeName != null) {
                    vResults.addError(new Message("PART_NOT_UNIQUE", LOG));
                    continue;
                }

                if (elementName != null && typeName == null) {
                    boolean valid = validatePartType(elementName.getNamespaceURI(),
                                                     elementName.getLocalPart(), true);
                    if (!valid) {
                        vResults.addError(new Message("TYPE_REF_NOT_FOUND", LOG, message.getQName(),
                                                      part.getName(), elementName));
                    }

                }
                if (typeName != null && elementName == null) {
                    boolean valid = validatePartType(typeName.getNamespaceURI(),
                                                     typeName.getLocalPart(),
                                                     false);
                    if (!valid) {
                        vResults.addError(new Message("TYPE_REF_NOT_FOUND", LOG, message.getQName(),
                                                      part.getName(), typeName));
                    }
                }
            }
        }
    }

    private PortType getPortType(QName ptName) {
        PortType portType = this.definition.getPortType(ptName);
        if (portType == null) {
            for (Definition d : importedDefinitions) {
                portType = d.getPortType(ptName);
                if (portType != null) {
                    break;
                }
            }
        }
        return portType;
    }

    private void collectValidationPointsForPortTypes() {
        for (QName ptName : portTypeRefNames) {
            PortType portType = getPortType(ptName);
            if (portType == null) {
                vResults.addError(new Message("NO_PORTTYPE", LOG, ptName));
                continue;
            }

            XNode vPortTypeNode = getXNode(portType);
            for (Operation operation : getOperations(portType).values()) {
                XNode vOperationNode = getOperationXNode(vPortTypeNode, operation.getName());
                if (operation.getInput() == null) {
                    vResults.addError(new Message("WRONG_MEP", LOG, operation.getName(),
                                                  portType.getQName()));
                    continue;
                }
                javax.wsdl.Message inMsg = operation.getInput().getMessage();
                if (inMsg == null) {
                    addWarning("Operation " + operation.getName() + " in PortType: "
                               + portType.getQName() + " has no input message");
                } else {
                    XNode vInMsgNode = getXNode(inMsg);
                    vInMsgNode.setFailurePoint(getInputXNode(vOperationNode, operation.getInput().getName()));
                    vNodes.add(vInMsgNode);
                    messageRefNames.add(inMsg.getQName());
                }

                if (operation.getOutput() != null) {
                    javax.wsdl.Message outMsg = operation.getOutput().getMessage();

                    if (outMsg == null) {
                        addWarning("Operation " + operation.getName() + " in PortType: "
                                   + portType.getQName() + " has no output message");
                    } else {
                        XNode vOutMsgNode = getXNode(outMsg);
                        vOutMsgNode.setFailurePoint(getOutputXNode(vOperationNode,
                                                                   operation.getOutput().getName()));
                        vNodes.add(vOutMsgNode);
                        messageRefNames.add(outMsg.getQName());
                    }
                }
                for (Iterator iter = operation.getFaults().values().iterator(); iter.hasNext();) {
                    Fault fault = (Fault) iter.next();
                    javax.wsdl.Message faultMsg = fault.getMessage();
                    XNode vFaultMsgNode = getXNode(faultMsg);
                    vFaultMsgNode.setFailurePoint(getFaultXNode(vOperationNode, fault.getName()));
                    vNodes.add(vFaultMsgNode);
                    messageRefNames.add(faultMsg.getQName());
                }
            }
        }
    }

    private boolean validatePartType(String namespace, String name, boolean isElement) {

        boolean partvalid = false;

        if (namespace.equals(WSDLConstants.NS_SCHEMA_XSD)) {
            if (isElement) {
                XmlSchemaElement schemaEle =
                    schemaCollection.getElementByQName(new QName(WSDLConstants.NS_SCHEMA_XSD, name));
                partvalid = schemaEle != null ? true : false;
            } else {
                if ("anyType".equals(name)) {
                    return true;
                }
                XmlSchemaType schemaType =
                    schemaCollection.getTypeByQName(new QName(WSDLConstants.NS_SCHEMA_XSD, name));

                partvalid = schemaType != null ? true : false;
            }

        } else {
            if (isElement) {
                if (schemaCollection.getElementByQName(new QName(namespace, name)) != null) {
                    partvalid = true;
                }
            } else {
                if (schemaCollection.getTypeByQName(new QName(namespace, name)) != null) {
                    partvalid = true;
                }
            }
        }
        return partvalid;
    }

    public String getErrorMessage() {
        return vResults.toString();
    }

    public Definition getDefinition() {
        return this.definition;
    }
}
