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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jws.soap.SOAPBinding;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.PortType;
import javax.wsdl.WSDLElement;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SOAPBindingUtil;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapBody;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapFault;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapHeader;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.wsdl.WSDLHelper;

public class WSIBPValidator extends AbstractDefinitionValidator {
    private List<String> operationMap = new ArrayList<>();
    private WSDLHelper wsdlHelper = new WSDLHelper();

    public WSIBPValidator(Definition def) {
        super(def);
    }

    public boolean isValid() {
        boolean valid = true;
        for (Method m : getClass().getMethods()) {
            if (m.getName().startsWith("check") && m.getGenericReturnType() == boolean.class
                    && m.getGenericParameterTypes().length == 0) {
                try {
                    Boolean res = (Boolean) m.invoke(this);
                    if (!res) {
                        valid = false;
                    }
                } catch (Exception e) {
                    throw new ToolException(e);
                }
            }
        }
        return valid;
    }

    private boolean checkR2716(final BindingOperation bop) {
        SoapBody inSoapBody = SOAPBindingUtil.getBindingInputSOAPBody(bop);
        SoapBody outSoapBody = SOAPBindingUtil.getBindingOutputSOAPBody(bop);
        if (inSoapBody != null && !StringUtils.isEmpty(inSoapBody.getNamespaceURI())
            || outSoapBody != null && !StringUtils.isEmpty(outSoapBody.getNamespaceURI())) {
            addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2716") + "Operation '"
                            + bop.getName() + "' soapBody MUST NOT have namespace attribute");
            return false;
        }

        SoapHeader inSoapHeader = SOAPBindingUtil.getBindingInputSOAPHeader(bop);
        SoapHeader outSoapHeader = SOAPBindingUtil.getBindingOutputSOAPHeader(bop);
        if (inSoapHeader != null && !StringUtils.isEmpty(inSoapHeader.getNamespaceURI())
            || outSoapHeader != null && !StringUtils.isEmpty(outSoapHeader.getNamespaceURI())) {
            addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2716") + "Operation '"
                            + bop.getName() + "' soapHeader MUST NOT have namespace attribute");
            return false;
        }

        List<SoapFault> soapFaults = SOAPBindingUtil.getBindingOperationSoapFaults(bop);
        for (SoapFault fault : soapFaults) {
            if (!StringUtils.isEmpty(fault.getNamespaceURI())) {
                addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2716") + "Operation '"
                                + bop.getName() + "' soapFault MUST NOT have namespace attribute");
                return false;
            }
        }
        return true;
    }

    private boolean checkR2717AndR2726(final BindingOperation bop) {
        if (null == bop) {
            return true;
        }
        SoapBody inSoapBody = SOAPBindingUtil.getBindingInputSOAPBody(bop);
        SoapBody outSoapBody = SOAPBindingUtil.getBindingOutputSOAPBody(bop);
        if (inSoapBody != null && StringUtils.isEmpty(inSoapBody.getNamespaceURI())
            || outSoapBody != null && StringUtils.isEmpty(outSoapBody.getNamespaceURI())) {
            addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2717")
                + "soapBody in the input/output of the binding operation '"
                + bop.getName() + "' MUST have namespace attribute");
            return false;
        }

        SoapHeader inSoapHeader = SOAPBindingUtil.getBindingInputSOAPHeader(bop);
        SoapHeader outSoapHeader = SOAPBindingUtil.getBindingOutputSOAPHeader(bop);
        if (inSoapHeader != null && !StringUtils.isEmpty(inSoapHeader.getNamespaceURI())
            || outSoapHeader != null && !StringUtils.isEmpty(outSoapHeader.getNamespaceURI())) {
            addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2726") + "Operation '"
                            + bop.getName() + "' soapHeader MUST NOT have namespace attribute");
            return false;
        }

        List<SoapFault> soapFaults = SOAPBindingUtil.getBindingOperationSoapFaults(bop);
        for (SoapFault fault : soapFaults) {
            if (!StringUtils.isEmpty(fault.getNamespaceURI())) {
                addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2726") + "Operation '"
                                + bop.getName() + "' soapFault MUST NOT have namespace attribute");
                return false;
            }
        }
        return true;
    }

    private boolean checkR2201Input(final Operation operation,
                                    final BindingOperation bop) {
        List<Part> partsList = wsdlHelper.getInMessageParts(operation);
        int inmessagePartsCount = partsList.size();
        SoapBody soapBody = SOAPBindingUtil.getBindingInputSOAPBody(bop);
        if (soapBody != null) {
            List<?> parts = soapBody.getParts();
            int boundPartSize = parts == null ? inmessagePartsCount : parts.size();
            SoapHeader soapHeader = SOAPBindingUtil.getBindingInputSOAPHeader(bop);
            boundPartSize = soapHeader != null
                && soapHeader.getMessage().equals(
                    operation.getInput().getMessage()
                    .getQName())
                ? boundPartSize - 1 : boundPartSize;

            if (parts != null) {
                Iterator<?> partsIte = parts.iterator();
                while (partsIte.hasNext()) {
                    String partName = (String)partsIte.next();
                    boolean isDefined = false;
                    for (Part part : partsList) {
                        if (partName.equalsIgnoreCase(part.getName())) {
                            isDefined = true;
                            break;
                        }
                    }
                    if (!isDefined) {
                        addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2201") + "Operation '"
                                        + operation.getName() + "' soapBody parts : "
                                        + partName + " not found in the message, wrong WSDL");
                        return false;
                    }
                }
            } else {
                if (partsList.size() > 1) {
                    addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2210") + "Operation '" + operation.getName()
                                    + "' more than one part bound to body");
                    return false;
                }
            }


            if (boundPartSize > 1) {
                addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2201") + "Operation '" + operation.getName()
                                + "' more than one part bound to body");
                return false;
            }
        }
        return true;
    }

    private boolean checkR2201Output(final Operation operation,
                                     final BindingOperation bop) {
        int outmessagePartsCount = wsdlHelper.getOutMessageParts(operation).size();
        SoapBody soapBody = SOAPBindingUtil.getBindingOutputSOAPBody(bop);
        if (soapBody != null) {
            List<?> parts = soapBody.getParts();
            int boundPartSize = parts == null ? outmessagePartsCount : parts.size();
            SoapHeader soapHeader = SOAPBindingUtil.getBindingOutputSOAPHeader(bop);
            boundPartSize = soapHeader != null
                && soapHeader.getMessage().equals(
                    operation.getOutput().getMessage()
                    .getQName())
                ? boundPartSize - 1 : boundPartSize;
            if (parts != null) {
                Iterator<?> partsIte = parts.iterator();
                while (partsIte.hasNext()) {
                    String partName = (String)partsIte.next();
                    boolean isDefined = false;
                    for (Part part : wsdlHelper.getOutMessageParts(operation)) {
                        if (partName.equalsIgnoreCase(part.getName())) {
                            isDefined = true;
                            break;
                        }
                    }
                    if (!isDefined) {
                        addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2201") + "Operation '"
                                        + operation.getName() + "' soapBody parts : "
                                        + partName + " not found in the message, wrong WSDL");
                        return false;
                    }

                }
            } else {
                if (wsdlHelper.getOutMessageParts(operation).size() > 1) {
                    addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2210") + "Operation '" + operation.getName()
                                    + "' more than one part bound to body");
                    return false;
                }
            }

            if (boundPartSize > 1) {
                addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2201") + "Operation '" + operation.getName()
                                + "' more than one part bound to body");
                return false;
            }
        }
        return true;
    }

    private boolean checkR2209(final Operation operation,
                               final BindingOperation bop) {
        if ((bop.getBindingInput() == null && operation.getInput() != null)
            || (bop.getBindingOutput() == null && operation.getOutput() != null)) {
            addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2209")
                            + "Unbound PortType elements in Operation '" + operation.getName() + "'");
            return false;
        }
        return true;
    }

    public boolean checkBinding() {
        for (PortType portType : wsdlHelper.getPortTypes(def)) {
            Iterator<?> ite = portType.getOperations().iterator();
            while (ite.hasNext()) {
                Operation operation = (Operation)ite.next();
                if (isOverloading(operation.getName())) {
                    continue;
                }
                BindingOperation bop = wsdlHelper.getBindingOperation(def, operation.getName());
                if (bop == null) {
                    addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2718")
                                    + "A wsdl:binding in a DESCRIPTION MUST have the same set of "
                                    + "wsdl:operations as the wsdl:portType to which it refers. "
                                    + operation.getName() + " not found in wsdl:binding.");
                    return false;
                }
                Binding binding = wsdlHelper.getBinding(bop, def);
                String bindingStyle = binding != null ? SOAPBindingUtil.getBindingStyle(binding) : "";
                String style = StringUtils.isEmpty(SOAPBindingUtil.getSOAPOperationStyle(bop))
                    ? bindingStyle : SOAPBindingUtil.getSOAPOperationStyle(bop);
                if ("DOCUMENT".equalsIgnoreCase(style) || StringUtils.isEmpty(style)) {
                    boolean passed = checkR2201Input(operation, bop)
                        && checkR2201Output(operation, bop)
                        && checkR2209(operation, bop)
                        && checkR2716(bop);
                    if (!passed) {
                        return false;
                    }
                } else {
                    if (!checkR2717AndR2726(bop)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isHeaderPart(final BindingOperation bop, final Part part) {
        QName elementName = part.getElementName();
        if (elementName != null) {
            String partName = elementName.getLocalPart();
            SoapHeader inSoapHeader = SOAPBindingUtil.getBindingInputSOAPHeader(bop);
            if (inSoapHeader != null) {
                return partName.equals(inSoapHeader.getPart());
            }
            SoapHeader outSoapHeader = SOAPBindingUtil.getBindingOutputSOAPHeader(bop);
            if (outSoapHeader != null) {
                return partName.equals(outSoapHeader.getPart());
            }
        }
        return false;
    }

    public boolean checkR2203And2204() {

        Collection<Binding> bindings = CastUtils.cast(def.getBindings().values());
        for (Binding binding : bindings) {

            String style = SOAPBindingUtil.getCanonicalBindingStyle(binding);

            if (binding.getPortType() == null) {
                return true;
            }

            //

            for (Iterator<?> ite2 = binding.getPortType().getOperations().iterator(); ite2.hasNext();) {
                Operation operation = (Operation)ite2.next();
                BindingOperation bop = wsdlHelper.getBindingOperation(def, operation.getName());
                if (operation.getInput() != null && operation.getInput().getMessage() != null) {
                    Message inMess = operation.getInput().getMessage();
                    Set<String> ignorableParts = getIgnorableParts(bop.getBindingInput());

                    for (Iterator<?> ite3 = inMess.getParts().values().iterator(); ite3.hasNext();) {
                        Part p = (Part)ite3.next();
                        if (SOAPBinding.Style.RPC.name().equalsIgnoreCase(style) && p.getTypeName() == null
                            && !isHeaderPart(bop, p) && !isIgnorablePart(p.getName(), ignorableParts)) {
                            addErrorMessage("An rpc-literal binding in a DESCRIPTION MUST refer, "
                                            + "in its soapbind:body element(s), only to "
                                            + "wsdl:part element(s) that have been defined "
                                            + "using the type attribute.");
                            return false;
                        }

                        if (SOAPBinding.Style.DOCUMENT.name().equalsIgnoreCase(style)
                            && p.getElementName() == null && !isIgnorablePart(p.getName(), ignorableParts)) {
                            addErrorMessage("A document-literal binding in a DESCRIPTION MUST refer, "
                                            + "in each of its soapbind:body element(s),"
                                            + "only to wsdl:part element(s)"
                                            + " that have been defined using the element attribute.");
                            return false;
                        }

                    }
                }
                if (operation.getOutput() != null && operation.getOutput().getMessage() != null) {
                    Message outMess = operation.getOutput().getMessage();
                    Set<String> ignorableParts = getIgnorableParts(bop.getBindingOutput());

                    for (Iterator<?> ite3 = outMess.getParts().values().iterator(); ite3.hasNext();) {
                        Part p = (Part)ite3.next();
                        if (style.equalsIgnoreCase(SOAPBinding.Style.RPC.name()) && p.getTypeName() == null
                            && !isHeaderPart(bop, p) && !isIgnorablePart(p.getName(), ignorableParts)) {
                            addErrorMessage("An rpc-literal binding in a DESCRIPTION MUST refer, "
                                            + "in its soapbind:body element(s), only to "
                                            + "wsdl:part element(s) that have been defined "
                                            + "using the type attribute.");
                            return false;
                        }

                        if (style.equalsIgnoreCase(SOAPBinding.Style.DOCUMENT.name())
                            && p.getElementName() == null && !isIgnorablePart(p.getName(), ignorableParts)) {
                            addErrorMessage("A document-literal binding in a DESCRIPTION MUST refer, "
                                            + "in each of its soapbind:body element(s),"
                                            + "only to wsdl:part element(s)"
                                            + " that have been defined using the element attribute.");
                            return false;
                        }

                    }
                }
            }

        }
        return true;
    }

    private static boolean isIgnorablePart(String name, Set<String> ignorableParts) {
        return ignorableParts != null && ignorableParts.contains(name);
    }

    private static Set<String> getIgnorableParts(WSDLElement ext) {
        Set<String> parts = null;
        if (ext != null && ext.getExtensibilityElements() != null && ext.getExtensibilityElements().size() > 0
            && ext.getExtensibilityElements().get(0) instanceof MIMEMultipartRelated) {
            MIMEMultipartRelated mpr = (MIMEMultipartRelated)ext.getExtensibilityElements().get(0);
            List<MIMEPart> mps = CastUtils.cast(mpr.getMIMEParts());
            parts = new HashSet<>(mps.size());
            for (Iterator<MIMEPart> it = mps.iterator(); it.hasNext();) {
                MIMEPart mp = it.next();
                if (mp.getExtensibilityElements() != null && mp.getExtensibilityElements().size() > 0
                    && mp.getExtensibilityElements().get(0) instanceof MIMEContent) {
                    parts.add(((MIMEContent)mp.getExtensibilityElements().get(0)).getPart());
                }
            }
        }

        return parts;
    }

    // TODO: Should also check SoapHeader/SoapHeaderFault
    public boolean checkR2205() {
        Collection<Binding> bindings = CastUtils.cast(def.getBindings().values());
        for (Binding binding : bindings) {

            if (!SOAPBindingUtil.isSOAPBinding(binding)) {
                System.err.println("WSIBP Validator found <"
                                   + binding.getQName() + "> is NOT a SOAP binding");
                continue;
            }
            if (binding.getPortType() == null) {
                //will error later
                continue;
            }

            for (Iterator<?> ite2 = binding.getPortType().getOperations().iterator(); ite2.hasNext();) {
                Operation operation = (Operation)ite2.next();
                Collection<Fault> faults = CastUtils.cast(operation.getFaults().values());
                if (CollectionUtils.isEmpty(faults)) {
                    continue;
                }

                for (Fault fault : faults) {
                    Message message = fault.getMessage();
                    Collection<Part> parts = CastUtils.cast(message.getParts().values());
                    for (Part part : parts) {
                        if (part.getElementName() == null) {
                            addErrorMessage(getErrorPrefix("WSI-BP-1.0 R2205") + "In Message "
                                + message.getQName() + ", part " + part.getName()
                                    + " must specify a 'element' attribute");
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean checkR2705() {
        Collection<Binding> bindings = CastUtils.cast(def.getBindings().values());
        for (Binding binding : bindings) {
            if (SOAPBindingUtil.isMixedStyle(binding)) {
                addErrorMessage("Mixed style, invalid WSDL");
                return false;
            }
        }
        return true;
    }

    private boolean isOverloading(String operationName) {
        if (operationMap.contains(operationName)) {
            return true;
        }
        operationMap.add(operationName);
        return false;
    }

    private static String getErrorPrefix(String ruleBroken) {
        return ruleBroken + " violation: ";
    }
}
