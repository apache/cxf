package org.apache.cxf.osgi.itests.jaxrs;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * Created by e.rosas.garcia on 05/04/2017.
 */
public interface BookFilter extends ContainerRequestFilter {
    UriInfo getUi();

    @Context
    void setUi(UriInfo ui);
}
