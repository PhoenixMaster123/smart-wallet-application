package app.subscription.service;

import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionPeriod;
import app.subscription.model.SubscriptionStatus;
import app.subscription.model.SubscriptionType;
import app.subscription.repository.SubscriptionRepository;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.user.model.User;
import app.wallet.service.WalletService;
import app.web.dto.UpgradeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final WalletService walletService;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository, WalletService walletService) {
        this.subscriptionRepository = subscriptionRepository;
        this.walletService = walletService;
    }

    public Subscription createDefaultSubscription(User user) {

        Subscription subscription = Subscription.builder()
                .owner(user)
                .status(SubscriptionStatus.ACTIVE)
                .period(SubscriptionPeriod.MONTHLY)
                .type(SubscriptionType.DEFAULT)
                .price(BigDecimal.ZERO)
                .renewalAllowed(true)
                .createdOn(LocalDateTime.now())
                .expiryOn(LocalDateTime.now().plusMonths(1))
                .build();

        return subscriptionRepository.save(subscription);
    }

    public Transaction upgrade(User user, UpgradeRequest upgradeRequest, SubscriptionType subscriptionType) {

        Optional<Subscription> currentActiveSubscriptionOpt = subscriptionRepository.findByStatusAndOwnerId(SubscriptionStatus.ACTIVE, user.getId());

        if(currentActiveSubscriptionOpt.isEmpty()) {
            throw new RuntimeException("User has no active subscription");
        }

        Subscription currentActiveSubscription = currentActiveSubscriptionOpt.get();

        BigDecimal subscriptionPrice = getUpgradePrice(subscriptionType, upgradeRequest.getPeriod());
        String chargeDescription = "Upgrade request for %s %s".formatted(upgradeRequest.getPeriod(), subscriptionType);

        Transaction chargeResultTransaction = walletService.withdrawal(user, upgradeRequest.getWalletId(), subscriptionPrice, chargeDescription);

        if(chargeResultTransaction.getStatus() == TransactionStatus.FAILED) {
            return chargeResultTransaction;
        }

        // 1. Create a new subscription
        // 2. Complete current subscription

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryOn;

        if(upgradeRequest.getPeriod() == SubscriptionPeriod.MONTHLY) {
            expiryOn =  now.plusMonths(1).truncatedTo(ChronoUnit.DAYS);
        } else {
            expiryOn = now.plusYears(1).truncatedTo(ChronoUnit.DAYS);
        }

        Subscription newActiveSubscription = Subscription.builder()
                .owner(user)
                .status(SubscriptionStatus.ACTIVE)
                .period(upgradeRequest.getPeriod())
                .type(subscriptionType)
                .price(subscriptionPrice)
                .renewalAllowed(upgradeRequest.getPeriod() == SubscriptionPeriod.MONTHLY)
                .createdOn(now)
                .expiryOn(expiryOn)
                .build();

        currentActiveSubscription.setStatus(SubscriptionStatus.COMPLETED);
        currentActiveSubscription.setExpiryOn(now);

        subscriptionRepository.save(currentActiveSubscription);
        subscriptionRepository.save(newActiveSubscription);



        return chargeResultTransaction;

    }

    private BigDecimal getUpgradePrice(SubscriptionType subscriptionType, SubscriptionPeriod period) {

        if(subscriptionType == SubscriptionType.DEFAULT) {
            return BigDecimal.ZERO;
        } else if(subscriptionType == SubscriptionType.PREMIUM && period == SubscriptionPeriod.MONTHLY) {
            return BigDecimal.valueOf(19.99);
        }  else if(subscriptionType == SubscriptionType.PREMIUM && period == SubscriptionPeriod.YEARLY) {
            return BigDecimal.valueOf(199.99);
        }  else if(subscriptionType == SubscriptionType.ULTIMATE && period == SubscriptionPeriod.MONTHLY) {
            return BigDecimal.valueOf(49.99);
        }  else if(subscriptionType == SubscriptionType.ULTIMATE && period == SubscriptionPeriod.YEARLY) {
            return BigDecimal.valueOf(499.99);
        }

        throw new RuntimeException("Price for subscription type %s and period %s not found".formatted(subscriptionType, period));
    }
}
