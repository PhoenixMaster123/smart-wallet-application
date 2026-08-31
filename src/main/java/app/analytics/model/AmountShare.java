package app.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One slice of what went out: a name, what it came to, and how much of the
 * total it accounts for. Used for both the categories and the counterparties,
 * which the page draws the same way.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmountShare {

    private String name;

    private BigDecimal amount;

    /** 0 to 1, so the template can size a bar without doing arithmetic. */
    private double share;

    /**
     * Everything ranked above this entry, added up. A donut has to know where
     * the slice before it stopped, and a template cannot carry a running total
     * through a loop.
     */
    private double precedingShare;
}
