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
package org.apache.cxf.tools.java2wsdl.processor.internal.jaxws;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.WebService;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.Processor;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JavaException;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaParameter;
import org.apache.cxf.tools.common.model.JavaReturn;
import org.apache.cxf.tools.common.model.JavaType.Style;
import org.apache.cxf.tools.java2ws.util.JavaFirstUtil;
import org.apache.cxf.tools.java2wsdl.processor.internal.AntGenerator;
import org.apache.cxf.tools.java2wsdl.processor.internal.jaxws.generator.JaxwsClientGenerator;
import org.apache.cxf.tools.java2wsdl.processor.internal.jaxws.generator.JaxwsImplGenerator;
import org.apache.cxf.tools.java2wsdl.processor.internal.jaxws.generator.JaxwsSEIGenerator;
import org.apache.cxf.tools.java2wsdl.processor.internal.jaxws.generator.JaxwsServerGenerator;
import org.apache.cxf.tools.wsdlto.core.AbstractGenerator;

public class JAXWSFrontEndProcessor implements Processor {
    private static final String SEI_SUFFIX = "_PortType";
    private static final Logger LOG = LogUtils.getL7dLogger(JAXWSFrontEndProcessor.class);
    private ToolContext context;
    private List<AbstractGenerator> generators = new ArrayList<AbstractGenerator>();
    private List<String> infList = new ArrayList<String>();
    
    @SuppressWarnings("unchecked")
    public void process() throws ToolException {
        checkJaxwsClass();
        List<ServiceInfo> services = (List<ServiceInfo>)context.get(ToolConstants.SERVICE_LIST);
        ServiceInfo serviceInfo = services.get(0);
        JavaInterface jinf = JavaFirstUtil.serviceInfo2JavaInf(serviceInfo);
        String className = (String)context.get(ToolConstants.IMPL_CLASS);
        if (className != null && className.equals(jinf.getFullClassName())) {
            jinf.setName(jinf.getName() + SEI_SUFFIX);
        }
        JavaModel jm = new JavaModel();
        jm.addInterface("inf", jinf);
        jinf.setJavaModel(jm);
        context.put(JavaModel.class, jm);
        context.put(ToolConstants.SERVICE_NAME, serviceInfo.getName());
        EndpointInfo endpointInfo = serviceInfo.getEndpoints().iterator().next();        
        context.put(ToolConstants.PORT_NAME, endpointInfo.getName());
        generators.add(new JaxwsSEIGenerator());
        generators.add(new JaxwsImplGenerator());
        generators.add(new JaxwsServerGenerator());
        generators.add(new JaxwsClientGenerator());
        generators.add(new AntGenerator());

        for (AbstractGenerator generator : generators) {
            generator.generate(context);
        }

    }

    public void setEnvironment(ToolContext env) {
        this.context = env;
    }

    public JavaInterface serviceInfo2JavaInf(ServiceInfo service) {
        JavaInterface javaInf = new JavaInterface();
        InterfaceInfo inf = service.getInterface();
        for (OperationInfo op : inf.getOperations()) {
            JavaMethod jm = new JavaMethod();
            Method m = (Method)op.getProperty(ReflectionServiceFactoryBean.METHOD);
            jm.setName(m.getName());
            int i = 0;
            for (Type type : m.getGenericParameterTypes()) {
                JavaParameter jp = new JavaParameter();
                jp.setClassName(getClassName(type));
                jp.setStyle(Style.IN);
                jp.setName("arg" + i++);
                jm.addParameter(jp);
            }

            for (Type type : m.getGenericExceptionTypes()) {
                JavaException jex = new JavaException();
                String className = getClassName(type);
                jex.setClassName(className);
                jex.setName(className);
                jm.addException(jex);
            }

            JavaReturn jreturn = new JavaReturn();
            jreturn.setClassName(getClassName(m.getGenericReturnType()));
            jreturn.setStyle(Style.OUT);
            jm.setReturn(jreturn);

            String pkg = PackageUtils.getPackageName(m.getDeclaringClass());
            javaInf.setPackageName(pkg.length() > 0 ? pkg : ToolConstants.DEFAULT_PACKAGE_NAME);
            javaInf.addMethod(jm);
            javaInf.setName(inf.getName().getLocalPart());

            jm.getParameterList();

        }
        return javaInf;
    }

    public String getClassName(Type type) {
        if (type instanceof Class) {
            Class clz = (Class)type;
            if (clz.isArray()) {
                return clz.getComponentType().getName() + "[]";
            } else {
                return clz.getName();
            }
        } else if (type instanceof ParameterizedType) {
            return type.toString();
        } else if (type instanceof GenericArrayType) {
            return type.toString();
        }

        return "";
    }

    public void checkJaxwsClass() {
        Class<?> clz = context.get(Class.class);
        WebService webServiceAnno = (WebService)clz.getAnnotation(WebService.class);
        if (webServiceAnno == null) {
            Message msg = new Message("CLASS_DOESNOT_CARRY_WEBSERVICE_ANNO", LOG, clz.getName());
            LOG.log(Level.WARNING, msg.toString());
            throw new ToolException(msg);
        }
        if (isImplRmiRemote(clz)) {
            Message msg = new Message("PARA_OR_RETURN_IMPL_REMOTE", LOG, clz.getName());
            LOG.log(Level.WARNING, msg.toString());
            throw new ToolException(msg);
        }
    }
    
    
    private boolean isImplRmiRemote(Class claz) {
        for (Method method : claz.getMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())
                && !method.getDeclaringClass().getName().equals("java.lang.Object")) {
                Class[] paraClasses = method.getParameterTypes();
                for (Class clz : paraClasses) {
                    getInfClass(clz);
                }
                Class returnClass = method.getReturnType();
                getInfClass(returnClass);
            }
        }
        if (infList.contains("java.rmi.Remote")) {
            return true;
        }
        return false;
    }
    
    
    private void getInfClass(Class claz) {
        for (Class inf : claz.getInterfaces()) {
            getInfClass(inf);
        }
        if (claz.getSuperclass() != null) {
            getInfClass(claz.getSuperclass());
        }
        if (claz.isInterface()) {
            infList.add(claz.getName());
        }
    }
    
}
