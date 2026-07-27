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

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    private static SharedPool.Member member(UUID id, double value) {
        return new SharedPool.Member(id, value, FULL);
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
    @DisplayName("gains take the largest alone for a pool that regenerates, like health")
    void gainsDoNotStackWhenRegenerated() {
        SharedPool pool = groupOfTwo(false, 10.0);
        pool.claimTick(GROUP, 1);

        OptionalDouble settled = pool.settle(GROUP,
                List.of(member(alice, 13.0), member(bob, 12.0)));

        // Both regenerated on their own; a shared bar comes back at the rate of
        // the quickest, not at the sum, or a pair would heal twice as fast.
        assertEquals(13.0, settled.getAsDouble());
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
                new SharedPool.Member(alice, FULL, FULL),
                new SharedPool.Member(bob, FULL, 6.0)));

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
