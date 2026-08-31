package app.utils;

import app.notification.client.dto.Email;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class EmailUtils {

    public static long getNonFailedEmailsCount(List<Email> emails) {
        if (emails == null) {
            return 0;
        }
        return emails.stream()
                .filter(e -> e != null && "SUCCEEDED".equals(e.getStatus()))
                .count();
    }

    public static long getFailedEmailsCount(List<Email> emails) {
        if (emails == null) {
            return 0;
        }
        return emails.stream()
                .filter(e -> e != null && "FAILED".equals(e.getStatus()))
                .count();
    }
}
