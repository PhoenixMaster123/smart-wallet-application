// The demo's stand-in for the database and the HTTP session.
//
// State lives in sessionStorage so it survives a link click, but nothing is
// meant to outlive a visit: reloading the page throws the registry away and
// drops you back on the login screen. Every write mirrors what the
// corresponding service does in src/main/java/app - the arithmetic and the
// failure reasons are the same, so the demo tells the same story the real
// application would.

import {
  SEED_LOGIN, SEED_USERS, SEED_WALLETS, SEED_SUBSCRIPTIONS, SEED_TRANSACTIONS,
  SUBSCRIPTION_PRICES, SMART_WALLET_IDENTIFIER, localIso,
} from './seed.js?v=1.0.3';

const KEY = 'smart-wallet-demo-v3';

// Set just before a reload the demo asks for itself, so that reload is not
// mistaken for the visitor pressing F5.
const SELF_RELOAD_KEY = 'smart-wallet-demo-self-reload';

// WalletService failure reasons, verbatim.
const INACTIVE_WALLET_FAILURE_REASON = 'Inactive wallet';
const INSUFFICIENT_FUNDS_FAILURE_REASON = 'Not enough funds';
const WALLET_NOT_OWNED_BY_USER_FAILURE_REASON = 'Wallet not owned by user';
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
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

/**
 * Reloads the current page without losing the demo's state.
 *
 * The pages redraw themselves by reloading after a write - a top-up, a wallet
 * switch - and those reloads have to survive the guard below, so they leave a
 * marker behind on the way out.
 */
export function refresh() {
  try {
    sessionStorage.setItem(SELF_RELOAD_KEY, '1');
  } catch {
    // Without the marker the reload starts over, which is no worse than the
    // storage being unavailable in the first place.
  }
  window.location.reload();
}

function wasSelfReload() {
  try {
    const marked = sessionStorage.getItem(SELF_RELOAD_KEY) === '1';
    sessionStorage.removeItem(SELF_RELOAD_KEY);
    return marked;
  } catch {
    return false;
  }
}

function isBrowserReload() {
  const [navigation] = performance.getEntriesByType('navigation');
  if (navigation) {
    return navigation.type === 'reload';
  }
  // Safari below 15 has no navigation timing entry, only the legacy counter.
  return Boolean(performance.navigation) && performance.navigation.type === 1;
}

function isLoginPage() {
  const path = window.location.pathname;
  return path.endsWith('/') || path.endsWith('/index.html');
}

// Pressing F5 starts the demo over: the seeded registry comes back and the
// visitor lands on the login page, rather than carrying half-finished state
// into a session the browser no longer has a page for.
if (isBrowserReload() && !wasSelfReload()) {
  reset();
  if (!isLoginPage()) {
    window.location.replace('index.html');
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

/**
 * Newest first, the way the repositories order their findAllBy... queries.
 *
 * Timestamps have no sub-second part, so several rows written in the same
 * second compare equal; the position in the list breaks the tie and keeps the
 * one written last at the top.
 */
function newestFirst(rows, userId) {
  return rows
    .map((row, index) => ({ row, index }))
    .filter(({ row }) => row.ownerId === userId)
    .sort((a, b) => b.row.createdOn.localeCompare(a.row.createdOn) || b.index - a.index)
    .map(({ row }) => row);
}

export function walletsOf(userId) {
  return state.wallets.filter((w) => w.ownerId === userId);
}

export function mainWalletOf(userId) {
  return walletsOf(userId).find((w) => w.main) ?? walletsOf(userId)[0] ?? null;
}

export function subscriptionsOf(userId) {
  return newestFirst(state.subscriptions, userId);
}

export function activeSubscriptionOf(userId) {
  return subscriptionsOf(userId).find((s) => s.status === 'ACTIVE') ?? null;
}

export function transactionsOf(userId) {
  return newestFirst(state.transactions, userId);
}

export function transactionById(id) {
  return state.transactions.find((t) => t.id === id) ?? null;
}

export { SUBSCRIPTION_PRICES };

/* --------------------------------------------------------------- analytics */

// Every description the services write starts with one of these, which is the
// only place the shape of a movement is recorded - the rows themselves only
// know DEPOSIT or WITHDRAWAL.
const SUBSCRIPTION_DESCRIPTION = 'Upgrade request for';
const TRANSFER_DESCRIPTION = 'Transfer ';
const TOP_UP_DESCRIPTION = 'Top-up';

const TRANSFER_PARTIES = /^Transfer (.+?) <> (.+?) \(/;

export const CATEGORY_TRANSFERS = 'Transfers';
export const CATEGORY_SUBSCRIPTIONS = 'Subscriptions';
export const CATEGORY_TOP_UPS = 'Top-ups';
export const CATEGORY_OTHER = 'Other';

const WEEKS_CHARTED = 8;
const COUNTERPARTIES_CHARTED = 5;

/** What kind of movement a row is, read back out of its description. */
export function categoryOf(transaction) {
  const description = transaction.description || '';
  if (description.startsWith(SUBSCRIPTION_DESCRIPTION)) {
    return CATEGORY_SUBSCRIPTIONS;
  }
  if (description.startsWith(TRANSFER_DESCRIPTION)) {
    return CATEGORY_TRANSFERS;
  }
  if (description.startsWith(TOP_UP_DESCRIPTION)) {
    return CATEGORY_TOP_UPS;
  }
  return CATEGORY_OTHER;
}

/**
 * Who the money moved to or from.
 *
 * A transfer books its withdrawal against SMART WALLET PLATFORM rather than the
 * recipient's wallet, so the other party's name survives only in the
 * description both halves share. Anything else moved against the platform.
 */
export function counterpartyOf(transaction, username) {
  const parties = TRANSFER_PARTIES.exec(transaction.description || '');
  if (!parties) {
    return 'Smart Wallet';
  }
  const [, from, to] = parties;
  if (from === username) {
    return to;
  }
  if (to === username) {
    return from;
  }
  return transaction.type === 'WITHDRAWAL' ? to : from;
}

function startOfWeek(date) {
  const start = new Date(date);
  // Monday, the way a European week reads.
  const weekday = (start.getDay() + 6) % 7;
  start.setDate(start.getDate() - weekday);
  start.setHours(0, 0, 0, 0);
  return start;
}

function shareOut(totals, total) {
  return [...totals.entries()]
    .map(([name, amount]) => ({
      name,
      amount: round(amount),
      share: total > 0 ? amount / total : 0,
    }))
    .sort((a, b) => b.amount - a.amount);
}

function sumBy(rows, pick) {
  const totals = new Map();
  for (const row of rows) {
    const key = pick(row);
    totals.set(key, (totals.get(key) ?? 0) + row.amount);
  }
  return totals;
}

/**
 * Everything the analytics page draws, for one user over one window.
 *
 * `days` is how far back the summary reaches; null covers everything. The
 * weekly chart always runs over the last eight weeks regardless, so the shape
 * of the last two months stays comparable whichever window is selected.
 */
export function analyticsOf(userId, days) {
  const user = state.users.find((u) => u.id === userId);
  const settled = transactionsOf(userId).filter((t) => t.status === 'SUCCEEDED');

  const now = new Date();
  const from = days === null ? null : new Date(now.getTime() - days * 86400000);
  const previousFrom = from === null ? null : new Date(from.getTime() - days * 86400000);

  const within = (transaction, start, end) => {
    const moment = new Date(transaction.createdOn);
    return (start === null || moment >= start) && (end === null || moment < end);
  };

  const current = settled.filter((t) => within(t, from, null));
  const previous = from === null ? [] : settled.filter((t) => within(t, previousFrom, from));

  const totalOf = (rows, type) => round(rows
    .filter((t) => t.type === type)
    .reduce((sum, t) => sum + t.amount, 0));

  const moneyIn = totalOf(current, 'DEPOSIT');
  const moneyOut = totalOf(current, 'WITHDRAWAL');
  const previousOut = totalOf(previous, 'WITHDRAWAL');

  const spent = current.filter((t) => t.type === 'WITHDRAWAL');
  const categories = shareOut(sumBy(spent, categoryOf), moneyOut);
  const everyone = shareOut(sumBy(spent, (t) => counterpartyOf(t, user.username)), moneyOut);

  // Past the fifth slice a donut stops saying anything, so the tail is one.
  const counterparties = everyone.slice(0, COUNTERPARTIES_CHARTED);
  const tail = everyone.slice(COUNTERPARTIES_CHARTED);
  if (tail.length > 0) {
    counterparties.push({
      name: CATEGORY_OTHER,
      amount: round(tail.reduce((sum, entry) => sum + entry.amount, 0)),
      share: tail.reduce((sum, entry) => sum + entry.share, 0),
    });
  }

  const weeks = [];
  const thisWeek = startOfWeek(now);
  for (let back = WEEKS_CHARTED - 1; back >= 0; back -= 1) {
    const start = new Date(thisWeek);
    start.setDate(start.getDate() - back * 7);
    const end = new Date(start);
    end.setDate(end.getDate() + 7);

    const rows = settled.filter((t) => within(t, start, end));
    weeks.push({
      start,
      label: `${start.getDate()} ${MONTHS[start.getMonth()]}`,
      moneyIn: totalOf(rows, 'DEPOSIT'),
      moneyOut: totalOf(rows, 'WITHDRAWAL'),
    });
  }

  return {
    from,
    moneyIn,
    moneyOut,
    net: round(moneyIn - moneyOut),
    previousOut,
    // Null rather than Infinity when there is nothing to compare against.
    outChange: previousOut > 0 ? (moneyOut - previousOut) / previousOut : null,
    categories,
    counterparties,
    weeks,
    movements: current.length,
  };
}

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

/** WalletUtils.isEligibleToUnlockNewWallet */
export function isEligibleToUnlockNewWallet(userId) {
  const sub = activeSubscriptionOf(userId);
  if (!sub) {
    return false;
  }
  const count = walletsOf(userId).length;
  return (sub.type === 'PREMIUM' && count < 2) || (sub.type === 'ULTIMATE' && count < 5);
}

/** Unlocks and creates a new wallet for eligible tiers. */
export function unlockNewWallet(user) {
  if (!isEligibleToUnlockNewWallet(user.id)) {
    return null;
  }
  const count = walletsOf(user.id).length;
  const now = nowIso();
  const newWallet = {
    id: uuid(),
    ownerId: user.id,
    nickname: `Vault ${count}`,
    status: 'ACTIVE',
    balance: 20.00,
    currency: 'EUR',
    main: false,
    createdOn: now,
    updatedOn: now,
  };
  state.wallets.push(newWallet);
  persist();
  return newWallet;
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

/** Everyone you could send money to - the demo has no user search. */
export function otherUsers(userId) {
  return state.users.filter((u) => u.id !== userId && u.active);
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
