package me.jadenp.notcensor;

import javax.annotation.Nullable;

public class MessageRecord {
    private final long time;
    private final String message;
    private String censoredMessage = null;

    public MessageRecord(String message) {
        this.message = message;
        this.time = System.currentTimeMillis();
    }

    public void setCensoredMessage(String censoredMessage) {
        this.censoredMessage = censoredMessage;
    }

    public long getTime() {
        return time;
    }

    @Nullable
    public String getCensoredMessage() {
        return censoredMessage;
    }

    public String getMessage() {
        return message;
    }
}
