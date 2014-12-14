package com.forwardcat.james;

import org.apache.mailet.MailetConfig;
import org.apache.mailet.MailetContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.mail.MessagingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FileDKIMSignTest {

    FileDKIMSign mailet;

    String privateKeyFile = "privateKey.pem";

    @Mock ResourceLoader resourcesLoader;
    @Mock Resource resource;
    @Mock MailetConfig config;
    @Mock MailetContext context;

    @Before
    public void setUp() throws MessagingException, UnknownHostException {
        mailet = new FileDKIMSign();
        mailet.setResourceLoader(resourcesLoader);
        when(resourcesLoader.getResource(privateKeyFile)).thenReturn(resource);

        when(config.getMailetContext()).thenReturn(context);
    }

    @Test(expected = MessagingException.class)
    public void fileParamNotSet_shouldThrowException() throws MessagingException {
        mailet.init(config);
    }

    @Test(expected = MessagingException.class)
    public void resourceDoesNotExist_shouldThrowException() throws MessagingException {
        when(config.getInitParameter(FileDKIMSign.PRIVATE_KEY_FILE_PARAM)).thenReturn(privateKeyFile);
        when(resource.exists()).thenReturn(false);

        mailet.init(config);
    }

    @Test(expected = MessagingException.class)
    public void privateKeyNotValid_shouldThrowException() throws MessagingException, IOException {
        when(config.getInitParameter(FileDKIMSign.PRIVATE_KEY_FILE_PARAM)).thenReturn(privateKeyFile);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream("not a valid private key".getBytes()));

        mailet.init(config);
    }

    @Test
    public void privateKeyValid_everythingEndsFine() throws MessagingException, IOException {
        when(config.getInitParameter(FileDKIMSign.PRIVATE_KEY_FILE_PARAM)).thenReturn(privateKeyFile);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(getPrivateKeyByteArray()));

        mailet.init(config);
    }

    private byte[] getPrivateKeyByteArray() {
        return ("-----BEGIN RSA PRIVATE KEY-----\n" +
                "MIICXQIBAAKBgQDMiitd8SM3uN+c8a/73ESdTrcISLAF306hHvQxglBC4/WPIdvr\n" +
                "fTURDPeSVPNZ6bjP6sVPy/VsaFfAwF1b6ZxF7PEXlK8gYp6E8lkGSWI7y722aGzJ\n" +
                "5CIu6DM8NtHmXlAs45vEX4poX0pfeMVqNakyxa0pm8tAFe0ewzW8pblEtQIDAQAB\n" +
                "AoGADO4jJbIrxscCI9rHhEV9dPBX88cckZJ3Vwos58BUMJZWnLDIRU/J/gTy1aZX\n" +
                "J/T1gPdXd97t6eeCvKWsgTX4cfoxqgfYdTW2MoZuQWDpS6xhKjDEHI0Q4M0wta34\n" +
                "j3pLVRlRk3+TCLVOjE1ZjJ1Hd+Syn8HodRBvf5x13hVeZPUCQQD2Ac/eUIATd2BL\n" +
                "biayEmVvZF87ps3ej67+JY4d/aQuce60dhtCCpBwfwFqTrDGOWMOo539nEUK1B8D\n" +
                "etyOe3jHAkEA1NklI8VxZjzRMgXDprfLoRIPcJnobE3bq2120FVfu2dlAq19DIko\n" +
                "Dfume1lAlS+Zk2npQpiVR73avRrtPNeyowJAQlT0vqYIEreigFRAHM23ChUPVJ9C\n" +
                "bVtivOZVbqLAjUFtMr2R1fnRPnQQZqC3K4u3uO/HHuXu+998SUzsgYKrawJBAJfs\n" +
                "bj/8HBb3bfIgfygupB/RvkeG84jqgdL4jQfjCDPBdy3UGx+pfneMmaYNbLWPhjTc\n" +
                "Meyg8FyGvOyhnZgB9bUCQQC45QKz2CYbtJKxMGHzuzaIgyPrSsqMDHR8Gj0uifyi\n" +
                "o8PZOBroz7zUJr11oQb80PuGPKYeSKlCeCGmpPZ0ffFG\n" +
                "-----END RSA PRIVATE KEY-----").getBytes();
    }
}
