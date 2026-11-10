package com.traveler.traveler_api.exception;

public class ConflictException extends RuntimeException {
    private final Integer currentVersion;

    public ConflictException(String message, Integer currentVersion){
        super(message);
        this.currentVersion = currentVersion;
    }

    public Integer getCurrentVersion(){
        return currentVersion;
    }
}