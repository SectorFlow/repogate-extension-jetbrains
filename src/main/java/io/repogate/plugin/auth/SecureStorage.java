package io.repogate.plugin.auth;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import org.jetbrains.annotations.Nullable;

/**
 * Secure storage wrapper using IntelliJ PasswordSafe
 */
public class SecureStorage {
    private static final String SERVICE_NAME = "RepoGate";
    
    // Storage keys
    public static final String API_TOKEN_KEY = "io.repogate.plugin.apiToken";
    public static final String ACCESS_TOKEN_KEY = "io.repogate.plugin.accessToken";
    public static final String REFRESH_TOKEN_KEY = "io.repogate.plugin.refreshToken";
    
    /**
     * Store a secret value
     */
    public static void store(String key, String value) {
        CredentialAttributes attributes = createCredentialAttributes(key);
        Credentials credentials = new Credentials(key, value);
        PasswordSafe.getInstance().set(attributes, credentials);
    }
    
    /**
     * Retrieve a secret value
     */
    @Nullable
    public static String get(String key) {
        CredentialAttributes attributes = createCredentialAttributes(key);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        return credentials != null ? credentials.getPasswordAsString() : null;
    }
    
    /**
     * Delete a secret value
     */
    public static void delete(String key) {
        CredentialAttributes attributes = createCredentialAttributes(key);
        PasswordSafe.getInstance().set(attributes, null);
    }
    
    /**
     * Create credential attributes for PasswordSafe
     */
    private static CredentialAttributes createCredentialAttributes(String key) {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName(SERVICE_NAME, key)
        );
    }
}
