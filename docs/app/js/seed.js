// The registry the demo opens on: the accounts the running application seeds
// on first start, and a couple of months of movements behind them.
//
// The history is here rather than left empty because every page reads better
// with something in it - the dashboard, the wallet activity, the transactions
// list and the analytics all draw from these rows. It is built from one ledger
// of events so the two sides of a transfer always agree, and the balances the
// wallets open with are whatever the ledger adds up to.
//
// Nothing here reaches a server: there is no backend behind GitHub Pages.

export const SEED_LOGIN = { username: 'example', password: 'password123' };

export const SMART_WALLET_IDENTIFIER = 'SMART WALLET PLATFORM';

// SubscriptionService.getUpgradePrice, kept in the same shape.
export const SUBSCRIPTION_PRICES = {
  DEFAULT: { MONTHLY: 0.00, YEARLY: 0.00 },
  PREMIUM: { MONTHLY: 19.99, YEARLY: 199.99 },
  ULTIMATE: { MONTHLY: 49.99, YEARLY: 499.99 },
};

/**
 * A LocalDateTime, the way the entities store one: no zone, no offset.
 *
 * toISOString() would convert to UTC, and the pages parse these strings back as
 * local time, so every stored timestamp would come out shifted by the offset.
 */
export function localIso(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
       + `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

/** Midnight-anchored so a page opened at any hour tells the same story. */
function at(daysAgo, hour, minute) {
  const date = new Date();
  date.setDate(date.getDate() - daysAgo);
  date.setHours(hour, minute, 0, 0);
  return date;
}

function daysAhead(days) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  date.setHours(0, 0, 0, 0);
  return localIso(date);
}

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

// Opening state. Every balance below is replaced by what the ledger adds up to.
const BASE_WALLETS = [
  {
    id: 'dccacda6-12d3-422c-8ad0-23992ef55a1b',
    ownerId: 'a4f1c9e2-3b7d-4c81-9f26-5d8e0a1b7c34',
    nickname: 'Vault Zero',
    status: 'ACTIVE',
    balance: 0.00,
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
    balance: 0.00,
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
    balance: 0.00,
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
    balance: 0.00,
    currency: 'EUR',
    main: true,
    createdOn: '2024-12-11T21:09:00',
    updatedOn: '2024-12-11T21:09:00',
  },
];

/**
 * What happened, in the order it happened, newest last.
 *
 * A transfer is one entry and becomes two transactions, the way
 * WalletService.transfer books it: a withdrawal from the sender paired with a
 * deposit to the recipient, both carrying the same description.
 */
const SEED_LEDGER = [
  { daysAgo: 56, hour: 9, minute: 12, kind: 'TOP_UP', to: 'example', amount: 250.00 },
  { daysAgo: 54, hour: 10, minute: 5, kind: 'TOP_UP', to: 'admin', amount: 400.00 },
  { daysAgo: 53, hour: 18, minute: 41, kind: 'TOP_UP', to: 'ivan123', amount: 90.00 },
  { daysAgo: 52, hour: 8, minute: 24, kind: 'TOP_UP', to: 'Vik1234', amount: 60.00 },
  { daysAgo: 51, hour: 13, minute: 2, kind: 'TRANSFER', from: 'example', to: 'ivan123', amount: 42.50 },
  { daysAgo: 50, hour: 7, minute: 30, kind: 'SUBSCRIPTION', from: 'ivan123', type: 'PREMIUM', period: 'MONTHLY' },
  { daysAgo: 49, hour: 19, minute: 55, kind: 'TRANSFER', from: 'Vik1234', to: 'example', amount: 18.00 },
  { daysAgo: 47, hour: 7, minute: 15, kind: 'SUBSCRIPTION', from: 'example', type: 'PREMIUM', period: 'MONTHLY' },
  { daysAgo: 45, hour: 16, minute: 38, kind: 'TRANSFER', from: 'example', to: 'admin', amount: 26.40 },
  { daysAgo: 41, hour: 11, minute: 20, kind: 'TOP_UP', to: 'example', amount: 120.00 },
  { daysAgo: 40, hour: 7, minute: 5, kind: 'SUBSCRIPTION', from: 'admin', type: 'ULTIMATE', period: 'MONTHLY' },
  { daysAgo: 38, hour: 21, minute: 47, kind: 'TRANSFER', from: 'example', to: 'ivan123', amount: 64.00 },
  { daysAgo: 34, hour: 12, minute: 9, kind: 'TRANSFER', from: 'ivan123', to: 'example', amount: 45.00 },
  { daysAgo: 33, hour: 15, minute: 26, kind: 'TRANSFER', from: 'admin', to: 'ivan123', amount: 90.00 },
  { daysAgo: 30, hour: 9, minute: 51, kind: 'TRANSFER', from: 'example', to: 'Vik1234', amount: 12.75 },
  { daysAgo: 26, hour: 17, minute: 33, kind: 'TOP_UP', to: 'example', amount: 80.00 },
  { daysAgo: 22, hour: 20, minute: 14, kind: 'TRANSFER', from: 'example', to: 'ivan123', amount: 33.20 },
  { daysAgo: 20, hour: 7, minute: 30, kind: 'SUBSCRIPTION', from: 'ivan123', type: 'PREMIUM', period: 'MONTHLY' },
  { daysAgo: 18, hour: 14, minute: 48, kind: 'TRANSFER', from: 'admin', to: 'example', amount: 27.50 },
  { daysAgo: 17, hour: 7, minute: 15, kind: 'SUBSCRIPTION', from: 'example', type: 'PREMIUM', period: 'MONTHLY' },
  { daysAgo: 15, hour: 19, minute: 6, kind: 'TRANSFER', from: 'example', to: 'Vik1234', amount: 58.90 },
  { daysAgo: 11, hour: 10, minute: 42, kind: 'TOP_UP', to: 'example', amount: 60.00 },
  { daysAgo: 10, hour: 7, minute: 5, kind: 'SUBSCRIPTION', from: 'admin', type: 'ULTIMATE', period: 'MONTHLY' },
  { daysAgo: 8, hour: 13, minute: 27, kind: 'TRANSFER', from: 'example', to: 'ivan123', amount: 21.30 },
  { daysAgo: 5, hour: 18, minute: 3, kind: 'TRANSFER', from: 'example', to: 'admin', amount: 47.60 },
  { daysAgo: 3, hour: 11, minute: 39, kind: 'TRANSFER', from: 'Vik1234', to: 'example', amount: 15.00 },
  { daysAgo: 2, hour: 16, minute: 21, kind: 'TRANSFER', from: 'admin', to: 'Vik1234', amount: 35.00 },
  { daysAgo: 1, hour: 20, minute: 58, kind: 'TRANSFER', from: 'example', to: 'ivan123', amount: 9.90 },
];

const TRANSFER_DESCRIPTION_FORMAT = 'Transfer %s <> %s (%s EUR)';

function round(n) {
  return Math.round(n * 100) / 100;
}

/** Stable enough to look like the real thing and to stay put between reloads. */
function seedId(index) {
  return `5eed0000-0000-4000-8000-${String(index).padStart(12, '0')}`;
}

function buildRegistry() {
  const wallets = structuredClone(BASE_WALLETS);
  const transactions = [];

  const walletFor = (username) => {
    const user = SEED_USERS.find((u) => u.username === username);
    return wallets.find((w) => w.ownerId === user.id);
  };

  const record = (wallet, moment, type, amount, description) => {
    const incoming = type === 'DEPOSIT';
    wallet.balance = round(wallet.balance + (incoming ? amount : -amount));
    wallet.updatedOn = localIso(moment);

    transactions.push({
      id: seedId(transactions.length + 1),
      ownerId: wallet.ownerId,
      sender: incoming ? SMART_WALLET_IDENTIFIER : wallet.id,
      receiver: incoming ? wallet.id : SMART_WALLET_IDENTIFIER,
      amount,
      currency: wallet.currency,
      type,
      description,
      failureReason: null,
      status: 'SUCCEEDED',
      createdOn: localIso(moment),
      balanceLeft: wallet.balance,
    });
  };

  for (const event of SEED_LEDGER) {
    const moment = at(event.daysAgo, event.hour, event.minute);

    if (event.kind === 'TOP_UP') {
      record(walletFor(event.to), moment, 'DEPOSIT', event.amount,
        `Top-up ${event.amount.toFixed(2)} EUR`);
      continue;
    }

    if (event.kind === 'SUBSCRIPTION') {
      const price = SUBSCRIPTION_PRICES[event.type][event.period];
      record(walletFor(event.from), moment, 'WITHDRAWAL', price,
        `Upgrade request for ${event.period} ${event.type}`);
      continue;
    }

    const description = TRANSFER_DESCRIPTION_FORMAT
      .replace('%s', event.from)
      .replace('%s', event.to)
      .replace('%s', event.amount.toFixed(2));

    record(walletFor(event.from), moment, 'WITHDRAWAL', event.amount, description);
    record(walletFor(event.to), moment, 'DEPOSIT', event.amount, description);
  }

  return { wallets, transactions };
}

const registry = buildRegistry();

export const SEED_WALLETS = registry.wallets;

export const SEED_TRANSACTIONS = registry.transactions;

// Dated off today so the dashboard never opens on a subscription that ran out
// months ago. The completed rows are the tiers the ledger's charges paid for.
export const SEED_SUBSCRIPTIONS = [
  {
    id: '126af97e-46af-4596-900e-1609c4769c90',
    ownerId: 'a4f1c9e2-3b7d-4c81-9f26-5d8e0a1b7c34',
    status: 'COMPLETED',
    period: 'MONTHLY',
    type: 'DEFAULT',
    price: 0.00,
    renewalAllowed: true,
    createdOn: localIso(at(56, 9, 10)),
    expiryOn: localIso(at(47, 7, 15)),
  },
  {
    id: '5f2a1b0c-9d8e-4f7a-6b5c-4d3e2f1a0b99',
    ownerId: 'a4f1c9e2-3b7d-4c81-9f26-5d8e0a1b7c34',
    status: 'ACTIVE',
    period: 'MONTHLY',
    type: 'PREMIUM',
    price: 19.99,
    renewalAllowed: true,
    createdOn: localIso(at(47, 7, 15)),
    expiryOn: daysAhead(13),
  },
  {
    id: '8c1d0e9f-2a3b-4c5d-8e7f-6a5b4c3d2e11',
    ownerId: 'b7e2d0a4-6c19-4f53-8a7b-2e9f4c6d1a85',
    status: 'COMPLETED',
    period: 'MONTHLY',
    type: 'DEFAULT',
    price: 0.00,
    renewalAllowed: true,
    createdOn: localIso(at(54, 10, 0)),
    expiryOn: localIso(at(40, 7, 5)),
  },
  {
    id: '237bf08f-57ba-56a7-011f-2710d5870d01',
    ownerId: 'b7e2d0a4-6c19-4f53-8a7b-2e9f4c6d1a85',
    status: 'ACTIVE',
    period: 'MONTHLY',
    type: 'ULTIMATE',
    price: 49.99,
    renewalAllowed: true,
    createdOn: localIso(at(40, 7, 5)),
    expiryOn: daysAhead(20),
  },
  {
    id: '348cf19a-68cb-67b8-122a-3821e6981e12',
    ownerId: '3f76daea-f0ea-4654-be8c-905c31ff7eb8',
    status: 'ACTIVE',
    period: 'MONTHLY',
    type: 'DEFAULT',
    price: 0.00,
    renewalAllowed: true,
    createdOn: localIso(at(52, 8, 20)),
    expiryOn: daysAhead(30),
  },
  {
    id: '459df20b-79dc-78c9-233b-4932f7092f23',
    ownerId: '9c8d7e6f-5a4b-3c2d-1e0f-9a8b7c6d5e4f',
    status: 'ACTIVE',
    period: 'MONTHLY',
    type: 'PREMIUM',
    price: 19.99,
    renewalAllowed: true,
    createdOn: localIso(at(50, 7, 30)),
    expiryOn: daysAhead(10),
  },
];
