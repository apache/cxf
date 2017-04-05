package org.apache.cxf.osgi.itests.jaxrs;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

/**
 * Created by e.rosas.garcia on 05/04/2017.
 */
@PreMatching
public class BookFilterImpl implements BookFilter {

    private UriInfo ui;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        final String query = ui.getAbsolutePath().getQuery();
        System.out.println("BookFilter.query = " + query);
    }

    @Override
    public UriInfo getUi() {
        return ui;
    }

    @Override
    @Context
    public void setUi(UriInfo ui) {
        this.ui = ui;
    }

}
