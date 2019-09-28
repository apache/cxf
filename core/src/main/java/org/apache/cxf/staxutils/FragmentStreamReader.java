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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Wraps a XMLStreamReader and provides optional START_DOCUMENT and END_DOCUMENT events.
 */
public class FragmentStreamReader extends DepthXMLStreamReader {
    private boolean startElement;
    private boolean middle = true;
    private boolean endDoc;
    private boolean doDocEvents = true;

    private int depth;
    private int current = XMLStreamConstants.START_DOCUMENT;
    private boolean filter = true;
    private boolean advanceAtEnd = true;

    public FragmentStreamReader(XMLStreamReader reader) {
        super(reader);
    }
    public FragmentStreamReader(XMLStreamReader reader, boolean doDocEvents) {
        super(reader);
        this.doDocEvents = doDocEvents;
        if (!doDocEvents) {
            depth = getDepth();
            current = reader.getEventType();
            if (current != XMLStreamConstants.START_DOCUMENT) {
                startElement = true;
            }
        }
    }

    public int getEventType() {
        return current;
    }

    public boolean isCharacters() {
        return current == XMLStreamConstants.CHARACTERS;
    }

    public boolean isEndElement() {
        return current == XMLStreamConstants.END_ELEMENT;
    }

    public boolean isStartElement() {
        return current == XMLStreamConstants.START_ELEMENT;
    }

    public boolean isWhiteSpace() {
        return current == XMLStreamConstants.CHARACTERS && reader.isWhiteSpace();
    }

    public boolean hasNext() throws XMLStreamException {

        if (endDoc) {
            return false;
        }

        return reader.hasNext();
    }

    public final int next() throws XMLStreamException {
        if (!startElement) {
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
                if (!doDocEvents) {
                    endDoc = true;
                }
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
     * @param a
     */
    public void setAdvanceAtEnd(boolean a) {
        this.advanceAtEnd = a;
    }

}
