package app.wallet.service;

import app.event.SuccessfulChargeEvent;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.wallet.model.Wallet;
import app.wallet.model.WalletStatus;
import app.wallet.repository.WalletRepository;
import app.web.dto.TransferRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

@Service
public class WalletService {

    private static final String SMART_WALLET_IDENTIFIER = "SMART WALLET PLATFORM";
    private static final String INACTIVE_WALLET_FAILURE_REASON = "Inactive wallet";
    private static final String INACTIVE_FUNDS_FAILURE_REASON = "Not enough funds";
    private static final String TOP_UP_DESCRIPTION_FORMAT = "Top-up %.2f EUR";
    private static final String TRANSFER_DESCRIPTION_FORMAT = "Transfer %s <> %s (%.2f EUR)";
    private static final String WALLET_NOT_OWNED_BY_USER_FAILURE_REASON = "Wallet not owned by user";

    private static final BigDecimal INITIAL_WALLET_BALANCE = new BigDecimal("20.00");
    private static final Currency DEFAULT_WALLET_CURRENCY = Currency.getInstance("EUR");

    private static final String FIRST_WALLET_NICKNAME = "Vault Zero";
    private static final String SECONT_WALLET_NICKNAME = "Nova Flow";
    private static final String THIRD_WALLET_NICKNAME = "Pulse Pay";


    private final WalletRepository walletRepository;
    private final TransactionService transactionService;

    // Event publisher -> Used for emitting events
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public WalletService(WalletRepository walletRepository, TransactionService transactionService, ApplicationEventPublisher eventPublisher) {
        this.walletRepository = walletRepository;
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Transaction withdrawal(User user, UUID walletId, BigDecimal amount, String description) {

        Wallet wallet = getById(walletId);
        
        Transaction transaction = Transaction.builder()
                .owner(user)
                .sender(wallet.getId().toString())
                .receiver(SMART_WALLET_IDENTIFIER)
                .amount(amount)
                .currency(wallet.getCurrency())
                .type(TransactionType.WITHDRAWAL)
                .description(description)
                .createdOn(LocalDateTime.now())
                .build();

        if (!isActiveWallet(wallet)) {
            transaction.setFailureReason(INACTIVE_WALLET_FAILURE_REASON);
            transaction.setStatus(TransactionStatus.FAILED);

        } else if (!hasSufficientFunds(wallet, amount)) {
            transaction.setFailureReason(INACTIVE_FUNDS_FAILURE_REASON);
            transaction.setStatus(TransactionStatus.FAILED);
        } else if (!isWalletOwnedByUser(user, wallet)) {
            transaction.setFailureReason(WALLET_NOT_OWNED_BY_USER_FAILURE_REASON);
            transaction.setStatus(TransactionStatus.FAILED);
        } else {
            transaction.setStatus(TransactionStatus.SUCCEEDED);
            wallet.setBalance(wallet.getBalance().subtract(amount));
            wallet.setUpdatedOn(LocalDateTime.now());
            walletRepository.save(wallet);
        }

        SuccessfulChargeEvent event = SuccessfulChargeEvent.builder()
                .userId(user.getId())
                .walletId(walletId)
                .amount(amount)
                .email(user.getEmail())
                .createdOn(LocalDateTime.now())
                .build();
        eventPublisher.publishEvent(event); // Checks all listener events and will call them

        transaction.setBalanceLeft(wallet.getBalance());

        return transactionService.upsert(transaction);
    }

    public boolean isWalletOwnedByUser(User user, Wallet wallet) {
        return wallet.getOwner().getId().equals(user.getId());
    }

    public boolean isActiveWallet(Wallet wallet) {
        return wallet.getStatus() == WalletStatus.ACTIVE;
    }

    public boolean hasSufficientFunds(Wallet wallet, BigDecimal amount) {
        return wallet.getBalance().compareTo(amount) >= 0;
    }

    @Transactional
    public Transaction deposit(UUID walletId, BigDecimal topUpAmount, String description) {

        Wallet wallet = getById(walletId);

        if(wallet.getStatus() == WalletStatus.INACTIVE) {

            return transactionService.createNewTransaction(
                    wallet.getOwner(),
                    SMART_WALLET_IDENTIFIER,
                    wallet.getId().toString(),
                    topUpAmount,
                    wallet.getBalance(),
                    wallet.getCurrency(),
                    TransactionType.DEPOSIT,
                    TransactionStatus.FAILED,
                    description,
                    INACTIVE_WALLET_FAILURE_REASON
                    );
        }

        wallet.setBalance(wallet.getBalance().add(topUpAmount));
        wallet.setUpdatedOn(LocalDateTime.now());
        walletRepository.save(wallet);

        return transactionService.createNewTransaction(
                wallet.getOwner(),
                SMART_WALLET_IDENTIFIER,
                wallet.getId().toString(),
                topUpAmount,
                wallet.getBalance(),
                wallet.getCurrency(),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCEEDED,
                description,
                null
        );
    }

    public void createDefaultWallet(User user) {

        Wallet wallet = Wallet.builder()
                .owner(user)
                .status(WalletStatus.ACTIVE)
                .nickname(FIRST_WALLET_NICKNAME)
                .balance(INITIAL_WALLET_BALANCE)
                .currency(DEFAULT_WALLET_CURRENCY)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .main(true)
                .build();

        walletRepository.save(wallet);
    }

    private Wallet getById(UUID walletId) {
        return walletRepository.findById(walletId).orElseThrow(() -> new RuntimeException("Wallet by id " + walletId + " not found"));
    }

    @Transactional
    public Transaction transfer(TransferRequest transferRequest) {

        Wallet senderWallet = getById(transferRequest.getWalletId());
        Wallet receiverWallet = getFirstByUsername(transferRequest.getRecipientUsername());

        String transferDescription = TRANSFER_DESCRIPTION_FORMAT.formatted(senderWallet.getOwner().getUsername(), receiverWallet.getOwner().getUsername(), transferRequest.getAmount());
        Transaction withdrawalTransaction = withdrawal(senderWallet.getOwner(), senderWallet.getId(), transferRequest.getAmount(), transferDescription);

        if (withdrawalTransaction.getStatus() == TransactionStatus.SUCCEEDED ) {
            deposit(receiverWallet.getId(), transferRequest.getAmount(), transferDescription);
        }

        return withdrawalTransaction;
    }


    private Wallet getFirstByUsername(String recipientUsername) {

        return walletRepository.findByOwnerUsername(recipientUsername).stream()
                .filter(this::isActiveWallet)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Wallet not found for username " + recipientUsername));
    }
}
