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

package org.apache.cxf.xjc.dv;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

import org.xml.sax.ErrorHandler;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.util.NamespaceContextAdapter;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.XmlString;

import org.apache.cxf.common.logging.LogUtils;

/**
 * Modifies the JAXB code model to initialize fields mapped from schema elements 
 * with their default value.
 */
public class DefaultValuePlugin {
    
    private static final Logger LOG = LogUtils.getL7dLogger(DefaultValuePlugin.class);
    
    public DefaultValuePlugin() {
    }

    public String getOptionName() {
        return "Xdv";
    }

    public String getUsage() {
        return "  -Xdv                 : Initialize fields mapped from elements with their default values";
    }

    private boolean containsDefaultValue(Outline outline, FieldOutline field) {
        ClassOutline fClass = null;
        for (ClassOutline classOutline : outline.getClasses()) {
            if (classOutline.implClass == field.getRawType()) {
                fClass = classOutline;
                break;
            }
        }
        if (fClass == null) {
            return false;
        }
        for (FieldOutline f : fClass.getDeclaredFields()) {
            if (f.getPropertyInfo().getSchemaComponent() instanceof XSParticle) {
                XSParticle particle = (XSParticle)f.getPropertyInfo().getSchemaComponent();
                XSTerm term = particle.getTerm();
                if (term.isElementDecl() && term.asElementDecl().getDefaultValue() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) {
        LOG.fine("Running default value plugin.");
        for (ClassOutline co : outline.getClasses()) {
            for (FieldOutline f : co.getDeclaredFields()) {

                // Use XML schema object model to determine if field is mapped
                // from an element (attributes default values are handled
                // natively) and get its default value.
                XmlString xmlDefaultValue = null;
                XSType xsType = null;
                boolean isElement = false;
                boolean isRequiredAttr = true;
                if (f.getPropertyInfo().getSchemaComponent() instanceof XSParticle) {
                    XSParticle particle = (XSParticle)f.getPropertyInfo().getSchemaComponent();
                    XSTerm term = particle.getTerm();
                    XSElementDecl element = null;

                    if (term.isElementDecl()) {
                        element = particle.getTerm().asElementDecl();
                        xmlDefaultValue = element.getDefaultValue();                        
                        xsType = element.getType();
                        isElement = true;
                    }
                } else if (f.getPropertyInfo().getSchemaComponent() instanceof XSAttributeUse) {
                    XSAttributeUse attributeUse = (XSAttributeUse)f.getPropertyInfo().getSchemaComponent();
                    XSAttributeDecl decl = attributeUse.getDecl();
                    xmlDefaultValue = decl.getDefaultValue();                        
                    xsType = decl.getType();
                    isRequiredAttr = attributeUse.isRequired();
                }

                
                if (xsType != null && xsType.isComplexType() && containsDefaultValue(outline, f)) {
                    String varName = f.getPropertyInfo().getName(false);
                    JFieldVar var = co.implClass.fields().get(varName);
                    if (var != null) {
                        co.implClass.removeField(var);

                        JFieldVar newVar = co.implClass.field(var.mods().getValue(), 
                                                              var.type(), 
                                                              var.name(), 
                                                              JExpr._new(f.getRawType()));
                        newVar.javadoc().append(var.javadoc());
                    }
                }

                JExpression dvExpr = null;
                if (null != xmlDefaultValue && null != xmlDefaultValue.value) {
                    dvExpr = getDefaultValueExpression(f, co, outline, xsType, isElement,
                                                       xmlDefaultValue, false);
                }
                 
                if (null == dvExpr
                    && !isElement && !isRequiredAttr
                    && xsType != null && xsType.getOwnerSchema() != null
                    && !"http://www.w3.org/2001/XMLSchema"
                        .equals(xsType.getOwnerSchema().getTargetNamespace())) {
                    //non-primitive attribute, may still be able to convert it, but need to do
                    //a bunch more checks and changes to setters and isSet and such
                    dvExpr = 
                        getDefaultValueExpression(f, co, outline, xsType, isElement, xmlDefaultValue, true);
                    
                    updateSetter(co, f, co.implClass);
                    updateGetter(co, f, co.implClass, dvExpr, true);                    
                    
                } else if (null == dvExpr) {
                    continue;
                } else {
                    updateGetter(co, f, co.implClass, dvExpr, false);                    
                }
            }
        }

        return true;
    }
    
    
    JExpression getDefaultValueExpression(FieldOutline f,
                                          ClassOutline co,
                                          Outline outline,
                                          XSType xsType,
                                          boolean isElement,
                                          XmlString xmlDefaultValue,
                                          boolean unbox) {
        JType type = f.getRawType();
        String typeName = type.fullName();
        String defaultValue = xmlDefaultValue == null ? null : xmlDefaultValue.value;
        if (defaultValue == null) {
            return null;
        }

        JExpression dv = null;
        
        if ("java.lang.Boolean".equals(typeName) && isElement) {
            dv = JExpr.direct(Boolean.valueOf(defaultValue) ? "Boolean.TRUE" : "Boolean.FALSE");
        } else if ("java.lang.Byte".equals(typeName) && isElement) {
            dv = JExpr._new(type)
                .arg(JExpr.cast(type.unboxify(), 
                    JExpr.lit(new Byte(Short.valueOf(defaultValue).byteValue()))));
        } else if ("java.lang.Double".equals(typeName) && isElement) {
            dv = JExpr._new(type)
                .arg(JExpr.lit(new Double(Double.valueOf(defaultValue).doubleValue())));
        } else if ("java.lang.Float".equals(typeName) && isElement) {
            dv = JExpr._new(type)
                     .arg(JExpr.lit(new Float(Float.valueOf(defaultValue).floatValue())));
        } else if ("java.lang.Integer".equals(typeName) && isElement) {
            dv = JExpr._new(type)
                .arg(JExpr.lit(new Integer(Integer.valueOf(defaultValue).intValue())));
        } else if ("java.lang.Long".equals(typeName) && isElement) {
            dv = JExpr._new(type)
                .arg(JExpr.lit(new Long(Long.valueOf(defaultValue).longValue())));
        } else if ("java.lang.Short".equals(typeName) && isElement) {
            dv = JExpr._new(type)
                .arg(JExpr.cast(type.unboxify(), 
                    JExpr.lit(new Short(Short.valueOf(defaultValue).shortValue()))));
        } else if ("java.lang.String".equals(type.fullName()) && isElement) {
            dv = JExpr.lit(defaultValue);
        } else if ("java.math.BigInteger".equals(type.fullName()) && isElement) {
            dv = JExpr._new(type).arg(JExpr.lit(defaultValue));
        } else if ("java.math.BigDecimal".equals(type.fullName()) && isElement) {
            dv = JExpr._new(type).arg(JExpr.lit(defaultValue));
        } else if ("byte[]".equals(type.fullName()) && xsType.isSimpleType() && isElement) {
            while (!"anySimpleType".equals(xsType.getBaseType().getName())) {
                xsType = xsType.getBaseType();
            }
            if ("base64Binary".equals(xsType.getName())) {
                dv = outline.getCodeModel().ref(DatatypeConverter.class)
                   .staticInvoke("parseBase64Binary").arg(defaultValue);
            } else if ("hexBinary".equals(xsType.getName())) {
                dv = JExpr._new(outline.getCodeModel().ref(HexBinaryAdapter.class))
                    .invoke("unmarshal").arg(defaultValue);
            }
        } else if ("javax.xml.namespace.QName".equals(typeName)) {
            NamespaceContext nsc = new NamespaceContextAdapter(xmlDefaultValue);
            QName qn = DatatypeConverter.parseQName(xmlDefaultValue.value, nsc);
            dv = JExpr._new(outline.getCodeModel().ref(QName.class))
                .arg(qn.getNamespaceURI())
                .arg(qn.getLocalPart())
                .arg(qn.getPrefix());
        } else if ("javax.xml.datatype.Duration".equals(typeName)) {
            dv = outline.getCodeModel().ref(org.apache.cxf.jaxb.DatatypeFactory.class)
                .staticInvoke("createDuration").arg(defaultValue);
        } else if (type instanceof JDefinedClass) {
            JDefinedClass cls = (JDefinedClass)type;
            if (cls.getClassType() == ClassType.ENUM) {
                dv = cls.staticInvoke("fromValue").arg(defaultValue);
            }
        } else if (unbox) {
            typeName = type.unboxify().fullName();
            if ("int".equals(typeName)) {
                dv = JExpr.lit(Integer.valueOf(defaultValue).intValue());
            } else if ("long".equals(typeName)) {
                dv = JExpr.lit(Long.valueOf(defaultValue).longValue());
            } else if ("short".equals(typeName)) {
                dv = JExpr.lit(Short.valueOf(defaultValue).shortValue());
            } else if ("boolean".equals(typeName)) {
                dv = JExpr.lit(Boolean.valueOf(defaultValue).booleanValue());
            } else if ("double".equals(typeName)) {
                dv = JExpr.lit(Double.valueOf(defaultValue).doubleValue());
            } else if ("float".equals(typeName)) {
                dv = JExpr.lit(Float.valueOf(defaultValue).floatValue());
            } else if ("byte".equals(typeName)) {
                dv = JExpr.lit(Byte.valueOf(defaultValue).byteValue());
            } else {
                dv = getDefaultValueExpression(f,
                                               co,
                                               outline,
                                               xsType,
                                               true,
                                               xmlDefaultValue,
                                               false);
            }
        }
        // TODO: GregorianCalendar, ...
        return dv;
    }
    
    private void updateGetter(ClassOutline co, FieldOutline fo, 
                              JDefinedClass dc, JExpression dvExpr,
                              boolean remapRet) {

        String fieldName = fo.getPropertyInfo().getName(false);
        JType type = fo.getRawType();
        String typeName = type.fullName();

        String getterName = ("java.lang.Boolean".equals(typeName) ? "is" : "get")
                            + fo.getPropertyInfo().getName(true);

        JMethod method = dc.getMethod(getterName, new JType[0]);
        JDocComment doc = method.javadoc();
        int mods = method.mods().getValue();
        JType mtype = method.type();
        if (remapRet) {
            mtype = mtype.unboxify();
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Updating getter: " + getterName);
        }
        // remove existing method and define new one

        dc.methods().remove(method);

        method = dc.method(mods, mtype, getterName);
        method.javadoc().append(doc);

        JFieldRef fr = JExpr.ref(fieldName);
        if (dvExpr != null) {
            JExpression test = JOp.eq(JExpr._null(), fr);
            JConditional jc =  method.body()._if(test);
            jc._then()._return(dvExpr);
            jc._else()._return(fr);
        } else {
            method.body()._return(fr);
        }
    }
    private void updateSetter(ClassOutline co, FieldOutline fo, 
                              JDefinedClass dc) {

        String fieldName = fo.getPropertyInfo().getName(false);
        JType type = fo.getRawType();
        String typeName = type.fullName();

        String getterName = ("java.lang.Boolean".equals(typeName) ? "is" : "get")
                            + fo.getPropertyInfo().getName(true);
        JMethod method = dc.getMethod(getterName, new JType[0]);
        JType mtype = method.type();
        String setterName = "set" + fo.getPropertyInfo().getName(true);
        method = dc.getMethod(setterName, new JType[] {mtype});
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Updating setter: " + setterName);
        }
        JDocComment doc = method.javadoc();
        // remove existing method and define new one
        dc.methods().remove(method);

        int mods = method.mods().getValue();
        mtype = mtype.unboxify();
        method = dc.method(mods, method.type(), setterName);
        
        method.javadoc().append(doc);
        method.param(mtype, "value");

        JFieldRef fr = JExpr.ref(fieldName);
        method.body().assign(fr, JExpr.ref("value"));
        
        method = dc.method(mods, method.type(), "unset" + fo.getPropertyInfo().getName(true));
        method.body().assign(fr, JExpr._null());
        
        method = dc.getMethod("isSet" + fo.getPropertyInfo().getName(true), new JType[0]);
        if (method != null) {
            //move to end
            dc.methods().remove(method);
            dc.methods().add(method);
        }
        
    }

}
