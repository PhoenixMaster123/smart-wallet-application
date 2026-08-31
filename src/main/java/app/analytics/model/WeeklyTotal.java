package app.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** One bar of the trend chart: what came in and what went out that week. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTotal {

    /** The Monday the week starts on, formatted the way the chart labels it. */
    private String label;

    private BigDecimal moneyIn;

    private BigDecimal moneyOut;
}
