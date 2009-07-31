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

package org.apache.cxf.sdo.tools;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.DefaultValueWriter;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.tuscany.sdo.generate.XSD2JavaGenerator;
import org.apache.tuscany.sdo.helper.HelperContextImpl;
import org.apache.tuscany.sdo.helper.XSDHelperImpl;
import org.apache.ws.commons.schema.XmlSchema;
import org.eclipse.emf.codegen.ecore.genmodel.GenClass;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.util.BasicExtendedMetaData;
import org.eclipse.emf.ecore.util.ExtendedMetaData;

import commonj.sdo.DataObject;
import commonj.sdo.Property;
import commonj.sdo.Type;
import commonj.sdo.helper.HelperContext;
import commonj.sdo.helper.TypeHelper;
import commonj.sdo.helper.XSDHelper;

public class SDODatabinding extends XSD2JavaGenerator implements DataBindingProfile {
    private static final String DATABINDING_DYNAMIC_SDO = "sdo-dynamic";

    private TypeHelper typeHelper;
    private XSDHelper xsdHelper;

    private boolean dynamic;
    private ExtendedMetaData extendedMetaData;
    private GenModel genModel;
    private Map<EClassifier, GenClass> genClasses = new HashMap<EClassifier, GenClass>();
    private SchemaCollection schemaCollection;
    private EPackage.Registry packageRegistry;
    
    public void initialize(ToolContext context) throws ToolException {
        String databinding = (String)context.get(ToolConstants.CFG_DATABINDING);
        if (DATABINDING_DYNAMIC_SDO.equalsIgnoreCase(databinding)) {
            dynamic = true;
        }

        generatedPackages = null;
        String outputDir = (String)context.get(ToolConstants.CFG_OUTPUTDIR);
        String pkg = context.getPackageName();


        // preparing the directories where files to be written.
        File targetDir;
        if (outputDir == null) {
            String wsdl = (String)context.get(ToolConstants.CFG_WSDLLOCATION);
            try {
                outputDir = new File(new URI(wsdl)).getParentFile().getAbsolutePath();
            } catch (URISyntaxException e) {
                outputDir = new File(".").getAbsolutePath();
            }
        }
        targetDir = new File(outputDir);
        targetDir.mkdirs();

        List<String> argList = new ArrayList<String>();
        argList.add("-targetDirectory");
        argList.add(targetDir.getAbsolutePath());

        if (pkg != null) {
            argList.add("-javaPackage");
            argList.add(pkg);
        }
        schemaCollection = (SchemaCollection) context.get(ToolConstants.XML_SCHEMA_COLLECTION);

        argList.add(""); //bogus arg
        String[] args = argList.toArray(new String[argList.size()]);

        packageRegistry = new EPackageRegistryImpl(EPackage.Registry.INSTANCE);
        extendedMetaData = new BasicExtendedMetaData(packageRegistry);
        HelperContext hc = new HelperContextImpl(extendedMetaData, false);
        xsdHelper = hc.getXSDHelper();
        typeHelper = hc.getTypeHelper();

    
        processArguments(args);

        ((XSDHelperImpl)xsdHelper).setRedefineBuiltIn(generateBuiltIn);
        for (XmlSchema schema : schemaCollection.getXmlSchemas()) {
            if (schema.getTargetNamespace().equals(XmlSchemaConstants.XSD_NAMESPACE_URI)) {
                continue;
            }
            StringWriter writer = new StringWriter();
            schema.write(writer);
            xsdHelper.define(new StringReader(writer.toString()), schema.getSourceURI());
        }
    }
    public void generate(ToolContext context) throws ToolException {
        Map<String, String> ns2pkgMap = context.getNamespacePackageMap();
        Map<String, PackageInfo> packageInfoTable =
            createPackageInfoTable(schemaCollection, ns2pkgMap);
        ClassCollector classCollector = context.get(ClassCollector.class);
        try {
            if (!dynamic) {
                // No XSD2Java is needed for dynamic SDO

                genModel = generatePackages(packageRegistry.values(), targetDirectory,
                                            new Hashtable<String, PackageInfo>(packageInfoTable),
                                            genOptions, allNamespaces);
                
                
                List<GenPackage> packages = CastUtils.cast(genModel.getGenPackages());
                for (Iterator<GenPackage> iter = packages.iterator(); iter.hasNext();) {
                    // loop through the list, once to build up the eclass to genclass mapper
                    GenPackage genPackage = iter.next();
                    List<GenClass> classes = CastUtils.cast(genPackage.getGenClasses());
                    for (Iterator<GenClass> classIter = classes.iterator(); classIter.hasNext();) {
                        GenClass genClass = classIter.next();
                        genClasses.put(genClass.getEcoreClass(), genClass);
    
                        //This gets the "impl" classes, how do we get everything else?                        
                        String s = genClass.getQualifiedClassName();
                        String p = s.substring(0, s.lastIndexOf('.'));
                        s = s.substring(s.lastIndexOf('.') + 1);
                        classCollector.addTypesClassName(p, 
                                                         s,
                                                         genClass.getQualifiedClassName());
                        
                        p = genClass.getGenPackage().getInterfacePackageName();
                        s = genClass.getInterfaceName();
                        classCollector.addTypesClassName(p, 
                                                         s,
                                                         p + "." + s);
                    }
                    String p = genPackage.getInterfacePackageName();
                    String s = genPackage.getFactoryInterfaceName();
                    classCollector.addTypesClassName(p, 
                                                     s,
                                                     p + "." + s);
                    p = genPackage.getClassPackageName();
                    s = genPackage.getFactoryClassName();
                    classCollector.addTypesClassName(p, 
                                                     s,
                                                     p + "." + s);
                }
            }
        } catch (Exception e) {
            throw new ToolException(e);
        }

    }


    private Map<String, PackageInfo> createPackageInfoTable(SchemaCollection schemas,
                                                                   Map<String, String> ns2PkgMap) {
        Map<String, PackageInfo> packageInfoTable = new HashMap<String, PackageInfo>();

        if (ns2PkgMap != null && !ns2PkgMap.isEmpty()) {
            for (Map.Entry<String, String> e : ns2PkgMap.entrySet()) {
                packageInfoTable.put(e.getKey(), new PackageInfo(e.getValue(), null, e.getKey(), null));
            }
        } else {
            for (XmlSchema schema : schemas.getXmlSchemas()) {
                packageInfoTable.put(schema.getTargetNamespace(),
                                     new PackageInfo(javaPackage, prefix, schema.getTargetNamespace(), null));
            }
        }
        return packageInfoTable;
    }



    public String getType(QName qName, boolean element) {
        Type type = null;
        if (element) {
            Property property = xsdHelper.getGlobalProperty(qName.getNamespaceURI(),
                                                            qName.getLocalPart(), true);
            if (property != null) {
                type = property.getType();
            }
        }
        if (type == null) {
            type = typeHelper.getType(qName.getNamespaceURI(), qName.getLocalPart());
        }
        if (type != null) {
            return getClassName(type);
        }
        return DataObject.class.getName();
    }

    private String getClassName(Type type) {
        EClassifier eClassifier = (EClassifier)type;
        String name = eClassifier.getInstanceClassName();
        if (name != null) {
            return name;
        }

        if (genModel == null) {
            if (dynamic) {
                return DataObject.class.getName();
            }
            return type.getName();
        }

        if (eClassifier instanceof EClass) {
            // complex type
            GenClass genEClass = (GenClass)genClasses.get(eClassifier);
            if (genEClass != null) {
                name = genEClass.getGenPackage().getInterfacePackageName() 
                    + '.' + genEClass.getInterfaceName();

            }
        } else {
            // simple type
            name = eClassifier.getInstanceClass().getName();
        }
        return name;

    }

    public String getWrappedElementType(QName wrapperElement, QName item) {
        Type type = null;
        Property property =
            xsdHelper.getGlobalProperty(wrapperElement.getNamespaceURI(), 
                                        wrapperElement.getLocalPart(), true);
        
        if (property != null) {
            type = property.getType();
            Property itemProp = type.getProperty(item.getLocalPart());
            if (itemProp != null) {
                type = itemProp.getType();
            }
        }
        if (type != null) {
            return getClassName(type);
        }
        return DataObject.class.getName();
    }

    public DefaultValueWriter createDefaultValueWriter(QName qName, boolean b) {
        // since we dont need any sample client/server code with default values we return null
        return null;
    }

    public DefaultValueWriter createDefaultValueWriterForWrappedElement(QName qName, QName qName1) {
        // since we dont need any sample client/server code with default values we return null
        return null;
    }

}
