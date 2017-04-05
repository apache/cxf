package org.apache.cxf.osgi.itests.jaxrs;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Created by e.rosas.garcia on 05/04/2017.
 */
public class FilterBundleActivator implements BundleActivator {

    private Server server;

    @Override
    public void start(BundleContext ctx) throws Exception {
        final BookFilter provider = new BookFilterImpl();

        Bus bus = BusFactory.newInstance().createBus();
        bus.setExtension(FilterBundleActivator.class.getClassLoader(), ClassLoader.class);
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setBus(bus);
        sf.setResourceClasses(BookStore.class);
        sf.setProvider(provider);
        sf.setAddress("/firstBundle");
        server = sf.create();

        ctx.registerService(BookFilter.class.getName(), provider, null);
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        server.stop();
        server.destroy();
    }

}
