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

package org.apache.cxf.jaxb;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.validation.Schema;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.annotation.XmlAttachmentRef;
import jakarta.xml.bind.annotation.XmlList;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.attachment.AttachmentMarshaller;
import jakarta.xml.bind.attachment.AttachmentUnmarshaller;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxb.attachment.JAXBAttachmentMarshaller;
import org.apache.cxf.jaxb.attachment.JAXBAttachmentUnmarshaller;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.AbstractMessageContainer;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;

/**
 *
 */
public abstract class JAXBDataBase {
    static final Logger LOG = LogUtils.getL7dLogger(JAXBDataBase.class);

    protected JAXBContext context;
    protected Schema schema;
    protected Collection<Attachment> attachments;
    protected Integer mtomThreshold; // null if we should default.
    protected boolean mtomEnabled;
    
    protected JAXBDataBase(JAXBContext ctx) {
        context = ctx;
    }

    public void setSchema(Schema s) {
        this.schema = s;
    }

    public void setJAXBContext(JAXBContext jc) {
        this.context = jc;
    }

    public Schema getSchema() {
        return schema;
    }
    public JAXBContext getJAXBContext() {
        return context;
    }

    public Collection<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Collection<Attachment> attachments) {
        this.attachments = attachments;
    }

    protected AttachmentUnmarshaller getAttachmentUnmarshaller() {
        return new JAXBAttachmentUnmarshaller(attachments);
    }

    protected AttachmentMarshaller getAttachmentMarshaller() {
        return new JAXBAttachmentMarshaller(attachments, mtomThreshold, mtomEnabled);
    }

    public void setProperty(String prop, Object value) {
    }

    protected Annotation[] getJAXBAnnotation(MessagePartInfo mpi) {
        List<Annotation> annoList = null;
        if (mpi != null) {
            annoList = extractJAXBAnnotations((Annotation[])mpi.getProperty("parameter.annotations"));
            if (annoList == null) {
                annoList = extractJAXBAnnotations(getReturnMethodAnnotations(mpi));
            }
        }
        return annoList == null ? new Annotation[0] : annoList.toArray(new Annotation[0]);
    }

    private List<Annotation> extractJAXBAnnotations(Annotation[] anns) {
        List<Annotation> annoList = null;
        if (anns != null) {
            for (Annotation ann : anns) {
                if (ann instanceof XmlList || ann instanceof XmlAttachmentRef
                    || ann instanceof XmlJavaTypeAdapter) {
                    if (annoList == null) {
                        annoList = new ArrayList<>();
                    }
                    annoList.add(ann);
                }
            }
        }
        return annoList;
    }

    private Annotation[] getReturnMethodAnnotations(MessagePartInfo mpi) {
        AbstractMessageContainer mi = mpi.getMessageInfo();
        if (mi == null || !isOutputMessage(mi)) {
            return null;
        }
        OperationInfo oi = mi.getOperation();
        return oi != null ? (Annotation[])oi.getProperty("method.return.annotations") : null;
    }

    protected boolean isOutputMessage(AbstractMessageContainer messageContainer) {
        if (messageContainer instanceof MessageInfo) {
            return MessageInfo.Type.OUTPUT == ((MessageInfo)messageContainer).getType();
        }
        return false;
    }

    public Integer getMtomThreshold() {
        return mtomThreshold;
    }

    public void setMtomThreshold(Integer threshold) {
        this.mtomThreshold = threshold;
    }

    protected final boolean honorJAXBAnnotations(MessagePartInfo part) {
        if (part == null) {
            return false;
        }
        if (!part.isElement()) {
            //RPC-Lit always needs to look for these
            return true;
        }
        //certain cases that use XmlJavaTypeAdapters will require this and the
        //JAXBSchemaInitializer will set this.
        Boolean b = (Boolean)part.getProperty("honor.jaxb.annotations");
        return b != null && b;
    }

    protected ValidationEventHandler getValidationEventHandler(String cn) {
        try {
            return (ValidationEventHandler)ClassLoaderUtils.loadClass(cn, getClass())
                .getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException 
            | IllegalArgumentException | InvocationTargetException | NoSuchMethodException 
            | SecurityException e) {
            LOG.log(Level.INFO, "Could not create validation event handler", e);
        }
        return null;
    }

    protected ValidationEventHandler getValidationEventHandler(Message m, String property) {
        Object value = m.getContextualProperty(property);
        ValidationEventHandler veventHandler;
        if (value instanceof String) {
            veventHandler = getValidationEventHandler((String)value);
        } else {
            veventHandler = (ValidationEventHandler)value;
        }
        if (veventHandler == null) {
            value = m.getContextualProperty(JAXBDataBinding.VALIDATION_EVENT_HANDLER);
            if (value instanceof String) {
                veventHandler = getValidationEventHandler((String)value);
            } else {
                veventHandler = (ValidationEventHandler)value;
            }
        }
        return veventHandler;
    }


}
