package app.analytics;

import app.analytics.model.AmountShare;
import app.analytics.model.SpendingSummary;
import app.analytics.model.WeeklyTotal;
import app.analytics.service.AnalyticsService;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceUTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USERNAME = "example";

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Transaction transaction(TransactionType type, String amount, String description, int daysAgo) {
        return transaction(type, amount, description, daysAgo, TransactionStatus.SUCCEEDED);
    }

    private Transaction transaction(TransactionType type, String amount, String description, int daysAgo,
                                    TransactionStatus status) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .amount(new BigDecimal(amount))
                .type(type)
                .status(status)
                .description(description)
                .createdOn(LocalDateTime.now().minusDays(daysAgo))
                .build();
    }

    @Test
    @DisplayName("categoryOf reads the shape of a movement out of the description the services write")
    void categoryOfReadsTheDescription() {
        assertThat(AnalyticsService.categoryOf(
                transaction(TransactionType.WITHDRAWAL, "19.99", "Upgrade request for MONTHLY PREMIUM", 1)))
                .isEqualTo(AnalyticsService.CATEGORY_SUBSCRIPTIONS);

        assertThat(AnalyticsService.categoryOf(
                transaction(TransactionType.WITHDRAWAL, "5.00", "Transfer example <> ivan123 (5.00 EUR)", 1)))
                .isEqualTo(AnalyticsService.CATEGORY_TRANSFERS);

        assertThat(AnalyticsService.categoryOf(
                transaction(TransactionType.DEPOSIT, "20.00", "Top-up 20.00 EUR", 1)))
                .isEqualTo(AnalyticsService.CATEGORY_TOP_UPS);

        assertThat(AnalyticsService.categoryOf(
                transaction(TransactionType.DEPOSIT, "20.00", "Something else", 1)))
                .isEqualTo(AnalyticsService.CATEGORY_OTHER);
    }

    @Test
    @DisplayName("counterpartyOf picks whichever side of a transfer is not the signed-in user")
    void counterpartyOfPicksTheOtherSide() {
        Transaction sent = transaction(
                TransactionType.WITHDRAWAL, "5.00", "Transfer example <> ivan123 (5.00 EUR)", 1);
        Transaction received = transaction(
                TransactionType.DEPOSIT, "5.00", "Transfer ivan123 <> example (5.00 EUR)", 1);

        assertThat(AnalyticsService.counterpartyOf(sent, USERNAME)).isEqualTo("ivan123");
        assertThat(AnalyticsService.counterpartyOf(received, USERNAME)).isEqualTo("example".equals(USERNAME)
                ? "ivan123"
                : "example");
    }

    @Test
    @DisplayName("counterpartyOf falls back to the platform for anything that is not a transfer")
    void counterpartyOfFallsBackToThePlatform() {
        Transaction topUp = transaction(TransactionType.DEPOSIT, "20.00", "Top-up 20.00 EUR", 1);

        assertThat(AnalyticsService.counterpartyOf(topUp, USERNAME))
                .isEqualTo(AnalyticsService.PLATFORM_COUNTERPARTY);
    }

    @Test
    @DisplayName("summarise totals only settled movements inside the window")
    void summariseTotalsTheWindow() {
        when(transactionService.getAllByUserId(USER_ID)).thenReturn(List.of(
                transaction(TransactionType.DEPOSIT, "100.00", "Top-up 100.00 EUR", 5),
                transaction(TransactionType.WITHDRAWAL, "30.00", "Transfer example <> ivan123 (30.00 EUR)", 4),
                transaction(TransactionType.WITHDRAWAL, "19.99", "Upgrade request for MONTHLY PREMIUM", 3),
                // Outside the window, and so is everything it would add.
                transaction(TransactionType.WITHDRAWAL, "500.00", "Transfer example <> admin (500.00 EUR)", 60),
                // Rejected, so it never moved any money.
                transaction(TransactionType.WITHDRAWAL, "70.00", "Transfer example <> admin (70.00 EUR)", 2,
                        TransactionStatus.FAILED)));

        SpendingSummary summary = analyticsService.summarise(USER_ID, USERNAME, 30);

        assertThat(summary.getMoneyIn()).isEqualByComparingTo("100.00");
        assertThat(summary.getMoneyOut()).isEqualByComparingTo("49.99");
        assertThat(summary.getNet()).isEqualByComparingTo("50.01");
        assertThat(summary.getMovements()).isEqualTo(3);
    }

    @Test
    @DisplayName("summarise splits spending by category and by counterparty, biggest first")
    void summariseSplitsSpending() {
        when(transactionService.getAllByUserId(USER_ID)).thenReturn(List.of(
                transaction(TransactionType.WITHDRAWAL, "60.00", "Transfer example <> ivan123 (60.00 EUR)", 5),
                transaction(TransactionType.WITHDRAWAL, "20.00", "Transfer example <> admin (20.00 EUR)", 4),
                transaction(TransactionType.WITHDRAWAL, "20.00", "Upgrade request for MONTHLY PREMIUM", 3)));

        SpendingSummary summary = analyticsService.summarise(USER_ID, USERNAME, 30);

        assertThat(summary.getCategories())
                .extracting(AmountShare::getName)
                .containsExactly(AnalyticsService.CATEGORY_TRANSFERS, AnalyticsService.CATEGORY_SUBSCRIPTIONS);
        assertThat(summary.getCategories().get(0).getAmount()).isEqualByComparingTo("80.00");
        assertThat(summary.getCategories().get(0).getShare()).isCloseTo(0.8, within(0.001));

        assertThat(summary.getCounterparties())
                .extracting(AmountShare::getName)
                .containsExactly("ivan123", "admin", AnalyticsService.PLATFORM_COUNTERPARTY);
    }

    @Test
    @DisplayName("the shares of the donut are laid end to end so a slice knows where the one before it stopped")
    void sharesCarryARunningTotal() {
        when(transactionService.getAllByUserId(USER_ID)).thenReturn(List.of(
                transaction(TransactionType.WITHDRAWAL, "75.00", "Transfer example <> ivan123 (75.00 EUR)", 5),
                transaction(TransactionType.WITHDRAWAL, "25.00", "Transfer example <> admin (25.00 EUR)", 4)));

        List<AmountShare> counterparties = analyticsService.summarise(USER_ID, USERNAME, 30).getCounterparties();

        assertThat(counterparties.get(0).getPrecedingShare()).isCloseTo(0.0, within(0.001));
        assertThat(counterparties.get(1).getPrecedingShare()).isCloseTo(0.75, within(0.001));
    }

    @Test
    @DisplayName("summarise compares the window with the one before it")
    void summariseComparesWithThePreviousWindow() {
        when(transactionService.getAllByUserId(USER_ID)).thenReturn(List.of(
                transaction(TransactionType.WITHDRAWAL, "80.00", "Transfer example <> ivan123 (80.00 EUR)", 5),
                transaction(TransactionType.WITHDRAWAL, "100.00", "Transfer example <> ivan123 (100.00 EUR)", 40)));

        SpendingSummary summary = analyticsService.summarise(USER_ID, USERNAME, 30);

        assertThat(summary.getPreviousOut()).isEqualByComparingTo("100.00");
        assertThat(summary.getOutChange()).isCloseTo(-0.2, within(0.001));
    }

    @Test
    @DisplayName("outChange is null rather than infinite when the previous window was empty")
    void outChangeIsNullWithoutAPreviousWindow() {
        when(transactionService.getAllByUserId(USER_ID)).thenReturn(List.of(
                transaction(TransactionType.WITHDRAWAL, "80.00", "Transfer example <> ivan123 (80.00 EUR)", 5)));

        assertThat(analyticsService.summarise(USER_ID, USERNAME, 30).getOutChange()).isNull();
    }

    @Test
    @DisplayName("a null window covers everything")
    void aNullWindowCoversEverything() {
        when(transactionService.getAllByUserId(USER_ID)).thenReturn(List.of(
                transaction(TransactionType.DEPOSIT, "10.00", "Top-up 10.00 EUR", 5),
                transaction(TransactionType.DEPOSIT, "90.00", "Top-up 90.00 EUR", 400)));

        SpendingSummary summary = analyticsService.summarise(USER_ID, USERNAME, null);

        assertThat(summary.getMoneyIn()).isEqualByComparingTo("100.00");
        assertThat(summary.getMovements()).isEqualTo(2);
        assertThat(summary.getOutChange()).isNull();
    }

    @Test
    @DisplayName("the chart always covers eight weeks and never divides by a zero peak")
    void theChartAlwaysCoversEightWeeks() {
        when(transactionService.getAllByUserId(USER_ID)).thenReturn(List.of());

        SpendingSummary summary = analyticsService.summarise(USER_ID, USERNAME, 30);

        assertThat(summary.getWeeks()).hasSize(8);
        assertThat(summary.getWeeks()).extracting(WeeklyTotal::getMoneyOut)
                .allMatch(amount -> amount.compareTo(BigDecimal.ZERO) == 0);
        assertThat(summary.getPeak()).isEqualByComparingTo("1");
        assertThat(summary.getCategories()).isEmpty();
        assertThat(summary.getCounterparties()).isEmpty();
    }
}
