package de.urbanpulse.dist.outbound.mailer.service;

import de.urbanpulse.dist.outbound.MainVerticle;
import de.urbanpulse.dist.outbound.mailer.service.formatter.DefaultEmailFormatter;
import de.urbanpulse.dist.outbound.mailer.service.formatter.EmailFormatter;
import de.urbanpulse.dist.util.StatementConsumerManagementVerticle;
import de.urbanpulse.dist.util.UpdateListenerConfig;
import io.vertx.core.Future;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.serviceproxy.ServiceProxyBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Christian Müller <christian.mueller@the-urban-institute.de>
 */
public class OutboundMailerControllerVerticle extends StatementConsumerManagementVerticle {

    private static final String OUTBOUND_MAILER_VERTX_ADDRESS = "outbound-mailer-service";

    public static final String SETUP_ADDRESS = OutboundMailerControllerVerticle.class.getName();

    protected final Map<UpdateListenerConfig, EmailUpdateListener> updateListenerToMailReceiverMap = new HashMap<>();

    private ServiceBinder serviceBinder;
    private MessageConsumer<JsonObject> outboundMailerProxyConsumer;
    private OutboundMailerService mailer;
    private EmailFormatter emailFormatter;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        startMailerService();
        registerMailerServiceProxy();
        this.emailFormatter = getEmailFormatter();

        statementPrefix = MainVerticle.LOCAL_STATEMENT_PREFIX;
        registerSetupConsumer(SETUP_ADDRESS);

        startFuture.complete();
    }

    private void registerMailerServiceProxy() {
        ServiceProxyBuilder builder = new ServiceProxyBuilder(vertx).setAddress(OUTBOUND_MAILER_VERTX_ADDRESS);
        mailer = builder.build(OutboundMailerService.class);
    }

    private void startMailerService() {
        JsonObject config = config().copy();
        OutboundMailerService service = new OutboundMailerServiceImpl(vertx, config);
        serviceBinder = new ServiceBinder(vertx);
        outboundMailerProxyConsumer = serviceBinder.setAddress(OUTBOUND_MAILER_VERTX_ADDRESS).register(OutboundMailerService.class, service);
    }

    @Override
    protected void handleEvent(String statementName, JsonObject event) {

        List<String> emailAddresses = getEmailAddressesOfActiveReceivers(statementName);

        if (!emailAddresses.isEmpty()) {
            mailer.sendMail(emailAddresses, statementName, emailFormatter.format(event), emailFormatter.requiresHTML(), handler -> {
                if (handler.failed()) {
                    logger.error("Failed to send email(s).", handler.cause());
                }
            });
        }
    }

    /**
     * Iterates the set of receivers and returns a list of email addresses.
     *
     * @param statementName
     * @return
     */
    protected List<String> getEmailAddressesOfActiveReceivers(String statementName) {
        return statementToListenerMap.get(statementName).stream().map(updateListenerToMailReceiverMap::get)
                .filter(EmailUpdateListener::isAllowedToSend).map(u -> {
                    u.updateMessageSent();
                    return u;
                })
                .map(EmailUpdateListener::getTarget).collect(Collectors.toList());
    }




    /**
     * Creates an EmailUpdateListener based of the given UpdateListenerConfig and adds it to the map of mail receivers
     *
     * @param ulConfig the UpdateListenerConfig the EmailUpdateListener is based of
     * @throws IllegalArgumentException if the target of the given config is not a valid email
     */
    @Override
    protected void registerUpdateListener(UpdateListenerConfig ulConfig) {
        EmailUpdateListener mailUpdateListener = new EmailUpdateListener(ulConfig);
        mailUpdateListener.setMinutesBetweenMessages(config().getLong("minutesBetweenMails", 1l));
        mailUpdateListener.setMaxMessagesPerPeriod(config().getInteger("maxMessagesPerPeriod", 5));
        updateListenerToMailReceiverMap.put(ulConfig, mailUpdateListener);
    }

    @Override
    protected void unregisterUpdateListener(UpdateListenerConfig ulConfig) {
        updateListenerToMailReceiverMap.remove(ulConfig);
    }

    @Override
    protected void reset() {
        updateListenerToMailReceiverMap.clear();
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        updateListenerToMailReceiverMap.clear();
        mailer.close();
        serviceBinder.unregister(outboundMailerProxyConsumer);
        super.stop(stopFuture);
    }

    protected EmailFormatter getEmailFormatter() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        String formatterClass = DefaultEmailFormatter.class.getName();
        JsonObject formatterConfig = new JsonObject();
        if (config() != null && config().containsKey("emailFormatter")) {
            formatterClass = config().getJsonObject("emailFormatter").getString("emailFormatterClass");
            formatterConfig = config().getJsonObject("emailFormatter").getJsonObject("emailFormatterConfig");
        }
        logger.info("Using email formatter class: " + formatterClass);
        EmailFormatter formatter = (EmailFormatter) this.getClass().getClassLoader().loadClass(formatterClass).newInstance();
        formatter.setConfig(formatterConfig);
        return formatter;
    }

}
