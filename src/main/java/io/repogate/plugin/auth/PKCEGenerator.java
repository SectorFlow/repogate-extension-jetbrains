package io.repogate.plugin.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PKCE (Proof Key for Code Exchange) generator for OAuth2 security
 */
public class PKCEGenerator {
    
    public static class PKCEChallenge {
        public final String codeVerifier;
        public final String codeChallenge;
        
        public PKCEChallenge(String codeVerifier, String codeChallenge) {
            this.codeVerifier = codeVerifier;
            this.codeChallenge = codeChallenge;
        }
    }
    
    /**
     * Generate PKCE code verifier and challenge
     */
    public static PKCEChallenge generate() {
        // Generate random code verifier (128 characters)
        String codeVerifier = generateCodeVerifier();
        
        // Generate SHA256 hash of verifier
        String codeChallenge = generateCodeChallenge(codeVerifier);
        
        return new PKCEChallenge(codeVerifier, codeChallenge);
    }
    
    /**
     * Generate random code verifier
     */
    private static String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[96]; // 96 bytes = 128 base64 characters
        secureRandom.nextBytes(randomBytes);
        return base64URLEncode(randomBytes);
    }
    
    /**
     * Generate SHA256 hash of code verifier
     */
    private static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            return base64URLEncode(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Base64 URL encode (without padding)
     */
    private static String base64URLEncode(byte[] bytes) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
