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

package org.apache.cxf.jaxrs.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ws.rs.CookieParam;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.MethodDispatcher;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.model.UserOperation;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.resource.ResourceManager;

public final class ResourceUtils {
    
    private static final Logger LOG = LogUtils.getL7dLogger(ResourceUtils.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(ResourceUtils.class);

    
    private ResourceUtils() {
        
    }
    
    public static ClassResourceInfo createClassResourceInfo(
        Map<String, UserResource> resources, UserResource model, boolean isRoot) {
        if (model.getName() == null) {
            return null;
        }
        Class<?> sClass = loadClass(model.getName());
        ClassResourceInfo cri  = new ClassResourceInfo(sClass, sClass, isRoot, true, true);
        if (InjectionUtils.isConcreteClass(cri.getServiceClass())) {
            try {
                cri.setResourceProvider(new SingletonResourceProvider(sClass.newInstance()));
            } catch (Exception ex) {
                throw new RuntimeException("Resource class " + model.getName() + " can not be created");
            }
        }
        URITemplate t = URITemplate.createTemplate(model.getPath());
        cri.setURITemplate(t);
        MethodDispatcher md = new MethodDispatcher();
        Map<String, UserOperation> ops = model.getOperationsAsMap();
        for (Method m : cri.getServiceClass().getMethods()) {
            UserOperation op = ops.get(m.getName());
            if (op == null || op.getName() == null) {
                continue;
            }
            OperationResourceInfo ori = 
                new OperationResourceInfo(m, cri, URITemplate.createTemplate(op.getPath()),
                                          op.getVerb(), op.getConsumes(), op.getProduces(),
                                          op.getParameters());
            String rClassName = m.getReturnType().getName();
            if (op.getVerb() == null) {
                if (resources.containsKey(rClassName)) {
                    ClassResourceInfo subCri = rClassName.equals(model.getName()) ? cri 
                        : createClassResourceInfo(resources, resources.get(rClassName), false);
                    if (subCri != null) {
                        cri.addSubClassResourceInfo(subCri);
                        md.bind(ori, m);
                    }
                }
            } else {
                md.bind(ori, m);
            }
        }
        cri.setMethodDispatcher(md);
        return checkMethodDispatcher(cri) ? cri : null;

    }
    
    public static ClassResourceInfo createServiceClassResourceInfo(
        Map<String, UserResource> resources, Class<?> sClass, boolean isRoot) {
        UserResource model = resources.get(sClass.getName());
        if (model == null) {
            throw new RuntimeException("Resource class " + sClass.getName() + " has no model info");
        }
        ClassResourceInfo cri  = new ClassResourceInfo(sClass, sClass, isRoot, true, true);
        URITemplate t = URITemplate.createTemplate(model.getPath());
        cri.setURITemplate(t);
        MethodDispatcher md = new MethodDispatcher();
        Map<String, UserOperation> ops = model.getOperationsAsMap();
        for (Method m : cri.getServiceClass().getMethods()) {
            UserOperation op = ops.get(m.getName());
            if (op == null || op.getName() == null) {
                continue;
            }
            OperationResourceInfo ori = 
                new OperationResourceInfo(m, cri, URITemplate.createTemplate(op.getPath()),
                                          op.getVerb(), op.getConsumes(), op.getProduces(),
                                          op.getParameters());
            String rClassName = m.getReturnType().getName();
            if (op.getVerb() == null) {
                if (resources.containsKey(rClassName)) {
                    ClassResourceInfo subCri = rClassName.equals(model.getName()) ? cri 
                        : createServiceClassResourceInfo(resources, m.getReturnType(), false);
                    if (subCri != null) {
                        cri.addSubClassResourceInfo(subCri);
                        md.bind(ori, m);
                    }
                }
            } else {
                md.bind(ori, m);
            }
        }
        cri.setMethodDispatcher(md);
        return checkMethodDispatcher(cri) ? cri : null;

    }
    
    
    public static ClassResourceInfo createClassResourceInfo(
        final Class<?> rClass, final Class<?> sClass, boolean root, boolean enableStatic) {
        ClassResourceInfo cri  = new ClassResourceInfo(rClass, sClass, root, enableStatic);

        if (root) {
            URITemplate t = URITemplate.createTemplate(cri.getPath());
            cri.setURITemplate(t);
        }
        
        evaluateResourceClass(cri, enableStatic);
        return checkMethodDispatcher(cri) ? cri : null;
    }

    private static void evaluateResourceClass(ClassResourceInfo cri, boolean enableStatic) {
        MethodDispatcher md = new MethodDispatcher();
        for (Method m : cri.getServiceClass().getMethods()) {
            
            Method annotatedMethod = AnnotationUtils.getAnnotatedMethod(m);
            
            String httpMethod = AnnotationUtils.getHttpMethodValue(annotatedMethod);
            Path path = (Path)AnnotationUtils.getMethodAnnotation(annotatedMethod, Path.class);
            
            if (httpMethod != null || path != null) {
                md.bind(createOperationInfo(m, annotatedMethod, cri, path, httpMethod), m);
                if (httpMethod == null) {
                    // subresource locator
                    Class<?> subClass = m.getReturnType();
                    if (enableStatic) {
                        ClassResourceInfo subCri = cri.findResource(subClass, subClass);
                        if (subCri == null) {
                            subCri = subClass == cri.getServiceClass() ? cri
                                     : createClassResourceInfo(subClass, subClass, false, enableStatic);
                        }
                        
                        if (subCri != null) {
                            cri.addSubClassResourceInfo(subCri);
                        }
                    }
                }
            }
        }
        cri.setMethodDispatcher(md);
    }
    
    public static Constructor findResourceConstructor(Class<?> resourceClass, boolean perRequest) {
        List<Constructor> cs = new LinkedList<Constructor>();
        for (Constructor c : resourceClass.getConstructors()) {
            Class<?>[] params = c.getParameterTypes();
            Annotation[][] anns = c.getParameterAnnotations();
            boolean match = true;
            for (int i = 0; i < params.length; i++) {
                if (!perRequest) { 
                    if (AnnotationUtils.getAnnotation(anns[i], Context.class) == null
                        || !AnnotationUtils.isContextClass(params[i])) {
                        match = false;
                        break;
                    }
                } else if (!AnnotationUtils.isValidParamAnnotations(anns[i])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                cs.add(c);
            }
        }
        Collections.sort(cs, new Comparator<Constructor>() {

            public int compare(Constructor c1, Constructor c2) {
                int p1 = c1.getParameterTypes().length;
                int p2 = c2.getParameterTypes().length;
                return p1 > p2 ? -1 : p1 < p2 ? 1 : 0;
            }
        
        });
        return cs.size() == 0 ? null : cs.get(0);
    }
    
    public static List<Parameter> getParameters(Method resourceMethod) {
        Annotation[][] paramAnns = resourceMethod.getParameterAnnotations();
        if (paramAnns.length == 0) {
            return CastUtils.cast(Collections.emptyList(), Parameter.class);
        }
        List<Parameter> params = new ArrayList<Parameter>(paramAnns.length);
        for (int i = 0; i < paramAnns.length; i++) {
            Parameter p = getParameter(i, paramAnns[i]);
            params.add(p);
        }
        return params;
    }
    
    public static Parameter getParameter(int index, Annotation[] anns) {
        
        Context ctx = AnnotationUtils.getAnnotation(anns, Context.class);
        if (ctx != null) {
            return new Parameter(ParameterType.CONTEXT, index, null);
        }
        
        boolean isEncoded = AnnotationUtils.getAnnotation(anns, Encoded.class) != null;
        String dValue = AnnotationUtils.getDefaultParameterValue(anns);
        
        Parameter p = null;
        
        PathParam a = AnnotationUtils.getAnnotation(anns, PathParam.class); 
        if (a != null) {
            p = new Parameter(ParameterType.PATH, index, a.value(), isEncoded, dValue);
        } 
        if (p == null) {
            QueryParam q = AnnotationUtils.getAnnotation(anns, QueryParam.class);
            if (q != null) {
                p = new Parameter(ParameterType.QUERY, index, q.value(), isEncoded, dValue);
            }
        }
        if (p != null) {
            return p;
        }
        
        MatrixParam m = AnnotationUtils.getAnnotation(anns, MatrixParam.class);
        if (m != null) {
            return new Parameter(ParameterType.MATRIX, index, m.value(), isEncoded, dValue);
        }  
    
        FormParam f = AnnotationUtils.getAnnotation(anns, FormParam.class);
        if (f != null) {
            return new Parameter(ParameterType.FORM, index, f.value(), isEncoded, dValue);
        }  
        
        HeaderParam h = AnnotationUtils.getAnnotation(anns, HeaderParam.class);
        if (h != null) {
            return new Parameter(ParameterType.HEADER, index, h.value(), isEncoded, dValue);
        }  
        
        p = null;
        CookieParam c = AnnotationUtils.getAnnotation(anns, CookieParam.class);
        if (c != null) {
            p = new Parameter(ParameterType.COOKIE, index, c.value(), isEncoded, dValue);
        } else {
            p = new Parameter(ParameterType.REQUEST_BODY, index, null); 
        }
        
        return p;
    }
    
    
    private static OperationResourceInfo createOperationInfo(Method m, Method annotatedMethod, 
                                                      ClassResourceInfo cri, Path path, String httpMethod) {
        OperationResourceInfo ori = new OperationResourceInfo(m, annotatedMethod, cri);
        URITemplate t = URITemplate.createTemplate(path);
        ori.setURITemplate(t);
        ori.setHttpMethod(httpMethod);
        return ori;
    }
    
       
    private static boolean checkMethodDispatcher(ClassResourceInfo cr) {
        if (cr.getMethodDispatcher().getOperationResourceInfos().isEmpty()) {
            LOG.warning(new org.apache.cxf.common.i18n.Message("NO_RESOURCE_OP_EXC", 
                                                               BUNDLE, 
                                                               cr.getServiceClass().getName()).toString());
            return false;
        }
        return true;
    }
    

    private static Class<?> loadClass(String cName) {
        try {
            return ClassLoaderUtils.loadClass(cName.trim(), ResourceUtils.class);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("No class " + cName.trim() + " can be found", ex); 
        }
    }
    
    
    public static List<UserResource> getUserResources(String loc, Bus bus) {
        try {
            InputStream is = null;
            if (loc.startsWith("classpath:")) {
                String path = loc.substring("classpath:".length());
                is = getClasspathResourceStream(path, ResourceUtils.class, bus);
            } else {
                File f = new File(loc);
                if (f.exists()) {
                    is = new FileInputStream(f);
                }
            }
            if (is == null) {
                LOG.warning("No user model is available at " + loc);
                return null;
            }
            return getUserResources(is);
        } catch (Exception ex) {
            LOG.warning("Problem with processing a user model at " + loc);
        }
        
        return null;
    }
    
    public static InputStream getClasspathResourceStream(String path, Class<?> callingClass, Bus bus) {
        InputStream is = ClassLoaderUtils.getResourceAsStream(path, callingClass);
        if (is == null && bus != null) {
            ResourceManager rm = bus.getExtension(ResourceManager.class);
            if (rm != null) {
                is = rm.getResourceAsStream(path);
            }
        }
        return is;
    }
    
    public static List<UserResource> getUserResources(String loc) {
        return getUserResources(loc, BusFactory.getThreadDefaultBus());
    }
    
    public static List<UserResource> getUserResources(InputStream is) throws Exception {
        Document doc = DOMUtils.readXml(new InputStreamReader(is, "UTF-8"));
        return getResourcesFromElement(doc.getDocumentElement());
    }
    
    public static List<UserResource> getResourcesFromElement(Element modelEl) {
        List<UserResource> resources = new ArrayList<UserResource>();
        List<Element> resourceEls = 
            DOMUtils.findAllElementsByTagNameNS(modelEl, 
                                                "http://cxf.apache.org/jaxrs", "resource");
        for (Element e : resourceEls) {
            resources.add(getResourceFromElement(e));
        }
        return resources;
    }
    

    public static Map<Class<?>, Type> getAllRequestResponseTypes(List<ClassResourceInfo> cris, 
                                                                 boolean jaxbOnly) {
        Map<Class<?>, Type> types = new HashMap<Class<?>, Type>();
        for (ClassResourceInfo root : cris) {
            for (OperationResourceInfo ori : root.getMethodDispatcher().getOperationResourceInfos()) {
                Class<?> cls = ori.getMethodToInvoke().getReturnType();
                Type type = ori.getMethodToInvoke().getGenericReturnType();
                if (jaxbOnly) {
                    checkJaxbType(cls, types);
                } else {
                    types.put(cls, type);
                }
                for (Parameter pm : ori.getParameters()) {
                    if (pm.getType() == ParameterType.REQUEST_BODY) {
                        Class<?> inType = ori.getMethodToInvoke().getParameterTypes()[pm.getIndex()];
                        Type type2 = ori.getMethodToInvoke().getGenericParameterTypes()[pm.getIndex()];
                        if (jaxbOnly) {
                            checkJaxbType(inType, types);
                        } else {
                            types.put(inType, type2);
                        }
                    }
                }
                
            }
        }
        
        return types;
    }

    private static void checkJaxbType(Class<?> type, Map<Class<?>, Type> types) {
        JAXBElementProvider provider = new JAXBElementProvider();
        if (!InjectionUtils.isPrimitive(type) 
            && !JAXBElement.class.isAssignableFrom(type)
            && provider.isReadable(type, type, new Annotation[0], MediaType.APPLICATION_XML_TYPE)) {
            types.put(type, type);
        }        
    }
    
    private static UserResource getResourceFromElement(Element e) {
        UserResource resource = new UserResource();
        resource.setName(e.getAttribute("name"));
        resource.setPath(e.getAttribute("path"));
        List<Element> operEls = 
            DOMUtils.findAllElementsByTagNameNS(e, 
                 "http://cxf.apache.org/jaxrs", "operation");
        List<UserOperation> opers = new ArrayList<UserOperation>(operEls.size());
        for (Element operEl : operEls) {
            opers.add(getOperationFromElement(operEl));
        }
        resource.setOperations(opers);
        return resource;
    }
    
    private static UserOperation getOperationFromElement(Element e) {
        UserOperation op = new UserOperation();
        op.setName(e.getAttribute("name"));
        op.setVerb(e.getAttribute("verb"));
        op.setPath(e.getAttribute("path"));
        op.setConsumes(e.getAttribute("consumes"));
        op.setProduces(e.getAttribute("produces"));
        List<Element> paramEls = 
            DOMUtils.findAllElementsByTagNameNS(e, 
                 "http://cxf.apache.org/jaxrs", "param");
        List<Parameter> params = new ArrayList<Parameter>(paramEls.size());
        for (int i = 0; i < paramEls.size(); i++) {
            Element paramEl = paramEls.get(i);
            Parameter p = new Parameter(paramEl.getAttribute("type"), i, paramEl.getAttribute("name"));
            p.setEncoded(Boolean.valueOf(paramEl.getAttribute("encoded")));
            p.setDefaultValue(paramEl.getAttribute("default"));
            params.add(p);
        }
        op.setParameters(params);
        return op;
    }
    
}
