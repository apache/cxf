package org.apache.cxf.jaxb;

import org.apache.cxf.Bus;
import org.apache.cxf.databinding.WrapperHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class WrapperHelperClassLoader implements WrapperHelperCreator {
    ClassLoader cl;
    public WrapperHelperClassLoader(ClassLoader cl) {
        this.cl = cl;
    }

    @Override
    public WrapperHelper compile(Bus bus, Class<?> wrapperType, Method[] setMethods, Method[] getMethods, Method[] jaxbMethods, Field[] fields, Object objectFactory) {

        int count = 1;
        String newClassName = wrapperType.getName() + "_WrapperTypeHelper" + count;

        Class<?> cls = null;
        try {
            cls = cl.loadClass(newClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        while (cls != null) {
            try {
                WrapperHelper helper = WrapperHelper.class.cast(cls.newInstance());
                if (!helper.getSignature().equals(WrapperHelperCompiler.computeSignature(setMethods, getMethods))) {
                    count++;
                    newClassName = wrapperType.getName() + "_WrapperTypeHelper" + count;
                    cls = cl.loadClass(newClassName);
                } else {
                    return helper;
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
