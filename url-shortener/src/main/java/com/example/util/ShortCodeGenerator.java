package com.example.util;

import java.security.SecureRandom;

//generate a random 6 character base62 shortCode
public class ShortCodeGenerator{
    private static final String CHARACTERS = "abcdefhijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();
    
    public static String generateShortCode(){
        StringBuilder shortCode = new StringBuilder();

        for(int i = 0; i<SHORT_CODE_LENGTH; i++){
            int index = random.nextInt(CHARACTERS.length());
            shortCode.append(CHARACTERS.charAt(index));
        }

        return shortCode.toString();
    }
}