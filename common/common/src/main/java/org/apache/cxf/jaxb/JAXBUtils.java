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


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.CachedClass;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.common.util.ReflectionInvokationHandler.WrapReturn;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.JavaUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public final class JAXBUtils {
    
    public enum IdentifierType {
        CLASS,
        INTERFACE,
        GETTER,
        SETTER,
        VARIABLE,
        CONSTANT
    };
    
    public static final String JAXB_URI = "http://java.sun.com/xml/ns/jaxb";
    
    
    private static final char[] XML_NAME_PUNCTUATION_CHARS = new char[] {
        /* hyphen                       */ '\u002D', 
        /* period                       */ '\u002E',
        /* colon                        */'\u003A',
        /* dot                          */ '\u00B7',
        /* greek ano teleia             */ '\u0387',
        /* arabic end of ayah           */ '\u06DD',
        /* arabic start of rub el hizb  */'\u06DE',
        /* underscore                   */ '\u005F',
    };
    
    private static final String XML_NAME_PUNCTUATION_STRING = new String(XML_NAME_PUNCTUATION_CHARS);
    
    private static final Map<String, String> BUILTIN_DATATYPES_MAP;
    private static final Map<String, Class<?>> HOLDER_TYPES_MAP;
    private static final Logger LOG = LogUtils.getL7dLogger(JAXBUtils.class, "CommonUtilityMessages");
    private static ClassLoader jaxbXjcLoader;

    static {
        BUILTIN_DATATYPES_MAP = new HashMap<String, String>();        
        BUILTIN_DATATYPES_MAP.put("string", "java.lang.String");
        BUILTIN_DATATYPES_MAP.put("integer", "java.math.BigInteger");
        BUILTIN_DATATYPES_MAP.put("int", "int");
        BUILTIN_DATATYPES_MAP.put("long", "long");
        BUILTIN_DATATYPES_MAP.put("short", "short");
        BUILTIN_DATATYPES_MAP.put("decimal", "java.math.BigDecimal");
        BUILTIN_DATATYPES_MAP.put("float", "float");
        BUILTIN_DATATYPES_MAP.put("double", "double");
        BUILTIN_DATATYPES_MAP.put("boolean", "boolean");
        BUILTIN_DATATYPES_MAP.put("byte", "byte");
        BUILTIN_DATATYPES_MAP.put("QName", "javax.xml.namespace.QName");
        BUILTIN_DATATYPES_MAP.put("dateTime", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("base64Binary", "byte[]");
        BUILTIN_DATATYPES_MAP.put("hexBinary", "byte[]");
        BUILTIN_DATATYPES_MAP.put("unsignedInt", "long");
        BUILTIN_DATATYPES_MAP.put("unsignedShort", "short");
        BUILTIN_DATATYPES_MAP.put("unsignedByte", "byte");
        BUILTIN_DATATYPES_MAP.put("time", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("date", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("gYear", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("gYearMonth", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("gMonth", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("gMonthDay", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("gDay", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("duration", "javax.xml.datatype.Duration");
        BUILTIN_DATATYPES_MAP.put("NOTATION", "javax.xml.namespace.QName");
        BUILTIN_DATATYPES_MAP.put("string", "java.lang.String");
        
        HOLDER_TYPES_MAP = new HashMap<String, Class<?>>();
        HOLDER_TYPES_MAP.put("int", java.lang.Integer.class);
        HOLDER_TYPES_MAP.put("long", java.lang.Long.class);
        HOLDER_TYPES_MAP.put("short", java.lang.Short.class);
        HOLDER_TYPES_MAP.put("float", java.lang.Float.class);
        HOLDER_TYPES_MAP.put("double", java.lang.Double.class);
        HOLDER_TYPES_MAP.put("boolean", java.lang.Boolean.class);
        HOLDER_TYPES_MAP.put("byte", java.lang.Byte.class);
    }
    
    
    /**
     * prevents instantiation
     *
     */
    private JAXBUtils() {
    }
    
    public static String builtInTypeToJavaType(String type) {
        return BUILTIN_DATATYPES_MAP.get(type);
    }
    
    public static Class<?> holderClass(String type) {
        return HOLDER_TYPES_MAP.get(type);
    }
    
    /** 
     * Checks if the specified word is a Java keyword (as of 1.5).
     * 
     * @param word the word to check.
     * @return true if the word is a keyword.
     */
    public static boolean isJavaKeyword(String word) {
        return JavaUtils.isJavaKeyword(word);
    }

    /**
     * Generates a Java package name from a URI according to the
     * algorithm outlined in JAXB 2.0.
     * 
     * @param namespaceURI the namespace URI.
     * @return the package name.
     */
    public static String namespaceURIToPackage(String namespaceURI) {
        try {
            return nameSpaceURIToPackage(new URI(namespaceURI));
        } catch (URISyntaxException ex) {
            return null;
        }
    }
    
    /**
     * Generates a Java package name from a URI according to the
     * algorithm outlined in JAXB 2.0.
     * 
     * @param namespaceURI the namespace URI.
     * @return the package name.
     */
    public static String nameSpaceURIToPackage(URI uri) {
       
        StringBuilder packageName = new StringBuilder();
        String authority = uri.getAuthority();
        if (authority == null && "urn".equals(uri.getScheme())) {
            authority = uri.getSchemeSpecificPart();
        }
        
        if (null != authority && !"".equals(authority)) {
            if ("urn".equals(uri.getScheme())) {
                packageName.append(authority);
                for (int i = 0; i < packageName.length(); i++) {
                    if (packageName.charAt(i) == '-') {
                        packageName.setCharAt(i, '.');
                    } 
                }
                authority = packageName.toString();
                packageName.setLength(0);
                
                StringTokenizer st = new StringTokenizer(authority, ":");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (packageName.length() > 0) {
                        packageName.insert(0, ".");
                        packageName.insert(0, normalizePackageNamePart(token));
                    } else {
                        packageName.insert(0, token);
                    }
                }
                authority = packageName.toString();
                packageName.setLength(0);
                
            }  
            
            StringTokenizer st = new StringTokenizer(authority, ".");
            if (st.hasMoreTokens()) {
                String token = null;
                while (st.hasMoreTokens()) {
                    token = st.nextToken();
                    if (packageName.length() == 0) {
                        if ("www".equals(token)) {
                            continue;
                        }
                    } else {
                        packageName.insert(0, ".");
                    }
                    packageName.insert(0, normalizePackageNamePart(token));
                }
               
            }
        }

        String path = uri.getPath();
        if (path == null) {
            path = "";
        }
        int index = path.lastIndexOf('.');
        if (index < 0) {
            index = path.length();
        }
        StringTokenizer st = new StringTokenizer(path.substring(0, index), "/");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (packageName.length() > 0) {
                packageName.append('.');
            }
            packageName.append(normalizePackageNamePart(token));
        }
        return packageName.toString();
    }
    
    private static String normalizePackageNamePart(String name) {
        StringBuilder sname = new StringBuilder(name.toLowerCase());

        for (int i = 0; i < sname.length(); i++) {
            sname.setCharAt(i, Character.toLowerCase(sname.charAt(i)));
        }
        
        for (int i = 0; i < sname.length(); i++) {
            if (!Character.isJavaIdentifierPart(sname.charAt(i))) {
                sname.setCharAt(i, '_');
            }
        }
        
        if (isJavaKeyword(sname.toString())) {
            sname.insert(0, '_');
        }
        
        if (!Character.isJavaIdentifierStart(sname.charAt(0))) {
            sname.insert(0, '_');  
        }
                
        return sname.toString();
    }
    
    
    /**
     * Converts an XML name to a Java identifier according to the mapping
     * algorithm outlines in the JAXB specification
     * 
     * @param name the XML name
     * @return the Java identifier
     */
    public static String nameToIdentifier(String name, IdentifierType type) {

        if (null == name || name.length() == 0) {
            return name;
        }

        // algorithm will not change an XML name that is already a legal and
        // conventional (!) Java class, method, or constant identifier

        boolean legalIdentifier = false;
        StringBuilder buf = new StringBuilder(name);
        legalIdentifier = Character.isJavaIdentifierStart(buf.charAt(0));

        for (int i = 1; i < name.length() && legalIdentifier; i++) {
            legalIdentifier = legalIdentifier && Character.isJavaIdentifierPart(buf.charAt(i));
        }
        
        boolean conventionalIdentifier = isConventionalIdentifier(buf, type); 
        if (legalIdentifier && conventionalIdentifier) {
            if (JAXBUtils.isJavaKeyword(name) && type == IdentifierType.VARIABLE) {
                name = normalizePackageNamePart(name.toString());
            }
            return name;
        }
        
        // split into words 

        List<String> words = new ArrayList<String>();

        StringTokenizer st = new StringTokenizer(name, XML_NAME_PUNCTUATION_STRING);
        while (st.hasMoreTokens()) {
            words.add(st.nextToken());
        }        

        for (int i = 0; i < words.size(); i++) {
            splitWord(words, i);
        }
        
        return makeConventionalIdentifier(words, type);
    }
    
    private static void splitWord(List<String> words, int listIndex) {
        String word = words.get(listIndex);
        if (word.length() <= 1) {
            return;
        }
        int index = listIndex + 1;
        StringBuilder sword = new StringBuilder(word);
        int first = 0;
        char firstChar = sword.charAt(first);
        if (Character.isLowerCase(firstChar)) {
            sword.setCharAt(first, Character.toUpperCase(firstChar));
        }
        int i = 1;
        
        while (i < sword.length()) {  
            if (Character.isDigit(firstChar)) {
                while (i < sword.length() && Character.isDigit(sword.charAt(i))) {
                    i++;
                }
            } else if (isCasedLetter(firstChar)) {
                boolean previousIsLower = Character.isLowerCase(firstChar); 
                while (i < sword.length() && isCasedLetter(sword.charAt(i))) {
                    if (Character.isUpperCase(sword.charAt(i)) && previousIsLower) {
                        break;
                    } 
                    previousIsLower = Character.isLowerCase(sword.charAt(i));
                    i++;
                }             
            } else { 
                // first must be a mark or an uncased letter
                while (i < sword.length() && (isMark(sword.charAt(i)) || !isCasedLetter(sword.charAt(i)))) {
                    i++;
                }       
            }
            
            // characters from first to i are all either
            // * digits
            // * upper or lower case letters, with only the first one an upper
            // * uncased letters or marks
            
            
            String newWord = sword.substring(first, i);
            words.add(index, newWord);
            index++;
            if (i >= sword.length()) {
                break;
            } else {
                first = i;
                firstChar = sword.charAt(first);
            }
        }
        
        if (index > (listIndex + 1)) {
            words.remove(listIndex);
        }
    }
    
    private static boolean isMark(char c) {
        return Character.isJavaIdentifierPart(c) && !Character.isLetter(c) && !Character.isDigit(c);
    }
    
    private static boolean isCasedLetter(char c) {
        return Character.isUpperCase(c) || Character.isLowerCase(c);
    }
    
    private static boolean isConventionalIdentifier(StringBuilder buf, IdentifierType type) {
        if (null == buf || buf.length() == 0) {
            return false;
        }
        boolean result = false;
        if (IdentifierType.CONSTANT == type) {
            for (int i = 0; i < buf.length(); i++) {
                if (Character.isLowerCase(buf.charAt(i))) {
                    return false;                  
                }
            }
            result = true;
        } else if (IdentifierType.VARIABLE == type) {
            result = Character.isLowerCase(buf.charAt(0));
        } else {
            int pos = 3;
            if (IdentifierType.GETTER == type 
                && !(buf.length() >= pos 
                    && "get".equals(buf.subSequence(0, 3)))) {
                return false;
            } else if (IdentifierType.SETTER == type 
                && !(buf.length() >= pos && "set".equals(buf.subSequence(0, 3)))) {
                return false;
            } else {
                pos = 0;
            }
            result = Character.isUpperCase(buf.charAt(pos));
        }
        return result;
    }
    
    private static String makeConventionalIdentifier(List<String> words, IdentifierType type) {
        StringBuilder buf = new StringBuilder();
        boolean firstWord = true;
        if (IdentifierType.GETTER == type) {
            buf.append("get");
        } else if (IdentifierType.SETTER == type) {
            buf.append("set");
        }
        for (String w : words) {
            int l = buf.length();
            if (l > 0 && IdentifierType.CONSTANT == type) {
                buf.append('_');
                l++;
            }
            buf.append(w);
            if (IdentifierType.CONSTANT == type) {
                for (int i = l; i < buf.length(); i++) {
                    if (Character.isLowerCase(buf.charAt(i))) {
                        buf.setCharAt(i, Character.toUpperCase(buf.charAt(i)));                  
                    }
                }
            } else if (IdentifierType.VARIABLE == type) {
                if (firstWord && Character.isUpperCase(buf.charAt(l))) {
                    buf.setCharAt(l, Character.toLowerCase(buf.charAt(l)));
                }
            } else {
                if (firstWord && Character.isLowerCase(buf.charAt(l))) {
                    buf.setCharAt(l, Character.toUpperCase(buf.charAt(l)));
                }
            }
            firstWord = false;
        }
        return buf.toString();
    }
    
    public static Class<?> getValidClass(Class<?> cls) {
        if (cls.isEnum()) {
            return cls;
        }
        if (cls.isArray()) {
            return cls;
        }

        if (cls == Object.class || cls == String.class
            || "javax.xml.ws.Holder".equals(cls.getName())) {
            cls = null;
        } else if (cls.isPrimitive() || cls.isAnnotation()) {
            cls = null;
        } else if (cls.isInterface()) {
            return cls;
        }
        if (cls != null) {
            if (cls.getName().equals("javax.xml.ws.wsaddressing.W3CEndpointReference")) {
                return cls;
            }
            try {
                if (cls.getConstructor(new Class[0]) == null) {
                    cls = null;
                }
            } catch (NoSuchMethodException ex) {
                cls = null;
            }
        }
        return cls;
    }

    private static synchronized ClassLoader getXJCClassLoader() {
        if (jaxbXjcLoader == null) {
            try {
                Class.forName("com.sun.tools.internal.xjc.api.XJC");
                jaxbXjcLoader = ClassLoader.getSystemClassLoader();
            } catch (Exception t2) {
                //couldn't find either, probably cause tools.jar isn't on 
                //the classpath.   Let's see if we can find the tools jar
                String s = System.getProperty("java.home");
                if (!StringUtils.isEmpty(s)) {
                    File home = new File(s);
                    File jar = new File(home, "lib/tools.jar");
                    if (!jar.exists()) {
                        jar = new File(home, "../lib/tools.jar");
                    }
                    if (jar.exists()) {
                        try {
                            jaxbXjcLoader = new URLClassLoader(new URL[] {jar.toURI().toURL()});
                            Class.forName("com.sun.tools.internal.xjc.api.XJC", false, jaxbXjcLoader);
                        } catch (Exception e) {
                            jaxbXjcLoader = null;
                        }
                    }
                }
            } 
        }
        return jaxbXjcLoader;
    }
    
    public static JAXBContext createRIContext(Class<?> clss[], String defaultNS) throws JAXBException {
        try {
            Class<?> cls;
            Map<String, Object> map = new HashMap<String, Object>();
            try {
                cls = Class.forName("com.sun.xml.bind.v2.ContextFactory");
                if (defaultNS != null) {
                    map.put("com.sun.xml.bind.defaultNamespaceRemap", defaultNS);
                }
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                cls = Class.forName("com.sun.xml.internal.bind.v2.ContextFactory", true, getXJCClassLoader());
                if (defaultNS != null) {
                    map.put("com.sun.xml.internal.bind.defaultNamespaceRemap", defaultNS);
                }
            }
            Method meth = cls.getMethod("createContext", clss.getClass(), Map.class);
            return (JAXBContext)meth.invoke(null, clss, map);
        } catch (Exception e) {
            throw new JAXBException(e);
        }
    }
    public static void setNamespaceWrapper(final Map<String, String> nspref,
                                           Marshaller marshaller) throws PropertyException {
        Object mapper = null;
        if (marshaller.getClass().getName().contains(".internal.")) {
            mapper = createNamespaceWrapper(nspref);
            if (mapper == null) {
                LOG.log(Level.INFO, "Could not create namespace mapper for JDK internal"
                        + " JAXB implementation.");
            } else {
                marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper",
                                   mapper);
            }
        } else {
            try {
                Class<?> cls = Class.forName("org.apache.cxf.jaxb.NamespaceMapper");
                mapper = cls.getConstructor(Map.class).newInstance(nspref);
            } catch (Exception ex) {
                LOG.log(Level.INFO, "Could not create NamespaceMapper", ex);
            }
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",
                                   mapper);
        }
    }
    public static BridgeWrapper createBridge(Set<Class<?>> ctxClasses,
                                      QName qname,
                                      Class<?> refcls,
                                      Annotation anns[]) throws JAXBException {
        try {
            Class<?> cls;
            Class<?> refClass;
            String pkg = "com.sun.xml.bind.";
            try {
                cls = Class.forName("com.sun.xml.bind.api.JAXBRIContext");
                refClass = Class.forName(pkg + "api.TypeReference");
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                cls = Class.forName("com.sun.xml.internal.bind.api.JAXBRIContext", true, getXJCClassLoader());
                pkg = "com.sun.xml.internal.bind.";
                refClass = Class.forName(pkg + "api.TypeReference", true, getXJCClassLoader());
            }
            Object ref = refClass.getConstructor(QName.class, 
                                                 Type.class, 
                                                 anns.getClass()).newInstance(qname, refcls, anns);
            List<Object> typeRefs = new ArrayList<Object>();
            typeRefs.add(ref);
            List<Class<?>> clses = new ArrayList<Class<?>>(ctxClasses);
            clses.add(refClass.getField("type").get(ref).getClass());
            if (!refcls.isInterface()) {
                clses.add(refcls);
            }
            
            Object ctx = null;
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getName().equals("newInstance")
                    && m.getParameterTypes().length == 6) {
                    ctx = m.invoke(null, clses.toArray(new Class[clses.size()]),
                                         typeRefs, null, null, true, null);
                    
                }
            }
            
            Object bridge = ctx.getClass().getMethod("createBridge", refClass).invoke(ctx, ref);
            return ReflectionInvokationHandler.createProxyWrapper(bridge,
                                                                  BridgeWrapper.class);
        } catch (Exception ex) {
            throw new JAXBException(ex);
        }
    }
    public static interface BridgeWrapper {

        Object unmarshal(XMLStreamReader source, AttachmentUnmarshaller am) throws JAXBException;

        Object unmarshal(InputStream source) throws JAXBException;

        Object unmarshal(Node source, AttachmentUnmarshaller am) throws JAXBException;

        void marshal(Object elValue, XMLStreamWriter source, AttachmentMarshaller m) throws JAXBException;

        void marshal(Object elValue, StreamResult s1) throws JAXBException;

        void marshal(Object elValue, Node source, AttachmentMarshaller am) throws JAXBException;
    }
    
    
    public static SchemaCompiler createSchemaCompiler() throws JAXBException {
        try {
            Class<?> cls;
            Object sc = null;
            try {
                cls = Class.forName("com.sun.tools.xjc.api.XJC");
                sc = cls.getMethod("createSchemaCompiler").invoke(null);
            } catch (Throwable e) {
                cls = Class.forName("com.sun.tools.internal.xjc.api.XJC", true, getXJCClassLoader());
                sc = cls.getMethod("createSchemaCompiler").invoke(null);
            }
            
            return ReflectionInvokationHandler.createProxyWrapper(sc,
                                                                  SchemaCompiler.class);
        } catch (Exception ex) {
            throw new JAXBException(ex);
        }
    }
    
    public static Object createFileCodeWriter(File f) throws JAXBException {
        try {
            Class<?> cls;
            try {
                cls = Class.forName("com.sun.codemodel.writer.FileCodeWriter");
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                cls = Class.forName("com.sun.codemodel.internal.writer.FileCodeWriter",
                                    true, getXJCClassLoader());
            }
            return cls.getConstructor(File.class).newInstance(f);
        } catch (Exception ex) {
            throw new JAXBException(ex);
        }
    }
    
    public static Class<?> getParamClass(SchemaCompiler sc, String method) {
        Object o = ((ReflectionInvokationHandler)Proxy.getInvocationHandler(sc)).getTarget();
        for (Method m : o.getClass().getMethods()) {
            if (m.getName().equals(method) && m.getParameterTypes().length == 1) {
                return m.getParameterTypes()[0];
            }
        }
        return null;
    }
    
    public static JAXBBeanInfo getBeanInfo(JAXBContextProxy context, Class<?> cls) {
        Object o = context.getBeanInfo(cls);
        if (o == null) {
            return null;
        }
        return ReflectionInvokationHandler.createProxyWrapper(o, JAXBBeanInfo.class);
    }
    
    public static List<DOMResult> generateJaxbSchemas(
        JAXBContext context, final Map<String, DOMResult> builtIns) throws IOException {
        final List<DOMResult> results = new ArrayList<DOMResult>();

        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String ns, String file) throws IOException {
                DOMResult result = new DOMResult();
                
                if (builtIns.containsKey(ns)) {
                    DOMResult dr = builtIns.get(ns);
                    result.setSystemId(dr.getSystemId());
                    results.add(dr);
                    return result;
                }
                result.setSystemId(file);
                results.add(result);
                return result;
            }
        });

        return results;
    }
    
    public static String getPackageNamespace(Class<?> cls) {
        Package p = Package.getPackage(PackageUtils.getPackageName(cls));
        if (p != null) {
            javax.xml.bind.annotation.XmlSchema schemaAnn = 
                p.getAnnotation(javax.xml.bind.annotation.XmlSchema.class);
            if (schemaAnn != null) {
                return schemaAnn.namespace();
            }
        }
        return null;
    }
    
    public static void scanPackages(Set<Class<?>> classes, 
                                    Map<Package, CachedClass> objectFactoryCache) {
        // try and read any jaxb.index files that are with the other classes.
        // This should
        // allow loading of extra classes (such as subclasses for inheritance
        // reasons)
        // that are in the same package. Also check for ObjectFactory classes
        Map<String, InputStream> packages = new HashMap<String, InputStream>();
        Map<String, ClassLoader> packageLoaders = new HashMap<String, ClassLoader>();
        Set<Class<?>> objectFactories = new HashSet<Class<?>>();
        for (Class<?> jcls : classes) {
            String pkgName = PackageUtils.getPackageName(jcls);
            if (!packages.containsKey(pkgName)) {
                Package pkg = jcls.getPackage();
                   
                packages.put(pkgName, jcls.getResourceAsStream("jaxb.index"));
                packageLoaders.put(pkgName, jcls.getClassLoader());
                String objectFactoryClassName = pkgName + "." + "ObjectFactory";
                Class<?> ofactory = null;
                CachedClass cachedFactory = null;
                if (pkg != null && objectFactoryCache != null) {
                    synchronized (objectFactoryCache) {
                        cachedFactory = objectFactoryCache.get(pkg);
                    }
                }
                if (cachedFactory != null) {
                    ofactory = cachedFactory.getCachedClass();
                }
                if (ofactory == null) {
                    try {
                        ofactory = Class.forName(objectFactoryClassName, false, jcls
                                                 .getClassLoader());
                        objectFactories.add(ofactory);
                        addToObjectFactoryCache(pkg, ofactory, objectFactoryCache);
                    } catch (ClassNotFoundException e) {
                        addToObjectFactoryCache(pkg, null, objectFactoryCache);
                    }
                } else {
                    objectFactories.add(ofactory);                    
                }
            }
        }
        for (Map.Entry<String, InputStream> entry : packages.entrySet()) {
            if (entry.getValue() != null) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entry.getValue(),
                                                                                     "UTF-8"));
                    String pkg = entry.getKey();
                    ClassLoader loader = packageLoaders.get(pkg);
                    if (!StringUtils.isEmpty(pkg)) {
                        pkg += ".";
                    }

                    String line = reader.readLine();
                    while (line != null) {
                        line = line.trim();
                        if (line.indexOf("#") != -1) {
                            line = line.substring(0, line.indexOf("#"));
                        }
                        if (!StringUtils.isEmpty(line)) {
                            try {
                                Class<?> ncls = Class.forName(pkg + line, false, loader);
                                classes.add(ncls);
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                        line = reader.readLine();
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    try {
                        entry.getValue().close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
        classes.addAll(objectFactories);
    }

       
    private static void addToObjectFactoryCache(Package objectFactoryPkg, 
                                         Class<?> ofactory,
                                         Map<Package, CachedClass> objectFactoryCache) {
        if (objectFactoryPkg == null || objectFactoryCache == null) {
            return;
        }
        synchronized (objectFactoryCache) {
            objectFactoryCache.put(objectFactoryPkg, 
                                     new CachedClass(ofactory));
        }
    }
    
    public static interface SchemaCompiler {

        void setErrorListener(Object elForRun);

        void setClassNameAllocator(Object allocator);

        @WrapReturn(S2JJAXBModel.class)
        S2JJAXBModel bind();

        void parseSchema(InputSource source);

        void parseSchema(String key, Element el);

        @WrapReturn(Options.class)
        Options getOptions();
    }
    public static interface S2JJAXBModel {

        @WrapReturn(JCodeModel.class)
        JCodeModel generateCode(Object object, Object elForRun);
        
        @WrapReturn(Mapping.class)
        Mapping get(QName qn);

        @WrapReturn(TypeAndAnnotation.class)
        TypeAndAnnotation getJavaType(QName typeQName);
    }
    public static interface Mapping {
        @WrapReturn(TypeAndAnnotation.class)
        TypeAndAnnotation getType();        
    }
    public static interface TypeAndAnnotation {
        @WrapReturn(JType.class)
        JType getTypeClass();
    }
    public static interface JType {
        boolean isArray();

        @WrapReturn(JType.class)
        JType elementType();

        boolean isPrimitive();

        String binaryName();

        String fullName();

        String name();

        @WrapReturn(value = JType.class, iterator = true)
        Iterator<JType> classes();
    }
    public static interface Options {

        void addGrammar(InputSource is);

        void addBindFile(InputSource is);

        String getBuildID();
    }
    public static interface JCodeModel {

        void build(Object writer) throws IOException;

        @WrapReturn(value = JPackage.class, iterator = true)
        Iterator<JPackage> packages();
    }
    public static interface JPackage {

        String name();

        @WrapReturn(value = JDefinedClass.class, iterator = true)
        Iterator<JDefinedClass> classes();
    }
    public static interface JDefinedClass {
        String name();

        String fullName();
    }
 
    private static synchronized Object createNamespaceWrapper(Map<String, String> map) {
        ASMHelper helper = new ASMHelper();
        String className = "org.apache.cxf.jaxb.NamespaceMapperInternal";
        Class<?> cls = helper.findClass(className, JAXBUtils.class);
        if (cls == null) {
            ClassWriter cw = helper.createClassWriter();
            if (cw == null) {
                return null;
            }
            cls = createNamespaceWrapperInternal(helper, cw);
        }
        try {
            return cls.getConstructor(Map.class).newInstance(map);
        } catch (Exception e) {
            return null;
        }
    }
    private static Class<?> createNamespaceWrapperInternal(ASMHelper helper, ClassWriter cw) {
        String className = "org.apache.cxf.jaxb.NamespaceMapperInternal";
        FieldVisitor fv;
        MethodVisitor mv;
        cw.visit(Opcodes.V1_5, 
                 Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER,
                 "org/apache/cxf/jaxb/NamespaceMapperInternal", null,
                 "com/sun/xml/internal/bind/marshaller/NamespacePrefixMapper", null);

        cw.visitSource("NamespaceMapper.java", null);

        fv = cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL, 
                           "nspref", "Ljava/util/Map;",
                           "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;", null);
        fv.visitEnd();
        
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", 
                            "(Ljava/util/Map;)V", 
                            "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;)V", null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(30, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                           "com/sun/xml/internal/bind/marshaller/NamespacePrefixMapper", "<init>", "()V");
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(31, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, "org/apache/cxf/jaxb/NamespaceMapperInternal",
                          "nspref", "Ljava/util/Map;");
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLineNumber(32, l2);
        mv.visitInsn(Opcodes.RETURN);
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLocalVariable("this", "Lorg/apache/cxf/jaxb/NamespaceMapperInternal;", null, l0, l3, 0);
        mv.visitLocalVariable("nspref", "Ljava/util/Map;",
                              "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;", 
                              l0, l3, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getPreferredPrefix",
                            "(Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;",
                            null, null);
        mv.visitCode();
        l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(38, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, 
                          "org/apache/cxf/jaxb/NamespaceMapperInternal", 
                          "nspref", "Ljava/util/Map;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", 
                           "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
        mv.visitVarInsn(Opcodes.ASTORE, 4);
        l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(39, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        l2 = new Label();
        mv.visitJumpInsn(Opcodes.IFNULL, l2);
        l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLineNumber(40, l3);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(l2);
        mv.visitLineNumber(42, l2);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARETURN);
        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitLocalVariable("this", "Lorg/apache/cxf/jaxb/NamespaceMapperInternal;", null, l0, l4, 0);
        mv.visitLocalVariable("namespaceUri", "Ljava/lang/String;", null, l0, l4, 1);
        mv.visitLocalVariable("suggestion", "Ljava/lang/String;", null, l0, l4, 2);
        mv.visitLocalVariable("requirePrefix", "Z", null, l0, l4, 3);
        mv.visitLocalVariable("prefix", "Ljava/lang/String;", null, l1, l4, 4);
        mv.visitMaxs(2, 5);
        mv.visitEnd();
        cw.visitEnd();

        byte bts[] = cw.toByteArray();
        
        
        Class<?> cls;
        try {
            cls = Class.forName("com.sun.xml.bind.api.JAXBRIContext");
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            try {
                cls = Class.forName("com.sun.xml.internal.bind.api.JAXBRIContext", true, getXJCClassLoader());
            } catch (ClassNotFoundException e1) {
                cls = JAXBUtils.class;
            }
        }
        
        return helper.loadClass(className,
                                cls, bts);
    }
}
