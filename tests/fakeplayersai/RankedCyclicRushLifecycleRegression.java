package custom.fakeplayersai;

import java.util.*;

/** Runner injects UNCHANGED ranked production bodies at the marker below.
 * Deterministic runtime doubles model the verified core CHARGE/task lifecycle;
 * not a live-server test. Separate runner step compiles all production against actual JARs. */
public final class RankedCyclicRushLifecycleRegression
{
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static final int CLASS_DOOMBRINGER=131, BERSERKER_RUSH_IMPACT_ID=793, BERSERKER_RUSH_MIN_DISTANCE=200;
    private static final int[][] OLYMPIAD_STADIUM_CENTERS={{0,0,0}};
    private static final double OLYMPIAD_ARENA_MAX_DISTANCE=1200;
    private static Player opponent;
    private static Player getOlympiadOpponent(Player bot) { return opponent; }
    private static int arms;
    private static void armBerserkerCombat(Player bot, BotClassPreset preset, BerserkerState state) {
        check(!bot.isCastingNow()&&!bot.isCastingSimultaneouslyNow(),"normal rotation not restored during a core cast");
        arms++; state.enginesStopped=false;
    }
    // INSERT_UNCHANGED_PRODUCTION_BODIES

    private enum Intention { IDLE, ATTACK, CAST, MOVE_TO }
    private enum EffectType { STUN, DAMAGE }
    private enum EffectFlag { STUNNED; int getMask(){return 1;} }
    private static class Location {
        final int x,y,z; Location(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
        int getX(){return x;} int getY(){return y;} int getZ(){return z;}
    }
    private static class WorldObject {
        int x,y,z; int getX(){return x;} int getY(){return y;} int getZ(){return z;}
    }
    private static class Creature extends WorldObject {
        final EffectList effects=new EffectList();
        boolean isPlayer(){return this instanceof Player;}
        EffectList getEffectList(){return effects;}
    }
    private static class Effect {
        EffectType getEffectType(){return EffectType.STUN;}
        int getEffectFlags(){return 1;}
    }
    private static class BuffInfo {
        Player effector; Creature effected; Skill skill; boolean inUse=true,removed;
        final long appliedAt; final List<Effect> effects=new ArrayList<>(List.of(new Effect()));
        BuffInfo(Player bot,Creature enemy,Skill skill,long now){effector=bot;effected=enemy;this.skill=skill;appliedAt=now;}
        Player getEffector(){return effector;} Creature getEffected(){return effected;}
        boolean isInUse(){return inUse;} boolean isRemoved(){return removed;}
        Skill getSkill(){return skill;} List<Effect> getEffects(){return effects;}
    }
    private static class EffectList {
        BuffInfo active; boolean stun=true;
        BuffInfo getBuffInfoBySkillId(int id){return active!=null&&active.skill.getId()==id?active:null;}
        boolean isAffected(EffectFlag flag){return stun&&active!=null;}
        BuffInfo getBuffInfoByAbnormalType(Object type){return active;}
    }
    private static class Skill {
        int getId(){return 793;} int getCastRange(){return 600;}
        int getMpInitialConsume(){return 83;} int getMpConsume(){return 0;}
        int getItemConsumeCount(){return 0;} int getItemConsumeId(){return 0;} int getChargeConsumeCount(){return 0;}
        Object getAbnormalType(){return EffectType.STUN;} boolean isBlockedInOlympiad(){return false;}
        boolean checkCondition(Player bot,Creature enemy,boolean message){return bot.conditions&&bot.calculateDistance2D(enemy)>=200;}
    }
    private static class Template { double getCollisionRadius(){return 8;} }
    private static class Inventory { long getInventoryItemCount(int id,int enchant){return 0;} }
    private static class Player extends Creature {
        final Skill rush=new Skill(); final Template template=new Template(); final Inventory inventory=new Inventory();
        final AI ai=new AI(this); Creature target; boolean cast,simul,moving,controlled,disabled,disarmed,conditions=true;
        boolean autoPlay=true,autoUse=true; double mp=10000; long now;
        int attacksAborted,castsAborted,queuedCleared,casts,damageHits,moves,stops;
        long castStarted; int taskPhase; boolean taskPending; Intention postCast=Intention.IDLE;
        long delayedAttackAt=-1;
        int getOlympiadGameId(){return 0;} int getInstanceId(){return 0;}
        AI getAI(){return ai;} WorldObject getTarget(){return target;}
        void setTarget(Creature value){target=value;}
        double calculateDistance2D(WorldObject other){return Math.hypot((double)x-other.x,(double)y-other.y);}
        boolean isCastingNow(){return cast;} boolean isCastingSimultaneouslyNow(){return simul;}
        boolean isMovementDisabled(){return controlled;} boolean isOutOfControl(){return controlled;}
        boolean isAllSkillsDisabled(){return controlled;} boolean isDisarmed(){return disarmed;}
        boolean isMoving(){return moving;} void abortAttack(){attacksAborted++;}
        void setQueuedSkill(Skill s,boolean ctrl,boolean shift){queuedCleared++;}
        void stopMove(Location loc){moving=false;stops++;}
        Skill getKnownSkill(int id){return id==793?rush:null;} boolean isSkillDisabled(Skill s){return disabled;}
        double getCurrentMp(){return mp;} int getCharges(){return 0;}
        Template getTemplate(){return template;} Inventory getInventory(){return inventory;}
        boolean useMagic(Skill s,boolean ctrl,boolean shift){
            check(!cast&&!simul,"ordinary cast request never overwrites either casting mode");
            check(target!=null&&s.checkCondition(this,target,false)&&calculateDistance2D(target)<=s.getCastRange(),"ordinary min/max distance respected");
            check(!disabled&&mp>=83&&conditions&&!controlled&&!disarmed,"ordinary resources/conditions respected");
            // Creature.beginCast flyType branch is synchronous: XYZ equals the target,
            // NOT an invented 40/45 melee gap. MagicUseTask finishes asynchronously.
            mp-=83;casts++;cast=true;taskPending=true;taskPhase=0;castStarted=now;
            ai.intention=Intention.CAST;moving=false;x=target.x;y=target.y;z=target.z;
            return true;
        }
        void advanceTask(int phase){
            check(taskPending&&phase==taskPhase+1,"ordered launched -> hit -> finalizer core task phases");
            taskPhase=phase;
            if(phase==1){check(cast,"cast remains busy through launched stage");}
            if(phase==2){
                check(cast,"effect application occurs while cast task still busy");damageHits++;
                if(target.effects.active==null)target.effects.active=new BuffInfo(this,target,rush,now);
                // Parent's no-refresh patch leaves the SAME original applied stun.
            }
            if(phase==3){
                cast=false;simul=false;taskPending=false;ai.intention=postCast;
                // Finalizer may restore MOVE_TO or CAST via PlayerAI, and can schedule
                // nextActionAttack 333 ms later if no MOVE_TO was queued.
                if(postCast!=Intention.MOVE_TO)delayedAttackAt=now+333;
            }
        }
        void advanceDelayedAttack(){
            if(delayedAttackAt>=0&&now>=delayedAttackAt){
                if(!cast&&!simul)ai.intention=Intention.ATTACK;
                delayedAttackAt=-1;
            }
        }
    }
    private static class AI {
        final Player bot; Intention intention=Intention.IDLE; Location destination;
        int cancels,idles;
        AI(Player bot){this.bot=bot;}
        Intention getIntention(){return intention;}
        void notifyActionCancel(){
            // Real CreatureAI.notifyActionCancel invokes abortCast. A busy call is a regression.
            check(!bot.cast&&!bot.simul,"cancel must never interrupt normal/simultaneous task");
            bot.castsAborted++;cancels++;
        }
        void setIntentionIdle(){check(!bot.cast&&!bot.simul,"idle cannot interrupt core task");idles++;intention=Intention.IDLE;bot.moving=false;}
        void setIntentionMoveTo(Location point){
            check(!bot.cast&&!bot.simul,"move cannot overwrite core task");
            intention=Intention.MOVE_TO;destination=point;bot.moving=true;bot.moves++;
        }
    }
    private static class BotClassPreset { int getClassId(){return 131;} }
    private static class OlympiadCombatGate { static boolean open=true; static boolean ready(Player bot){return open;} }
    private static class GeoEngine {
        static final GeoEngine INSTANCE=new GeoEngine(); static boolean visible=true,walkable=true;
        static GeoEngine getInstance(){return INSTANCE;}
        boolean canSeeTarget(Player bot,WorldObject enemy){return visible;}
        boolean canMoveToTarget(int x,int y,int z,int ex,int ey,int ez,int instance){return walkable;}
    }
    private static class AutoPlayTaskManager {
        static final AutoPlayTaskManager INSTANCE=new AutoPlayTaskManager();
        static AutoPlayTaskManager getInstance(){return INSTANCE;} void stopAutoPlay(Player bot){bot.autoPlay=false;}
    }
    private static class AutoUseTaskManager {
        static final AutoUseTaskManager INSTANCE=new AutoUseTaskManager();
        static AutoUseTaskManager getInstance(){return INSTANCE;} void stopAutoUseTask(Player bot){bot.autoUse=false;}
    }
    private static Player bot; private static BerserkerState state; private static final BotClassPreset PRESET=new BotClassPreset();
    private static boolean tick(){
        boolean result=driveOwnRushStunLoop(bot,PRESET,state,opponent,bot.rush,bot.now);
        check(state.rushStun.original==null||(!bot.autoPlay&&!bot.autoUse),"both automatic engines stopped while exact lease is active");
        return result;
    }
    private static void busyTick(boolean simultaneous){
        boolean normalBefore=bot.cast,simBefore=bot.simul;bot.cast=!simultaneous;bot.simul=simultaneous;
        int oldMoves=bot.moves,oldCasts=bot.casts,oldCancel=bot.ai.cancels,oldQueue=bot.queuedCleared,oldIdle=bot.ai.idles,oldAbort=bot.attacksAborted;
        check(tick(),"busy active/release loop consumes tick");
        check(bot.moves==oldMoves&&bot.casts==oldCasts&&bot.ai.cancels==oldCancel&&bot.queuedCleared==oldQueue&&bot.ai.idles==oldIdle&&bot.attacksAborted==oldAbort,"no action mutation while "+(simultaneous?"simultaneous":"normal")+" casting");
        bot.cast=normalBefore;bot.simul=simBefore;
    }
    private static void seed(){
        opponent=new Player();bot=new Player();bot.target=opponent;bot.x=300;bot.now=100;state=new BerserkerState(300,0,0,0);arms=0;
        OlympiadCombatGate.open=true;GeoEngine.walkable=true;GeoEngine.visible=true;
        check(castSkill(bot,793),"normal driver accepted original Rush through actual copied castSkill");
        state.rushStun.requested(opponent,null,bot.now);state.movement.requested(bot.now,300,0);
        check(bot.calculateDistance2D(opponent)==0,"actual CHARGE lands at exact opponent XYZ immediately");
    }
    private static void moveToEndpoint(){
        Location point=bot.ai.destination;check(point!=null&&bot.moving,"loop requested ordinary movement");
        check(Math.hypot((double)point.x-bot.x,(double)point.y-bot.y)<=241,"bounded ordinary step, no arena-wide teleport");
        bot.x=point.x;bot.y=point.y;bot.z=point.z;bot.moving=false;bot.now+=250;
        check(bot.calculateDistance2D(opponent)>=200,"ordinary move reaches legal repeated793 distance");
    }
    private static void finishCharge(Intention leftover){
        bot.now+=100;bot.advanceTask(1);
        if(state.rushStun.original!=null||state.stunLoopReleasing)busyTick(false);
        else check(!tick(),"pre-hit cast has no stun lease yet; outer normal driver owns pending task");
        bot.now+=400;bot.advanceTask(2);
        check(tick(),"applied original STUN captured/retained during hit stage");busyTick(true);
        bot.now+=300;bot.postCast=leftover;bot.advanceTask(3);
    }
    private static void cyclicLandings(){
        for(Intention leftover:Intention.values()){
            seed();BuffInfo original=null;
            for(int charge=0;charge<5;charge++){
                finishCharge(leftover);
                if(original==null)original=opponent.effects.active;
                check(opponent.effects.active==original&&state.rushStun.original==original,"same original stun survives every task phase/charge "+charge);
                check(tick(),"post-cast exact-zero landing consumes next retreat tick "+charge);
                check(bot.moving&&state.stunLoopMoving,"new retreat armed after EACH zero-distance landing "+charge);
                check(Math.hypot(bot.ai.destination.x,bot.ai.destination.y)>=200,"non-null legal retreat from coincident core landing");
                // Exercise delayed finalizer nextActionAttack and stale CAST-follow after
                // a partial ordinary approach. Neither may turn repeated retreat into one-shot.
                bot.now+=400;bot.advanceDelayedAttack();
                if(charge%2==0)bot.ai.intention=Intention.CAST;
                check(tick(),"late ATTACK/CAST intention cannot steal original stun lease");
                moveToEndpoint();int prior=bot.casts;
                check(tick(),"legal-distance repeat consumes tick");
                check(bot.casts==prior+1&&bot.cast&&bot.calculateDistance2D(opponent)==0,"new real Rush after EACH retreat, charge "+charge);
                check(!state.stunLoopMoving,"old retreat ownership cleared before synchronous CHARGE landing");
                check(opponent.effects.active==original&&original.appliedAt==600,"repeat damage cannot replace/refresh original stun");
            }
            check(bot.casts==6&&bot.damageHits==5,"five consecutive retreat->repeat cycles, not only one");
            check(bot.mp==10000-6*83,"all six ordinary cast requests pay MP; no fake refill/reuse");
            check(arms==0,"ordinary phase/autoplay never rearmed in active repeated-stun lease");
            // End during a running task: finish it rather than aborting it.
            original.removed=true;busyTick(false);busyTick(true);
            check(state.stunLoopReleasing,"end/removal immediately ends lease but lets pending task finish");
            finishCharge(leftover);
            check(!tick()&&arms==1&&state.rushDone&&!state.stunLoopReleasing,"effect ended -> clear/release once and ordinary phase resumes");
            check(state.rushStun.original==null&&!state.stunLoopMoving,"no retained original/approach after release");
            check(!tick()&&arms==1,"removed old effect cannot restart loop or repeatedly rearm engines");
            // A NEW ordinary own793 stun starts an independent cyclic lease.
            opponent.effects.active=null;bot.x=300;bot.y=0;bot.ai.intention=Intention.IDLE;bot.now+=100;
            check(castSkill(bot,793),"next normal own793 can start after previous loop release");
            state.rushStun.requested(opponent,null,bot.now);finishCharge(Intention.IDLE);
            check(state.rushStun.original!=null&&state.rushStun.original!=original,"new successful original own793 reacquired, not stale previous stun");
            check(tick()&&bot.moving,"new lease starts retreat even after previous five-cycle lease ended");
        }
    }
    private static void blockedAndReplacement(){
        seed();finishCharge(Intention.MOVE_TO);BuffInfo original=opponent.effects.active;
        GeoEngine.walkable=false;int oldMove=bot.moves;check(tick()&&bot.moves==oldMove&&bot.casts==1,"blocked geodata cannot invent retreat/cast");
        GeoEngine.walkable=true;check(tick()&&bot.moving,"legal ordinary retreat resumes after geodata obstruction clears");
        bot.now+=1001;check(tick()&&!state.stunLoopMoving,"stalled ordinary retreat stopped at its progress budget");
        check(tick()&&bot.moving,"stalled step does not permanently consume the cyclic retreat lease");
        moveToEndpoint();bot.mp=82;int oldCast=bot.casts;check(tick()&&bot.casts==oldCast,"unpayable real793 not cast");
        bot.mp=500;bot.disabled=true;check(tick()&&bot.casts==oldCast,"real skill disabled/reuse not bypassed");
        bot.disabled=false;OlympiadCombatGate.open=false;check(tick()&&bot.casts==oldCast,"closed official gate prevents repeat");
        OlympiadCombatGate.open=true;GeoEngine.visible=false;check(tick()&&bot.casts==oldCast,"LoS prevents repeat");
        GeoEngine.visible=true;bot.conditions=false;check(tick()&&bot.casts==oldCast,"real skill condition prevents repeat");
        bot.conditions=true;check(tick()&&bot.casts==oldCast+1,"ready original-stun charge resumes without synthetic reuse edits");
        finishCharge(Intention.CAST);
        opponent.effects.active=new BuffInfo(bot,opponent,bot.rush,bot.now);
        check(!tick()&&state.rushStun.original==null&&arms==1,"replacement same793 identity ends original lease");
        check(!tick()&&arms==1,"same-skill replacement alone cannot silently restart lease");
        check(original!=opponent.effects.active,"replacement fixture actually changed BuffInfo identity");
    }
    public static void main(String[]args){cyclicLandings();blockedAndReplacement();System.out.println("PASS: "+checks+" ranked source-exact cyclic Rush lifecycle checks (deterministic core-compatible doubles; no live server)");}
}
