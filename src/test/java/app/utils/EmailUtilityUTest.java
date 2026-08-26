package app.utils;

import app.notification.client.dto.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class EmailUtilityUTest {

    @Test
    void getNonFailedEmailsCount_whenPassListOfTwoSucceededAndOneFailedEmails_thenReturnTwo() {

        // Given
        Email oneSucceeded = Email.builder().status("SUCCEEDED").build();
        Email anotherSucceeded = Email.builder().status("SUCCEEDED").build();
        Email oneFailed = Email.builder().status("FAILED").build();
        List<Email> emails = List.of(oneSucceeded, anotherSucceeded, oneFailed);

        // When
        long result = EmailUtils.getNonFailedEmailsCount(emails);

        // Then
        assertEquals(2, result);
    }

    @Test
    void getNonFailedEmailsCount_whenPassEmptyList_thenReturnZero() {

        // Given & When
        long result = EmailUtils.getNonFailedEmailsCount(List.of());

        // Then
        assertEquals(0, result);
    }

    @Test
    void getFailedEmailsCount_whenPassListOfTwoSucceededAndOneFailedEmails_thenReturnOne() {

        // Given
        Email oneSucceeded = Email.builder().status("SUCCEEDED").build();
        Email anotherSucceeded = Email.builder().status("SUCCEEDED").build();
        Email oneFailed = Email.builder().status("FAILED").build();
        List<Email> emails = List.of(oneSucceeded, anotherSucceeded, oneFailed);

        // When
        long result = EmailUtils.getFailedEmailsCount(emails);

        // Then
        assertEquals(1, result);
    }

    @Test
    void getFailedEmailsCount_whenPassEmptyList_thenReturnZero() {

        // Given & When
        long result = EmailUtils.getFailedEmailsCount(List.of());

        // Then
        assertEquals(0, result);
    }
}
