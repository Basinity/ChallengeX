/* Human copy for every catalog entry, keyed by the same frozen id the mod uses.

   This lives on the web side on purpose: the generated catalog carries what an
   entry IS (its id, its parameters, whether it takes a scope), and this file
   carries what an entry READS AS. Adding a catalog entry without adding copy
   here is not an error; the entry falls back to a name derived from its id and
   renders without a blurb.

   Three fields:
     name    the label shown everywhere in the UI.
     blurb   one or two dry lines for the picker card.
     phrase  how the entry reads inside a sentence on the shared-link page.

   Phrase templates understand two things and nothing else:
     {param}          the value, or the whole [chunk] is dropped when unset
     {param?fallback} the value, or the fallback text when unset
     [ ...{p}... ]    a chunk kept only when every bare {p} inside it has a
                      truthy value, which is what makes optional parameters and
                      booleans disappear cleanly

   A BOOL renders as empty text: it only decides whether its chunk survives.
   Scoped entries write a bare verb phrase, because a subject ("someone", "each
   of Pix and Kettu") is put in front of them at render time. Playerless
   entries write a whole clause instead, since no subject exists for them. */

window.CX_COPY = {

  /* ---------- triggers ---------- */

  'trigger.block_broken': {
    name: 'Block broken',
    blurb: 'Someone mines a block. Any block, or one in particular.',
    phrase: 'breaks {block?a block}'
  },
  'trigger.block_placed': {
    name: 'Block placed',
    blurb: 'A block gets placed. Any block, or one in particular.',
    phrase: 'places {block?a block}'
  },
  'trigger.mob_killed': {
    name: 'Mob killed',
    blurb: 'A mob gets killed. Any mob, or one in particular.',
    phrase: 'kills {mob?a mob}'
  },
  'trigger.kill_player': {
    name: 'Player killed',
    blurb: 'One player kills another. Name one, or leave it open season.',
    phrase: 'kills {name?another player}'
  },
  'trigger.player_died': {
    name: 'Player died',
    blurb: 'A player dies, by any or a specific cause.',
    phrase: 'dies[ to {source}]'
  },
  'trigger.damage_taken': {
    name: 'Damage taken',
    blurb: 'Any damage delivered counts. Shield blocks do not.',
    phrase: 'takes damage[ from {source}]'
  },
  'trigger.damage_dealt': {
    name: 'Damage dealt',
    blurb: 'Any damage received counts. Shield blocks do not.',
    phrase: 'deals damage[ with {source}][ to {target}]'
  },
  'trigger.item_crafted': {
    name: 'Item crafted',
    blurb: 'Something leaves a crafting grid.',
    phrase: 'crafts {item?anything}'
  },
  'trigger.item_picked_up': {
    name: 'Item picked up',
    blurb: 'An item hits the inventory, however it got there.',
    phrase: 'picks up {item?an item}'
  },
  'trigger.item_dropped': {
    name: 'Item dropped',
    blurb: 'An item leaves the inventory by getting dropped.',
    phrase: 'drops {item?an item}'
  },
  'trigger.food_eaten': {
    name: 'Food eaten',
    blurb: 'Something is consumed. Optionally a specific food.',
    phrase: 'eats {item?something}'
  },
  'trigger.xp_gained': {
    name: 'XP gained',
    blurb: 'Fires on every gain, not on a total. Orbs add up fast.',
    phrase: 'gains XP'
  },
  'trigger.advancement_earned': {
    name: 'Advancement earned',
    blurb: 'Whenever an advancement is earned.',
    phrase: 'earns {advancement?an advancement}'
  },
  'trigger.dimension_changed': {
    name: 'Dimension changed',
    blurb: 'A portal is stepped through, either way.',
    phrase: 'changes dimension[ to {dimension}]'
  },
  'trigger.biome_changed': {
    name: 'Biome changed',
    blurb: 'Someone enteres a different biome.',
    phrase: 'enters {biome?a new biome}'
  },
  'trigger.height_crossed': {
    name: 'Height crossed',
    blurb: 'A player crosses the given Y level, either way.',
    phrase: 'crosses Y {y}'
  },
  'trigger.health_below': {
    name: 'Health below',
    blurb: 'Health dips under the given hearts.',
    phrase: 'drops below {hearts} hearts'
  },
  'trigger.hunger_below': {
    name: 'Hunger below',
    blurb: 'The hunger bar drops under the given points.',
    phrase: 'drops below {points} hunger'
  },
  'trigger.level_reached': {
    name: 'Level reached',
    blurb: 'A specific XP level is hit.',
    phrase: 'reaches level {level}'
  },
  'trigger.level_interval': {
    name: 'Level interval',
    blurb: 'Fires every N levels instead of once.',
    phrase: 'passes every {level} levels'
  },
  'trigger.slept': {
    name: 'Slept',
    blurb: 'Someone lays down in bed.',
    phrase: 'sleeps'
  },
  'trigger.jumped': {
    name: 'Jumped',
    blurb: 'Feet leave the ground. Fires a lot. You know this.',
    phrase: 'jumps'
  },
  'trigger.sneaked': {
    name: 'Sneaked',
    blurb: 'Shift is pressed. Also fires a lot.',
    phrase: 'sneaks'
  },
  'trigger.fish_caught': {
    name: 'Fish caught',
    blurb: 'The bobber goes down and something comes up.',
    phrase: 'catches a fish'
  },
  'trigger.villager_traded': {
    name: 'Villager traded',
    blurb: 'A trade completes. The villager remains unbothered.',
    phrase: 'trades with a villager'
  },
  'trigger.enchantment_applied': {
    name: 'Enchantment applied',
    blurb: 'Something comes out of the table or the anvil enchanted.',
    phrase: 'applies {enchantment?an enchantment}[ {level}]'
  },
  'trigger.item_smelted': {
    name: 'Item smelted',
    blurb: 'A furnace finishes a job.',
    phrase: 'smelts {item?something}'
  },
  'trigger.projectile_shot': {
    name: 'Projectile shot',
    blurb: 'An arrow, a trident, a snowball. Anything that leaves the hand.',
    phrase: 'shoots {projectile?a projectile}'
  },
  'trigger.mob_tamed': {
    name: 'Mob tamed',
    blurb: 'A new friend is made.',
    phrase: 'tames {mob?a mob}'
  },
  'trigger.mob_bred': {
    name: 'Mob bred',
    blurb: 'Two mobs produce a baby mob.',
    phrase: 'breeds {mob?a mob}'
  },
  'trigger.container_opened': {
    name: 'Container opened',
    blurb: 'A chest, a barrel, a shulker. Anything with a lid.',
    phrase: 'opens {container?a container}'
  },
  'trigger.item_used': {
    name: 'Item used',
    blurb: 'Right-click with something in hand.',
    phrase: 'uses {item?an item}'
  },
  'trigger.block_interacted': {
    name: 'Block interacted',
    blurb: 'A block is right-clicked. Doors, buttons, crafting tables.',
    phrase: 'interacts with {block?a block}'
  },
  'trigger.started_gliding': {
    name: 'Started gliding',
    blurb: 'Elytra deployed.',
    phrase: 'starts gliding'
  },
  'trigger.mounted': {
    name: 'Mounted',
    blurb: 'A horse, a boat, a pig with a saddle.',
    phrase: 'mounts {mob?a mob}'
  },
  'trigger.effect_gained': {
    name: 'Effect gained',
    blurb: 'A status effect lands, from any source at all.',
    phrase: 'gains {effect?a status effect}'
  },
  'trigger.tool_broke': {
    name: 'Tool broke',
    blurb: 'Durability hits zero.',
    phrase: 'breaks {item?a tool}'
  },
  'trigger.crit_landed': {
    name: 'Critical hit landed',
    blurb: 'The jump-attack sparkle.',
    phrase: 'lands a critical hit'
  },
  'trigger.shield_blocked': {
    name: 'Shield blocked',
    blurb: 'A hit is absorbed by a shield.',
    phrase: 'blocks a hit with a shield'
  },
  'trigger.weather_changed': {
    name: 'Weather changed',
    blurb: 'The sky changes its mind.',
    phrase: 'the weather changes[ to {weather}]'
  },
  'trigger.time_of_day': {
    name: 'Time of day',
    blurb: 'The world clock reaches a set time.',
    phrase: 'the world clock reaches {time}'
  },
  'trigger.fixed_interval': {
    name: 'Fixed interval',
    blurb: 'A metronome: fires every N seconds.',
    phrase: 'Every {seconds} seconds',
    lead: ''
  },
  'trigger.chat_message': {
    name: 'Chat message',
    blurb: 'Someone says a certain word. Or anything at all.',
    phrase: 'says {message?something in chat}'
  },
  'trigger.game_beaten': {
    name: 'Game beaten',
    blurb: 'Roll the end credits.',
    phrase: 'beats the game'
  },

  /* ---------- effects ---------- */

  'effect.apply_status_effect': {
    name: 'Apply status effect',
    blurb: 'Grant a potion effect, a blessing or a curse.',
    phrase: 'gets {effect}[ {amplifier}][ for {duration}s]'
  },
  'effect.remove_item_slot': {
    name: 'Remove item slot',
    blurb: 'The item in the hand slot gets removed.',
    phrase: 'loses whatever they are holding'
  },
  'effect.drop_held_item': {
    name: 'Drop held item',
    blurb: 'Whatever is in hand hits the floor.',
    phrase: 'drops whatever they are holding'
  },
  'effect.drop_inventory': {
    name: 'Drop inventory',
    blurb: 'Everything on the floor.',
    phrase: 'drops their whole inventory'
  },
  'effect.give_random_item': {
    name: 'Give random item',
    blurb: 'A gift. Quality not guaranteed.',
    phrase: 'gets a random item'
  },
  'effect.give_item': {
    name: 'Give item',
    blurb: 'Hand over an item, any amount.',
    phrase: 'gets {amount?1}x {item}'
  },
  'effect.teleport_random': {
    name: 'Teleport randomly',
    blurb: 'Yeet to a random spot within a radius.',
    phrase: 'is teleported somewhere random[ within {radius} blocks]'
  },
  'effect.teleport_up': {
    name: 'Teleport up',
    blurb: 'Straight up. Gravity handles the rest.',
    phrase: 'is teleported {blocks?a long way} blocks upward'
  },
  'effect.spawn_mob': {
    name: 'Spawn mob',
    blurb: 'Conjure company at the player.',
    phrase: 'has {count?1}[{baby} baby] {mob} spawned on them'
  },
  'effect.ignite': {
    name: 'Ignite',
    blurb: 'Set the player alight.',
    phrase: 'catches fire[ for {seconds}s]'
  },
  'effect.damage': {
    name: 'Damage',
    blurb: 'Straight damage, in hearts.',
    phrase: 'takes {hearts?1} hearts of damage'
  },
  'effect.heal': {
    name: 'Heal',
    blurb: 'Heal the player',
    phrase: 'is healed[ {hearts} hearts]'
  },
  'effect.change_max_health': {
    name: 'Change max health',
    blurb: 'Move the health ceiling itself, up or down. Persists across death.',
    phrase: 'has their maximum health changed by {hearts} hearts'
  },
  'effect.drain_hunger': {
    name: 'Drain hunger',
    blurb: 'The bar goes down.',
    phrase: 'loses {amount?some} hunger'
  },
  'effect.restore_hunger': {
    name: 'Restore hunger',
    blurb: 'The bar comes back.',
    phrase: 'regains {amount?all their} hunger'
  },
  'effect.change_xp': {
    name: 'Change XP',
    blurb: 'Add, remove, or set outright, in points or in levels.',
    phrase: 'has their XP changed by {amount}[ levels{levels}]'
  },
  'effect.shuffle_hotbar': {
    name: 'Shuffle hotbar',
    blurb: 'Everything is still there. Nothing is where you left it.',
    phrase: 'has their hotbar shuffled'
  },
  'effect.swap_inventory': {
    name: 'Swap inventory',
    blurb: 'Trade everything with a random player, consent not required.',
    phrase: 'swaps inventories with a random player'
  },
  'effect.swap_position': {
    name: 'Swap position',
    blurb: 'Two players trade places.',
    phrase: 'swaps places with a random player'
  },
  'effect.clear_effects': {
    name: 'Clear effects',
    blurb: 'Every active effect goes, the good ones included.',
    phrase: 'loses all active effects'
  },
  'effect.lightning': {
    name: 'Lightning',
    blurb: 'Get smited.',
    phrase: 'is struck by lightning'
  },
  'effect.falling_anvil': {
    name: 'Falling anvil',
    blurb: 'The classics never die.',
    phrase: 'gets an anvil dropped on them[ from {height} blocks up]'
  },
  'effect.launch': {
    name: 'Launch',
    blurb: 'Upward, at speed. Fall damage may occur.',
    phrase: 'is launched into the air[ at strength {strength}]'
  },
  'effect.broadcast': {
    name: 'Broadcast',
    blurb: 'Put a line of text in the chat of every player.',
    phrase: 'sets off the message "{text}"'
  },
  'effect.play_sound': {
    name: 'Play sound',
    blurb: 'Any game sound. Use responsibly, or don\'t.',
    phrase: 'hears {sound}'
  },
  'effect.change_time': {
    name: 'Change time',
    blurb: 'Set the world clock.',
    phrase: 'the time is set to {value}'
  },
  'effect.change_weather': {
    name: 'Change weather',
    blurb: 'Set the sky.',
    phrase: 'the weather is set to {value}'
  },
  'effect.replace_held_random': {
    name: 'Replace held item',
    blurb: 'Whatever is in hand becomes something else entirely.',
    phrase: 'has their held item replaced with something random'
  },
  'effect.random_effect': {
    name: 'Random effect',
    blurb: 'Roll the dice: a random effect, good, bad.',
    phrase: 'gets a random {type?} effect[ for {seconds}s]'
  },
  'effect.freeze': {
    name: 'Freeze',
    blurb: 'Movement stops. The mobs, notably, keep going.',
    phrase: 'is frozen in place[ for {seconds}s]'
  },
  'effect.knockback': {
    name: 'Knockback',
    blurb: 'A shove in a direction nobody chose.',
    phrase: 'is knocked in a random direction[ at strength {strength}]'
  },
  'effect.explode': {
    name: 'Explode',
    blurb: 'A creeper, but on schedule.',
    phrase: 'explodes[ at power {power}]'
  },
  'effect.clear_inventory': {
    name: 'Clear inventory',
    blurb: 'Not dropped, gone.',
    phrase: 'loses their entire inventory'
  },
  'effect.repair_held_item': {
    name: 'Repair held item',
    blurb: 'Durability back, no anvil and no XP cost.',
    phrase: 'has their held item repaired[ by {amount}]'
  },
  'effect.damage_held_item': {
    name: 'Damage held item',
    blurb: 'Durability away, no mining required.',
    phrase: 'has their held item damaged[ by {amount}]'
  },
  'effect.kill': {
    name: 'Kill',
    blurb: 'Instant, unappealable death.',
    phrase: 'dies on the spot'
  },
  'effect.lose_challenge': {
    name: 'Lose the challenge',
    blurb: 'Ends the run as a loss, instantly.',
    phrase: 'the run ends as a loss'
  },

  /* ---------- goals ---------- */

  'goal.kill_mob': {
    name: 'Kill a mob',
    blurb: 'Win by slaying the named mob.',
    phrase: 'Kill {mob}'
  },
  'goal.obtain_item': {
    name: 'Obtain an item',
    blurb: 'Win the moment the item is picked up or crafted.',
    phrase: 'Obtain {item}'
  },
  'goal.earn_advancement': {
    name: 'Earn an advancement',
    blurb: 'Win by earning a specific advancement.',
    phrase: 'Earn the advancement {advancement}'
  },
  'goal.beat_game': {
    name: 'Beat the game',
    blurb: 'Win by beating the ender dragon and getting the end credits.',
    phrase: 'Beat the ender dragon'
  },

  /* ---------- modifiers ---------- */
  /* Modifiers are states, not events, so they read as a name plus a detail
     rather than as a sentence. An empty detail means the modifier speaks for
     itself. */

  'modifier.disable_jump': {
    name: 'Disable jumping',
    blurb: 'The ground is your home now.',
    detail: ''
  },
  'modifier.disable_item_use': {
    name: 'Disable item use',
    blurb: 'One item becomes decorative. Or all of them.',
    detail: '{item?all items}'
  },
  'modifier.disable_interaction': {
    name: 'Disable interaction',
    blurb: 'A block or item you simply may not touch.',
    detail: '{target}'
  },
  'modifier.no_natural_regen': {
    name: 'No natural regen',
    blurb: 'Hearts no longer come back naturally.',
    detail: ''
  },
  'modifier.time_limit': {
    name: 'Time limit',
    blurb: 'The run ends as a loss when the clock runs out.',
    detail: '{minutes} minutes'
  },
  'modifier.randomize_block_drops': {
    name: 'Randomize block drops',
    blurb: 'Every block drops something else. Stone might be a diamond.',
    detail: 'seed {seed?random}[, per player{per_player}]'
  },
  'modifier.randomize_mob_drops': {
    name: 'Randomize mob drops',
    blurb: 'Every mob drops something else. Rotten flesh might be a music disc.',
    detail: 'seed {seed?random}[, per player{per_player}]'
  },
  'modifier.buff_hostile_mobs': {
    name: 'Buff hostile mobs',
    blurb: 'Everything that wants you dead is better at it.',
    detail: ''
  },
  'modifier.status_effect': {
    name: 'Persistent status effect',
    blurb: 'A permanent potion effect for the whole run.',
    detail: '{effect}[ {amplifier}]'
  },
  'modifier.keep_inventory': {
    name: 'Keep inventory',
    blurb: 'Death keeps your stuff.',
    detail: ''
  },
  'modifier.no_hunger_drain': {
    name: 'No hunger drain',
    blurb: 'The bar stays put. One less thing to worry about.',
    detail: ''
  },
  'modifier.share_inventory': {
    name: 'Share inventory',
    blurb: 'Everyone shares the same inventory. Have fun.',
    detail: ''
  }
};
