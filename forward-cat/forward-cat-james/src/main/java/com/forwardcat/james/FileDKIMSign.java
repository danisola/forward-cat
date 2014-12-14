package com.forwardcat.james;

import org.apache.commons.io.IOUtils;
import org.apache.commons.ssl.PKCS8Key;
import org.apache.james.jdkim.mailets.DKIMSign;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.mail.MessagingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

/**
 * Mailet based on {@link DKIMSign} that loads the private key from a file
 * instead of from a init parameter.
 * <p/>
 * Since the field privateKey is private it {@link DKIMSign}, a new field privateKey
 * is declared in order to override {@link #getPrivateKey()}.
 * <p/>
 * The {@link #init()} method of the superclass is called first in order to load all
 * the other init parameters.
 */
public class FileDKIMSign extends DKIMSign implements ResourceLoaderAware {

    static final String PRIVATE_KEY_FILE_PARAM = "privateKeyFile";

    private PrivateKey privateKey;
    private ResourceLoader resourceLoader;

    @Override
    protected PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void init() throws MessagingException {
        // Loading all the parameters: privateKey will fail
        try {
            super.init();
        } catch (Exception ex) {
            // Do nothing!
        }

        byte[] privateKey = loadPrivateKey();
        String privateKeyPassword = getInitParameter("privateKeyPassword", null);
        try {
            PKCS8Key pkcs8 = new PKCS8Key(new ByteArrayInputStream(
                    privateKey),
                    privateKeyPassword != null ? privateKeyPassword
                            .toCharArray() : null);
            this.privateKey = pkcs8.getPrivateKey();
            // privateKey = DKIMSigner.getPrivateKey(privateKeyString);
        } catch (NoSuchAlgorithmException e) {
            throw new MessagingException("Unknown private key algorithm: "
                    + e.getMessage(), e);
        } catch (InvalidKeySpecException e) {
            throw new MessagingException(
                    "PrivateKey should be in base64 encoded PKCS8 (der) format: "
                            + e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            throw new MessagingException("General security exception: "
                    + e.getMessage(), e);
        }
    }

    private byte[] loadPrivateKey() throws MessagingException {
        String privateKeyFile = getInitParameter(PRIVATE_KEY_FILE_PARAM);
        if (privateKeyFile == null) {
            throw new MessagingException("The init parameter 'privateKeyFile' has not been set");
        }

        Resource resource = resourceLoader.getResource(privateKeyFile);
        if (!resource.exists()) {
            throw new MessagingException("Resource " + privateKeyFile + " has not been found. Make sure " +
                    "is in the conf/ directory");
        }

        try {
            return IOUtils.toByteArray(resource.getInputStream());
        } catch (IOException e) {
            throw new MessagingException("Error while reading private key", e);
        }
    }
}
