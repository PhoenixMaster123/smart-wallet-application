package app.subscription;

import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionPeriod;
import app.subscription.model.SubscriptionStatus;
import app.subscription.model.SubscriptionType;
import app.subscription.repository.SubscriptionRepository;
import app.subscription.service.SubscriptionService;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.user.model.User;
import app.wallet.service.WalletService;
import app.web.dto.UpgradeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceUTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private WalletService walletService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private static User user(UUID id) {
        return User.builder().id(id).username("Vik1234").build();
    }

    private static Subscription activeDefaultSubscription(User owner) {
        return Subscription.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .status(SubscriptionStatus.ACTIVE)
                .period(SubscriptionPeriod.MONTHLY)
                .type(SubscriptionType.DEFAULT)
                .price(BigDecimal.ZERO)
                .renewalAllowed(true)
                .createdOn(LocalDateTime.now().minusMonths(1))
                .expiryOn(LocalDateTime.now().plusDays(1))
                .build();
    }

    private static Transaction transaction(TransactionStatus status) {
        return Transaction.builder().id(UUID.randomUUID()).status(status).build();
    }

    private static UpgradeRequest request(SubscriptionPeriod period, UUID walletId) {
        return UpgradeRequest.builder().period(period).walletId(walletId).build();
    }

    /* -------------------------------------------------- createDefaultSubscription */

    @Test
    @DisplayName("A new user's default subscription is an active, free, renewing monthly plan")
    void whenCreateDefaultSubscription_thenItIsActiveFreeAndRenewsMonthly() {

        // Given
        User owner = user(UUID.randomUUID());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Subscription result = subscriptionService.createDefaultSubscription(owner);

        // Then
        assertEquals(owner, result.getOwner());
        assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
        assertEquals(SubscriptionPeriod.MONTHLY, result.getPeriod());
        assertEquals(SubscriptionType.DEFAULT, result.getType());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getPrice()));
        assertThat(result.isRenewalAllowed()).isTrue();
        assertThat(result.getExpiryOn()).isCloseTo(LocalDateTime.now().plusMonths(1), within(1, ChronoUnit.MINUTES));
    }

    /* ------------------------------------------------------------------ upgrade */

    @Test
    void whenUpgrade_andUserHasNoActiveSubscription_thenThrowsException() {

        // Given
        User owner = user(UUID.randomUUID());
        when(subscriptionRepository.findByStatusAndOwnerId(SubscriptionStatus.ACTIVE, owner.getId()))
                .thenReturn(Optional.empty());

        UpgradeRequest upgradeRequest = request(SubscriptionPeriod.MONTHLY, UUID.randomUUID());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> subscriptionService.upgrade(owner, upgradeRequest, SubscriptionType.PREMIUM));
    }

    @Test
    @DisplayName("A rejected charge leaves both subscriptions exactly as they were")
    void whenUpgrade_andChargeFails_thenNoSubscriptionIsWrittenAndTheFailureIsReturned() {

        // Given
        User owner = user(UUID.randomUUID());
        Subscription current = activeDefaultSubscription(owner);
        Transaction failed = transaction(TransactionStatus.FAILED);

        when(subscriptionRepository.findByStatusAndOwnerId(SubscriptionStatus.ACTIVE, owner.getId()))
                .thenReturn(Optional.of(current));
        when(walletService.withdrawal(any(), any(), any(), any())).thenReturn(failed);

        // When
        Transaction result = subscriptionService.upgrade(
                owner, request(SubscriptionPeriod.MONTHLY, UUID.randomUUID()), SubscriptionType.PREMIUM);

        // Then
        assertEquals(failed, result);
        assertEquals(SubscriptionStatus.ACTIVE, current.getStatus());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("A successful upgrade completes the old subscription and opens the new one")
    void whenUpgrade_andChargeSucceeds_thenOldSubscriptionCompletesAndNewOneBecomesActive() {

        // Given
        User owner = user(UUID.randomUUID());
        Subscription current = activeDefaultSubscription(owner);

        when(subscriptionRepository.findByStatusAndOwnerId(SubscriptionStatus.ACTIVE, owner.getId()))
                .thenReturn(Optional.of(current));
        when(walletService.withdrawal(any(), any(), any(), any()))
                .thenReturn(transaction(TransactionStatus.SUCCEEDED));

        // When
        subscriptionService.upgrade(
                owner, request(SubscriptionPeriod.MONTHLY, UUID.randomUUID()), SubscriptionType.PREMIUM);

        // Then
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        List<Subscription> saved = captor.getAllValues();
        Subscription completed = saved.get(0);
        Subscription opened = saved.get(1);

        assertEquals(SubscriptionStatus.COMPLETED, completed.getStatus());
        assertThat(completed.getExpiryOn()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));

        assertEquals(SubscriptionStatus.ACTIVE, opened.getStatus());
        assertEquals(SubscriptionType.PREMIUM, opened.getType());
        assertEquals(SubscriptionPeriod.MONTHLY, opened.getPeriod());
        assertEquals(owner, opened.getOwner());
    }

    @Test
    @DisplayName("A monthly plan renews automatically and expires at midnight one month out")
    void whenUpgrade_andPeriodIsMonthly_thenRenewalIsAllowedAndExpiryIsTruncatedToTheDay() {

        // Given
        User owner = user(UUID.randomUUID());
        when(subscriptionRepository.findByStatusAndOwnerId(SubscriptionStatus.ACTIVE, owner.getId()))
                .thenReturn(Optional.of(activeDefaultSubscription(owner)));
        when(walletService.withdrawal(any(), any(), any(), any()))
                .thenReturn(transaction(TransactionStatus.SUCCEEDED));

        // When
        subscriptionService.upgrade(
                owner, request(SubscriptionPeriod.MONTHLY, UUID.randomUUID()), SubscriptionType.ULTIMATE);

        // Then
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        Subscription opened = captor.getAllValues().get(1);

        assertThat(opened.isRenewalAllowed()).isTrue();
        assertEquals(LocalDateTime.now().plusMonths(1).truncatedTo(ChronoUnit.DAYS), opened.getExpiryOn());
    }

    @Test
    @DisplayName("A yearly plan does not auto-renew and expires at midnight one year out")
    void whenUpgrade_andPeriodIsYearly_thenRenewalIsNotAllowedAndExpiryIsOneYearOut() {

        // Given
        User owner = user(UUID.randomUUID());
        when(subscriptionRepository.findByStatusAndOwnerId(SubscriptionStatus.ACTIVE, owner.getId()))
                .thenReturn(Optional.of(activeDefaultSubscription(owner)));
        when(walletService.withdrawal(any(), any(), any(), any()))
                .thenReturn(transaction(TransactionStatus.SUCCEEDED));

        // When
        subscriptionService.upgrade(
                owner, request(SubscriptionPeriod.YEARLY, UUID.randomUUID()), SubscriptionType.PREMIUM);

        // Then
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        Subscription opened = captor.getAllValues().get(1);

        assertThat(opened.isRenewalAllowed()).isFalse();
        assertEquals(LocalDateTime.now().plusYears(1).truncatedTo(ChronoUnit.DAYS), opened.getExpiryOn());
    }

    /* ------------------------------------------------------------------- prices */

    @Test
    @DisplayName("Each tier and period is charged its published price")
    void whenUpgrade_thenTheWalletIsChargedThePublishedPriceForThatTierAndPeriod() {

        assertChargedPrice(SubscriptionType.DEFAULT, SubscriptionPeriod.MONTHLY, BigDecimal.ZERO);
        assertChargedPrice(SubscriptionType.PREMIUM, SubscriptionPeriod.MONTHLY, BigDecimal.valueOf(19.99));
        assertChargedPrice(SubscriptionType.PREMIUM, SubscriptionPeriod.YEARLY, BigDecimal.valueOf(199.99));
        assertChargedPrice(SubscriptionType.ULTIMATE, SubscriptionPeriod.MONTHLY, BigDecimal.valueOf(49.99));
        assertChargedPrice(SubscriptionType.ULTIMATE, SubscriptionPeriod.YEARLY, BigDecimal.valueOf(499.99));
    }

    private void assertChargedPrice(SubscriptionType type, SubscriptionPeriod period, BigDecimal expected) {

        SubscriptionRepository repository = org.mockito.Mockito.mock(SubscriptionRepository.class);
        WalletService wallets = org.mockito.Mockito.mock(WalletService.class);
        SubscriptionService service = new SubscriptionService(repository, wallets);

        User owner = user(UUID.randomUUID());
        UUID walletId = UUID.randomUUID();
        when(repository.findByStatusAndOwnerId(SubscriptionStatus.ACTIVE, owner.getId()))
                .thenReturn(Optional.of(activeDefaultSubscription(owner)));
        when(wallets.withdrawal(any(), any(), any(), any()))
                .thenReturn(transaction(TransactionStatus.FAILED));

        service.upgrade(owner, request(period, walletId), type);

        verify(wallets).withdrawal(eq(owner), eq(walletId), eq(expected),
                eq("Upgrade request for %s %s".formatted(period, type)));
    }
}
