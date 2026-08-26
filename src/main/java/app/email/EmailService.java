package app.email;

import app.event.SuccessfulChargeEvent;
import app.user.model.User;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Async
    @EventListener
    public void sendEmail(SuccessfulChargeEvent event) {

        System.out.printf("Sending email for a new payment that happened for the user with email [%s]",
                event.getEmail());
    }

    public void sendReminderEmail(User user) {

        System.out.printf("Email sent to [%s] with username [%s]. \n", user.getEmail(), user.getUsername());
    }
}

// NOTE: EVENT Listener -> Receives events from other services

// -> Event-Driven Communication
