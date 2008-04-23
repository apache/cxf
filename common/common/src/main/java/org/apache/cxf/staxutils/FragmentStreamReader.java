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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Wraps a XMLStreamReader and provides START_DOCUMENT and END_DOCUMENT events.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class FragmentStreamReader extends DepthXMLStreamReader {
    private boolean startDoc;
    private boolean startElement;
    private boolean middle = true;
    private boolean endDoc;

    private int depth;
    private int current = -1;
    private boolean filter = true;
    private boolean advanceAtEnd = true;
    
    public FragmentStreamReader(XMLStreamReader reader) {
        super(reader);
    }    
   
    public int getEventType() {
        return current;
    }

    public boolean hasNext() throws XMLStreamException {
        if (!startDoc) {
            return true;
        }
        
        if (endDoc) {
            return false;
        }
        
        return reader.hasNext();
    }
    
    public int next() throws XMLStreamException {
        if (!startDoc) {
            startDoc = true;
            current = START_DOCUMENT;
        } else if (!startElement) {
            depth = getDepth();
            
            current = reader.getEventType();
            
            if (filter) {
                while (current != START_ELEMENT && depth >= getDepth() && super.hasNext()) {
                    current = super.next();
                }
                
                filter = false;
            }
            
            startElement = true;
            current = START_ELEMENT;
        } else if (middle) {
            current = super.next();

            if (current == END_ELEMENT && getDepth() < depth) {
                middle = false;
            }
        } else if (!endDoc) {
            // Move past the END_ELEMENT token.
            if (advanceAtEnd) {
                super.next();
            }
            
            endDoc = true;
            current = END_DOCUMENT;
        } else {
            throw new XMLStreamException("Already at the end of the document.");
        }

        return current;
    }

    public boolean isAdvanceAtEnd() {
        return advanceAtEnd;
    }

    /**
     * Set whether or not the FragmentStreamReader should move past the END_ELEMENT
     * when it is done parsing.
     * @param advanceAtEnd
     */
    public void setAdvanceAtEnd(boolean a) {
        this.advanceAtEnd = a;
    }    

}
