package net.silentchaos512.treasurebags.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemHandlerHelper;
import net.silentchaos512.lib.item.LootContainerItem;
import net.silentchaos512.treasurebags.TreasureBags;
import net.silentchaos512.treasurebags.config.Config;
import net.silentchaos512.treasurebags.lib.BagTypeManager;
import net.silentchaos512.treasurebags.lib.IBagType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.minecraft.item.Item.Properties;

public class TreasureBagItem extends LootContainerItem {
    private static final String NBT_BAG_TYPE = "BagType";

    public TreasureBagItem(Properties properties) {
        super(TreasureBags.getId("default_bag"), true, properties);
    }

    public ItemStack stackOfType(IBagType type) {
        return stackOfType(type, 1);
    }

    public ItemStack stackOfType(IBagType type, int count) {
        ItemStack result = new ItemStack(this, count);
        return setBagProperties(result, type);
    }

    /**
     * Create a bag of a type that may exist. Useful for recipes/ingredients, since bag types may
     * not exist before they load.
     *
     * @param typeName The type ID
     * @return A stack of one treasure bag
     */
    public ItemStack stackOfType(ResourceLocation typeName) {
        ItemStack result = new ItemStack(this);
        return setBagType(result, typeName);
    }

    @Nullable
    public static IBagType getBagType(ItemStack stack) {
        String typeStr = getBagTypeString(stack);
        return BagTypeManager.getValue(new ResourceLocation(typeStr));
    }

    private static String getBagTypeString(ItemStack stack) {
        return getData(stack).getString(NBT_BAG_TYPE);
    }

    /**
     * Set the BagType tag, nothing else. This is needed for loading from loot tables, because bag
     * types may not exist yet.
     *
     * @param stack     The bag
     * @param bagTypeId The bag type ID
     * @return The bag (modified original, not a copy)
     */
    public static ItemStack setBagType(ItemStack stack, ResourceLocation bagTypeId) {
        if (!(stack.getItem() instanceof TreasureBagItem)) return stack;
        CompoundNBT tag = getData(stack);
        tag.putString(NBT_BAG_TYPE, bagTypeId.toString());
        return stack;
    }

    public static ItemStack setBagProperties(ItemStack stack, IBagType type) {
        if (!(stack.getItem() instanceof TreasureBagItem)) return stack;
        CompoundNBT tag = getData(stack);
        tag.putString(NBT_BAG_TYPE, type.getId().toString());
        setLootTable(stack, type.getLootTable());
        return stack;
    }

    @Nonnull
    @Override
    protected ResourceLocation getLootTable(ItemStack stack) {
        IBagType type = getBagType(stack);
        if (type == null) {
            return super.getLootTable(stack);
        }
        return type.getLootTable();
    }

    @Nonnull
    @Override
    public ITextComponent getName(@Nonnull ItemStack stack) {
        IBagType type = getBagType(stack);
        if (type != null) {
            return type.getCustomName();
        }
        return super.getName(stack);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        IBagType type = getBagType(stack);
        if (type != null) {
            return type.getRarity();
        }
        return super.getRarity(stack);
    }

    @OnlyIn(Dist.CLIENT)
    public static int getColor(ItemStack stack, int tintIndex) {
        IBagType bagType = getBagType(stack);
        if (bagType != null) {
            if (tintIndex == 0) return bagType.getBagColor();
            if (tintIndex == 1) return bagType.getBagOverlayColor();
            if (tintIndex == 2) return bagType.getBagStringColor();
        }
        return 0xFFFFFF;
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        // Bag type
        if (flagIn.isAdvanced()) {
            IBagType type = getBagType(stack);
            if (type != null) {
                tooltip.add(new TranslationTextComponent(this.getDescriptionId() + ".type", type.getId())
                        .withStyle(TextFormatting.YELLOW));
            } else {
                String typeStr = getBagTypeString(stack);
                tooltip.add(new TranslationTextComponent(this.getDescriptionId() + ".unknownType")
                        .withStyle(TextFormatting.RED));
                tooltip.add(new TranslationTextComponent(this.getDescriptionId() + ".type", typeStr)
                        .withStyle(TextFormatting.YELLOW));
            }
        }

        // Loot table (or whatever ItemLootContainer wants to do)
        super.appendHoverText(stack, worldIn, tooltip, flagIn);

        // Display data pack ID
        String typeStr = getBagTypeString(stack);
        ResourceLocation typeName = ResourceLocation.tryParse(typeStr);
        if (typeName != null) {
            tooltip.add(new StringTextComponent(typeName.getNamespace())
                    .withStyle(TextFormatting.BLUE)
                    .withStyle(TextFormatting.ITALIC));
        } else {
            tooltip.add(new TranslationTextComponent(this.getDescriptionId() + ".invalidType")
                    .withStyle(TextFormatting.RED));
        }
    }

    @Override
    public void fillItemCategory(@Nonnull ItemGroup group, @Nonnull NonNullList<ItemStack> items) {
        if (!allowdedIn(group)) return;
        items.add(new ItemStack(this));

        // Add for each type (sorted by ID)
        List<IBagType> list = new ArrayList<>(BagTypeManager.getValues());
        list.sort(Comparator.comparing(o -> o.getId().toString()));
        for (IBagType type : list) {
            if (type.isVisible()) {
                items.add(stackOfType(type));
            }
        }
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, @Nonnull Hand handIn) {
        ItemStack heldItem = playerIn.getItemInHand(handIn);
        if (!(playerIn instanceof ServerPlayerEntity)) {
            return new ActionResult<>(ActionResultType.SUCCESS, heldItem);
        }
        ServerPlayerEntity playerMP = (ServerPlayerEntity) playerIn;

        // Generate items from loot table, give to player.
        boolean openWholeStack = playerMP.isCrouching();
        Collection<ItemStack> lootDrops = getDropsFromStack(heldItem, playerMP, openWholeStack);
        if (lootDrops.isEmpty()) {
            TreasureBags.LOGGER.warn("No drops from bag, is the loot table valid? {}, table={}", heldItem, getLootTable(heldItem));
            return new ActionResult<>(ActionResultType.FAIL, heldItem);
        }
        lootDrops.forEach(stack -> {
            giveOrDropItem(playerIn, stack.copy());
            listItemReceivedInChat(playerMP, stack);
        });

        // Play item pickup sound...
        playerMP.level.playSound(null, playerMP.getX(), playerMP.getY(), playerMP.getZ(),
                SoundEvents.ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F,
                ((playerMP.getRandom().nextFloat() - playerMP.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
        heldItem.shrink(openWholeStack ? heldItem.getCount() : 1);
        return new ActionResult<>(ActionResultType.SUCCESS, heldItem);
    }

    private void giveOrDropItem(PlayerEntity playerIn, ItemStack copy) {
        if (Config.Common.alwaysSpawnItems.get() || !playerIn.inventory.add(copy)) {
            ItemEntity entityItem = new ItemEntity(playerIn.level, playerIn.getX(), playerIn.getY(0.5), playerIn.getZ(), copy);
            entityItem.setNoPickUpDelay();
            entityItem.setOwner(playerIn.getUUID());
            playerIn.level.addFreshEntity(entityItem);
        }
    }

    private Collection<ItemStack> getDropsFromStack(ItemStack stack, ServerPlayerEntity player, boolean wholeStack) {
        Collection<ItemStack> list = new ArrayList<>();
        int openCount = wholeStack ? stack.getCount() : 1;
        for (int i = 0; i < openCount; ++i) {
            this.getLootDrops(stack, player).forEach(s -> mergeItem(list, s));
        }
        return list;
    }

    private static void mergeItem(Collection<ItemStack> list, ItemStack stack) {
        for (ItemStack itemStack : list) {
            if (ItemHandlerHelper.canItemStacksStack(itemStack, stack)) {
                int space = itemStack.getMaxStackSize() - itemStack.getCount();
                int toMerge = Math.min(space, stack.getCount());
                itemStack.grow(toMerge);
                stack.shrink(toMerge);

                if (stack.isEmpty()) {
                    return;
                }
            }
        }

        if (!stack.isEmpty()) {
            list.add(stack);
        }
    }

    private static void listItemReceivedInChat(ServerPlayerEntity playerMP, ItemStack stack) {
        ITextComponent itemReceivedText = new TranslationTextComponent(
                "item.silentlib.lootContainer.itemReceived",
                stack.getCount(),
                stack.getHoverName());
        playerMP.sendMessage(itemReceivedText, Util.NIL_UUID);
    }
}
