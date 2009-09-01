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
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.type.basic.ArrayType;
import org.apache.cxf.aegis.xml.stax.ElementReader;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;

public class AegisXMLStreamDataReader extends AbstractAegisIoImpl implements AegisReader<XMLStreamReader> {

    private static final Logger LOG = LogUtils.getL7dLogger(AegisXMLStreamDataReader.class);

    public AegisXMLStreamDataReader(AegisContext globalContext) {
        super(globalContext);
    }
    
    /**
     * This constructor is used by the Element data reader to borrow this class.
     * @param globalContext
     * @param context
     */
    public AegisXMLStreamDataReader(AegisContext globalContext, Context context) {
        super(globalContext, context);
    }

    private void setupReaderPosition(XMLStreamReader reader) throws Exception {
        if (reader.getEventType() == XMLStreamConstants.START_DOCUMENT) {
            while (XMLStreamConstants.START_ELEMENT != reader.getEventType()) {
                reader.nextTag();
            }
        }
        if (reader.getEventType() != XMLStreamConstants.START_ELEMENT) {
            Message message = new Message("STREAM_BAD_POSITION", LOG);
            throw new DatabindingException(message.toString());
        }
    }

    /** {@inheritDoc}*/
    public Object read(XMLStreamReader reader) throws Exception {
        return read(reader, null);
    }
    
    /** {@inheritDoc}*/
    public Object read(XMLStreamReader reader, AegisType desiredType) throws Exception {
        setupReaderPosition(reader);
        ElementReader elReader = new ElementReader(reader);

        if (elReader.isXsiNil()) {
            elReader.readToEnd();
            return null;
        }
        
        AegisType type = TypeUtil.getReadTypeStandalone(reader, aegisContext, desiredType);
        
        if (type == null) {
            throw new DatabindingException(new Message("NO_MAPPING", LOG));
        }

        return type.readObject(elReader, context);
    }

    public Object readFlatArray(XMLStreamReader input, 
                                ArrayType arrayType, QName concreteName) throws Exception {
        setupReaderPosition(input);
        ElementReader elReader = new ElementReader(input);
        return arrayType.readObject(elReader, concreteName, context);
        
    }
}
