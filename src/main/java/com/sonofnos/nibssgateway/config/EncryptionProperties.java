package com.sonofnos.nibssgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Field-level encryption keys, keyed by version ("v1", "v2", ...).
 * Only one version needs to be live at a time; the current version is
 * app.encryption.active-key-version. Older versions stay listed here so
 * existing ciphertext encrypted under them can still be decrypted - see
 * EncryptionService for the rotation procedure.
 */
@ConfigurationProperties(prefix = "app.encryption")
public class EncryptionProperties {

    private Map<String, String> keys = Map.of();
    private String activeKeyVersion = "v1";

    public Map<String, String> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, String> keys) {
        this.keys = keys;
    }

    public String getActiveKeyVersion() {
        return activeKeyVersion;
    }

    public void setActiveKeyVersion(String activeKeyVersion) {
        this.activeKeyVersion = activeKeyVersion;
    }
}
