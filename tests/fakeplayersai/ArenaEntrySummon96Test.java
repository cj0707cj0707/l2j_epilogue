package custom.fakeplayersai;

import java.util.*;
import java.lang.invoke.*;
import org.l2jmobius.gameserver.entity.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.entity.actor.instance.Servitor;
import org.l2jmobius.gameserver.entity.actor.holders.player.Duel;
import org.l2jmobius.gameserver.entity.actor.templates.*;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.entity.actor.status.*;
import org.l2jmobius.gameserver.entity.actor.stat.SummonStat;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.skill.*;
import org.l2jmobius.gameserver.mechanics.stats.*;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.*;
import static custom.fakeplayersai.ArenaOrbitTranceManaTest.*;

/** Actual patched coordinates/skills/guards. Recording UI, cast and movement; no live DB/world. */
public class ArenaEntrySummon96Test extends TankChargeFinisher95Test
{
    static int serial=900000;
    static void duel96(Player p,Player e)throws Exception
    {
        for(Player a:List.of(p,e))
        {f(Player.class,"_isInDuel").setBoolean(a,true);f(Player.class,"_duelId").setInt(a,50);f(Player.class,"_duelState").setInt(a,Duel.DUELSTATE_DUELLING);}
    }
    static void entry96()throws Throwable
    {
        for(var preset:BotPresets.getPresets())for(boolean ranked:new boolean[]{false,true})
        for(boolean human:new boolean[]{false,true})for(int side:new int[]{-1,1})
        {
            Player p=actor(preset.getClassId(),ranked,false),e=actor(95,ranked,human);if(!ranked)duel96(p,e);
            int[] c=ArenaCombatRules.center(p);at(p,c[0]+side*1200,c[1]);at(e,c[0]-side*1200,c[1]);register(p,e,ranked,human);
            try
            {
                var commit=MethodHandles.privateLookupIn(WorldObject.class,MethodHandles.lookup()).findSpecial(WorldObject.class,"setXYZ",MethodType.methodType(void.class,int.class,int.class,int.class),WorldObject.class).bindTo(p);
                if(!human)ArenaCombatRules.update(e,p,BotPresets.getPresetByClassId(95));
                verify(!ArenaCombatRules.update(p,e,preset)&&p.getX()==c[0]+side*1200,"all19 start-room admission never snaps cid="+preset.getClassId());
                for(int radius:new int[]{1100,900,600,461,450})
                {
                    commit.invoke(c[0]+side*radius,c[1],0);
                    verify(p.getX()==c[0]+side*radius,"ordinary native position commits enter from start room, no clipped jump");
                    ArenaCombatRules.update(p,e,preset);
                }
                commit.invoke(c[0]+side*700,c[1],0);
                boolean limited=!human&&!ArenaCombatRules.meleeClass(p);
                verify(limited?Math.abs(p.getX()-c[0])<=460:p.getX()==c[0]+side*700,"ranged boundary activates only after walking into disk; melee pursuit unchanged");
            }
            finally{cleanup(p,e);}
        }
    }
    static Servitor pet96(Player owner,int id)throws Exception
    {
        Servitor pet=pet(owner,new PetAI[1]);NpcTemplate template=(NpcTemplate)unsafe.allocateInstance(NpcTemplate.class);
        f(NpcTemplate.class,"_id").setInt(template,id);f(NpcTemplate.class,"_level").setByte(template,(byte)83);
        f(NpcTemplate.class,"_targetable").setBoolean(template,true);
        f(CreatureTemplate.class,"_collisionRadius").setInt(template,10);f(CreatureTemplate.class,"_baseHpMax").setFloat(template,10000);
        f(CreatureTemplate.class,"_baseMpMax").setFloat(template,10000);f(Creature.class,"_template").set(pet,template);
        f(Creature.class,"_calculators").set(pet,new Calculator[Stat.NUM_STATS]);f(Creature.class,"_stat").set(pet,new SummonStat(pet));
        var status=(SummonStatus)unsafe.allocateInstance(SummonStatus.class);f(CreatureStatus.class,"_creature").set(status,pet);
        f(CreatureStatus.class,"_currentHp").setDouble(status,10000);f(CreatureStatus.class,"_currentMp").setDouble(status,10000);f(Creature.class,"_status").set(pet,status);
        f(Creature.class,"_skills").set(pet,new HashMap<Integer,Skill>());f(Creature.class,"_zones").set(pet,new byte[64]);
        f(WorldObject.class,"_objectId").setInt(pet,serial++);f(WorldObject.class,"_isSpawned").setBoolean(pet,true);
        f(WorldObject.class,"_location").set(pet,new Location(owner.getX(),owner.getY(),0,0,owner.getInstanceId()));
        return pet;
    }
    static Player armed96(BotClassPreset preset,Player e,boolean ranked)throws Exception
    {
        Player p=knight(preset.getClassId(),ranked,e);var inv=(NativeDebuffEpicTest.TestInventory)p.getInventory();
        var weapon=new NativeDebuffEpicTest.TestItem(BotPresets.getWeaponId(preset,true));inv.own(weapon);inv.setPaperdollItem(Inventory.PAPERDOLL_RHAND,weapon);
        for(int id:preset.getRotation())if(nativeSkill(id)!=null)learn(p,nativeSkill(id));
        for(int id:new int[]{3031,5589,7575,1341,9632}){var item=new NativeDebuffEpicTest.TestItem(id);item.setCount(1000);inv.own(item);}
        if(preset.getClassId()==97)learn(p,nativeSkill(1230));
        ((java.util.concurrent.atomic.AtomicInteger)f(Player.class,"_charges").get(p)).set(7);
        int[] c=ArenaCombatRules.center(p);at(p,c[0]+30,c[1]);at(e,c[0],c[1]);if(!ranked)duel96(p,e);
        return p;
    }
    static OnCreatureSkillUse event96(Player p,Creature target,int id)
    {var event=new OnCreatureSkillUse();event.setCaster(p);event.setSkill(nativeSkill(id));event.setTarget(target);event.setTargets(List.of(target));return event;}
    static void summons96()throws Exception
    {
        for(var preset:BotPresets.getPresets())for(boolean ranked:new boolean[]{false,true})for(int owner:new int[]{90,91})for(boolean human:new boolean[]{false,true})
        {
            Player e=actor(owner,ranked,human),p=armed96(preset,e,ranked);register(p,e,ranked,human);Servitor pet=pet96(e,owner==90?14918:14835);
            try
            {
                targetEnemy=pet;last=0;
                verify(BotTactics.tickSummonPriority(p,e,preset,ranked),"every class owns summon-first phase cid="+preset.getClassId());
                verify(BotTactics.prioritySummon(p,e)==pet,"actual current owned tank summon chosen");
                verify(p.getTarget()==pet||p.getTarget()==p&&last!=0,"never select owner while priority pet is alive");
                verify(last!=5,"no Double Sonic Slash in pet phase");
                verify(last!=0&&p.getTarget()==pet&&BotTactics.directDamage(p.getKnownSkill(last)),"every class requests actual native damage on pet cid="+preset.getClassId()+" skill="+last);
                verify(BotRecoveryRules.guard(event96(p,e,1230))!=null,"legacy owner-directed damage vetoed during pet focus");
                var attack=new OnCreatureAttack();attack.setAttacker(p);attack.setTarget(e);
                verify(BotRecoveryRules.bluffAttack(attack)!=null,"legacy owner autoattack vetoed");
                if(last!=0&&p.getTarget()==pet)verify(BotRecoveryRules.guard(event96(p,pet,last))==null,"native pet cast passes shared guard cid="+preset.getClassId()+" skill="+last);
                if(p.isCastingNow())finish(p,e,publicState(p),Long.MAX_VALUE/4);
                // The pet dying/removing releases the phase; it is never resurrected by target selection.
                f(Player.class,"_summon").set(e,null);p.setCastingNow(false);last=0;
                verify(!BotTactics.tickSummonPriority(p,e,preset,ranked)&&p.getTarget()==e,"after pet death return to owner and normal class driver");
            }
            finally{p.setCastingNow(false);cleanup(p,e);}
        }
    }
    static void necro96()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(var preset:BotPresets.getPresets())
        {
            Player e=actor(95,ranked,true),p=armed96(preset,e,ranked);register(p,e,ranked,true);
            var pet=pet96(e,14110);targetEnemy=pet;
            try
            {
                BotTactics.beginCombat(p,e);
                verify(BotTactics.prioritySummon(p,e)==null,"Cursed Man not prioritized before real owner invulnerability");
                e.setInvul(true);last=0;verify(BotTactics.tickSummonPriority(p,e,preset,ranked)&&BotTactics.prioritySummon(p,e)==pet,"real necromancer invulnerability redirects to Cursed Man");
                e.setInvul(false);verify(BotTactics.prioritySummon(p,e)==pet,"finish same Cursed Man even when invulnerability ends");
                if(p.isCastingNow())finish(p,e,publicState(p),Long.MAX_VALUE/4);
                f(WorldObject.class,"_location").set(pet,new Location(e.getX(),e.getY(),0,0,11));
                verify(BotTactics.prioritySummon(p,e)==null,"foreign instance invalidates pet target");
                f(WorldObject.class,"_location").set(pet,new Location(e.getX(),e.getY(),0,0,p.getInstanceId()));e.setInvul(true);pet.setInvul(true);last=0;
                verify(BotTactics.tickSummonPriority(p,e,preset,ranked)&&last==0,"invulnerable pet is held, never attacked or bypassed to protected owner");
            }
            finally{p.setCastingNow(false);cleanup(p,e);}
        }
    }
    static void chargedPet96()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{90,91,106})
        {
            var preset=BotPresets.getPresetByClassId(cid);Player e=actor(91,ranked,true),p=armed96(preset,e,ranked);register(p,e,ranked,true);
            Servitor pet=pet96(e,14835);targetEnemy=pet;apply(p,trigger(cid),3);
            try
            {
                last=0;verify(BotTactics.tickSummonPriority(p,e,preset,ranked)&&last==finisher(cid),"stage3 finisher retained during pet priority cid="+cid);
                var event=event96(p,p,finisher(cid));event.setTargets(cid==91?List.of(pet):List.of(p));
                verify(BotRecoveryRules.guard(event)==null,"native AURA main-target self still recognizes contact Crusher pet hit");
                if(p.isCastingNow())finish(p,e,publicState(p),Long.MAX_VALUE/4);
            }
            finally{p.setCastingNow(false);cleanup(p,e);}
        }
    }
    static void chargeBuildersPet96()throws Exception
    {
        for(boolean ranked:new boolean[]{false,true})for(int cid:new int[]{88,114})for(boolean self:new boolean[]{false,true})
        {
            var preset=BotPresets.getPresetByClassId(cid);int[] old=(int[])f(BotClassPreset.class,"_rotationOverride").get(preset);
            int id=self?(cid==88?8:50):(cid==88?345:346);preset.setRotationOverride(new int[]{id});
            Player e=actor(91,ranked,true),p=armed96(preset,e,ranked);register(p,e,ranked,true);var pet=pet96(e,14835);targetEnemy=pet;
            ((java.util.concurrent.atomic.AtomicInteger)f(Player.class,"_charges").get(p)).set(0);
            try
            {
                last=0;verify(BotTactics.tickSummonPriority(p,e,preset,ranked)&&last==id&&p.getTarget()==(self?p:pet),"self8/50 and targeted345/346 stay distinct against pet id="+id);
            }
            finally{p.setCastingNow(false);cleanup(p,e);preset.setRotationOverride(old);}
        }
        for(boolean ranked:new boolean[]{false,true})
        {
            var preset=BotPresets.getPresetByClassId(97);Player e=actor(91,ranked,true),p=armed96(preset,e,ranked);register(p,e,ranked,true);
            var pet=pet96(e,14835);targetEnemy=pet;learn(p,nativeSkill(1394));shield(p,2);
            try{last=0;verify(BotTactics.tickSummonPriority(p,e,preset,ranked)&&last==1394&&p.getTarget()==pet,"protected Cardinal targets pet with native Trance, never the owner");}
            finally{p.setCastingNow(false);cleanup(p,e);}
        }
    }
    static void stunAI96()throws Exception
    {
        for(int cid:new int[]{92,102,109})for(boolean ranked:new boolean[]{false,true})
        {
            var preset=BotPresets.getPresetByClassId(cid);int[] old=(int[])f(BotClassPreset.class,"_rotationOverride").get(preset);
            preset.setRotationOverride(new int[]{101});
            try
            {
                Player e=actor(88,ranked,true),p=armed96(preset,e,ranked);effects(e).add(new BuffInfo(p,e,nativeSkill(101)));
                flags(e,org.l2jmobius.gameserver.mechanics.effects.EffectFlag.STUNNED.getMask());
                verify(run(p,e,centered(),10000)&&last==101,"archer AI may request101 over existing stun cid="+cid);p.setCastingNow(false);
            }
            finally{preset.setRotationOverride(old);}
        }
    }
    static void duelist96()throws Exception
    {
        var preset=BotPresets.getPresetByClassId(88);int[] old=(int[])f(BotClassPreset.class,"_rotationOverride").get(preset),buffs=(int[])f(BotClassPreset.class,"_buffsOverride").get(preset);
        try
        {
            preset.setRotationOverride(new int[]{5,261,5});preset.setBuffsOverride(new int[]{5,78});
            verify(Arrays.equals(preset.getRotation(),new int[]{261})&&Arrays.equals(preset.getBuffs(),new int[]{78}),"all effective lists sanitize stale Double Sonic Slash overrides");
            Player p=owner(88,false),e=actor(90,false,true);register(p,e,false,true);
            try{verify(BotRecoveryRules.guard(event96(p,e,5))!=null,"training native skill5 veto");}
            finally{cleanup(p,e);}
            @SuppressWarnings("unchecked") var bots=(Map<Integer,Player>)f(BotManager.class,"BOTS").get(null);bots.put(p.getObjectId(),p);
            try{verify(BotRecoveryRules.guard(event96(p,e,5))!=null,"world native skill5 veto, not only arena");}
            finally{bots.remove(p.getObjectId());}
        }
        finally{preset.setRotationOverride(old);preset.setBuffsOverride(buffs);}
    }
    public static void main(String[]args){try
    {
        org.l2jmobius.gameserver.config.ServerConfig.DATAPACK_ROOT=new java.io.File(args[0]);org.l2jmobius.commons.config.ThreadConfig.THREADS_FOR_LOADING=false;
        unsafe=(sun.misc.Unsafe)f(sun.misc.Unsafe.class,"theUnsafe").get(null);f(LowManaAutoAttackTest.class,"unsafe").set(null,unsafe);CpMeleeRushRecoveryTest.unsafe=unsafe;
        handlers.EffectMasterHandler.main(new String[0]);skill=org.l2jmobius.gameserver.data.xml.SkillData.getInstance().getSkill(1510,1);recordingType();state=Class.forName("custom.fakeplayersai.BotTactics$State");
        act=BotTactics.class.getDeclaredMethod("act",Player.class,Player.class,BotClassPreset.class,state,long.class,boolean.class);act.setAccessible(true);
        poll=BotTactics.class.getDeclaredMethod("pollAttempt",Player.class,Player.class,state,long.class);poll.setAccessible(true);
        var geo=java.nio.file.Files.createTempDirectory("arena96-geo-");org.l2jmobius.gameserver.config.GeoEngineConfig.GEODATA_PATH=geo;org.l2jmobius.gameserver.geoengine.GeoEngine.getInstance();java.nio.file.Files.delete(geo);
        org.l2jmobius.gameserver.handler.TargetHandler.getInstance().registerHandler(new handlers.skill.targets.Party());
        entry96();summons96();necro96();duelist96();stunAI96();chargedPet96();chargeBuildersPet96();
        System.out.println("PASS: "+checks+" rule96 all19 entry/summon focus/guards/necromancer latch/Duelist checks; no live server");System.exit(0);
    }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
