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
package org.apache.cxf.aegis.type;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.basic.XMLBeanTypeInfo;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;
import org.apache.cxf.aegis.util.NamespaceHelper;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XPathUtils;

/**
 * Deduce mapping information from an xml file. The xml file should be in the
 * same packages as the class, with the name <code>className.aegis.xml</code>.
 * For example, given the following service interface: <p/>
 * 
 * <pre>
 * public Collection getResultsForValues(String id, Collection values); //method 1
 * 
 * public Collection getResultsForValues(int id, Collection values); //method 2
 * 
 * public String getResultForValue(String value); //method 3
 * </pre>
 * 
 * An example of the type xml is:
 * 
 * <pre>
 *  &lt;mappings&gt;
 *   &lt;mapping&gt;
 *     &lt;method name=&quot;getResultsForValues&quot;&gt;
 *       &lt;return-type componentType=&quot;com.acme.ResultBean&quot; /&gt;
 *       &lt;!-- no need to specify index 0, since it's a String --&gt;
 *       &lt;parameter index=&quot;1&quot; componentType=&quot;java.lang.String&quot; /&gt;
 *     &lt;/method&gt;
 *   &lt;/mapping&gt;
 *  &lt;/mappings&gt;
 * </pre>
 * 
 * <p/> Note that for values which can be easily deduced (such as the String
 * parameter, or the second service method) no mapping need be specified in the
 * xml descriptor, which is why no mapping is specified for method 3. <p/>
 * However, if you have overloaded methods with different semantics, then you
 * will need to specify enough parameters to disambiguate the method and
 * uniquely identify it. So in the example above, the mapping specifies will
 * apply to both method 1 and method 2, since the parameter at index 0 is not
 * specified.
 */
public class XMLTypeCreator extends AbstractTypeCreator {
    private static final Logger LOG = LogUtils.getL7dLogger(XMLTypeCreator.class);
    private static List<Class> stopClasses = new ArrayList<Class>();
    static {
        stopClasses.add(Object.class);
        stopClasses.add(Exception.class);
        stopClasses.add(RuntimeException.class);
        stopClasses.add(Throwable.class);
    }

    private static DocumentBuilderFactory aegisDocumentBuilderFactory;
    private static Schema aegisSchema;
    // cache of classes to documents
    private Map<String, Document> documents = new HashMap<String, Document>();
    static {
        String path = "/META-INF/cxf/aegis.xsd";
        InputStream is = XMLTypeCreator.class.getResourceAsStream(path);
        if (is != null) {
            try {
                aegisDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
                aegisDocumentBuilderFactory.setNamespaceAware(true);
                
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                aegisSchema = schemaFactory.newSchema(new StreamSource(is));
                is.close();
                
                aegisDocumentBuilderFactory.setSchema(aegisSchema);
            } catch (UnsupportedOperationException e) {
                //Parsers that don't support schema validation
                LOG.log(Level.INFO, "Parser doesn't support setSchema.  Not validating.", e);
            } catch (IOException ie) {
                LOG.log(Level.SEVERE, "Error reading Aegis schema", ie);
            } catch (FactoryConfigurationError e) {
                LOG.log(Level.SEVERE, "Error reading Aegis schema", e);
            } catch (SAXException e) {
                LOG.log(Level.SEVERE, "Error reading Aegis schema", e);
            }
        }
    }
    
    private XPathUtils xpathUtils = new XPathUtils();

    private Document readAegisFile(InputStream is, final String path) throws IOException {
        DocumentBuilder documentBuilder;
        try {
            documentBuilder = aegisDocumentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOG.log(Level.SEVERE, "Unable to create a document builder, e");
            throw new RuntimeException("Unable to create a document builder, e");
        }
        org.w3c.dom.Document doc;
        documentBuilder.setErrorHandler(new ErrorHandler() {

            private String errorMessage(SAXParseException exception) {
                return MessageFormat.format("{0} at {1} line {2} column {3}.",
                                            new Object[] {exception.getMessage(), path,
                                                          Integer.valueOf(exception.getLineNumber()),
                                                          Integer.valueOf(exception.getColumnNumber())});
            }
            
            private void throwDatabindingException(String message) {
                //DatabindingException is quirky. This dance is required to get the full message
                //to where it belongs.
                DatabindingException e = new DatabindingException(message);
                e.setMessage(message);
                throw e;
            }

            public void error(SAXParseException exception) throws SAXException {
                String message = errorMessage(exception);
                LOG.log(Level.SEVERE, message, exception);
                throwDatabindingException(message);

            }

            public void fatalError(SAXParseException exception) throws SAXException {
                String message = errorMessage(exception);
                LOG.log(Level.SEVERE, message, exception);
                throwDatabindingException(message);
            }

            public void warning(SAXParseException exception) throws SAXException {
                LOG.log(Level.INFO, errorMessage(exception), exception);
            }
        });

        try {
            doc = documentBuilder.parse(is);
        } catch (SAXException e) {
            LOG.log(Level.SEVERE, "Error parsing Aegis file.", e); // can't happen due to
                                                        // above.
            return null;
        }
        return doc;
    }

    protected Document getDocument(Class clazz) {
        if (clazz == null) {
            return null;
        }
        Document doc = documents.get(clazz.getName());
        if (doc != null) {
            return doc;
        }
        String path = '/' + clazz.getName().replace('.', '/') + ".aegis.xml";
        InputStream is = clazz.getResourceAsStream(path);
        if (is == null) {
            LOG.finest("Mapping file : " + path + " not found.");
            return null;
        }
        LOG.finest("Found mapping file : " + path);
        try {
            doc = readAegisFile(is, path);
            documents.put(clazz.getName(), doc);
            return doc;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error loading file " + path, e);
            return null;
        }
    }

    @Override
    protected boolean isEnum(Class javaType) {
        Element mapping = findMapping(javaType);
        if (mapping != null) {
            return super.isEnum(javaType);
        } else {
            return nextCreator.isEnum(javaType);
        }
    }

    @Override
    public AegisType createEnumType(TypeClassInfo info) {
        Element mapping = findMapping(info.getType());
        if (mapping != null) {
            return super.createEnumType(info);
        } else {
            return nextCreator.createEnumType(info);
        }
    }

    @Override
    public AegisType createCollectionType(TypeClassInfo info) {
        /* If it is a parameterized type, then we already know
         * the parameter(s) and we don't need to fish them out of the XML.
         */
        if (info.getType() instanceof Class) {
            return createCollectionTypeFromGeneric(info);
        }

        return nextCreator.createCollectionType(info);
    }

    @Override
    public TypeClassInfo createClassInfo(PropertyDescriptor pd) {
        Element mapping = findMapping(pd.getReadMethod().getDeclaringClass());
        if (mapping == null) {
            return nextCreator.createClassInfo(pd);
        }

        Element propertyEl = getMatch(mapping, "./property[@name='" + pd.getName() + "']");
        if (propertyEl == null) {
            return nextCreator.createClassInfo(pd);
        }

        TypeClassInfo info = new TypeClassInfo();
        Type returnType = pd.getReadMethod().getGenericReturnType();
        info.setType(returnType);
        info.setDescription("property " + pd.getDisplayName());
        readMetadata(info, mapping, propertyEl);

        return info;
    }

    protected Element findMapping(Type type) {
        // We are not prepared to find .aegis.xml files for Parameterized types.
        
        Class<?> clazz = TypeUtil.getTypeClass(type, false);
        
        if (clazz == null) {
            return null;
        }
        Document doc = getDocument(clazz);
        if (doc == null) {
            return null;
        }

        Element mapping = getMatch(doc, "/mappings/mapping[@uri='" 
                                   + getTypeMapping().getMappingIdentifierURI()
                                   + "']");
        if (mapping == null) {
            mapping = getMatch(doc, "/mappings/mapping[not(@uri)]");
        }

        return mapping;
    }

    protected List<Element> findMappings(Type type) {
        Class clazz = TypeUtil.getTypeClass(type, false);
        List<Element> mappings = new ArrayList<Element>();
        if (clazz == null) {
            return mappings;
        }

        Element top = findMapping(clazz);
        if (top != null) {
            mappings.add(top);
        }

        Class parent = clazz;
        while (true) {

            // Read mappings for interfaces as well
            Class[] interfaces = parent.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                Class interfaze = interfaces[i];
                List<Element> interfaceMappings = findMappings(interfaze);
                mappings.addAll(interfaceMappings);
            }

            Class sup = parent.getSuperclass();

            if (sup == null || stopClasses.contains(sup)) {
                break;
            }

            Element mapping = findMapping(sup);
            if (mapping != null) {
                mappings.add(mapping);
            }

            parent = sup;
        }

        return mappings;
    }

    @Override
    public AegisType createDefaultType(TypeClassInfo info) {
        Element mapping = findMapping(info.getType());
        List<Element> mappings = findMappings(info.getType());
        Class<?> relatedClass = TypeUtil.getTypeRelatedClass(info.getType());

        if (mapping != null || mappings.size() > 0) {
            String typeNameAtt = null;
            if (mapping != null) {
                typeNameAtt = DOMUtils.getAttributeValueEmptyNull(mapping, "name");
            }

            String extensibleElements = null;
            if (mapping != null) {
                extensibleElements = mapping.getAttribute("extensibleElements");
            }

            String extensibleAttributes = null;
            if (mapping != null) {
                extensibleAttributes = mapping.getAttribute("extensibleAttributes");
            }

            String defaultNS = NamespaceHelper.makeNamespaceFromClassName(relatedClass.getName(), "http");
            QName name = null;
            if (typeNameAtt != null) {
                name = NamespaceHelper.createQName(mapping, typeNameAtt, defaultNS);

                defaultNS = name.getNamespaceURI();
            }

            // We do not deal with Generic beans at this point.
            XMLBeanTypeInfo btinfo = new XMLBeanTypeInfo(relatedClass, mappings, defaultNS);
            btinfo.setTypeMapping(getTypeMapping());
            btinfo.setDefaultMinOccurs(getConfiguration().getDefaultMinOccurs());
            btinfo.setDefaultNillable(getConfiguration().isDefaultNillable());

            if (extensibleElements != null) {
                btinfo.setExtensibleElements(Boolean.valueOf(extensibleElements).booleanValue());
            } else {
                btinfo.setExtensibleElements(getConfiguration().isDefaultExtensibleElements());
            }

            if (extensibleAttributes != null) {
                btinfo.setExtensibleAttributes(Boolean.valueOf(extensibleAttributes).booleanValue());
            } else {
                btinfo.setExtensibleAttributes(getConfiguration().isDefaultExtensibleAttributes());
            }
            
            btinfo.setQualifyAttributes(this.getConfiguration().isQualifyAttributes());
            btinfo.setQualifyElements(this.getConfiguration().isQualifyElements());
            BeanType type = new BeanType(btinfo);

            if (name == null) {
                name = createQName(relatedClass);
            }

            type.setSchemaType(name);

            type.setTypeClass(info.getType());
            type.setTypeMapping(getTypeMapping());

            return type;
        } else {
            return nextCreator.createDefaultType(info);
        }
    }

    @Override
    public TypeClassInfo createClassInfo(Method m, int index) {
        Element mapping = findMapping(m.getDeclaringClass());

        if (mapping == null) {
            return nextCreator.createClassInfo(m, index);
        }

        // find the elements that apply to the specified method
        TypeClassInfo info = nextCreator.createClassInfo(m, index); // start
        // with the
        // java5
        // (or whatever) version.
        if (info == null) {
            info = new TypeClassInfo();
        }

        info.setDescription("method " + m.getName() + " parameter " + index);
        if (index >= 0) {
            if (index >= m.getParameterTypes().length) {
                throw new DatabindingException("Method " 
                                               + m 
                                               + " does not have a parameter at index " 
                                               + index);
            }
            // we don't want nodes for which the specified index is not
            // specified
            List<Element> nodes = getMatches(mapping, "./method[@name='" + m.getName()
                                                      + "']/parameter[@index='" + index + "']/parent::*");
            if (nodes.size() == 0) {
                // no mapping for this method
                return info;
            }
            // pick the best matching node
            Element bestMatch = getBestMatch(mapping, m, nodes);

            if (bestMatch == null) {
                // no mapping for this method
                return info;
            }
            info.setType(m.getGenericParameterTypes()[index]);
            // info.setAnnotations(m.getParameterAnnotations()[index]);
            Element parameter = getMatch(bestMatch, "parameter[@index='" + index + "']");
            readMetadata(info, mapping, parameter);
        } else {
            List<Element> nodes = getMatches(mapping, "./method[@name='" + m.getName()
                                                      + "']/return-type/parent::*");
            if (nodes.size() == 0) {
                return info;
            }
            Element bestMatch = getBestMatch(mapping, m, nodes);
            if (bestMatch == null) {
                // no mapping for this method
                return info;
            }
            info.setType(m.getGenericReturnType());
            // info.setAnnotations(m.getAnnotations());
            Element rtElement = DOMUtils.getFirstChildWithName(bestMatch, "", "return-type");
            readMetadata(info, mapping, rtElement);
        }

        return info;
    }

    protected void readMetadata(TypeClassInfo info, Element mapping, Element parameter) {
        info.setTypeName(createQName(parameter, DOMUtils.getAttributeValueEmptyNull(parameter, "typeName")));
        info.setMappedName(createQName(parameter, 
                                       DOMUtils.getAttributeValueEmptyNull(parameter, "mappedName")));
        Class<?> relatedClass = TypeUtil.getTypeRelatedClass(info.getType());
        // we only mess with the generic issues for list and map
        if (Collection.class.isAssignableFrom(relatedClass)) {
            Type componentType = getComponentType(mapping, parameter);
            if (componentType != null) { // there is actually XML config.
                Type fullType = ParameterizedTypeFactory.createParameterizedType(relatedClass,
                                                                                 new Type[] {componentType});
                info.setType(fullType);
            }
        } else if (Map.class.isAssignableFrom(relatedClass)) {
            Type keyType = getKeyType(mapping, parameter);
            if (keyType != null) {
                info.setKeyType(keyType);
            }
            Type valueType = getValueType(mapping, parameter);
            if (valueType != null) {
                info.setValueType(valueType);
            }
            // if the XML only specifies one, we expect the other to come from a full
            // parameterized type.
            if (keyType != null || valueType != null) {
                if (keyType == null || valueType == null) {
                    if (keyType == null) {
                        keyType = TypeUtil.getSingleTypeParameter(info.getType(), 0);
                    }
                    if (keyType == null) {
                        keyType = Object.class;
                    }
                    if (valueType == null) {
                        valueType = TypeUtil.getSingleTypeParameter(info.getType(), 1);
                    }
                    if (valueType == null) {
                        valueType = Object.class;
                    }
                }
                Type fullType 
                    = ParameterizedTypeFactory.createParameterizedType(relatedClass,
                                                                       new Type[] {keyType, valueType});
                info.setType(fullType);
                
            }
            

        }
        setType(info, parameter);

        String min = DOMUtils.getAttributeValueEmptyNull(parameter, "minOccurs");
        if (min != null) {
            info.setMinOccurs(Long.parseLong(min));
        }

        String max = DOMUtils.getAttributeValueEmptyNull(parameter, "maxOccurs");
        if (max != null) {
            info.setMaxOccurs(Long.parseLong(max));
        }

        String flat = DOMUtils.getAttributeValueEmptyNull(parameter, "flat");
        if (flat != null) {
            info.setFlat(Boolean.valueOf(flat.toLowerCase()).booleanValue());
        }
        
        String nillable = DOMUtils.getAttributeValueEmptyNull(parameter, "nillable");
        if (nillable != null) {
            info.setNillable(Boolean.valueOf(nillable.toLowerCase()).booleanValue());
        }
    }

    @Override
    protected AegisType getOrCreateGenericType(TypeClassInfo info) {
        AegisType type = null;
        if (info.getType() instanceof ParameterizedType) { 
            type = createTypeFromGeneric(info.getType());
        }

        if (type == null) {
            type = super.getOrCreateGenericType(info);
        }

        return type;
    }

    private AegisType createTypeFromGeneric(Object cType) {
        if (cType instanceof TypeClassInfo) {
            return createTypeForClass((TypeClassInfo)cType);
        } else if (cType instanceof Class) {
            return createType((Class)cType);
        } else {
            return null;
        }
    }

    @Override
    protected AegisType getOrCreateMapKeyType(TypeClassInfo info) {
        AegisType type = null;
        if (info.getKeyType() != null) {
            type = createTypeFromGeneric(info.getKeyType());
        }

        if (type == null) {
            type = super.getOrCreateMapKeyType(info);
        }

        return type;
    }

    @Override
    protected AegisType getOrCreateMapValueType(TypeClassInfo info) {
        AegisType type = null;
        if (info.getType() instanceof ParameterizedType) {
            // well, let's hope that someone has filled in the value type.
            type = createTypeFromGeneric(info.getValueType());
        }

        if (type == null) {
            type = super.getOrCreateMapValueType(info);
        }

        return type;
    }

    private Type getComponentType(Element mapping, Element parameter) {
        String componentSpec = DOMUtils.getAttributeValueEmptyNull(parameter, "componentType");
        if (componentSpec == null) {
            return null;
        }
        return getGenericParameterFromSpec(mapping, componentSpec);
    }
    
    private Type getKeyType(Element mapping, Element parameter) {
        String spec = DOMUtils.getAttributeValueEmptyNull(parameter, "keyType");
        if (spec == null) {
            return null;
        }
        return getGenericParameterFromSpec(mapping, spec);
    }

    private Type getValueType(Element mapping, Element parameter) {
        String spec = DOMUtils.getAttributeValueEmptyNull(parameter, "valueType");
        if (spec == null) {
            return null;
        }
        return getGenericParameterFromSpec(mapping, spec);
    }

    // This cannot do List<List<x>>.
    private Type getGenericParameterFromSpec(Element mapping, String componentType) {
        if (componentType.startsWith("#")) {
            String name = componentType.substring(1);
            Element propertyEl = getMatch(mapping, "./component[@name='" + name + "']");
            if (propertyEl == null) {
                throw new DatabindingException("Could not find <component> element in mapping named '" + name
                                               + "'");
            }

            String className = DOMUtils.getAttributeValueEmptyNull(propertyEl, "class");
            if (className == null) {
                throw new DatabindingException("A 'class' attribute must be specified for <component> "
                                               + name);
            }
            
            return loadComponentClass(className);
        } else {
            return loadComponentClass(componentType);
        }
    }

    private Class loadComponentClass(String componentType) {
        try {
            return ClassLoaderUtils.loadClass(componentType, getClass());
        } catch (ClassNotFoundException e) {
            throw new DatabindingException("Unable to load component type class " + componentType, e);
        }
    }

    protected void setType(TypeClassInfo info, Element parameter) {
        String type = DOMUtils.getAttributeValueEmptyNull(parameter, "type");
        if (type != null) {
            try {
                Class aegisTypeClass = ClassLoaderUtils.loadClass(type, getClass()); 
                info.setAegisTypeClass(Java5TypeCreator.castToAegisTypeClass(aegisTypeClass));
            } catch (ClassNotFoundException e) {
                throw new DatabindingException("Unable to load type class " + type, e);
            }
        }
    }



    private Element getBestMatch(Element mapping, Method method, List<Element> availableNodes) {
        // first find all the matching method names
        List<Element> nodes = getMatches(mapping, "./method[@name='" + method.getName() + "']");
        // remove the ones that aren't in our acceptable set, if one is
        // specified
        if (availableNodes != null) {
            nodes.retainAll(availableNodes);
        }
        // no name found, so no matches
        if (nodes.size() == 0) {
            return null;
        }
        // if the method has no params, then more than one mapping is pointless
        Class[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            return nodes.get(0);
        }
        // here's the fun part.
        // we go through the method parameters, ruling out matches
        for (int i = 0; i < parameterTypes.length; i++) {
            Class parameterType = parameterTypes[i];
            for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
                Element element = (Element)iterator.next();
                // first we check if the parameter index is specified
                Element match = getMatch(element, "parameter[@index='" + i + "']");
                if (match != null
                // we check if the type is specified and matches
                    && DOMUtils.getAttributeValueEmptyNull(match, "class") != null
                    // if it doesn't match, then we can definitely rule out
                    // this result
                    && !DOMUtils.getAttributeValueEmptyNull(match, "class").equals(parameterType.getName())) {

                    iterator.remove();
                }
            }
        }
        // if we have just one node left, then it has to be the best match
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        // all remaining definitions could apply, so we need to now pick the
        // best one
        // the best one is the one with the most parameters specified
        Element bestCandidate = null;
        int highestSpecified = 0;
        for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
            Element element = (Element)iterator.next();

            List<Element> params = DOMUtils.getChildrenWithName(element, "", "parameter");
            int availableParameters = params.size();
            if (availableParameters > highestSpecified) {
                bestCandidate = element;
                highestSpecified = availableParameters;
            }
        }
        return bestCandidate;
    }

    private Element getMatch(Node doc, String xpath) {
        return (Element)xpathUtils.getValue(xpath, doc, XPathConstants.NODE);
    }

    private List<Element> getMatches(Node doc, String xpath) {
        NodeList nl = (NodeList)xpathUtils.getValue(xpath, doc, XPathConstants.NODESET);
        List<Element> r = new ArrayList<Element>();
        for (int x = 0; x < nl.getLength(); x++) {
            r.add((Element)nl.item(x));
        }
        return r;
    }

    /**
     * Creates a QName from a string, such as "ns:Element".
     */
    protected QName createQName(Element e, String value) {
        if (value == null || value.length() == 0) {
            return null;
        }

        int index = value.indexOf(":");

        if (index == -1) {
            return new QName(getTypeMapping().getMappingIdentifierURI(), value);
        }

        String prefix = value.substring(0, index);
        String localName = value.substring(index + 1);
        String ns = DOMUtils.getNamespace(e, prefix);

        if (ns == null || localName == null) {
            throw new DatabindingException("Invalid QName in mapping: " + value);
        }

        return new QName(ns, localName, prefix);
    }
}
