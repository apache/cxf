package org.apache.cxf.jaxb;

import org.apache.cxf.Bus;
import org.apache.cxf.databinding.WrapperHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class WrapperHelperProxyService implements WrapperHelperCreator {
    WrapperHelperCreator srv;
    public WrapperHelperProxyService(Bus bus) {
        this(new WrapperHelperCompiler(bus));
    }
    public WrapperHelperProxyService(WrapperHelperCreator srv) {
        super();
        this.srv = srv;
    }

    @Override
    public WrapperHelper compile(Bus bus, Class<?> wrapperType, Method[] setMethods, Method[] getMethods,
                                 Method[] jaxbMethods, Field[] fields, Object objectFactory) {
        return srv.compile(bus, wrapperType, setMethods, getMethods, jaxbMethods, fields, objectFactory);
    }

    public class LoadFirst extends WrapperHelperProxyService {
        public LoadFirst(ClassLoader cl) {
            //TODO not sure here if I get class loader like that ???
            // or I need to inject another class loader from outside
            super(new WrapperHelperClassLoader(cl));
        }
    }
    public class GenerateJustInTime extends WrapperHelperProxyService {
        public GenerateJustInTime(Bus bus) {
            super(new WrapperHelperCompiler(bus));
        }
    }
}
