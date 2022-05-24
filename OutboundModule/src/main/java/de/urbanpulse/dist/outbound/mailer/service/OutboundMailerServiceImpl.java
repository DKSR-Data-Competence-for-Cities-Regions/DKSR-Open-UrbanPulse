package de.urbanpulse.dist.outbound.mailer.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import java.util.List;

/**
 *
 * @author Christian Müller <christian.mueller@the-urban-institute.de>
 */
public class OutboundMailerServiceImpl implements OutboundMailerService {

    public static final String MAIL_SERVER_CONFIG = "mailServerConfig";

    private static final Logger LOG = LoggerFactory.getLogger(OutboundMailerServiceImpl.class);
    private final JsonObject config;
    private final MailClient mailClient;

    OutboundMailerServiceImpl(Vertx vertx, JsonObject config) {
        this.config = config.copy();
        MailConfig mailConfig = createMailConfig(config);
        mailClient = MailClient.createShared(vertx, mailConfig);
    }

    @Override
    public void close() {

    }

    @Override
    public void sendMail(List<String> receiver, String subject, String message, boolean htmlText, Handler<AsyncResult<JsonObject>> handler) {
        MailMessage mailMessage = new MailMessage();
        mailMessage.setFrom(this.config.getString("from"));
        mailMessage.setTo(receiver);
        mailMessage.setSubject(subject);
        if (htmlText) {
            mailMessage.setHtml(message);
        } else {
            mailMessage.setText(message);
        }
        mailClient.sendMail(mailMessage, sent -> {
            if (sent.succeeded()) {
                LOG.debug(message + " sent to: " + receiver);
                handler.handle(Future.succeededFuture(new JsonObject()));
            } else {
                handler.handle(Future.failedFuture(sent.cause()));
            }
        });
    }

    private MailConfig createMailConfig(JsonObject config) {
        JsonObject mailServerConfigJson = config.getJsonObject(MAIL_SERVER_CONFIG);
        MailConfig mailconfig = new MailConfig(mailServerConfigJson);
        LOG.debug(mailconfig.toJson());
        return mailconfig;
    }
}
