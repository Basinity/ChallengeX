/* The builder.

   Every card on this page is rendered generically from catalog data. There is
   no per-entry code anywhere: a rule card knows about "a trigger and an
   effect", a parameter form knows about "a name, a type and whether it is
   required", and the scope control knows which values its side allows. Adding
   a catalog entry to the mod puts it on this page with no change here.

   Structural edits (picking an entry, adding, deleting, changing a scope)
   re-render the board. Typing in a parameter does not: it updates the model
   and refreshes only the summary rail, so an input never loses focus
   mid-keystroke. */

(function () {
  var ui = window.CX.ui;
  var el = ui.el;
  var entries = window.CX.entries;
  var preset = window.CX.preset;
  var phrase = window.CX.phrase;
  var link = window.CX.link;
  var suggest = window.CX.suggest;

  var challenge = preset.blankChallenge();

  var view = {
    tab: 'rules',
    expanded: null,
    picker: null
  };

  var dom = {};

  /* ---------- model helpers ---------- */

  function ruleAt(uid) {
    return challenge.rules.filter(function (rule) { return rule.uid === uid; })[0] || null;
  }

  function isEmptyChallenge() {
    return !challenge.rules.length && !challenge.goal && !challenge.modifiers.length;
  }

  function problemsByUid() {
    var map = {};
    preset.problems(challenge).forEach(function (item) {
      (map[item.uid] = map[item.uid] || []).push(item);
    });
    return map;
  }

  /* ---------- roster ---------- */

  function addPlayer(name) {
    var trimmed = (name || '').trim();
    if (!trimmed || challenge.players.indexOf(trimmed) >= 0) {
      return false;
    }
    challenge.players.push(trimmed);
    return true;
  }

  /* Dropping a player also drops them from every scope that named them, so a
     scope can never point at somebody the roster no longer knows. */
  function removePlayer(name) {
    challenge.players = challenge.players.filter(function (player) { return player !== name; });
    eachBlock(function (block) {
      if (Array.isArray(block.scope)) {
        block.scope = block.scope.filter(function (player) { return player !== name; });
      }
    });
  }

  function eachBlock(visit) {
    challenge.rules.forEach(function (rule) {
      visit(rule.trigger);
      visit(rule.effect);
    });
    if (challenge.goal) {
      visit(challenge.goal);
    }
    challenge.modifiers.forEach(visit);
  }

  function renderRoster() {
    var bar = ui.clear(dom.roster);
    ui.append(bar, el('span.meta', { text: 'PLAYERS' }));

    challenge.players.forEach(function (name) {
      ui.append(bar, el('span.chip', null, [
        name,
        el('button.chip__drop', {
          type: 'button',
          title: 'Remove ' + name,
          'aria-label': 'Remove ' + name,
          text: '✕',
          onclick: function () { removePlayer(name); render(); }
        })
      ]));
    });

    var input = el('input.input.input--chip', {
      type: 'text',
      placeholder: '+ add player name',
      'aria-label': 'Add a player to the roster'
    });
    input.addEventListener('keydown', function (event) {
      if (event.key === 'Enter') {
        event.preventDefault();
        if (addPlayer(input.value)) {
          render();
          var next = dom.roster.querySelector('.input--chip');
          if (next) {
            next.focus();
          }
        }
      }
    });
    input.addEventListener('blur', function () {
      if (addPlayer(input.value)) {
        render();
      }
    });
    ui.append(bar, input);

    ui.append(bar, el('span.roster-bar__hint', null, [
      'Optional. Only needed when a trigger, effect or modifier targets ',
      el('b', { text: 'specific players' }),
      '.'
    ]));
  }

  /* ---------- the generic parameter form ---------- */

  function paramField(block, entry, param) {
    var label = el('span.field__label', { 'data-kind': entry.kind }, [
      param.name.replace(/_/g, ' '),
      param.required ? el('b.field__req', { text: ' *' }) : null
    ]);

    if (param.type === 'BOOL') {
      var checked = preset.rawValue(block, param) === true;
      var toggle = el('span.toggle', {
        role: 'checkbox',
        tabindex: '0',
        'aria-checked': String(checked)
      }, [checked ? '✓ true' : '✕ false']);
      function flip() {
        block.params[param.name] = !(block.params[param.name] === true);
        render();
      }
      toggle.addEventListener('click', flip);
      toggle.addEventListener('keydown', function (event) {
        if (event.key === ' ' || event.key === 'Enter') {
          event.preventDefault();
          flip();
        }
      });
      return el('label.field', null, [label, toggle]);
    }

    if (param.type === 'STRING' && param.suggests) {
      return el('div.field', null, [label, suggestField(block, entry, param)]);
    }

    var numeric = param.type === 'INT' || param.type === 'DECIMAL';
    var input = el('input.input', {
      type: numeric ? 'number' : 'text',
      step: param.type === 'DECIMAL' ? 'any' : (param.type === 'INT' ? '1' : null),
      inputmode: numeric ? 'decimal' : null,
      min: param.min != null ? String(param.min) : null,
      max: param.max != null ? String(param.max) : null,
      value: preset.rawValue(block, param),
      placeholder: placeholderFor(entry, param),
      'aria-label': entry.name + ' ' + param.name
    });
    input.addEventListener('input', function () {
      block.params[param.name] = input.value;
      refreshRail();
      markField(input, block, param);
    });
    // The mod clamps these same bounds at runtime; clamp on commit so the
    // preset carries a value the mod will honor rather than one it silently
    // pulls into range. On 'change' rather than 'input' so a value mid-type
    // ("2" on the way to "25") is not snapped up while the field still has focus.
    input.addEventListener('change', function () {
      var clamped = clampToBounds(param, input.value);
      if (clamped !== input.value) {
        input.value = clamped;
        block.params[param.name] = clamped;
        refreshRail();
        markField(input, block, param);
      }
    });
    markField(input, block, param);
    return el('label.field', null, [label, input]);
  }

  /* A parameter whose value is a game id renders as a type-ahead: the field
     shows display names ("Diamond Sword"), the model keeps the id the mod
     matches on (minecraft:diamond_sword). Nothing is ever forced: text that
     matches no suggestion is kept exactly as typed, so a modded or future id
     stays as exportable as it always was. The "player" source draws from the
     challenge roster instead of the game data. */
  function suggestField(block, entry, param) {
    var stored = preset.rawValue(block, param);
    var shown = param.suggests === 'player' ? null : suggest.displayName(param.suggests, stored);
    var input = el('input.input', {
      type: 'text',
      role: 'combobox',
      autocomplete: 'off',
      spellcheck: 'false',
      'aria-expanded': 'false',
      'aria-autocomplete': 'list',
      value: shown === null ? stored : shown,
      placeholder: placeholderFor(entry, param),
      'aria-label': entry.name + ' ' + param.name
    });
    var list = el('div.suggest__list', { hidden: true });
    var box = el('div.suggest', null, [input, list]);
    var options = [];
    var active = -1;

    function optionsFor(query) {
      if (param.suggests === 'player') {
        var needle = (query || '').trim().toLowerCase();
        return challenge.players.filter(function (name) {
          return !needle || name.toLowerCase().indexOf(needle) >= 0;
        }).map(function (name) { return { id: name, name: name }; });
      }
      return suggest.search(param.suggests, query, 8);
    }

    function close() {
      list.hidden = true;
      ui.clear(list);
      options = [];
      active = -1;
      input.setAttribute('aria-expanded', 'false');
    }

    function pick(option) {
      block.params[param.name] = option.id;
      input.value = option.name;
      close();
      refreshRail();
      markField(input, block, param);
    }

    function open(query) {
      options = optionsFor(query);
      ui.clear(list);
      active = -1;
      if (!options.length) {
        close();
        return;
      }
      options.forEach(function (option) {
        var row = el('button.suggest__option', { type: 'button', tabindex: '-1', text: option.name });
        // mousedown, not click: it runs before the input's blur tears the list down.
        row.addEventListener('mousedown', function (event) {
          event.preventDefault();
          pick(option);
        });
        ui.append(list, row);
      });
      list.hidden = false;
      input.setAttribute('aria-expanded', 'true');
    }

    function highlight(step) {
      if (list.hidden || !options.length) {
        return;
      }
      active = (active + step + options.length) % options.length;
      for (var i = 0; i < list.children.length; i++) {
        if (i === active) {
          list.children[i].setAttribute('data-active', 'true');
        } else {
          list.children[i].removeAttribute('data-active');
        }
      }
      list.children[active].scrollIntoView({ block: 'nearest' });
    }

    input.addEventListener('input', function () {
      block.params[param.name] = input.value;
      refreshRail();
      markField(input, block, param);
      open(input.value);
    });
    input.addEventListener('focus', function () { open(input.value); });
    input.addEventListener('keydown', function (event) {
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        if (list.hidden) {
          open(input.value);
        }
        highlight(1);
      } else if (event.key === 'ArrowUp') {
        event.preventDefault();
        highlight(-1);
      } else if (event.key === 'Enter') {
        if (!list.hidden && (active >= 0 || options.length === 1)) {
          event.preventDefault();
          pick(options[active >= 0 ? active : 0]);
        }
      } else if (event.key === 'Escape') {
        close();
      }
    });
    // Blur snaps exact text onto the entry it names ("creeper", "Creeper" and
    // the full id all mean the same one); anything else stays as typed.
    input.addEventListener('blur', function () {
      close();
      var match = param.suggests === 'player' ? null : suggest.resolve(param.suggests, input.value);
      if (match) {
        block.params[param.name] = match.id;
        input.value = match.name;
        refreshRail();
      }
      markField(input, block, param);
    });

    markField(input, block, param);
    return box;
  }

  /* Holds a numeric input to the param's declared bounds, the ones the catalog
     carries straight from the mod's own clamps. A blank or non-numeric value is
     left for validation to speak to. */
  function clampToBounds(param, raw) {
    if (raw === '' || raw == null) {
      return raw;
    }
    var value = Number(raw);
    if (!isFinite(value)) {
      return raw;
    }
    if (param.min != null && value < param.min) {
      value = param.min;
    }
    if (param.max != null && value > param.max) {
      value = param.max;
    }
    return String(value);
  }

  function markField(input, block, param) {
    var missing = param.required && preset.isBlank(block, param);
    if (missing) {
      input.setAttribute('aria-invalid', 'true');
    } else {
      input.removeAttribute('aria-invalid');
    }
  }

  function placeholderFor(entry, param) {
    if (param.required) {
      return 'required';
    }
    if (param.type === 'INT' || param.type === 'DECIMAL') {
      return 'default';
    }
    return 'any';
  }

  function paramsForm(block, entry) {
    if (!entry.params.length) {
      return null;
    }
    var form = el('div.params', entry.params.length === 1 ? { class: 'params params--single' } : null);
    entry.params.forEach(function (param) {
      ui.append(form, paramField(block, entry, param));
    });
    return form;
  }

  /* ---------- the scope control ---------- */

  var SCOPE_LABELS = {
    trigger: { every_player: 'Every player', specific_players: 'Specific players…' },
    effect: { per_player: 'Whoever triggered', every_player: 'Every player', specific_players: 'Specific…' },
    modifier: { every_player: 'Every player', specific_players: 'Specific…' }
  };

  var SCOPE_QUESTION = {
    trigger: 'WHO CAN TRIGGER IT',
    effect: 'WHO GETS THE EFFECT',
    modifier: 'WHO IT APPLIES TO'
  };

  function currentScopeChoice(block) {
    if (Array.isArray(block.scope)) {
      return 'specific_players';
    }
    return block.scope;
  }

  function scopeControl(block, entry, context) {
    if (!entry.scoped) {
      return null;
    }
    var allowed = entries.scopes(entry.kind);
    // A playerless trigger has nobody who "triggered it": the engine treats a
    // per-player effect as hitting everyone there, so the choice disappears
    // and a stored per_player (an import, or a later trigger swap) becomes
    // the every_player it already meant.
    if (context && context.playerlessTrigger) {
      allowed = allowed.filter(function (value) { return value !== 'per_player'; });
      if (currentScopeChoice(block) === 'per_player') {
        block.scope = 'every_player';
      }
    }
    var labels = SCOPE_LABELS[entry.kind] || {};
    var choice = currentScopeChoice(block);
    var missing = !preset.scopeIsSet(block.scope);

    var seg = el('div.seg', { 'data-kind': entry.kind, 'data-missing': missing ? 'true' : null });
    allowed.forEach(function (value) {
      ui.append(seg, el('button.seg__opt', {
        type: 'button',
        'aria-pressed': String(choice === value),
        text: labels[value] || value,
        onclick: function () {
          block.scope = value === 'specific_players' ? [] : value;
          render();
        }
      }));
    });

    var body = [
      el('span.scope__label', null, [
        SCOPE_QUESTION[entry.kind] || 'SCOPE',
        el('b.field__req', { text: ' *' })
      ]),
      seg
    ];

    if (choice === 'specific_players') {
      body.push(rosterPicker(block));
    }

    return el('div.scope', { 'data-missing': missing ? 'true' : null }, body);
  }

  function rosterPicker(block) {
    if (!challenge.players.length) {
      return el('span.roster__empty', {
        text: 'Add player names in the Players box, then tick them here.'
      });
    }
    var wrap = el('div.roster');
    challenge.players.forEach(function (name) {
      var on = block.scope.indexOf(name) >= 0;
      ui.append(wrap, el('button.roster__name', {
        type: 'button',
        'aria-pressed': String(on),
        text: (on ? '✓ ' : '') + name,
        onclick: function () {
          block.scope = on
            ? block.scope.filter(function (player) { return player !== name; })
            : block.scope.concat([name]);
          render();
        }
      }));
    });
    return wrap;
  }

  /* ---------- rule cards ---------- */

  var HALF_TAG = { trigger: 'WHEN', effect: 'THEN' };

  function emptyHalf(rule, side) {
    return el('div.half', { 'data-bad': 'true' }, [
      el('div.half__head', null, [
        el('span.tag.tag--bad', { text: HALF_TAG[side] }),
        el('b.half__name.half__name--bad', {
          text: side === 'trigger' ? 'No trigger chosen' : 'No effect chosen'
        })
      ]),
      el('p.half__why', {
        text: 'A rule is a trigger and an effect. This half cannot be empty.'
      }),
      el('button.btn.btn--add', {
        type: 'button',
        'data-kind': side,
        text: '+ PICK ' + (side === 'trigger' ? 'A TRIGGER' : 'AN EFFECT'),
        onclick: function () { openPicker(side, { type: 'half', ruleUid: rule.uid, side: side }); }
      })
    ]);
  }

  function halfView(rule, side) {
    var block = rule[side];
    if (!block.id) {
      return emptyHalf(rule, side);
    }
    var entry = entries.get(block.id);
    if (!entry) {
      return el('div.half', { 'data-bad': 'true' }, [
        el('div.half__head', null, [
          el('span.tag.tag--bad', { text: HALF_TAG[side] }),
          el('b.half__name.half__name--bad', { text: 'Unknown: ' + block.id })
        ]),
        el('p.half__why', { text: 'This id is not in the catalog this site was built against.' })
      ]);
    }

    return el('div.half', null, [
      el('div.half__head', null, [
        el('span.tag', { class: 'tag tag--' + (side === 'trigger' ? 'when' : 'then'), text: HALF_TAG[side] }),
        el('b.half__name', { text: entry.name }),
        el('button.icon-btn', {
          type: 'button',
          title: 'Choose a different ' + side,
          'aria-label': 'Choose a different ' + side,
          text: '⇄',
          style: 'margin-left:auto',
          onclick: function () { openPicker(side, { type: 'half', ruleUid: rule.uid, side: side }); }
        })
      ]),
      paramsForm(block, entry),
      scopeControl(block, entry, side === 'effect' ? { playerlessTrigger: playerlessTrigger(rule) } : null)
    ]);
  }

  /* Whether a rule's chosen trigger is a playerless one (world clock, weather,
     fixed interval): an unchosen or unknown trigger counts as player-ful, so
     the effect keeps all its scope choices until a playerless pick says otherwise. */
  function playerlessTrigger(rule) {
    var entry = rule.trigger.id ? entries.get(rule.trigger.id) : null;
    return Boolean(entry && !entry.scoped);
  }

  function cardTools(actions) {
    var tools = el('div.card__tools');
    actions.forEach(function (action) {
      ui.append(tools, el('button.icon-btn', {
        type: 'button',
        class: 'icon-btn' + (action.danger ? ' icon-btn--bad' : ''),
        title: action.title,
        'aria-label': action.title,
        text: action.glyph,
        onclick: action.run
      }));
    });
    return tools;
  }

  function ruleCard(rule, index, problems) {
    var bad = (problems[rule.uid] || []).length > 0;
    var collapsed = view.expanded !== rule.uid;

    var card = el('article.card', {
      id: 'card-' + rule.uid,
      'data-bad': bad ? 'true' : null,
      'data-collapsed': collapsed ? 'true' : null,
      'data-fresh': rule.fresh ? 'true' : null
    }, [
      el('div.card__strip', null, [
        el('span.card__label', { text: 'RULE ' + String(index + 1).padStart(2, '0') }),
        el('span.badge', {
          hidden: !bad,
          text: ui.plural(bad ? problems[rule.uid].length : 0, 'PROBLEM', 'PROBLEMS')
        }),
        cardTools([
          {
            glyph: '⧉', title: 'Duplicate this rule', run: function () {
              var copy = preset.copyRule(rule);
              copy.fresh = true;
              challenge.rules.splice(index + 1, 0, copy);
              view.expanded = copy.uid;
              render();
            }
          },
          {
            glyph: '✕', title: 'Delete this rule', danger: true, run: function () {
              challenge.rules.splice(index, 1);
              render();
            }
          }
        ])
      ]),
      el('button.card__summary', {
        type: 'button',
        text: phrase.ruleSummary(rule),
        onclick: function () { view.expanded = rule.uid; render(); }
      }),
      el('div.rule', null, [
        halfView(rule, 'trigger'),
        el('div.rule__arrow', { 'aria-hidden': 'true' }, [el('span.arrow__glyph', { text: '→' })]),
        halfView(rule, 'effect')
      ])
    ]);

    delete rule.fresh;
    return card;
  }

  /* ---------- goal and modifier cards ---------- */

  function goalCard(problems) {
    var goal = challenge.goal;
    var entry = entries.get(goal.id);
    var bad = (problems[goal.uid] || []).length > 0;

    return el('article.card.goal-card', {
      id: 'card-' + goal.uid,
      'data-bad': bad ? 'true' : null
    }, [
      el('span.tag.tag--win', { text: 'WIN' }),
      el('b.half__name', { text: entry ? entry.name : goal.id }),
      el('span.badge', {
        hidden: !bad,
        text: ui.plural(bad ? problems[goal.uid].length : 0, 'PROBLEM', 'PROBLEMS')
      }),
      entry ? paramsForm(goal, entry) : null,
      cardTools([
        {
          glyph: '⇄', title: 'Choose a different goal', run: function () {
            openPicker('goal', { type: 'goal' });
          }
        },
        {
          glyph: '✕', title: 'Remove the goal', danger: true, run: function () {
            challenge.goal = null;
            render();
          }
        }
      ])
    ]);
  }

  function modifierCard(modifier, index, problems) {
    var entry = entries.get(modifier.id);
    var bad = (problems[modifier.uid] || []).length > 0;
    var collapsed = view.expanded !== modifier.uid;
    var line = phrase.modifierLine(modifier);

    var form = el('div.mod-card__form.stack.stack--tight', null, [
      entry ? paramsForm(modifier, entry) : null,
      entry ? scopeControl(modifier, entry) : null
    ]);

    return el('article.card.mod-card', {
      id: 'card-' + modifier.uid,
      'data-bad': bad ? 'true' : null,
      'data-collapsed': collapsed ? 'true' : null,
      'data-fresh': modifier.fresh ? 'true' : null
    }, [
      el('div.mod-card__head', null, [
        el('b.mod-card__name', { text: entry ? entry.name : modifier.id }),
        el('span.badge', {
          hidden: !bad,
          text: ui.plural(bad ? problems[modifier.uid].length : 0, 'PROBLEM', 'PROBLEMS')
        }),
        cardTools([
          {
            glyph: '⧉', title: 'Duplicate this modifier', run: function () {
              var copy = preset.copyBlock(modifier);
              copy.fresh = true;
              challenge.modifiers.splice(index + 1, 0, copy);
              view.expanded = copy.uid;
              render();
            }
          },
          {
            glyph: '✕', title: 'Remove this modifier', danger: true, run: function () {
              challenge.modifiers.splice(index, 1);
              render();
            }
          }
        ])
      ]),
      el('button.card__summary', {
        type: 'button',
        text: [line.detail, line.scope].filter(Boolean).join(' | ') || 'No settings',
        onclick: function () { view.expanded = modifier.uid; render(); }
      }),
      form
    ]);
  }

  /* ---------- sections ---------- */

  function section(kind, title, hint, cards, addLabel, addHint, onAdd) {
    return el('section.section', { 'data-kind': kind }, [
      el('div.section__head', null, [
        el('h2.section__title', { text: title }),
        el('span.section__hint', { text: hint })
      ]),
      cards,
      el('button.btn.btn--add', {
        type: 'button',
        'data-kind': kind === 'rules' ? 'trigger' : (kind === 'goal' ? 'goal' : 'modifier'),
        onclick: onAdd
      }, [addLabel, addHint ? el('span.btn__hint', { text: addHint }) : null])
    ]);
  }

  function addRule() {
    var rule = preset.blankRule();
    rule.fresh = true;
    challenge.rules.push(rule);
    view.expanded = rule.uid;
    view.tab = 'rules';
    render();
    openPicker('trigger', { type: 'half', ruleUid: rule.uid, side: 'trigger' });
  }

  function renderBoard(problems) {
    var main = ui.clear(dom.main);
    main.setAttribute('data-tab', view.tab);

    var ruleCards = el('div.stack');
    challenge.rules.forEach(function (rule, index) {
      ui.append(ruleCards, ruleCard(rule, index, problems));
    });
    ui.append(main, section('rules', 'RULES', 'trigger + effect, both required',
      ruleCards, '+ ADD RULE', null, addRule));

    var goalBody = challenge.goal
      ? goalCard(problems)
      : el('p.section__hint', { text: 'No goal. The run has no win condition and simply continues.' });
    ui.append(main, section('goal', 'GOAL', 'at most one', goalBody,
      challenge.goal ? '+ REPLACE GOAL' : '+ SET A GOAL', null,
      function () { openPicker('goal', { type: 'goal' }); }));

    var modGrid = el('div.mod-grid');
    challenge.modifiers.forEach(function (modifier, index) {
      ui.append(modGrid, modifierCard(modifier, index, problems));
    });
    ui.append(main, section('modifiers', 'MODIFIERS', 'always-on for the whole run',
      modGrid, '+ ADD MODIFIER', null,
      function () { openPicker('modifier', { type: 'modifier' }); }));
  }

  function renderEmpty() {
    ui.clear(dom.main);
    ui.append(dom.main, el('div.empty', null, [
      el('div.empty__choices', null, [
        el('button.btn.btn--add', { type: 'button', 'data-kind': 'trigger', onclick: addRule },
          ['+ ADD A RULE', el('span.btn__hint', { text: 'when X happens, do Y' })]),
        el('button.btn.btn--add', {
          type: 'button', 'data-kind': 'goal',
          onclick: function () { openPicker('goal', { type: 'goal' }); }
        }, ['+ SET A GOAL', el('span.btn__hint', { text: 'optional win condition' })]),
        el('button.btn.btn--add', {
          type: 'button', 'data-kind': 'modifier',
          onclick: function () { openPicker('modifier', { type: 'modifier' }); }
        }, ['+ ADD A MODIFIER', el('span.btn__hint', { text: 'always-on, whole run' })])
      ]),
      el('p.section__hint', null, [
        'or drop a preset JSON on the ',
        el('a', { href: link.pageHref('index.html') + '#import', text: 'front page' }),
        ' and edit it.'
      ])
    ]));
  }

  /* ---------- the summary rail ---------- */

  function refreshRail() {
    var problems = preset.problems(challenge);
    renderRail(problems);
    renderTabs();
    refreshCardStates();
    dom.exportTop.disabled = problems.length > 0;
    dom.exportBottom.disabled = problems.length > 0;
  }

  /* Typing in a parameter deliberately skips a re-render so the input keeps
     focus, which would otherwise leave each card's own error mark showing a
     stale answer. Every card carries its badge from the start, so bringing it
     up to date is a text and attribute change rather than a rebuild. */
  function refreshCardStates() {
    var map = problemsByUid();
    var owners = challenge.rules.concat(challenge.modifiers);
    if (challenge.goal) {
      owners.push(challenge.goal);
    }
    owners.forEach(function (owner) {
      var card = document.getElementById('card-' + owner.uid);
      if (!card) {
        return;
      }
      var count = (map[owner.uid] || []).length;
      if (count) {
        card.setAttribute('data-bad', 'true');
      } else {
        card.removeAttribute('data-bad');
      }
      var badge = card.querySelector('.badge');
      if (badge) {
        badge.hidden = count === 0;
        badge.textContent = ui.plural(count, 'PROBLEM', 'PROBLEMS');
      }
    });
  }

  function renderRail(problems) {
    var panel = ui.clear(dom.rail);

    if (problems.length) {
      var list = el('div.problems', null, [
        el('span.problems__title', { text: ui.plural(problems.length, 'PROBLEM', 'PROBLEMS') })
      ]);
      problems.slice(0, 6).forEach(function (item) {
        ui.append(list, el('button.problems__item', {
          type: 'button',
          text: '▸ ' + item.text,
          onclick: function () { jumpTo(item.uid); }
        }));
      });
      if (problems.length > 6) {
        ui.append(list, el('span.problems__foot', {
          text: 'and ' + (problems.length - 6) + ' more'
        }));
      }
      ui.append(list, el('span.problems__foot', {
        text: 'Only structure blocks export. Bad ideas never do.'
      }));
      ui.append(panel, list);
    }

    ui.append(panel, el('button.btn.btn--primary', {
      type: 'button',
      text: problems.length ? 'EXPORT | FIX ' + problems.length + ' FIRST' : 'EXPORT ▸',
      disabled: problems.length > 0,
      onclick: goToShare
    }));
  }

  function jumpTo(uid) {
    if (uid === 'challenge-name') {
      dom.name.focus();
      dom.name.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }
    // On a phone the owning section and card may both be out of view.
    var rule = ruleAt(uid);
    var isModifier = challenge.modifiers.some(function (m) { return m.uid === uid; });
    view.tab = rule ? 'rules' : (isModifier ? 'modifiers' : 'goal');
    view.expanded = uid;
    render();
    ui.ping(document.getElementById('card-' + uid));
  }

  /* ---------- mobile tabs and action bar ---------- */

  function renderTabs() {
    var bar = ui.clear(dom.tabs);
    var counts = {
      rules: challenge.rules.length,
      goal: challenge.goal ? 1 : 0,
      modifiers: challenge.modifiers.length
    };
    [['rules', 'RULES'], ['goal', 'GOAL'], ['modifiers', 'MODS']].forEach(function (pair) {
      ui.append(bar, el('button.seg__opt', {
        type: 'button',
        'aria-pressed': String(view.tab === pair[0]),
        text: pair[1] + ' ' + counts[pair[0]],
        onclick: function () { view.tab = pair[0]; render(); }
      }));
    });
  }

  /* ---------- the picker ---------- */

  var pickerState = { kind: 'effect', query: '', target: null };

  function openPicker(kind, target) {
    pickerState.kind = kind;
    pickerState.target = target;
    pickerState.query = '';
    dom.picker.hidden = false;
    renderPicker();
    var search = dom.picker.querySelector('.modal__filters .input');
    if (search) {
      search.focus();
    }
  }

  function closePicker() {
    dom.picker.hidden = true;
    pickerState.target = null;
  }

  function pickerTitle() {
    var target = pickerState.target;
    if (!target) {
      return '';
    }
    if (target.type === 'half') {
      var index = challenge.rules.map(function (r) { return r.uid; }).indexOf(target.ruleUid);
      return 'for RULE ' + String(index + 1).padStart(2, '0')
        + ' | ' + (target.side === 'trigger' ? 'WHEN' : 'THEN') + ' slot';
    }
    return target.type === 'goal' ? 'the run ends in a win when this is reached'
      : 'always on, for the whole run';
  }

  function choose(entry) {
    var target = pickerState.target;
    if (target.type === 'half') {
      var rule = ruleAt(target.ruleUid);
      if (rule) {
        preset.assign(rule[target.side], entry.id);
        view.expanded = rule.uid;
      }
    } else if (target.type === 'goal') {
      challenge.goal = preset.assign(preset.blankBlock('goal'), entry.id);
    } else {
      var modifier = preset.assign(preset.blankBlock('modifier'), entry.id);
      modifier.fresh = true;
      challenge.modifiers.push(modifier);
      view.expanded = modifier.uid;
      view.tab = 'modifiers';
    }
    closePicker();
    render();
    focusFirstRequired(entry);
  }

  /* Picking an entry should land the cursor where the work is. */
  function focusFirstRequired(entry) {
    if (!entries.requiredParams(entry).length) {
      return;
    }
    var input = document.querySelector('.card:not([data-collapsed="true"]) .input[aria-invalid="true"]');
    if (input) {
      input.focus();
    }
  }

  function pickCard(entry) {
    var chips = el('div.pick__chips');
    // Parameters read the same whether required or optional: no asterisk, no
    // colour split. Entries with no parameters simply show no chips.
    entry.params.forEach(function (param) {
      ui.append(chips, el('span.pick__chip', { text: param.name }));
    });

    return el('button.pick', {
      type: 'button',
      'data-kind': entry.kind,
      onclick: function () { choose(entry); }
    }, [
      el('span.pick__name', { text: entry.name }),
      entry.blurb ? el('span.pick__blurb', { text: entry.blurb }) : null,
      chips
    ]);
  }

  function renderPicker() {
    var kind = pickerState.kind;
    ui.clear(dom.pickerTitle).appendChild(document.createTextNode('PICK A ' + kind.toUpperCase()));
    dom.pickerSub.textContent = pickerTitle();

    dom.pickerSearch.placeholder = 'Search ' + entries.count(kind) + ' ' + kind + 's…';
    dom.pickerSearch.value = pickerState.query;

    var shown = entries.search(kind, pickerState.query);
    var grid = ui.clear(dom.pickerGrid);
    if (!shown.length) {
      ui.append(grid, el('div.pick-empty', {
        text: 'Nothing matches "' + pickerState.query + '".'
      }));
      return;
    }
    shown.forEach(function (entry) { ui.append(grid, pickCard(entry)); });
  }

  /* ---------- export ---------- */

  /* Export is not a modal on this page. The share page already presents the
     finished challenge with copy-link, download, and edit all in one place, so
     the builder just sends the composed preset straight there. A challenge with
     structural problems never leaves, the same gate the old export moment had. */
  function goToShare() {
    if (preset.problems(challenge).length) {
      return;
    }
    window.location.href = link.urlFor('index.html', challenge);
  }

  /* ---------- render ---------- */

  function render() {
    dom.name.value = challenge.name;
    renderRoster();
    if (isEmptyChallenge()) {
      renderEmpty();
    } else {
      renderBoard(problemsByUid());
    }
    refreshRail();
  }

  /* ---------- startup ---------- */

  function loadFromFragment() {
    var encoded = link.readFragment();
    if (!encoded) {
      return;
    }
    try {
      challenge = link.decode(encoded);
      ui.toast('Loaded from a share link. Edit away.');
    } catch (error) {
      ui.toast('That link did not decode: ' + error.message);
    }
  }

  function bind() {
    dom.name = document.getElementById('challenge-name');
    dom.roster = document.getElementById('roster');
    dom.main = document.getElementById('board-main');
    dom.rail = document.getElementById('rail-panel');
    dom.tabs = document.getElementById('tabbar');
    dom.exportTop = document.getElementById('export-top');
    dom.exportBottom = document.getElementById('export-bottom');
    dom.addBottom = document.getElementById('add-bottom');

    dom.picker = document.getElementById('picker');
    dom.pickerTitle = document.getElementById('picker-title');
    dom.pickerSub = document.getElementById('picker-sub');
    dom.pickerSearch = document.getElementById('picker-search');
    dom.pickerGrid = document.getElementById('picker-grid');

    dom.name.addEventListener('input', function () {
      challenge.name = dom.name.value;
      refreshRail();
    });

    dom.exportTop.addEventListener('click', goToShare);
    dom.exportBottom.addEventListener('click', goToShare);
    dom.addBottom.addEventListener('click', function () {
      if (view.tab === 'rules') {
        addRule();
      } else if (view.tab === 'goal') {
        openPicker('goal', { type: 'goal' });
      } else {
        openPicker('modifier', { type: 'modifier' });
      }
    });

    dom.pickerSearch.addEventListener('input', function () {
      pickerState.query = dom.pickerSearch.value;
      renderPicker();
    });

    document.getElementById('picker-close').addEventListener('click', closePicker);

    dom.picker.addEventListener('mousedown', function (event) {
      if (event.target === dom.picker) {
        dom.picker.hidden = true;
      }
    });

    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') {
        closePicker();
      }
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    bind();
    loadFromFragment();
    render();
  });
})();
