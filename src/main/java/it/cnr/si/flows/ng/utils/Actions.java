package it.cnr.si.flows.ng.utils;

public enum Actions {

    revoca("Revoca"),
    revocaSemplice("RevocaSemplice");

    private String value;

    private Actions(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}