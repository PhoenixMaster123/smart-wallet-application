package app.subscription.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SubscriptionPeriod {
    MONTHLY("Monthly (1 month)"),
    YEARLY("Yearly (12 months)");

    private final String displayName;
}
