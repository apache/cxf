package org.apache.cxf.jaxws.spi;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.model.InterfaceInfo;

import java.util.Set;

public interface WrapperClassCreator {
    Set<Class<?>> generate(Bus bus, JaxWsServiceFactoryBean fact, InterfaceInfo inf, boolean q);
}
