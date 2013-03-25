package org.apache.cxf.jaxrs.impl;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import javax.ws.rs.core.Link;

import org.junit.Ignore;
import org.junit.Test;

public class TestLinkBuilderImpl {
    Link.Builder linkBuilder = new LinkBuilderImpl();

    @Test
    public void build() throws Exception {
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").build();
        assertEquals("<http://example.com/page1>;rel=\"previous\"", prevLink.toString());
    }

    @Test
    public void relativeBuild() throws Exception {
        URI base = URI.create("http://example.com/page2");
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").buildRelativized(base);
        assertEquals("<page1>;rel=\"previous\"", prevLink.toString());
    }

    @Test
    public void severalAttributes() throws Exception {
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").title("A title").build();
        assertEquals("<http://example.com/page1>;rel=\"previous\";title=\"A title\"", prevLink.toString());
    }

    @Test
    public void copyOnBuild() throws Exception {
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").build();
        Link nextLink = linkBuilder.uri("http://example.com/page3").rel("next").build();

        // Previously built link should not be affected by reuse of link builder
        assertEquals("<http://example.com/page1>;rel=\"previous\"", prevLink.toString());
        assertEquals("<http://example.com/page3>;rel=\"next\"", nextLink.toString());
    }

    @Ignore("Ignored due to CXF-4919")
    @Test
    public void copyOnRelativeBuild() throws Exception {
        URI base = URI.create("http://example.com/page2");
        Link prevLink = linkBuilder.uri("http://example.com/page1").rel("previous").buildRelativized(base);
        Link nextLink = linkBuilder.uri("http://example.com/page3").rel("next").buildRelativized(base);

        // Previously built link should not be affected by reuse of link builder
        assertEquals("<page1>;rel=\"previous\"", prevLink.toString());
        assertEquals("<page3>;rel=\"next\"", nextLink.toString());
    }
}
