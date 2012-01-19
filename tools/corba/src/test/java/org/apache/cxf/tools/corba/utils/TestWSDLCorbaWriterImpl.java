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
package org.apache.cxf.tools.corba.utils;

import java.util.Map;
import java.util.TreeMap;

import javax.wsdl.Definition;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;

import javax.xml.namespace.QName;

import com.ibm.wsdl.xml.WSDLWriterImpl;

import org.apache.cxf.helpers.CastUtils;


 /*
  * This class is extending the wsdl4j RI class to print out the 
  * maps in a particular order
  * 
  */
public class TestWSDLCorbaWriterImpl extends WSDLWriterImpl {

    private TestComparator comparator = new TestComparator();

    protected void printBindingFaults(Map bindingFaults,
                                      Definition def,
                                      java.io.PrintWriter pw)
        throws WSDLException {
        Map<Object, Object> bfaults = new TreeMap<Object, Object>(comparator);
        bfaults.putAll(CastUtils.cast(bindingFaults));
        super.printBindingFaults(bfaults, def, pw);
    }

    protected void printBindings(java.util.Map bindings, Definition def, java.io.PrintWriter pw)
        throws WSDLException {
        Map<Object, Object> map = new TreeMap<Object, Object>(comparator);
        map.putAll(CastUtils.cast(bindings));
        super.printBindings(map, def, pw);
    }
    
    protected void printFaults(java.util.Map faults, Definition def, java.io.PrintWriter pw) 
        throws WSDLException {
        Map<Object, Object> map = new TreeMap<Object, Object>(comparator);
        map.putAll(CastUtils.cast(faults));
        super.printFaults(map, def, pw);
    }

    protected void printImports(java.util.Map imports, Definition def, java.io.PrintWriter pw) 
        throws WSDLException {
        Map<Object, Object> map = new TreeMap<Object, Object>(comparator);
        map.putAll(CastUtils.cast(imports));
        super.printImports(map, def, pw);
    }

    protected void printMessages(java.util.Map messages, Definition def, java.io.PrintWriter pw) 
        throws WSDLException {
        Map<Object, Object> map = new TreeMap<Object, Object>(comparator);
        map.putAll(CastUtils.cast(messages));
        super.printMessages(map, def, pw);
    }

    protected void printNamespaceDeclarations(java.util.Map namespaces, java.io.PrintWriter pw) 
        throws WSDLException {
        Map<Object, Object> map = new TreeMap<Object, Object>(comparator);
        map.putAll(CastUtils.cast(namespaces));
        super.printNamespaceDeclarations(map, pw);
    }

    protected void printPorts(java.util.Map ports, Definition def, java.io.PrintWriter pw) 
        throws WSDLException {
        Map<Object, Object> map = new TreeMap<Object, Object>(comparator);
        map.putAll(CastUtils.cast(ports));
        super.printPorts(map, def, pw);
    }
    protected void printPortTypes(java.util.Map portTypes, Definition def, java.io.PrintWriter pw) 
        throws WSDLException {
        Map<QName, PortType> map = new TreeMap<QName, PortType>(comparator);
        map.putAll(CastUtils.cast(portTypes, QName.class, PortType.class));
        super.printPortTypes(map, def, pw);
    }
           
    protected void printServices(java.util.Map services, Definition def, java.io.PrintWriter pw) 
        throws WSDLException {
        Map<Object, Object> map = new TreeMap<Object, Object>(comparator);
        map.putAll(CastUtils.cast(services));
        super.printServices(map, def, pw);
    }

    public class TestComparator implements java.util.Comparator<Object> {
        
        private java.text.Collator collator;

        public TestComparator() {
            collator = java.text.Collator.getInstance();
        }

        public int compare(Object o1, Object o2) {
            return collator.compare(o1.toString(), o2.toString());
        }
 
        public boolean equals(Object o1, Object o2) {
            return collator.equals(o1.toString(), o2.toString());
        }
    }
}
