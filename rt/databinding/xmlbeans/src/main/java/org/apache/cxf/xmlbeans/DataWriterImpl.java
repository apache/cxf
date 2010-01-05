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

package org.apache.cxf.xmlbeans;


import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlTokenSource;
import org.apache.xmlbeans.impl.values.XmlObjectBase;

public class DataWriterImpl implements DataWriter<XMLStreamWriter> {
    private static final Logger LOG = LogUtils.getLogger(XmlBeansDataBinding.class);
    private Schema schema;
    private Message message;
    
    public DataWriterImpl() {
    }
    
    public void write(Object obj, XMLStreamWriter output) {
        write(obj, null, output);
    }
    
    public void write(Object obj, MessagePartInfo part, XMLStreamWriter output) {
        try {
            Class<?> typeClass = part.getTypeClass();
            if (!XmlObject.class.isAssignableFrom(typeClass)) {
                typeClass = (Class<?>)part.getProperty(XmlAnySimpleType.class.getName());
                
                Class<?> cls[] = typeClass.getDeclaredClasses();
                for (Class<?> c : cls) {
                    if ("Factory".equals(c.getSimpleName())) {
                        try {
                            SchemaType st = (SchemaType)part.getProperty(SchemaType.class.getName());
                            XmlOptions options = new XmlOptions();
                            if (schema != null) {
                                options.setValidateOnSet();
                            }
                            if (!st.isDocumentType()) {
                                options.setLoadReplaceDocumentElement(null);
                            }
                            Method meth = c.getMethod("newValue", Object.class);
                            obj = meth.invoke(null, obj);
                            break;
                        } catch (Exception e) {
                            throw new Fault("UNMARSHAL_ERROR", LOG, e, part.getTypeClass());
                        }
                    }
                }
            }

            
            if (obj != null
                || !(part.getXmlSchema() instanceof XmlSchemaElement)) {
                XmlOptions options = new XmlOptions();
                if (schema != null) {
                    options.setValidateOnSet();
                }
                if (message != null 
                    && MessageUtils.getContextualBoolean(message, 
                                                      XmlBeansDataBinding.XMLBEANS_NAMESPACE_HACK, 
                                                      false)) {
                    Object dom;
                    if (obj instanceof XmlObjectBase) {
                        XmlObjectBase source = (XmlObjectBase)obj;
                        dom = source.newDomNode(options);
                    } else {
                        XmlTokenSource source = (XmlTokenSource)obj;
                        dom = source.newDomNode(options);
                    }
                    
                    if (dom instanceof Document) {
                        org.w3c.dom.Element e = ((Document)dom).getDocumentElement();
                        StaxUtils.copy(e, output);
                    } else if (dom instanceof DocumentFragment) {
                        DocumentFragment frag = (DocumentFragment) dom;
                        Node node = frag.getFirstChild();
                        while (node != null) {
                            if (node instanceof Element) {
                                StaxUtils.copy((Element)node, output);
                            } else if (node instanceof Comment) {
                                output.writeComment(((Comment)node).getData());
                            } else if (node instanceof Text) {
                                output.writeCharacters(((Text)node).getData());
                            }
                            node = node.getNextSibling();
                        }
                    } else {
                        throw new Fault("Invalid document type returned: " + dom.toString(), LOG);
                    }
                    return;
                }
                
                XMLStreamReader reader;
                if (obj instanceof XmlObjectBase) {
                    XmlObjectBase source = (XmlObjectBase)obj;
                    reader = source.newCursorForce().newXMLStreamReader(options);
                } else {
                    XmlTokenSource source = (XmlTokenSource)obj;
                    reader = source.newCursor().newXMLStreamReader(options);                    
                }
                SchemaType st = (SchemaType)part.getProperty(SchemaType.class.getName());
                int i = reader.getEventType();
                if (i == XMLStreamReader.START_DOCUMENT) {
                    i = reader.next();
                }
                
                if (st != null && !st.isDocumentType()
                    || reader.getEventType() == XMLStreamReader.CHARACTERS) {
                    if (StringUtils.isEmpty(part.getConcreteName().getNamespaceURI())) {
                        output.writeStartElement(part.getConcreteName().getLocalPart());
                        
                    } else {
                        String pfx = output.getPrefix(part.getConcreteName().getNamespaceURI());
                        if (StringUtils.isEmpty(pfx)) {
                            output.writeStartElement("tns",
                                             part.getConcreteName().getLocalPart(),
                                             part.getConcreteName().getNamespaceURI());
                            output.writeNamespace("tns", part.getConcreteName().getNamespaceURI());
                        } else {
                            output.writeStartElement(pfx,
                                                     part.getConcreteName().getLocalPart(),
                                                     part.getConcreteName().getNamespaceURI());
                        }
                    }
                    StaxUtils.copy(reader, output, true);
                    output.writeEndElement();
                } else {
                    StaxUtils.copy(reader, output, true);
                }
            } else if (obj == null && needToRender(obj, part)) {
                output.writeStartElement(part.getConcreteName().getNamespaceURI(),
                                         part.getConcreteName().getLocalPart());
                output.writeEndElement();
            }
        } catch (XMLStreamException e) {
            throw new Fault("MARSHAL_ERROR", LOG, e, obj);
        }
    }

    private boolean needToRender(Object obj, MessagePartInfo part) {
        if (part != null && part.getXmlSchema() instanceof XmlSchemaElement) {
            XmlSchemaElement element = (XmlSchemaElement)part.getXmlSchema();
            return element.isNillable() && element.getMinOccurs() > 0;
        }
        return false;
    }

    public void setAttachments(Collection<Attachment> attachments) {
    }

    public void setProperty(String key, Object value) {
        if (Message.class.getName().equals(key)) {
            message = (Message)value;
        }
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }
}
