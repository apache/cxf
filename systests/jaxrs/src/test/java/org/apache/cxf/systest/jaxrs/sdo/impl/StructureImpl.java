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
package org.apache.cxf.systest.jaxrs.sdo.impl;

import java.util.Collection;
import java.util.List;

import org.apache.cxf.systest.jaxrs.sdo.SdoFactory;
import org.apache.cxf.systest.jaxrs.sdo.Structure;
import org.apache.tuscany.sdo.impl.DataObjectBase;

import commonj.sdo.Type;

//CHECKSTYLE:OFF
public class StructureImpl extends DataObjectBase implements Structure {

    public final static int TEXT = 0;
    
    public final static int INT = 1;
    
    public final static int DBL = 2;
    
    public final static int TEXTS = 3;
    
    public final static int SDO_PROPERTY_COUNT = 4;
    
    public final static int EXTENDED_PROPERTY_COUNT = 0;


    /**
     * The internal feature id for the '<em><b>Text</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     * @ordered
     */ 
    public final static int _INTERNAL_TEXT = 0;
    
    /**
     * The internal feature id for the '<em><b>Int</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     * @ordered
     */ 
    public final static int _INTERNAL_INT = 1;
    
    /**
     * The internal feature id for the '<em><b>Dbl</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     * @ordered
     */ 
    public final static int _INTERNAL_DBL = 2;
    
    /**
     * The internal feature id for the '<em><b>Texts</b></em>' attribute list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     * @ordered
     */ 
    public final static int _INTERNAL_TEXTS = 3;
    
    /**
     * The number of properties for this type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     * @ordered
     */
    public final static int INTERNAL_PROPERTY_COUNT = 4;
    
    /**
     * The default value of the '{@link #getText() <em>Text</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getText()
     * @generated
     * @ordered
     */
    protected static final String TEXT_DEFAULT_ = null;
    
    /**
     * The cached value of the '{@link #getText() <em>Text</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getText()
     * @generated
     * @ordered
     */
    protected String text = TEXT_DEFAULT_;
    
    /**
     * This is true if the Text attribute has been set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     * @ordered
     */
    protected boolean text_set_ = false;
    
    /**
     * The default value of the '{@link #getInt() <em>Int</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getInt()
     * @generated
     * @ordered
     */
    protected static final int INT_DEFAULT_ = 0;
    
    /**
     * The cached value of the '{@link #getInt() <em>Int</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getInt()
     * @generated
     * @ordered
     */
    protected int int_ = INT_DEFAULT_;
    
    /**
     * This is true if the Int attribute has been set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     * @ordered
     */
    protected boolean int_set_ = false;
    
    /**
     * The default value of the '{@link #getDbl() <em>Dbl</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getDbl()
     * @generated
     * @ordered
     */
    protected static final double DBL_DEFAULT_ = 0.0;
    
    /**
     * The cached value of the '{@link #getDbl() <em>Dbl</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getDbl()
     * @generated
     * @ordered
     */
    protected double dbl = DBL_DEFAULT_;
    
    /**
     * This is true if the Dbl attribute has been set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     * @ordered
     */
    protected boolean dbl_set_ = false;
    
    /**
     * The cached value of the '{@link #getTexts() <em>Texts</em>}' attribute list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see #getTexts()
     * @generated
     * @ordered
     */
    
    protected List texts = null;
    
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public StructureImpl() {
        super();
    }
    
    
    protected int internalConvertIndex(int internalIndex) {
        switch (internalIndex) {
            case _INTERNAL_TEXT: 
                return TEXT;
            case _INTERNAL_INT: 
                return INT;
            case _INTERNAL_DBL: 
                return DBL;
            case _INTERNAL_TEXTS: 
                return TEXTS;
        }
        return super.internalConvertIndex(internalIndex);
    }
    
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public Type getStaticType() {
        return ((SdoFactoryImpl)SdoFactory.INSTANCE).getStructure();
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public int getStaticPropertyCount() {
        return INTERNAL_PROPERTY_COUNT;
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public String getText() {
        return text;
    }
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setText(String newText) {
        String oldText = text;
        text = newText;
        boolean oldText_set_ = text_set_;
        text_set_ = true;
        if (isNotifying()) {
            notify(ChangeKind.SET, _INTERNAL_TEXT, oldText, text, !oldText_set_);
        }
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void unsetText() {
        String oldText = text;
        boolean oldText_set_ = text_set_;
        text = TEXT_DEFAULT_;
        text_set_ = false;
        if (isNotifying()) {
            notify(ChangeKind.UNSET, _INTERNAL_TEXT, oldText, TEXT_DEFAULT_, oldText_set_);
        }
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public boolean isSetText() {
        return text_set_;
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public int getInt() {
        return int_;
    }
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setInt(int newInt) {
        int oldInt = int_;
        int_ = newInt;
        boolean oldInt_set_ = int_set_;
        int_set_ = true;
        if (isNotifying()) {
            notify(ChangeKind.SET, _INTERNAL_INT, oldInt, int_, !oldInt_set_);
        }
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void unsetInt() {
        int oldInt = int_;
        boolean oldInt_set_ = int_set_;
        int_ = INT_DEFAULT_;
        int_set_ = false;
        if (isNotifying()) {
            notify(ChangeKind.UNSET, _INTERNAL_INT, oldInt, INT_DEFAULT_, oldInt_set_);
        }
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public boolean isSetInt() {
        return int_set_;
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public double getDbl() {
        return dbl;
    }
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void setDbl(double newDbl) {
        double oldDbl = dbl;
        dbl = newDbl;
        boolean oldDbl_set_ = dbl_set_;
        dbl_set_ = true;
        if (isNotifying()) {
            notify(ChangeKind.SET, _INTERNAL_DBL, oldDbl, dbl, !oldDbl_set_);
        }
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void unsetDbl() {
        double oldDbl = dbl;
        boolean oldDbl_set_ = dbl_set_;
        dbl = DBL_DEFAULT_;
        dbl_set_ = false;
        if (isNotifying()) {
            notify(ChangeKind.UNSET, _INTERNAL_DBL, oldDbl, DBL_DEFAULT_, oldDbl_set_);
        }
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public boolean isSetDbl() {
        return dbl_set_;
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public List getTexts() {
        if (texts == null) {
          texts = createPropertyList(ListKind.DATATYPE, String.class, TEXTS, 0);
        }
        return texts;
    }
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public Object get(int propertyIndex, boolean resolve) {
        switch (propertyIndex) {
            case TEXT :
                return getText();
            case INT :
                return new Integer(getInt());
            case DBL :
                return new Double(getDbl());
            case TEXTS :
                return getTexts();
        }
        return super.get(propertyIndex, resolve);
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    @SuppressWarnings("unchecked")
    public void set(int propertyIndex, Object newValue) {
        switch (propertyIndex) {
            case TEXT :
                setText((String)newValue);
                return;
            case INT :
                setInt(((Integer)newValue).intValue());
                return;
            case DBL :
                setDbl(((Double)newValue).doubleValue());
                return;
            case TEXTS :
                getTexts().clear();
                getTexts().addAll((Collection)newValue);
            return;
        }
        super.set(propertyIndex, newValue);
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public void unset(int propertyIndex) {
        switch (propertyIndex) {
            case TEXT :
                unsetText();
                return;
            case INT :
                unsetInt();
                return;
            case DBL :
                unsetDbl();
                return;
            case TEXTS :
                getTexts().clear();
                return;
        }
        super.unset(propertyIndex);
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public boolean isSet(int propertyIndex) {
        switch (propertyIndex) {
            case TEXT :
                return isSetText();
            case INT :
                return isSetInt();
            case DBL :
                return isSetDbl();
            case TEXTS :
                return texts != null && !texts.isEmpty();
        }
        return super.isSet(propertyIndex);
    }
    
    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    public String toString() {
        if (isProxy(this)) {
            return super.toString();
        }
    
        StringBuffer result = new StringBuffer(super.toString());
        result.append(" (text: ");
        if (text_set_) result.append(text); else result.append("<unset>");
        result.append(", int: ");
        if (int_set_) result.append(int_); else result.append("<unset>");
        result.append(", dbl: ");
        if (dbl_set_) result.append(dbl); else result.append("<unset>");
        result.append(", texts: ");
        result.append(texts);
        result.append(')');
        return result.toString();
    }

}
//CHECKSTYLE:ON