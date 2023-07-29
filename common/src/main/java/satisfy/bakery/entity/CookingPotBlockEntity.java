package satisfy.bakery.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import satisfy.bakery.block.CookingPotBlock;
import satisfy.bakery.client.gui.handler.CookingPotGuiHandler;
import satisfy.bakery.recipe.CookingPotRecipe;
import satisfy.bakery.registry.BlockEntityRegistry;
import satisfy.bakery.registry.RecipeTypeRegistry;
import satisfy.bakery.registry.TagsRegistry;

import static net.minecraft.world.item.ItemStack.isSameItemSameTags;

public class CookingPotBlockEntity extends BlockEntity implements BlockEntityTicker<CookingPotBlockEntity>, ImplementedInventory, MenuProvider {

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(MAX_CAPACITY, ItemStack.EMPTY);
    private static final int MAX_CAPACITY = 8;
    public static final int MAX_COOKING_TIME = 600; // Time in ticks (30s)
    private int cookingTime;
    protected static final int[] RECIPE_SLOTS = {1, 2, 3, 4, 5, 6};
    public static final int BOTTLE_INPUT_SLOT = 7;
    public static final int OUTPUT_SLOT = 0;
    private static final int INGREDIENTS_AREA = 2 * 3;

    private boolean isBeingBurned;

    private final ContainerData delegate;

    public CookingPotBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.COOKING_POT_BLOCK_ENTITY.get(), pos, state);
        this.delegate = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> CookingPotBlockEntity.this.cookingTime;
                    case 1 -> CookingPotBlockEntity.this.isBeingBurned ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> CookingPotBlockEntity.this.cookingTime = value;
                    case 1 -> CookingPotBlockEntity.this.isBeingBurned = value != 0;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if(side.equals(Direction.UP)){
            return RECIPE_SLOTS;
        } else if (side.equals(Direction.DOWN)){
            return new int[]{OUTPUT_SLOT};
        } else return new int[]{BOTTLE_INPUT_SLOT};
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        ContainerHelper.loadAllItems(nbt, this.inventory);
        this.cookingTime = nbt.getInt("CookingTime");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        ContainerHelper.saveAllItems(nbt, this.inventory);
        nbt.putInt("CookingTime", this.cookingTime);
    }

    public boolean isBeingBurned() {
        if (getLevel() == null)
            throw new NullPointerException("Null world invoked");
        final BlockState belowState = this.getLevel().getBlockState(getBlockPos().below());
        if (belowState.is(TagsRegistry.ALLOWS_COOKING)) {
            try {
                return belowState.getValue(BlockStateProperties.LIT);
            } catch (IllegalArgumentException e) {
                return true;
            }
        }
        return false;
    }


    private boolean canCraft(Recipe<?> recipe, RegistryAccess access) {
        if (recipe == null || recipe.getResultItem(access).isEmpty()) {
            return false;
        }
        if (recipe instanceof CookingPotRecipe cookingRecipe) {
            if (!this.getItem(BOTTLE_INPUT_SLOT).is(cookingRecipe.getContainer().getItem())) {
                return false;
            } else if (this.getItem(OUTPUT_SLOT).isEmpty()) {
                return true;
            } else {
                if (this.getItem(OUTPUT_SLOT).isEmpty()) {
                    return true;
                }
                final ItemStack recipeOutput = recipe.getResultItem(access);
                final ItemStack outputSlotStack = this.getItem(OUTPUT_SLOT);
                final int outputSlotCount = outputSlotStack.getCount();
                if (this.getItem(OUTPUT_SLOT).isEmpty()) {
                    return true;
                } else if (!isSameItemSameTags(outputSlotStack, recipeOutput)) {
                    return false;
                } else if (outputSlotCount < this.getMaxStackSize() && outputSlotCount < outputSlotStack.getMaxStackSize()) {
                    return true;
                } else {
                    return outputSlotCount < recipeOutput.getMaxStackSize();
                }
            }
        }
        return false;
    }


    private void craft(Recipe<?> recipe, RegistryAccess access) {
        if (!canCraft(recipe, access)) {
            return;
        }
        final ItemStack recipeOutput = recipe.getResultItem(access);
        final ItemStack outputSlotStack = this.getItem(OUTPUT_SLOT);
        if (outputSlotStack.isEmpty()) {
            setItem(OUTPUT_SLOT, recipeOutput.copy());
        } else if (outputSlotStack.is(recipeOutput.getItem())) {
            outputSlotStack.grow(recipeOutput.getCount());
        }
        final NonNullList<Ingredient> ingredients = recipe.getIngredients();
        boolean[] slotUsed = new boolean[INGREDIENTS_AREA];
        for (int i = 0; i < recipe.getIngredients().size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            final ItemStack bestSlot = this.getItem(i);
            if (ingredient.test(bestSlot) && !slotUsed[i]) {
                slotUsed[i] = true;
                bestSlot.shrink(1);
            } else {
                for (int j = 0; j < INGREDIENTS_AREA; j++) {
                    ItemStack stack = this.getItem(j);
                    if (ingredient.test(stack) && !slotUsed[j]) {
                        slotUsed[j] = true;
                        stack.shrink(1);
                    }
                }
            }
        }
        this.getItem(BOTTLE_INPUT_SLOT).shrink(1);
    }


    @Override
    public void tick(Level world, BlockPos pos, BlockState state, CookingPotBlockEntity blockEntity) {
        if (world.isClientSide()) {
            return;
        }
        boolean isBeingBurned = isBeingBurned();
        if (!isBeingBurned) {
            delegate.set(1, 0);
            if (state.getValue(CookingPotBlock.LIT)) {
                world.setBlock(pos, state.setValue(CookingPotBlock.LIT, false), Block.UPDATE_ALL);
            }
            return;
        }
        delegate.set(1, 1);
        Recipe<?> recipe = world.getRecipeManager().getRecipeFor(RecipeTypeRegistry.COOKING_POT_RECIPE_TYPE.get(), blockEntity, world).orElse(null);
        RegistryAccess access = level.registryAccess();
        boolean canCraft = canCraft(recipe, access);
        if (canCraft) {
            this.cookingTime++;
            if (this.cookingTime >= MAX_COOKING_TIME) {
                this.cookingTime = 0;
                craft(recipe, access);
            }
        } else if (!canCraft(recipe, access)) {
            this.cookingTime = 0;
        }
        if (canCraft) {
            world.setBlock(pos, this.getBlockState().getBlock().defaultBlockState().setValue(CookingPotBlock.COOKING, true).setValue(CookingPotBlock.LIT, true), Block.UPDATE_ALL);
        } else if (state.getValue(CookingPotBlock.COOKING)) {
            world.setBlock(pos, this.getBlockState().getBlock().defaultBlockState().setValue(CookingPotBlock.COOKING, false).setValue(CookingPotBlock.LIT, true), Block.UPDATE_ALL);
        } else if (state.getValue(CookingPotBlock.LIT) != isBeingBurned) {
            world.setBlock(pos, state.setValue(CookingPotBlock.LIT, isBeingBurned), Block.UPDATE_ALL);
        }
    }


    @Override
    public NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }


    @Override
    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double) this.worldPosition.getX() + 0.5, (double) this.worldPosition.getY() + 0.5, (double) this.worldPosition.getZ() + 0.5) <= 64.0;
        }
    }


    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new CookingPotGuiHandler(syncId, inv, this, this.delegate);
    }
}


