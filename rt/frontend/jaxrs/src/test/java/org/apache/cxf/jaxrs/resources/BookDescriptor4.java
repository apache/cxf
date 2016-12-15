package org.apache.cxf.jaxrs.resources;

import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author nkey
 * @since 05.06.2014
 */
@XmlRootElement(name = "BookDescriptor")
public class BookDescriptor4 {
    private Integer year;
    private String isbn;

    public BookDescriptor4(Integer year, String isbn) {
        this.isbn = isbn;
        this.year = year;
    }

    public BookDescriptor4() {
    }

    public Integer getYear() {
        return year;
    }

    public String getIsbn() {
        return isbn;
    }


    @PathParam("year")
    public void setYear(@PathParam("isbn") Integer year) {
        this.year = year;
    }

    @PathParam("isbn")
    public void setIsbn(@PathParam("isbn") String isbn) {
        this.isbn = isbn;
    }
}
