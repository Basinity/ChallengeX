package com.basinity.challengex.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RegistryTest {

    @Test
    void rejectsIdWithWrongCategoryPrefix() {
        Registry<TriggerDefinition> registry = new Registry<>("trigger");
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(new TriggerDefinition("effect.jump", true, List.of())));
    }

    @Test
    void rejectsIdThatIsNotLowerSnakeCase() {
        Registry<TriggerDefinition> registry = new Registry<>("trigger");
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(new TriggerDefinition("trigger.BlockBroken", true, List.of())));
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(new TriggerDefinition("trigger.block-broken", true, List.of())));
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(new TriggerDefinition("trigger.", true, List.of())));
    }

    @Test
    void rejectsDuplicateId() {
        Registry<TriggerDefinition> registry = new Registry<>("trigger");
        registry.register(new TriggerDefinition("trigger.jumped", true, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(new TriggerDefinition("trigger.jumped", true, List.of())));
    }

    @Test
    void findsRegisteredDefinitions() {
        Registry<TriggerDefinition> registry = new Registry<>("trigger");
        TriggerDefinition definition = new TriggerDefinition("trigger.jumped", true, List.of());
        registry.register(definition);

        assertEquals(definition, registry.require("trigger.jumped"));
        assertTrue(registry.find("trigger.sneaked").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> registry.require("trigger.sneaked"));
    }
}
