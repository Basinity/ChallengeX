/* The shareable link: a whole preset carried in the URL fragment.

   The fragment is used rather than a query string because a fragment is never
   sent to a server. Nothing about a challenge leaves the browser that opened
   it, which is what lets the site stay a pile of static files.

   Encoding is base64url over the UTF-8 bytes of the same JSON the download
   produces, so a link and a downloaded file always describe the same preset. */

window.CX = window.CX || {};

window.CX.link = (function () {
  var KEY = 'c';

  function toBase64Url(text) {
    var bytes = new TextEncoder().encode(text);
    var binary = '';
    for (var i = 0; i < bytes.length; i += 0x8000) {
      binary += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
    }
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }

  function fromBase64Url(encoded) {
    var padded = encoded.replace(/-/g, '+').replace(/_/g, '/');
    while (padded.length % 4) {
      padded += '=';
    }
    var binary = atob(padded);
    var bytes = new Uint8Array(binary.length);
    for (var i = 0; i < binary.length; i += 1) {
      bytes[i] = binary.charCodeAt(i);
    }
    return new TextDecoder().decode(bytes);
  }

  function readFragment() {
    var hash = window.location.hash.replace(/^#/, '');
    if (!hash) {
      return null;
    }
    var params = new URLSearchParams(hash);
    return params.get(KEY);
  }

  function decode(encoded) {
    var json;
    try {
      json = fromBase64Url(encoded);
    } catch (error) {
      throw new Error('the link data is not valid base64, so it was probably truncated when copied');
    }
    return window.CX.preset.parse(json);
  }

  function encode(challenge) {
    return toBase64Url(window.CX.preset.stringify(challenge));
  }

  /* Where a link to a page should point, from wherever this script runs. The
     pages keep their old logical names ('index.html' the landing page,
     'build.html' the builder) but both are directory indexes when served over
     http(s), so links read "challengex.basinity.com/" and
     "challengex.basinity.com/build/" with no filename in sight; opened off
     disk there is no directory index, so the filenames stay. The builder
     lives one level down, which is what the '../' cases undo. */
  var IN_BUILDER = /\/build(\/(index\.html)?)?$/.test(window.location.pathname || '');

  function pageHref(page) {
    var served = window.location.protocol !== 'file:';
    if (page === 'index.html') {
      if (served) {
        return IN_BUILDER ? '../' : './';
      }
      return IN_BUILDER ? '../index.html' : 'index.html';
    }
    if (page === 'build.html') {
      if (served) {
        return IN_BUILDER ? './' : 'build/';
      }
      return IN_BUILDER ? 'index.html' : 'build/index.html';
    }
    return page;
  }

  function urlFor(page, challenge) {
    var base = new URL(pageHref(page), window.location.href);
    base.hash = KEY + '=' + encode(challenge);
    return base.toString();
  }

  /* The static anchors in the two pages' own markup carry real filenames so a
     copy opened off disk still navigates; served, they are rewritten once here
     (this script loads after them, at the end of the body). Longer prefixes
     first, so '../index.html' is never half-matched. */
  if (typeof document !== 'undefined' && document.querySelectorAll) {
    var STATIC_PREFIXES = [
      ['build/index.html', 'build.html'],
      ['../index.html', 'index.html'],
      ['index.html', 'index.html']
    ];
    Array.prototype.forEach.call(document.querySelectorAll('a[href]'), function (anchor) {
      var href = anchor.getAttribute('href');
      for (var i = 0; i < STATIC_PREFIXES.length; i += 1) {
        if (href.indexOf(STATIC_PREFIXES[i][0]) === 0) {
          anchor.setAttribute('href', pageHref(STATIC_PREFIXES[i][1]) + href.slice(STATIC_PREFIXES[i][0].length));
          break;
        }
      }
    });
  }

  return {
    key: KEY,
    encode: encode,
    decode: decode,
    readFragment: readFragment,
    pageHref: pageHref,
    urlFor: urlFor
  };
})();
