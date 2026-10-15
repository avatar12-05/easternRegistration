package com.example;

/**
 * 
 * The main application for the Eastern Registration system
 */
public class EasternRegistration {
    
    private EasternRegistration() {
        // Prevent instantiation
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Main entry point of application
     * @param args not used
     * @throws Exception if cannot retreive courses
     */
    public static void main(String[] args) throws Exception {
        MainMenu.run();
    }



}