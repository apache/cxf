package org.apache.cxf.jaxrs.resources;

import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author nkey
 * @since 05.06.2014
 */
@XmlRootElement(name = "BookDescriptor")
public class BookDescriptor {
    private Integer year;
    private String isbn;

    public BookDescriptor(Integer year, String isbn) {
        this.isbn = isbn;
        this.year = year;
    }

    public BookDescriptor() {
    }


    @PathParam("isbn")
    public Integer getYear() {
        return year;
    }

    @PathParam("year")
    public String getIsbn() {
        return isbn;
    }

    public void setYear( Integer year) {
        this.year = year;
    }

    public void setIsbn( String isbn) {
        this.isbn = isbn;
    }
}
