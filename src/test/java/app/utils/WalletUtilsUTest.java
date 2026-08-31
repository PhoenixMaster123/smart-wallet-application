package app.utils;

import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionType;
import app.user.model.User;
import app.wallet.model.Wallet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class WalletUtilsUTest {

    /** A user on `type` who already holds `walletCount` wallets. */
    private static User userOn(SubscriptionType type, int walletCount) {
        List<Wallet> wallets = new ArrayList<>();
        for (int i = 0; i < walletCount; i++) {
            wallets.add(Wallet.builder().id(UUID.randomUUID()).build());
        }

        return User.builder()
                .id(UUID.randomUUID())
                .subscriptions(List.of(Subscription.builder().id(UUID.randomUUID()).type(type).build()))
                .wallets(wallets)
                .build();
    }

    @Test
    @DisplayName("The default tier never unlocks a second wallet")
    void isEligibleToUnlockNewWallet_whenTierIsDefault_thenNeverEligible() {

        assertThat(WalletUtils.isEligibleToUnlockNewWallet(userOn(SubscriptionType.DEFAULT, 0))).isFalse();
        assertThat(WalletUtils.isEligibleToUnlockNewWallet(userOn(SubscriptionType.DEFAULT, 1))).isFalse();
    }

    @Test
    @DisplayName("Premium unlocks a second wallet and stops there")
    void isEligibleToUnlockNewWallet_whenTierIsPremium_thenEligibleBelowTwoWallets() {

        assertThat(WalletUtils.isEligibleToUnlockNewWallet(userOn(SubscriptionType.PREMIUM, 1))).isTrue();
        assertThat(WalletUtils.isEligibleToUnlockNewWallet(userOn(SubscriptionType.PREMIUM, 2))).isFalse();
        assertThat(WalletUtils.isEligibleToUnlockNewWallet(userOn(SubscriptionType.PREMIUM, 3))).isFalse();
    }

    @Test
    @DisplayName("Ultimate unlocks wallets up to five")
    void isEligibleToUnlockNewWallet_whenTierIsUltimate_thenEligibleBelowFiveWallets() {

        assertThat(WalletUtils.isEligibleToUnlockNewWallet(userOn(SubscriptionType.ULTIMATE, 1))).isTrue();
        assertThat(WalletUtils.isEligibleToUnlockNewWallet(userOn(SubscriptionType.ULTIMATE, 4))).isTrue();
        assertThat(WalletUtils.isEligibleToUnlockNewWallet(userOn(SubscriptionType.ULTIMATE, 5))).isFalse();
    }

    @Test
    @DisplayName("Eligibility reads the most recent subscription, which is the first one")
    void isEligibleToUnlockNewWallet_readsTheFirstSubscriptionInTheList() {

        // Given: subscriptions are ordered createdOn DESC, so index 0 is the current one
        User user = User.builder()
                .id(UUID.randomUUID())
                .subscriptions(List.of(
                        Subscription.builder().id(UUID.randomUUID()).type(SubscriptionType.ULTIMATE).build(),
                        Subscription.builder().id(UUID.randomUUID()).type(SubscriptionType.DEFAULT).build()))
                .wallets(new ArrayList<>())
                .build();

        // When & Then
        assertThat(WalletUtils.isEligibleToUnlockNewWallet(user)).isTrue();
    }

    @Test
    @DisplayName("Returns false when user or subscriptions are null or empty")
    void isEligibleToUnlockNewWallet_whenUserOrSubscriptionsNullOrEmpty_thenReturnsFalse() {

        assertThat(WalletUtils.isEligibleToUnlockNewWallet(null)).isFalse();
        assertThat(WalletUtils.isEligibleToUnlockNewWallet(User.builder().subscriptions(null).build())).isFalse();
        assertThat(WalletUtils.isEligibleToUnlockNewWallet(User.builder().subscriptions(List.of()).build())).isFalse();
        assertThat(WalletUtils.isEligibleToUnlockNewWallet(User.builder()
                .subscriptions(List.of(Subscription.builder().type(SubscriptionType.PREMIUM).build()))
                .wallets(null)
                .build())).isFalse();
    }
}
