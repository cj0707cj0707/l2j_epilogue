package custom.fakeplayersai;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import org.l2jmobius.commons.config.ThreadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.mechanics.skill.holders.*;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;
import sun.misc.Unsafe;

/** Native templates/route availability and actual bot migration, without live DB/world. */
public class NativeDebuffEpicTest {
    // Reproduce only the previous route-selection policy to distinguish changed vs already-Chance.
    static int previousLevel(Skill base,EnchantSkillLearn learn)throws Exception {
        if(learn==null)return base.getLevel();
        Method max=BotSpawner.class.getDeclaredMethod("getRouteMaxLevel",EnchantSkillLearn.class,int.class),label=BotSpawner.class.getDeclaredMethod("getRouteLabel",Skill.class,Skill.class,int.class),penalty=BotSpawner.class.getDeclaredMethod("getMovePenalty",Skill.class);
        max.setAccessible(true);label.setAccessible(true);penalty.setAccessible(true);
        double basePenalty=(double)penalty.invoke(null,base),left=basePenalty;int duel=0,power=0,chance=0,lessPenalty=0;
        var routes=new ArrayList<>(learn.getAllRoutes());Collections.sort(routes);
        for(int route:routes) {
            int level=(int)max.invoke(null,learn,route);if(level==0)continue;Skill s=SkillData.getInstance().getSkill(base.getId(),level);if(s==null)continue;
            if(basePenalty>0){double n=(double)penalty.invoke(null,s);if(n<basePenalty&&n<left){left=n;lessPenalty=level;}}
            String kind=(String)label.invoke(null,base,s,route);
            if(kind.equals("Duel")||!base.hasEffects(EffectScope.PVP)&&s.hasEffects(EffectScope.PVP)){if(duel==0)duel=level;}
            else if(kind.equals("Power")&&power==0)power=level;
            else if(kind.equals("Chance")&&chance==0)chance=level;
            else if(base.isDamage()&&power==0&&kind.startsWith("Enchant "))power=level;
        }
        int target=base.isDamage()?(duel>0?duel:power>0?power:chance):basePenalty>0?lessPenalty:power>0?power:chance;
        return target==0?base.getLevel():target;
    }
    static void audit(String output)throws Exception {
        var data=SkillData.getInstance();var groups=EnchantSkillGroupsData.getInstance();var trees=SkillTreeData.getInstance();
        List<String> report=new ArrayList<>(),changes=new ArrayList<>();
        changes.add("| Профессия | Фактически изменённый маршрут заточки |\n| --- | --- |");
        report.add("| Профессия | Native Chance (ID, имя, заточка, серверный уровень) | Без маршрута Chance: обычный навык/прежняя допустимая заточка |");report.add("| --- | --- | --- |");
        for(var preset:BotPresets.getPresetList()) {
            var ids=new TreeSet<Integer>();for(var learn:trees.getCompleteClassSkillTree(PlayerClass.getPlayerClass(preset.getClassId())).values()) if(learn.getGetLevel()<=85)ids.add(learn.getSkillId());
            var changed=new ArrayList<String>();var chance=new ArrayList<String>();var fallback=new ArrayList<String>();
            for(int id:ids) {
                var learn=groups.getSkillEnchantmentBySkillId(id);Skill base=data.getSkill(id,learn==null?data.getMaxLevel(id):learn.getBaseLevel());if(base==null||base.isPassive()||base.getTargetType()==org.l2jmobius.gameserver.mechanics.skill.targets.TargetType.SELF||!(base.isDebuff()||base.hasNegativeEffect()))continue;
                int target=BotSpawner.debuffChanceLevel(base,learn);
                if(target>0) {
                    Skill s=data.getSkill(id,target);check(s!=null&&s.getActivateRate()>base.getActivateRate(),"real improving Chance route "+id);
                    check(target%100==30||target%100==15,"only native +30 or +15, never invented +30 "+id+"/"+target);
                    if(previousLevel(base,learn)!=target)changed.add(id+" "+base.getName()+" → Chance+"+(target%100));
                    chance.add(id+" "+base.getName()+" **+"+(target%100)+"** (`"+target+"`)");
                } else if((preset.getClassId()==90||preset.getClassId()==91||preset.getClassId()==106) && base.isDebuff()) fallback.add(id+" "+base.getName());
            }
            if(!changed.isEmpty())changes.add("| "+preset.getClassName()+" | "+String.join("; ",changed)+" |");
            report.add("| "+preset.getClassName()+" ("+preset.getClassId()+") | "+String.join("; ",chance)+" | "+(fallback.isEmpty()?"—":String.join("; ",fallback))+" |");
        }
        Files.write(Path.of(output),report);Files.write(Path.of(output+".changes.md"),changes);
        for(int[] pair:new int[][]{{1394,230},{1208,130},{260,130},{358,115},{353,115}}) {
            var learn=groups.getSkillEnchantmentBySkillId(pair[0]);var base=data.getSkill(pair[0],learn.getBaseLevel());
            int actual=BotSpawner.debuffChanceLevel(base,learn);check(actual==pair[1],"expected Chance route "+pair[0]+": "+actual);
        }
        for(int cid:new int[]{90,91,106,97,115,93}){
            Player p=actor(cid,true,false);Formulas.FIXED_DEBUFF_RATE_PLAYERS.add(p.getObjectId());
            BotManager.updateTankDebuffRegistration(p,BotPresets.getPresetByClassId(cid));check(!Formulas.FIXED_DEBUFF_RATE_PLAYERS.contains(p.getObjectId()),"no fixed25 registration for "+cid);
        }
        Player p=actor(97,true,false);learn(p,data.getSkill(1394,230));CardinalTrance.apply(p);check(p.getKnownSkill(1394)!=data.getSkill(1394,230)&&p.getKnownSkill(1394).getLevel()==230,"bot-only15 preserves Chance30 level");
        check(p.getKnownSkill(1394).getActivateRate()==15&&data.getSkill(1394,230).getActivateRate()==100,"only bot15, native unchanged");
        check(Arrays.equals(BotPresets.getJewelryIds(),new int[]{6660,6658,6659,6657,6656}),"five exact epic IDs");
        Method parse=BotPresets.class.getDeclaredMethod("parseIds",String.class);parse.setAccessible(true);
        check(Arrays.equals((int[])parse.invoke(null,"1394, 1398"),new int[]{1394,1398}),"rotation/buff override parser must not append equipment IDs");
        check(((int[])parse.invoke(null,"")).length==0,"empty override remains empty");
        for(int id:BotPresets.getJewelryIds())check(Arrays.stream(BotPresets.getAllGearIds()).anyMatch(gear->gear==id),"epic belongs only to gear cleanup "+id);
        int[] copy=BotPresets.getJewelryIds();copy[0]=1;check(BotPresets.getJewelryIds()[0]==6660,"immutable preset via defensive copy");
        for(boolean s84:new boolean[]{false,true}){BotManager.getSettings().setUseS84(s84);check(BotPresets.getJewelryIds().length==5&&BotPresets.getJewelryIds()[0]==6660,"S80/S84 cannot overwrite epic preset");}
        BotManager.getSettings().load();check(BotManager.getSettings().getEnchantJewel()==6,"persisted EnchantJewel=6");
    }
    static void nativeRolls()throws Exception {
        Player p=actor(90,true,false),target=actor(94,true,true);Skill root=SkillData.getInstance().getSkill(403,230);
        check(root.getActivateRate()==100,"actual root Chance30 high activation");
        Formulas.FIXED_DEBUFF_RATE_PLAYERS.add(p.getObjectId());int passed=0;
        for(int i=0;i<2000;i++)if(Formulas.calcEffectSuccess(p,target,root))passed++;
        check(passed>350&&passed<650,"reproduce old fixed25 core gate: "+passed);
        BotManager.updateTankDebuffRegistration(p,BotPresets.getPresetByClassId(90));
        for(int i=0;i<200;i++)check(Formulas.calcEffectSuccess(p,target,root),"restored native root, no fixed25");
        var name=new org.l2jmobius.gameserver.util.StatSet();name.set("name","TargetCancel");var params=new org.l2jmobius.gameserver.util.StatSet();params.set("chance",100);
        var cancel=new handlers.skill.effects.TargetCancel(null,null,name,params);
        @SuppressWarnings("unchecked") var roster=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);roster.put(p.getObjectId(),p);
        for(int i=0;i<200;i++)check(cancel.calcSuccess(p,target,root),"tank instant cancel uses native probability, not fixed25");roster.remove(p.getObjectId());
        Player human=actor(97,true,true);Skill original=SkillData.getInstance().getSkill(1394,10);learn(human,original);
        check(BotSpawner.applySkillEnchants(human)==0&&human.getKnownSkill(1394)==original,"human enchant excluded");
        BotSpawner.ensurePresetJewelry(human);check(human.getInventory().getPaperdollItems().isEmpty(),"human jewelry excluded");
    }

    static Unsafe unsafe;
    static Class<?> recording;
    static int writes, learnedWrites, seq=900000;
    public static Skill persist(Player p,Skill skill,boolean store)throws Exception {
        if(store)learnedWrites++;
        @SuppressWarnings("unchecked") var skills=(Map<Integer,Skill>)f(Creature.class,"_skills").get(p);
        return skills.put(skill.getId(),skill);
    }
    static Player recordingPlayer(int cid,boolean ranked)throws Exception {
        if(recording==null) {
            var player=java.lang.constant.ClassDesc.of(Player.class.getName());var sk=java.lang.constant.ClassDesc.of(Skill.class.getName());var self=java.lang.constant.ClassDesc.of(NativeDebuffEpicTest.class.getName());
            byte[] bytes=java.lang.classfile.ClassFile.of().build(java.lang.constant.ClassDesc.of("custom.fakeplayersai.EpicRecordingPlayer"),c->c.withSuperclass(player)
                .withMethodBody("addSkill",java.lang.constant.MethodTypeDesc.of(sk,sk,java.lang.constant.ConstantDescs.CD_boolean),java.lang.classfile.ClassFile.ACC_PUBLIC,b->b.aload(0).aload(1).iload(2).invokestatic(self,"persist",java.lang.constant.MethodTypeDesc.of(sk,player,sk,java.lang.constant.ConstantDescs.CD_boolean)).areturn())
                .withMethodBody("refreshOverloaded",java.lang.constant.MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_void),java.lang.classfile.ClassFile.ACC_PUBLIC,b->b.return_())
                .withMethodBody("updateAndBroadcastStatus",java.lang.constant.MethodTypeDesc.of(java.lang.constant.ConstantDescs.CD_void),java.lang.classfile.ClassFile.ACC_PUBLIC,b->b.return_()));
            recording=java.lang.invoke.MethodHandles.lookup().defineClass(bytes);
        }
        Player base=actor(cid,ranked,false),p=(Player)unsafe.allocateInstance(recording);
        for(Class<?> c=Player.class;c!=Object.class;c=c.getSuperclass())for(Field field:c.getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())){field.setAccessible(true);field.set(p,field.get(base));}
        p.setPrivateStoreType(org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType.NONE);
        f(Player.class,"_accessLevel").set(p,AdminData.getInstance().getAccessLevel(0));return p;
    }
    // DB and owner-skill/packet seams only. Native Item enchant/location and Inventory slot selection remain real.
    static class TestItem extends org.l2jmobius.gameserver.entity.item.instance.Item {
        TestItem(int id){super(seq++,id);}
        @Override public void updateDatabase(){writes++;}
        @Override public void updateDatabase(boolean force){writes++;}
        @Override public void giveSkillsToOwner(){}
        @Override public void removeSkillsFromOwner(){}
        @Override public void clearEnchantStats(){}
        @Override public void applyEnchantStats(){}
    }
    static class TestInventory extends org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory {
        TestInventory(Player owner){super(owner);}
        @Override public org.l2jmobius.gameserver.entity.item.instance.Item addItem(org.l2jmobius.gameserver.entity.item.enums.ItemProcessType type,int id,long count,Player owner,Object ref){var item=new TestItem(id);_items.add(item);return item;}
        void own(org.l2jmobius.gameserver.entity.item.instance.Item item){_items.add(item);}
    }
    static void skillsAndGear()throws Exception {
        var trees=SkillTreeData.getInstance();var data=SkillData.getInstance();var groups=EnchantSkillGroupsData.getInstance();
        for(var preset:BotPresets.getPresetList())for(boolean ranked:new boolean[]{false,true}) {
            Player p=recordingPlayer(preset.getClassId(),ranked);
            int expected=0;
            for(var entry:trees.getCompleteClassSkillTree(PlayerClass.getPlayerClass(preset.getClassId())).values())if(entry.getGetLevel()<=85) {
                int id=entry.getSkillId();Skill known=p.getKnownSkill(id);if(known==null||known.getLevel()<entry.getSkillLevel())learn(p,data.getSkill(id,entry.getSkillLevel()));
            }
            // Start from another valid enchant route to reproduce persisted older builds.
            for(Skill skill:new ArrayList<>(p.getAllSkills())) {
                var learn=groups.getSkillEnchantmentBySkillId(skill.getId());if(learn==null)continue;
                int level=BotSpawner.debuffChanceLevel(data.getSkill(skill.getId(),learn.getBaseLevel()),learn);
                if(level>0) {expected++;for(int route:learn.getAllRoutes())if(route!=level/100) {Skill old=data.getSkill(skill.getId(),route*100+1);if(old!=null){learn(p,old);break;}}}
            }
            Formulas.FIXED_DEBUFF_RATE_PLAYERS.add(p.getObjectId());BotSpawner.applySkillEnchants(p);
            check(!Formulas.FIXED_DEBUFF_RATE_PLAYERS.contains(p.getObjectId()),"enchant path clears fixed gate also for training");
            int found=0;
            for(Skill skill:p.getAllSkills()) {
                var learn=groups.getSkillEnchantmentBySkillId(skill.getId());if(learn==null)continue;
                int level=BotSpawner.debuffChanceLevel(data.getSkill(skill.getId(),learn.getBaseLevel()),learn);
                if(level>0){found++;check(BotControlChance.applies(preset.getClassId(),skill.getId()) ? skill.getLevel()==level&&skill.getActivateRate()==15 : skill==data.getSkill(skill.getId(),level),"native Chance except scoped bot15, "+preset.getClassName()+"/"+skill.getId());}
            }
            check(found==expected,"no absent skill learned by Chance policy");int before=learnedWrites;BotSpawner.applySkillEnchants(p);check(learnedWrites==before,"enchant idempotence");
            // Pure nukes' debuff-enchant routes must not be mistaken for Chance.
            for(int id:new int[]{1230,1235,1239}){var learn=groups.getSkillEnchantmentBySkillId(id);check(BotSpawner.debuffChanceLevel(data.getSkill(id,learn.getBaseLevel()),learn)==0,"do not switch pure nuke to added-debuff route "+id);}
            TestInventory inv=new TestInventory(p);f(org.l2jmobius.gameserver.entity.itemcontainer.Inventory.class,"_paperdollListeners").set(inv,new ArrayList<>());f(Player.class,"_inventory").set(p,inv);
            // Native two earrings/two rings slots; old complete jewelry must be replaced, not coexist in paperdoll.
            for(int id:new int[]{14165,14165,14163,14163,14164}){TestItem item=new TestItem(id);inv.own(item);inv.equipItem(item);}
            TestItem armor=new TestItem(13434),weapon=new TestItem(13457);armor.setEnchantLevel(13);weapon.setEnchantLevel(19);inv.own(armor);inv.own(weapon);inv.setPaperdollItem(org.l2jmobius.gameserver.entity.itemcontainer.Inventory.PAPERDOLL_CHEST,armor);inv.setPaperdollItem(org.l2jmobius.gameserver.entity.itemcontainer.Inventory.PAPERDOLL_RHAND,weapon);
            TestItem reuse=new TestItem(6658);reuse.setEnchantLevel(10);inv.own(reuse);
            BotSpawner.ensurePresetJewelry(p);
            Set<Integer> unique=new HashSet<>();for(var item:inv.getPaperdollItems())if(Arrays.stream(BotPresets.getJewelryIds()).anyMatch(id->id==item.getId())) {unique.add(item.getId());check(item.getEnchantLevel()==6,"epic +6 on paperdoll");}
            check(unique.size()==5,"all five unique epics equipped, "+preset.getClassName()+" ranked="+ranked);
            check(inv.getPaperdollItemByItemId(6658)==reuse,"existing Baium reused, no duplicate");
            check(armor.isEquipped()&&armor.getEnchantLevel()==13&&weapon.isEquipped()&&weapon.getEnchantLevel()==19,"armor and weapon unchanged");
            check(inv.getPaperdollItemByItemId(14165)==null&&inv.getPaperdollItemByItemId(14163)==null&&inv.getPaperdollItemByItemId(14164)==null,"old jewels unequipped");
            int size=inv.getSize();before=writes;BotSpawner.ensurePresetJewelry(p);check(size==inv.getSize()&&before==writes,"jewelry idempotent: no duplicates/DB churn");
            inv.getPaperdollItemByItemId(6658).setEnchantLevel(10);BotSpawner.ensurePresetJewelry(p);check(inv.getPaperdollItemByItemId(6658).getEnchantLevel()==6&&inv.getSize()==size,"restore persisted wrong enchant without new items");
            inv.unEquipItemInSlot(inv.getPaperdollItemByItemId(6656).getLocationSlot());BotSpawner.ensurePresetJewelry(p);check(inv.getPaperdollItemByItemId(6656)!=null&&inv.getSize()==size,"restore missing equipped slot from owned item");
        }
        check(learnedWrites>0&&writes>0,"ordinary learned/enchant/paperdoll persistence requested");
    }
    public static void main(String[] args)throws Exception {
        unsafe=(Unsafe)f(Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);
        ServerConfig.DATAPACK_ROOT=new File(args[0]);ThreadConfig.THREADS_FOR_LOADING=false;handlers.EffectMasterHandler.main(new String[0]);
        audit(args[1]);skillsAndGear();nativeRolls();System.out.println("PASS: "+checks+" native skill/route/class/preset checks; no live server/DB");System.exit(0);
    }
}
