package custom.fakeplayersai;

import java.util.List;
import static custom.fakeplayersai.TacticsPolicy.*;

/** Actual production tank close/catch/defense decisions; no server actors or fake effects. */
public final class TankCloseControlRegression
{
    private static int checks;
    private static void check(boolean ok,String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static Option option(int id,Kind kind,double score) { return new Option(id,kind,score); }
    public static void main(String[] args)
    {
        List<Option> all=List.of(option(65,Kind.DAMAGE,100),option(368,Kind.DEFENSE,234),option(110,Kind.DEFENSE,230),
            option(403,Kind.SLOW,92),option(92,Kind.CONTROL,54),option(353,Kind.DAMAGE,62));
        for(int cls:new int[]{90,91,106})
        {
            check(tankClass(cls),"all three configured tanks included");
            for(double collision:new double[]{0,7.5,16,30})
                for(double physical:new double[]{40,80,150,600})
                    check(tankApproachRange(physical,collision)==(int)(40+collision),"stable real cast40 envelope cannot become drain600 when close skills onCD/MPempty");
        }
        for(int cls:new int[]{88,89,92,99,102,103,108,109,110,113,114,115,131}) check(!tankClass(cls),"no unrelated class movement policy change");
        check(tankApproachRange(0,0)==20,"bounded fallback even when unarmed");
        for(boolean closing:new boolean[]{false,true}) for(boolean self:new boolean[]{false,true})
            for(Kind kind:Kind.values()) for(boolean control:new boolean[]{false,true})
                check(tankClosingSkillAllowed(closing,self,kind,control)==(!closing||(self?(kind==Kind.HEAL||kind==Kind.DEFENSE):control)),"closing allows catch and emergency support but no ranged damage/debuff stall");
        check(!tankClosingSkillAllowed(true,false,Kind.DAMAGE,false),"drain prohibited while closing");
        check(!tankClosingSkillAllowed(true,false,Kind.CONTROL,false),"horror/mute not mistaken for STUN/ROOT");
        check(tankClosingSkillAllowed(true,false,Kind.SLOW,true),"403/402 Root classified SLOW remains legal catch");
        check(tankClosingSkillAllowed(false,false,Kind.DAMAGE,false),"explicit other offense still available at close");
        for(boolean melee:new boolean[]{false,true}) for(boolean closing:new boolean[]{false,true}) for(boolean immobile:new boolean[]{false,true})
            check(tankSupportAllowed(closing,immobile)==(!closing||!immobile),"immobile support deferred while closing versus BOTH melee and ranged: melee="+melee);
        check(tankSupportAllowed(true,false),"plain760 antiMagicArmor not wrongly treated as self root");
        check(!tankRecoveryAllowed(true,false),"armed tank does not backpedal on ordinary cooldown");
        check(tankRecoveryAllowed(true,true),"real disarm/noWeapon recovery retained");
        check(tankRecoveryAllowed(false,false),"other class recovery unchanged");
        for(boolean ranked:new boolean[]{false,true}) for(int mask=0;mask<512;mask++)
        {
            boolean tank=(mask&1)!=0,armed=(mask&2)!=0,blocked=(mask&4)!=0,sleep=(mask&8)!=0,
                invul=(mask&16)!=0,los=(mask&32)!=0,attackable=(mask&64)!=0,pending=(mask&128)!=0,closing=(mask&256)!=0;
            check(tankPursuit(tank,armed,blocked,sleep,invul,attackable,pending)==(tank&&armed&&!blocked&&!sleep&&!invul&&attackable&&!pending),"ranked="+ranked+" los="+los+" closing="+closing+" near/far blind MOVEMENT yield preserves CC/disarm/sleep/cast guards");
        }
        for(boolean ranked:new boolean[]{false,true})
        {
            check(tankPursuit(true,true,false,false,false,true,false),"ranked="+ranked+" foe around obstacle yields movement-only distant pursuit");
            check(tankPursuit(true,true,false,false,false,true,false),"ranked="+ranked+" near blind target yields movement-only route around pillar, not permission toattack");
            check(!tankPursuit(true,true,true,false,false,true,false),"ranked="+ranked+" blocked caster/CC never yields blind movement");
            check(!allowed(Kind.CONTROL,true,true,false,false,false,true,true),"ranked="+ranked+" blind pursuit never authorizes stun cast");
            check(!allowed(Kind.DAMAGE,true,true,false,false,false,true,true),"ranked="+ranked+" blind pursuit never authorizes damage cast");
        }
        List<Integer> stun=List.of(92),root=List.of(403),melee=List.of(353);
        check(tankOffenseChoice(all,stun,root,melee)==92,"legal close STUN before ROOT and melee/drain");
        check(tankOffenseChoice(all,List.of(),root,melee)==403,"legal ranged ROOT catcher when no in-range stun");
        check(tankOffenseChoice(all,List.of(),List.of(),melee)==353,"melee damage before generic magic atclose");
        check(tankOffenseChoice(List.of(option(65,Kind.DAMAGE,100)),List.of(),List.of(),List.of())==-1,"no invented control/melee under override");
        check(tankActionChoice(all,stun,root,melee,368,true)==92,"catch free foe BEFORE self-root Vengeance");
        check(tankActionChoice(all,stun,root,melee,368,false)==368,"after catch actual active control allows timed Vengeance");
        check(tankActionChoice(all,List.of(),root,melee,368,true)==403,"root catch also wins before ordinary defense");
        check(tankActionChoice(all,List.of(),List.of(),melee,368,true)==368,"ordinary damage does not suppress timed defense");
        check(tankActionChoice(all,stun,root,melee,-1,true)==92,"no defense yet means normal legal control");
        List<Option> emergency=new java.util.ArrayList<>(all); emergency.add(option(49,Kind.HEAL,160));
        check(tankActionChoice(emergency,stun,root,melee,368,true)==49,"genuine emergency heal wins even catch/defense");
        TankDefenseSequence sequence=new TankDefenseSequence(); sequence.reset(10000);
        check(sequence.choose(all,14999,false)==-1,"keep actual combat+5 gate");
        check(sequence.choose(all,15000,false)==368,"Vengeance first exactly combat+5");
        check(sequence.choose(all,15000,true)==-1,"no two strong at once");
        sequence.observed(368);
        check(sequence.choose(all,15001,true)==-1,"active Vengeance never refreshed");
        check(sequence.choose(all,15001,false)==110,"expiration/cancel/steal advances immediately toUD");
        sequence.observed(110);
        check(sequence.choose(all,15002,false)==368,"next legal observed strong cycle still Vengeance beforeUD");
        check(sequence.choose(List.of(option(110,Kind.DEFENSE,230)),15002,false)==110,"only-UD explicit override honored");
        check(sequence.choose(List.of(),15002,false)==-1,"no known legal defense never injected");
        sequence.reset(20000); check(sequence.choose(all,25000,false)==368,"new pair resets Vengeance-first order");
        // Combined real policy path: near30 behind pillar, due strong defenses,
        // not merely a pursuit-helper test. Apply production option gates BEFORE
        // timed/ordinary selection, then check the actual fallback decision.
        for(boolean ranked:new boolean[]{false,true}) for(boolean visible:new boolean[]{false,true})
        {
            TankDefenseSequence due=new TankDefenseSequence(); due.reset(10000);
            boolean needsApproach=!visible || 30>tankApproachRange(80,8);
            List<Option> candidates=List.of(option(368,Kind.DEFENSE,234),option(110,Kind.DEFENSE,230),option(92,Kind.CONTROL,54));
            List<Option> legal=new java.util.ArrayList<>(); List<Integer> legalStuns=new java.util.ArrayList<>();
            for(Option candidate:candidates)
            {
                boolean self=candidate.kind()==Kind.DEFENSE;
                if(!tankClosingSkillAllowed(needsApproach,self,candidate.kind(),!self)) continue;
                if(self&&!tankSupportAllowed(needsApproach,candidate.id()!=368)) continue; // New explicit Vengeance exception, UD still deferred.
                if(!allowed(candidate.kind(),true,true,visible,false,false,true,true)) continue;
                legal.add(candidate); if(!self) legalStuns.add(candidate.id());
            }
            int defense=due.choose(legal,15000,false);
            int chosen=tankActionChoice(legal,legalStuns,List.of(),List.of(),defense,true);
            if(!visible)
            {
                check(legal.size()==1&&legal.get(0).id()==368,"ranked="+ranked+" near30 blind allows explicit Vengeance exception but removes UD and blindstun");
                check(defense==368&&chosen==368,"ranked="+ranked+" explicit Vengeance exception may precede pursuit; actual offense still needs LoS");
                check(tankPursuit(true,true,false,false,false,true,false),"ranked="+ranked+" movement-only pursuit remains available if no legal Vengeance");
            }
            else
            {
                check(defense==368,"ranked="+ranked+" visible near30 due defense remains legal Vengeance-first");
                check(chosen==92,"ranked="+ranked+" visible free nearfoe catcher remains before due defense");
            }
        }
        for(boolean ready:new boolean[]{false,true}) for(boolean inRange:new boolean[]{false,true})
            for(boolean resource:new boolean[]{false,true}) for(boolean visible:new boolean[]{false,true}) for(boolean sleeping:new boolean[]{false,true})
                for(Kind kind:new Kind[]{Kind.CONTROL,Kind.SLOW})
                    check(allowed(kind,ready,inRange,visible,false,sleeping,resource,true)==(ready&&inRange&&resource&&visible&&!sleeping),"control precedence cannot bypass real legality");
        System.out.println("PASS: "+checks+" production tank close/catch/pressure/Vengeance lifecycle assertions");
    }
}
