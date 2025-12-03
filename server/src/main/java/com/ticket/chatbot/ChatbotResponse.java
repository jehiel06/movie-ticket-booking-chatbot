package com.ticket.chatbot;

import java.util.List;

public class ChatbotResponse {
    private List<String> messages;
    private Object richContent; // For structured data if needed

    public ChatbotResponse(List<String> messages) {
        this.messages = messages;
    }

    public ChatbotResponse(List<String> messages, Object richContent) {
        this.messages = messages;
        this.richContent = richContent;
    }

    // Getters and setters
    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public Object getRichContent() {
        return richContent;
    }

    public void setRichContent(Object richContent) {
        this.richContent = richContent;
    }
}