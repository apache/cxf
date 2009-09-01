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

import org.w3c.dom.Element;

import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.staxutils.W3CDOMStreamReader;

public class AegisElementDataReader extends AbstractAegisIoImpl 
                                    implements AegisReader<Element> {
    protected AegisXMLStreamDataReader reader;

    public AegisElementDataReader(AegisContext globalContext) {
        super(globalContext);
        reader = new AegisXMLStreamDataReader(globalContext, context);
    }

    /**
     * Convert a DOM element to a type.
     * @param input
     * @return
     */
    public Object read(Element input) throws Exception {
        W3CDOMStreamReader sreader = new W3CDOMStreamReader(input);
        sreader.nextTag(); //advance into the first tag
        return reader.read(sreader);
    }
    
    public Object read(Element input, AegisType desiredType) throws Exception {
        W3CDOMStreamReader sreader = new W3CDOMStreamReader(input);
        sreader.nextTag(); //advance into the first tag
        return reader.read(sreader, desiredType);
    }
}
