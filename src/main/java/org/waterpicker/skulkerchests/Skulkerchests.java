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
import java.util.stream.Collectors;

import static org.spongepowered.api.data.DataQuery.of;
import static org.spongepowered.api.data.manipulator.catalog.CatalogItemData.DISPLAY_NAME_DATA;
import static org.spongepowered.api.data.manipulator.catalog.CatalogItemData.INVENTORY_ITEM_DATA;

@Plugin(id = "skulkerchests", description = "Makes chest behave similar to Skulker Boxes", authors = "Waterpicker")
public class Skulkerchests {
    List<Location<World>> map = new ArrayList<>();

    @Listener()
    public void onBlockItemDrop(DropItemEvent.Destruct event, @First Player player, @First BlockSnapshot blockSnapshot) {
        if(blockSnapshot.getState().getType().equals(BlockTypes.CHEST)) {

            long count = event.getEntities().stream().map(e -> e.get(Keys.REPRESENTED_ITEM).get()).map(ItemStackSnapshot::getType).count();

            player.sendMessage(Text.of(count));

            event.filterEntities(s -> false);

            if (count == 1) {
                map.add(blockSnapshot.getLocation().get());
                ItemStack stack = ItemStack.builder().fromBlockSnapshot(blockSnapshot).build();
                stack.offer(Keys.ITEM_LORE, getTotals(stack.toContainer()));

                Entity itemEntity = blockSnapshot.getLocation().get().createEntity(EntityTypes.ITEM);
                itemEntity.offer(Keys.REPRESENTED_ITEM, stack.createSnapshot());

                blockSnapshot.getLocation().get().spawnEntity(itemEntity);
            }
        }
    }

    List<Text> getTotals(DataView view) {
        ArrayList<Text> draft = new ArrayList<>();
        view.getViewList(DataQuery.of('.', "UnsafeData.BlockEntityTag.Items")).ifPresent((items) -> {
            items.forEach((stack) -> {
                String name = stack.getString(DataQuery.of("tag.display.Name", ".")).orElse(Sponge.getRegistry().getType(ItemType.class, stack.getString(DataQuery.of("id")).get()).get().getTranslation().get());
                Integer count = stack.getInt(DataQuery.of("Count")).get();
                draft.add(Text.of(TextColors.WHITE, name + " x" + count));
            });
        });

        int extra = draft.size() - 5;
        if(extra <= 0) {
            return draft;
        } else {
            List<Text> fin = draft.stream().limit(5).collect(Collectors.toList());

            fin.add(Text.of(TextColors.WHITE, "and " + extra + " more..."));
            return fin;
        }
    }
}
