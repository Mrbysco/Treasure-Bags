package net.silentchaos512.loot;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.silentchaos512.loot.command.TreasureBagsCommand;
import net.silentchaos512.loot.crafting.ingredient.TreasureBagIngredient;
import net.silentchaos512.loot.crafting.recipe.ShapedTreasureBagRecipe;
import net.silentchaos512.loot.crafting.recipe.ShapelessTreasureBagRecipe;
import net.silentchaos512.loot.init.ModBlocks;
import net.silentchaos512.loot.init.ModItems;
import net.silentchaos512.loot.init.ModLoot;
import net.silentchaos512.loot.item.TreasureBagItem;
import net.silentchaos512.loot.lib.BagTypeManager;
import net.silentchaos512.loot.network.Network;

class SideProxy {
    SideProxy() {
        TreasureBags.LOGGER.debug("SideProxy init");

        // TODO: Config
        Network.init();

        ModLoot.init();

        // Recipes and ingredients
        IRecipeSerializer.register("treasurebags:shaped_bag", ShapedTreasureBagRecipe.SERIALIZER);
        IRecipeSerializer.register("treasurebags:shapeless_bag", ShapelessTreasureBagRecipe.SERIALIZER);
        CraftingHelper.register(TreasureBagIngredient.Serializer.NAME, TreasureBagIngredient.Serializer.INSTANCE);

        // Add listeners for common events
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::imcEnqueue);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::imcProcess);

        // Add listeners for registry events
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(Block.class, ModBlocks::registerAll);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(Item.class, ModItems::registerAll);

        MinecraftForge.EVENT_BUS.addListener(this::serverAboutToStart);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

    private void imcEnqueue(InterModEnqueueEvent event) {
    }

    private void imcProcess(InterModProcessEvent event) {
    }

    private void serverAboutToStart(FMLServerAboutToStartEvent event) {
        event.getServer().getResourceManager().addReloadListener(BagTypeManager.INSTANCE);
        TreasureBagsCommand.register(event.getServer().getCommandManager().getDispatcher());
    }

    static class Client extends SideProxy {
        Client() {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onItemColors);
        }

        private void clientSetup(FMLClientSetupEvent event) {
        }

        private void onItemColors(ColorHandlerEvent.Item event) {
            ItemColors colors = event.getItemColors();
            if (colors == null) {
                TreasureBags.LOGGER.error("ItemColors is null!", new NullPointerException("wat?"));
                return;
            }

            try {
                colors.register(TreasureBagItem::getColor, ModItems.treasureBag);
            } catch (NullPointerException ex) {
                TreasureBags.LOGGER.error("Something went horribly wrong with ItemColors", ex);
            }
        }
    }

    static class Server extends SideProxy {
        Server() {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverSetup);
        }

        private void serverSetup(FMLDedicatedServerSetupEvent event) {
        }
    }
}
