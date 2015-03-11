package com.forwardcat.james;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forwardcat.common.ProxyMail;
import com.forwardcat.common.RedisKeys;
import com.google.common.annotations.VisibleForTesting;
import org.apache.james.core.MailImpl;
import org.apache.james.transport.mailets.AbstractRedirect;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.RFC2822Headers;
import org.apache.mailet.base.RFC822DateFormat;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.forwardcat.common.RedisKeys.generateProxyKey;
import static com.forwardcat.james.MailUtils.shouldBounce;

public class ForwardMailet extends AbstractRedirect {

    @VisibleForTesting
    static final String BOUNCE_ATTRIBUTE = "com.forwardcat.bounce";
    private ForwardCatResourcesProvider resourcesProvider;
    private RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();

    @Resource(name = "forwardcatresources")
    public void setResourceProvider(ForwardCatResourcesProvider resourcesProvider) {
        this.resourcesProvider = resourcesProvider;
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        // Don't accept emails sent to multiple recipients
        MailAddress recipient = getRecipient(mail);
        if (recipient == null) {
            reject(mail);
            return;
        }

        // Getting the proxy information
        JedisPool pool = resourcesProvider.getPool();
        Jedis jedis = pool.getResource();
        ProxyMail proxy = null;
        try {
            String proxyKey = generateProxyKey(recipient);
            String proxyString = jedis.get(proxyKey);
            if (proxyString != null) {
                ObjectMapper mapper = resourcesProvider.getMapper();
                proxy = mapper.readValue(proxyString, ProxyMail.class);
            }
        } catch (Exception e) {
            log("Error: " + getStackTrace(e));
            reject(mail);
            return;
        } finally {
            pool.returnResource(jedis);
        }

        logIfDebug("New mail - sender: %s, recipients: %s, name: %s, remoteHost: %s, remoteAddr: %s, state: %s, lastUpdated: %s, errorMessage: %s",
                mail.getSender(), arrayToString(mail.getRecipients().toArray()), mail.getName(), mail.getRemoteHost(),
                mail.getRemoteAddr(), mail.getState(), mail.getLastUpdated(), mail.getErrorMessage());
        logIfDebug("ForwardMailet.cat - headers: ", getMessageHeaders(mail.getMessage()));

        // Checking whether we have this proxy created
        if (proxy == null) {
            logIfDebug("Proxy not found for key: %s - Sender: %s", recipient, mail.getSender());
            reject(mail);
            incrementCounter(RedisKeys.EMAILS_BLOCKED_COUNTER);
            return;
        }

        // Checking whether we have this proxy created
        if (!proxy.isActive()) {
            logIfDebug("Proxy not active: %s", recipient);
            reject(mail);
            incrementCounter(RedisKeys.EMAILS_BLOCKED_COUNTER);
            return;
        }

        // Checking whether the proxy is considered to be a spammer: just ignore
        if (proxy.isBlocked()) {
            logIfDebug("Proxy is blocked: %s", recipient);
            ignore(mail);
            incrementCounter(RedisKeys.SPAMMER_EMAILS_BLOCKED_COUNTER);
            return;
        }

        // Getting the proxied address and sending the new mail
        String proxiedMail = proxy.getUserAddress();
        MailAddress to = new MailAddress(proxiedMail);
        buildNewMail(mail, to);
        incrementCounter(RedisKeys.EMAILS_FORWARDED_COUNTER);
    }

    /**
     * Increments the given key in Redis
     */
    private void incrementCounter(String key) {
        JedisPool pool = resourcesProvider.getPool();
        Jedis jedis = pool.getResource();
        try {
            jedis.incr(key);
        } catch (Exception ex) {
            log("Error: " + getStackTrace(ex));
        } finally {
            pool.returnResource(jedis);
        }
    }

    private void reject(Mail mail) {
        if (shouldBounce(mail)) {
            mail.setAttribute(BOUNCE_ATTRIBUTE, "true");
        } else {
            logIfDebug("Ignoring %s", mail.getName());
            ignore(mail);
        }
    }

    private void ignore(Mail mail) {
        mail.setState(Mail.GHOST);
    }

    /**
     * Service does the hard work, and redirects the originalMail in the form
     * specified.
     * Based on its super-type, but changing the sender
     */
    private void buildNewMail(Mail originalMail, MailAddress to) throws MessagingException {

        // duplicates the Mail object, to be able to modify the new mail keeping
        // the original untouched
        MailImpl newMail = new MailImpl(originalMail);
        try {
            // We don't need to use the original Remote Address and Host,
            // and doing so would likely cause a loop with spam detecting
            // matchers.
            try {
                newMail.setRemoteAddr(dns.getLocalHost().getHostAddress());
            } catch (UnknownHostException e) {
                newMail.setRemoteAddr("127.0.0.1");
            }
            try {
                newMail.setRemoteHost(dns.getLocalHost().getHostName());
            } catch (UnknownHostException e) {
                newMail.setRemoteHost("localhost");
            }

            logIfDebug("New mail - sender: %s, recipients: %s, name: %s, remoteHost: %s, remoteAddr: %s, state: %s, lastUpdated: %s, errorMessage: %s",
                    newMail.getSender(), arrayToString(newMail.getRecipients().toArray()), newMail.getName(), newMail.getRemoteHost(),
                    newMail.getRemoteAddr(), newMail.getState(), newMail.getLastUpdated(), newMail.getErrorMessage());

            // Create the message
            logIfDebug("Alter message");
            newMail.setMessage(new MimeMessage(Session.getDefaultInstance(System.getProperties(), null)));

            // handle the new message if altered
            buildAlteredMessage(newMail, originalMail);


            // Set additional headers
            List<MailAddress> newRecipients = new ArrayList<MailAddress>();
            newRecipients.add(to);
            setRecipients(newMail, newRecipients, originalMail);

            InternetAddress[] newTo = new InternetAddress[]{to.toInternetAddress()};
            setTo(newMail, newTo, originalMail);

            setSubjectPrefix(newMail, getSubjectPrefix(originalMail), originalMail);

            if (newMail.getMessage().getHeader(RFC2822Headers.DATE) == null) {
                newMail.getMessage().setHeader(RFC2822Headers.DATE, rfc822DateFormat.format(new Date()));
            }

            setReplyTo(newMail, getReplyTo(originalMail), originalMail);

            setReversePath(newMail, getReversePath(originalMail), originalMail);

            setSender(newMail, getSender(originalMail), originalMail);

            // That's the main change we perform from the original method:
            newMail.setSender(getSender(originalMail));

            setIsReply(newMail, isReply(originalMail), originalMail);

            newMail.getMessage().saveChanges();

            if (senderDomainIsValid(newMail)) {
                // Send it off...
                getMailetContext().sendMail(newMail);
            } else {
                StringBuffer logBuffer = new StringBuffer(256).append(getMailetName()).append(" mailet cannot forward ").append(originalMail.getName()).append(". Invalid sender domain for ").append(newMail.getSender()).append(". Consider using the Resend mailet ")
                        .append("using a different sender.");
                throw new MessagingException(logBuffer.toString());
            }

        } finally {
            newMail.dispose();
        }

        if (!getPassThrough(originalMail)) {
            originalMail.setState(Mail.GHOST);
        }
    }

    /**
     * Builds the subject of <i>newMail</i> appending the subject of
     * <i>originalMail</i> to <i>subjectPrefix</i>. Is a "setX(Mail, Tx, Mail)"
     * method.
     */
    @Override
    protected void setSubjectPrefix(Mail newMail, String subjectPrefix, Mail originalMail) throws MessagingException {
        String subject = "[" + getRecipient(originalMail) + "] " + originalMail.getMessage().getSubject();
        logIfDebug("Subject set to: %s", subject);
        changeSubject(newMail.getMessage(), subject);
    }

    /**
     * Takes the message of the original Mail and sets it to the new Mail
     *
     * @param originalMail the original Mail object
     * @param newMail      the Mail object to build
     */
    @Override
    protected void buildAlteredMessage(Mail newMail, Mail originalMail) throws MessagingException {
        MimeMessage originalMessage = originalMail.getMessage();
        newMail.setMessage(originalMessage);
    }

    @Override
    protected MailAddress getSender() throws MessagingException {
        String addressString = getInitParameter("sender");
        try {
            return new MailAddress(addressString);
        } catch (Exception e) {
            throw new MessagingException("Exception thrown in getSender() parsing: " + addressString, e);
        }
    }

    /**
     * Logs the given format String and arguments if the debug parameter is set to true
     */
    private void logIfDebug(String format, Object... args) {
        if (isDebug) {
            log(String.format(format, args));
        }
    }

    /**
     * Returns the stack trace of the Exception as String
     */
    private String getStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * If the given {@link Mail} has one recipient, returns it.
     * Otherwise returns null.
     */
    private MailAddress getRecipient(Mail mail) {
        if (mail.getRecipients().size() > 1) {
            return null;
        }
        return mail.getRecipients().iterator().next();
    }

    @Override
    public String getMailetInfo() {
        return "Mailet that forwards incoming mails if the user has an active proxy";
    }
}
