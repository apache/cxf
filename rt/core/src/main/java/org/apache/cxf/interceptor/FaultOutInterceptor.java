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

package org.apache.cxf.interceptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.frontend.FaultInfoException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

public class FaultOutInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getL7dLogger(FaultOutInterceptor.class); 

    public FaultOutInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }

    public void handleMessage(Message message) throws Fault {
        Fault f = (Fault)message.getContent(Exception.class);

        Throwable cause = f.getCause();
        if (cause == null) {
            return;
        }
        
        BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
        if (bop == null) {
            return;
        }
        FaultInfo fi = getFaultForClass(bop, cause.getClass());

        if (cause instanceof Exception && fi != null) {
            Exception ex = (Exception)cause;
            Object bean = getFaultBean(cause, fi, message);
            Service service = message.getExchange().get(Service.class);

            MessagePartInfo part = fi.getMessageParts().iterator().next();
            DataBinding db = service.getDataBinding();

            try {
                if (isDOMSupported(db)) {
                    DataWriter<Node> writer = db.createWriter(Node.class);

                    if (f.hasDetails()) {
                        writer.write(bean, part, f.getDetail());
                    } else {
                        writer.write(bean, part, f.getOrCreateDetail());
                        if (!f.getDetail().hasChildNodes()) {
                            f.setDetail(null);
                        }
                    }
                } else {
                    if (f.hasDetails()) {
                        XMLStreamWriter xsw = new W3CDOMStreamWriter(f.getDetail());
                        DataWriter<XMLStreamWriter> writer = db.createWriter(XMLStreamWriter.class);
                        writer.write(bean, part, xsw);
                    } else {
                        XMLStreamWriter xsw = new W3CDOMStreamWriter(f.getOrCreateDetail());
                        DataWriter<XMLStreamWriter> writer = db.createWriter(XMLStreamWriter.class);
                        writer.write(bean, part, xsw);
                        if (!f.getDetail().hasChildNodes()) {
                            f.setDetail(null);
                        }
                    }
                }
    
                f.setMessage(ex.getMessage());
            } catch (Exception fex) {
                //ignore - if any exceptions occur here, we'll ignore them
                //and let the default fault handling of the binding convert 
                //the fault like it was an unchecked exception.
                LOG.log(Level.WARNING, "EXCEPTION_WHILE_WRITING_FAULT", fex);
            }
        }
    }

    private boolean isDOMSupported(DataBinding db) {
        boolean supportsDOM = false;
        for (Class c : db.getSupportedWriterFormats()) {
            if (c.equals(Node.class)) {
                supportsDOM = true;
            }
        }
        return supportsDOM;
    }

    protected Object getFaultBean(Throwable cause, FaultInfo faultPart, Message message) {
        if (FaultInfoException.class.isAssignableFrom(cause.getClass())) {
            try {
                Method method = cause.getClass().getMethod("getFaultInfo", new Class[0]);
                return method.invoke(cause, new Object[0]);
            } catch (InvocationTargetException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("INVOKE_FAULT_INFO", LOG), e);
            } catch (NoSuchMethodException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("NO_FAULT_INFO_METHOD", LOG), e);
            } catch (Exception e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("NO_ACCCESS_FAULT_INFO", LOG), e);
            }
        }
        return cause;
    }

    /**
     * Find the correct Fault part for a particular exception.
     * 
     * @param op
     * @param class1
     * @return
     */
    public FaultInfo getFaultForClass(BindingOperationInfo op, Class class1) {
        for (BindingFaultInfo bfi : op.getFaults()) {

            FaultInfo faultInfo = bfi.getFaultInfo();
            Class<?> c = (Class)faultInfo.getProperty(Class.class.getName());
            if (c.isAssignableFrom(class1)) {
                return faultInfo;
            }
        }

        return null;
    }
}
