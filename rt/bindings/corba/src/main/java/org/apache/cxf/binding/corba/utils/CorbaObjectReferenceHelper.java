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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.xml.namespace.QName;

import com.ibm.wsdl.ImportImpl;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.cxf.binding.corba.wsdl.BindingType;
import org.apache.cxf.common.logging.LogUtils;
import org.omg.CORBA.Object;

public final class CorbaObjectReferenceHelper {

    public static final String WSDLI_NAMESPACE_URI = "http://www.w3.org/2006/01/wsdl-instance";
    public static final String ADDRESSING_NAMESPACE_URI = "http://www.w3.org/2005/08/addressing";
    public static final String ADDRESSING_WSDL_NAMESPACE_URI = "http://www.w3.org/2006/05/addressing/wsdl";
    
    private static final Logger LOG = LogUtils.getL7dLogger(CorbaObjectReferenceHelper.class);
    
    private CorbaObjectReferenceHelper() {
        //utility class
    }

    public static String getWSDLLocation(Definition wsdlDef) {
        return wsdlDef.getDocumentBaseURI();
    }
    
    public static QName getServiceName(Binding binding, Definition wsdlDef) {
        LOG.log(Level.FINE, "Getting service name for an object reference");
        Collection services = wsdlDef.getServices().values();
        for (Iterator iter = services.iterator(); iter.hasNext();) {
            Service serv = (Service)iter.next();
            Collection ports = serv.getPorts().values();
            for (Iterator portIter = ports.iterator(); portIter.hasNext();) {
                Port pt = (Port)portIter.next();
                if (pt.getBinding().equals(binding)) {
                    return serv.getQName();
                }
            }
        }
        return null;
    }
    
    public static String getEndpointName(Binding binding, Definition wsdlDef) {
        LOG.log(Level.FINE, "Getting endpoint name for an object reference");
        Collection services = wsdlDef.getServices().values();
        for (Iterator iter = services.iterator(); iter.hasNext();) {
            Service serv = (Service)iter.next();
            Collection ports = serv.getPorts().values();
            for (Iterator portIter = ports.iterator(); portIter.hasNext();) {
                Port pt = (Port)portIter.next();
                if (pt.getBinding().equals(binding)) {
                    return pt.getName();
                }
            }
        }
        return null;
    }
    
    public static Binding getDefaultBinding(Object obj, Definition wsdlDef) {
        LOG.log(Level.FINEST, "Getting binding for a default object reference");
        Collection bindings = wsdlDef.getBindings().values();
        for (Iterator iter = bindings.iterator(); iter.hasNext();) {
            Binding b = (Binding)iter.next();
            List extElements = b.getExtensibilityElements();
            
            // Get the list of all extensibility elements
            for (Iterator extIter = extElements.iterator(); extIter.hasNext();) {
                java.lang.Object element = extIter.next();

                // Find a binding type so we can check against its repository ID
                if (element instanceof BindingType) {
                    BindingType type = (BindingType)element;
                    if (obj._is_a(type.getRepositoryID())) {
                        return b;
                    }
                }
            }
        }
        
        return null;
    }

    public static EprMetaData getBindingForTypeId(String repId, Definition wsdlDef) {
        LOG.log(Level.FINE, "RepositoryId " + repId 
                + ", wsdl namespace " + wsdlDef.getTargetNamespace());
        EprMetaData ret = new EprMetaData();
        Collection bindings = wsdlDef.getBindings().values();
        for (Iterator iter = bindings.iterator(); iter.hasNext();) {
            Binding b = (Binding)iter.next();
            List extElements = b.getExtensibilityElements();
            
            // Get the list of all extensibility elements
            for (Iterator extIter = extElements.iterator(); extIter.hasNext();) {
                java.lang.Object element = extIter.next();

                // Find a binding type so we can check against its repository ID
                if (element instanceof BindingType) {
                    BindingType type = (BindingType)element;
                    if (repId.equals(type.getRepositoryID())) {
                        ret.setCandidateWsdlDef(wsdlDef);
                        ret.setBinding(b);
                        return ret;
                    }
                }
            }
        }
        
        if (!ret.isValid()) {
            // recursivly check imports
            Iterator importLists = wsdlDef.getImports().values().iterator();
            while (importLists.hasNext()) {
                List imports = (List) importLists.next();
                for (java.lang.Object imp : imports) {
                    if (imp instanceof ImportImpl) {
                        Definition importDef = ((ImportImpl)imp).getDefinition();
                        LOG.log(Level.INFO, "Following import " + importDef.getDocumentBaseURI()); 
                        ret = getBindingForTypeId(repId, importDef);
                        if (ret.isValid()) {
                            return ret;
                        }
                    }
                }
            }
        }
        return ret;
    }

    public static String extractTypeIdFromIOR(String url) {        
        String ret = new String();
        byte data[] = null;
        try {
            // skip past IOR:
            data = Hex.decodeHex(url.substring(4).toCharArray());
        } catch (DecoderException e) {
            throw new RuntimeException("Failed to convert ascii hex ior to byte[], ior url=" + url, e);
        }
        if (data.length > 0) {
            // parse out type_id from IOR CDR encapsulation
            boolean bigIndian = !(data[0] > 0);
            int typeIdStringSize = readIntFromAlignedCDREncaps(data, 4, bigIndian);
            if (typeIdStringSize > 1) {
                ret = readStringFromAlignedCDREncaps(data, 8, typeIdStringSize - 1);
            }
        }
        return ret;
    }

    private static String readStringFromAlignedCDREncaps(byte[] data, int startIndex, int length) {
        char[] arr = new char[length];
        for (int i = 0; i < length; i++) {
            arr[i] = (char) (data[startIndex + i] & 0xff);
        }
        return new String(arr);
    }

    public static int readIntFromAlignedCDREncaps(byte[] data, int index, boolean bigEndian) {
        if (bigEndian) {
            int partial = ((((int)data[index]) << 24) & 0xff000000)
                | ((((int)data[index + 1]) << 16) & 0x00ff0000);
            return partial | ((((int)data[index + 2]) << 8) & 0x0000ff00) 
                | ((((int)data[index + 3])) & 0x000000ff);
        } else {
            int partial = ((((int)data[index])) & 0x000000ff)
                | ((((int)data[index + 1]) << 8) & 0x0000ff00);
            return partial | ((((int)data[index + 2]) << 16) & 0x00ff0000) 
                | ((((int)data[index + 3]) << 24) & 0xff000000);
        }
    }

    public static void populateEprInfo(EprMetaData info) {
        if (!info.isValid()) {
            return;
        }
        Binding match = info.getBinding();
        Definition wsdlDef = info.getCandidateWsdlDef();
        Collection services = wsdlDef.getServices().values();
        for (Iterator iter = services.iterator(); iter.hasNext();) {
            Service serv = (Service)iter.next();
            Collection ports = serv.getPorts().values();
            for (Iterator portIter = ports.iterator(); portIter.hasNext();) {
                Port pt = (Port)portIter.next();
                if (pt.getBinding().equals(match)) {
                    info.setPortName(pt.getName());
                    info.setServiceQName(serv.getQName());
                    break;
                }
            }
        }
        
        if (info.getServiceQName() == null) {
            Iterator importLists = wsdlDef.getImports().values().iterator();
            while (info.getServiceQName() == null && importLists.hasNext()) {
                List imports = (List) importLists.next();
                for (java.lang.Object imp : imports) {
                    if (imp instanceof ImportImpl) {
                        Definition importDef = ((ImportImpl)imp).getDefinition();
                        LOG.log(Level.FINE, "following wsdl import " + importDef.getDocumentBaseURI());
                        info.setCandidateWsdlDef(importDef);
                        populateEprInfo(info);
                        if (info.getServiceQName() != null) {
                            break;
                        }
                    }
                }
            }
        }
    }
}
