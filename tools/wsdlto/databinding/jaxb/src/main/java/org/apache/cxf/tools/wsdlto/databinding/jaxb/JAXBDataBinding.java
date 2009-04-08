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
package org.apache.cxf.tools.wsdlto.databinding.jaxb;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.SchemaFactory;


import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.api.Mapping;
import com.sun.tools.xjc.api.Property;
import com.sun.tools.xjc.api.S2JJAXBModel;
import com.sun.tools.xjc.api.SchemaCompiler;
import com.sun.tools.xjc.api.TypeAndAnnotation;
import com.sun.tools.xjc.api.XJC;

import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.DefaultValueWriter;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.util.JAXBUtils;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.DefaultValueProvider;
import org.apache.cxf.tools.wsdlto.core.RandomValueProvider;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaSerializer;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;

public class JAXBDataBinding implements DataBindingProfile {


    public class LocationFilterReader extends StreamReaderDelegate implements XMLStreamReader {
        boolean isImport;
        int locIdx = -1;
        LocationFilterReader(XMLStreamReader read) {
            super(read);
        }

        public int next() throws XMLStreamException {
            int i = super.next();
            if (i == XMLStreamReader.START_ELEMENT) {
                QName qn = super.getName();
                isImport = qn.equals(WSDLConstants.QNAME_SCHEMA_IMPORT);
                if (isImport) {
                    findLocation();
                } else {
                    locIdx = -1;
                }
            } else {
                isImport = false;
                locIdx = -1;
            }
            return i;
        }

        public int nextTag() throws XMLStreamException {
            int i = super.nextTag();
            if (i == XMLStreamReader.START_ELEMENT) {
                isImport = super.getName().equals(WSDLConstants.QNAME_SCHEMA_IMPORT);
                if (isImport) {
                    findLocation();
                } else {
                    locIdx = -1;
                }
            } else {
                isImport = false;
                locIdx = -1;
            }
            return i;
        }
        private void findLocation() {
            locIdx = -1;
            for (int x = super.getAttributeCount(); x > 0; --x) {
                String nm = super.getAttributeLocalName(x - 1);
                if ("schemaLocation".equals(nm)) {
                    locIdx = x - 1;
                }
            }
        }
        public String getAttributeValue(String namespaceURI, String localName) {
            return super.getAttributeValue(namespaceURI, localName);
        }
    
        public int getAttributeCount() {
            int i = super.getAttributeCount();
            if (locIdx != -1) {
                --i;
            }
            return i;
        }
        private int mapIdx(int index) {
            if (locIdx != -1
                && index >= locIdx) {
                ++index;
            }
            return index;
        }
    
        public QName getAttributeName(int index) {
            return super.getAttributeName(mapIdx(index));
        }
    
        public String getAttributePrefix(int index) {
            return super.getAttributePrefix(mapIdx(index));
        }
    
        public String getAttributeNamespace(int index) {
            return super.getAttributeNamespace(mapIdx(index));
        }
    
        public String getAttributeLocalName(int index) {
            return super.getAttributeLocalName(mapIdx(index));
        }
    
        public String getAttributeType(int index) {
            return super.getAttributeType(mapIdx(index));
        }
    
        public String getAttributeValue(int index) {
            return super.getAttributeValue(mapIdx(index));
        }
    
        public boolean isAttributeSpecified(int index) {
            return super.isAttributeSpecified(mapIdx(index));
        }

        

    }


    private static final Logger LOG = LogUtils.getL7dLogger(JAXBDataBinding.class);
    
    private static final Set<String> DEFAULT_TYPE_MAP = new HashSet<String>();
    private static final Map<String, String> JLDEFAULT_TYPE_MAP = new HashMap<String, String>();
    
    private S2JJAXBModel rawJaxbModelGenCode;
    private ToolContext context;
    private DefaultValueProvider defaultValues;
    private boolean initialized;
    
    static {
        DEFAULT_TYPE_MAP.add("boolean");
        DEFAULT_TYPE_MAP.add("int");
        DEFAULT_TYPE_MAP.add("long");
        DEFAULT_TYPE_MAP.add("short");
        DEFAULT_TYPE_MAP.add("byte");
        DEFAULT_TYPE_MAP.add("float");
        DEFAULT_TYPE_MAP.add("double");
        DEFAULT_TYPE_MAP.add("char");
        DEFAULT_TYPE_MAP.add("java.lang.String");
        DEFAULT_TYPE_MAP.add("javax.xml.namespace.QName");
        DEFAULT_TYPE_MAP.add("java.net.URI");
        DEFAULT_TYPE_MAP.add("java.math.BigInteger");
        DEFAULT_TYPE_MAP.add("java.math.BigDecimal");
        DEFAULT_TYPE_MAP.add("javax.xml.datatype.XMLGregorianCalendar");
        DEFAULT_TYPE_MAP.add("javax.xml.datatype.Duration");
        
        JLDEFAULT_TYPE_MAP.put("java.lang.Character", "char");
        JLDEFAULT_TYPE_MAP.put("java.lang.Boolean", "boolean");
        JLDEFAULT_TYPE_MAP.put("java.lang.Integer", "int");
        JLDEFAULT_TYPE_MAP.put("java.lang.Long", "long");
        JLDEFAULT_TYPE_MAP.put("java.lang.Short", "short");
        JLDEFAULT_TYPE_MAP.put("java.lang.Byte", "byte");
        JLDEFAULT_TYPE_MAP.put("java.lang.Float", "float");
        JLDEFAULT_TYPE_MAP.put("java.lang.Double", "double");
        DEFAULT_TYPE_MAP.addAll(JLDEFAULT_TYPE_MAP.keySet());
    }   
    
    public void initialize(ToolContext c) throws ToolException {
        this.context = c;
        
        SchemaCompiler schemaCompiler = XJC.createSchemaCompiler();
        ClassCollector classCollector = context.get(ClassCollector.class);
        //installResolverIntoSchemaCompiler(schemaCompiler);
        
        ClassNameAllocatorImpl allocator 
            = new ClassNameAllocatorImpl(classCollector,
                                         c.optionSet(ToolConstants.CFG_AUTORESOLVE));

        schemaCompiler.setClassNameAllocator(allocator);
           
        JAXBBindErrorListener listener = new JAXBBindErrorListener(context.isVerbose());
        schemaCompiler.setErrorListener(listener);
        // Collection<SchemaInfo> schemas = serviceInfo.getSchemas();
        List<InputSource> jaxbBindings = context.getJaxbBindingFile();
        SchemaCollection schemas = (SchemaCollection) context.get(ToolConstants.XML_SCHEMA_COLLECTION);
        
        Options opts = null;
        opts = getOptions(schemaCompiler);
        
        List<String> args = new ArrayList<String>();
        
        if (context.get(ToolConstants.CFG_NO_ADDRESS_BINDING) == null) {
            //hard code to enable jaxb extensions
            args.add("-extension");
            URL bindingFileUrl = getClass().getResource("W3CEPRJaxbBinding.xml");
            InputSource ins = new InputSource(bindingFileUrl.toString());
            opts.addBindFile(ins);
        }
        
        if (context.get(ToolConstants.CFG_XJC_ARGS) != null) {
            String xjcArgs = (String)context.get(ToolConstants.CFG_XJC_ARGS);
            StringTokenizer tokenizer = new StringTokenizer(xjcArgs, ",", false);
            while (tokenizer.hasMoreTokens()) {
                String arg = tokenizer.nextToken();
                if ("-npa".equalsIgnoreCase(arg)) {
                    throw new ToolException("NPA_NOT_SUPPORTED", LOG);
                }
                args.add(arg);
                LOG.log(Level.FINE, "xjc arg:" + arg);
            }
        }

        if (context.get(ToolConstants.CFG_NO_ADDRESS_BINDING) == null
            || context.get(ToolConstants.CFG_XJC_ARGS) != null) {
            try {
                // keep parseArguments happy, supply dummy required command-line
                // opts
                opts.addGrammar(new InputSource("null"));
                opts.parseArguments(args.toArray(new String[] {}));
            } catch (BadCommandLineException e) {
                String msg = "XJC reported 'BadCommandLineException' for -xjc argument:";
                for (String arg : args) {
                    msg = msg + arg + " ";
                }
                LOG.log(Level.FINE, msg, e);
                if (opts != null) {
                    String pluginUsage = getPluginUsageString(opts);
                    msg = msg + System.getProperty("line.separator");
                    if (args.contains("-X")) {
                        msg = pluginUsage;
                    } else {
                        msg += pluginUsage;
                    }
                }

                throw new ToolException(msg, e);
            }
        }
        
        addSchemas(opts, schemaCompiler, schemas);
        for (InputSource binding : jaxbBindings) {
            opts.addBindFile(binding);
        }

                       
        for (String ns : context.getNamespacePackageMap().keySet()) {
            File file = JAXBUtils.getPackageMappingSchemaBindingFile(ns, context.mapPackageName(ns));
            try {
                InputSource ins = new InputSource(file.toURI().toString());
                schemaCompiler.parseSchema(ins);
            } finally {
                FileUtils.delete(file);                
            }
        }
        
        if (context.getPackageName() != null) {
            schemaCompiler.setDefaultPackageName(context.getPackageName());
        }  
        

        rawJaxbModelGenCode = schemaCompiler.bind();

        addedEnumClassToCollector(schemas, allocator);

        if (context.get(ToolConstants.CFG_DEFAULT_VALUES) != null) {
            String cname = (String)context.get(ToolConstants.CFG_DEFAULT_VALUES);
            if (StringUtils.isEmpty(cname)) {
                defaultValues = new RandomValueProvider();
            } else {
                if (cname.charAt(0) == '=') {
                    cname = cname.substring(1);
                }
                try {
                    defaultValues = (DefaultValueProvider)Class.forName(cname).newInstance();
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, e.getMessage());
                    throw new ToolException(e);
                }
            }
        }
        initialized = true;
    }

    @SuppressWarnings("unchecked")
    private void addSchemas(Options opts, 
                            SchemaCompiler schemaCompiler,
                            SchemaCollection schemaCollection) {
        
        Set<String> ids = new HashSet<String>();
        List<ServiceInfo> serviceList = (List<ServiceInfo>)context.get(ToolConstants.SERVICE_LIST);
        for (ServiceInfo si : serviceList) {
            for (SchemaInfo sci : si.getSchemas()) {
                String key = sci.getSystemId();
                if (ids.contains(key)) {
                    continue;
                }
                ids.add(key);
            }
        }
        for (XmlSchema schema : schemaCollection.getXmlSchemas()) {
            if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(schema.getTargetNamespace())) {
                continue;
            }
            String key = schema.getSourceURI();
            if (ids.contains(key)) {
                continue;
            }
            
            if (!key.startsWith("file:")) {
                XmlSchemaSerializer xser = new XmlSchemaSerializer();
                xser.setExtReg(schemaCollection.getExtReg());
                Document[] docs;
                try {
                    docs = xser.serializeSchema(schema, false);
                } catch (XmlSchemaSerializerException e) {
                    throw new RuntimeException(e);
                }
                Element ele = docs[0].getDocumentElement();
    
                ele = removeImportElement(ele);
                if (context.get(ToolConstants.CFG_VALIDATE_WSDL) != null) {
                    validateSchema(ele);
                }           
                InputSource is = new InputSource((InputStream)null);
                //key = key.replaceFirst("#types[0-9]+$", "");
                is.setSystemId(key);
                is.setPublicId(key);
                opts.addGrammar(is);
                schemaCompiler.parseSchema(key, ele);
            }
        }
        for (XmlSchema schema : schemaCollection.getXmlSchemas()) {
            if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(schema.getTargetNamespace())) {
                continue;
            }
            String key = schema.getSourceURI();
            if (ids.contains(key)) {
                continue;
            }
            if (key.startsWith("file:")) {
                try {
                    FileInputStream fin = new FileInputStream(new File(new URI(key)));
                    XMLStreamReader reader = StaxUtils.createXMLStreamReader(key, fin);
                    reader = new LocationFilterReader(reader);
                    InputSource is = new InputSource(key);
                    opts.addGrammar(is);
                    schemaCompiler.parseSchema(key, reader);
                    reader.close();
                } catch (RuntimeException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        ids.clear();
        for (ServiceInfo si : serviceList) {
            for (SchemaInfo sci : si.getSchemas()) {
                String key = sci.getSystemId();
                if (ids.contains(key)) {
                    continue;
                }
                ids.add(key);
                Element ele = sci.getElement();
                ele = removeImportElement(ele);
                if (context.get(ToolConstants.CFG_VALIDATE_WSDL) != null) {
                    validateSchema(ele);
                }           
                InputSource is = new InputSource((InputStream)null);
                //key = key.replaceFirst("#types[0-9]+$", "");
                is.setSystemId(key);
                is.setPublicId(key);
                opts.addGrammar(is);
                schemaCompiler.parseSchema(key, ele);
            }
        }

    }
    private String getPluginUsageString(Options opts) {
        StringBuffer buf = new StringBuffer();
        buf.append("\navaliable plugin options:\n");
        for (Plugin pl : opts.getAllPlugins()) {
            buf.append(pl.getUsage());
            buf.append('\n');
        }
        return buf.toString();
    }

    // JAXB 'deprecates' getOptions, by which they mean that they reserve the right to change it.
    @SuppressWarnings("deprecation")
    private Options getOptions(SchemaCompiler schemaCompiler) throws ToolException {
        return schemaCompiler.getOptions();
    }

    // JAXB bug. JAXB ClassNameCollector may not be invoked when generated
    // class is an enum. We need to use this method to add the missed file
    // to classCollector.
    private void addedEnumClassToCollector(SchemaCollection schemaCollection, 
                                           ClassNameAllocatorImpl allocator) {
        //for (Element schemaElement : schemaList.values()) {
        for (XmlSchema schema : schemaCollection.getXmlSchemas()) {
            String targetNamespace = schema.getTargetNamespace();
            if (StringUtils.isEmpty(targetNamespace)) {
                continue;
            }
            String packageName = context.mapPackageName(targetNamespace);
            if (!addedToClassCollector(packageName)) {
                allocator.assignClassName(packageName, "*");
            }
        }
    }

    private boolean addedToClassCollector(String packageName) {
        ClassCollector classCollector = context.get(ClassCollector.class);
        Collection<String> files = classCollector.getGeneratedFileInfo();
        for (String file : files) {
            int dotIndex = file.lastIndexOf(".");
            String sub = file.substring(0, dotIndex - 1);
            if (sub.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSuppressCodeGen() {
        return context.optionSet(ToolConstants.CFG_SUPPRESS_GEN);
    }

    public void generate(ToolContext c) throws ToolException {
        if (!initialized) {
            initialize(c);
        }
        if (rawJaxbModelGenCode == null) {
            return;
        }

        try {
            String dir = (String)context.get(ToolConstants.CFG_OUTPUTDIR);

            TypesCodeWriter fileCodeWriter = new TypesCodeWriter(new File(dir), context.getExcludePkgList());

            if (rawJaxbModelGenCode instanceof S2JJAXBModel) {
                S2JJAXBModel schem2JavaJaxbModel = (S2JJAXBModel)rawJaxbModelGenCode;

                ClassCollector classCollector = context.get(ClassCollector.class);
                for (JClass cls : schem2JavaJaxbModel.getAllObjectFactories()) {
                    classCollector.getTypesPackages().add(cls._package().name());
                }

                JCodeModel jcodeModel = schem2JavaJaxbModel.generateCode(null, null);

                if (!isSuppressCodeGen()) {
                    jcodeModel.build(fileCodeWriter);
                }

                context.put(JCodeModel.class, jcodeModel);

                for (String str : fileCodeWriter.getExcludeFileList()) {
                    context.getExcludeFileList().add(str);
                }
            }
            return;
        } catch (IOException e) {
            Message msg = new Message("FAIL_TO_GENERATE_TYPES", LOG);
            throw new ToolException(msg);
        }
    }

    public String getType(QName qname, boolean element) {
        TypeAndAnnotation typeAnno = rawJaxbModelGenCode.getJavaType(qname);
        if (element) {
            Mapping mapping = rawJaxbModelGenCode.get(qname);
            if (mapping != null) {
                typeAnno = mapping.getType();
            }
        }

        if (typeAnno != null && typeAnno.getTypeClass() != null) {
            return typeAnno.getTypeClass().fullName();
        }
        return null;

    }

    public String getWrappedElementType(QName wrapperElement, QName item) {
        Mapping mapping = rawJaxbModelGenCode.get(wrapperElement);
        if (mapping != null) {
            List<? extends Property> propList = mapping.getWrapperStyleDrilldown();
            if (propList != null) {
                for (Property pro : propList) {
                    if (pro.elementName().getNamespaceURI().equals(item.getNamespaceURI())
                        && pro.elementName().getLocalPart().equals(item.getLocalPart())) {
                        return pro.type().fullName();
                    }
                }
            }
        }
        return null;
    }

    private Element removeImportElement(Element element) {
        List<Element> elemList = DOMUtils.findAllElementsByTagNameNS(element, 
                                                                     ToolConstants.SCHEMA_URI, 
                                                                     "import");
        if (elemList.size() == 0) {
            return element;
        }
        element = (Element)cloneNode(element.getOwnerDocument(), element, true);
        elemList = DOMUtils.findAllElementsByTagNameNS(element, 
                                                       ToolConstants.SCHEMA_URI, 
                                                       "import");
        List<Node> ns = new ArrayList<Node>();
        for (Element elem : elemList) {
            Node importNode = elem;
            ns.add(importNode);
        }
        for (Node item : ns) {
            Node schemaNode = item.getParentNode();
            schemaNode.removeChild(item);
        }
        return element;
    }

    public Node cloneNode(Document document, Node node, boolean deep) throws DOMException {
        if (document == null || node == null) {
            return null;
        }
        int type = node.getNodeType();

        if (node.getOwnerDocument() == document) {
            return node.cloneNode(deep);
        }
        Node clone;
        switch (type) {
        case Node.CDATA_SECTION_NODE:
            clone = document.createCDATASection(node.getNodeValue());
            break;
        case Node.COMMENT_NODE:
            clone = document.createComment(node.getNodeValue());
            break;
        case Node.ENTITY_REFERENCE_NODE:
            clone = document.createEntityReference(node.getNodeName());
            break;
        case Node.ELEMENT_NODE:
            clone = document.createElement(node.getNodeName());
            NamedNodeMap attributes = node.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                ((Element)clone).setAttribute(attributes.item(i).getNodeName(), attributes.item(i)
                    .getNodeValue());
            }
            break;

        case Node.TEXT_NODE:
            clone = document.createTextNode(node.getNodeValue());
            break;
        default:
            return null;
        }
        if (deep && type == Node.ELEMENT_NODE) {
            Node child = node.getFirstChild();
            while (child != null) {
                clone.appendChild(cloneNode(document, child, true));
                child = child.getNextSibling();
            }
        }
        return clone;
    }

    
    public void validateSchema(Element ele) throws ToolException {
        SchemaFactory schemaFact = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        DOMSource domSrc = new DOMSource(ele);
        try {
            schemaFact.newSchema(domSrc);
        } catch (SAXException e) {
            if (e.getLocalizedMessage().indexOf("src-resolve.4.2") > -1)  {
                //Ignore schema resolve error and do nothing
            } else {
                throw new ToolException("Schema Error : " + e.getLocalizedMessage(), e);
            }
        }
    }
    
    public DefaultValueWriter createDefaultValueWriter(QName qname, boolean element) {
        if (defaultValues == null) {
            return null;
        }
        TypeAndAnnotation typeAnno = rawJaxbModelGenCode.getJavaType(qname);
        if (element) {
            Mapping mapping = rawJaxbModelGenCode.get(qname);
            if (mapping != null) {
                typeAnno = mapping.getType();
            }
        }
        if (typeAnno != null) { 
            final JType type = typeAnno.getTypeClass();
            return new JAXBDefaultValueWriter(type);
        } 
        return null;
    }
    
    public DefaultValueWriter createDefaultValueWriterForWrappedElement(QName wrapperElement, QName item) {
        if (defaultValues != null) {
            Mapping mapping = rawJaxbModelGenCode.get(wrapperElement);
            if (mapping != null) {
                List<? extends Property> propList = mapping.getWrapperStyleDrilldown();
                for (Property pro : propList) {
                    if (pro.elementName().getNamespaceURI().equals(item.getNamespaceURI())
                        && pro.elementName().getLocalPart().equals(item.getLocalPart())) {
                        return new JAXBDefaultValueWriter(pro.type());
                    }
                }
            }
        }
        return null;
    }


    private class JAXBDefaultValueWriter implements DefaultValueWriter {
        final JType type;
        JAXBDefaultValueWriter(JType tp) {
            type = tp;
        }
        public void writeDefaultValue(Writer writer, String indent,
                                      String path, String varName) throws IOException {
            path = path + "/" + varName;
            writeDefaultValue(writer, indent, path, varName, type);
        }
        
        public void writeDefaultValue(Writer writer, String indent,
                                      String path, String varName,
                                      JType tp) throws IOException {
            writer.write(tp.fullName());
            writer.write(" ");
            writer.write(varName);
            writer.write(" = ");
            if (tp.isArray()) {
                writer.write("new ");
                writer.write(tp.fullName());
                writer.write(" {};");
            } else if (DEFAULT_TYPE_MAP.contains(tp.fullName())) {
                writeDefaultType(writer, tp, path);
                writer.write(";");
            } else if (tp instanceof JDefinedClass) {
                JDefinedClass jdc = (JDefinedClass)tp;
                if (jdc.getClassType() == ClassType.ENUM) {
                    //no way to get the field list as it's private with 
                    //no accessors :-(
                    try {
                        Field f = jdc.getClass().getDeclaredField("enumConstantsByName");
                        f.setAccessible(true);
                        Map map = (Map)f.get(jdc);
                        Set<String> values = CastUtils.cast(map.keySet()); 
                        String first = defaultValues.chooseEnumValue(path, values);
                        writer.write(tp.fullName());
                        writer.write(".");                        
                        writer.write(first);                        
                        writer.write(";");
                    } catch (Exception e) {
                        IOException ex = new IOException(e.getMessage());
                        ex.initCause(e);
                        throw ex;
                    }
                } else {
                    writer.write("new ");
                    writer.write(tp.fullName());
                    writer.write("();");
                    fillInFields(writer, indent, path, varName, jdc);
                }
            } else {
                boolean found = false;
                JType tp2 = tp.erasure();
                try {
                    Field f = tp2.getClass().getDeclaredField("_class");
                    f.setAccessible(true);
                    Class<?> cls = (Class)f.get(tp2);
                    if (List.class.isAssignableFrom(cls)) {
                        found = true;

                        writer.write("new ");
                        writer.write(tp.fullName().replace("java.util.List", "java.util.ArrayList"));
                        writer.write("();");
                        
                        f = tp.getClass().getDeclaredField("args");
                        f.setAccessible(true);
                        List<JClass> lcl = CastUtils.cast((List)f.get(tp));
                        JClass cl = lcl.get(0);
                        
                        int cnt = defaultValues.getListLength(path + "/" + varName);
                        for (int x = 0; x < cnt; x++) {

                            writer.write("\n");
                            writer.write(indent);
                            writeDefaultValue(writer, indent, path + "/" + varName + "Val",
                                              varName + "Val" + cnt , cl);
                            writer.write("\n");
                            writer.write(indent);
                            writer.write(varName);
                            writer.write(".add(");
                            writer.write(varName + "Val" + cnt);
                            writer.write(");");
                        }
                    }
                } catch (Exception e) {
                    //ignore
                }
                
                if (!found) {
                    //System.err.println("No idea what to do with " + tp.fullName());
                    //System.err.println("        class " + tp.getClass().getName());
                    writer.write("null;");
                }
            }
        }
        public void fillInFields(Writer writer, String indent,
                                      String path, String varName,
                                      JDefinedClass tp) throws IOException {
            JClass sp = tp._extends();
            if (sp instanceof JDefinedClass) {
                fillInFields(writer, indent, path, varName, (JDefinedClass)sp);
            }
            
            Collection<JMethod> methods = tp.methods();
            for (JMethod m : methods) {
                if (m.name().startsWith("set")) {
                    writer.write("\n");
                    writer.write(indent);
                    if (DEFAULT_TYPE_MAP.contains(m.listParamTypes()[0].fullName())) {
                        writer.write(varName);
                        writer.write(".");
                        writer.write(m.name());
                        writer.write("(");
                        writeDefaultType(writer, m.listParamTypes()[0], path + "/" + m.name().substring(3));
                        writer.write(");");
                    } else {
                        writeDefaultValue(writer, indent,
                                          path + "/" + m.name().substring(3),
                                          varName + m.name().substring(3),
                                          m.listParamTypes()[0]);
                        writer.write("\n");
                        writer.write(indent);
                        writer.write(varName);
                        writer.write(".");
                        writer.write(m.name());
                        writer.write("(");
                        writer.write(varName + m.name().substring(3));
                        writer.write(");");
                    }
                } else if (m.type().fullName().startsWith("java.util.List")) {
                    writer.write("\n");
                    writer.write(indent);
                    writeDefaultValue(writer, indent,
                                      path + "/" + m.name().substring(3),
                                      varName + m.name().substring(3),
                                      m.type());
                    writer.write("\n");
                    writer.write(indent);
                    writer.write(varName);
                    writer.write(".");
                    writer.write(m.name());
                    writer.write("().addAll(");
                    writer.write(varName + m.name().substring(3));
                    writer.write(");");                    
                }
            }
        }
        private void writeDefaultType(Writer writer, JType t, String path) throws IOException {
            String name = t.fullName();
            writeDefaultType(writer, name, path);
            
        }    
        private void writeDefaultType(Writer writer, String name, String path) throws IOException {
            if (JLDEFAULT_TYPE_MAP.containsKey(name)) {
                writer.append(name.substring("java.lang.".length())).append(".valueOf(");
                writeDefaultType(writer, JLDEFAULT_TYPE_MAP.get(name), path);
                writer.append(")");
            } else if ("boolean".equals(name)) {
                writer.append(defaultValues.getBooleanValue(path) ? "true" : "false");
            } else if ("int".equals(name)) {
                writer.append(Integer.toString(defaultValues.getIntValue(path)));
            } else if ("long".equals(name)) {
                writer.append(Long.toString(defaultValues.getLongValue(path))).append("l");
            } else if ("short".equals(name)) {
                writer.append("(short)").append(Short.toString(defaultValues.getShortValue(path)));
            } else if ("byte".equals(name)) {
                writer.append("(byte)").append(Byte.toString(defaultValues.getByteValue(path)));
            } else if ("float".equals(name)) {
                writer.append(Float.toString(defaultValues.getFloatValue(path))).append("f");
            } else if ("double".equals(name)) {
                writer.append(Double.toString(defaultValues.getDoubleValue(path)));
            } else if ("char".equals(name)) {
                writer.append("(char)").append(Character.toString(defaultValues.getCharValue(path)));
            } else if ("java.lang.String".equals(name)) {
                writer.append("\"")
                    .append(defaultValues.getStringValue(path))
                    .append("\"");
            } else if ("javax.xml.namespace.QName".equals(name)) {
                QName qn = defaultValues.getQNameValue(path);
                writer.append("new javax.xml.namespace.QName(\"")
                      .append(qn.getNamespaceURI())
                      .append("\", \"")
                      .append(qn.getLocalPart())
                      .append("\")");
            } else if ("java.net.URI".equals(name)) {
                writer.append("new java.net.URI(\"")
                      .append(defaultValues.getURIValue(path).toASCIIString())
                      .append("\")");
            } else if ("java.math.BigInteger".equals(name)) {
                writer.append("new java.math.BigInteger(\"")
                      .append(defaultValues.getBigIntegerValue(path).toString())
                      .append("\")");
            } else if ("java.math.BigDecimal".equals(name)) {
                writer.append("new java.math.BigDecimal(\"")
                      .append(defaultValues.getBigDecimalValue(path).toString())
                      .append("\")");
            } else if ("javax.xml.datatype.XMLGregorianCalendar".equals(name)) {
                writer.append("javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(\"")
                      .append(defaultValues.getXMLGregorianCalendarValueString(path))
                      .append("\")");
            } else if ("javax.xml.datatype.Duration".equals(name)) {
                writer.append("javax.xml.datatype.DatatypeFactory.newInstance().newDuration(\"")
                      .append(defaultValues.getDurationValueString(path))
                      .append("\")");
            }
        }
    }


}
