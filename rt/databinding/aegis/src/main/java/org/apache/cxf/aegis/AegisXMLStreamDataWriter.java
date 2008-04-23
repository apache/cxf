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

import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.aegis.xml.stax.ElementWriter;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;

public class AegisXMLStreamDataWriter extends AbstractAegisIoImpl implements AegisWriter<XMLStreamWriter> {

    private static final Logger LOG = LogUtils.getL7dLogger(AegisXMLStreamDataWriter.class);

    AegisXMLStreamDataWriter(AegisContext globalContext) {
        super(globalContext);
    }
    
    /**
     * Write an object to the output.
     * @param obj The object to write.
     * @param elementName the QName of the XML Element.
     * @param optional set this for minOccurs = 0. It omits null elements.
     * @param output the output stream
     * @param aegisType the aegis type.
     * @throws Exception
     */
    public void write(Object obj, 
                      QName elementName,
                      boolean optional,
                      XMLStreamWriter output, 
                      Type aegisType) throws Exception {
        
        if (obj == null && aegisType == null && !optional) {
            Message message = new Message("WRITE_NEEDS_TYPE", LOG);
            throw new DatabindingException(message);
        }
        
        if (obj != null) {
            aegisType = TypeUtil.getWriteType(aegisContext, obj, aegisType);
        }
        
        if (obj == null) {
            if (optional) { // minOccurs = 0
                return;
            }
            if (aegisType.isNillable() && aegisType.isWriteOuter()) {
                ElementWriter writer = new ElementWriter(output);
                MessageWriter w2 = writer.getElementWriter(elementName);
                w2.writeXsiNil();
                w2.close();
                return;
            }
        }
        
        ElementWriter writer = new ElementWriter(output);
        MessageWriter w2 = writer.getElementWriter(elementName);
        aegisType.writeObject(obj, w2, context);
        w2.close();
    }
}
