package de.urbanpulse.dist.outbound.mailer.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.urbanpulse.dist.util.UpdateListenerConfig;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.apache.commons.validator.routines.EmailValidator;

/**
 *
 * @author Christian Müller <christian.mueller@the-urban-institute.de>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailUpdateListener {

    private static final ZoneId ZONE_ID = ZoneId.of("UTC");
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailUpdateListener.class);

    private String id;
    private String statementName;
    private String target;
    private ZonedDateTime lastBatchStartTimestamp = ZonedDateTime.now(ZONE_ID).minusHours(1);
    private long minutesBetweenMessages = 1l;
    private int maxMessagesPerPeriod = 5;
    private int messageCounter = 0;

    public EmailUpdateListener() {

    }

    public EmailUpdateListener(String id, String statementName, String target) {
        this.id = id;
        this.statementName = statementName;
        this.target = target;
    }

    /**
     * Creates an EmailUpdateListener based on the given UpdateListenerConfig.
     * Automatically maps the given target to the schema specific mail target.
     * For example mailto:foo@bar.de gets mapped to foo@bar.de
     * @param config
     */
    public EmailUpdateListener(UpdateListenerConfig config){
        this.id = config.getId();
        this.statementName = config.getStatementName();

        final String email = URI.create(config.getTarget()).getSchemeSpecificPart();
        setTarget(email);

    }

    public boolean isAllowedToSend() {
        updateSendPeriod();
        boolean allowedToSent = messageCounter < maxMessagesPerPeriod;
        if (!allowedToSent) {
            LOGGER.info("Sending to {0} denied. Next mail can be send after {1}", this.id, this.lastBatchStartTimestamp.plusMinutes(minutesBetweenMessages));
        }
        return allowedToSent;
    }

    private void updateSendPeriod() {
        if (lastBatchStartTimestamp.plusMinutes(minutesBetweenMessages).isBefore(ZonedDateTime.now(ZONE_ID))) {
            this.lastBatchStartTimestamp = ZonedDateTime.now(ZoneId.of("UTC"));
            this.messageCounter = 0;
        }
    }

    public void updateMessageSent() {
        this.messageCounter++;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        if(EmailValidator.getInstance().isValid(target)){
            this.target = target;
        }else{
            throw new IllegalArgumentException("The given target [" +target + "] is not a valid email");
        }

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatementName() {
        return statementName;
    }

    public void setStatementName(String name) {
        this.statementName = name;
    }

    public boolean isValid() {
        return (null != statementName) && (null != id);
    }

    public long getMinutesBetweenMessages() {
        return minutesBetweenMessages;
    }

    public void setMinutesBetweenMessages(long minutesBetweenMessages) {
        this.minutesBetweenMessages = minutesBetweenMessages;
    }

    public void setMaxMessagesPerPeriod(int maxMessagesPerPeriod) {
        this.maxMessagesPerPeriod = maxMessagesPerPeriod;
    }

    protected void setLastBatchStartTimestamp(ZonedDateTime lastBatchStartTimestamp) {
        this.lastBatchStartTimestamp = lastBatchStartTimestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EmailUpdateListener other = (EmailUpdateListener) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.id);
        return hash;
    }

}
