package models;

import com.forwardcat.common.RedisKeys;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import org.apache.mailet.MailAddress;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.mail.internet.AddressException;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpamCatcherTest {

    SpamCatcher spamCatcher;
    Set<String> usernameStopwords = Sets.newHashSet("spammer", "buynow");

    @Mock JedisPool jedisPool;
    @Mock Jedis jedis;

    @Before
    public void setup() {
        when(jedisPool.getResource()).thenReturn(jedis);
        when(jedis.smembers(RedisKeys.USERNAME_STOPWORDS_SET)).thenReturn(usernameStopwords);
        spamCatcher = new SpamCatcher(jedisPool);
    }

    @Test
    public void normalAddressIsNotSpam() {
        MailAddress address = address("jim@forward.cat");
        assertFalse(spamCatcher.isSpam(address));
    }

    @Test
    public void addressWithKeywordIsSpam() {
        MailAddress address = address("hurry-Buy.Now@forward.cat");
        assertTrue(spamCatcher.isSpam(address));
    }

    private MailAddress address(String username) {
        try {
            return new MailAddress(username);
        } catch (AddressException e) {
            throw Throwables.propagate(e);
        }
    }
}
