package app.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Everything the analytics page draws, for one user over one window. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendingSummary {

    private BigDecimal moneyIn;

    private BigDecimal moneyOut;

    private BigDecimal net;

    private BigDecimal previousOut;

    /**
     * How this window's spending compares with the one before it, as a
     * fraction: -0.15 is 15% less. Null when there is nothing to compare
     * against, which is a different thing from no change.
     */
    private Double outChange;

    private List<AmountShare> categories;

    private List<AmountShare> counterparties;

    private List<WeeklyTotal> weeks;

    /**
     * The largest single week in the chart, in or out. One scale for both
     * halves, so a tall in bar and a tall out bar mean the same thing.
     */
    private BigDecimal peak;

    private int movements;
}
