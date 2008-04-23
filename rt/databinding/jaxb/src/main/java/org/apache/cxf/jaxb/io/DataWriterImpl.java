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

package org.apache.cxf.jaxb.io;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;

import com.sun.xml.bind.api.TypeReference;

import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.jaxb.JAXBDataBase;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxb.JAXBEncoderDecoder;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.ws.commons.schema.XmlSchemaElement;

public class DataWriterImpl<T> extends JAXBDataBase implements DataWriter<T> {
    private Set<Class<?>> contextClasses;
    private Map<String, Object> marshallerProperties = Collections.emptyMap();
    
    public DataWriterImpl(JAXBContext ctx, Set<Class<?>> contextClasses) {
        super(ctx);
        this.contextClasses = contextClasses;
    }
    
    public DataWriterImpl(JAXBContext ctx, 
                          Map<String, Object> marshallerProperties,
                          Set<Class<?>> contextClasses) {
        super(ctx);
        this.marshallerProperties = marshallerProperties;
        this.contextClasses = contextClasses;
    }
    
    public void write(Object obj, T output) {
        write(obj, null, output);
    }
    
    public void write(Object obj, MessagePartInfo part, T output) {
        boolean honorJaxbAnnotation = false;
        if (part != null && part.getProperty("honor.jaxb.annotations") != null) {
            honorJaxbAnnotation = (Boolean)part.getProperty("honor.jaxb.annotations");
        }
        
        if (obj != null
            || !(part.getXmlSchema() instanceof XmlSchemaElement)) {
            
            if (obj instanceof Exception 
                && part != null
                && Boolean.TRUE.equals(part.getProperty(JAXBDataBinding.class.getName() 
                                                        + ".CUSTOM_EXCEPTION"))) {
                JAXBEncoderDecoder.marshallException(getJAXBContext(), getSchema(), (Exception)obj,
                                                     part, output, getAttachmentMarshaller(),
                                                     marshallerProperties);                
            } else {
                Annotation[] anns = getJAXBAnnotation(part);
                if (!honorJaxbAnnotation || anns.length == 0) {
                    JAXBEncoderDecoder.marshall(getJAXBContext(), getSchema(), obj, part, output,
                                                getAttachmentMarshaller(), marshallerProperties);
                } else if (honorJaxbAnnotation && anns.length > 0) {
                    //RpcLit will use the JAXB Bridge to marshall part message when it is 
                    //annotated with @XmlList,@XmlAttachmentRef,@XmlJavaTypeAdapter
                    //TODO:Cache the JAXBRIContext
                    QName qname = new QName(null, part.getConcreteName().getLocalPart());
                    TypeReference typeReference = new TypeReference(qname, part.getTypeClass(), anns);
                    JAXBEncoderDecoder.marshalWithBridge(typeReference, contextClasses, obj, 
                                                         output, getAttachmentMarshaller());
                }
            }
        } else if (obj == null && needToRender(obj, part)) {
            JAXBEncoderDecoder.marshallNullElement(getJAXBContext(), getSchema(), output, part,
                                                   marshallerProperties);
        }
    }

    private boolean needToRender(Object obj, MessagePartInfo part) {
        if (part != null && part.getXmlSchema() instanceof XmlSchemaElement) {
            XmlSchemaElement element = (XmlSchemaElement)part.getXmlSchema();
            return element.isNillable() && element.getMinOccurs() > 0;
        }
        return false;
    }

    public Map<String, Object> getMarshallerProperties() {
        return marshallerProperties;
    }

    public void setMarshallerProperties(Map<String, Object> marshallerProperties) {
        this.marshallerProperties = marshallerProperties;
    }
    

    
}
