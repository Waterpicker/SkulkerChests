package org.waterpicker.skulkerchests;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.InventoryItemData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.entity.spawn.BlockSpawnCause;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.property.SlotIndex;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.nio.file.Path;
import java.util.*;

import static org.spongepowered.api.data.DataQuery.of;
import static org.spongepowered.api.data.manipulator.catalog.CatalogItemData.DISPLAY_NAME_DATA;
import static org.spongepowered.api.data.manipulator.catalog.CatalogItemData.INVENTORY_ITEM_DATA;

@Plugin(id = "skulkerchests", description = "Makes chest behave similar to Skulker Chests", authors = "Waterpicker")
public class Skulkerchests {
    @Inject
    private PluginContainer container;


    @Inject
    @ConfigDir(sharedRoot = false)
    private Path derp;

    private Map<Location<World>, Content> blocks = new HashMap<>();

    @Listener
    public void onChestBreak(ChangeBlockEvent.Break event) {


        event.getTransactions().stream().map(Transaction::getOriginal).filter(blockSnapshot -> blockSnapshot.getState().getType() == BlockTypes.CHEST).forEach(block -> {
            block.getLocation().ifPresent(loc -> {
                ItemStack stack = ItemStack.builder().fromBlockSnapshot(block).build();
                stack.offer(Keys.ITEM_LORE, getLore(stack));

                if(!stack.equalTo(ItemStack.empty())) blocks.put(loc, new Content(stack));
            });
        });
    }

    @Listener
    public void onItemDrops(DropItemEvent.Destruct event, @First BlockSpawnCause cause) {
        System.out.println(event.getCause());

        cause.getBlockSnapshot().getLocation().filter(location -> blocks.containsKey(location)).ifPresent(location -> {
            Content content = blocks.get(location);

            if(content.isUsed()) {
                blocks.remove(location);
            }

            if(content.getItemStack() != null && !content.isUsed()) {
                Entity itemEntity = location.getExtent().createEntity(EntityTypes.ITEM, location.getPosition());
                itemEntity.offer(Keys.REPRESENTED_ITEM, content.getItemStack().createSnapshot());

                location.getExtent().spawnEntity(itemEntity, event.getCause());

                content.toggleUsed();
            }

            event.setCancelled(true);
        });
    }

    private List<Text> getLore(ItemStack stack) {
        Optional<Inventory> inventory = stack.get(INVENTORY_ITEM_DATA).map(Carrier::getInventory);

        List<Text> list = new ArrayList<>();

        if (inventory.isPresent() && !isEmpty(inventory.get())) {
            int remaining = inventory.get().totalItems() - 5;

            inventory.get().slots().forEach(slot -> {
                slot.peek().ifPresent(s -> {
                    list.add(Text.of(s.get(Keys.DISPLAY_NAME), " x" + s.getQuantity()));
                });
            });


            if (remaining > 0) {
                list.add(Text.of(remaining, "x more items"));
            }
        } else {
            System.out.println("Derp");
        }

        return list;
    }

    private boolean isEmpty(Inventory inventory) {
        return inventory.totalItems() <= 0;
    }
}

class Content {
        private boolean isUsed = false;
        private ItemStack stack;

        Content(ItemStack stack) {
            this.stack = stack;
        }

        boolean isUsed() {
            return isUsed;
        }

        void toggleUsed() {
            isUsed = true;
        }

        ItemStack getItemStack() {
            return stack;
        }
}
