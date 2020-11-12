package org.apache.cxf.jaxb;

import org.apache.cxf.Bus;
import org.apache.cxf.databinding.WrapperHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface WrapperHelperCreator {
    WrapperHelper compile(Bus bus, Class<?> wrapperType, Method[] setMethods,
                          Method[] getMethods, Method[] jaxbMethods,
                          Field[] fields, Object objectFactory);
}
