package controllers;

import com.google.inject.Inject;
import org.apache.mailet.MailAddress;
import play.mvc.Result;
import redis.clients.jedis.JedisPool;

import java.util.Optional;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static models.ControllerUtils.getMailAddress;

public class ValidateProxy extends AbstractController {

    @Inject
    ValidateProxy(JedisPool jedisPool) {
        super(jedisPool);
    }

    public Result validate(String proxy) {
        // Checking params
        Optional<MailAddress> mailAddress = getMailAddress(proxy);
        if (!mailAddress.isPresent()) {
            return ok(Boolean.FALSE.toString());
        }

        String proxyKey = generateProxyKey(mailAddress.get());

        Boolean valid = dbFunction(jedis -> !jedis.exists(proxyKey))
                .orElseGet(() -> false);

        return ok(valid.toString());
    }
}
