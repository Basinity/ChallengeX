/* The shared-link view.

   Someone opened a link from a video description or a Discord message, most
   likely on a phone, and wants to know what the challenge is. So this page
   leads with sentences, not ids, and puts the download in reach without a
   scroll. The exact ids and parameters are not shown at all: they are in the
   .json the download button hands over, for whoever actually wants them.

   Everything is decoded from the URL fragment in the browser. No request is
   made, and the challenge never reaches a server. */

window.CX = window.CX || {};

window.CX.share = (function () {
  var ui = window.CX.ui;
  var el = ui.el;
  var entries = window.CX.entries;
  var preset = window.CX.preset;
  var phrase = window.CX.phrase;
  var link = window.CX.link;

  function actionRow(challenge) {
    var filename = preset.slug(challenge.name) + '.json';

    return el('div.share__actions', null, [
      el('button.btn.btn--primary.btn--lift', {
        type: 'button',
        text: '⬇ DOWNLOAD .JSON',
        onclick: function () {
          ui.download(filename, preset.stringify(challenge));
          ui.toast('Downloaded ' + filename);
        }
      }),
      el('a.btn', {
        href: link.urlFor('build.html', challenge),
        text: 'Edit in the builder',
        style: 'border-color:var(--effect);color:var(--effect)'
      }),
      el('button.btn', {
        type: 'button',
        text: '⧉ Copy link',
        onclick: function () {
          ui.copyText(window.location.href).then(function (ok) {
            ui.toast(ok ? 'Link copied' : 'Could not copy: use the address bar');
          });
        }
      })
    ]);
  }

  /* A rule as one readable sentence. The page states the challenge in English
     and nothing else; the ids and parameters behind it live in the .json the
     download button hands over. */
  function ruleLine(rule, index) {
    var line = phrase.ruleLine(rule);
    var body = el('p.line__text');

    if (line.lead) {
      ui.append(body, line.lead + ' ');
    }
    ui.append(body, el('b', { 'data-kind': 'trigger', text: line.trigger }));
    ui.append(body, ' → ');
    ui.append(body, el('b', { 'data-kind': 'effect', text: line.effect }));

    return el('div.line', { id: 'rule-' + (index + 1) }, [body]);
  }

  /* The mode note beside the goal stays muted like a modifier's scope note:
     a versus race reads no louder than the win-together default. */
  function goalBlock(goal) {
    var note = phrase.goalModeNote(goal);
    return el('div.line', null, [
      el('div.line__row', null, [
        el('p.line__text', null, [el('b', { 'data-kind': 'goal', text: phrase.goalLine(goal) })]),
        note ? el('span.line__scope', { text: note }) : null
      ])
    ]);
  }

  /* The modifier's name carries the colour, like the trigger, effect and goal
     names do. The parameter and scope note beside it stays muted whatever the
     scope is, so a specific-player scope reads no louder than "everyone". */
  function modifierBlock(modifier) {
    var line = phrase.modifierLine(modifier);
    var note = [line.detail, line.scope].filter(Boolean).join(' | ');

    return el('div.line', null, [
      el('div.line__row', null, [
        el('p.line__text', null, [el('b', { 'data-kind': 'modifier', text: line.name })]),
        note ? el('span.line__scope', { text: note }) : null
      ])
    ]);
  }

  function howTo(challenge) {
    var name = preset.slug(challenge.name);
    return el('section.howto', null, [
      el('h2.share__section-title', { text: 'RUN IT ON YOUR SERVER OR SINGLEPLAYER' }),
      el('ol', null, [
        el('li', null, [
          'Install the ',
          el('a', { href: link.pageHref('index.html') + '#getmod', text: 'ChallengeX mod' }),
          ' (Fabric, server-side, so vanilla clients can join)'
        ]),
        el('li', null, [
          'Put ', el('code', { text: name + '.json' }),
          ' in ', el('code', { text: 'config/challengex/presets/' })
        ]),
        el('li', null, [
          'Run ', el('code', { text: '/challengex import' }),
          ' and then ', el('code', { text: '/challengex start' })
        ])
      ])
    ]);
  }

  function section(kind, title, children) {
    return el('section.stack.stack--tight', null, [
      el('h2.share__section-title', { 'data-kind': kind, text: title }),
      children
    ]);
  }

  function renderChallenge(challenge, root) {
    document.title = 'ChallengeX | ' + challenge.name;

    var rules = el('div.stack.stack--tight');
    challenge.rules.forEach(function (rule, index) {
      ui.append(rules, ruleLine(rule, index));
    });

    var mods = el('div.stack.stack--tight');
    challenge.modifiers.forEach(function (modifier) {
      ui.append(mods, modifierBlock(modifier));
    });

    var body = [
      el('div.stack', null, [
        el('h1.share__title', { text: challenge.name }),
        actionRow(challenge)
      ])
    ];

    if (challenge.rules.length) {
      body.push(section('rules', 'THE RULES', rules));
    }

    if (challenge.goal || challenge.modifiers.length) {
      body.push(el('div.share__pair', null, [
        challenge.goal ? section('goal', 'THE GOAL', goalBlock(challenge.goal)) : el('div'),
        challenge.modifiers.length ? section('modifiers', 'MODIFIERS', mods) : el('div')
      ]));
    }

    if (!challenge.rules.length && !challenge.goal && !challenge.modifiers.length) {
      body.push(el('p.empty__body', {
        text: 'This challenge is completely empty. Someone shared a blank slate.'
      }));
    }

    body.push(howTo(challenge));

    ui.clear(root);
    ui.append(root, el('main.share.shell.shell--narrow', null, body));

    // On a phone the buttons at the top scroll away, and the download is the
    // whole point of the page, so it also rides along at the bottom.
    ui.append(root, stickyBar(challenge));
    document.body.classList.add('has-actionbar');
  }

  function stickyBar(challenge) {
    var filename = preset.slug(challenge.name) + '.json';
    return el('div.actionbar', null, [
      el('button.btn.btn--primary', {
        type: 'button',
        text: '⬇ DOWNLOAD',
        onclick: function () {
          ui.download(filename, preset.stringify(challenge));
          ui.toast('Downloaded ' + filename);
        }
      }),
      el('a.btn', {
        href: link.urlFor('build.html', challenge),
        text: 'EDIT →',
        style: 'border-color:var(--effect);color:var(--effect)'
      })
    ]);
  }

  function renderFailure(reason, root) {
    document.title = 'ChallengeX | Broken link';
    ui.clear(root);
    ui.append(root, el('main.fail', null, [
      el('div.fail__title', { text: 'THIS LINK IS SCRAMBLED' }),
      el('p.fail__body', {
        text: 'The challenge data in this URL did not decode. It was probably truncated when it '
          + 'was copied. Ask for the link again, or ask for the .json file instead.'
      }),
      el('p.fail__detail', { text: 'error: ' + reason }),
      el('a.btn.btn--primary', { href: link.pageHref('build.html'), text: 'Build one from scratch instead' })
    ]));
  }

  /* Renders whatever the fragment holds. Returns false when there was nothing
     to render, which is the signal to show the landing page instead. */
  function renderFromUrl(root) {
    var encoded = link.readFragment();
    if (!encoded) {
      return false;
    }
    try {
      renderChallenge(link.decode(encoded), root);
    } catch (error) {
      renderFailure(error.message, root);
    }
    return true;
  }

  return {
    renderFromUrl: renderFromUrl,
    renderChallenge: renderChallenge,
    renderFailure: renderFailure
  };
})();
