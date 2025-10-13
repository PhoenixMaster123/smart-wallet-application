package app.email;

import app.event.SuccessfulChargeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @EventListener
    public void sendEmail(SuccessfulChargeEvent event) {

        System.out.printf("Sending email for a new payment that happened for the user with email [%s]", event.getEmail());
    }
}

// NOTE: EVENT Listener -> Receives events from other services

// -> Event-Driven Communication
