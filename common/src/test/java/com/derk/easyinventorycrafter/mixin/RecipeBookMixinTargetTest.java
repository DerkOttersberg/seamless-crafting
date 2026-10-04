package com.derk.easyinventorycrafter.mixin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import org.junit.jupiter.api.Test;

/** Guards the declaring-class boundary that Mixin requires for private shadows. */
class RecipeBookMixinTargetTest {
    @Test
    void inheritedPrivateRecipeBookFieldIsAccessedOnlyFromItsDeclaringTargetMixin() {
        assertDoesNotThrow(() -> RecipeUpdateListener.class.getMethod("getRecipeBookComponent"));
        assertDoesNotThrow(() -> CraftingScreen.class.getDeclaredField("recipeBookComponent"));
        assertDoesNotThrow(() -> InventoryScreen.class.getDeclaredField("recipeBookComponent"));
        assertFalse(declaresRecipeBookComponent(AbstractRecipeBookScreenMixin.class));
        assertFalse(declaresRecipeBookComponent(CraftingScreenMixin.class));
        assertFalse(declaresRecipeBookComponent(InventoryScreenMixin.class));
    }

    private static boolean declaresRecipeBookComponent(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .anyMatch(field -> field.getName().equals("recipeBookComponent"));
    }
}
