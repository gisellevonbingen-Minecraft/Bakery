package net.satisfy.bakery.compat.jei.category;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.satisfy.bakery.Bakery;
import net.satisfy.bakery.block.entity.CookingPotBlockEntity;
import net.satisfy.bakery.client.gui.CookingPotGui;
import net.satisfy.bakery.compat.jei.BakeryJEIPlugin;
import net.satisfy.bakery.recipe.CookingPotRecipe;
import net.satisfy.bakery.registry.ObjectRegistry;
import org.jetbrains.annotations.NotNull;

public class CookingPotCategory implements IRecipeCategory<CookingPotRecipe> {
    public static final RecipeType<CookingPotRecipe> COOKING_POT = RecipeType.create(Bakery.MOD_ID, "pot_cooking", CookingPotRecipe.class);
    public static final int WIDTH = 124;
    public static final int HEIGHT = 60;
    public static final int WIDTH_OF = 26;
    public static final int HEIGHT_OF = 13;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;
    private final Component localizedName;

    public CookingPotCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(CookingPotGui.BACKGROUND, WIDTH_OF, HEIGHT_OF, WIDTH, HEIGHT);
        this.arrow = helper.drawableBuilder(CookingPotGui.BACKGROUND, 178, 15, 18, 30)
                .buildAnimated(CookingPotBlockEntity.MAX_COOKING_TIME, IDrawableAnimated.StartDirection.LEFT, false);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, ObjectRegistry.SMALL_COOKING_POT.get().asItem().getDefaultInstance());
        this.localizedName = Component.translatable("rei.bakery.cooking_pot_category");
    }


    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CookingPotRecipe recipe, IFocusGroup focuses) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        int s = ingredients.size();

        builder.addSlot(RecipeIngredientRole.INPUT, 95 - WIDTH_OF, 55 - HEIGHT_OF).addItemStack(recipe.getContainer());

        for (int row = 0; row < 2; row++) {
            for (int slot = 0; slot < 3; slot++) {
                int current = slot + row + (row * 2);
                if(s - 1 < current) break;
                BakeryJEIPlugin.addSlot(builder,30 + (slot * 18) - WIDTH_OF, 17 + (row * 18) - HEIGHT_OF, ingredients.get(current));
            }
        }

        assert Minecraft.getInstance().level != null;
        builder.addSlot(RecipeIngredientRole.OUTPUT, 124 - WIDTH_OF,  28 - HEIGHT_OF).addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
    }

    @Override
    public void draw(CookingPotRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, CookingPotGui.ARROW_X - WIDTH_OF, CookingPotGui.ARROW_Y - HEIGHT_OF);
    }

    @Override
    public @NotNull RecipeType<CookingPotRecipe> getRecipeType() {
        return COOKING_POT;
    }

    @Override
    public @NotNull Component getTitle() {
        return this.localizedName;
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return this.background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return this.icon;
    }
}
