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
public class ClientBundleActivator implements BundleActivator {

    private Server server;

    @Override
    public void start(BundleContext ctx) throws Exception {
        final BookFilter bookFilter = ctx.getService(ctx.getServiceReference(BookFilter.class));

        Bus bus = BusFactory.newInstance().createBus();
        bus.setExtension(FilterBundleActivator.class.getClassLoader(), ClassLoader.class);
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setBus(bus);
        sf.setResourceClasses(BookStore.class);
        sf.setProvider(bookFilter);
        sf.setAddress("/secondBundle");
        server = sf.create();
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        server.stop();
        server.destroy();
    }

}
