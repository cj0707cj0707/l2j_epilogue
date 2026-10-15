/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.skill.effects;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.StopMove;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Bluff effect implementation.
 * @author decad
 */
public class Bluff extends AbstractEffect
{
	/** Length of the zero-effect "step" used to unlock the victim's own client, in units. */
	private static final int TURN_STEP_OFFSET = 30;
	/** Client heading units per degree (65536 / 360). */
	private static final double HEADING_PER_DEGREE = 182.044444444;
	
	private final int _chance;
	
	public Bluff(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_chance = params.getInt("chance", 100);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcProbability(_chance, effector, effected, skill);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		// Headquarters NPC should not rotate.
		if ((effected.getId() == 35062) || effected.isRaid() || effected.isRaidMinion())
		{
			return;
		}
		
		// Turn the victim exactly back-to-the-attacker (same heading as the effector).
		final int oldHeading = effected.getHeading();
		final int newHeading = effector.getHeading();
		if (newHeading == oldHeading)
		{
			return;
		}
		effected.setHeading(newHeading);
		
		// Fix 19.09.2026: no more zero-distance teleport. TeleportToLocation applied the turn,
		// but a teleport received for its own pawn makes the client play the full teleport
		// transition (screen fades to black), which is very intrusive in normal play. The
		// teleport packet is gone; the turn is now delivered with movement packets only.
		
		// Surrounding clients: a heading-only ValidateLocation rebuilds every pawn the client
		// does not own (NPCs, bots, other players as seen by the attacker and bystanders).
		effected.broadcastPacket(new ValidateLocation(effected));
		
		// Victim's own client: while standing it is the position authority for its own pawn
		// and ignores heading-only corrections (attempt 2, 16-17.09.2026). So first unlock
		// the own pawn with a zero-effect server-initiated step toward the new heading - the
		// same mechanic fear uses (the client applies MoveToLocation for its own pawn) - and
		// immediately stop the pawn on the original coordinates with the new heading. The
		// pawn only turns on the spot; no teleport packet is involved, hence no fade.
		// Skipped while moving/teleporting/dead to avoid fighting the client's own movement.
		if (!effected.isMoving() && !effected.isTeleporting() && !effected.isAlikeDead() //
			&& !(effected.isPlayer() && effected.asPlayer().isSitting()))
		{
			final double radians = Math.toRadians(newHeading / HEADING_PER_DEGREE);
			final int stepX = effected.getX() + (int) (Math.cos(radians) * TURN_STEP_OFFSET);
			final int stepY = effected.getY() + (int) (Math.sin(radians) * TURN_STEP_OFFSET);
			effected.sendPacket(new TurnStep(effected, stepX, stepY, effected.getZ()));
			effected.sendPacket(new StopMove(effected));
		}
	}
	
	/**
	 * Minimal MoveToLocation packet with an explicit destination: makes the victim's own
	 * client start a tiny server-initiated step (and face the new heading), which the
	 * immediately following StopMove cancels on the original coordinates.
	 */
	private static class TurnStep extends ServerPacket
	{
		private final int _objectId;
		private final int _x;
		private final int _y;
		private final int _z;
		private final int _xDst;
		private final int _yDst;
		private final int _zDst;
		
		public TurnStep(Creature creature, int xDst, int yDst, int zDst)
		{
			_objectId = creature.getObjectId();
			_x = creature.getX();
			_y = creature.getY();
			_z = creature.getZ();
			_xDst = xDst;
			_yDst = yDst;
			_zDst = zDst;
		}
		
		@Override
		public void writeImpl(GameClient client, WriteBuffer buffer)
		{
			ServerPackets.MOVE_TO_LOCATION.writeId(this, buffer);
			buffer.writeInt(_objectId);
			buffer.writeInt(_xDst);
			buffer.writeInt(_yDst);
			buffer.writeInt(_zDst);
			buffer.writeInt(_x);
			buffer.writeInt(_y);
			buffer.writeInt(_z);
		}
	}
}
