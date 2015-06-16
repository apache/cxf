package demo.jaxrs.server;

public class ShakespeareText {
    private String text;
    private String date;
    public ShakespeareText() {
        
    }
    public ShakespeareText(String text, String date) {
        this.text = text;
        this.date = date;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
}
