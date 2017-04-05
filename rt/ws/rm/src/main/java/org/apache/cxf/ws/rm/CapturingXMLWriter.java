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
package org.apache.cxf.ws.rm;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.transform.OutTransformWriter;

public class CapturingXMLWriter implements XMLStreamWriter {

    XMLStreamWriter delegate;
    XMLStreamWriter capture;
    LoadingByteArrayOutputStream bos = new LoadingByteArrayOutputStream();
    Throwable throwable;

    public CapturingXMLWriter(XMLStreamWriter del) {
        delegate = del;
        capture = StaxUtils.createXMLStreamWriter(bos, StandardCharsets.UTF_8.name());

        Map<String, String> map = new HashMap<>();
        map.put("{http://schemas.xmlsoap.org/ws/2005/02/rm}Sequence", "");
        map.put("{http://schemas.xmlsoap.org/ws/2005/02/rm}SequenceAcknowledgement", "");
        map.put("{http://docs.oasis-open.org/ws-rx/wsrm/200702}Sequence", "");
        map.put("{http://docs.oasis-open.org/ws-rx/wsrm/200702}SequenceAcknowledgement", "");

        capture = new OutTransformWriter(capture,
                                         map,
                                         Collections.<String, String>emptyMap(),
                                         Collections.<String>emptyList(),
                                         false,
                                         null);
    }

    public void setDefaultNamespace(String uri) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.setDefaultNamespace(uri);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.setDefaultNamespace(uri);
    }
    public void setNamespaceContext(NamespaceContext ctx) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.setNamespaceContext(ctx);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.setNamespaceContext(ctx);
    }
    public void setPrefix(String pfx, String uri) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.setPrefix(pfx, uri);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.setPrefix(pfx, uri);
    }

    public void writeAttribute(String prefix, String uri,
                               String local, String value) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeAttribute(prefix, uri, local, value);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeAttribute(prefix, uri, local, value);
    }

    public void writeAttribute(String uri, String local, String value) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeAttribute(uri, local, value);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeAttribute(uri, local, value);
    }

    public void writeAttribute(String local, String value) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeAttribute(local, value);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeAttribute(local, value);
    }

    public void writeCData(String cdata) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeCData(cdata);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeCData(cdata);
    }

    public void writeCharacters(char[] arg0, int arg1, int arg2) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeCharacters(arg0, arg1, arg2);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeCharacters(arg0, arg1, arg2);
    }

    public void writeCharacters(String text) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeCharacters(text);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeCharacters(text);
    }

    public void writeComment(String text) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeComment(text);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeComment(text);
    }

    public void writeDefaultNamespace(String uri) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeDefaultNamespace(uri);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeDefaultNamespace(uri);
    }

    public void writeDTD(String dtd) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeDTD(dtd);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeDTD(dtd);
    }

    public void writeEmptyElement(String prefix, String local, String uri) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeEmptyElement(prefix, local, uri);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeEmptyElement(prefix, local, uri);
    }

    public void writeEmptyElement(String uri, String local) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeEmptyElement(uri, local);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeEmptyElement(uri, local);
    }

    public void writeEmptyElement(String localName) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeEmptyElement(localName);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeEmptyElement(localName);
    }

    public void writeEndDocument() throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeEndDocument();
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeEndDocument();
    }

    public void writeEndElement() throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeEndElement();
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeEndElement();
    }

    public void writeEntityRef(String ent) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeEntityRef(ent);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeEntityRef(ent);
    }

    public void writeNamespace(String prefix, String uri) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeNamespace(prefix, uri);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeNamespace(prefix, uri);
    }

    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeProcessingInstruction(target, data);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeProcessingInstruction(target, data);
    }
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeProcessingInstruction(target);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeProcessingInstruction(target);
    }

    public void writeStartDocument() throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeStartDocument();
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeStartDocument();
    }

    public void writeStartDocument(String encoding, String ver) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeStartDocument(encoding, ver);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeStartDocument(encoding, ver);
    }

    public void writeStartDocument(String ver) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeStartDocument(ver);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeStartDocument(ver);
    }

    public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeStartElement(prefix, local, uri);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeStartElement(prefix, local, uri);
    }

    public void writeStartElement(String uri, String local) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeStartElement(uri, local);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeStartElement(uri, local);
    }

    public void writeStartElement(String local) throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.writeStartElement(local);
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
        capture.writeStartElement(local);
    }


    public void close() throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.close();
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
    }

    public void flush() throws XMLStreamException {
        if (delegate != null) {
            try {
                delegate.flush();
            } catch (Throwable t) {
                stopToDelegate(t);
            }
        }
    }

    public String getPrefix(String uri) throws XMLStreamException {
        if (delegate != null) {
            return delegate.getPrefix(uri);
        }
        return capture.getPrefix(uri);
    }

    public NamespaceContext getNamespaceContext() {
        if (delegate != null) {
            return delegate.getNamespaceContext();
        }
        return capture.getNamespaceContext();
    }
    public Object getProperty(String name) throws IllegalArgumentException {
        if (delegate != null) {
            return delegate.getProperty(name);
        }
        return capture.getProperty(name);
    }

    public LoadingByteArrayOutputStream getOutputStream() throws XMLStreamException {
        capture.flush();
        capture.close();
        return bos;
    }
    public Throwable getThrowable() {
        return throwable;
    }

    //if there is some problem writing to the original output, we need to stop writing
    // to the output, but keep capturing the message so we can try and resend later
    private void stopToDelegate(Throwable t) {
        if (throwable == null) {
            throwable = t;
            delegate = null;
        }
    }


}