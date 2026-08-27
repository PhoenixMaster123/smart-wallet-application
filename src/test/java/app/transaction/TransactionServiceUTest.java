package app.transaction;

import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.repository.TransactionRepository;
import app.transaction.service.TransactionService;
import app.user.model.User;
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
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceUTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("createNewTransaction persists every field it was handed and stamps createdOn")
    void whenCreateNewTransaction_thenAllFieldsArePersistedAndCreatedOnIsStamped() {

        // Given
        User owner = User.builder().id(UUID.randomUUID()).username("Vik1234").build();
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        transactionService.createNewTransaction(owner, "sender-id", "receiver-id",
                new BigDecimal("7.50"), new BigDecimal("12.50"), EUR,
                TransactionType.WITHDRAWAL, TransactionStatus.SUCCEEDED, "Coffee", null);

        // Then
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        Transaction saved = captor.getValue();
        assertEquals(owner, saved.getOwner());
        assertEquals("sender-id", saved.getSender());
        assertEquals("receiver-id", saved.getReceiver());
        assertEquals(0, new BigDecimal("7.50").compareTo(saved.getAmount()));
        assertEquals(0, new BigDecimal("12.50").compareTo(saved.getBalanceLeft()));
        assertEquals(EUR, saved.getCurrency());
        assertEquals(TransactionType.WITHDRAWAL, saved.getType());
        assertEquals(TransactionStatus.SUCCEEDED, saved.getStatus());
        assertEquals("Coffee", saved.getDescription());
        assertThat(saved.getFailureReason()).isNull();
        assertThat(saved.getCreatedOn()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("A failed transaction keeps its failure reason")
    void whenCreateNewTransaction_andItFailed_thenTheFailureReasonIsPersisted() {

        // Given
        User owner = User.builder().id(UUID.randomUUID()).build();
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Transaction result = transactionService.createNewTransaction(owner, "sender", "receiver",
                BigDecimal.ONE, BigDecimal.ZERO, EUR, TransactionType.DEPOSIT,
                TransactionStatus.FAILED, "Top-up", "Inactive wallet");

        // Then
        assertEquals(TransactionStatus.FAILED, result.getStatus());
        assertEquals("Inactive wallet", result.getFailureReason());
    }

    @Test
    void whenUpsert_thenTheTransactionIsHandedToTheRepository() {

        // Given
        Transaction transaction = Transaction.builder().id(UUID.randomUUID()).build();
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        // When
        Transaction result = transactionService.upsert(transaction);

        // Then
        assertEquals(transaction, result);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void whenGetById_andTheTransactionExists_thenItIsReturned() {

        // Given
        UUID id = UUID.randomUUID();
        Transaction transaction = Transaction.builder().id(id).build();
        when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));

        // When
        Transaction result = transactionService.getById(id);

        // Then
        assertEquals(transaction, result);
    }

    @Test
    void whenGetById_andTheTransactionIsMissing_thenThrowsException() {

        // Given
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> transactionService.getById(id));
    }

    @Test
    void whenGetAllByUserId_thenTheRepositoryResultIsReturned() {

        // Given
        UUID userId = UUID.randomUUID();
        List<Transaction> transactions = List.of(
                Transaction.builder().id(UUID.randomUUID()).build(),
                Transaction.builder().id(UUID.randomUUID()).build());
        when(transactionRepository.findAllByOwnerId(userId)).thenReturn(transactions);

        // When
        List<Transaction> result = transactionService.getAllByUserId(userId);

        // Then
        assertEquals(2, result.size());
        assertEquals(transactions, result);
    }

    @Test
    void whenGetAllByUserId_andTheUserHasNone_thenAnEmptyListIsReturned() {

        // Given
        UUID userId = UUID.randomUUID();
        when(transactionRepository.findAllByOwnerId(userId)).thenReturn(List.of());

        // When
        List<Transaction> result = transactionService.getAllByUserId(userId);

        // Then
        assertThat(result).isEmpty();
    }
}
