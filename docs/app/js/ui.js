// Shared chrome. The sidebar is identical on every signed-in page in the real
// templates, so it is rendered from one place here rather than pasted seven
// times, and the icons are the same paths the Thymeleaf pages use.

import { currentUser, signOut, refresh } from './store.js?v=1.0.2';

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** Matches #temporals.format(..., 'dd MMM yyyy HH:mm') in the templates. */
export function formatDateTime(iso) {
  if (!iso) {
    return '';
  }
  const d = new Date(iso);
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(d.getDate())} ${MONTHS[d.getMonth()]} ${d.getFullYear()} `
       + `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function money(amount) {
  return Number(amount).toFixed(2);
}

const ICONS = {
  dashboard: '<path stroke="currentColor" stroke-linecap="round" stroke-width="2" d="M5 7h14M5 12h14M5 17h14"/>',
  upgrade: '<path fill-rule="evenodd" d="M12 2c-.791 0-1.55.314-2.11.874l-.893.893a.985.985 0 0 1-.696.288H7.04A2.984 2.984 0 0 0 4.055 7.04v1.262a.986.986 0 0 1-.288.696l-.893.893a2.984 2.984 0 0 0 0 4.22l.893.893a.985.985 0 0 1 .288.696v1.262a2.984 2.984 0 0 0 2.984 2.984h1.262c.261 0 .512.104.696.288l.893.893a2.984 2.984 0 0 0 4.22 0l.893-.893a.985.985 0 0 1 .696-.288h1.262a2.984 2.984 0 0 0 2.984-2.984V15.7c0-.261.104-.512.288-.696l.893-.893a2.984 2.984 0 0 0 0-4.22l-.893-.893a.985.985 0 0 1-.288-.696V7.04a2.984 2.984 0 0 0-2.984-2.984h-1.262a.985.985 0 0 1-.696-.288l-.893-.893A2.984 2.984 0 0 0 12 2Zm3.683 7.73a1 1 0 1 0-1.414-1.413l-4.253 4.253-1.277-1.277a1 1 0 0 0-1.415 1.414l1.985 1.984a1 1 0 0 0 1.414 0l4.96-4.96Z" clip-rule="evenodd" fill="currentColor"/>',
  transfers: '<path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m16 10 3-3m0 0-3-3m3 3H5v3m3 4-3 3m0 0 3 3m-3-3h14v-3"/>',
  wallets: '<path fill-rule="evenodd" d="M12 14a3 3 0 0 1 3-3h4a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2h-4a3 3 0 0 1-3-3Zm3-1a1 1 0 1 0 0 2h4v-2h-4Z" clip-rule="evenodd" fill="currentColor"/><path fill-rule="evenodd" d="M12.293 3.293a1 1 0 0 1 1.414 0L16.414 6h-2.828l-1.293-1.293a1 1 0 0 1 0-1.414ZM12.414 6 9.707 3.293a1 1 0 0 0-1.414 0L5.586 6h6.828ZM4.586 7l-.056.055A2 2 0 0 0 3 9v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2h-4a5 5 0 0 1 0-10h4a2 2 0 0 0-1.53-1.945L17.414 7H4.586Z" clip-rule="evenodd" fill="currentColor"/>',
  transactions: '<path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 20V7m0 13-4-4m4 4 4-4m4-12v13m0-13 4 4m-4-4-4 4"/>',
  dots: '<path stroke="currentColor" stroke-linecap="round" stroke-width="2" d="M12 6h.01M12 12h.01M12 18h.01"/>',
  logout: '<path stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H8m12 0-4 4m4-4-4-4M9 4H7a3 3 0 0 0-3 3v10a3 3 0 0 0 3 3h2"/>',
};

function icon(name) {
  return `<svg class="w-6 h-6 text-gray-800 dark:text-white" aria-hidden="true" `
       + `xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" `
       + `viewBox="0 0 24 24">${ICONS[name]}</svg>`;
}

function link(href, label, iconName, active, extraClass = '') {
  const cls = [extraClass, active ? 'active' : ''].filter(Boolean).join(' ');
  return `<a href="${href}"${cls ? ` class="${cls}"` : ''}>`
       + `<p>${label}</p>${icon(iconName)}</a>`;
}

/** Renders the sidebar into <div class="nav-bar">, marking `active` as current. */
export function renderSidebar(active) {
  const host = document.querySelector('.nav-bar');
  if (!host) {
    return;
  }
  const user = currentUser();
  host.innerHTML = `
    <div class="side-bar-container">
      <nav>
        <div class="side_navbar">
          <div class="smart-wallet-nav-bar">
            <div class="smart-wallet-nav-bar-name">Smart Wallet</div>
            <div class="smart-wallet-nav-bar-version">v1.0.0</div>
          </div>
          <div class="func-block">
            ${link('home.html', 'Dashboard', 'dashboard', active === 'home')}
            ${link('subscriptions.html', 'Upgrade', 'upgrade', active === 'subscriptions', 'special-gold')}
          </div>
          <div class="func-block">
            <span>Quick Link</span>
            ${link('transfers.html', 'Transfers', 'transfers', active === 'transfers')}
            ${link('wallets.html', 'Wallets', 'wallets', active === 'wallets')}
            ${link('transactions.html', 'Transactions', 'transactions', active === 'transactions')}
          </div>
          <div class="func-block">
            <span>Utility</span>
            ${link('subscription-history.html', 'Subscription History', 'dots', active === 'history')}
            ${link('notifications.html', 'Notifications', 'dots', active === 'notifications')}
          </div>
          ${user && user.role === 'ADMIN' ? `
          <div class="func-block admin-func-block">
            <span>Admin</span>
            ${link('users.html', 'Users', 'dots', active === 'users')}
          </div>` : ''}
          <div class="func-block">
            <a class="logout" href="#" data-logout><p>Logout</p>${icon('logout')}</a>
          </div>
        </div>
      </nav>
    </div>`;

  host.querySelector('[data-logout]').addEventListener('click', (e) => {
    e.preventDefault();
    signOut();
    window.location.href = 'index.html?logout=true';
  });
}

/** Footer text follows fragments/footer.html, which differs for an admin. */
export function renderFooter() {
  const footer = document.querySelector('.footer');
  if (!footer) {
    return;
  }
  const user = currentUser();
  const which = user && user.role === 'ADMIN' ? 'Admin Footer' : 'Normal Footer';
  footer.innerHTML = `<p>&copy; 2025 Smart Wallet. All rights reserved. ${which}</p>`;
}

/** Draws the chrome every signed-in page shares. */
export function initPage(active) {
  renderSidebar(active);
  renderFooter();

  // A page restored from the back/forward cache keeps the state it was frozen
  // with, so an admin who has just changed a role would come back to a sidebar
  // and a table drawn from the old registry. Redraw from what is stored now.
  window.addEventListener('pageshow', (event) => {
    if (event.persisted) {
      refresh();
    }
  });
}
