package app.web.dto;

import app.subscription.model.SubscriptionPeriod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeRequest {

    @NotNull
    private SubscriptionPeriod period;

    @NotNull
    private UUID walletId;
}
