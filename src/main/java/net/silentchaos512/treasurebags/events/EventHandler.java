package net.silentchaos512.treasurebags.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.LootTable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.silentchaos512.treasurebags.TreasureBags;
import net.silentchaos512.treasurebags.api.IEntityGroup;
import net.silentchaos512.treasurebags.setup.EntityGroups;

import javax.annotation.Nullable;
import java.util.List;

@Mod.EventBusSubscriber(modid = TreasureBags.MOD_ID)
public final class EventHandler {
    private EventHandler() {}

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        // Mostly copied from Scaling Health
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity entity = (LivingEntity) event.getEntity();
        World world = entity.level;
        if (world.isClientSide) return;
        MinecraftServer server = world.getServer();
        if (server == null) return;

        // Mob loot disabled?
        if (!world.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;

        PlayerEntity player = getPlayerThatCausedDeath(event.getSource());

        // Get the bonus drops loot table for this mob type
        EntityGroups.getGroups(entity).forEach(group -> doDropsForGroup(event, entity, world, server, player, group));
    }

    private static void doDropsForGroup(LivingDropsEvent event, LivingEntity entity, World world, MinecraftServer server, PlayerEntity player, IEntityGroup group) {
        LootTable lootTable = server.getLootTables().get(group.getLootTable());
        LootContext.Builder contextBuilder = new LootContext.Builder((ServerWorld) world)
                .withParameter(LootParameters.THIS_ENTITY, entity)
                .withParameter(LootParameters.ORIGIN, entity.position())
                .withParameter(LootParameters.DAMAGE_SOURCE, event.getSource())
                .withOptionalParameter(LootParameters.KILLER_ENTITY, player)
                .withOptionalParameter(LootParameters.LAST_DAMAGE_PLAYER, player)
                .withOptionalParameter(LootParameters.DIRECT_KILLER_ENTITY, event.getSource().getDirectEntity());
        if (player != null) {
            contextBuilder.withLuck(player.getLuck());
        }
        List<ItemStack> list = lootTable.getRandomItems(contextBuilder.create(LootParameterSets.ENTITY));
        list.forEach(stack -> event.getDrops().add(new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), stack)));
    }

    /**
     * Get the player that caused a mob's death. Could be a FakePlayer or null. Copied from Scaling
     * Health.
     *
     * @return The player that caused the damage, or the owner of the tamed animal that caused the
     * damage.
     */
    @SuppressWarnings("ChainOfInstanceofChecks")
    @Nullable
    private static PlayerEntity getPlayerThatCausedDeath(DamageSource source) {
        if (source == null) return null;

        // Player is true source?
        Entity entitySource = source.getEntity();
        if (entitySource instanceof PlayerEntity) {
            return (PlayerEntity) entitySource;
        }

        // Player's pet is true source?
        if (entitySource instanceof TameableEntity) {
            TameableEntity tamed = (TameableEntity) entitySource;
            if (tamed.isTame() && tamed.getOwner() instanceof PlayerEntity) {
                return (PlayerEntity) tamed.getOwner();
            }
        }

        // No player responsible
        return null;
    }
}
