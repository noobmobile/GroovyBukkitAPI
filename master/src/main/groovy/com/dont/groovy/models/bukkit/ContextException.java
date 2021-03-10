package com.dont.groovy.models.bukkit;


public class ContextException extends Exception {

    private String[] returned;

    public ContextException(String[] returned) {
        super((String) null);
        this.returned = returned;
    }

    public ContextException(String message) {
        super(message);
    }


}
