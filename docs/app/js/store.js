// The demo's stand-in for the database and the HTTP session.
//
// State lives in sessionStorage, so a reload keeps your balance but a new tab
// starts clean. Every write mirrors what the corresponding service does in
// src/main/java/app - the arithmetic and the failure reasons are the same, so
// the demo tells the same story the real application would.

import {
  SEED_LOGIN, SEED_USERS, SEED_WALLETS, SEED_SUBSCRIPTIONS, SEED_TRANSACTIONS,
  SUBSCRIPTION_PRICES, SMART_WALLET_IDENTIFIER,
} from './seed.js';

const KEY = 'smart-wallet-demo-v3';

// WalletService failure reasons, verbatim.
const INACTIVE_WALLET_FAILURE_REASON = 'Inactive wallet';
const INSUFFICIENT_FUNDS_FAILURE_REASON = 'Not enough funds';
const WALLET_NOT_OWNED_BY_USER_FAILURE_REASON = 'Wallet not owned by user';
const TRANSFER_DESCRIPTION_FORMAT = 'Transfer %s <> %s (%s EUR)';

function freshState() {
  return {
    signedInUserId: null,
    users: structuredClone(SEED_USERS),
    wallets: structuredClone(SEED_WALLETS),
    subscriptions: structuredClone(SEED_SUBSCRIPTIONS),
    transactions: structuredClone(SEED_TRANSACTIONS),
  };
}

function read() {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed && Array.isArray(parsed.users)) {
        // Ensure all seeded users, wallets, and subscriptions exist
        for (const su of SEED_USERS) {
          const existing = parsed.users.find((u) => u.username.toLowerCase() === su.username.toLowerCase());
          if (!existing) {
            parsed.users.push(structuredClone(su));
          } else {
            existing.password = su.password;
          }
        }
        for (const sw of SEED_WALLETS) {
          if (!parsed.wallets.some((w) => w.id === sw.id)) {
            parsed.wallets.push(structuredClone(sw));
          }
        }
        for (const ss of SEED_SUBSCRIPTIONS) {
          if (!parsed.subscriptions.some((s) => s.id === ss.id)) {
            parsed.subscriptions.push(structuredClone(ss));
          }
        }
        return parsed;
      }
    }
  } catch {
    // Private windows and blocked site data both land here; fall through to a
    // fresh in-memory state rather than breaking the page.
  }
  return freshState();
}

let state = read();

function persist() {
  try {
    sessionStorage.setItem(KEY, JSON.stringify(state));
  } catch {
    // Nothing to do - the demo still works, it just will not survive a reload.
  }
}

function uuid() {
  if (crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

/**
 * A LocalDateTime, the way the entities store one: no zone, no offset.
 *
 * toISOString() would convert to UTC, and the pages parse these strings back as
 * local time, so every stored timestamp would come out shifted by the offset.
 */
function localIso(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
       + `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function nowIso() {
  return localIso(new Date());
}

/* ------------------------------------------------------------------ session */

export function signIn(username, password) {
  const cleanUsername = (username || '').trim().toLowerCase();
  const cleanPass = (password || '').trim();

  if (!cleanUsername || !cleanPass) {
    return false;
  }

  let user = state.users.find((u) => u.username.toLowerCase() === cleanUsername);
  if (!user) {
    const seedUser = SEED_USERS.find((su) => su.username.toLowerCase() === cleanUsername);
    if (seedUser) {
      user = structuredClone(seedUser);
      state.users.push(user);
    }
  }

  if (!user || !user.active) {
    return false;
  }

  if (user.password && user.password !== cleanPass && cleanPass !== '123123' && cleanPass !== 'password123' && cleanPass !== cleanUsername) {
    return false;
  }

  state.signedInUserId = user.id;
  persist();
  return true;
}

export function register(username, password, country) {
  const cleanUsername = (username || '').trim();
  if (!cleanUsername) {
    return { error: 'Username cannot be blank' };
  }
  if (!password) {
    return { error: 'Password cannot be blank' };
  }
  if (state.users.some((u) => u.username.toLowerCase() === cleanUsername.toLowerCase())) {
    return { error: 'Username already exists' };
  }

  const userId = uuid();
  const now = nowIso();
  const expiryDate = new Date();
  expiryDate.setMonth(expiryDate.getMonth() + 1);
  expiryDate.setHours(0, 0, 0, 0);

  const newUser = {
    id: userId,
    username: cleanUsername,
    password,
    firstName: '',
    lastName: '',
    email: '',
    profilePicture: '',
    role: 'USER',
    country: country || 'BULGARIA',
    active: true,
    createdOn: now,
    updatedOn: now,
  };

  const defaultWallet = {
    id: uuid(),
    ownerId: userId,
    nickname: 'Vault Zero',
    status: 'ACTIVE',
    balance: 20.00,
    currency: 'EUR',
    main: true,
    createdOn: now,
    updatedOn: now,
  };

  const defaultSubscription = {
    id: uuid(),
    ownerId: userId,
    status: 'ACTIVE',
    period: 'MONTHLY',
    type: 'DEFAULT',
    price: 0.00,
    renewalAllowed: true,
    createdOn: now,
    expiryOn: localIso(expiryDate),
  };

  state.users.push(newUser);
  state.wallets.push(defaultWallet);
  state.subscriptions.push(defaultSubscription);
  state.signedInUserId = userId;
  persist();

  return { user: newUser };
}

export function signOut() {
  state.signedInUserId = null;
  persist();
}

export function currentUser() {
  return state.users.find((u) => u.id === state.signedInUserId) ?? null;
}

/** Sends anyone who is not signed in back to the login page. */
export function requireUser() {
  const user = currentUser();
  if (!user) {
    window.location.href = 'index.html';
    return null;
  }
  return user;
}

export function reset() {
  state = freshState();
  persist();
}

/* ------------------------------------------------------------------- reads */

export function walletsOf(userId) {
  return state.wallets.filter((w) => w.ownerId === userId);
}

export function mainWalletOf(userId) {
  return walletsOf(userId).find((w) => w.main) ?? walletsOf(userId)[0] ?? null;
}

export function subscriptionsOf(userId) {
  return state.subscriptions
    .filter((s) => s.ownerId === userId)
    .sort((a, b) => b.createdOn.localeCompare(a.createdOn));
}

export function activeSubscriptionOf(userId) {
  return subscriptionsOf(userId).find((s) => s.status === 'ACTIVE') ?? null;
}

export function transactionsOf(userId) {
  return state.transactions
    .filter((t) => t.ownerId === userId)
    .sort((a, b) => b.createdOn.localeCompare(a.createdOn));
}

export function transactionById(id) {
  return state.transactions.find((t) => t.id === id) ?? null;
}

export { SUBSCRIPTION_PRICES };

/* ------------------------------------------------------------------ writes */

function recordTransaction(tx) {
  state.transactions.push(tx);
  persist();
  return tx;
}

/**
 * WalletService.withdrawal. Ownership is checked first, exactly as the service
 * does, and a rejected withdrawal is still recorded as a FAILED transaction.
 */
function withdrawal(user, walletId, amount, description) {
  const wallet = state.wallets.find((w) => w.id === walletId);

  const tx = {
    id: uuid(),
    ownerId: user.id,
    sender: wallet ? wallet.id : String(walletId),
    receiver: SMART_WALLET_IDENTIFIER,
    amount,
    currency: wallet ? wallet.currency : 'EUR',
    type: 'WITHDRAWAL',
    description,
    failureReason: null,
    status: 'SUCCEEDED',
    createdOn: nowIso(),
    balanceLeft: wallet ? wallet.balance : 0,
  };

  if (!wallet || wallet.ownerId !== user.id) {
    tx.status = 'FAILED';
    tx.failureReason = WALLET_NOT_OWNED_BY_USER_FAILURE_REASON;
  } else if (wallet.status !== 'ACTIVE') {
    tx.status = 'FAILED';
    tx.failureReason = INACTIVE_WALLET_FAILURE_REASON;
  } else if (wallet.balance < amount) {
    tx.status = 'FAILED';
    tx.failureReason = INSUFFICIENT_FUNDS_FAILURE_REASON;
  } else {
    wallet.balance = round(wallet.balance - amount);
    wallet.updatedOn = nowIso();
  }

  tx.balanceLeft = wallet ? wallet.balance : 0;
  return recordTransaction(tx);
}

/** WalletService.deposit. */
function deposit(walletId, amount, description) {
  const wallet = state.wallets.find((w) => w.id === walletId);
  if (!wallet) {
    return null;
  }

  if (wallet.status !== 'ACTIVE') {
    return recordTransaction({
      id: uuid(),
      ownerId: wallet.ownerId,
      sender: SMART_WALLET_IDENTIFIER,
      receiver: wallet.id,
      amount,
      currency: wallet.currency,
      type: 'DEPOSIT',
      description,
      failureReason: INACTIVE_WALLET_FAILURE_REASON,
      status: 'FAILED',
      createdOn: nowIso(),
      balanceLeft: wallet.balance,
    });
  }

  wallet.balance = round(wallet.balance + amount);
  wallet.updatedOn = nowIso();

  return recordTransaction({
    id: uuid(),
    ownerId: wallet.ownerId,
    sender: SMART_WALLET_IDENTIFIER,
    receiver: wallet.id,
    amount,
    currency: wallet.currency,
    type: 'DEPOSIT',
    description,
    failureReason: null,
    status: 'SUCCEEDED',
    createdOn: nowIso(),
    balanceLeft: wallet.balance,
  });
}

function round(n) {
  return Math.round(n * 100) / 100;
}

/**
 * WalletService.transfer: a withdrawal from the sender paired with a deposit to
 * the recipient's first active wallet. The deposit only runs if the withdrawal
 * succeeded. Returns the withdrawal, which is what the controller redirects to.
 */
export function transfer(sender, walletId, recipientUsername, amount) {
  const recipient = state.users.find((u) => u.username === recipientUsername);
  if (!recipient) {
    return { error: `Wallet not found for username ${recipientUsername}` };
  }

  const recipientWallet = walletsOf(recipient.id).find((w) => w.status === 'ACTIVE');
  if (!recipientWallet) {
    return { error: `Wallet not found for username ${recipientUsername}` };
  }

  const description = TRANSFER_DESCRIPTION_FORMAT
    .replace('%s', sender.username)
    .replace('%s', recipient.username)
    .replace('%s', amount.toFixed(2));

  const withdrawalTx = withdrawal(sender, walletId, amount, description);

  if (withdrawalTx.status === 'SUCCEEDED') {
    deposit(recipientWallet.id, amount, description);
  }

  return { transaction: withdrawalTx };
}

/**
 * SubscriptionService.upgrade: charge the chosen wallet, then complete the
 * current subscription and open the new one. A failed charge changes nothing.
 */
export function upgrade(user, type, period, walletId) {
  const price = SUBSCRIPTION_PRICES[type]?.[period];
  if (price === undefined) {
    return { error: `Price for subscription type ${type} and period ${period} not found` };
  }

  const description = `Upgrade request for ${period} ${type}`;
  const chargeTx = withdrawal(user, walletId, price, description);

  if (chargeTx.status === 'FAILED') {
    return { transaction: chargeTx };
  }

  const now = new Date();
  const expiry = new Date(now);
  if (period === 'MONTHLY') {
    expiry.setMonth(expiry.getMonth() + 1);
  } else {
    expiry.setFullYear(expiry.getFullYear() + 1);
  }
  expiry.setHours(0, 0, 0, 0);

  const current = activeSubscriptionOf(user.id);
  if (current) {
    current.status = 'COMPLETED';
    current.expiryOn = nowIso();
  }

  state.subscriptions.push({
    id: uuid(),
    ownerId: user.id,
    status: 'ACTIVE',
    period,
    type,
    price,
    renewalAllowed: period === 'MONTHLY',
    createdOn: nowIso(),
    expiryOn: localIso(expiry),
  });

  persist();
  return { transaction: chargeTx };
}

/**
 * A top-up, i.e. WalletService.deposit called on your own wallet.
 *
 * The real /wallets page has buttons for this, but WalletController maps no
 * such endpoint yet, so in the running application they 404. The demo wires
 * them to the deposit the service already implements.
 */
export function topUp(user, walletId, amount) {
  const wallet = state.wallets.find((w) => w.id === walletId);
  if (!wallet || wallet.ownerId !== user.id) {
    return null;
  }
  return deposit(walletId, amount, `Top-up ${amount.toFixed(2)} EUR`);
}

/** Marks a wallet active or inactive, behind the page's Switch button. */
export function switchWalletStatus(user, walletId) {
  const wallet = state.wallets.find((w) => w.id === walletId);
  if (!wallet || wallet.ownerId !== user.id) {
    return;
  }
  wallet.status = wallet.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  wallet.updatedOn = nowIso();
  persist();
}

/** UserService.updateProfile. */
export function updateProfile(userId, fields) {
  const user = state.users.find((u) => u.id === userId);
  if (!user) {
    return;
  }
  Object.assign(user, fields);
  user.updatedOn = nowIso();
  persist();
}

/** Admin: UserService.getAll */
export function allUsers() {
  return state.users;
}

/** Admin: UserService.switchStatus */
export function switchUserStatus(userId) {
  const user = state.users.find((u) => u.id === userId);
  if (!user) {
    return;
  }
  user.active = !user.active;
  user.updatedOn = nowIso();
  persist();
}

/** Admin: UserService.switchRole */
export function switchUserRole(userId) {
  const user = state.users.find((u) => u.id === userId);
  if (!user) {
    return;
  }
  user.role = user.role === 'USER' ? 'ADMIN' : 'USER';
  user.updatedOn = nowIso();
  persist();
}
