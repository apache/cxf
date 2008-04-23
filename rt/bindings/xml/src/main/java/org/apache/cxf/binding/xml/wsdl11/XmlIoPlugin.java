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

package org.apache.cxf.binding.xml.wsdl11;

import java.util.Map;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import org.apache.cxf.bindings.xformat.XMLBindingMessageFormat;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.wsdl.AbstractWSDLPlugin;

public final class XmlIoPlugin extends AbstractWSDLPlugin {

    public ExtensibilityElement createExtension(final Map<String, Object> args) throws WSDLException {
        XMLBindingMessageFormat xmlFormat = null;

        Class clz = getOption(args, Class.class);
        QName qname = getOption(args, QName.class);

        xmlFormat = (XMLBindingMessageFormat)registry.createExtension(clz, ToolConstants.XML_FORMAT);
        xmlFormat.setRootNode(qname);
        return xmlFormat;
    }
}
