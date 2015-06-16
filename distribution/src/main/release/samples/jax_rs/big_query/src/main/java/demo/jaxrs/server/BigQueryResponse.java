package demo.jaxrs.server;

import java.util.LinkedList;
import java.util.List;

public class BigQueryResponse {
    private String userName;
    private String searchWord;
    private List<ShakespeareText> texts = new LinkedList<ShakespeareText>();
    
    public BigQueryResponse() {
        
    }
    public BigQueryResponse(String userName, String searchWord) {
        this.userName = userName;
        this.searchWord = searchWord;
    }
    
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getSearchWord() {
        return searchWord;
    }
    public void setSearchWord(String searchWord) {
        this.searchWord = searchWord;
    }
    public List<ShakespeareText> getTexts() {
        return texts;
    }
    public void setTexts(List<ShakespeareText> texts) {
        this.texts = texts;
    }
}
