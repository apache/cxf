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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


/**
 * Abstract logic for creating XMLStreamReader from DOM documents. Its works
 * using adapters for Element, Node and Attribute (
 * 
 * @see ElementAdapter }
 * @author <a href="mailto:tsztelak@gmail.com">Tomasz Sztelak</a>
 */
public abstract class AbstractDOMStreamReader<T, I> implements XMLStreamReader {
    protected int currentEvent = XMLStreamConstants.START_DOCUMENT;

    private Map properties = new HashMap();

    private FastStack<ElementFrame<T, I>> frames = new FastStack<ElementFrame<T, I>>();

    private ElementFrame<T, I> frame;

    
    /**
     *     
     */
    public static class ElementFrame<T, I> {
        T element;
        I currentChild;

        boolean started;
        boolean ended;
        
        int currentAttribute = -1;
        int currentNamespace = -1;

        List<String> uris;
        List<String> prefixes;
        List<Object> attributes;
        List<Object> allAttributes;

        final ElementFrame<T, I> parent;
        
        public ElementFrame(T element, ElementFrame<T, I> parent) {
            this.element = element;
            this.parent = parent;
        }
        
        public ElementFrame(T element, ElementFrame<T, I> parent, I ch) {
            this.element = element;
            this.parent = parent;
            this.currentChild = ch;
        }
        public ElementFrame(T doc, boolean s) {
            this.element = doc;
            parent = null;
            started = s;
            attributes = Collections.emptyList();
            prefixes = Collections.emptyList();
            uris = Collections.emptyList();
            allAttributes = Collections.emptyList();
        }
        public ElementFrame(T doc) {
            this(doc, true);
        }        
        public T getElement() {
            return element;
        }

        public I getCurrentChild() {
            return currentChild;
        }
        public void setCurrentChild(I o) {
            currentChild = o;
        }
        public boolean isDocument() {
            return false;
        }
        public boolean isDocumentFragment() {
            return false;
        }
    }

    /**
     * @param element
     */
    public AbstractDOMStreamReader(ElementFrame<T, I> frame) {
        this.frame = frame;
        frames.push(this.frame);
    }

    protected ElementFrame<T, I> getCurrentFrame() {
        return frame;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.stream.XMLStreamReader#getProperty(java.lang.String)
     */
    public Object getProperty(String key) throws IllegalArgumentException {
        return properties.get(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.stream.XMLStreamReader#next()
     */
    public int next() throws XMLStreamException {
        if (frame.ended) {
            frames.pop();
            if (!frames.empty()) {
                frame = frames.peek();
            } else {
                currentEvent = END_DOCUMENT;
                return currentEvent;
            }
        }

        if (!frame.started) {
            frame.started = true;
            currentEvent = frame.isDocument() ? START_DOCUMENT : START_ELEMENT;
        } else if (frame.currentAttribute < getAttributeCount() - 1) {
            frame.currentAttribute++;
            currentEvent = ATTRIBUTE;
        } else if (frame.currentNamespace < getNamespaceCount() - 1) {
            frame.currentNamespace++;
            currentEvent = NAMESPACE;
        } else if (hasMoreChildren()) {
            currentEvent = nextChild();

            if (currentEvent == START_ELEMENT) {
                ElementFrame<T, I> newFrame = getChildFrame();
                newFrame.started = true;
                frame = newFrame;
                frames.push(this.frame);
                currentEvent = START_ELEMENT;

                newFrame(newFrame);
            }
        } else {
            frame.ended = true;
            if (frame.isDocument()) {
                currentEvent = END_DOCUMENT;                
            } else {
                currentEvent = END_ELEMENT;
                endElement();
            }
        }
        return currentEvent;
    }

    protected void newFrame(ElementFrame<T, I> newFrame) {
    }

    protected void endElement() {
    }

    protected abstract boolean hasMoreChildren();
    protected abstract int nextChild();
    protected abstract ElementFrame<T, I> getChildFrame();

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.stream.XMLStreamReader#require(int, java.lang.String,
     *      java.lang.String)
     */
    public void require(int arg0, String arg1, String arg2) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.stream.XMLStreamReader#getElementText()
     */
    public abstract String getElementText() throws XMLStreamException;

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.stream.XMLStreamReader#nextTag()
     */
    public int nextTag() throws XMLStreamException {
        while (hasNext()) {
            if (START_ELEMENT == next()) {
                return START_ELEMENT;
            }
        }

        return currentEvent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.stream.XMLStreamReader#hasNext()
     */
    public boolean hasNext() throws XMLStreamException {
        
        return !(frame.ended && (frames.size() == 0 || frame.isDocumentFragment()));

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.stream.XMLStreamReader#close()
     */
    public void close() throws XMLStreamException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.stream.XMLStreamReader#getNamespaceURI(java.lang.String)
     */
    public abstract String getNamespaceURI(String prefix);

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.stream.XMLStreamReader#isStartElement()
     */
    public boolean isStartElement() {
        return currentEvent == START_ELEMENT;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.stream.XMLStreamReader#isEndElement()
     */
    public boolean isEndElement() {
        return currentEvent == END_ELEMENT;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.stream.XMLStreamReader#isCharacters()
     */
    public boolean isCharacters() {
        return currentEvent == CHARACTERS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.xml.stream.XMLStreamReader#isWhiteSpace()
     */
    public boolean isWhiteSpace() {
        if (currentEvent == CHARACTERS || currentEvent == CDATA) {
            String text = getText();
            int len = text.length();
            for (int i = 0; i < len; ++i) {
                if (text.charAt(i) > 0x0020) {
                    return false;
                }
            }
            return true;
        }
        return currentEvent == SPACE;
    }

    public int getEventType() {
        return currentEvent;
    }

    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
        throws XMLStreamException {
        char[] src = getText().toCharArray();

        if (sourceStart + length >= src.length) {
            length = src.length - sourceStart;
        }

        for (int i = 0; i < length; i++) {
            target[targetStart + i] = src[i + sourceStart];
        }

        return length;
    }

    public boolean hasText() {
        return currentEvent == CHARACTERS || currentEvent == DTD || currentEvent == ENTITY_REFERENCE
                || currentEvent == COMMENT || currentEvent == SPACE;
    }

    public Location getLocation() {
        return new Location() {

            public int getCharacterOffset() {
                return 0;
            }

            public int getColumnNumber() {
                return 0;
            }

            public int getLineNumber() {
                return 0;
            }

            public String getPublicId() {
                return null;
            }

            public String getSystemId() {
                return null;
            }

        };
    }

    public boolean hasName() {
        return currentEvent == START_ELEMENT || currentEvent == END_ELEMENT;
    }

    public String getVersion() {
        return null;
    }

    public boolean isStandalone() {
        return false;
    }

    public boolean standaloneSet() {
        // TODO Auto-generated method stub
        return false;
    }

    public String getCharacterEncodingScheme() {
        // TODO Auto-generated method stub
        return null;
    }
}
