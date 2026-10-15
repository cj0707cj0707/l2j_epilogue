package custom.GiranServices;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;

import custom.fakeplayersai.BotPresets;
import custom.fakeplayersai.TrainingArena;

/** Stores object IDs, never item templates; cannot duplicate an item. */
public final class Preparation
{
    private static final String KEY = "GiranEquipmentSet.";
    private Preparation() { }
    public static boolean allowed(Player p)
    {
        return p != null && p.isOnline() && !p.isAlikeDead() && !p.isInCombat() && !p.isCastingNow()
            && !p.isAttackingNow() && !p.isTeleporting() && !p.isInStoreMode() && !p.isProcessingRequest()
            && !p.isProcessingTransaction() && !p.isFishing() && !p.isInventoryDisabled() && !p.isOutOfControl()
            && !p.isMovementDisabled() && !p.isTransformed() && !p.isCursedWeaponEquipped() && !p.isCombatFlagEquipped()
            && !p.isInDuel() && !p.inObserverMode() && !p.isInOlympiadMode() && !p.isOlympiadStart()
            && !TrainingArena.isParticipant(p) && p.getInstanceId() == 0
            && !Olympiad.getInstance().isRegistered(p) && !Olympiad.getInstance().isRegisteredInComp(p);
    }
    static Set<Integer> parse(String text)
    {
        Set<Integer> ids = new LinkedHashSet<>();
        if (text == null || text.isBlank()) return ids;
        String[] values = text.split(",", -1);
        if (values.length > 32) throw new IllegalArgumentException("Too many items");
        for (String value : values)
        {
            int id = Integer.parseInt(value);
            if (id <= 0 || !ids.add(id)) throw new IllegalArgumentException("Invalid saved item");
        }
        return ids;
    }
    public static String save(Player player, int slot)
    {
        if (slot < 0 || slot > 2 || !allowed(player)) return "Сохранение сейчас недоступно. Выйдите из боя и очереди Олимпиады.";
        synchronized (player.getInventory())
        {
            if (!allowed(player)) return "Состояние персонажа изменилось.";
            StringJoiner ids = new StringJoiner(",");
            for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
            {
                Item item = player.getInventory().getPaperdollItem(i);
                if (item != null) ids.add(Integer.toString(item.getObjectId()));
            }
            if (ids.length() == 0) return "Нечего сохранять: экипировка не надета.";
            player.getVariables().set(KEY + slot, ids.toString());
            if (!player.getVariables().saveNow()) return "Не удалось подтвердить запись набора. Проверьте журнал сервера.";
            return "Набор сохранён. Запомнены только ваши предметы, новые вещи не создаются.";
        }
    }
    public static String apply(Player player, int slot)
    {
        if (slot < 0 || slot > 2 || !allowed(player)) return "Переодевание сейчас запрещено: бой, очередь/матч, дуэль или другое занятие.";
        synchronized (player.getInventory())
        {
            if (!allowed(player)) return "Состояние персонажа изменилось.";
            final Set<Integer> ids;
            try { ids = parse(player.getVariables().getString(KEY + slot, "")); }
            catch (RuntimeException e) { return "Некорректный сохранённый набор. Сохраните его заново."; }
            if (ids.isEmpty()) return "Этот набор ещё не сохранён.";
            List<Item> desired = new ArrayList<>();
            for (int id : ids)
            {
                Item item = player.getInventory().getItemByObjectId(id);
                if (item == null || item.getOwnerId() != player.getObjectId() || !item.isEquipable())
                    return "Один из предметов отсутствует в инвентаре. Набор не применён.";
                if (!item.isEquipped() && !item.getTemplate().checkCondition(player, player, true))
                    return "Предмет не проходит штатные условия экипировки. Набор не применён.";
                desired.add(item);
            }
            List<Item> old = new ArrayList<>(player.getInventory().getPaperdollItems());
            for (Item item : old)
            {
                if (!allowed(player)) return "Переключение остановлено: состояние персонажа изменилось. Проверьте надетые вещи.";
                if (!ids.contains(item.getObjectId())) player.useEquippableItem(item, true);
            }
            for (Item item : desired)
            {
                if (!allowed(player)) return "Переключение остановлено: проверьте надетые вещи.";
                if (!item.isEquipped() && item.getTemplate().checkCondition(player, player, true)) player.useEquippableItem(item, true);
            }
            Set<Integer> actual = new HashSet<>();
            for (Item item : player.getInventory().getPaperdollItems()) actual.add(item.getObjectId());
            player.sendItemList(false);
            player.broadcastUserInfo();
            return actual.equals(ids) ? "Набор надет. Состав предметов совпадает с сохранённым."
                : "Набор применён частично: штатные ограничения не позволили надеть/снять всё. Проверьте экипировку.";
        }
    }
    public static String supplies(Player player, Npc npc)
    {
        if (!allowed(player)) return "Расходники недоступны во время боя, очереди, матча или тренировки.";
        synchronized (player.getInventory())
        {
            if (!allowed(player)) return "Состояние персонажа изменилось.";
            int[][] pack = {{BotPresets.getFighterShotId(),5000},{BotPresets.getBlessedSpiritshotId(),5000},
                {BotPresets.getArrowId(),2000},{BotPresets.getBoltId(),2000},{3031,1000},{6645,2000}};
            int added = 0;
            for (int[] entry : pack)
            {
                long have = player.getInventory().getInventoryItemCount(entry[0], -1);
                long amount = Math.max(0, entry[1] - have);
                if (amount == 0) continue;
                if (!player.getInventory().validateCapacityByItemId(entry[0], amount) || !player.getInventory().validateWeightByItemId(entry[0], amount))
                    return "Выдано позиций: " + added + ". Остальное не поместилось по весу/месту.";
                if (player.addItem(ItemProcessType.QUEST, entry[0], amount, npc, true) != null) added++;
            }
            return "Запас пополнен до лимитов пакета. Повторное нажатие не умножает уже имеющийся запас.";
        }
    }
    public static String supply(Player player, Npc npc, int kind, int target)
    {
        int[] itemIds = {BotPresets.getFighterShotId(),BotPresets.getBlessedSpiritshotId(),BotPresets.getArrowId(),BotPresets.getBoltId(),3031,6645};
        int[] caps = {5000,5000,2000,2000,1000,2000};
        if (kind < 0 || kind >= itemIds.length || (target != 100 && target != 1000 && target != caps[kind]) || target > caps[kind]) return "Недопустимый пакет расходников.";
        if (!allowed(player)) return "Расходники недоступны в текущем состоянии.";
        synchronized (player.getInventory())
        {
            if (!allowed(player)) return "Состояние персонажа изменилось.";
            long amount = Math.max(0, target-player.getInventory().getInventoryItemCount(itemIds[kind],-1));
            if (amount == 0) return "Выбранный запас уже имеется. Лишние предметы не удаляются.";
            if (!player.getInventory().validateCapacityByItemId(itemIds[kind],amount) || !player.getInventory().validateWeightByItemId(itemIds[kind],amount)) return "Недостаточно места или превышен вес.";
            return player.addItem(ItemProcessType.QUEST,itemIds[kind],amount,npc,true) != null ? "Запас выбранного расходника пополнен до " + target + "." : "Выдача не подтверждена.";
        }
    }
    public static String check(Player player)
    {
        int equipped = player.getInventory().getPaperdollItems().size();
        return "Уровень: " + player.getLevel() + "; надето предметов: " + equipped
            + ". Оружие: " + (player.getActiveWeaponInstance() == null ? "не надето" : "+"+player.getActiveWeaponInstance().getEnchantLevel())
            + "; броня груди: " + (player.getChestArmorInstance() == null ? "нет" : "+"+player.getChestArmorInstance().getEnchantLevel())
            + "; S soulshots: " + player.getInventory().getInventoryItemCount(BotPresets.getFighterShotId(),-1)
            + "; S blessed spiritshots: " + player.getInventory().getInventoryItemCount(BotPresets.getBlessedSpiritshotId(),-1)
            + "; Spirit Ore: " + player.getInventory().getInventoryItemCount(3031,-1)
            + ". Для тренировок нужны полные HP/MP/CP; расходники проверяйте по грейду оружия."
            + " Сервис не меняет заточку/аугментацию и не восстанавливает HP, MP или откаты.";
    }
}
