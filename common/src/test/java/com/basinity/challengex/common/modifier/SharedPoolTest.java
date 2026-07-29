package com.basinity.challengex.common.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.Scope;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SharedPoolTest {

    private static final String GROUP = "*";
    private static final double FULL = 20.0;

    // Fixed rather than random, and in order: a pool that does not add gains up
    // nominates the lowest id among the living as its pacer, so random ids left
    // it to chance which of these two the regeneration tests were describing.
    private final UUID alice = new UUID(0L, 1L);
    private final UUID bob = new UUID(0L, 2L);

    private static SharedPool.Member member(UUID id, double value) {
        return new SharedPool.Member(id, value, FULL, true);
    }

    private static SharedPool.Member down(UUID id) {
        return new SharedPool.Member(id, 0.0, FULL, false);
    }

    /** Seeds a two-member group both sitting at {@code start}. */
    private SharedPool groupOfTwo(boolean gainsAddUp, double start) {
        SharedPool pool = new SharedPool(gainsAddUp);
        pool.addMember(GROUP, alice);
        pool.seed(GROUP, start);
        pool.recordSettled(alice, start);
        pool.addMember(GROUP, bob);
        pool.recordSettled(bob, start);
        return pool;
    }

    @Test
    @DisplayName("two members losing in the same tick both cost the pool")
    void lossesAddUp() {
        SharedPool pool = groupOfTwo(true, FULL);
        assertTrue(pool.claimTick(GROUP, 1));

        OptionalDouble settled = pool.settle(GROUP,
                List.of(member(alice, 17.0), member(bob, 18.0)));

        // Down three and down two, so the pool is down five, not down three.
        assertEquals(15.0, settled.getAsDouble());
    }

    @Test
    @DisplayName("gains add up for a pool that collects, like food or experience")
    void gainsAddUpWhenCollected() {
        SharedPool pool = groupOfTwo(true, 10.0);
        pool.claimTick(GROUP, 1);

        OptionalDouble settled = pool.settle(GROUP,
                List.of(member(alice, 13.0), member(bob, 12.0)));

        assertEquals(15.0, settled.getAsDouble());
    }

    @Test
    @DisplayName("only one member's gains move a pool that regenerates, like health")
    void gainsDoNotStackWhenRegenerated() {
        SharedPool pool = groupOfTwo(false, 10.0);
        pool.claimTick(GROUP, 1);

        OptionalDouble settled = pool.settle(GROUP,
                List.of(member(alice, 13.0), member(bob, 12.0)));

        // One nominated member's gain counts. Both regenerated on their own, and
        // a shared bar has to come back at one player's rate however many share it.
        assertEquals(13.0, settled.getAsDouble());
    }

    @Test
    @DisplayName("staggered regeneration does not double the rate over successive ticks")
    void regenerationStaysAtOnePlayersRateAcrossTicks() {
        SharedPool pool = groupOfTwo(false, 10.0);

        // Members regenerate on their own timers, so their gains land on
        // different ticks. Taking the largest gain of each tick separately
        // counts both, which is what healed a pair twice as fast in play.
        pool.claimTick(GROUP, 1);
        double afterAlice = pool.settle(GROUP,
                List.of(member(alice, 11.0), member(bob, 10.0))).getAsDouble();
        pool.recordSettled(alice, afterAlice);
        pool.recordSettled(bob, afterAlice);

        pool.claimTick(GROUP, 2);
        double afterBob = pool.settle(GROUP,
                List.of(member(alice, afterAlice), member(bob, afterAlice + 1.0))).getAsDouble();

        assertEquals(11.0, afterAlice, "the first regeneration counts");
        assertEquals(11.0, afterBob, "the second, from the other member, does not");
    }

    @Test
    @DisplayName("a member going down empties the pool, so one shared bar is one shared life")
    void aDeathEmptiesThePool() {
        SharedPool pool = groupOfTwo(true, 4.0);
        pool.claimTick(GROUP, 1);

        // The blow that killed them took their value with it, so the loss can
        // never be read back. The death itself has to be the signal.
        OptionalDouble settled = pool.settle(GROUP, List.of(member(alice, 4.0), down(bob)));

        assertEquals(0.0, settled.getAsDouble());
    }

    @Test
    @DisplayName("a member lying on the respawn screen does not empty the pool again")
    void lyingThereIsNotAFreshDeath() {
        SharedPool pool = groupOfTwo(true, 4.0);

        // Bob goes down and takes the pool with him.
        pool.claimTick(GROUP, 1);
        assertEquals(0.0, pool.settle(GROUP, List.of(member(alice, 4.0), down(bob))).getAsDouble());

        // Both down: the pool is forgotten.
        pool.claimTick(GROUP, 2);
        assertTrue(pool.settle(GROUP, List.of(down(alice), down(bob))).isEmpty());

        // Alice gets back up while Bob is still choosing to respawn. Counting
        // him as freshly dead here is what killed her again the instant she
        // clicked respawn.
        pool.claimTick(GROUP, 3);
        double seeded = pool.settle(GROUP, List.of(member(alice, FULL), down(bob))).getAsDouble();
        assertEquals(FULL, seeded, "she comes back on a full bar");
        pool.recordSettled(alice, seeded);

        pool.claimTick(GROUP, 4);
        double next = pool.settle(GROUP, List.of(member(alice, FULL), down(bob))).getAsDouble();
        assertEquals(FULL, next, "and stays up while he lies there");
    }

    @Test
    @DisplayName("a second, later death empties the pool again")
    void dyingAgainStillCounts() {
        SharedPool pool = groupOfTwo(true, FULL);

        pool.claimTick(GROUP, 1);
        assertEquals(0.0, pool.settle(GROUP, List.of(member(alice, FULL), down(bob))).getAsDouble());

        // Everybody back up, pool reseeded.
        pool.claimTick(GROUP, 2);
        double back = pool.settle(GROUP, List.of(member(alice, FULL), member(bob, FULL))).getAsDouble();
        pool.recordSettled(alice, back);
        pool.recordSettled(bob, back);

        pool.claimTick(GROUP, 3);
        assertEquals(0.0, pool.settle(GROUP, List.of(member(alice, FULL), down(bob))).getAsDouble(),
                "going down a second time is a fresh death");
    }

    @Test
    @DisplayName("once everybody is down the pool is forgotten rather than held at zero")
    void everybodyDownForgetsThePool() {
        SharedPool pool = groupOfTwo(true, 4.0);
        pool.claimTick(GROUP, 1);

        assertTrue(pool.settle(GROUP, List.of(down(alice), down(bob))).isEmpty());
        assertFalse(pool.hasPool(GROUP));
    }

    @Test
    @DisplayName("a loss and a gain in the same tick both count")
    void lossesAndGainsCombine() {
        SharedPool pool = groupOfTwo(true, 10.0);
        pool.claimTick(GROUP, 1);

        OptionalDouble settled = pool.settle(GROUP,
                List.of(member(alice, 6.0), member(bob, 12.0)));

        assertEquals(8.0, settled.getAsDouble());
    }

    @Test
    @DisplayName("a group settles once a tick, whoever asks first")
    void settlesOncePerTick() {
        SharedPool pool = groupOfTwo(true, FULL);

        assertTrue(pool.claimTick(GROUP, 7));
        assertFalse(pool.claimTick(GROUP, 7));
        assertTrue(pool.claimTick(GROUP, 8));
    }

    @Test
    @DisplayName("the pool is held to the smallest ceiling in the group")
    void cappedBySmallestCeiling() {
        SharedPool pool = groupOfTwo(true, FULL);
        pool.claimTick(GROUP, 1);

        OptionalDouble settled = pool.settle(GROUP, List.of(
                new SharedPool.Member(alice, FULL, FULL, true),
                new SharedPool.Member(bob, FULL, 6.0, true)));

        assertEquals(6.0, settled.getAsDouble());
    }

    @Test
    @DisplayName("the pool never goes below zero")
    void neverNegative() {
        SharedPool pool = groupOfTwo(true, 2.0);
        pool.claimTick(GROUP, 1);

        OptionalDouble settled = pool.settle(GROUP,
                List.of(member(alice, 0.0), member(bob, 0.0)));

        assertEquals(0.0, settled.getAsDouble());
    }

    @Test
    @DisplayName("a member who was not there last settle adopts rather than contributing")
    void newcomerDoesNotContribute() {
        SharedPool pool = new SharedPool(true);
        pool.addMember(GROUP, alice);
        pool.seed(GROUP, 10.0);
        pool.recordSettled(alice, 10.0);
        // Bob joins with a full bar and no recorded history.
        pool.addMember(GROUP, bob);
        pool.claimTick(GROUP, 1);

        OptionalDouble settled = pool.settle(GROUP,
                List.of(member(alice, 10.0), member(bob, FULL)));

        // Bob's 20 is not a gain of ten; he takes the pool's value instead.
        assertEquals(10.0, settled.getAsDouble());
    }

    @Test
    @DisplayName("an empty group forgets its pool, so the next member seeds a fresh one")
    void emptyGroupForgetsThePool() {
        SharedPool pool = groupOfTwo(true, 4.0);
        pool.claimTick(GROUP, 1);

        assertTrue(pool.settle(GROUP, List.of()).isEmpty());
        assertFalse(pool.hasPool(GROUP));

        // The first back on their feet starts it over rather than inheriting the
        // zero that emptied it.
        pool.claimTick(GROUP, 2);
        assertEquals(FULL, pool.settle(GROUP, List.of(member(alice, FULL))).getAsDouble());
    }

    @Test
    @DisplayName("a group is forgotten once its last member leaves")
    void lastMemberLeavingClearsTheGroup() {
        SharedPool pool = groupOfTwo(true, FULL);

        pool.leave(GROUP, alice);
        assertTrue(pool.hasPool(GROUP));
        pool.leave(GROUP, bob);
        assertFalse(pool.hasPool(GROUP));
    }

    @Test
    @DisplayName("each named roster pools separately from every-player and from each other")
    void groupKeysSeparateRosters() {
        Modifier everyone = new Modifier("modifier.share_health", Map.of(),
                Optional.of(Scope.EVERY_PLAYER));
        Modifier pair = new Modifier("modifier.share_health", Map.of(),
                Optional.of(Scope.players("Pix", "Kettu")));
        Modifier samePair = new Modifier("modifier.share_health", Map.of(),
                Optional.of(Scope.players("Kettu", "Pix")));
        Modifier other = new Modifier("modifier.share_health", Map.of(),
                Optional.of(Scope.players("Basinity")));

        assertEquals("*", SharedPool.groupKey(everyone));
        // Order in the scope must not make two of the same roster different groups.
        assertEquals(SharedPool.groupKey(pair), SharedPool.groupKey(samePair));
        assertFalse(SharedPool.groupKey(pair).equals(SharedPool.groupKey(other)));
        assertFalse(SharedPool.groupKey(pair).equals(SharedPool.groupKey(everyone)));
    }
}
