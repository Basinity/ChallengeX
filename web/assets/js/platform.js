/* Which platforms run a challenge, read off the generated support file.

   The rule the whole feature follows: this only ever tells you something. A
   challenge using a piece one platform lacks still exports, still shares, and
   still downloads, the same as a doubled modifier or an unwinnable challenge
   does. Structural validation is the only thing that stops an export. */

window.CX = window.CX || {};

window.CX.platform = (function () {
  var data = window.CX_SUPPORT || { platforms: [], unsupported: {} };

  /* The default: judge a challenge against every platform, so the builder
     tells you when something will not travel before you share it. */
  var BOTH = 'both';
  var STORAGE_KEY = 'cx-platform';

  var names = { fabric: 'Fabric', paper: 'Paper' };

  function displayName(platform) {
    return names[platform] || platform.charAt(0).toUpperCase() + platform.slice(1);
  }

  function stored() {
    try {
      var saved = window.localStorage.getItem(STORAGE_KEY);
      if (saved === BOTH || (data.platforms || []).indexOf(saved) >= 0) {
        return saved;
      }
    } catch (blocked) {
      /* Private browsing, or storage turned off. The default is fine. */
    }
    return BOTH;
  }

  var chosen = stored();

  /* The platforms a choice is judged against: one, or all of them. */
  function judgedAgainst() {
    return chosen === BOTH ? (data.platforms || []) : [chosen];
  }

  /* The platforms that cannot run this id, out of the ones being judged. */
  function missingFrom(id) {
    return judgedAgainst().filter(function (platform) {
      return (data.unsupported[platform] || []).indexOf(id) >= 0;
    });
  }

  /* Every distinct catalog id a challenge uses, both halves of every rule. */
  function idsOf(challenge) {
    var ids = [];
    (challenge.rules || []).forEach(function (rule) {
      if (rule.trigger) { ids.push(rule.trigger.id); }
      if (rule.effect) { ids.push(rule.effect.id); }
    });
    (challenge.modifiers || []).forEach(function (modifier) { ids.push(modifier.id); });
    return ids.filter(function (id, index) { return ids.indexOf(id) === index; });
  }

  return {
    BOTH: BOTH,

    /* Every platform the support file knows, for the builder's control. */
    all: function () { return (data.platforms || []).slice(); },

    displayName: displayName,

    chosen: function () { return chosen; },

    choose: function (platform) {
      chosen = platform;
      try {
        window.localStorage.setItem(STORAGE_KEY, platform);
      } catch (blocked) {
        /* Not being able to remember it is not worth failing over. */
      }
    },

    /* Whether every judged platform runs this id. */
    runs: function (id) { return missingFrom(id).length === 0; },

    /* "Not on Paper", or null when everything judged runs it. */
    noteFor: function (id) {
      var missing = missingFrom(id);
      if (missing.length === 0) {
        return null;
      }
      return 'Not on ' + missing.map(displayName).join(' or ');
    },

    /* Every id in a challenge some judged platform cannot run. */
    gapIn: function (challenge) {
      return idsOf(challenge).filter(function (id) { return missingFrom(id).length > 0; });
    },

    /* Every id in a challenge one named platform cannot run, whatever is selected. */
    gapOn: function (challenge, platform) {
      var missing = data.unsupported[platform] || [];
      return idsOf(challenge).filter(function (id) { return missing.indexOf(id) >= 0; });
    },

    /* Which platforms cannot run this whole challenge, whatever is selected. */
    missingPlatformsFor: function (challenge) {
      var ids = idsOf(challenge);
      return (data.platforms || []).filter(function (platform) {
        var missing = data.unsupported[platform] || [];
        return ids.some(function (id) { return missing.indexOf(id) >= 0; });
      });
    },

    /* The platforms that run all of it. */
    runningPlatformsFor: function (challenge) {
      var ids = idsOf(challenge);
      return (data.platforms || []).filter(function (platform) {
        var missing = data.unsupported[platform] || [];
        return !ids.some(function (id) { return missing.indexOf(id) >= 0; });
      });
    }
  };
})();
