/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.urbanpulse.dist.outbound.mailer.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Christian Müller <christian.mueller@the-urban-institute.de>
 */
public class EmailUpdateListenerTest {

    public EmailUpdateListenerTest() {
    }

    /**
     * Test of isAllowedToSend method, of class EmailUpdateListener.
     */
    @Test
    public void testIsAllowedToSend() {
        EmailUpdateListener instance = new EmailUpdateListener();
        instance.setMaxMessagesPerPeriod(6); //defualt is 5
        instance.setMinutesBetweenMessages(2);
        assertEquals(true, instance.isAllowedToSend());
        instance.updateMessageSent();
        assertEquals(true, instance.isAllowedToSend());
        instance.updateMessageSent();
        assertEquals(true, instance.isAllowedToSend());
        instance.updateMessageSent();
        assertEquals(true, instance.isAllowedToSend());
        instance.updateMessageSent();
        assertEquals(true, instance.isAllowedToSend());
        instance.updateMessageSent();
        assertEquals(true, instance.isAllowedToSend());
        instance.updateMessageSent();
        assertEquals(false, instance.isAllowedToSend());
        //Now we jump into the future and shall be able to send up to 6 messages again
        instance.setLastBatchStartTimestamp(ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(3));
        
        assertEquals(true, instance.isAllowedToSend());
        instance.updateMessageSent();
        assertEquals(true, instance.isAllowedToSend());
        instance.updateMessageSent();
        assertEquals(true, instance.isAllowedToSend());
        instance.updateMessageSent();
        assertEquals(true, instance.isAllowedToSend());
        instance.updateMessageSent();
        assertEquals(true, instance.isAllowedToSend());
        instance.updateMessageSent();
        assertEquals(true, instance.isAllowedToSend());
        instance.updateMessageSent();
        assertEquals(false, instance.isAllowedToSend());
    }

}
