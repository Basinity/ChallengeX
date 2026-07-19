/* Headless checks for the site's logic layer.

   These load the real browser scripts into a bare `window` and drive them the
   way the pages do. What they are really guarding is the contract with the
   mod: the JSON this site writes has to be the JSON the mod's PresetCodec
   reads, and no test on this side can prove that alone. So the last thing this
   file does is write a preset out to disk, where PresetContractTest in :core
   parses it with the actual codec.

   Run with: node web/test/run.js */

'use strict';

const fs = require('fs');
const path = require('path');
const vm = require('vm');

const ROOT = path.join(__dirname, '..');
const OUT = path.join(__dirname, 'out');

/* ---------- a browser-shaped sandbox ---------- */

const sandbox = {
  window: {},
  TextEncoder,
  TextDecoder,
  btoa: (s) => Buffer.from(s, 'binary').toString('base64'),
  atob: (s) => Buffer.from(s, 'base64').toString('binary'),
  URL,
  URLSearchParams,
  console
};
sandbox.window.location = { href: 'https://challengexmc.com/index.html', hash: '' };
sandbox.globalThis = sandbox;
vm.createContext(sandbox);

for (const file of ['catalog.js', 'copy.js', 'entries.js', 'preset.js', 'phrase.js', 'link.js']) {
  const source = fs.readFileSync(path.join(ROOT, 'assets', 'js', file), 'utf8');
  vm.runInContext(source, sandbox, { filename: file });
}

const CX = sandbox.window.CX;
const { entries, preset, phrase, link } = CX;

/* ---------- a tiny assertion harness ---------- */

let passed = 0;
const failures = [];

function check(name, fn) {
  try {
    fn();
    passed += 1;
  } catch (error) {
    failures.push(name + '\n    ' + error.message);
  }
}

function eq(actual, expected, what) {
  const a = JSON.stringify(actual);
  const b = JSON.stringify(expected);
  if (a !== b) {
    throw new Error((what || 'value') + '\n    expected ' + b + '\n    actual   ' + a);
  }
}

function ok(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

/* ---------- catalog ---------- */

check('catalog carries the whole frozen catalog', () => {
  eq(entries.count('trigger'), 44, 'triggers');
  eq(entries.count('effect'), 36, 'effects');
  eq(entries.count('goal'), 4, 'goals');
  eq(entries.count('modifier'), 12, 'modifiers');
  eq(entries.schemaVersion, 1, 'schema version');
});

check('every entry has copy, and every non-modifier has a phrase', () => {
  const missing = [];
  entries.kinds.forEach((kind) => {
    entries.all(kind).forEach((entry) => {
      if (!entry.blurb) {
        missing.push(entry.id + ' (blurb)');
      }
      if (kind !== 'modifier' && !entry.phrase) {
        missing.push(entry.id + ' (phrase)');
      }
    });
  });
  eq(missing, [], 'entries missing copy');
});

check('playerless entries are exactly the ones the mod pins', () => {
  const playerless = [];
  ['trigger', 'effect', 'modifier'].forEach((kind) => {
    entries.all(kind).forEach((entry) => {
      if (!entry.scoped) {
        playerless.push(entry.id);
      }
    });
  });
  eq(playerless.sort(), [
    'effect.change_time', 'effect.change_weather', 'effect.lose_challenge',
    'modifier.buff_hostile_mobs', 'modifier.time_limit',
    'trigger.fixed_interval', 'trigger.time_of_day', 'trigger.weather_changed'
  ], 'playerless ids');
});

check('only effects offer per_player, goals offer no scope', () => {
  ok(entries.scopes('effect').includes('per_player'), 'effects take per_player');
  ok(!entries.scopes('trigger').includes('per_player'), 'triggers do not');
  ok(!entries.scopes('modifier').includes('per_player'), 'modifiers do not');
  eq(entries.scopes('goal'), [], 'goals');
});

check('search finds entries by name, id and blurb', () => {
  ok(entries.search('trigger', 'damage').length >= 2, 'by name');
  ok(entries.search('effect', 'effect.lightning').length === 1, 'by id');
  ok(entries.search('modifier', '').length === 12, 'empty query returns all');
  eq(entries.search('effect', 'zzzz').length, 0, 'no match');
});

/* ---------- building a challenge ---------- */

function starter() {
  const challenge = preset.blankChallenge();
  challenge.name = 'Blood Sugar Rush';
  challenge.players = ['Basinity', 'Pix'];

  const one = preset.blankRule();
  preset.assign(one.trigger, 'trigger.damage_taken');
  one.trigger.scope = 'every_player';
  preset.assign(one.effect, 'effect.random_effect');
  one.effect.params = { type: 'negative', seconds: '15' };
  one.effect.scope = 'per_player';

  const two = preset.blankRule();
  preset.assign(two.trigger, 'trigger.mob_killed');
  two.trigger.scope = 'every_player';
  preset.assign(two.effect, 'effect.spawn_mob');
  two.effect.params = { mob: 'minecraft:zombie', count: '2', baby: true };
  two.effect.scope = 'per_player';

  const three = preset.blankRule();
  preset.assign(three.trigger, 'trigger.fixed_interval');
  three.trigger.params = { seconds: '300' };
  preset.assign(three.effect, 'effect.lightning');
  three.effect.scope = 'every_player';

  challenge.rules.push(one, two, three);
  challenge.goal = preset.assign(preset.blankBlock('goal'), 'goal.beat_game');

  const regen = preset.assign(preset.blankBlock('modifier'), 'modifier.no_natural_regen');
  regen.scope = 'every_player';
  const keep = preset.assign(preset.blankBlock('modifier'), 'modifier.keep_inventory');
  keep.scope = ['Basinity'];
  const clock = preset.assign(preset.blankBlock('modifier'), 'modifier.time_limit');
  clock.params = { minutes: '90' };
  challenge.modifiers.push(regen, keep, clock);

  return challenge;
}

check('a composed challenge exports exactly the preset shape the codec reads', () => {
  const json = preset.toPreset(starter());
  eq(json, {
    schemaVersion: 1,
    name: 'Blood Sugar Rush',
    rules: [
      {
        trigger: { id: 'trigger.damage_taken', scope: 'every_player' },
        effect: {
          id: 'effect.random_effect',
          params: { type: 'negative', seconds: 15 },
          scope: 'per_player'
        }
      },
      {
        trigger: { id: 'trigger.mob_killed', scope: 'every_player' },
        effect: {
          id: 'effect.spawn_mob',
          params: { mob: 'minecraft:zombie', count: 2, baby: true },
          scope: 'per_player'
        }
      },
      {
        // Playerless on both sides: no scope key on either, which the codec requires.
        trigger: { id: 'trigger.fixed_interval', params: { seconds: 300 } },
        effect: { id: 'effect.lightning', scope: 'every_player' }
      }
    ],
    goal: { id: 'goal.beat_game' },
    modifiers: [
      { id: 'modifier.no_natural_regen', scope: 'every_player' },
      { id: 'modifier.keep_inventory', scope: ['Basinity'] },
      { id: 'modifier.time_limit', params: { minutes: 90 } }
    ]
  }, 'exported preset');
});

check('numbers are written as numbers, not as the strings the inputs hold', () => {
  const json = preset.toPreset(starter());
  eq(typeof json.rules[1].effect.params.count, 'number', 'INT');
  eq(typeof json.rules[1].effect.params.baby, 'boolean', 'BOOL');
  eq(typeof json.modifiers[2].params.minutes, 'number', 'INT');
});

check('an empty challenge exports with no rules, goal or modifiers keys', () => {
  const challenge = preset.blankChallenge();
  challenge.name = 'Nothing At All';
  eq(preset.toPreset(challenge), { schemaVersion: 1, name: 'Nothing At All' }, 'empty preset');
});

check('a modifier-only challenge is a valid shape', () => {
  const challenge = preset.blankChallenge();
  challenge.name = 'No Jumping';
  const jump = preset.assign(preset.blankBlock('modifier'), 'modifier.disable_jump');
  jump.scope = 'every_player';
  challenge.modifiers.push(jump);
  eq(preset.problems(challenge), [], 'problems');
  eq(preset.toPreset(challenge).modifiers.length, 1, 'modifier survives');
});

check('blank optional parameters are dropped rather than written empty', () => {
  const challenge = preset.blankChallenge();
  challenge.name = 'Bare';
  const rule = preset.blankRule();
  preset.assign(rule.trigger, 'trigger.mob_killed');
  rule.trigger.scope = 'every_player';
  rule.trigger.params = { mob: '   ' };
  preset.assign(rule.effect, 'effect.teleport_random');
  rule.effect.scope = 'per_player';
  rule.effect.params = { radius: '' };
  challenge.rules.push(rule);

  const json = preset.toPreset(challenge);
  ok(!('params' in json.rules[0].trigger), 'whitespace-only string dropped');
  ok(!('params' in json.rules[0].effect), 'empty number dropped');
});

check('a false boolean is dropped, a true one is written', () => {
  const challenge = preset.blankChallenge();
  challenge.name = 'Bool';
  const rule = preset.blankRule();
  preset.assign(rule.trigger, 'trigger.slept');
  rule.trigger.scope = 'every_player';
  preset.assign(rule.effect, 'effect.spawn_mob');
  rule.effect.scope = 'per_player';
  rule.effect.params = { mob: 'minecraft:bat', baby: false };
  challenge.rules.push(rule);
  eq(preset.toPreset(challenge).rules[0].effect.params, { mob: 'minecraft:bat' }, 'false dropped');

  rule.effect.params.baby = true;
  eq(preset.toPreset(challenge).rules[0].effect.params,
    { mob: 'minecraft:bat', baby: true }, 'true written');
});

/* ---------- validation ---------- */

check('a complete challenge has no problems', () => {
  eq(preset.problems(starter()), [], 'problems');
});

check('the structural failures are caught', () => {
  const challenge = preset.blankChallenge();

  const halfRule = preset.blankRule();
  preset.assign(halfRule.trigger, 'trigger.jumped');
  halfRule.trigger.scope = 'every_player';
  challenge.rules.push(halfRule);

  const noScope = preset.blankRule();
  preset.assign(noScope.trigger, 'trigger.jumped');
  preset.assign(noScope.effect, 'effect.kill');
  noScope.effect.scope = 'per_player';
  challenge.rules.push(noScope);

  const noParam = preset.assign(preset.blankBlock('modifier'), 'modifier.time_limit');
  challenge.modifiers.push(noParam);

  const texts = preset.problems(challenge).map((p) => p.text);
  ok(texts.some((t) => t.includes('needs a name')), 'blank name: ' + texts);
  ok(texts.some((t) => t.includes('no effect chosen')), 'half rule: ' + texts);
  ok(texts.some((t) => t.includes('choose who this applies to')), 'missing scope: ' + texts);
  ok(texts.some((t) => t.includes('needs minutes')), 'missing required param: ' + texts);
});

check('choosing specific players and then ticking nobody is a problem', () => {
  const challenge = preset.blankChallenge();
  challenge.name = 'Empty Scope';
  const modifier = preset.assign(preset.blankBlock('modifier'), 'modifier.keep_inventory');
  modifier.scope = [];
  challenge.modifiers.push(modifier);
  const texts = preset.problems(challenge).map((p) => p.text);
  ok(texts.some((t) => t.includes('pick at least one player')), texts.join(' / '));
});

check('a non-numeric value in a number field is a problem', () => {
  const challenge = preset.blankChallenge();
  challenge.name = 'Bad Number';
  const modifier = preset.assign(preset.blankBlock('modifier'), 'modifier.time_limit');
  modifier.params = { minutes: '90 minutes' };
  challenge.modifiers.push(modifier);
  const texts = preset.problems(challenge).map((p) => p.text);
  ok(texts.some((t) => t.includes('must be a number')), texts.join(' / '));

  modifier.params.minutes = '2.5';
  ok(preset.problems(challenge).map((p) => p.text).some((t) => t.includes('whole number')),
    'a decimal in an INT field');
});

check('a bad idea is never blocked', () => {
  const challenge = preset.blankChallenge();
  challenge.name = 'Unwinnable';

  // Die instantly, twice, plus the same modifier four times over.
  for (let i = 0; i < 2; i += 1) {
    const rule = preset.blankRule();
    preset.assign(rule.trigger, 'trigger.jumped');
    rule.trigger.scope = 'every_player';
    preset.assign(rule.effect, 'effect.kill');
    rule.effect.scope = 'per_player';
    challenge.rules.push(rule);
  }
  for (let i = 0; i < 4; i += 1) {
    const modifier = preset.assign(preset.blankBlock('modifier'), 'modifier.disable_jump');
    modifier.scope = 'every_player';
    challenge.modifiers.push(modifier);
  }

  eq(preset.problems(challenge), [], 'contradictions and duplicates are allowed');
  eq(preset.toPreset(challenge).modifiers.length, 4, 'nothing is deduplicated');
});

/* ---------- links ---------- */

check('a share link round-trips, including non-ASCII names', () => {
  const original = starter();
  original.name = 'Zuckerschöck — 90 Minuten';
  const decoded = link.decode(link.encode(original));
  eq(preset.toPreset(decoded), preset.toPreset(original), 'round trip');
});

check('the roster is rebuilt from the scopes a returning preset mentions', () => {
  const decoded = link.decode(link.encode(starter()));
  eq(decoded.players, ['Basinity'], 'only names actually used come back');
});

check('a truncated link fails with a readable reason instead of a crash', () => {
  let message = null;
  try {
    link.decode('bm90IGEgcHJlc2V0');
  } catch (error) {
    message = error.message;
  }
  ok(message && message.includes('JSON'), 'got: ' + message);
});

check('a preset from a newer schema is refused', () => {
  let message = null;
  try {
    preset.parse(JSON.stringify({ schemaVersion: 3, name: 'From The Future' }));
  } catch (error) {
    message = error.message;
  }
  ok(message && message.includes('schema v3'), 'got: ' + message);
});

check('the export filename is one the mod will accept', () => {
  // PresetStore.isSafeName rejects separators, "..", and blanks.
  const cases = ['Blood Sugar Rush', '../../etc/passwd', 'a/b\\c', '   ', 'Zuckerschöck!!'];
  cases.forEach((name) => {
    const slug = preset.slug(name);
    ok(slug.length > 0, name + ' produced a blank slug');
    ok(!slug.includes('/') && !slug.includes('\\') && !slug.includes('..'),
      name + ' produced an unsafe slug: ' + slug);
    ok(/^[a-z0-9-]+$/.test(slug), name + ' produced ' + slug);
  });
});

/* ---------- phrasing ---------- */

check('rules read as English', () => {
  const challenge = starter();
  eq(phrase.ruleSummary(challenge.rules[0]),
    'When anyone takes damage → whoever triggers it gets a random negative effect for 15s',
    'rule 1');
  eq(phrase.ruleSummary(challenge.rules[1]),
    'When anyone kills a mob → whoever triggers it has 2 baby minecraft:zombie spawned on them',
    'rule 2');
  eq(phrase.ruleSummary(challenge.rules[2]),
    'Every 300 seconds → everyone is struck by lightning',
    'a playerless trigger drops the "when" and the subject');
});

check('specific-player scopes stay grammatical', () => {
  const rule = preset.blankRule();
  preset.assign(rule.trigger, 'trigger.slept');
  rule.trigger.scope = ['Pix', 'Kettu'];
  preset.assign(rule.effect, 'effect.lightning');
  rule.effect.scope = ['Basinity'];
  eq(phrase.ruleSummary(rule),
    'When any of Pix or Kettu sleeps → Basinity is struck by lightning', 'two names');

  rule.trigger.scope = ['Pix'];
  rule.effect.scope = ['Basinity', 'Pix', 'Kettu'];
  eq(phrase.ruleSummary(rule),
    'When Pix sleeps → each of Basinity, Pix and Kettu is struck by lightning', 'three names');
});

check('optional parameters appear only when set', () => {
  const block = { id: 'trigger.damage_taken', params: {}, scope: 'every_player' };
  eq(phrase.ruleLine({ trigger: block, effect: { id: null, params: {} } }).trigger,
    'anyone takes damage', 'without the optional source');
  block.params.source = 'minecraft:fall';
  eq(phrase.ruleLine({ trigger: block, effect: { id: null, params: {} } }).trigger,
    'anyone takes damage from minecraft:fall', 'with it');
});

check('every entry renders a phrase with required parameters filled', () => {
  const bad = [];
  ['trigger', 'effect', 'goal'].forEach((kind) => {
    entries.all(kind).forEach((entry) => {
      const block = { id: entry.id, params: {}, scope: 'every_player' };
      entry.params.forEach((param) => {
        block.params[param.name] = param.type === 'BOOL' ? true : 'X';
      });
      const text = phrase.render(entry.phrase, block);
      if (!text || text.includes('{') || text.includes('[') || text.includes('  ')) {
        bad.push(entry.id + ' -> "' + text + '"');
      }
    });
  });
  eq(bad, [], 'entries whose phrase did not render cleanly');
});

check('every entry also renders with only its required parameters', () => {
  const bad = [];
  ['trigger', 'effect', 'goal'].forEach((kind) => {
    entries.all(kind).forEach((entry) => {
      const block = { id: entry.id, params: {}, scope: 'every_player' };
      entry.params.filter((p) => p.required).forEach((param) => {
        block.params[param.name] = param.type === 'BOOL' ? true : 'X';
      });
      const text = phrase.render(entry.phrase, block);
      if (!text || text.includes('{') || text.includes('[')) {
        bad.push(entry.id + ' -> "' + text + '"');
      }
    });
  });
  eq(bad, [], 'entries whose phrase left a placeholder behind');
});

check('the technical line states exactly what goes in the file', () => {
  const challenge = starter();
  eq(phrase.technical(challenge.rules[1].effect),
    'effect.spawn_mob | mob=minecraft:zombie | count=2 | baby=true | scope=per_player',
    'technical detail');
});

/* ---------- hand the result to the mod ---------- */

fs.mkdirSync(OUT, { recursive: true });

const fixtures = {
  'site-export.json': preset.stringify(starter()),
  'site-export-modifier-only.json': (() => {
    const challenge = preset.blankChallenge();
    challenge.name = 'Minecraft But No Jumping';
    const jump = preset.assign(preset.blankBlock('modifier'), 'modifier.disable_jump');
    jump.scope = 'every_player';
    const blind = preset.assign(preset.blankBlock('modifier'), 'modifier.status_effect');
    blind.params = { effect: 'minecraft:blindness', amplifier: '0' };
    blind.scope = ['Pix'];
    challenge.modifiers.push(jump, blind);
    return preset.stringify(challenge);
  })(),
  'site-export-every-entry.json': (() => {
    // One rule per trigger and per effect, so the codec sees every id the site
    // can emit rather than only the handful a sample challenge happens to use.
    const challenge = preset.blankChallenge();
    challenge.name = 'Every Entry In The Catalog';
    const triggers = entries.all('trigger');
    const effects = entries.all('effect');
    const count = Math.max(triggers.length, effects.length);

    function fill(block, entry) {
      entry.params.forEach((param) => {
        if (param.type === 'BOOL') {
          block.params[param.name] = true;
        } else if (param.type === 'INT') {
          block.params[param.name] = '3';
        } else if (param.type === 'DECIMAL') {
          block.params[param.name] = '2.5';
        } else {
          block.params[param.name] = 'minecraft:test';
        }
      });
      if (entry.scoped) {
        block.scope = entry.kind === 'effect' ? 'per_player' : 'every_player';
      }
    }

    for (let i = 0; i < count; i += 1) {
      const rule = preset.blankRule();
      fill(preset.assign(rule.trigger, triggers[i % triggers.length].id),
        triggers[i % triggers.length]);
      fill(preset.assign(rule.effect, effects[i % effects.length].id),
        effects[i % effects.length]);
      challenge.rules.push(rule);
    }

    entries.all('modifier').forEach((entry) => {
      const modifier = preset.assign(preset.blankBlock('modifier'), entry.id);
      fill(modifier, entry);
      challenge.modifiers.push(modifier);
    });

    challenge.goal = (() => {
      const goal = preset.assign(preset.blankBlock('goal'), 'goal.obtain_item');
      goal.params = { item: 'minecraft:diamond' };
      return goal;
    })();

    return preset.stringify(challenge);
  })()
};

Object.keys(fixtures).forEach((name) => {
  fs.writeFileSync(path.join(OUT, name), fixtures[name], 'utf8');
});

check('the every-entry fixture really covers the whole catalog', () => {
  const json = JSON.parse(fixtures['site-export-every-entry.json']);
  const seen = new Set();
  json.rules.forEach((rule) => {
    seen.add(rule.trigger.id);
    seen.add(rule.effect.id);
  });
  json.modifiers.forEach((modifier) => seen.add(modifier.id));
  seen.add(json.goal.id);
  eq(seen.size, entries.total() - 3, 'ids covered (all but the three unused goals)');
});

/* ---------- report ---------- */

console.log('');
failures.forEach((failure) => console.log('  FAIL  ' + failure));
console.log('  ' + passed + ' passed, ' + failures.length + ' failed');
console.log('  fixtures written to web/test/out/ for :core:test to parse');
console.log('');
process.exit(failures.length ? 1 : 0);
