package org.apache.cxf.jaxws.spi;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.WrapperClassGenerator;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.model.InterfaceInfo;

import java.util.Set;

public class WrapperClassCreatorProxyService implements WrapperClassCreator {
    WrapperClassCreator srv;
    public WrapperClassCreatorProxyService() {
        this(new WrapperClassGenerator());
    }
    public WrapperClassCreatorProxyService(WrapperClassCreator srv) {
        super();
        this.srv = srv;
    }

    @Override
    public Set<Class<?>> generate(Bus bus, JaxWsServiceFactoryBean fact, InterfaceInfo inf, boolean q) {
        return srv.generate(bus, fact, inf, q);
    }

    public class LoadFirst extends WrapperClassCreatorProxyService {
        public LoadFirst(ClassLoader cl) {
            //TODO not sure here if I get class loader like that ???
            // or I need to inject another class loader from outside
            super(new GeneratedWrapperClassLoader(cl));
        }
    }
    public class GenerateJustInTime extends WrapperClassCreatorProxyService {
        public GenerateJustInTime() {
            super(new WrapperClassGenerator());
        }
    }
}
