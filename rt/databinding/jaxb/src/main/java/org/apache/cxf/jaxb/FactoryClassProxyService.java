package org.apache.cxf.jaxb;

import org.apache.cxf.Bus;

public class FactoryClassProxyService implements FactoryClassCreator {
    FactoryClassCreator srv;
    public FactoryClassProxyService(Bus bus) {
        this(new FactoryClassGenerator(bus));
    }
    public FactoryClassProxyService(FactoryClassCreator srv) {
        super();
        this.srv = srv;
    }

    @Override
    public Class<?> createFactory(Class<?> cls) {
        return srv.createFactory(cls);
    }

    public class LoadFirst extends FactoryClassProxyService {
        public LoadFirst(ClassLoader cl) {
            //TODO not sure here if I get class loader like that ???
            // or I need to inject another class loader from outside
            super(new FactoryClassLoader(cl));
        }
    }
    public class GenerateJustInTime extends FactoryClassProxyService {
        public GenerateJustInTime(Bus bus) {
            super(new FactoryClassGenerator(bus));
        }
    }
}
