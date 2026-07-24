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
    blurb: 'Fires when a player breaks a block. Name a block to watch only that one, or leave it open for any.',
    phrase: 'breaks {block?a block}'
  },
  'trigger.block_placed': {
    name: 'Block placed',
    blurb: 'Fires when a player places a block. Name a block to watch only that one, or leave it open for any.',
    phrase: 'places {block?a block}'
  },
  'trigger.mob_killed': {
    name: 'Mob killed',
    blurb: 'Fires when a player kills a mob. Killing another player counts as Player killed instead, not as this.',
    phrase: 'kills {mob?a mob}'
  },
  'trigger.kill_player': {
    name: 'Player killed',
    blurb: 'Fires when one player kills another. Name a victim to watch only kills on them.',
    phrase: 'kills {name?another player}'
  },
  'trigger.player_died': {
    name: 'Player died',
    blurb: 'Fires when a player dies, by any cause. Name a damage type to watch only deaths from it.',
    phrase: 'dies[ to {source}]'
  },
  'trigger.damage_taken': {
    name: 'Damage taken',
    blurb: 'Fires on every hit a player takes, including hits that deal no damage. A hit stopped by a shield does not count.',
    phrase: 'takes damage[ from {source}]'
  },
  'trigger.damage_dealt': {
    name: 'Damage dealt',
    blurb: 'Fires when a player damages something. A hit stopped by a shield does not count. Filter by damage type, by what was hit, or both.',
    phrase: 'deals damage[ with {source}][ to {target}]'
  },
  'trigger.item_crafted': {
    name: 'Item crafted',
    blurb: 'Fires when a player takes the result out of a crafting grid.',
    phrase: 'crafts {item?anything}'
  },
  'trigger.item_picked_up': {
    name: 'Item picked up',
    blurb: 'Fires when an item enters a player\'s inventory, however it got there.',
    phrase: 'picks up {item?an item}'
  },
  'trigger.item_dropped': {
    name: 'Item dropped',
    blurb: 'Fires when a player drops an item out of their inventory.',
    phrase: 'drops {item?an item}'
  },
  'trigger.food_eaten': {
    name: 'Food eaten',
    blurb: 'Fires when a player finishes eating. Covers every item the game treats as food.',
    phrase: 'eats {item?something}'
  },
  'trigger.xp_gained': {
    name: 'XP gained',
    blurb: 'Fires each time a player picks up experience, once per pickup rather than on a total. Orbs arrive in bursts.',
    phrase: 'gains XP'
  },
  'trigger.advancement_earned': {
    name: 'Advancement earned',
    blurb: 'Fires when a player earns an advancement. Only the five real trees count; recipe unlocks are ignored.',
    phrase: 'earns {advancement?an advancement}'
  },
  'trigger.dimension_changed': {
    name: 'Dimension changed',
    blurb: 'Fires when a player arrives in another dimension, in either direction.',
    phrase: 'changes dimension[ to {dimension}]'
  },
  'trigger.biome_changed': {
    name: 'Biome changed',
    blurb: 'Fires when a player walks into a different biome.',
    phrase: 'enters {biome?a new biome}'
  },
  'trigger.height_crossed': {
    name: 'Height crossed',
    blurb: 'Fires when a player crosses the given Y level, going up or going down.',
    phrase: 'crosses Y {y}'
  },
  'trigger.health_below': {
    name: 'Health below',
    blurb: 'Fires when a player\'s health drops under the given number of hearts.',
    phrase: 'drops below {hearts} hearts'
  },
  'trigger.hunger_below': {
    name: 'Hunger below',
    blurb: 'Fires when a player\'s hunger drops under the given number of points. The bar holds 20.',
    phrase: 'drops below {points} hunger'
  },
  'trigger.level_reached': {
    name: 'Level reached',
    blurb: 'Fires when a player reaches the given experience level.',
    phrase: 'reaches level {level}'
  },
  'trigger.level_interval': {
    name: 'Level interval',
    blurb: 'Fires every time a player passes another multiple of the given level, so it repeats through the run.',
    phrase: 'passes every {level} levels'
  },
  'trigger.slept': {
    name: 'Slept',
    blurb: 'Fires when a player sleeps in a bed.',
    phrase: 'sleeps'
  },
  'trigger.jumped': {
    name: 'Jumped',
    blurb: 'Fires every time a player jumps, which in normal play is constantly.',
    phrase: 'jumps'
  },
  'trigger.sneaked': {
    name: 'Sneaked',
    blurb: 'Fires when a player starts sneaking, which in normal play is often.',
    phrase: 'sneaks'
  },
  'trigger.fish_caught': {
    name: 'Fish caught',
    blurb: 'Fires when a player reels something in with a fishing rod.',
    phrase: 'catches a fish'
  },
  'trigger.villager_traded': {
    name: 'Villager traded',
    blurb: 'Fires when a player completes a trade with a villager.',
    phrase: 'trades with a villager'
  },
  'trigger.enchantment_applied': {
    name: 'Enchantment applied',
    blurb: 'Fires when a player enchants an item at a table or an anvil. Filter by enchantment, by level, or both.',
    phrase: 'applies {enchantment?an enchantment}[ {level}]'
  },
  'trigger.item_smelted': {
    name: 'Item smelted',
    blurb: 'Fires when a player takes a finished item out of a furnace.',
    phrase: 'smelts {item?something}'
  },
  'trigger.projectile_shot': {
    name: 'Projectile shot',
    blurb: 'Fires when a player launches a projectile: an arrow, a trident, a snowball, an ender pearl.',
    phrase: 'shoots {projectile?a projectile}'
  },
  'trigger.mob_tamed': {
    name: 'Mob tamed',
    blurb: 'Fires when a player tames a mob.',
    phrase: 'tames {mob?a mob}'
  },
  'trigger.mob_bred': {
    name: 'Mob bred',
    blurb: 'Fires when a player breeds two mobs and a baby appears.',
    phrase: 'breeds {mob?a mob}'
  },
  'trigger.container_opened': {
    name: 'Container opened',
    blurb: 'Fires when a player opens a container screen. Chests, furnaces, anvils and every other block with an interface.',
    phrase: 'opens {container?a container}'
  },
  'trigger.item_used': {
    name: 'Item used',
    blurb: 'Fires when a player uses the item in their hand.',
    phrase: 'uses {item?an item}'
  },
  'trigger.block_interacted': {
    name: 'Block interacted',
    blurb: 'Fires when a player right-clicks a block: a door, a button, a crafting table.',
    phrase: 'interacts with {block?a block}'
  },
  'trigger.started_gliding': {
    name: 'Started gliding',
    blurb: 'Fires when a player opens their elytra and starts gliding.',
    phrase: 'starts gliding'
  },
  'trigger.mounted': {
    name: 'Mounted',
    blurb: 'Fires when a player starts riding something. Horses, boats and minecarts all count, so this matches any entity rather than only mobs.',
    phrase: 'mounts {vehicle?a vehicle}'
  },
  'trigger.effect_gained': {
    name: 'Effect gained',
    blurb: 'Fires when a status effect lands on a player, from a potion, a beacon, a mob or anything else.',
    phrase: 'gains {effect?a status effect}'
  },
  'trigger.tool_broke': {
    name: 'Tool broke',
    blurb: 'Fires when an item in a player\'s hands runs out of durability and breaks.',
    phrase: 'breaks {item?a tool}'
  },
  'trigger.crit_landed': {
    name: 'Critical hit landed',
    blurb: 'Fires when a player lands a critical hit, the one that comes from attacking while falling.',
    phrase: 'lands a critical hit'
  },
  'trigger.shield_blocked': {
    name: 'Shield blocked',
    blurb: 'Fires when a player stops a hit with a shield. That hit does not count as damage taken.',
    phrase: 'blocks a hit with a shield'
  },
  'trigger.weather_changed': {
    name: 'Weather changed',
    blurb: 'Fires when the world\'s weather changes. It watches the world rather than any player, so it has no scope.',
    phrase: 'the weather changes[ to {weather}]'
  },
  'trigger.time_of_day': {
    name: 'Time of day',
    blurb: 'Fires when the world clock reaches the given time. It watches the world rather than any player, so it has no scope.',
    phrase: 'the world clock reaches {time}'
  },
  'trigger.fixed_interval': {
    name: 'Fixed interval',
    blurb: 'Fires once every given number of seconds, for as long as the run lasts.',
    phrase: 'Every {seconds} seconds',
    lead: ''
  },
  'trigger.chat_message': {
    name: 'Chat message',
    blurb: 'Fires when a player sends a chat message. Give text to match only messages containing it.',
    phrase: 'says {message?something in chat}'
  },
  'trigger.game_beaten': {
    name: 'Game beaten',
    blurb: 'Fires when a player beats the game, at the moment the end credits begin.',
    phrase: 'beats the game'
  },

  /* ---------- effects ---------- */

  'effect.apply_status_effect': {
    name: 'Apply status effect',
    blurb: 'Applies a status effect for a set number of seconds, at a strength you choose.',
    phrase: 'gets {effect}[ {amplifier}][ for {duration}s]'
  },
  'effect.remove_item_slot': {
    name: 'Remove item slot',
    blurb: 'Deletes whatever is in the selected hotbar slot. The item is destroyed, not dropped.',
    phrase: 'loses whatever they are holding'
  },
  'effect.drop_held_item': {
    name: 'Drop held item',
    blurb: 'Drops the held item on the ground, where anyone can pick it up.',
    phrase: 'drops whatever they are holding'
  },
  'effect.drop_inventory': {
    name: 'Drop inventory',
    blurb: 'Drops the entire inventory on the ground, where anyone can pick it up.',
    phrase: 'drops their whole inventory'
  },
  'effect.give_random_item': {
    name: 'Give random item',
    blurb: 'Gives an item picked at random from everything in the game. How many arrives is random too, up to a full stack of whatever came up, so ender pearls give at most 16 and a boat gives 1. Switch to a fixed amount to set the number yourself.',
    phrase: 'gets [{amount} of ]a random item'
  },
  'effect.give_item': {
    name: 'Give item',
    blurb: 'Gives a specific item, in whatever amount you set.',
    phrase: 'gets {amount?1}x {item}'
  },
  'effect.teleport_random': {
    name: 'Teleport randomly',
    blurb: 'Teleports the player to a random spot within the given radius.',
    phrase: 'is teleported somewhere random[ within {radius} blocks]'
  },
  'effect.teleport_up': {
    name: 'Teleport up',
    blurb: 'Teleports the player straight up by the given number of blocks. What happens on the way down is not this effect\'s problem.',
    phrase: 'is teleported {blocks?a long way} blocks upward'
  },
  'effect.spawn_mob': {
    name: 'Spawn mob',
    blurb: 'Spawns the named mob at the player\'s feet, as many times as you set. Ageable mobs can be spawned as babies.',
    phrase: 'has {count?1}[{baby} baby] {mob} spawned on them'
  },
  'effect.ignite': {
    name: 'Ignite',
    blurb: 'Sets the player on fire for the given number of seconds.',
    phrase: 'catches fire[ for {seconds}s]'
  },
  'effect.damage': {
    name: 'Damage',
    blurb: 'Deals the given number of hearts as damage. Armor still applies.',
    phrase: 'takes {hearts?1} hearts of damage'
  },
  'effect.heal': {
    name: 'Heal',
    blurb: 'Restores the given number of hearts, up to the player\'s maximum.',
    phrase: 'is healed[ {hearts} hearts]'
  },
  'effect.change_max_health': {
    name: 'Change max health',
    blurb: 'Raises or lowers the maximum number of hearts a player has. The change stays through death and rejoining.',
    phrase: 'has their maximum health changed by {hearts} hearts'
  },
  'effect.drain_hunger': {
    name: 'Drain hunger',
    blurb: 'Removes the given number of hunger points from the bar.',
    phrase: 'loses {amount?some} hunger'
  },
  'effect.restore_hunger': {
    name: 'Restore hunger',
    blurb: 'Restores the given number of hunger points, or fills the bar when left blank.',
    phrase: 'regains {amount?all their} hunger'
  },
  'effect.change_xp': {
    name: 'Change XP',
    blurb: 'Adds, removes or sets experience, counted either in points or in whole levels.',
    phrase: 'has their XP changed by {amount}[ levels{levels}]'
  },
  'effect.shuffle_hotbar': {
    name: 'Shuffle hotbar',
    blurb: 'Reorders the hotbar at random. Nothing is lost, and nothing is where it was.',
    phrase: 'has their hotbar shuffled'
  },
  'effect.swap_inventory': {
    name: 'Swap inventory',
    blurb: 'Swaps the player\'s entire inventory with that of another player picked at random.',
    phrase: 'swaps inventories with a random player'
  },
  'effect.swap_position': {
    name: 'Swap position',
    blurb: 'Teleports the player to another player picked at random, and that player to where they were.',
    phrase: 'swaps places with a random player'
  },
  'effect.clear_effects': {
    name: 'Clear effects',
    blurb: 'Removes every status effect the player has, helpful ones included.',
    phrase: 'loses all active effects'
  },
  'effect.lightning': {
    name: 'Lightning',
    blurb: 'Strikes the player with lightning, which sets fires and damages as real lightning does.',
    phrase: 'is struck by lightning'
  },
  'effect.falling_anvil': {
    name: 'Falling anvil',
    blurb: 'Drops an anvil onto the player from the given height above them.',
    phrase: 'gets an anvil dropped on them[ from {height} blocks up]'
  },
  'effect.launch': {
    name: 'Launch',
    blurb: 'Throws the player upward at the given strength. The landing is left to fall damage.',
    phrase: 'is launched into the air[ at strength {strength}]'
  },
  'effect.broadcast': {
    name: 'Broadcast',
    blurb: 'Sends a line of text to every player in the chat.',
    phrase: 'sets off the message "{text}"'
  },
  'effect.play_sound': {
    name: 'Play sound',
    blurb: 'Plays any sound in the game to the player.',
    phrase: 'hears {sound}'
  },
  'effect.change_time': {
    name: 'Change time',
    blurb: 'Sets the world clock. It changes the world rather than any player, so it has no scope.',
    phrase: 'the time is set to {value}'
  },
  'effect.change_weather': {
    name: 'Change weather',
    blurb: 'Sets the weather. It changes the world rather than any player, so it has no scope.',
    phrase: 'the weather is set to {value}'
  },
  'effect.replace_held_random': {
    name: 'Replace held item',
    blurb: 'Replaces the held item with a random one. The original is gone.',
    phrase: 'has their held item replaced with something random'
  },
  'effect.random_effect': {
    name: 'Random effect',
    blurb: 'Applies a status effect picked at random. Restrict it to helpful ones, harmful ones, or leave it open to both.',
    phrase: 'gets a random {type?} effect[ for {seconds}s]'
  },
  'effect.freeze': {
    name: 'Freeze',
    blurb: 'Holds the player still for the given number of seconds. Everything else in the world keeps moving.',
    phrase: 'is frozen in place[ for {seconds}s]'
  },
  'effect.knockback': {
    name: 'Knockback',
    blurb: 'Shoves the player in a random direction at the given strength.',
    phrase: 'is knocked in a random direction[ at strength {strength}]'
  },
  'effect.explode': {
    name: 'Explode',
    blurb: 'Sets off an explosion at the player, at the given power. It damages terrain the way a creeper does.',
    phrase: 'explodes[ at power {power}]'
  },
  'effect.clear_inventory': {
    name: 'Clear inventory',
    blurb: 'Deletes the entire inventory. Nothing drops and nothing can be recovered.',
    phrase: 'loses their entire inventory'
  },
  'effect.repair_held_item': {
    name: 'Repair held item',
    blurb: 'Restores durability to the held item, with no anvil and no experience cost.',
    phrase: 'has their held item repaired[ by {amount}]'
  },
  'effect.damage_held_item': {
    name: 'Damage held item',
    blurb: 'Takes durability off the held item, which can break it outright.',
    phrase: 'has their held item damaged[ by {amount}]'
  },
  'effect.kill': {
    name: 'Kill',
    blurb: 'Kills the player immediately, ignoring armor and health.',
    phrase: 'dies on the spot'
  },
  'effect.lose_challenge': {
    runControl: true,
    name: 'Lose the challenge',
    blurb: 'Knocks whoever it lands on out of the run. The run itself is lost once nobody is left playing.',
    phrase: 'loses the challenge'
  },
  'effect.win_challenge': {
    runControl: true,
    name: 'Win the challenge',
    blurb: 'Finishes the run. Pair it with any trigger to make that the win condition.',
    phrase: 'finishes the challenge',
    /* Labels for a parameter restricted to a closed set. The values are the
       frozen vocabulary; these are what a person reads instead. */
    values: {
      end: {
        on_first_completion: 'End on the first finish',
        after_all_complete: 'End after everyone finishes'
      }
    }
  },

  /* ---------- modifiers ---------- */
  /* Modifiers are states, not events, so they read as a name plus a detail
     rather than as a sentence. An empty detail means the modifier speaks for
     itself. */

  'modifier.disable_jump': {
    name: 'Disable jumping',
    blurb: 'Players cannot jump for the whole run. Enforced through the jump-strength attribute, so drinking milk does not undo it.',
    detail: ''
  },
  'modifier.disable_item_use': {
    name: 'Disable item use',
    blurb: 'Using the named item does nothing, for the whole run. Leave it blank to disable every item.',
    detail: '{item?all items}'
  },
  'modifier.disable_interaction': {
    name: 'Disable interaction',
    blurb: 'Right-clicking the named block does nothing, for the whole run.',
    detail: '{target}'
  },
  'modifier.no_natural_regen': {
    name: 'No natural regen',
    blurb: 'Health never comes back on its own. Food, potions and other healing still work.',
    detail: ''
  },
  'modifier.time_limit': {
    name: 'Time limit',
    blurb: 'The run clock counts down instead of up, and the run is lost when it reaches zero.',
    detail: '{minutes} minutes'
  },
  'modifier.randomize_block_drops': {
    name: 'Randomize block drops',
    blurb: 'Every block drops some other item instead of its own. The same seed gives the same shuffle twice, and it can be rolled separately for each player.',
    detail: 'seed {seed?random}[, per player{per_player}]'
  },
  'modifier.randomize_mob_drops': {
    name: 'Randomize mob drops',
    blurb: 'Every mob drops some other item instead of its own. The same seed gives the same shuffle twice, and it can be rolled separately for each player.',
    detail: 'seed {seed?random}[, per player{per_player}]'
  },
  'modifier.scale_hostile_mobs': {
    name: 'Scale hostile mobs',
    blurb: 'Multiplies the health and attack damage of every hostile mob. Above 1 makes them harder, below 1 makes them easier, and 1 leaves them as they are.',
    detail: 'x{multiplier}'
  },
  'modifier.status_effect': {
    name: 'Persistent status effect',
    blurb: 'Keeps a status effect applied for the whole run. It is reapplied if anything removes it.',
    detail: '{effect}[ {amplifier}]'
  },
  'modifier.keep_inventory': {
    name: 'Keep inventory',
    blurb: 'Items and experience stay with the player through death instead of dropping.',
    detail: ''
  },
  'modifier.no_hunger_drain': {
    name: 'No hunger drain',
    blurb: 'The hunger bar never empties, whatever the player does.',
    detail: ''
  },
  'modifier.share_inventory': {
    name: 'Share inventory',
    blurb: 'The scoped players all use one inventory: hotbar, main slots, armor and offhand. Each keeps their own selected slot, and the item on the cursor stays private.',
    detail: ''
  }
};
