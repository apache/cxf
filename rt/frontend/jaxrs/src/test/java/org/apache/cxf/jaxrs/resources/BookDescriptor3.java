package org.apache.cxf.jaxrs.resources;

import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author nkey
 * @since 05.06.2014
 */
@XmlRootElement(name = "BookDescriptor")
public class BookDescriptor3 {
    private Integer year;
    private String isbn;

    public BookDescriptor3(Integer year, String isbn) {
        this.isbn = isbn;
        this.year = year;
    }

    public BookDescriptor3() {
    }

    @PathParam("year")
    public Integer getYear() {
        return year;
    }

    @PathParam("isbn")
    public String getIsbn() {
        return isbn;
    }


    public void setYear(@PathParam("year") Integer year) {
        this.year = year;
    }

    public void setIsbn(@PathParam("isbn") String isbn) {
        this.isbn = isbn;
    }
}
