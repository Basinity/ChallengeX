/* Small shared DOM helpers.

   el() exists because every page builds its content as nodes rather than as
   HTML strings: challenge names, parameter values and player names all arrive
   from a file or a stranger's link, and textContent makes injecting markup
   through them impossible. There is no template engine here on purpose. */

window.CX = window.CX || {};

window.CX.ui = (function () {

  /* el('div.card', { 'data-bad': 'true' }, [child, 'text']) */
  function el(spec, attrs, children) {
    var parts = spec.split('.');
    var node = document.createElement(parts[0] || 'div');
    parts.slice(1).forEach(function (name) { node.classList.add(name); });

    if (attrs) {
      Object.keys(attrs).forEach(function (key) {
        var value = attrs[key];
        if (value === null || value === undefined || value === false) {
          return;
        }
        if (key === 'text') {
          node.textContent = value;
        } else if (key === 'html') {
          throw new Error('ui.el does not set innerHTML: build nodes instead');
        } else if (key.slice(0, 2) === 'on' && typeof value === 'function') {
          node.addEventListener(key.slice(2).toLowerCase(), value);
        } else if (value === true) {
          node.setAttribute(key, '');
        } else {
          node.setAttribute(key, value);
        }
      });
    }

    append(node, children);
    return node;
  }

  function append(node, children) {
    if (children === null || children === undefined || children === false) {
      return node;
    }
    if (Array.isArray(children)) {
      children.forEach(function (child) { append(node, child); });
      return node;
    }
    node.appendChild(children.nodeType ? children : document.createTextNode(String(children)));
    return node;
  }

  function clear(node) {
    while (node.firstChild) {
      node.removeChild(node.firstChild);
    }
    return node;
  }

  /* ---------- feedback ---------- */

  var toastTimer = null;

  function toast(message) {
    var node = document.getElementById('toast');
    if (!node) {
      return;
    }
    node.textContent = message;
    node.hidden = false;
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(function () { node.hidden = true; }, 2600);
  }

  /* ---------- exporting ---------- */

  function download(filename, text) {
    var blob = new Blob([text], { type: 'application/json' });
    var url = URL.createObjectURL(blob);
    var anchor = el('a', { href: url, download: filename });
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    // Revoked on the next frame so the click has certainly been handled.
    window.setTimeout(function () { URL.revokeObjectURL(url); }, 0);
  }

  /* Clipboard access is blocked outside a secure context, so a page opened
     straight off disk falls back to the old selection trick rather than
     silently doing nothing. */
  function copyText(text) {
    if (navigator.clipboard && window.isSecureContext) {
      return navigator.clipboard.writeText(text).then(function () { return true; },
        function () { return legacyCopy(text); });
    }
    return Promise.resolve(legacyCopy(text));
  }

  function legacyCopy(text) {
    var area = el('textarea', { value: text, 'aria-hidden': 'true' });
    area.style.position = 'fixed';
    area.style.opacity = '0';
    document.body.appendChild(area);
    area.select();
    var ok = false;
    try {
      ok = document.execCommand('copy');
    } catch (error) {
      ok = false;
    }
    document.body.removeChild(area);
    return ok;
  }

  /* ---------- misc ---------- */

  /* Marks a card so a problem link is visibly answered when it jumps there. */
  function ping(node) {
    if (!node) {
      return;
    }
    node.scrollIntoView({ behavior: 'smooth', block: 'center' });
    node.setAttribute('data-pinged', 'true');
    window.setTimeout(function () { node.removeAttribute('data-pinged'); }, 1400);
  }

  function plural(count, one, many) {
    return count + ' ' + (count === 1 ? one : (many || one + 's'));
  }

  return {
    el: el,
    append: append,
    clear: clear,
    toast: toast,
    download: download,
    copyText: copyText,
    ping: ping,
    plural: plural
  };
})();
