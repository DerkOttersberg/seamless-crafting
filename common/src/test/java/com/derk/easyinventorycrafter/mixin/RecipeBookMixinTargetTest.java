package com.derk.easyinventorycrafter.mixin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.junit.jupiter.api.Test;

/** Guards the declaring-class boundary that Mixin requires for private shadows. */
class RecipeBookMixinTargetTest {
    @Test
    void inheritedPrivateRecipeBookFieldIsAccessedOnlyFromItsDeclaringTargetMixin() {
        assertDoesNotThrow(() -> AbstractRecipeBookScreen.class.getDeclaredField("recipeBookComponent"));
        assertFalse(declaresRecipeBookComponent(CraftingScreenMixin.class));
        assertFalse(declaresRecipeBookComponent(InventoryScreenMixin.class));
    }

    private static boolean declaresRecipeBookComponent(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .anyMatch(field -> field.getName().equals("recipeBookComponent"));
    }
}
