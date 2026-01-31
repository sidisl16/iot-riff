package com.iot.riff.vault;

import io.github.jopenlibs.vault.Vault;
import io.github.jopenlibs.vault.VaultConfig;
import io.github.jopenlibs.vault.VaultException;
import io.github.jopenlibs.vault.rest.Rest;
import io.github.jopenlibs.vault.rest.RestResponse;
import io.micronaut.context.annotation.Property;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import io.micronaut.core.type.Argument;
import io.micronaut.serde.ObjectMapper;
import com.iot.riff.service.exception.IotException;

@Slf4j
@Singleton
public class IotVaultService {

    private Vault vault;
    private String vaultToken;
    private String vaultUrl;
    private ObjectMapper objectMapper;

    private static final String VAULT_GENERATE_PASSWORD_PATH = "/v1/sys/policies/password/iot-policy/generate";
    private static final String HEADER_VAULT_TOKEN = "X-Vault-Token";
    private static final String SYS_ENV_VAULT_TOKEN = "VAULT_TOKEN";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_PASSWORD = "password";
    private static final String SECRET_DATA_PREFIX = "secret/";

    public IotVaultService(@Property(name = "vault.url") String vaultUrl, ObjectMapper objectMapper) {
        this.vaultUrl = vaultUrl;
        this.vaultToken = System.getenv(SYS_ENV_VAULT_TOKEN);
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() throws VaultException {
        VaultConfig config = new VaultConfig()
                .engineVersion(2)
                .address(vaultUrl)
                .token(vaultToken)
                .build();

        this.vault = Vault.create(config);
    }

    public String generateSecretPath() {
        try {
            final RestResponse getResponse = new Rest()
                    .url(vaultUrl + VAULT_GENERATE_PASSWORD_PATH)
                    .header(HEADER_VAULT_TOKEN, vaultToken)
                    .get();
            if (getResponse.getStatus() != 200) {
                String body = new String(getResponse.getBody(), StandardCharsets.UTF_8);
                log.error("Vault password generation failed. Status: {}, Body: {}", getResponse.getStatus(), body);
                throw new IotException("Vault password generation failed: " + body);
            }
            Map<String, Object> response = objectMapper.readValue(
                    new String(getResponse.getBody(), StandardCharsets.UTF_8),
                    Argument.mapOf(String.class, Object.class));
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get(FIELD_DATA);

            if (data == null) {
                log.error("Vault response missing 'data' field: {}", response);
                throw new IotException("Vault response missing 'data' field");
            }

            return (String) data.get(FIELD_PASSWORD);
        } catch (Exception e) {
            log.error("Unable to generate secretPath", e);
            throw new IotException("Unable to generate secretPath", e);
        }
    }

    public String generateAndStoreSecretPath(String pathSuffix) throws VaultException {
        var password = generateSecretPath();
        log.info("Generated secretPath for pathSuffix: {} and secretPath {}", pathSuffix, password);
        return storeSecretPath(pathSuffix, password);
    }

    public String storeSecretPath(String pathSuffix, String password) throws VaultException {
        vault.logical().write(
                SECRET_DATA_PREFIX + pathSuffix, Map.of(FIELD_PASSWORD, password));
        return SECRET_DATA_PREFIX + pathSuffix;
    }

    public String readSecret(String secretPath) throws VaultException {
        log.info("Reading secret from vault for secretPath: {}", secretPath);
        return vault.logical().read(secretPath).getData().get(FIELD_PASSWORD);
    }
}
