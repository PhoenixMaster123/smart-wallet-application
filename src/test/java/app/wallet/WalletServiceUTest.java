package app.wallet;

import app.event.SuccessfulChargeEvent;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.wallet.model.Wallet;
import app.wallet.model.WalletStatus;
import app.wallet.repository.WalletRepository;
import app.wallet.service.WalletService;
import app.web.dto.TransferRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WalletServiceUTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionService transactionService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WalletService walletService;

    private static User user(UUID id) {
        return User.builder()
                .id(id)
                .username("Vik1234")
                .email("vik@example.com")
                .build();
    }

    private static Wallet wallet(UUID walletId, User owner, WalletStatus status, String balance) {
        return Wallet.builder()
                .id(walletId)
                .owner(owner)
                .nickname("Vault Zero")
                .status(status)
                .balance(new BigDecimal(balance))
                .currency(EUR)
                .main(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    /** upsert returns whatever the service handed it, the way a save would. */
    private void returnTransactionAsSaved() {
        when(transactionService.upsert(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /* -------------------------------------------------------------- withdrawal */

    @Test
    @DisplayName("Withdrawal from an active wallet with enough funds succeeds and debits the balance")
    void whenWithdrawal_andWalletIsActiveWithEnoughFunds_thenBalanceIsDebitedAndTransactionSucceeds() {

        // Given
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        User owner = user(userId);
        Wallet wallet = wallet(walletId, owner, WalletStatus.ACTIVE, "20.00");
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        returnTransactionAsSaved();

        // When
        Transaction result = walletService.withdrawal(owner, walletId, new BigDecimal("7.50"), "Coffee");

        // Then
        assertEquals(TransactionStatus.SUCCEEDED, result.getStatus());
        assertEquals(TransactionType.WITHDRAWAL, result.getType());
        assertThat(result.getFailureReason()).isNull();
        assertEquals(0, new BigDecimal("12.50").compareTo(wallet.getBalance()));
        assertEquals(0, new BigDecimal("12.50").compareTo(result.getBalanceLeft()));
        verify(walletRepository).save(wallet);
    }

    @Test
    @DisplayName("A successful withdrawal publishes the charge event")
    void whenWithdrawalSucceeds_thenSuccessfulChargeEventIsPublished() {

        // Given
        UUID userId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        User owner = user(userId);
        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet(walletId, owner, WalletStatus.ACTIVE, "20.00")));
        returnTransactionAsSaved();

        // When
        walletService.withdrawal(owner, walletId, new BigDecimal("5.00"), "Coffee");

        // Then
        ArgumentCaptor<SuccessfulChargeEvent> captor = ArgumentCaptor.forClass(SuccessfulChargeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        SuccessfulChargeEvent event = captor.getValue();
        assertEquals(userId, event.getUserId());
        assertEquals(walletId, event.getWalletId());
        assertEquals("vik@example.com", event.getEmail());
        assertEquals(0, new BigDecimal("5.00").compareTo(event.getAmount()));
    }

    @Test
    @DisplayName("A wallet belonging to someone else is rejected before its balance is considered")
    void whenWithdrawal_andWalletBelongsToAnotherUser_thenTransactionFailsAndBalanceIsUntouched() {

        // Given
        UUID walletId = UUID.randomUUID();
        User owner = user(UUID.randomUUID());
        User intruder = user(UUID.randomUUID());
        Wallet wallet = wallet(walletId, owner, WalletStatus.ACTIVE, "20.00");
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        returnTransactionAsSaved();

        // When
        Transaction result = walletService.withdrawal(intruder, walletId, new BigDecimal("5.00"), "Coffee");

        // Then
        assertEquals(TransactionStatus.FAILED, result.getStatus());
        assertEquals("Wallet not owned by user", result.getFailureReason());
        assertEquals(0, new BigDecimal("20.00").compareTo(wallet.getBalance()));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Ownership is checked ahead of the balance, so an intruder learns nothing about the funds")
    void whenWithdrawal_andWalletBelongsToAnotherUserAndFundsAreShort_thenOwnershipIsTheReportedReason() {

        // Given
        UUID walletId = UUID.randomUUID();
        User owner = user(UUID.randomUUID());
        User intruder = user(UUID.randomUUID());
        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet(walletId, owner, WalletStatus.INACTIVE, "1.00")));
        returnTransactionAsSaved();

        // When
        Transaction result = walletService.withdrawal(intruder, walletId, new BigDecimal("500.00"), "Coffee");

        // Then
        assertEquals("Wallet not owned by user", result.getFailureReason());
    }

    @Test
    @DisplayName("An inactive wallet cannot be withdrawn from")
    void whenWithdrawal_andWalletIsInactive_thenTransactionFailsWithInactiveWallet() {

        // Given
        UUID walletId = UUID.randomUUID();
        User owner = user(UUID.randomUUID());
        Wallet wallet = wallet(walletId, owner, WalletStatus.INACTIVE, "20.00");
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        returnTransactionAsSaved();

        // When
        Transaction result = walletService.withdrawal(owner, walletId, new BigDecimal("5.00"), "Coffee");

        // Then
        assertEquals(TransactionStatus.FAILED, result.getStatus());
        assertEquals("Inactive wallet", result.getFailureReason());
        assertEquals(0, new BigDecimal("20.00").compareTo(wallet.getBalance()));
    }

    @Test
    @DisplayName("A withdrawal larger than the balance fails with not enough funds")
    void whenWithdrawal_andAmountExceedsBalance_thenTransactionFailsWithNotEnoughFunds() {

        // Given
        UUID walletId = UUID.randomUUID();
        User owner = user(UUID.randomUUID());
        Wallet wallet = wallet(walletId, owner, WalletStatus.ACTIVE, "20.00");
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        returnTransactionAsSaved();

        // When
        Transaction result = walletService.withdrawal(owner, walletId, new BigDecimal("20.01"), "Coffee");

        // Then
        assertEquals(TransactionStatus.FAILED, result.getStatus());
        assertEquals("Not enough funds", result.getFailureReason());
        assertEquals(0, new BigDecimal("20.00").compareTo(wallet.getBalance()));
    }

    @Test
    @DisplayName("Withdrawing the entire balance is allowed")
    void whenWithdrawal_andAmountEqualsBalance_thenTransactionSucceedsAndLeavesZero() {

        // Given
        UUID walletId = UUID.randomUUID();
        User owner = user(UUID.randomUUID());
        Wallet wallet = wallet(walletId, owner, WalletStatus.ACTIVE, "20.00");
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        returnTransactionAsSaved();

        // When
        Transaction result = walletService.withdrawal(owner, walletId, new BigDecimal("20.00"), "Coffee");

        // Then
        assertEquals(TransactionStatus.SUCCEEDED, result.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()));
    }

    @Test
    @DisplayName("A failed withdrawal publishes no charge event, so no gift or email follows")
    void whenWithdrawalFails_thenNoEventIsPublished() {

        // Given
        UUID walletId = UUID.randomUUID();
        User owner = user(UUID.randomUUID());
        when(walletRepository.findById(walletId))
                .thenReturn(Optional.of(wallet(walletId, owner, WalletStatus.ACTIVE, "1.00")));
        returnTransactionAsSaved();

        // When
        walletService.withdrawal(owner, walletId, new BigDecimal("500.00"), "Coffee");

        // Then
        verify(eventPublisher, never()).publishEvent(any(SuccessfulChargeEvent.class));
    }

    @Test
    void whenWithdrawal_andWalletDoesNotExist_thenThrowsException() {

        // Given
        UUID walletId = UUID.randomUUID();
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> walletService.withdrawal(user(UUID.randomUUID()), walletId, BigDecimal.ONE, "Coffee"));
    }

    /* ----------------------------------------------------------------- deposit */

    @Test
    @DisplayName("A deposit credits the balance and records a succeeded transaction")
    void whenDeposit_andWalletIsActive_thenBalanceIsCreditedAndTransactionSucceeds() {

        // Given
        UUID walletId = UUID.randomUUID();
        User owner = user(UUID.randomUUID());
        Wallet wallet = wallet(walletId, owner, WalletStatus.ACTIVE, "20.00");
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        // When
        walletService.deposit(walletId, new BigDecimal("5.00"), "Top-up");

        // Then
        assertEquals(0, new BigDecimal("25.00").compareTo(wallet.getBalance()));
        verify(walletRepository).save(wallet);
        verify(transactionService).createNewTransaction(eq(owner), eq("SMART WALLET PLATFORM"),
                eq(walletId.toString()), eq(new BigDecimal("5.00")), eq(new BigDecimal("25.00")), eq(EUR),
                eq(TransactionType.DEPOSIT), eq(TransactionStatus.SUCCEEDED), eq("Top-up"), eq(null));
    }

    @Test
    @DisplayName("A deposit into an inactive wallet is recorded as failed and leaves the balance alone")
    void whenDeposit_andWalletIsInactive_thenTransactionFailsAndBalanceIsUntouched() {

        // Given
        UUID walletId = UUID.randomUUID();
        User owner = user(UUID.randomUUID());
        Wallet wallet = wallet(walletId, owner, WalletStatus.INACTIVE, "20.00");
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        // When
        walletService.deposit(walletId, new BigDecimal("5.00"), "Top-up");

        // Then
        assertEquals(0, new BigDecimal("20.00").compareTo(wallet.getBalance()));
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionService).createNewTransaction(any(), any(), any(), any(), any(), any(),
                eq(TransactionType.DEPOSIT), eq(TransactionStatus.FAILED), any(), eq("Inactive wallet"));
    }

    /* ---------------------------------------------------------------- transfer */

    @Test
    @DisplayName("A transfer debits the sender and credits the recipient's first active wallet")
    void whenTransfer_andEverythingIsValid_thenSenderIsDebitedAndRecipientIsCredited() {

        // Given
        User sender = user(UUID.randomUUID());
        User recipient = User.builder().id(UUID.randomUUID()).username("ivan123").build();

        UUID senderWalletId = UUID.randomUUID();
        UUID recipientWalletId = UUID.randomUUID();
        Wallet senderWallet = wallet(senderWalletId, sender, WalletStatus.ACTIVE, "20.00");
        Wallet recipientWallet = wallet(recipientWalletId, recipient, WalletStatus.ACTIVE, "10.00");

        when(walletRepository.findById(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findById(recipientWalletId)).thenReturn(Optional.of(recipientWallet));
        when(walletRepository.findByOwnerUsername("ivan123")).thenReturn(List.of(recipientWallet));
        returnTransactionAsSaved();

        TransferRequest request = TransferRequest.builder()
                .walletId(senderWalletId)
                .recipientUsername("ivan123")
                .amount(new BigDecimal("7.50"))
                .build();

        // When
        Transaction result = walletService.transfer(sender, request);

        // Then
        assertEquals(TransactionStatus.SUCCEEDED, result.getStatus());
        assertEquals(0, new BigDecimal("12.50").compareTo(senderWallet.getBalance()));
        assertEquals(0, new BigDecimal("17.50").compareTo(recipientWallet.getBalance()));
    }

    @Test
    @DisplayName("Transferring from a wallet you do not own is refused and never reaches the recipient")
    void whenTransfer_andSenderDoesNotOwnTheWallet_thenNothingMoves() {

        // Given
        User intruder = user(UUID.randomUUID());
        User owner = User.builder().id(UUID.randomUUID()).username("someoneElse").build();
        User recipient = User.builder().id(UUID.randomUUID()).username("ivan123").build();

        UUID victimWalletId = UUID.randomUUID();
        Wallet victimWallet = wallet(victimWalletId, owner, WalletStatus.ACTIVE, "20.00");
        Wallet recipientWallet = wallet(UUID.randomUUID(), recipient, WalletStatus.ACTIVE, "10.00");

        when(walletRepository.findById(victimWalletId)).thenReturn(Optional.of(victimWallet));
        when(walletRepository.findByOwnerUsername("ivan123")).thenReturn(List.of(recipientWallet));
        returnTransactionAsSaved();

        TransferRequest request = TransferRequest.builder()
                .walletId(victimWalletId)
                .recipientUsername("ivan123")
                .amount(new BigDecimal("7.50"))
                .build();

        // When
        Transaction result = walletService.transfer(intruder, request);

        // Then
        assertEquals(TransactionStatus.FAILED, result.getStatus());
        assertEquals("Wallet not owned by user", result.getFailureReason());
        assertEquals(0, new BigDecimal("20.00").compareTo(victimWallet.getBalance()));
        assertEquals(0, new BigDecimal("10.00").compareTo(recipientWallet.getBalance()));
    }

    @Test
    @DisplayName("A transfer that fails on funds leaves the recipient untouched")
    void whenTransfer_andSenderHasTooLittle_thenRecipientIsNotCredited() {

        // Given
        User sender = user(UUID.randomUUID());
        User recipient = User.builder().id(UUID.randomUUID()).username("ivan123").build();

        UUID senderWalletId = UUID.randomUUID();
        Wallet senderWallet = wallet(senderWalletId, sender, WalletStatus.ACTIVE, "1.00");
        Wallet recipientWallet = wallet(UUID.randomUUID(), recipient, WalletStatus.ACTIVE, "10.00");

        when(walletRepository.findById(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByOwnerUsername("ivan123")).thenReturn(List.of(recipientWallet));
        returnTransactionAsSaved();

        TransferRequest request = TransferRequest.builder()
                .walletId(senderWalletId)
                .recipientUsername("ivan123")
                .amount(new BigDecimal("500.00"))
                .build();

        // When
        Transaction result = walletService.transfer(sender, request);

        // Then
        assertEquals(TransactionStatus.FAILED, result.getStatus());
        assertEquals(0, new BigDecimal("10.00").compareTo(recipientWallet.getBalance()));
    }

    @Test
    @DisplayName("An inactive recipient wallet is skipped in favour of an active one")
    void whenTransfer_andRecipientHasAnInactiveWalletFirst_thenTheActiveOneIsCredited() {

        // Given
        User sender = user(UUID.randomUUID());
        User recipient = User.builder().id(UUID.randomUUID()).username("ivan123").build();

        UUID senderWalletId = UUID.randomUUID();
        UUID activeRecipientWalletId = UUID.randomUUID();
        Wallet senderWallet = wallet(senderWalletId, sender, WalletStatus.ACTIVE, "20.00");
        Wallet inactive = wallet(UUID.randomUUID(), recipient, WalletStatus.INACTIVE, "0.00");
        Wallet active = wallet(activeRecipientWalletId, recipient, WalletStatus.ACTIVE, "10.00");

        when(walletRepository.findById(senderWalletId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findById(activeRecipientWalletId)).thenReturn(Optional.of(active));
        when(walletRepository.findByOwnerUsername("ivan123")).thenReturn(List.of(inactive, active));
        returnTransactionAsSaved();

        TransferRequest request = TransferRequest.builder()
                .walletId(senderWalletId)
                .recipientUsername("ivan123")
                .amount(new BigDecimal("5.00"))
                .build();

        // When
        walletService.transfer(sender, request);

        // Then
        assertEquals(0, new BigDecimal("15.00").compareTo(active.getBalance()));
        assertEquals(0, new BigDecimal("0.00").compareTo(inactive.getBalance()));
    }

    @Test
    void whenTransfer_andRecipientHasNoActiveWallet_thenThrowsException() {

        // Given
        User sender = user(UUID.randomUUID());
        User recipient = User.builder().id(UUID.randomUUID()).username("ivan123").build();

        UUID senderWalletId = UUID.randomUUID();
        when(walletRepository.findById(senderWalletId))
                .thenReturn(Optional.of(wallet(senderWalletId, sender, WalletStatus.ACTIVE, "20.00")));
        when(walletRepository.findByOwnerUsername("ivan123"))
                .thenReturn(List.of(wallet(UUID.randomUUID(), recipient, WalletStatus.INACTIVE, "10.00")));

        TransferRequest request = TransferRequest.builder()
                .walletId(senderWalletId)
                .recipientUsername("ivan123")
                .amount(new BigDecimal("5.00"))
                .build();

        // When & Then
        assertThrows(RuntimeException.class, () -> walletService.transfer(sender, request));
    }

    /* ----------------------------------------------------- createDefaultWallet */

    @Test
    @DisplayName("A new user's default wallet is active, main and seeded with 20.00 EUR")
    void whenCreateDefaultWallet_thenWalletIsActiveMainAndSeeded() {

        // Given
        User owner = user(UUID.randomUUID());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Wallet result = walletService.createDefaultWallet(owner);

        // Then
        assertEquals(owner, result.getOwner());
        assertEquals(WalletStatus.ACTIVE, result.getStatus());
        assertEquals("Vault Zero", result.getNickname());
        assertEquals(EUR, result.getCurrency());
        assertEquals(0, new BigDecimal("20.00").compareTo(result.getBalance()));
        assertThat(result.isMain()).isTrue();
    }

    /* ----------------------------------------------------------------- guards */

    @Test
    void isWalletOwnedByUser_comparesIdsRatherThanInstances() {

        // Given
        UUID userId = UUID.randomUUID();
        Wallet wallet = wallet(UUID.randomUUID(), user(userId), WalletStatus.ACTIVE, "20.00");

        // When & Then: a different instance carrying the same id still counts as the owner
        assertThat(walletService.isWalletOwnedByUser(user(userId), wallet)).isTrue();
        assertThat(walletService.isWalletOwnedByUser(user(UUID.randomUUID()), wallet)).isFalse();
    }

    @Test
    void hasSufficientFunds_treatsAnExactBalanceAsEnough() {

        // Given
        Wallet wallet = wallet(UUID.randomUUID(), user(UUID.randomUUID()), WalletStatus.ACTIVE, "20.00");

        // When & Then
        assertThat(walletService.hasSufficientFunds(wallet, new BigDecimal("19.99"))).isTrue();
        assertThat(walletService.hasSufficientFunds(wallet, new BigDecimal("20.00"))).isTrue();
        assertThat(walletService.hasSufficientFunds(wallet, new BigDecimal("20.01"))).isFalse();
    }
}
