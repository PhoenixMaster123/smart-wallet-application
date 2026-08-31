// Mirrors what the running application creates on first start, so the demo
// opens on the same state a fresh `./mvnw spring-boot:run` would show.
//
//   application.properties            -> the default user
//   WalletService.createDefaultWallet -> Vault Zero, 20.00 EUR, ACTIVE, main
//   SubscriptionService
//     .createDefaultSubscription      -> DEFAULT / MONTHLY / 0.00, renews
//
// Nothing here reaches a server: there is no backend behind GitHub Pages.

export const SEED_LOGIN = { username: 'Vik1234', password: '123123' };

// A second account so a transfer has somewhere to land.
export const SEED_USERS = [
  {
    id: 'a4f1c9e2-3b7d-4c81-9f26-5d8e0a1b7c34',
    username: 'Vik1234',
    password: '123123',
    firstName: '',
    lastName: '',
    email: '',
    profilePicture: '',
    role: 'USER',
    country: 'BULGARIA',
    active: true,
    createdOn: '2025-01-08T21:39:00',
    updatedOn: '2025-01-08T21:39:00',
  },
  {
    id: 'b7e2d0a4-6c19-4f53-8a7b-2e9f4c6d1a85',
    username: 'ivan123',
    password: '123123',
    firstName: 'Ivan',
    lastName: 'Petrov',
    email: 'ivan@example.com',
    profilePicture: '',
    role: 'ADMIN',
    country: 'BULGARIA',
    active: true,
    createdOn: '2024-12-11T21:09:00',
    updatedOn: '2024-12-11T21:09:00',
  },
];

export const SEED_WALLETS = [
  {
    id: 'dccacda6-12d3-422c-8ad0-23992ef55a1b',
    ownerId: 'a4f1c9e2-3b7d-4c81-9f26-5d8e0a1b7c34',
    nickname: 'Vault Zero',
    status: 'ACTIVE',
    balance: 20.00,
    currency: 'EUR',
    main: true,
    createdOn: '2025-01-08T21:39:00',
    updatedOn: '2025-01-08T21:39:00',
  },
  {
    id: 'f3b8c1d7-49a2-4e60-b5c3-7a1d8e2f9b04',
    ownerId: 'b7e2d0a4-6c19-4f53-8a7b-2e9f4c6d1a85',
    nickname: 'Vault Zero',
    status: 'ACTIVE',
    balance: 20.00,
    currency: 'EUR',
    main: true,
    createdOn: '2024-12-11T21:09:00',
    updatedOn: '2024-12-11T21:09:00',
  },
];

export const SEED_SUBSCRIPTIONS = [
  {
    id: '126af97e-46af-4596-900e-1609c4769c90',
    ownerId: 'a4f1c9e2-3b7d-4c81-9f26-5d8e0a1b7c34',
    status: 'ACTIVE',
    period: 'MONTHLY',
    type: 'DEFAULT',
    price: 0.00,
    renewalAllowed: true,
    createdOn: '2025-01-08T21:39:00',
    expiryOn: '2025-02-08T21:39:00',
  },
];

export const SEED_TRANSACTIONS = [];

// SubscriptionService.getUpgradePrice, kept in the same shape.
export const SUBSCRIPTION_PRICES = {
  DEFAULT: { MONTHLY: 0.00, YEARLY: 0.00 },
  PREMIUM: { MONTHLY: 19.99, YEARLY: 199.99 },
  ULTIMATE: { MONTHLY: 49.99, YEARLY: 499.99 },
};

export const SMART_WALLET_IDENTIFIER = 'SMART WALLET PLATFORM';
