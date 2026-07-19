/* The light/dark switch.

   The site follows the system colour scheme until the visitor presses the
   topbar switch, which forces a side by stamping data-theme on the root
   element and remembering the choice in localStorage. A small inline script
   in each page's head re-applies the stored choice before first paint, so a
   forced theme never flashes the other one; this script only wires the
   button. The stored value is the single thing the site keeps in the browser
   between visits, which the privacy page discloses. */

window.CX = window.CX || {};

window.CX.theme = (function () {
  var KEY = 'challengex-theme';

  function effective() {
    var forced = document.documentElement.getAttribute('data-theme');
    if (forced === 'light' || forced === 'dark') {
      return forced;
    }
    return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  }

  function flip() {
    var next = effective() === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', next);
    try {
      window.localStorage.setItem(KEY, next);
    } catch (ignored) {
      /* Storage can be blocked; the flip still applies for this page view. */
    }
  }

  var button = document.getElementById('theme-toggle');
  if (button) {
    button.addEventListener('click', flip);
  }

  return {
    key: KEY,
    effective: effective,
    flip: flip
  };
})();
