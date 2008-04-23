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
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;

import com.sun.xml.bind.api.TypeReference;

import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.jaxb.JAXBDataBase;
import org.apache.cxf.jaxb.JAXBEncoderDecoder;
import org.apache.cxf.service.model.MessagePartInfo;

public class DataReaderImpl<T> extends JAXBDataBase implements DataReader<T> {
    Set<Class<?>> contextClasses;
    public DataReaderImpl(JAXBContext ctx, Set<Class<?>> contextClasses) {
        super(ctx);
        this.contextClasses = contextClasses;
    }

    public Object read(T input) {
        return read(null, input);
    }

    public Object read(MessagePartInfo part, T reader) {
        boolean honorJaxbAnnotation = false;
        if (part != null && part.getProperty("honor.jaxb.annotations") != null) {
            honorJaxbAnnotation = (Boolean)part.getProperty("honor.jaxb.annotations");
        }
        Annotation[] anns = getJAXBAnnotation(part);
        if (honorJaxbAnnotation && anns.length > 0) {
            //RpcLit will use the JAXB Bridge to unmarshall part message when it is 
            //annotated with @XmlList,@XmlAttachmentRef,@XmlJavaTypeAdapter
            //TODO:Cache the JAXBRIContext
            QName qname = new QName(null, part.getConcreteName().getLocalPart());
            TypeReference typeReference = new TypeReference(qname, part.getTypeClass(), anns);
            return JAXBEncoderDecoder.unmarshalWithBridge(typeReference, 
                                                          contextClasses,
                                                          reader,
                                                          getAttachmentUnmarshaller());
        }
        
        return JAXBEncoderDecoder.unmarshall(getJAXBContext(), getSchema(), reader, part, 
                                             getAttachmentUnmarshaller(), unwrapJAXBElement);
    }

    public Object read(QName name, T input, Class type) {
        return JAXBEncoderDecoder.unmarshall(getJAXBContext(), getSchema(), input, name, type, 
                                             getAttachmentUnmarshaller(), unwrapJAXBElement);
    }

}
