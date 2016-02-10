package demo.jaxrs.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;

public class InputStreamReceiver extends Receiver<String> {

    private static final long serialVersionUID = 1L;
    private List<String> inputStrings = new LinkedList<String>();
    
    public InputStreamReceiver(InputStream is) {
        super(StorageLevel.MEMORY_ONLY());
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String userInput = null;
        while ((userInput = readLine(reader)) != null) {
            inputStrings.add(userInput);
        }
    }
    @Override
    public void onStart() {
        super.store(inputStrings.iterator());
    }

    private String readLine(BufferedReader reader) {
        try {
            return reader.readLine();
        } catch (IOException ex) {
            throw new WebApplicationException(500);
        }
    }
    @Override
    public void onStop() {
        // complete
    }
    
}
