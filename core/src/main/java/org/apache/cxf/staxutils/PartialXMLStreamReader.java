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

package org.apache.cxf.staxutils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Read from a StaX reader, stopping when the next element is a specified element.
 * For example, this can be used to read all of the Header of a soap message into a DOM
 * document stopping on contact with the body element.
 */
public class PartialXMLStreamReader extends DepthXMLStreamReader {
    private QName endTag;
    private boolean foundEnd;
    private int endDepth;
    private int currentEvent;
    
    public PartialXMLStreamReader(XMLStreamReader r, QName endTag) {
        super(r);
        this.endTag = endTag;
        currentEvent = r.getEventType();
    }

    @Override
    public int next() throws XMLStreamException {
        if (!foundEnd) { 
            currentEvent = super.next();

            if (currentEvent == START_ELEMENT && getName().equals(endTag)) {
                foundEnd = true;
                endDepth = getDepth();
                return START_ELEMENT;
            }
            
            return currentEvent;
        } else if (endDepth > 0) {
            endDepth--;
            currentEvent = END_ELEMENT;
        } else {
            currentEvent = END_DOCUMENT;
        }
        
        return currentEvent;
    }

    @Override
    public int getEventType() {
        return currentEvent;
    }

    @Override
    public boolean hasNext() {
        return currentEvent != END_DOCUMENT;
    }
    
    
}
