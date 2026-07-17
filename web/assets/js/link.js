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

  function urlFor(page, challenge) {
    var base = new URL(page, window.location.href);
    base.hash = KEY + '=' + encode(challenge);
    return base.toString();
  }

  return {
    key: KEY,
    encode: encode,
    decode: decode,
    readFragment: readFragment,
    urlFor: urlFor
  };
})();
