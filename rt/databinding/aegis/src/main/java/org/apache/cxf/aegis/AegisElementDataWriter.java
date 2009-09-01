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

package org.apache.cxf.aegis;

import javax.xml.namespace.QName;
import org.w3c.dom.Element;

import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

/**
 * 
 */
public class AegisElementDataWriter extends AbstractAegisIoImpl 
       implements AegisWriter<Element> {
    
    protected AegisXMLStreamDataWriter writer;

    public AegisElementDataWriter(AegisContext globalContext) {
        super(globalContext);
        writer = new AegisXMLStreamDataWriter(globalContext);
    }

    public void write(Object obj, QName elementName, boolean optional, Element output, AegisType aegisType)
        throws Exception {
        W3CDOMStreamWriter swriter = new W3CDOMStreamWriter(output);
        writer.write(obj, elementName, optional, swriter, aegisType);
    }

    public void write(Object obj, QName elementName, boolean optional, Element output,
                      java.lang.reflect.Type objectType) throws Exception {
        W3CDOMStreamWriter swriter = new W3CDOMStreamWriter(output);
        writer.write(obj, elementName, optional, swriter, objectType);
        
    }
}
