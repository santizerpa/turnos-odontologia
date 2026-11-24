import { mountPublicBooking } from './public-reservation/ui.js';

const app = document.querySelector<HTMLDivElement>('#app');

if (app) {
  mountPublicBooking(app);
}
