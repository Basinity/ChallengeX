/* The front page, and the dispatcher that decides what the front page is.

   One URL serves both entry paths the site has. With no fragment it is the
   landing page; with an encoded preset in the fragment it is the shared-link
   view, which is what a link posted in a video description resolves to. Same
   file, same address, no redirect. */

(function () {
  var ui = window.CX.ui;
  var el = ui.el;
  var entries = window.CX.entries;
  var preset = window.CX.preset;
  var link = window.CX.link;

  var SAMPLES = [
    [['when anyone ', null], ['takes damage', 'trigger'], [', everyone gets a ', null],
      ['random effect', 'effect']],
    [['only ', null], ['Basinity keeps inventory', 'modifier'], [', and the goal is ', null],
      ['one diamond', 'goal']],
    [['every ', null], ['five minutes', 'trigger'], [', everyone ', null],
      ['swaps places', 'effect']],
    [['when anyone ', null], ['sleeps', 'trigger'], [', everyone is ', null],
      ['struck by lightning', 'effect']],
    [['you all ', null], ['share one inventory', 'modifier'], [' and ', null],
      ['hearts never regenerate', 'modifier']],
    [['when a ', null], ['mob dies', 'trigger'], [', the killer gets ', null],
      ['two baby zombies', 'effect']],
    [['block drops are ', null], ['randomized', 'modifier'], [', and the goal is the ', null],
      ['ender dragon', 'goal']],
    [['jumping is disabled', 'modifier'], [', and the goal is a ', null],
      ['full beacon', 'goal']],
    [['when anyone ', null], ['breaks a block', 'trigger'], [', they ', null],
      ['teleport somewhere random', 'effect']],
    [['whenever anyone ', null], ['crafts an item', 'trigger'], [', an ', null],
      ['anvil drops on them', 'effect']],
    [['when anyone ', null], ['catches a fish', 'trigger'], [', they get a ', null],
      ['random item', 'effect']],
    [['hostile mobs are buffed', 'modifier'], [', and the goal is to ', null],
      ['kill the warden', 'goal']],
    [['every time anyone ', null], ['gains XP', 'trigger'], [', their ', null],
      ['hotbar shuffles', 'effect']],
    [['shields are disabled', 'modifier'], [', though ', null],
      ['hunger never drains', 'modifier']],
    [['every ', null], ['jump', 'trigger'], [' ', null],
      ['launches you skyward', 'effect']],
    [['entering a ', null], ['new dimension', 'trigger'], [' gives everyone ', null],
      ['nausea', 'effect']],
    [['there is a ', null], ['30-minute clock', 'modifier'], [', and the goal is an ', null],
      ['elytra', 'goal']],
    [['when anyone ', null], ['sneaks', 'trigger'], [', they ', null],
      ['freeze in place', 'effect']]
  ];

  var KIND_COLOR = {
    trigger: 'var(--trigger)',
    effect: 'var(--effect)',
    goal: 'var(--goal)',
    modifier: 'var(--modifier)'
  };

  function sampleCard(segments) {
    var text = el('div.sample__text');
    segments.forEach(function (segment) {
      if (segment[1]) {
        ui.append(text, el('b', { style: 'color:' + KIND_COLOR[segment[1]], text: segment[0] }));
      } else {
        ui.append(text, segment[0]);
      }
    });
    return el('div.sample', null, [
      el('div.sample__kicker', { text: '…BUT' }),
      text
    ]);
  }

  function startSamples(host) {
    var offset = 0;

    function paint() {
      ui.clear(host);
      for (var i = 0; i < 3; i += 1) {
        ui.append(host, sampleCard(SAMPLES[(offset + i) % SAMPLES.length]));
      }
    }

    function advance() {
      offset = (offset + 3) % SAMPLES.length;
      paint();
    }

    paint();

    window.setInterval(function () {
      var cards = host.querySelectorAll('.sample');
      Array.prototype.forEach.call(cards, function (card) {
        card.setAttribute('data-fading', 'true');
      });
      window.setTimeout(advance, 400);
    }, 5000);
  }

  function combinations() {
    return entries.count('trigger')
      * entries.count('effect')
      * (entries.count('goal') + 1)
      * Math.pow(2, entries.count('modifier'));
  }

  function group(value) {
    return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  }

  function renderTally(host) {
    ui.clear(host);
    [
      [entries.count('trigger'), 'TRIGGERS', 'var(--trigger)'],
      [entries.count('effect'), 'EFFECTS', 'var(--effect)'],
      [entries.count('goal'), 'GOALS', 'var(--goal)'],
      [entries.count('modifier'), 'MODIFIERS', 'var(--modifier)'],
      [group(combinations()) + '+', 'WAYS TO PLAY', 'var(--text)']
    ].forEach(function (row) {
      ui.append(host, el('span', null, [
        el('b', { style: 'color:' + row[2], text: row[0] }),
        ' ' + row[1]
      ]));
    });
  }

  /* Deliberately small: one rule, one goal, one modifier. It is the first
     composed challenge most visitors see, so it teaches the whole model at a
     glance rather than showing off every field at once. Every scope is
     every_player, so it needs no roster. */
  function starterChallenge() {
    var challenge = preset.blankChallenge();
    challenge.name = 'Salt in the Wound';

    var rule = preset.blankRule();
    preset.assign(rule.trigger, 'trigger.damage_taken');
    rule.trigger.scope = 'every_player';
    preset.assign(rule.effect, 'effect.random_effect');
    rule.effect.params = { type: 'negative', seconds: '15' };
    rule.effect.scope = 'per_player';
    challenge.rules.push(rule);

    challenge.goal = preset.assign(preset.blankBlock('goal'), 'goal.beat_game');

    var jump = preset.assign(preset.blankBlock('modifier'), 'modifier.disable_jump');
    jump.scope = 'every_player';
    challenge.modifiers.push(jump);

    return challenge;
  }

  function wireDropzone(zone, input) {
    function fail(message) {
      zone.setAttribute('data-bad', 'true');
      zone.textContent = message;
      window.setTimeout(function () {
        zone.removeAttribute('data-bad');
        zone.textContent = 'DROP PRESET JSON, OR CLICK TO PICK A FILE';
      }, 4000);
    }

    function accept(file) {
      if (!file) {
        return;
      }
      var reader = new FileReader();
      reader.onload = function () {
        var challenge;
        try {
          challenge = preset.parse(String(reader.result));
        } catch (error) {
          fail('That file did not read: ' + error.message);
          return;
        }
        window.location.href = link.urlFor('index.html', challenge);
      };
      reader.onerror = function () { fail('That file could not be read.'); };
      reader.readAsText(file);
    }

    zone.addEventListener('click', function () { input.click(); });
    input.addEventListener('change', function () { accept(input.files[0]); });

    ['dragenter', 'dragover'].forEach(function (name) {
      zone.addEventListener(name, function (event) {
        event.preventDefault();
        zone.setAttribute('data-over', 'true');
      });
    });
    ['dragleave', 'drop'].forEach(function (name) {
      zone.addEventListener(name, function (event) {
        event.preventDefault();
        zone.removeAttribute('data-over');
      });
    });
    zone.addEventListener('drop', function (event) {
      accept(event.dataTransfer && event.dataTransfer.files[0]);
    });
  }

  function showLanding() {
    document.getElementById('landing').hidden = false;
    renderTally(document.getElementById('tally'));
    startSamples(document.getElementById('samples'));
    document.getElementById('starter').href = link.urlFor('index.html', starterChallenge());
    wireDropzone(document.getElementById('dropzone'), document.getElementById('dropfile'));
  }

  document.addEventListener('DOMContentLoaded', function () {
    var root = document.getElementById('shared');
    if (window.CX.share.renderFromUrl(root)) {
      document.getElementById('landing').hidden = true;
      return;
    }
    showLanding();
  });

  window.addEventListener('hashchange', function () {
    window.location.reload();
  });
})();
