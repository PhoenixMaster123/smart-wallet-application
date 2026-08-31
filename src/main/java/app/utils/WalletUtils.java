package app.utils;

import app.subscription.model.SubscriptionType;
import app.user.model.User;
import lombok.experimental.UtilityClass;

@UtilityClass
public class WalletUtils {

    public static boolean isEligibleToUnlockNewWallet(User user) {
        if (user == null || user.getSubscriptions() == null || user.getSubscriptions().isEmpty()
                || user.getWallets() == null) {
            return false;
        }

        SubscriptionType subscriptionType = user.getSubscriptions().get(0).getType();
        int walletSize = user.getWallets().size();

        return subscriptionType == SubscriptionType.PREMIUM && walletSize < 2
                || subscriptionType == SubscriptionType.ULTIMATE && walletSize < 5;
    }
}
