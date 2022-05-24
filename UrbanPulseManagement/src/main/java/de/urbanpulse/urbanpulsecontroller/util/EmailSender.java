package de.urbanpulse.urbanpulsecontroller.util;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 *
 * @author mthoma
 */
@Named
@RequestScoped
public class EmailSender {

    private static final Logger LOGGER = Logger.getLogger(EmailSender.class.getName());

    public boolean sendEmail(Session mailSession, String subject, String body) {
        if ("true".equalsIgnoreCase(mailSession.getProperty("mail.debug"))) {
            LOGGER.log(Level.INFO, "would send mail: {0} -> {1}", new Object[]{subject, body});
            return true;
        }
        MimeMessage message = new MimeMessage(mailSession);
        try {
            message.setFrom(new InternetAddress(mailSession.getProperty("mail.from")));
            InternetAddress[] address = {new InternetAddress(mailSession.getProperty("mail.to"))};
            message.setRecipients(Message.RecipientType.TO, address);
            message.setSubject(subject);
            message.setSentDate(new Date());
            message.setText(body);
            Transport.send(message);
            return true;
        } catch (MessagingException ex) {
            LOGGER.log(Level.INFO, "cannot send email", ex);
            return false;
        }
    }
}
