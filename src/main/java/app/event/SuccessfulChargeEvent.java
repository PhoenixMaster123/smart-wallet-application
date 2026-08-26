package app.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuccessfulChargeEvent {

    private UUID userId;

    private UUID walletId;

    private String email;

    private BigDecimal amount;

    private LocalDateTime createdOn;
}

// NOTE: EVENT Object -> Used to communicate between services (e.g., via message broker)

// -> Event-Driven Communication
