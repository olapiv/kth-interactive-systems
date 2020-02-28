package chat.server;

import java.io.Serializable;

public class Message implements Serializable {

    private long id;
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return text;
    }
}
