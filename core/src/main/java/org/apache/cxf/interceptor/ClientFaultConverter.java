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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.W3CDOMStreamReader;

/**
 * Takes a Fault and converts it to a local exception type if possible.
 */
public class ClientFaultConverter extends AbstractInDatabindingInterceptor {
    public static final String DISABLE_FAULT_MAPPING = "disable-fault-mapping";
    public static final Pattern CAUSE_SUFFIX_SPLITTER
        = Pattern.compile(Message.EXCEPTION_CAUSE_SUFFIX, Pattern.LITERAL | Pattern.MULTILINE);
    private static final Logger LOG = LogUtils.getLogger(ClientFaultConverter.class);

    public ClientFaultConverter() {
        super(Phase.UNMARSHAL);
    }
    public ClientFaultConverter(String phase) {
        super(phase);
    }

    public void handleMessage(Message msg) {
        Fault fault = (Fault) msg.getContent(Exception.class);

        if (fault.getDetail() != null
            && !MessageUtils.getContextualBoolean(msg,
                                                 DISABLE_FAULT_MAPPING,
                                                 false)) {
            processFaultDetail(fault, msg);
            setStackTrace(fault, msg);
        }

        FaultMode faultMode = FaultMode.UNCHECKED_APPLICATION_FAULT;

        // Check if the raised exception is declared in the WSDL or by the JAX-RS resource
        Method m = msg.getExchange().get(Method.class);
        if (m != null) {
            Exception e = msg.getContent(Exception.class);
            for (Class<?> cl : m.getExceptionTypes()) {
                if (cl.isInstance(e)) {
                    faultMode = FaultMode.CHECKED_APPLICATION_FAULT;
                    break;
                }
            }
        }

        msg.getExchange().put(FaultMode.class, faultMode);
    }

    protected void processFaultDetail(Fault fault, Message msg) {
        Element exDetail = (Element) DOMUtils.getChild(fault.getDetail(), Node.ELEMENT_NODE);
        if (exDetail == null) {
            return;
        }
        QName qname = new QName(exDetail.getNamespaceURI(), exDetail.getLocalName());
        FaultInfo faultWanted = null;
        MessagePartInfo part = null;
        BindingOperationInfo boi = msg.getExchange().getBindingOperationInfo();
        if (boi == null) {
            return;
        }
        if (boi.isUnwrapped()) {
            boi = boi.getWrappedOperation();
        }
        for (FaultInfo faultInfo : boi.getOperationInfo().getFaults()) {
            for (MessagePartInfo mpi : faultInfo.getMessageParts()) {
                if (qname.equals(mpi.getConcreteName())) {
                    faultWanted = faultInfo;
                    part = mpi;
                    break;
                }
            }
            if (faultWanted != null) {
                break;
            }
        }
        if (faultWanted == null) {
            //did not find it using the proper qualified names, we'll try again with just the localpart
            for (FaultInfo faultInfo : boi.getOperationInfo().getFaults()) {
                for (MessagePartInfo mpi : faultInfo.getMessageParts()) {
                    if (qname.getLocalPart().equals(mpi.getConcreteName().getLocalPart())) {
                        faultWanted = faultInfo;
                        part = mpi;
                        break;
                    }
                }
                if (faultWanted != null) {
                    break;
                }
            }
        }
        if (faultWanted == null) {
            return;
        }
        Service s = msg.getExchange().getService();
        DataBinding dataBinding = s.getDataBinding();

        Object e;
        if (isDOMSupported(dataBinding)) {
            DataReader<Node> reader = this.getNodeDataReader(msg);
            reader.setProperty(DataReader.FAULT, fault);
            e = reader.read(part, exDetail);
        } else {
            DataReader<XMLStreamReader> reader = this.getDataReader(msg);
            XMLStreamReader xsr = new W3CDOMStreamReader(exDetail);
            try {
                xsr.nextTag();
            } catch (XMLStreamException e1) {
                throw new Fault(e1);
            }
            reader.setProperty(DataReader.FAULT, fault);
            e = reader.read(part, xsr);
        }

        if (!(e instanceof Exception)) {

            try {
                Class<?> exClass = faultWanted.getProperty(Class.class.getName(), Class.class);
                if (exClass == null) {
                    return;
                }
                if (e == null) {
                    Constructor<?> constructor = exClass.getConstructor(String.class);
                    e = constructor.newInstance(fault.getMessage());
                } else {

                    try {
                        Constructor<?> constructor = getConstructor(exClass, e);
                        e = constructor.newInstance(fault.getMessage(), e);
                    } catch (NoSuchMethodException e1) {
                        //Use reflection to convert fault bean to exception
                        e = convertFaultBean(exClass, e, fault);
                    }
                }
                msg.setContent(Exception.class, e);
            } catch (Exception e1) {
                LogUtils.log(LOG, Level.INFO, "EXCEPTION_WHILE_CREATING_EXCEPTION", e1, e1.getMessage());
            }
        } else {
            Exception ex = (Exception)e; 
            final String message = fault.getMessage();
            if (message != null) {
                Field f;
                try {
                    f = Throwable.class.getDeclaredField("detailMessage");
                    ReflectionUtil.setAccessible(f);
                    f.set(ex, fault.getMessage());
                } catch (Exception e1) {
                    if (isJdkException(ex.getClass().getPackageName())) {
                        ex = cloneJdkException(ex, message);
                    }
                }
            }
            msg.setContent(Exception.class, ex);
        }
    }

    private static Exception cloneJdkException(Exception ex, final String message) {
        try {
            // Fallback, try to clone the exception instead of accessing the detailMessage 
            // over reflection
            Constructor<? extends Object> constructor = ReflectionUtil.getConstructor(
                ex.getClass(), String.class, Throwable.class); /* String message, Throwable cause */
   
            Exception clone = null;
            if (constructor != null) {
                clone = (Exception) constructor.newInstance(message, ex.getCause());
            } else {
                constructor = ReflectionUtil.getConstructor(ex.getClass(), String.class); /* String message */
                if (constructor != null) {
                    clone = (Exception) constructor.newInstance(message);
                    clone.initCause(ex.getCause());
                }
            }

            if (clone != null) {
                clone.setStackTrace(ex.getStackTrace());
                if (ex.getSuppressed().length > 0) {
                    Arrays.stream(ex.getSuppressed()).forEach(clone::addSuppressed);
                }
                return clone;
            }
        } catch (Exception e2) {
            /* nothing to do */
        }
        return ex;
    }

    private Constructor<?> getConstructor(Class<?> faultClass, Object e) throws NoSuchMethodException {
        Class<?> beanClass = e.getClass();
        Constructor<?>[] cons = faultClass.getConstructors();
        for (Constructor<?> c : cons) {
            if (c.getParameterTypes().length == 2
                && String.class.equals(c.getParameterTypes()[0])
                && c.getParameterTypes()[1].isInstance(e)) {
                return c;
            }
        }
        try {
            return faultClass.getConstructor(String.class, beanClass);
        } catch (NoSuchMethodException ex) {
            Class<?> cls = getPrimitiveClass(beanClass);
            if (cls != null) {
                return faultClass.getConstructor(String.class, cls);
            }
            throw ex;
        }

    }

    private boolean isDOMSupported(DataBinding db) {
        boolean supportsDOM = false;
        for (Class<?> c : db.getSupportedReaderFormats()) {
            if (c.equals(Node.class)) {
                supportsDOM = true;
            }
        }
        return supportsDOM;
    }

    private void setStackTrace(Fault fault, Message msg) {
        Throwable cause = null;
        Map<String, String> ns = new HashMap<>();
        XPathUtils xu = new XPathUtils(ns);
        ns.put("s", Fault.STACKTRACE_NAMESPACE);
        String ss = (String) xu.getValue("//s:" + Fault.STACKTRACE + "/text()", fault.getDetail(),
                XPathConstants.STRING);
        List<StackTraceElement> stackTraceList = new ArrayList<>();
        if (!StringUtils.isEmpty(ss)) {
            Iterator<String> linesIterator = Arrays.asList(CAUSE_SUFFIX_SPLITTER.split(ss)).iterator();
            while (linesIterator.hasNext()) {
                String oneLine = linesIterator.next();
                if (oneLine.startsWith("Caused by:")) {
                    cause = getCause(linesIterator, oneLine);
                    break;
                }
                stackTraceList.add(parseStackTrackLine(oneLine));
            }
            if (!stackTraceList.isEmpty() || cause != null) {
                Exception e = msg.getContent(Exception.class);
                if (!stackTraceList.isEmpty()) {
                    StackTraceElement[] stackTraceElement = new StackTraceElement[stackTraceList.size()];
                    e.setStackTrace(stackTraceList.toArray(stackTraceElement));
                } else if (cause != null
                    && cause.getMessage() != null
                    && cause.getMessage().startsWith(e.getClass().getName())) {
                    e.setStackTrace(cause.getStackTrace());
                    if (cause.getCause() != null) {
                        e.initCause(cause.getCause());
                    }
                } else if (cause != null) {
                    e.initCause(cause);
                }
            }
        }

    }

    // recursively parse the causes and instantiate corresponding throwables
    private Throwable getCause(Iterator<String> linesIterator, String firstLine) {
        // The actual exception class of the cause might be unavailable at the
        // client -> use a standard throwable to represent the cause.
        firstLine = firstLine.substring(firstLine.indexOf(':') + 1).trim();
        Throwable res = null;
        if (firstLine.indexOf(':') != -1) {
            String cn = firstLine.substring(0, firstLine.indexOf(':')).trim();
            if (isJdkException(cn)) {
                try {
                    res = (Throwable)Class.forName(cn).getConstructor(String.class)
                            .newInstance(firstLine.substring(firstLine.indexOf(':') + 2));
                } catch (Throwable t) {
                    //ignore, use the default
                }
            }
        }
        if (res == null) {
            res = new Throwable(firstLine);
        }
        List<StackTraceElement> stackTraceList = new ArrayList<>();
        while (linesIterator.hasNext()) {
            String oneLine = linesIterator.next();
            if (oneLine.startsWith("Caused by:")) {
                Throwable nestedCause = getCause(linesIterator, oneLine);
                res.initCause(nestedCause);
                break;
            }
            stackTraceList.add(parseStackTrackLine(oneLine));
        }
        StackTraceElement[] stackTraceElement = new StackTraceElement[stackTraceList.size()];
        res.setStackTrace(stackTraceList.toArray(stackTraceElement));
        return res;
    }

    private static boolean isJdkException(String pkg) {
        return pkg.startsWith("java.lang");
    }

    private static StackTraceElement parseStackTrackLine(String oneLine) {
        StringTokenizer stInner = new StringTokenizer(oneLine, "!");
        return new StackTraceElement(stInner.nextToken(), stInner.nextToken(),
                stInner.nextToken(), Integer.parseInt(stInner.nextToken()));
    }

    private Class<?> getPrimitiveClass(Class<?> cls) {
        if (cls.isPrimitive()) {
            return cls;
        }
        try {
            Field field = cls.getField("TYPE");
            Object obj = cls;
            Object type = field.get(obj);
            if (type instanceof Class) {
                return (Class<?>)type;
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }

    private Exception convertFaultBean(Class<?> exClass, Object faultBean, Fault fault) throws Exception {
        Constructor<?> constructor = exClass.getConstructor(String.class);
        Exception e = (Exception)constructor.newInstance(fault.getMessage());

        //Copy fault bean fields to exception
        for (Class<?> obj = exClass; !obj.equals(Object.class);  obj = obj.getSuperclass()) {
            Field[] fields = obj.getDeclaredFields();
            for (Field f : fields) {
                try {
                    Field beanField = faultBean.getClass().getDeclaredField(f.getName());
                    ReflectionUtil.setAccessible(beanField);
                    ReflectionUtil.setAccessible(f);
                    f.set(e, beanField.get(faultBean));
                } catch (NoSuchFieldException e1) {
                    //do nothing
                }
            }
        }
        //also use/try public getter/setter methods
        Method[] meth = faultBean.getClass().getMethods();
        for (Method m : meth) {
            if (m.getParameterTypes().length == 0
                && (m.getName().startsWith("get")
                || m.getName().startsWith("is"))) {
                try {
                    String name;
                    if (m.getName().startsWith("get")) {
                        name = "set" + m.getName().substring(3);
                    } else {
                        name = "set" + m.getName().substring(2);
                    }
                    Method m2 = exClass.getMethod(name, m.getReturnType());
                    m2.invoke(e, m.invoke(faultBean));
                } catch (Exception e1) {
                    //ignore
                }
            }
        }


        return e;
    }
}
