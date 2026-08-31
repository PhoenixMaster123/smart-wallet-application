package app.analytics.service;

import app.analytics.model.AmountShare;
import app.analytics.model.SpendingSummary;
import app.analytics.model.WeeklyTotal;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads a user's transactions back as a picture of where their money goes.
 *
 * Nothing new is stored for this. A movement only knows whether it was a
 * deposit or a withdrawal, so the shape of it - a transfer, a subscription
 * charge, a top-up - is read out of the description the services write, and
 * the other party to a transfer out of the same string: the withdrawal half of
 * a transfer books its receiver as the platform, so the recipient's name
 * survives nowhere else.
 */
@Service
public class AnalyticsService {

    public static final String CATEGORY_TRANSFERS = "Transfers";
    public static final String CATEGORY_SUBSCRIPTIONS = "Subscriptions";
    public static final String CATEGORY_TOP_UPS = "Top-ups";
    public static final String CATEGORY_OTHER = "Other";
    public static final String PLATFORM_COUNTERPARTY = "Smart Wallet";

    private static final String SUBSCRIPTION_DESCRIPTION = "Upgrade request for";
    private static final String TRANSFER_DESCRIPTION = "Transfer ";
    private static final String TOP_UP_DESCRIPTION = "Top-up";

    private static final Pattern TRANSFER_PARTIES = Pattern.compile("^Transfer (.+?) <> (.+?) \\(");
    private static final DateTimeFormatter WEEK_LABEL = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private static final int WEEKS_CHARTED = 8;
    private static final int COUNTERPARTIES_CHARTED = 5;

    private final TransactionService transactionService;

    @Autowired
    public AnalyticsService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /** What kind of movement a transaction is, read back out of its description. */
    public static String categoryOf(Transaction transaction) {
        String description = transaction.getDescription() == null ? "" : transaction.getDescription();

        if (description.startsWith(SUBSCRIPTION_DESCRIPTION)) {
            return CATEGORY_SUBSCRIPTIONS;
        }
        if (description.startsWith(TRANSFER_DESCRIPTION)) {
            return CATEGORY_TRANSFERS;
        }
        if (description.startsWith(TOP_UP_DESCRIPTION)) {
            return CATEGORY_TOP_UPS;
        }
        return CATEGORY_OTHER;
    }

    /** Who the money moved to or from, as far as the description records it. */
    public static String counterpartyOf(Transaction transaction, String username) {
        Matcher parties = TRANSFER_PARTIES.matcher(
                transaction.getDescription() == null ? "" : transaction.getDescription());

        if (!parties.find()) {
            return PLATFORM_COUNTERPARTY;
        }

        String from = parties.group(1);
        String to = parties.group(2);

        if (from.equals(username)) {
            return to;
        }
        if (to.equals(username)) {
            return from;
        }
        return transaction.getType() == TransactionType.WITHDRAWAL ? to : from;
    }

    /**
     * @param days how far back the summary reaches, or null for everything.
     *             The weekly chart always covers the last eight weeks, so the
     *             shape of the last two months stays comparable whichever
     *             window is asked for.
     */
    public SpendingSummary summarise(UUID userId, String username, Integer days) {
        List<Transaction> settled = transactionService.getAllByUserId(userId).stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.SUCCEEDED)
                .toList();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = days == null ? null : now.minusDays(days);
        LocalDateTime previousFrom = from == null ? null : from.minusDays(days);

        List<Transaction> current = between(settled, from, null);
        List<Transaction> previous = from == null ? List.of() : between(settled, previousFrom, from);

        BigDecimal moneyIn = total(current, TransactionType.DEPOSIT);
        BigDecimal moneyOut = total(current, TransactionType.WITHDRAWAL);
        BigDecimal previousOut = total(previous, TransactionType.WITHDRAWAL);

        List<Transaction> spent = current.stream()
                .filter(transaction -> transaction.getType() == TransactionType.WITHDRAWAL)
                .toList();

        List<WeeklyTotal> charted = weeks(settled, now.toLocalDate());

        return SpendingSummary.builder()
                .moneyIn(moneyIn)
                .moneyOut(moneyOut)
                .net(moneyIn.subtract(moneyOut))
                .previousOut(previousOut)
                .outChange(change(moneyOut, previousOut))
                .categories(shares(group(spent, AnalyticsService::categoryOf), moneyOut))
                .counterparties(topCounterparties(spent, username, moneyOut))
                .weeks(charted)
                .peak(peakOf(charted))
                .movements(current.size())
                .build();
    }

    private List<Transaction> between(List<Transaction> transactions, LocalDateTime from, LocalDateTime to) {
        return transactions.stream()
                .filter(transaction -> from == null || !transaction.getCreatedOn().isBefore(from))
                .filter(transaction -> to == null || transaction.getCreatedOn().isBefore(to))
                .toList();
    }

    private BigDecimal total(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Double change(BigDecimal moneyOut, BigDecimal previousOut) {
        if (previousOut.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return moneyOut.subtract(previousOut)
                .divide(previousOut, 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Map<String, BigDecimal> group(List<Transaction> transactions,
                                          Function<Transaction, String> key) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Transaction transaction : transactions) {
            totals.merge(key.apply(transaction), transaction.getAmount(), BigDecimal::add);
        }
        return totals;
    }

    private List<AmountShare> shares(Map<String, BigDecimal> totals, BigDecimal overall) {
        List<AmountShare> ranked = totals.entrySet().stream()
                .map(entry -> AmountShare.builder()
                        .name(entry.getKey())
                        .amount(entry.getValue().setScale(2, RoundingMode.HALF_UP))
                        .share(shareOf(entry.getValue(), overall))
                        .build())
                .sorted(Comparator.comparing(AmountShare::getAmount).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        return withRunningTotal(ranked);
    }

    private List<AmountShare> withRunningTotal(List<AmountShare> ranked) {
        double travelled = 0;
        for (AmountShare entry : ranked) {
            entry.setPrecedingShare(travelled);
            travelled += entry.getShare();
        }
        return ranked;
    }

    private double shareOf(BigDecimal amount, BigDecimal overall) {
        if (overall.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return amount.divide(overall, 4, RoundingMode.HALF_UP).doubleValue();
    }

    private List<AmountShare> topCounterparties(List<Transaction> spent, String username, BigDecimal moneyOut) {
        List<AmountShare> everyone =
                shares(group(spent, transaction -> counterpartyOf(transaction, username)), moneyOut);

        if (everyone.size() <= COUNTERPARTIES_CHARTED) {
            return everyone;
        }

        // Past the fifth slice a donut stops saying anything, so the tail is one.
        List<AmountShare> charted = new ArrayList<>(everyone.subList(0, COUNTERPARTIES_CHARTED));
        List<AmountShare> tail = everyone.subList(COUNTERPARTIES_CHARTED, everyone.size());

        charted.add(AmountShare.builder()
                .name(CATEGORY_OTHER)
                .amount(tail.stream()
                        .map(AmountShare::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .share(tail.stream().mapToDouble(AmountShare::getShare).sum())
                .build());

        return withRunningTotal(charted);
    }

    /** Never zero: the chart divides by it. */
    private BigDecimal peakOf(List<WeeklyTotal> weeks) {
        return weeks.stream()
                .flatMap(week -> Stream.of(week.getMoneyIn(), week.getMoneyOut()))
                .max(Comparator.naturalOrder())
                .filter(peak -> peak.compareTo(BigDecimal.ZERO) > 0)
                .orElse(BigDecimal.ONE);
    }

    private List<WeeklyTotal> weeks(List<Transaction> settled, LocalDate today) {
        LocalDate thisWeek = today.with(DayOfWeek.MONDAY);
        List<WeeklyTotal> weeks = new ArrayList<>();

        for (int back = WEEKS_CHARTED - 1; back >= 0; back--) {
            LocalDate start = thisWeek.minusWeeks(back);
            List<Transaction> rows = between(settled, start.atStartOfDay(), start.plusWeeks(1).atStartOfDay());

            weeks.add(WeeklyTotal.builder()
                    .label(start.format(WEEK_LABEL))
                    .moneyIn(total(rows, TransactionType.DEPOSIT))
                    .moneyOut(total(rows, TransactionType.WITHDRAWAL))
                    .build());
        }

        return weeks;
    }
}
