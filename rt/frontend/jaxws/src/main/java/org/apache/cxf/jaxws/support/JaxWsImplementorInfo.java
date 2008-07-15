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

package org.apache.cxf.jaxws.support;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingType;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxb.JAXBEncoderDecoder;

public class JaxWsImplementorInfo {

    private static final Logger LOG = LogUtils.getL7dLogger(JaxWsImplementorInfo.class);
    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();

    private Class<?> implementorClass;
    private Class<?> seiClass;
    private List<WebService> wsAnnotations = new ArrayList<WebService>(2);
    private WebServiceProvider wsProviderAnnotation;

    public JaxWsImplementorInfo(Class<?> ic) {
        implementorClass = ic;
        initialize();
    }

    public Class<?> getSEIClass() {
        return seiClass;
    }

    public Class<?> getImplementorClass() {
        return implementorClass;
    }
    
    public Class<?> getEndpointClass() {
        Class endpointInterface = getSEIClass();
        if (null == endpointInterface) {
            endpointInterface = getImplementorClass();
        }
        return endpointInterface;
    }

    public String getWsdlLocation() {
        for (WebService service : wsAnnotations) {
            if (!StringUtils.isEmpty(service.wsdlLocation())) {
                return service.wsdlLocation();
            }
        }
        
        if (null != wsProviderAnnotation 
            && !StringUtils.isEmpty(wsProviderAnnotation.wsdlLocation())) {
            return wsProviderAnnotation.wsdlLocation();
        }
        return null;
    }

    /**
     * See use of targetNamespace in {@link WebService}.
     * 
     * @return the qualified name of the service.
     */
    public QName getServiceName() {
        String serviceName = null;
        String namespace = null;

        // serviceName cannot be specified on SEI so check impl class only
        if (wsAnnotations.size() > 0) {
            int offset = 1;
            if (seiClass == null) {
                offset = 0;
            }
            //traverse up the parent impl classes for this info as well, but
            //not the last one which would be the sei annotation
            for (int x = 0; x < wsAnnotations.size() - offset; x++) {
                if (StringUtils.isEmpty(serviceName)) {
                    serviceName = wsAnnotations.get(x).serviceName();
                }
                if (StringUtils.isEmpty(namespace)) {
                    namespace = wsAnnotations.get(x).targetNamespace();
                }
            }
        }
        
        if ((serviceName == null || namespace == null) 
            && wsProviderAnnotation != null) {
            serviceName = wsProviderAnnotation.serviceName();
            namespace = wsProviderAnnotation.targetNamespace();
        }

        if (StringUtils.isEmpty(serviceName)) {
            serviceName = implementorClass.getSimpleName() + "Service";
        }

        if (StringUtils.isEmpty(namespace)) {
            namespace = getDefaultNamespace(implementorClass);
        }

        return new QName(namespace, serviceName);
    }

    /**
     * See use of targetNamespace in {@link WebService}.
     * 
     * @return the qualified name of the endpoint.
     */
    public QName getEndpointName() {
        String portName = null;
        String namespace = null;
        String name = null;

        // portName cannot be specified on SEI so check impl class only
        if (wsAnnotations.size() > 0) {
            int offset = 1;
            if (seiClass == null) {
                offset = 0;
            }
            //traverse up the parent impl classes for this info as well, but
            //not the last one which would be the sei annotation
            for (int x = 0; x < wsAnnotations.size() - offset; x++) {
                if (StringUtils.isEmpty(portName)) {
                    portName = wsAnnotations.get(x).portName();
                }
                if (StringUtils.isEmpty(namespace)) {
                    namespace = wsAnnotations.get(x).targetNamespace();
                }
                if (StringUtils.isEmpty(name)) {
                    name = wsAnnotations.get(x).name();
                }
            }
        }

        if ((portName == null || namespace == null)
            && wsProviderAnnotation != null) {
            portName = wsProviderAnnotation.portName();
            namespace = wsProviderAnnotation.targetNamespace();
        }
        if (StringUtils.isEmpty(portName)
            && !StringUtils.isEmpty(name)) {
            portName = name + "Port";
        }
        if (StringUtils.isEmpty(portName)) {
            portName = implementorClass.getSimpleName() + "Port";
        }

        if (StringUtils.isEmpty(namespace)) {
            namespace = getDefaultNamespace(implementorClass);
        }

        return new QName(namespace, portName);
    }

    public QName getInterfaceName() {
        String name = null;
        String namespace = null;
        
        if (seiClass != null) {
            WebService service = seiClass.getAnnotation(WebService.class);
            if (!StringUtils.isEmpty(service.name())) {
                name = service.name();
            }
            if (!StringUtils.isEmpty(service.targetNamespace())) {
                namespace = service.targetNamespace();
            }
        } else {
            for (WebService service : wsAnnotations) {
                if (!StringUtils.isEmpty(service.name()) && name == null) {
                    name = service.name();
                }
                if (!StringUtils.isEmpty(service.targetNamespace()) && namespace == null) {
                    namespace = service.targetNamespace();
                }
            }
        }
        if (name == null) {
            if (seiClass != null) {
                name = seiClass.getSimpleName();
            } else if (implementorClass != null) {
                name = implementorClass.getSimpleName();
            }
        }
        if (namespace == null) {
            if (seiClass != null) {
                namespace = getDefaultNamespace(seiClass);
            } else if (implementorClass != null) {
                namespace = getDefaultNamespace(implementorClass);
            }
        }
        
        return new QName(namespace, name);
    }

    private String getDefaultNamespace(Class clazz) {
        String pkg = PackageUtils.getNamespace(PackageUtils.getPackageName(clazz));
        return StringUtils.isEmpty(pkg) ? "http://unknown.namespace/" : pkg;
    }
        
    private String getWSInterfaceName(Class<?> implClz) {
        if (implClz.isInterface() 
            && implClz.getAnnotation(WebService.class) != null) {
            return implClz.getName();
        }
        Class<?>[] clzs = implClz.getInterfaces();
        for (Class<?> clz : clzs) {
            if (null != clz.getAnnotation(WebService.class)) {
                return clz.getName();
            }
        }
        return null;
    }

    private String getImplementorClassName() {
        for (WebService service : wsAnnotations) {
            if (!StringUtils.isEmpty(service.endpointInterface())) {
                return service.endpointInterface();
            }
        }
        return null;
    }
    private void initialize() {
        Class<?> cls = implementorClass;
        while (cls != null) {
            WebService annotation = cls.getAnnotation(WebService.class);
            if (annotation != null) {
                wsAnnotations.add(annotation);
                if (cls.isInterface()) {
                    cls = null;
                }
            }
            if (cls != null) {
                cls = cls.getSuperclass();                
            }
        }
        String sei = getImplementorClassName();
        boolean seiFromWsAnnotation = true;
        if (StringUtils.isEmpty(sei)) {
            seiFromWsAnnotation = false;
            sei = getWSInterfaceName(implementorClass);                
        }
        if (!StringUtils.isEmpty(sei)) {
            try {
                seiClass = ClassLoaderUtils.loadClass(sei, implementorClass);
            } catch (ClassNotFoundException ex) {
                throw new WebServiceException(BUNDLE.getString("SEI_LOAD_FAILURE_MSG"), ex);
            }
            WebService seiAnnotation = seiClass.getAnnotation(WebService.class);
            if (null == seiAnnotation) {
                throw new WebServiceException(BUNDLE.getString("SEI_WITHOUT_WEBSERVICE_ANNOTATION_EXC"));
            }
            if (seiFromWsAnnotation 
                && (!StringUtils.isEmpty(seiAnnotation.portName())
                || !StringUtils.isEmpty(seiAnnotation.serviceName())
                || !StringUtils.isEmpty(seiAnnotation.endpointInterface()))) {
                String expString = BUNDLE.getString("ILLEGAL_ATTRIBUTE_IN_SEI_ANNOTATION_EXC");
                throw new WebServiceException(expString);
            }
            wsAnnotations.add(seiAnnotation);
        }
        wsProviderAnnotation = implementorClass.getAnnotation(WebServiceProvider.class);
    }

    public boolean isWebServiceProvider() {
        return Provider.class.isAssignableFrom(implementorClass);
    }

    public WebServiceProvider getWsProvider() {
        return wsProviderAnnotation;
    }

    public Service.Mode getServiceMode() {
        ServiceMode m = implementorClass.getAnnotation(ServiceMode.class);
        if (m != null && m.value() != null) {
            return m.value();
        }
        return Service.Mode.PAYLOAD;
    }

    public Class<?> getProviderParameterType() {
        // The Provider Implementor inherits out of Provider<T>
        Type intfTypes[] = implementorClass.getGenericInterfaces();
        for (Type t : intfTypes) {
            Class<?> clazz = JAXBEncoderDecoder.getClassFromType(t);
            if (Provider.class == clazz) {
                Type paramTypes[] = ((ParameterizedType)t).getActualTypeArguments();
                return JAXBEncoderDecoder.getClassFromType(paramTypes[0]);
            }
        }
        return null;
    }

    public String getBindingType() {
        BindingType bType = implementorClass.getAnnotation(BindingType.class);
        if (bType != null) {
            return bType.value();
        }
        return SOAPBinding.SOAP11HTTP_BINDING;
    }
}
