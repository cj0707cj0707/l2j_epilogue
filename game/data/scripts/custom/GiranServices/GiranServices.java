/* Four Giran service NPCs. NPC origin/proximity checked on EVERY mutating event. */
package custom.GiranServices;

import java.util.Map;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.handler.VoicedCommandHandler;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.util.HtmlUtil;

import custom.fakeplayersai.*;

public final class GiranServices extends Script
{
    public static final int TRAINER = 1008000, PREPARATION = 1009000, HOST = 1010000, GUIDE = 1011000;
    private static final String[] SET_NAMES = {"Основной", "Против мага", "Против мили"};
    private static final String[] DIFFICULTIES = {"normal", "experienced", "strong"};
    private static final Map<String,String> DIFF_NAMES = Map.of("normal","Обычный","experienced","Опытный","strong","Сильный");
    private static final String PREFIX = "GiranTrainer.";
    private GiranServices()
    {
        addStartNpc(TRAINER,PREPARATION,HOST,GUIDE);
        addTalkId(TRAINER,PREPARATION,HOST,GUIDE);
        addFirstTalkId(TRAINER,PREPARATION,HOST,GUIDE);
        TacticalSettings.init();
        CombatAnalytics.init();
        TrainingArena.init();
        VoicedCommandHandler.getInstance().registerHandler(new RemoteTraining());
        BypassHandler.getInstance().registerHandler(new NpcCommands());
    }
    public static String escape(String text)
    {
        return text == null ? "" : text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
    private static String button(String label, String event)
    {
        return "<button value=\"" + escape(label) + "\" action=\"bypass -h GiranService " + event
            + "\" width=240 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";
    }
    private static String frame(String body)
    {
        return "<html><title>Сервисы Гирана</title><body><center><br><font color=\"LEVEL\">Сервисы сервера</font><br>"
            + body + "<br></center></body></html>";
    }
    private static boolean canUse(Npc npc, Player p)
    {
        return npc != null && p != null && p.isOnline() && npc.canInteract(p)
            && (npc.getId()==TRAINER || npc.getId()==PREPARATION || npc.getId()==HOST || npc.getId()==GUIDE);
    }
    @Override public String onFirstTalk(Npc npc, Player p)
    {
        if (canUse(npc,p)) show(npc,p,"main","");
        return null;
    }
    /** Legacy ScriptLink dispatch uses remembered lastFolkNpc, not validated HTML
     * origin. Do not execute mutations through that ambiguous route anymore. */
    @Override public String onEvent(String event, Npc npc, Player p)
    {
        if (p != null) p.sendMessage("Меню устарело. Поговорите с нужным NPC заново.");
        return null;
    }
    private String handleNpcEvent(String event, Npc npc, Player p)
    {
        if (!canUse(npc,p))
        {
            if (p != null) p.sendMessage("Подойдите к нужному NPC, встаньте и дождитесь окончания каста. Откройте его меню заново.");
            return null;
        }
        if (event == null || event.length()>100) { p.sendMessage("Некорректная команда меню. Откройте NPC заново."); return null; }
        String[] a = event.trim().split(" +");
        String notice="", page="main";
        try
        {
            if (npc.getId()==TRAINER)
            {
                if (a.length==2 && a[0].equals("class"))
                {
                    int id = Integer.parseInt(a[1]);
                    if (BotPresets.getPresetByClassId(id)!=null) p.getVariables().set(PREFIX+"class",id);
                }
                else if (a.length==2 && a[0].equals("difficulty") && TacticalSettings.validDifficulty(a[1])) p.getVariables().set(PREFIX+"difficulty",a[1]);
                else if (a.length==2 && a[0].equals("profile") && (a[1].equals("custom") || a[1].equals("equal"))) p.getVariables().set(PREFIX+"profile","custom"); // legacy equal menu links normalize to ranked baseline
                else if (a.length==1 && a[0].equals("classes")) page="classes";
                else if (a.length==1 && a[0].equals("start")) notice=TrainingArena.start(p,selectedClass(p),selectedDifficulty(p),selectedProfile(p));
                else if (a.length==1 && a[0].equals("repeat")) notice=TrainingArena.repeat(p);
                else if (a.length==1 && a[0].equals("leave")) notice=TrainingArena.leave(p);
                else if (a.length==1 && a[0].equals("stats")) page="stats";
            }
            else if (npc.getId()==PREPARATION)
            {
                if (a.length==2 && a[0].equals("save")) notice=Preparation.save(p,Integer.parseInt(a[1]));
                else if (a.length==2 && a[0].equals("equip")) notice=Preparation.apply(p,Integer.parseInt(a[1]));
                else if (a.length==1 && a[0].equals("supplies")) notice=Preparation.supplies(p,npc);
                else if (a.length==1 && a[0].equals("supplyMenu")) page="supplyMenu";
                else if (a.length==3 && a[0].equals("supply")) { notice=Preparation.supply(p,npc,Integer.parseInt(a[1]),Integer.parseInt(a[2])); page="supplyMenu"; }
                else if (a.length==1 && a[0].equals("check")) notice=Preparation.check(p);
            }
            else if (npc.getId()==HOST)
            {
                if (a.length==1 && a[0].equals("register"))
                {
                    if (Preparation.allowed(p)) notice=Olympiad.getInstance().registerNoble(p,false) ? "Заявка на бесклассовый бой принята." : "Регистрация отклонена. Смотрите системное сообщение.";
                    else notice="Регистрация сейчас недоступна: проверьте бой, очередь, дуэль или тренировку.";
                }
                else if (a.length==1 && a[0].equals("cancel")) notice=Olympiad.getInstance().unRegisterNoble(p) ? "Заявка отменена." : "Отмена недоступна: возможно, матч уже назначен.";
                else if (a.length==1 && a[0].equals("watch"))
                {
                    if (Preparation.allowed(p)) { Olympiad.sendMatchList(p); return null; }
                    notice="Наблюдение недоступно в вашем текущем состоянии.";
                }
                else if (a.length==1 && a[0].equals("trainingHelp")) page="trainingHelp";
                else if (a.length==1 && a[0].equals("stats")) page="stats";
            }
            else if (npc.getId()==GUIDE)
            {
                if (a.length==2 && a[0].equals("help") && HELP.containsKey(a[1])) page=a[1];
                else if (a.length==2 && a[0].equals("gmDifficulty") && p.isGM() && TacticalSettings.validDifficulty(a[1]))
                {
                    if (TacticalSettings.saveDifficulty(a[1]))
                    {
                        for (Player bot : BotManager.getBots().values()) BotTactics.setDifficulty(bot.getObjectId(),a[1]);
                        notice="Сложность рейтинговых ботов сохранена: "+DIFF_NAMES.get(a[1]);
                    }
                    else notice="Настройку записать не удалось. Проверьте журнал.";
                }
            }
        }
        catch (NumberFormatException e) { notice="Некорректный параметр."; }
        catch (Exception e) { LOGGER.warning("GiranServices: " + e); notice="Операция не завершена. Проверьте журнал сервера."; }
        if (!notice.isBlank()) p.sendMessage(notice);
        if (TrainingArena.isParticipant(p))
        {
            // The player was teleported away: old NPC-bound buttons are now invalid.
            // A noninteractive, origin-free status is safe; NEVER expose remote start.
            HtmlUtil.sendCBHtml(p,frame("<font color=\"LEVEL\">Тренировка</font><br>" + escape(TrainingArena.describe(p))
                + "<br>Команды чата:<br>.training leave<br>.training status<br>.training report"),0);
        }
        else show(npc,p,page,notice);
        return null;
    }
    private static int selectedClass(Player p)
    {
        int id=p.getVariables().getInt(PREFIX+"class",88);
        return BotPresets.getPresetByClassId(id)==null ? 88 : id;
    }
    private static String selectedDifficulty(Player p)
    {
        String d=p.getVariables().getString(PREFIX+"difficulty","strong");
        return TacticalSettings.validDifficulty(d) ? d : "strong";
    }
    private static String selectedProfile(Player p)
    {
        if (!"custom".equals(p.getVariables().getString(PREFIX+"profile","custom")))
            p.getVariables().set(PREFIX+"profile","custom"); // migrate saved equal selections
        return "custom";
    }
    private void show(Npc npc,Player p,String page,String notice)
    {
        StringBuilder body=new StringBuilder();
        if (!notice.isBlank()) body.append("<font color=\"LEVEL\">").append(escape(notice)).append("</font><br><br>");
        if (page.equals("trainingHelp")) body.append(HELP.get("training")).append(button("Назад","main"));
        else if (page.equals("stats")) body.append(CombatAnalytics.report(p)).append(button("Назад","main"));
        else if (npc.getId()==TRAINER)
        {
            body.append("<font color=\"LEVEL\">Тренер и аналитик</font><br>Отдельная дуэль с ботом. Без рейтинговых очков и наград.<br>");
            if (page.equals("classes"))
            {
                for (BotClassPreset preset:BotPresets.getPresetList()) body.append(button(preset.getClassName(),"class "+preset.getClassId()));
                body.append(button("Назад","main"));
            }
            else
            {
                body.append("Противник: ").append(escape(BotPresets.getPresetByClassId(selectedClass(p)).getClassName())).append("<br>")
                    .append("Сложность: ").append(DIFF_NAMES.get(selectedDifficulty(p))).append("<br>")
                    .append("Бот 85 уровня: те же настройки снаряжения и заточки умений, что у рейтинговых ботов.<br>")
                    .append(button("Выбрать класс","classes"));
                for (String d:DIFFICULTIES) body.append(button(DIFF_NAMES.get(d),"difficulty "+d));
                body.append("Снаряжение и уровень игрока не копируются и не меняются.<br>")
                    .append(button("Начать тренировку","start")).append(button("Повторить последний бой","repeat"))
                    .append(button("Моя статистика","stats")).append(button("Завершить тренировку","leave"))
                    .append("HP/MP от 50%, без саммона, боя и очереди. После запуска дуэли около 6 сек. — " + TacticalSettings.getTrainingPreparationSeconds() + " сек. на селфбаф; общий лимит 120 сек., включая подготовку.<br>")
                    .append("Откаты ваших умений сбрасываются один раз после подтверждённого входа на арену, не в конце боя. Предбоевые усиления бота — примерно за 3 сек. до боя.<br>")
                    .append("На арене: <font color=\"LEVEL\">.training leave</font>. Бой остановится; безопасный возврат ждёт завершения дуэли — до остатка общего лимита 120 сек. Состояние: .training status.<br>")
                    .append(escape(TrainingArena.describe(p)));
            }
        }
        else if (npc.getId()==PREPARATION && page.equals("supplyMenu"))
        {
            String[] names={"S Soulshots","S Blessed Spiritshots","S Arrows","S Bolts","Spirit Ore","Beast Soulshots"};
            int[] caps={5000,5000,2000,2000,1000,2000};
            body.append("Выберите целевой запас: имеющееся количество учитывается, лишнее не удаляется.<br>");
            for (int i=0;i<names.length;i++)
            {
                body.append(button(names[i]+": до 100","supply "+i+" 100")).append(button(names[i]+": до 1000","supply "+i+" 1000"));
                if(caps[i]>1000) body.append(button(names[i]+": до "+caps[i],"supply "+i+" "+caps[i]));
            }
            body.append(button("Назад","main"));
        }
        else if (npc.getId()==PREPARATION)
        {
            body.append("<font color=\"LEVEL\">Мастер подготовки</font><br>")
                .append(button("Проверить готовность","check")).append(button("Пополнить весь запас","supplies")).append(button("Выбрать расходник и количество","supplyMenu"))
                .append("До 5000 S soulshots и blessed spiritshots; 2000 S стрел/болтов и Beast Soulshots; 1000 Spirit Ore. Без HP/MP/CP банок.<br>");
            for(int i=0;i<3;i++) body.append(button("Сохранить: "+SET_NAMES[i],"save "+i)).append(button("Надеть: "+SET_NAMES[i],"equip "+i));
            body.append("Только собственные вещи из инвентаря. Нет выдачи экипировки, лечения и сброса откатов.<br>Недоступно в бою, очереди, матчах, дуэлях и тренировках.");
        }
        else if (npc.getId()==HOST)
        {
            body.append("<font color=\"LEVEL\">Распорядитель Олимпиады</font><br>")
                .append("В очереди: ").append(Olympiad.getInstance().isRegistered(p)?"да":"нет").append("<br>")
                .append(button("Бесклассовый бой","register")).append(button("Отменить заявку","cancel")).append(button("Мои результаты","stats"))
                .append(button("Список и просмотр боёв","watch")).append(button("Как тренироваться","trainingHelp"))
                .append("Удалённо: /oly, /oly cancel, /oly status (также .oly).<br>Для наблюдения используйте штатного менеджера Олимпиады и список текущих арен.<br>")
                .append("Тренер: рядом, отдельные бои без очков. Заточка/аугментация и адена: соседние сервисные NPC.<br>")
                .append("Серверные правила: 13 стартовых бафов; без CoV/танцев/песен; подробности у Справочника сервера.");
        }
        else
        {
            if (HELP.containsKey(page)) body.append(HELP.get(page)).append(button("Все разделы","main"));
            else
            {
                body.append("<font color=\"LEVEL\">Справочник сервера</font><br>Объясняю кастомные системы и команды.<br>");
                for(String[] item:HELP_MENU) body.append(button(item[1],"help "+item[0]));
                body.append("<br>Состояние дополнительных модулей по конфигам:<br>").append(moduleStatus());
                if(p.isGM())
                {
                    body.append("<br>Только GM: сложность рейтинговых ботов. Изменения сохраняются.<br>");
                    for(String d:DIFFICULTIES) body.append(button("GM: "+DIFF_NAMES.get(d),"gmDifficulty "+d));
                }
            }
        }
        HtmlUtil.sendCBHtml(p,frame(body.toString()),npc.getObjectId());
    }
    private static final String[][] HELP_MENU={
        {"gear","Заточка и аугментация"},{"adena","Бесплатная адена и подготовка"},
        {"olympiad","Правила Олимпиады и команды"},{"training","Тренировки и сложность"},
        {"analytics","Статистика и сравнение боёв"},{"bots","Как работают боевые боты"},
        {"community","Alt+B, торговля и модули"},{"commands","Команды и дополнительные системы"},{"admin","Управление для администратора"}};
    private static final Map<String,String> HELP=Map.ofEntries(
        Map.entry("gear","<font color=\"LEVEL\">Экипировка</font><br>Enchanter: бесплатная заточка экипировки по слотам, от +1 до +16.<br>Skill Enchanter: выберите навык и маршрут заточки; применяется максимальная доступная заточка маршрута. Cost-only навыки отдельно отмечены.<br>Blacksmith Aug: выберите своё оружие, затем аугмент-умение; можно снять аугментацию. Умение активно от надетого оружия. Смена заменяет прежнюю аугментацию.<br>Мастера атрибутов нет: этот пакет его не добавляет."),
        Map.entry("adena","<font color=\"LEVEL\">Адена и подготовка</font><br>Adena Giver: разговор открывает окно; кнопка добавляет 100 000 000 адены. Можно повторять, пока хватает места до лимита. В бою/торговле выдачи нет.<br>Мастер подготовки пополняет фиксированный запас S-расходников, стрел/болтов, Spirit Ore и Beast Soulshots. Не включает автошоты за игрока: активируйте нужные в клиенте.<br>Сохраните до 3 комплектов надетых вещей и переодевайтесь вне боя/очереди. Вещи не копируются; проданные/переданные предметы делают набор недоступным."),
        Map.entry("olympiad","<font color=\"LEVEL\">Олимпиада</font><br>/oly или .oly — бесклассовая регистрация. /oly cancel — отмена; /oly status — состояние и очки. Нужны условия штатной Олимпиады, включая ноблес и допустимое состояние.<br>13 стартовых бафов выдаются обоим участникам. CoV/аналоги, танцы и песни запрещены. Длинные селфбафы после старта ограничены серверными правилами.<br>У ботов есть кастомные правила ресурсов/классов; это не retail-Олимпиада. Очки ноблеса при входе по текущему правилу сервера устанавливаются в 200.<br>Наблюдение — штатный менеджер Олимпиады. Стен и геодаты этот пакет не добавляет."),
        Map.entry("training","<font color=\"LEVEL\">Тренировка</font><br>У Тренера выберите один из 18 классов и сложность. Вход с HP/MP от 50%, без саммона, очереди, боя и другого инстанса; CP может быть неполным.<br>Отдельный инстанс: запуск дуэли около 6 сек., затем 10–60 сек. на селфбаф (по конфигу, по умолчанию 10). Оба получают 13 базовых бафов и олимпийский профиль класса. Ваши эффекты не очищаются; новые бафы имеют обычную длительность. Общий лимит дуэли 120 сек., включая подготовку. Без рейтинговых очков и наград. Это тренировочная дуэль, не полная копия правил ядра Олимпиады.<br>normal / experienced / strong меняют реакцию и выбор действий, не урон/скорость. Все боты 85 уровня с теми же настройками снаряжения и заточки умений, что у рейтинговых ботов. Уровень/экипировка игрока не копируются и не меняются; старый сравнительный профиль использует ту же серверную базу.<br>Откаты ваших умений сбрасываются один раз после подтверждённого входа на арену, не после боя. Короткие предбоевые усиления бота применяются один раз примерно за 3 сек. до начала боя.<br>.training leave — остановить бой и дождаться штатного завершения дуэли и безопасного возврата (до остатка общего лимита 120 сек.); победа боту автоматически не присуждается. .training status — состояние, .training report — отчёт. Повтор запускается у Тренера."),
        Map.entry("analytics","<font color=\"LEVEL\">Аналитика</font><br>Кнопка Моя статистика у Тренера/Распорядителя или .training report.<br>Рейтинговые бои и тренировки отмечены отдельно. Сравнивайте один класс и одинаковую сложность/профиль.<br>Урон/применения навыков считаются по серверным событиям; подробности и ограничения полей указаны в отчёте. Если штатный результат недоступен, победитель не угадывается.<br>Статистика собирается после установки пакета, старые бои не восстанавливаются задним числом."),
        Map.entry("bots","<font color=\"LEVEL\">Боевые боты</font><br>18 классов в ростере. Боты — клиентless Player, не декоративные NPC.<br>Тактика учитывает состояние цели, дистанцию, доступность навыков, контроль и опасность. Перехват использует уже принятую сервером команду движения противника, не его будущие действия.<br>Касты/контроль не отменяются ради бега. Берсеркер на рейтинговой Олимпиаде сохраняет собственный фазовый драйвер Rush/отхода.<br>Без геодаты нет гарантии обхода стен и удержания на точном контуре арены. Равного по скорости бегущего прямо противника одним перехватом догнать нельзя."),
        Map.entry("community","<font color=\"LEVEL\">Alt+B и прочие модули</font><br>Alt+B открывает серверную Community Board. Доступные магазин/бафы/телепорты зависят от её активных страниц; наличие файлов не означает включение услуги.<br>Офлайн-торговля: откройте обычную продажу/покупку или крафт и используйте включённый на сервере способ выхода/.offline. Не путать с офлайн-боем.<br>Премиум, ребёрс, свадьбы, фракции, трансмогрификация, ноблес-мастер, банк и торговля бафами — отдельные опциональные модули. Список ниже показывает только основной флаг конфигурации, не гарантирует спавн их NPC. Отключённые услуги справочник не включает.<br>Подробное описание всех модулей и путей конфигов: structure/03-game-config.md и 08-admin-and-mods.md у администратора."),
        Map.entry("commands","<font color=\"LEVEL\">Прочие системы</font><br>/time — серверное время. .offline — офлайн-торговля/крафт после открытия соответствующей лавки. .play — интерфейс штатного автобоя, если разрешён; не замена тренировкам и не совет для Олимпиады.<br>Опциональные .online, .findpvp, .hellbound, .changepassword работают только при включённых соответствующих модулях. Пароли вводятся только в штатном игровом механизме, не в сообщения справочнику.<br>Банк меняет адену на слитки по серверному курсу; ребёрс меняет развитие персонажа и может сбрасывать навыки — читайте подтверждение его NPC. Ноблес/профы/снижение уровня доступны только если включён соответствующий мастер.<br>Трансмогрификация — внешний вид, свадьбы — отдельный социальный модуль, фракции — команды Good/Evil. Премиум, торговля бафами и почтовые расширения зависят от флагов администратора; справочник их не активирует.<br>Случайные спавны/чемпионы/множители NPC, рейд-анонсы, PvP-награды/цвета/анонсы, приветствие и онлайн-информация работают автоматически, если включены. Капча, лимиты окон/IP, антибот, ограничения лавок и модерация — защитные правила, а не услуги для обхода.<br>Звёзды стана, панель противника Олимпиады и серверные исправления Blink/Bluff применяются автоматически; отдельной кнопки включения у этого NPC нет."),
        Map.entry("admin","<font color=\"LEVEL\">Администратору</font><br>//bot — управление ростером, ротациями, экипировкой и паузой боя. Не давайте обычным игрокам GM-права ради сервисов.<br>Рейтинговые боты по умолчанию strong. Сложность тактики: config/Custom/OlympiadTactics.ini или GM-кнопки главной страницы этого NPC. Enabled / TrainingEnabled требуют перезапуска; отключение новых тренировок не выключает восстановление; отдельная сложность тренировки задаётся у Тренера.<br>Логи тактики/ошибок — GameServer. История/восстановление тренировок — data/training-arena-recovery. Не удаляйте журнал незавершённого возврата.<br>Установка, проверки и откат: structure/20-olympiad-suite-and-giran-services.md. Нужны резерв кода И отдельный дамп живой БД перед установкой."));
    private static String moduleStatus()
    {
        String[][] modules={{"OfflineTrade.ini","OfflineTradeEnable","Офлайн-торговля"},{"OfflinePlay.ini","EnableOfflinePlayCommand","Офлайн-бой"},
            {"PremiumSystem.ini","EnablePremiumSystem","Премиум"},{"Rebirth.ini","RebirthAllow","Ребёрс"},{"Wedding.ini","AllowWedding","Свадьбы"},
            {"Transmog.ini","TransmogEnabled","Трансмогрификация"},{"FactionSystem.ini","EnableFactionSystem","Фракции"},
            {"NoblessMaster.ini","Enabled","Ноблес-мастер"},{"Banking.ini","BankingEnabled","Банк"},{"SellBuffs.ini","SellBuffEnable","Торговля бафами"},
            {"AutoPlay.ini","EnableAutoPlay","Автобой"},{"AutoPotions.ini","AutoPotionsEnabled","Автопотики"},{"CommunityBoard.ini","CustomCommunityBoard","Расширенная Community Board"}};
        StringBuilder out=new StringBuilder();
        for(String[] m:modules)
        {
            Properties values=new Properties(); String state="не определено";
            Path file=Path.of("config/Custom",m[0]);
            try(InputStream in=Files.newInputStream(file))
            {
                values.load(in);String v=values.getProperty(m[1]);
                if(v!=null) state=Boolean.parseBoolean(v.trim())?"включён":"отключён";
            }
            catch(Exception ignored) { }
            out.append(m[2]).append(": ").append(state).append("<br>");
        }
        return out.toString();
    }
    static String npcEvent(String command)
    {
        if (command == null || !command.startsWith("GiranService ")) return null;
        String event = command.substring("GiranService ".length()).trim();
        return event.isEmpty() || event.length()>100 ? null : event;
    }
    /** Core passes the NPC from validated HTML action provenance here. Unlike
     * ScriptLink, this does not consult Player.lastFolkNpc or the current target. */
    private final class NpcCommands implements IBypassHandler
    {
        @Override public String[] getCommandList() { return new String[]{"GiranService"}; }
        @Override public boolean onCommand(String command, Player player, Creature target)
        {
            if (player == null) return false;
            if (target == null || !target.isNpc())
            { player.sendMessage("Откройте меню нужного NPC рядом с ним."); return false; }
            String event = npcEvent(command);
            if (event == null) { player.sendMessage("Некорректная команда меню. Поговорите с NPC заново."); return false; }
            handleNpcEvent(event,target.asNpc(),player);
            return true;
        }
    }
    /** Remote commands never grant items/equip/register/start; only own training exit/report. */
    private static final class RemoteTraining implements IVoicedCommandHandler
    {
        @Override public String[] getCommandList() { return new String[]{"training"}; }
        @Override public boolean onCommand(String command,Player player,String target)
        {
            if(player==null || !"training".equals(command)) return false;
            String action=target==null?"status":target.trim();
            if(action.equals("leave")) player.sendMessage(TrainingArena.leave(player));
            else if(action.equals("report")) HtmlUtil.sendCBHtml(player,frame(CombatAnalytics.report(player)),0);
            else player.sendMessage(TrainingArena.describe(player)+" | .training leave / .training report");
            return true;
        }
    }
    public static void main(String[] args) { new GiranServices(); }
}
