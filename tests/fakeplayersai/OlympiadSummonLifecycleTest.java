package org.l2jmobius.gameserver.mechanics.olympiad;
import custom.fakeplayersai.*;
import org.l2jmobius.gameserver.entity.actor.*;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/** Actual shipped helper and gate, native world/skill effects replaced by inert doubles. */
public class OlympiadSummonLifecycleTest
{
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static OlympiadGame match(Player p,Player enemy)
    {
        p.game=enemy.game=0;p.start=enemy.start=false;p.mode=enemy.mode=true;
        OlympiadGame g=new OlympiadGame();g._playerOne=p;g._playerTwo=enemy;g._startTime=0;
        Olympiad.INSTANCE.games.put(0,g);return g;
    }
    public static void main(String[]args)
    {
        for(int cid:new int[]{95,91,90})
        {
            Player p=new Player(cid),enemy=new Player(1000+cid);p.cid=cid;
            BotClassPreset preset=new BotClassPreset(cid);p.skill=new Skill(BotSpawner.getOlympiadSummonSkillId(preset));
            OlympiadGame g=match(p,enemy);
            Summon old=new Summon();old.owner=p;old.hp=1;p.summon=old;
            for(int round=0;round<100;round++)
            {
                check(OlympiadCombatGate.claimPreStart(p),"existing opener claim independent/new actual game");
                OlympiadSummonRules.prepare(p,preset);
                Summon fresh=p.summon;
                check(fresh!=null&&fresh!=old&&fresh.owner==p&&fresh.hp==fresh.max,"fresh full HP, no carry over");
                check(p.spawns==round+1,"exactly one summon each match");
                fresh.hp=37;
                for(int i=0;i<10;i++)OlympiadSummonRules.prepare(p,preset);
                check(p.summon==fresh&&fresh.hp==37,"repeated prestart neither replaces nor heals");
                p.start=enemy.start=true;g._startTime=1;
                fresh.dead=true;p.summon=null;
                for(int i=0;i<30;i++)
                {
                    p.skill.applyEffects(p,p);OlympiadSummonRules.prepare(p,preset);
                    check(p.summon==null&&p.spawns==round+1,"death/dismissal never re-summons in combat");
                }
                // Flags return to countdown, same mapped game: NOT a new match.
                p.start=enemy.start=false;g._startTime=0;
                OlympiadSummonRules.prepare(p,preset);check(p.summon==null,"same game cannot reset spent summon");
                old=fresh;p.summon=old;g=match(p,enemy); // same stadium ID, NEW game object
            }
            g.aborted=true;OlympiadSummonRules.prepare(p,preset);check(p.spawns==100,"aborted game denied");
            g=match(p,enemy);p.skill.fail=true;OlympiadSummonRules.prepare(p,preset);
            p.skill.fail=false;OlympiadSummonRules.prepare(p,preset);check(p.summon==null&&p.spawns==100,"failed creation does not retry");
            g=match(p,enemy);p.skill.throwsNow=true;
            OlympiadSummonRules.prepare(p,preset);check(p.summon==null,"creation exception isolated from other pre-start buffs/openers");
            p.skill.throwsNow=false;p.skill.applyEffects(p,p);check(p.summon==null,"exception revokes permit");
            g=match(p,enemy);p.skill=null;OlympiadSummonRules.prepare(p,preset);check(p.summon==null,"missing skill fails closed");
            p.skill=new Skill(BotSpawner.getOlympiadSummonSkillId(preset));OlympiadSummonRules.prepare(p,preset);check(p.summon==null,"missing skill not retried in spent match");
            g=match(p,enemy);old=new Summon();old.owner=p;old.detach=false;p.summon=old;
            OlympiadSummonRules.prepare(p,preset);check(p.summon==old&&p.spawns==100,"detach failure never duplicates");
            g=match(p,enemy);old.owner=enemy;OlympiadSummonRules.prepare(p,preset);check(p.summon==old,"foreign summon never removed");p.summon=null;
            g=match(p,enemy);p.online=false;OlympiadSummonRules.prepare(p,preset);check(p.summon==null,"offline denied");p.online=true;
            p.dead=true;OlympiadSummonRules.prepare(p,preset);check(p.summon==null,"dead owner denied");p.dead=false;
            p.teleport=true;OlympiadSummonRules.prepare(p,preset);check(p.summon==null,"teleport denied");p.teleport=false;
            p.cid=97;OlympiadSummonRules.prepare(p,preset);check(p.summon==null,"mismatched preset denied");p.cid=cid;
            Olympiad.INSTANCE.games.clear();OlympiadSummonRules.prepare(p,preset);check(p.summon==null,"no real game denied");
            p.start=true;g=match(p,enemy);p.start=true;OlympiadSummonRules.prepare(p,preset);check(p.summon==null,"late hook denied");
            p.client=new Object();check(OlympiadSummonRules.allowSummon(p,p.skill),"human unaffected");p.client=null;
            p.managed=false;check(OlympiadSummonRules.allowSummon(p,p.skill),"unmanaged actor unaffected");p.managed=true;
            p.mode=false;p.game=-1;check(OlympiadSummonRules.allowSummon(p,p.skill),"training/world unaffected");
            p.game=0;check(!OlympiadSummonRules.allowSummon(p,p.skill),"assigned bot blocked even before mode flag");
        }
        System.out.println("PASS: "+checks+" production summon lifecycle checks with narrow native doubles; 300 matches, reused stadium, death/repeat/failure/scope");
    }
}
