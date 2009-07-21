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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.DefaultValueWriter;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.tuscany.sdo.api.SDOUtil;
import org.apache.tuscany.sdo.generate.XSD2JavaGenerator;
import org.apache.tuscany.sdo.helper.HelperContextImpl;
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

    public void generate(ToolContext context) throws ToolException {
        if (dynamic) {
            // Node XSD2Java is needed for dynamic SDO
            return;
        }

        Map<String, String> ns2pkgMap = context.getNamespacePackageMap();

        String outputDir = (String)context.get(ToolConstants.CFG_OUTPUTDIR);
        String pkg = context.getPackageName();

        String wsdl = (String)context.get(ToolConstants.CFG_WSDLLOCATION);

        // preparing the directories where files to be written.
        File targetDir;
        if (outputDir == null) {
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

        // We need to copy the wsdl to a local file if it is not
        argList.add(new File(URI.create(wsdl)).getAbsolutePath());

        String[] args = argList.toArray(new String[argList.size()]);
        ClassCollector classCollector = context.get(ClassCollector.class);

        try {
            processArguments(args);
            genModel = runXSD2Java(args, ns2pkgMap); 
            
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
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            printUsage();
            return;
        }

        HelperContext hc = new HelperContextImpl(extendedMetaData, false);
        xsdHelper = hc.getXSDHelper();
        typeHelper = hc.getTypeHelper();
    }

    protected GenModel runXSD2Java(String args[], Map<String, String> ns2PkgMap) {
        String xsdFileName = args[inputIndex];
        EPackage.Registry packageRegistry = new EPackageRegistryImpl(EPackage.Registry.INSTANCE);
        extendedMetaData = new BasicExtendedMetaData(packageRegistry);
        String packageURI = getSchemaNamespace(xsdFileName);
        Map<String, PackageInfo> packageInfoTable =
            createPackageInfoTable(packageURI, schemaNamespace, javaPackage, prefix, ns2PkgMap);
        return generateFromXMLSchema(xsdFileName,
                                     packageRegistry,
                                     extendedMetaData,
                                     targetDirectory,
                                     new Hashtable<String, PackageInfo>(packageInfoTable),
                                     genOptions,
                                     generateBuiltIn,
                                     allNamespaces);
    }

    private static Map<String, PackageInfo> createPackageInfoTable(String packageURI,
                                                                   String schemaNamespace,
                                                                   String javaPackage,
                                                                   String prefix,
                                                                   Map<String, String> ns2PkgMap) {
        Map<String, PackageInfo> packageInfoTable = new HashMap<String, PackageInfo>();

        if (ns2PkgMap != null && !ns2PkgMap.isEmpty()) {
            for (Map.Entry<String, String> e : ns2PkgMap.entrySet()) {
                packageInfoTable.put(e.getKey(), new PackageInfo(e.getValue(), null, e.getKey(), null));
            }
        } else {
            if (schemaNamespace != null) {
                packageInfoTable.put(schemaNamespace,
                                     new PackageInfo(javaPackage, prefix, schemaNamespace, null));
            } else if (packageURI != null) {
                packageInfoTable.put(packageURI, new PackageInfo(javaPackage, prefix, null, null));
            }
        }
        return packageInfoTable;
    }

    public void initialize(ToolContext context) throws ToolException {
        String databinding = (String)context.get(ToolConstants.CFG_DATABINDING);
        if (DATABINDING_DYNAMIC_SDO.equalsIgnoreCase(databinding)) {
            dynamic = true;
        }

        generatedPackages = null;
        String wsdl = (String)context.get(ToolConstants.CFG_WSDLLOCATION);

        if (dynamic) {
            HelperContext helperContext = SDOUtil.createHelperContext();
            xsdHelper = helperContext.getXSDHelper();
            URL location;
            try {
                location = new URL(wsdl);
                InputStream is = location.openStream();
                xsdHelper.define(is, wsdl);
            } catch (IOException e) {
                throw new ToolException(e);
            }
            this.typeHelper = helperContext.getTypeHelper();
        }
        


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
