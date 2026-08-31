// Mirrors what the running application creates on first start, so the demo
// opens on the same state a fresh `./mvnw spring-boot:run` would show.
//
// Nothing here reaches a server: there is no backend behind GitHub Pages.

export const SEED_LOGIN = { username: 'example', password: 'password123' };

export const SEED_USERS = [
  {
    id: 'a4f1c9e2-3b7d-4c81-9f26-5d8e0a1b7c34',
    username: 'example',
    password: 'password123',
    firstName: 'Demo',
    lastName: 'User',
    email: 'example@smartwallet.com',
    profilePicture: '',
    role: 'USER',
    country: 'BULGARIA',
    active: true,
    createdOn: '2025-01-08T21:39:00',
    updatedOn: '2025-01-08T21:39:00',
  },
  {
    id: 'b7e2d0a4-6c19-4f53-8a7b-2e9f4c6d1a85',
    username: 'admin',
    password: 'password123',
    firstName: 'System',
    lastName: 'Admin',
    email: 'admin@smartwallet.com',
    profilePicture: '',
    role: 'ADMIN',
    country: 'BULGARIA',
    active: true,
    createdOn: '2024-12-11T21:09:00',
    updatedOn: '2024-12-11T21:09:00',
  },
  {
    id: '3f76daea-f0ea-4654-be8c-905c31ff7eb8',
    username: 'Vik1234',
    password: 'password123',
    firstName: 'Viktor',
    lastName: 'Todorov',
    email: 'vik@example.com',
    profilePicture: '',
    role: 'USER',
    country: 'BULGARIA',
    active: true,
    createdOn: '2025-01-08T21:39:00',
    updatedOn: '2025-01-08T21:39:00',
  },
  {
    id: '9c8d7e6f-5a4b-3c2d-1e0f-9a8b7c6d5e4f',
    username: 'ivan123',
    password: 'password123',
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
    balance: 50.00,
    currency: 'EUR',
    main: true,
    createdOn: '2024-12-11T21:09:00',
    updatedOn: '2024-12-11T21:09:00',
  },
  {
    id: 'e1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c',
    ownerId: '3f76daea-f0ea-4654-be8c-905c31ff7eb8',
    nickname: 'Vault Zero',
    status: 'ACTIVE',
    balance: 20.00,
    currency: 'EUR',
    main: true,
    createdOn: '2025-01-08T21:39:00',
    updatedOn: '2025-01-08T21:39:00',
  },
  {
    id: '8a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d',
    ownerId: '9c8d7e6f-5a4b-3c2d-1e0f-9a8b7c6d5e4f',
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
  {
    id: '237bf08f-57ba-56a7-011f-2710d5870d01',
    ownerId: 'b7e2d0a4-6c19-4f53-8a7b-2e9f4c6d1a85',
    status: 'ACTIVE',
    period: 'MONTHLY',
    type: 'ULTIMATE',
    price: 49.99,
    renewalAllowed: true,
    createdOn: '2024-12-11T21:09:00',
    expiryOn: '2025-01-11T21:09:00',
  },
  {
    id: '348cf19a-68cb-67b8-122a-3821e6981e12',
    ownerId: '3f76daea-f0ea-4654-be8c-905c31ff7eb8',
    status: 'ACTIVE',
    period: 'MONTHLY',
    type: 'DEFAULT',
    price: 0.00,
    renewalAllowed: true,
    createdOn: '2025-01-08T21:39:00',
    expiryOn: '2025-02-08T21:39:00',
  },
  {
    id: '459df20b-79dc-78c9-233b-4932f7092f23',
    ownerId: '9c8d7e6f-5a4b-3c2d-1e0f-9a8b7c6d5e4f',
    status: 'ACTIVE',
    period: 'MONTHLY',
    type: 'PREMIUM',
    price: 19.99,
    renewalAllowed: true,
    createdOn: '2024-12-11T21:09:00',
    expiryOn: '2025-01-11T21:09:00',
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
