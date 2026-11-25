import { createAdminApi } from './api.js';
import { mountAdminConsole, type AdminController } from './ui.js';

let controller: AdminController | null = null;
const api = createAdminApi({
  onSessionExpired: (status, body) => controller?.handleSessionExpired(status, body)
});
const root = document.querySelector<HTMLElement>('#app');

if (root) {
  controller = mountAdminConsole(root, api);
}
