package org.l2jmobius.gameserver.mechanics.sevensigns;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.FestivalMonster;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyMessageType;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

public class SevenSignsFestival {
   protected static final Logger LOGGER = Logger.getLogger(SevenSignsFestival.class.getName());
   private static final String GET_CLAN_NAME = "SELECT clan_name FROM clan_data WHERE clan_id = (SELECT clanid FROM characters WHERE char_name = ?)";
   public static final long FESTIVAL_SIGNUP_TIME = FeatureConfig.ALT_FESTIVAL_CYCLE_LENGTH - FeatureConfig.ALT_FESTIVAL_LENGTH - 60000L;
   private static final int FESTIVAL_MAX_OFFSET_X = 230;
   private static final int FESTIVAL_MAX_OFFSET_Y = 230;
   private static final int FESTIVAL_DEFAULT_RESPAWN = 60;
   public static final int FESTIVAL_COUNT = 5;
   public static final int FESTIVAL_LEVEL_MAX_31 = 0;
   public static final int FESTIVAL_LEVEL_MAX_42 = 1;
   public static final int FESTIVAL_LEVEL_MAX_53 = 2;
   public static final int FESTIVAL_LEVEL_MAX_64 = 3;
   public static final int FESTIVAL_LEVEL_MAX_NONE = 4;
   public static final int[] FESTIVAL_LEVEL_SCORES = new int[]{60, 70, 100, 120, 150};
   public static final int FESTIVAL_OFFERING_ID = 5901;
   public static final int FESTIVAL_OFFERING_VALUE = 5;
   public static final int[][] FESTIVAL_DAWN_PLAYER_SPAWNS = new int[][]{
      {-79187, 113186, -4895, 0}, {-75918, 110137, -4895, 0}, {-73835, 111969, -4895, 0}, {-76170, 113804, -4895, 0}, {-78927, 109528, -4895, 0}
   };
   public static final int[][] FESTIVAL_DUSK_PLAYER_SPAWNS = new int[][]{
      {-77200, 88966, -5151, 0}, {-76941, 85307, -5151, 0}, {-74855, 87135, -5151, 0}, {-80208, 88222, -5151, 0}, {-79954, 84697, -5151, 0}
   };
   protected static final int[][] FESTIVAL_DAWN_WITCH_SPAWNS = new int[][]{
      {-79183, 113052, -4891, 0, 31132},
      {-75916, 110270, -4891, 0, 31133},
      {-73979, 111970, -4891, 0, 31134},
      {-76174, 113663, -4891, 0, 31135},
      {-78930, 109664, -4891, 0, 31136}
   };
   protected static final int[][] FESTIVAL_DUSK_WITCH_SPAWNS = new int[][]{
      {-77199, 88830, -5147, 0, 31142},
      {-76942, 85438, -5147, 0, 31143},
      {-74990, 87135, -5147, 0, 31144},
      {-80207, 88222, -5147, 0, 31145},
      {-79952, 84833, -5147, 0, 31146}
   };
   protected static final int[][][] FESTIVAL_DAWN_PRIMARY_SPAWNS = new int[][][]{
      {
            {-78537, 113839, -4895, -1, 18009},
            {-78466, 113852, -4895, -1, 18010},
            {-78509, 113899, -4895, -1, 18010},
            {-78481, 112557, -4895, -1, 18009},
            {-78559, 112504, -4895, -1, 18010},
            {-78489, 112494, -4895, -1, 18010},
            {-79803, 112543, -4895, -1, 18012},
            {-79854, 112492, -4895, -1, 18013},
            {-79886, 112557, -4895, -1, 18014},
            {-79821, 113811, -4895, -1, 18015},
            {-79857, 113896, -4895, -1, 18017},
            {-79878, 113816, -4895, -1, 18018},
            {-79190, 113660, -4895, -1, 18011},
            {-78710, 113188, -4895, -1, 18011},
            {-79190, 112730, -4895, -1, 18016},
            {-79656, 113188, -4895, -1, 18016}
      },
      {
            {-76558, 110784, -4895, -1, 18019},
            {-76607, 110815, -4895, -1, 18020},
            {-76559, 110820, -4895, -1, 18020},
            {-75277, 110792, -4895, -1, 18019},
            {-75225, 110801, -4895, -1, 18020},
            {-75262, 110832, -4895, -1, 18020},
            {-75249, 109441, -4895, -1, 18022},
            {-75278, 109495, -4895, -1, 18023},
            {-75223, 109489, -4895, -1, 18024},
            {-76556, 109490, -4895, -1, 18025},
            {-76607, 109469, -4895, -1, 18027},
            {-76561, 109450, -4895, -1, 18028},
            {-76399, 110144, -4895, -1, 18021},
            {-75912, 110606, -4895, -1, 18021},
            {-75444, 110144, -4895, -1, 18026},
            {-75930, 109665, -4895, -1, 18026}
      },
      {
            {-73184, 111319, -4895, -1, 18029},
            {-73135, 111294, -4895, -1, 18030},
            {-73185, 111281, -4895, -1, 18030},
            {-74477, 111321, -4895, -1, 18029},
            {-74523, 111293, -4895, -1, 18030},
            {-74481, 111280, -4895, -1, 18030},
            {-74489, 112604, -4895, -1, 18032},
            {-74491, 112660, -4895, -1, 18033},
            {-74527, 112629, -4895, -1, 18034},
            {-73197, 112621, -4895, -1, 18035},
            {-73142, 112631, -4895, -1, 18037},
            {-73182, 112656, -4895, -1, 18038},
            {-73834, 112430, -4895, -1, 18031},
            {-74299, 111959, -4895, -1, 18031},
            {-73841, 111491, -4895, -1, 18036},
            {-73363, 111959, -4895, -1, 18036}
      },
      {
            {-75543, 114461, -4895, -1, 18039},
            {-75514, 114493, -4895, -1, 18040},
            {-75488, 114456, -4895, -1, 18040},
            {-75521, 113158, -4895, -1, 18039},
            {-75504, 113110, -4895, -1, 18040},
            {-75489, 113142, -4895, -1, 18040},
            {-76809, 113143, -4895, -1, 18042},
            {-76860, 113138, -4895, -1, 18043},
            {-76831, 113112, -4895, -1, 18044},
            {-76831, 114441, -4895, -1, 18045},
            {-76840, 114490, -4895, -1, 18047},
            {-76864, 114455, -4895, -1, 18048},
            {-75703, 113797, -4895, -1, 18041},
            {-76180, 114263, -4895, -1, 18041},
            {-76639, 113797, -4895, -1, 18046},
            {-76180, 113337, -4895, -1, 18046}
      },
      {
            {-79576, 108881, -4895, -1, 18049},
            {-79592, 108835, -4895, -1, 18050},
            {-79614, 108871, -4895, -1, 18050},
            {-79586, 110171, -4895, -1, 18049},
            {-79589, 110216, -4895, -1, 18050},
            {-79620, 110177, -4895, -1, 18050},
            {-78825, 110182, -4895, -1, 18052},
            {-78238, 110182, -4895, -1, 18053},
            {-78266, 110218, -4895, -1, 18054},
            {-78275, 108883, -4895, -1, 18055},
            {-78267, 108839, -4895, -1, 18057},
            {-78241, 108871, -4895, -1, 18058},
            {-79394, 109538, -4895, -1, 18051},
            {-78929, 109992, -4895, -1, 18051},
            {-78454, 109538, -4895, -1, 18056},
            {-78929, 109053, -4895, -1, 18056}
      }
   };
   protected static final int[][][] FESTIVAL_DUSK_PRIMARY_SPAWNS = new int[][][]{
      {
            {-76542, 89653, -5151, -1, 18009},
            {-76509, 89637, -5151, -1, 18010},
            {-76548, 89614, -5151, -1, 18010},
            {-76539, 88326, -5151, -1, 18009},
            {-76512, 88289, -5151, -1, 18010},
            {-76546, 88287, -5151, -1, 18010},
            {-77879, 88308, -5151, -1, 18012},
            {-77886, 88310, -5151, -1, 18013},
            {-77879, 88278, -5151, -1, 18014},
            {-77857, 89605, -5151, -1, 18015},
            {-77858, 89658, -5151, -1, 18017},
            {-77891, 89633, -5151, -1, 18018},
            {-76728, 88962, -5151, -1, 18011},
            {-77194, 88494, -5151, -1, 18011},
            {-77660, 88896, -5151, -1, 18016},
            {-77195, 89438, -5151, -1, 18016}
      },
      {
            {-77585, 84650, -5151, -1, 18019},
            {-77628, 84643, -5151, -1, 18020},
            {-77607, 84613, -5151, -1, 18020},
            {-76603, 85946, -5151, -1, 18019},
            {-77606, 85994, -5151, -1, 18020},
            {-77638, 85959, -5151, -1, 18020},
            {-76301, 85960, -5151, -1, 18022},
            {-76257, 85972, -5151, -1, 18023},
            {-76286, 85992, -5151, -1, 18024},
            {-76281, 84667, -5151, -1, 18025},
            {-76291, 84611, -5151, -1, 18027},
            {-76257, 84616, -5151, -1, 18028},
            {-77419, 85307, -5151, -1, 18021},
            {-76952, 85768, -5151, -1, 18021},
            {-76477, 85312, -5151, -1, 18026},
            {-76942, 84832, -5151, -1, 18026}
      },
      {
            {-74211, 86494, -5151, -1, 18029},
            {-74200, 86449, -5151, -1, 18030},
            {-74167, 86464, -5151, -1, 18030},
            {-75495, 86482, -5151, -1, 18029},
            {-75540, 86473, -5151, -1, 18030},
            {-75509, 86445, -5151, -1, 18030},
            {-75509, 87775, -5151, -1, 18032},
            {-75518, 87826, -5151, -1, 18033},
            {-75542, 87780, -5151, -1, 18034},
            {-74214, 87789, -5151, -1, 18035},
            {-74169, 87801, -5151, -1, 18037},
            {-74198, 87827, -5151, -1, 18038},
            {-75324, 87135, -5151, -1, 18031},
            {-74852, 87606, -5151, -1, 18031},
            {-74388, 87146, -5151, -1, 18036},
            {-74856, 86663, -5151, -1, 18036}
      },
      {
            {-79560, 89007, -5151, -1, 18039},
            {-79521, 89016, -5151, -1, 18040},
            {-79544, 89047, -5151, -1, 18040},
            {-79552, 87717, -5151, -1, 18039},
            {-79552, 87673, -5151, -1, 18040},
            {-79510, 87702, -5151, -1, 18040},
            {-80866, 87719, -5151, -1, 18042},
            {-80897, 87689, -5151, -1, 18043},
            {-80850, 87685, -5151, -1, 18044},
            {-80848, 89013, -5151, -1, 18045},
            {-80887, 89051, -5151, -1, 18047},
            {-80891, 89004, -5151, -1, 18048},
            {-80205, 87895, -5151, -1, 18041},
            {-80674, 88350, -5151, -1, 18041},
            {-80209, 88833, -5151, -1, 18046},
            {-79743, 88364, -5151, -1, 18046}
      },
      {
            {-80624, 84060, -5151, -1, 18049},
            {-80621, 84007, -5151, -1, 18050},
            {-80590, 84039, -5151, -1, 18050},
            {-80605, 85349, -5151, -1, 18049},
            {-80639, 85363, -5151, -1, 18050},
            {-80611, 85385, -5151, -1, 18050},
            {-79311, 85353, -5151, -1, 18052},
            {-79277, 85384, -5151, -1, 18053},
            {-79273, 85539, -5151, -1, 18054},
            {-79297, 84054, -5151, -1, 18055},
            {-79285, 84006, -5151, -1, 18057},
            {-79260, 84040, -5151, -1, 18058},
            {-79945, 85171, -5151, -1, 18051},
            {-79489, 84707, -5151, -1, 18051},
            {-79952, 84222, -5151, -1, 18056},
            {-80423, 84703, -5151, -1, 18056}
      }
   };
   protected static final int[][][] FESTIVAL_DAWN_SECONDARY_SPAWNS = new int[][][]{
      {
            {-78757, 112834, -4895, -1, 18016},
            {-78581, 112834, -4895, -1, 18016},
            {-78822, 112526, -4895, -1, 18011},
            {-78822, 113702, -4895, -1, 18011},
            {-78822, 113874, -4895, -1, 18011},
            {-79524, 113546, -4895, -1, 18011},
            {-79693, 113546, -4895, -1, 18011},
            {-79858, 113546, -4895, -1, 18011},
            {-79545, 112757, -4895, -1, 18016},
            {-79545, 112586, -4895, -1, 18016}
      },
      {
            {-75565, 110580, -4895, -1, 18026},
            {-75565, 110740, -4895, -1, 18026},
            {-75577, 109776, -4895, -1, 18021},
            {-75413, 109776, -4895, -1, 18021},
            {-75237, 109776, -4895, -1, 18021},
            {-76274, 109468, -4895, -1, 18021},
            {-76274, 109635, -4895, -1, 18021},
            {-76274, 109795, -4895, -1, 18021},
            {-76351, 110500, -4895, -1, 18056},
            {-76528, 110500, -4895, -1, 18056}
      },
      {
            {-74191, 111527, -4895, -1, 18036},
            {-74191, 111362, -4895, -1, 18036},
            {-73495, 111611, -4895, -1, 18031},
            {-73327, 111611, -4895, -1, 18031},
            {-73154, 111611, -4895, -1, 18031},
            {-73473, 112301, -4895, -1, 18031},
            {-73473, 112475, -4895, -1, 18031},
            {-73473, 112649, -4895, -1, 18031},
            {-74270, 112326, -4895, -1, 18036},
            {-74443, 112326, -4895, -1, 18036}
      },
      {
            {-75738, 113439, -4895, -1, 18046},
            {-75571, 113439, -4895, -1, 18046},
            {-75824, 114141, -4895, -1, 18041},
            {-75824, 114309, -4895, -1, 18041},
            {-75824, 114477, -4895, -1, 18041},
            {-76513, 114158, -4895, -1, 18041},
            {-76683, 114158, -4895, -1, 18041},
            {-76857, 114158, -4895, -1, 18041},
            {-76535, 113357, -4895, -1, 18056},
            {-76535, 113190, -4895, -1, 18056}
      },
      {
            {-79350, 109894, -4895, -1, 18056},
            {-79534, 109894, -4895, -1, 18056},
            {-79285, 109187, -4895, -1, 18051},
            {-79285, 109019, -4895, -1, 18051},
            {-79285, 108860, -4895, -1, 18051},
            {-78587, 109172, -4895, -1, 18051},
            {-78415, 109172, -4895, -1, 18051},
            {-78249, 109172, -4895, -1, 18051},
            {-78575, 109961, -4895, -1, 18056},
            {-78575, 110130, -4895, -1, 18056}
      }
   };
   protected static final int[][][] FESTIVAL_DUSK_SECONDARY_SPAWNS = new int[][][]{
      {
            {-76844, 89304, -5151, -1, 18011},
            {-76844, 89479, -5151, -1, 18011},
            {-76844, 89649, -5151, -1, 18011},
            {-77544, 89326, -5151, -1, 18011},
            {-77716, 89326, -5151, -1, 18011},
            {-77881, 89326, -5151, -1, 18011},
            {-77561, 88530, -5151, -1, 18016},
            {-77561, 88364, -5151, -1, 18016},
            {-76762, 88615, -5151, -1, 18016},
            {-76594, 88615, -5151, -1, 18016}
      },
      {
            {-77307, 84969, -5151, -1, 18021},
            {-77307, 84795, -5151, -1, 18021},
            {-77307, 84623, -5151, -1, 18021},
            {-76614, 84944, -5151, -1, 18021},
            {-76433, 84944, -5151, -1, 18021},
            {-76251, 84944, -5151, -1, 18021},
            {-76594, 85745, -5151, -1, 18026},
            {-76594, 85910, -5151, -1, 18026},
            {-77384, 85660, -5151, -1, 18026},
            {-77555, 85660, -5151, -1, 18026}
      },
      {
            {-74517, 86782, -5151, -1, 18031},
            {-74344, 86782, -5151, -1, 18031},
            {-74185, 86782, -5151, -1, 18031},
            {-74496, 87464, -5151, -1, 18031},
            {-74496, 87636, -5151, -1, 18031},
            {-74496, 87815, -5151, -1, 18031},
            {-75298, 87497, -5151, -1, 18036},
            {-75460, 87497, -5151, -1, 18036},
            {-75219, 86712, -5151, -1, 18036},
            {-75219, 86531, -5151, -1, 18036}
      },
      {
            {-79851, 88703, -5151, -1, 18041},
            {-79851, 88868, -5151, -1, 18041},
            {-79851, 89040, -5151, -1, 18041},
            {-80548, 88722, -5151, -1, 18041},
            {-80711, 88722, -5151, -1, 18041},
            {-80883, 88722, -5151, -1, 18041},
            {-80565, 87916, -5151, -1, 18046},
            {-80565, 87752, -5151, -1, 18046},
            {-79779, 87996, -5151, -1, 18046},
            {-79613, 87996, -5151, -1, 18046}
      },
      {
            {-79271, 84330, -5151, -1, 18051},
            {-79448, 84330, -5151, -1, 18051},
            {-79601, 84330, -5151, -1, 18051},
            {-80311, 84367, -5151, -1, 18051},
            {-80311, 84196, -5151, -1, 18051},
            {-80311, 84015, -5151, -1, 18051},
            {-80556, 85049, -5151, -1, 18056},
            {-80384, 85049, -5151, -1, 18056},
            {-79598, 85127, -5151, -1, 18056},
            {-79598, 85303, -5151, -1, 18056}
      }
   };
   protected static final int[][][] FESTIVAL_DAWN_CHEST_SPAWNS = new int[][][]{
      {
            {-78999, 112957, -4927, -1, 18109},
            {-79153, 112873, -4927, -1, 18109},
            {-79256, 112873, -4927, -1, 18109},
            {-79368, 112957, -4927, -1, 18109},
            {-79481, 113124, -4927, -1, 18109},
            {-79481, 113275, -4927, -1, 18109},
            {-79364, 113398, -4927, -1, 18109},
            {-79213, 113500, -4927, -1, 18109},
            {-79099, 113500, -4927, -1, 18109},
            {-78960, 113398, -4927, -1, 18109},
            {-78882, 113235, -4927, -1, 18109},
            {-78882, 113099, -4927, -1, 18109}
      },
      {
            {-76119, 110383, -4927, -1, 18110},
            {-75980, 110442, -4927, -1, 18110},
            {-75848, 110442, -4927, -1, 18110},
            {-75720, 110383, -4927, -1, 18110},
            {-75625, 110195, -4927, -1, 18110},
            {-75625, 110063, -4927, -1, 18110},
            {-75722, 109908, -4927, -1, 18110},
            {-75863, 109832, -4927, -1, 18110},
            {-75989, 109832, -4927, -1, 18110},
            {-76130, 109908, -4927, -1, 18110},
            {-76230, 110079, -4927, -1, 18110},
            {-76230, 110215, -4927, -1, 18110}
      },
      {
            {-74055, 111781, -4927, -1, 18111},
            {-74144, 111938, -4927, -1, 18111},
            {-74144, 112075, -4927, -1, 18111},
            {-74055, 112173, -4927, -1, 18111},
            {-73885, 112289, -4927, -1, 18111},
            {-73756, 112289, -4927, -1, 18111},
            {-73574, 112141, -4927, -1, 18111},
            {-73511, 112040, -4927, -1, 18111},
            {-73511, 111912, -4927, -1, 18111},
            {-73574, 111772, -4927, -1, 18111},
            {-73767, 111669, -4927, -1, 18111},
            {-73899, 111669, -4927, -1, 18111}
      },
      {
            {-76008, 113566, -4927, -1, 18112},
            {-76159, 113485, -4927, -1, 18112},
            {-76267, 113485, -4927, -1, 18112},
            {-76386, 113566, -4927, -1, 18112},
            {-76482, 113748, -4927, -1, 18112},
            {-76482, 113885, -4927, -1, 18112},
            {-76371, 114029, -4927, -1, 18112},
            {-76220, 114118, -4927, -1, 18112},
            {-76092, 114118, -4927, -1, 18112},
            {-75975, 114029, -4927, -1, 18112},
            {-75861, 113851, -4927, -1, 18112},
            {-75861, 113713, -4927, -1, 18112}
      },
      {
            {-79100, 109782, -4927, -1, 18113},
            {-78962, 109853, -4927, -1, 18113},
            {-78851, 109853, -4927, -1, 18113},
            {-78721, 109782, -4927, -1, 18113},
            {-78615, 109596, -4927, -1, 18113},
            {-78615, 109453, -4927, -1, 18113},
            {-78746, 109300, -4927, -1, 18113},
            {-78881, 109203, -4927, -1, 18113},
            {-79027, 109203, -4927, -1, 18113},
            {-79159, 109300, -4927, -1, 18113},
            {-79240, 109480, -4927, -1, 18113},
            {-79240, 109615, -4927, -1, 18113}
      }
   };
   protected static final int[][][] FESTIVAL_DUSK_CHEST_SPAWNS = new int[][][]{
      {
            {-77016, 88726, -5183, -1, 18114},
            {-77136, 88646, -5183, -1, 18114},
            {-77247, 88646, -5183, -1, 18114},
            {-77380, 88726, -5183, -1, 18114},
            {-77512, 88883, -5183, -1, 18114},
            {-77512, 89053, -5183, -1, 18114},
            {-77378, 89287, -5183, -1, 18114},
            {-77254, 89238, -5183, -1, 18114},
            {-77095, 89238, -5183, -1, 18114},
            {-76996, 89287, -5183, -1, 18114},
            {-76901, 89025, -5183, -1, 18114},
            {-76901, 88891, -5183, -1, 18114}
      },
      {
            {-77128, 85553, -5183, -1, 18115},
            {-77036, 85594, -5183, -1, 18115},
            {-76919, 85594, -5183, -1, 18115},
            {-76755, 85553, -5183, -1, 18115},
            {-76635, 85392, -5183, -1, 18115},
            {-76635, 85216, -5183, -1, 18115},
            {-76761, 85025, -5183, -1, 18115},
            {-76908, 85004, -5183, -1, 18115},
            {-77041, 85004, -5183, -1, 18115},
            {-77138, 85025, -5183, -1, 18115},
            {-77268, 85219, -5183, -1, 18115},
            {-77268, 85410, -5183, -1, 18115}
      },
      {
            {-75150, 87303, -5183, -1, 18116},
            {-75150, 87175, -5183, -1, 18116},
            {-75150, 87175, -5183, -1, 18116},
            {-75150, 87303, -5183, -1, 18116},
            {-74943, 87433, -5183, -1, 18116},
            {-74767, 87433, -5183, -1, 18116},
            {-74556, 87306, -5183, -1, 18116},
            {-74556, 87184, -5183, -1, 18116},
            {-74556, 87184, -5183, -1, 18116},
            {-74556, 87306, -5183, -1, 18116},
            {-74757, 86830, -5183, -1, 18116},
            {-74927, 86830, -5183, -1, 18116}
      },
      {
            {-80010, 88128, -5183, -1, 18117},
            {-80113, 88066, -5183, -1, 18117},
            {-80220, 88066, -5183, -1, 18117},
            {-80359, 88128, -5183, -1, 18117},
            {-80467, 88267, -5183, -1, 18117},
            {-80467, 88436, -5183, -1, 18117},
            {-80381, 88639, -5183, -1, 18117},
            {-80278, 88577, -5183, -1, 18117},
            {-80142, 88577, -5183, -1, 18117},
            {-80028, 88639, -5183, -1, 18117},
            {-79915, 88466, -5183, -1, 18117},
            {-79915, 88322, -5183, -1, 18117}
      },
      {
            {-80153, 84947, -5183, -1, 18118},
            {-80003, 84962, -5183, -1, 18118},
            {-79848, 84962, -5183, -1, 18118},
            {-79742, 84947, -5183, -1, 18118},
            {-79668, 84772, -5183, -1, 18118},
            {-79668, 84619, -5183, -1, 18118},
            {-79772, 84471, -5183, -1, 18118},
            {-79888, 84414, -5183, -1, 18118},
            {-80023, 84414, -5183, -1, 18118},
            {-80166, 84471, -5183, -1, 18118},
            {-80253, 84600, -5183, -1, 18118},
            {-80253, 84780, -5183, -1, 18118}
      }
   };
   private SevenSignsFestival.FestivalManager _managerInstance;
   protected ScheduledFuture<?> _managerScheduledTask;
   protected int _signsCycle = SevenSigns.getInstance().getCurrentCycle();
   protected int _festivalCycle;
   protected long _nextFestivalCycleStart;
   protected long _nextFestivalStart;
   protected boolean _festivalInitialized;
   protected boolean _festivalInProgress;
   protected List<Integer> _accumulatedBonuses = new ArrayList<>();
   boolean _noPartyRegister;
   private Npc _dawnChatGuide;
   private Npc _duskChatGuide;
   protected Map<Integer, List<Integer>> _dawnFestivalParticipants = new HashMap<>();
   protected Map<Integer, List<Integer>> _duskFestivalParticipants = new HashMap<>();
   protected Map<Integer, List<Integer>> _dawnPreviousParticipants = new HashMap<>();
   protected Map<Integer, List<Integer>> _duskPreviousParticipants = new HashMap<>();
   private final Map<Integer, Long> _dawnFestivalScores = new HashMap<>();
   private final Map<Integer, Long> _duskFestivalScores = new HashMap<>();
   private final Map<Integer, Map<Integer, StatSet>> _festivalData = new HashMap<>();

   protected SevenSignsFestival() {
      this.restoreFestivalData();
      if (SevenSigns.getInstance().isSealValidationPeriod()) {
         LOGGER.info("SevenSignsFestival: Initialization bypassed due to Seal Validation in effect.");
      } else {
         Spawn.addSpawnListener(this);
         this.startFestivalManager();
      }
   }

   public static SevenSignsFestival getInstance() {
      return SevenSignsFestival.SingletonHolder.INSTANCE;
   }

   public static String getFestivalName(int festivalID) {
      return switch (festivalID) {
         case 0 -> "Level 31 or lower";
         case 1 -> "Level 42 or lower";
         case 2 -> "Level 53 or lower";
         case 3 -> "Level 64 or lower";
         default -> "No Level Limit";
      };
   }

   public static int getMaxLevelForFestival(int festivalId) {
      int maxLevel = ExperienceData.getInstance().getMaxLevel() - 1;
      switch (festivalId) {
         case 0:
            maxLevel = 31;
            break;
         case 1:
            maxLevel = 42;
            break;
         case 2:
            maxLevel = 53;
            break;
         case 3:
            maxLevel = 64;
      }

      return maxLevel;
   }

   protected static boolean isFestivalArcher(int npcId) {
      if (npcId >= 18009 && npcId <= 18108) {
         int identifier = npcId % 10;
         return identifier == 4 || identifier == 9;
      } else {
         return false;
      }
   }

   protected static boolean isFestivalChest(int npcId) {
      return npcId < 18109 || npcId > 18118;
   }

   protected ScheduledFuture<?> getFestivalManagerSchedule() {
      if (this._managerScheduledTask == null) {
         this.startFestivalManager();
      }

      return this._managerScheduledTask;
   }

   protected void startFestivalManager() {
      this._managerInstance = new SevenSignsFestival.FestivalManager();
      this.setNextFestivalStart(FeatureConfig.ALT_FESTIVAL_MANAGER_START + FESTIVAL_SIGNUP_TIME);
      this._managerScheduledTask = ThreadPool.scheduleAtFixedRate(
         this._managerInstance, FeatureConfig.ALT_FESTIVAL_MANAGER_START, FeatureConfig.ALT_FESTIVAL_CYCLE_LENGTH
      );
      LOGGER.info("SevenSignsFestival: The first Festival of Darkness cycle begins in " + FeatureConfig.ALT_FESTIVAL_MANAGER_START / 60000L + " minute(s).");
   }

   protected void restoreFestivalData() {
      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery("SELECT festivalId, cabal, cycle, date, score, members FROM seven_signs_festival");
      ) {
         while (rs.next()) {
            int festivalCycle = rs.getInt("cycle");
            int festivalId = rs.getInt("festivalId");
            String cabal = rs.getString("cabal");
            StatSet festivalDat = new StatSet();
            festivalDat.set("festivalId", festivalId);
            festivalDat.set("cabal", cabal);
            festivalDat.set("cycle", festivalCycle);
            festivalDat.set("date", rs.getString("date"));
            festivalDat.set("score", rs.getInt("score"));
            festivalDat.set("members", rs.getString("members"));
            if (cabal.equals("dawn")) {
               festivalId += 5;
            }

            Map<Integer, StatSet> tempData = this._festivalData.computeIfAbsent(festivalCycle, var0 -> new HashMap<>());
            tempData.put(festivalId, festivalDat);
         }
      } catch (Exception var22) {
         LOGGER.log(Level.SEVERE, "SevenSignsFestival: Failed to load configuration: " + var22.getMessage(), (Throwable)var22);
      }

      StringBuilder query = new StringBuilder();
      query.append("SELECT festival_cycle, ");

      for (int i = 0; i < 4; i++) {
         query.append("accumulated_bonus");
         query.append(String.valueOf(i));
         query.append(", ");
      }

      query.append("accumulated_bonus");
      query.append(String.valueOf(4));
      query.append(' ');
      query.append("FROM seven_signs_status WHERE id=0");

      try (
         Connection con = DatabaseFactory.getConnection();
         Statement s = con.createStatement();
         ResultSet rs = s.executeQuery(query.toString());
      ) {
         while (rs.next()) {
            this._festivalCycle = rs.getInt("festival_cycle");

            for (int i = 0; i < 5; i++) {
               this._accumulatedBonuses.add(i, rs.getInt("accumulated_bonus" + i));
            }
         }
      } catch (Exception var18) {
         LOGGER.log(Level.SEVERE, "SevenSignsFestival: Failed to load configuration: " + var18.getMessage(), (Throwable)var18);
      }
   }

   public void saveFestivalData(boolean updateSettings) {
      try (
         Connection con = DatabaseFactory.getConnection();
         PreparedStatement psInsert = con.prepareStatement(
            "REPLACE INTO seven_signs_festival (festivalId, cabal, cycle, date, score, members) VALUES (?,?,?,?,?,?)"
         );
      ) {
         for (Map<Integer, StatSet> currCycleData : this._festivalData.values()) {
            for (StatSet festivalDat : currCycleData.values()) {
               psInsert.setInt(1, festivalDat.getInt("festivalId"));
               psInsert.setString(2, festivalDat.getString("cabal"));
               psInsert.setInt(3, festivalDat.getInt("cycle"));
               psInsert.setLong(4, Long.parseLong(festivalDat.getString("date")));
               psInsert.setInt(5, festivalDat.getInt("score"));
               psInsert.setString(6, festivalDat.getString("members"));
               psInsert.addBatch();
            }
         }

         psInsert.executeBatch();
      } catch (Exception var12) {
         LOGGER.log(Level.SEVERE, "SevenSignsFestival: Failed to save configuration: " + var12.getMessage(), (Throwable)var12);
      }

      if (updateSettings) {
         SevenSigns.getInstance().saveSevenSignsStatus();
      }
   }

   protected void rewardHighestRanked() {
      StatSet overallData = this.getOverallHighestScoreData(0);
      if (overallData != null) {
         String[] partyMembers = overallData.getString("members").split(",");

         for (String partyMemberName : partyMembers) {
            this.addReputationPointsForPartyMemberClan(partyMemberName);
         }
      }

      overallData = this.getOverallHighestScoreData(1);
      if (overallData != null) {
         String[] partyMembers = overallData.getString("members").split(",");

         for (String partyMemberName : partyMembers) {
            this.addReputationPointsForPartyMemberClan(partyMemberName);
         }
      }

      overallData = this.getOverallHighestScoreData(2);
      if (overallData != null) {
         String[] partyMembers = overallData.getString("members").split(",");

         for (String partyMemberName : partyMembers) {
            this.addReputationPointsForPartyMemberClan(partyMemberName);
         }
      }

      overallData = this.getOverallHighestScoreData(3);
      if (overallData != null) {
         String[] partyMembers = overallData.getString("members").split(",");

         for (String partyMemberName : partyMembers) {
            this.addReputationPointsForPartyMemberClan(partyMemberName);
         }
      }

      overallData = this.getOverallHighestScoreData(4);
      if (overallData != null) {
         String[] partyMembers = overallData.getString("members").split(",");

         for (String partyMemberName : partyMembers) {
            this.addReputationPointsForPartyMemberClan(partyMemberName);
         }
      }
   }

   private void addReputationPointsForPartyMemberClan(String partyMemberName) {
      Player player = World.getPlayer(partyMemberName);
      if (player != null) {
         Clan clan = player.getClan();
         if (clan != null) {
            clan.addReputationScore(FeatureConfig.FESTIVAL_WIN_POINTS);
            SystemMessage sm = new SystemMessage(
               SystemMessageId.CLAN_MEMBER_C1_WAS_AN_ACTIVE_MEMBER_OF_THE_HIGHEST_RANKED_PARTY_IN_THE_FESTIVAL_OF_DARKNESS_S2_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_S_REPUTATION_SCORE
            );
            sm.addString(partyMemberName);
            sm.addInt(FeatureConfig.FESTIVAL_WIN_POINTS);
            clan.broadcastToOnlineMembers(sm);
         }
      } else {
         try (
            Connection con = DatabaseFactory.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT clan_name FROM clan_data WHERE clan_id = (SELECT clanid FROM characters WHERE char_name = ?)");
         ) {
            ps.setString(1, partyMemberName);

            try (ResultSet rs = ps.executeQuery()) {
               if (rs.next()) {
                  String clanName = rs.getString("clan_name");
                  if (clanName != null) {
                     Clan clan = ClanTable.getInstance().getClanByName(clanName);
                     if (clan != null) {
                        clan.addReputationScore(FeatureConfig.FESTIVAL_WIN_POINTS);
                        SystemMessage sm = new SystemMessage(
                           SystemMessageId.CLAN_MEMBER_C1_WAS_AN_ACTIVE_MEMBER_OF_THE_HIGHEST_RANKED_PARTY_IN_THE_FESTIVAL_OF_DARKNESS_S2_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_S_REPUTATION_SCORE
                        );
                        sm.addString(partyMemberName);
                        sm.addInt(FeatureConfig.FESTIVAL_WIN_POINTS);
                        clan.broadcastToOnlineMembers(sm);
                     }
                  }
               }
            }
         } catch (Exception var15) {
            LOGGER.log(Level.WARNING, "Could not get clan name of " + partyMemberName + ": " + var15.getMessage(), (Throwable)var15);
         }
      }
   }

   protected void resetFestivalData(boolean updateSettings) {
      this._festivalCycle = 0;
      this._signsCycle = SevenSigns.getInstance().getCurrentCycle();

      for (int i = 0; i < 5; i++) {
         this._accumulatedBonuses.set(i, 0);
      }

      this._dawnFestivalParticipants.clear();
      this._dawnPreviousParticipants.clear();
      this._dawnFestivalScores.clear();
      this._duskFestivalParticipants.clear();
      this._duskPreviousParticipants.clear();
      this._duskFestivalScores.clear();
      Map<Integer, StatSet> newData = new HashMap<>();

      for (int i = 0; i < 10; i++) {
         int festivalId = i;
         if (i >= 5) {
            festivalId = i - 5;
         }

         StatSet tempStats = new StatSet();
         tempStats.set("festivalId", festivalId);
         tempStats.set("cycle", this._signsCycle);
         tempStats.set("date", "0");
         tempStats.set("score", 0);
         tempStats.set("members", "");
         if (i >= 5) {
            tempStats.set("cabal", SevenSigns.getCabalShortName(2));
         } else {
            tempStats.set("cabal", SevenSigns.getCabalShortName(1));
         }

         newData.put(i, tempStats);
      }

      this._festivalData.put(this._signsCycle, newData);
      this.saveFestivalData(updateSettings);

      for (Player player : World.getPlayers()) {
         Item bloodOfferings = player.getInventory().getItemByItemId(5901);
         if (bloodOfferings != null) {
            player.destroyItem(ItemProcessType.FEE, bloodOfferings, null, false);
         }
      }

      LOGGER.info("SevenSignsFestival: Reinitialized engine for next competition period.");
   }

   public int getCurrentFestivalCycle() {
      return this._festivalCycle;
   }

   public boolean isFestivalInitialized() {
      return this._festivalInitialized;
   }

   public boolean isFestivalInProgress() {
      return this._festivalInProgress;
   }

   public void setNextCycleStart() {
      this._nextFestivalCycleStart = System.currentTimeMillis() + FeatureConfig.ALT_FESTIVAL_CYCLE_LENGTH;
   }

   public void setNextFestivalStart(long milliFromNow) {
      this._nextFestivalStart = System.currentTimeMillis() + milliFromNow;
   }

   public long getMinsToNextCycle() {
      return SevenSigns.getInstance().isSealValidationPeriod() ? -1L : (this._nextFestivalCycleStart - System.currentTimeMillis()) / 60000L;
   }

   public int getMinsToNextFestival() {
      return SevenSigns.getInstance().isSealValidationPeriod() ? -1 : (int)((this._nextFestivalStart - System.currentTimeMillis()) / 60000L + 1L);
   }

   public String getTimeToNextFestivalStr() {
      return SevenSigns.getInstance().isSealValidationPeriod()
         ? "<font color=\"FF0000\">This is the Seal Validation period. Festivals will resume next week.</font>"
         : "<font color=\"FF0000\">The next festival will begin in " + this.getMinsToNextFestival() + " minute(s).</font>";
   }

   public int[] getFestivalForPlayer(Player player) {
      int[] playerFestivalInfo = new int[]{-1, -1};

      for (int festivalId = 0; festivalId < 5; festivalId++) {
         List<Integer> participants = this._dawnFestivalParticipants.get(festivalId);
         if (participants != null && participants.contains(player.getObjectId())) {
            playerFestivalInfo[0] = 2;
            playerFestivalInfo[1] = festivalId;
            return playerFestivalInfo;
         }

         participants = this._duskFestivalParticipants.get(++festivalId);
         if (participants != null && participants.contains(player.getObjectId())) {
            playerFestivalInfo[0] = 1;
            playerFestivalInfo[1] = festivalId;
            return playerFestivalInfo;
         }
      }

      return playerFestivalInfo;
   }

   public boolean isParticipant(Player player) {
      if (SevenSigns.getInstance().isSealValidationPeriod()) {
         return false;
      } else if (this._managerInstance == null) {
         return false;
      } else {
         for (List<Integer> participants : this._dawnFestivalParticipants.values()) {
            if (participants != null && participants.contains(player.getObjectId())) {
               return true;
            }
         }

         for (List<Integer> participantsx : this._duskFestivalParticipants.values()) {
            if (participantsx != null && participantsx.contains(player.getObjectId())) {
               return true;
            }
         }

         return false;
      }
   }

   public List<Integer> getParticipants(int oracle, int festivalId) {
      return oracle == 2 ? this._dawnFestivalParticipants.get(festivalId) : this._duskFestivalParticipants.get(festivalId);
   }

   public List<Integer> getPreviousParticipants(int oracle, int festivalId) {
      return oracle == 2 ? this._dawnPreviousParticipants.get(festivalId) : this._duskPreviousParticipants.get(festivalId);
   }

   public void setParticipants(int oracle, int festivalId, Party festivalParty) {
      List<Integer> participants = null;
      if (festivalParty != null) {
         participants = new ArrayList<>(festivalParty.getMemberCount());

         for (Player player : festivalParty.getMembers()) {
            if (player != null) {
               participants.add(player.getObjectId());
            }
         }
      }

      if (oracle == 2) {
         this._dawnFestivalParticipants.put(festivalId, participants);
      } else {
         this._duskFestivalParticipants.put(festivalId, participants);
      }
   }

   public void updateParticipants(Player player, Party festivalParty) {
      if (this.isParticipant(player)) {
         int[] playerFestInfo = this.getFestivalForPlayer(player);
         int oracle = playerFestInfo[0];
         int festivalId = playerFestInfo[1];
         if (festivalId > -1) {
            if (this._festivalInitialized) {
               SevenSignsFestival.DarknessFestival festivalInst = this._managerInstance.getFestivalInstance(oracle, festivalId);
               if (festivalParty == null) {
                  for (int partyMemberObjId : this.getParticipants(oracle, festivalId)) {
                     Player partyMember = World.getPlayer(partyMemberObjId);
                     if (partyMember != null) {
                        festivalInst.relocatePlayer(partyMember, true);
                     }
                  }
               } else {
                  festivalInst.relocatePlayer(player, true);
               }
            }

            this.setParticipants(oracle, festivalId, festivalParty);
            if (festivalParty != null && festivalParty.getMemberCount() < FeatureConfig.ALT_FESTIVAL_MIN_PLAYER) {
               this.updateParticipants(player, null);
               festivalParty.removePartyMember(player, PartyMessageType.EXPELLED);
            }
         }
      }
   }

   public long getFinalScore(int oracle, int festivalId) {
      return oracle == 2 ? this._dawnFestivalScores.get(festivalId) : this._duskFestivalScores.get(festivalId);
   }

   public int getHighestScore(int oracle, int festivalId) {
      return this.getHighestScoreData(oracle, festivalId).getInt("score");
   }

   public StatSet getHighestScoreData(int oracle, int festivalId) {
      int offsetId = festivalId;
      if (oracle == 2) {
         offsetId = festivalId + 5;
      }

      StatSet currData = null;

      try {
         currData = this._festivalData.get(this._signsCycle).get(offsetId);
      } catch (Exception var6) {
         currData = new StatSet();
         currData.set("score", 0);
         currData.set("members", "");
      }

      return currData;
   }

   public StatSet getOverallHighestScoreData(int festivalId) {
      StatSet result = null;
      int highestScore = 0;

      for (Map<Integer, StatSet> currCycleData : this._festivalData.values()) {
         for (StatSet currFestData : currCycleData.values()) {
            int currFestID = currFestData.getInt("festivalId");
            if (currFestID == festivalId) {
               int festivalScore = currFestData.getInt("score");
               if (festivalScore > highestScore) {
                  highestScore = festivalScore;
                  result = currFestData;
               }
            }
         }
      }

      return result;
   }

   public boolean setFinalScore(Player player, int oracle, int festivalId, long offeringScore) {
      int currDawnHighScore = this.getHighestScore(2, festivalId);
      int currDuskHighScore = this.getHighestScore(1, festivalId);
      int thisCabalHighScore = 0;
      int otherCabalHighScore = 0;
      if (oracle == 2) {
         thisCabalHighScore = currDawnHighScore;
         otherCabalHighScore = currDuskHighScore;
         this._dawnFestivalScores.put(festivalId, offeringScore);
      } else {
         thisCabalHighScore = currDuskHighScore;
         otherCabalHighScore = currDawnHighScore;
         this._duskFestivalScores.put(festivalId, offeringScore);
      }

      StatSet currFestData = this.getHighestScoreData(oracle, festivalId);
      if (offeringScore <= thisCabalHighScore) {
         return false;
      } else if (thisCabalHighScore > otherCabalHighScore) {
         return false;
      } else {
         List<Integer> prevParticipants = this.getPreviousParticipants(oracle, festivalId);
         StringJoiner sj = new StringJoiner(",");

         for (Integer partyMember : prevParticipants) {
            sj.add(CharInfoTable.getInstance().getNameById(partyMember));
         }

         currFestData.set("date", String.valueOf(System.currentTimeMillis()));
         currFestData.set("score", offeringScore);
         currFestData.set("members", sj.toString());
         if (offeringScore > otherCabalHighScore) {
            int contribPoints = FESTIVAL_LEVEL_SCORES[festivalId];
            SevenSigns.getInstance().addFestivalScore(oracle, contribPoints);
         }

         this.saveFestivalData(true);
         return true;
      }
   }

   public int getAccumulatedBonus(int festivalId) {
      return this._accumulatedBonuses.get(festivalId);
   }

   public int getTotalAccumulatedBonus() {
      int totalAccumBonus = 0;

      for (int accumBonus : this._accumulatedBonuses) {
         totalAccumBonus += accumBonus;
      }

      return totalAccumBonus;
   }

   public void addAccumulatedBonus(int festivalId, int stoneType, int stoneAmount) {
      int eachStoneBonus = 0;
      switch (stoneType) {
         case 6360:
            eachStoneBonus = 3;
            break;
         case 6361:
            eachStoneBonus = 5;
            break;
         case 6362:
            eachStoneBonus = 10;
      }

      int newTotalBonus = this._accumulatedBonuses.get(festivalId) + stoneAmount * eachStoneBonus;
      this._accumulatedBonuses.set(festivalId, newTotalBonus);
   }

   public int distribAccumulatedBonus(Player player) {
      int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
      if (playerCabal != SevenSigns.getInstance().getCabalHighestScore()) {
         return 0;
      } else {
         int playerBonus = 0;
         String playerName = player.getName();
         Map<Integer, StatSet> festivalDataMap = this._festivalData.get(this._signsCycle);
         if (festivalDataMap != null) {
            for (StatSet festivalData : festivalDataMap.values()) {
               if (festivalData.getString("members").contains(playerName)) {
                  int festivalId = festivalData.getInt("festivalId");
                  int numPartyMembers = festivalData.getString("members").split(",").length;
                  int totalAccumBonus = this._accumulatedBonuses.get(festivalId);
                  playerBonus = totalAccumBonus / numPartyMembers;
                  this._accumulatedBonuses.set(festivalId, totalAccumBonus - playerBonus);
                  break;
               }
            }
         }

         return playerBonus;
      }
   }

   public void sendMessageToAll(String npcString) {
      if (this._dawnChatGuide != null && this._duskChatGuide != null) {
         this.sendMessageToAll(npcString, this._dawnChatGuide);
         this.sendMessageToAll(npcString, this._duskChatGuide);
      }
   }

   public void sendMessageToAll(String npcString, Npc npc) {
      CreatureSay cs = new CreatureSay(npc, ChatType.NPC_SHOUT, npcString);
      npc.broadcastPacket(cs);
   }

   public boolean increaseChallenge(int oracle, int festivalId) {
      SevenSignsFestival.DarknessFestival festivalInst = this._managerInstance.getFestivalInstance(oracle, festivalId);
      return festivalInst.increaseChallenge();
   }

   public void npcSpawned(Npc npc) {
      if (npc != null) {
         int npcId = npc.getId();
         if (npcId == 31127) {
            this._dawnChatGuide = npc;
         }

         if (npcId == 31137) {
            this._duskChatGuide = npc;
         }
      }
   }

   private class DarknessFestival {
      protected final int _cabal;
      protected final int _levelRange;
      protected boolean _challengeIncreased;
      private SevenSignsFestival.FestivalSpawn _startLocation;
      private SevenSignsFestival.FestivalSpawn _witchSpawn;
      private Npc _witchInst;
      List<FestivalMonster> _npcInsts;
      private List<Integer> _participants;
      private final Map<Integer, SevenSignsFestival.FestivalSpawn> _originalLocations;

      protected DarknessFestival(int cabal, int levelRange) {
         Objects.requireNonNull(SevenSignsFestival.this);
         super();
         this._npcInsts = new ArrayList<>();
         this._originalLocations = new ConcurrentHashMap<>();
         this._cabal = cabal;
         this._levelRange = levelRange;
         if (cabal == 2) {
            this._participants = SevenSignsFestival.this._dawnFestivalParticipants.get(levelRange);
            this._witchSpawn = new SevenSignsFestival.FestivalSpawn(SevenSignsFestival.FESTIVAL_DAWN_WITCH_SPAWNS[levelRange]);
            this._startLocation = new SevenSignsFestival.FestivalSpawn(SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[levelRange]);
         } else {
            this._participants = SevenSignsFestival.this._duskFestivalParticipants.get(levelRange);
            this._witchSpawn = new SevenSignsFestival.FestivalSpawn(SevenSignsFestival.FESTIVAL_DUSK_WITCH_SPAWNS[levelRange]);
            this._startLocation = new SevenSignsFestival.FestivalSpawn(SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[levelRange]);
         }

         if (this._participants == null) {
            this._participants = new ArrayList<>();
         }

         this.festivalInit();
      }

      protected void festivalInit() {
         if (this._participants != null && !this._participants.isEmpty()) {
            try {
               for (int participantObjId : this._participants) {
                  Player participant = World.getPlayer(participantObjId);
                  if (participant != null) {
                     this._originalLocations
                        .put(
                           participantObjId,
                           new SevenSignsFestival.FestivalSpawn(participant.getX(), participant.getY(), participant.getZ(), participant.getHeading())
                        );
                     int x = this._startLocation._x;
                     int y = this._startLocation._y;
                     if (Rnd.nextBoolean()) {
                        x += Rnd.get(230);
                        y += Rnd.get(230);
                     } else {
                        x -= Rnd.get(230);
                        y -= Rnd.get(230);
                     }

                     participant.getAI().setIntentionIdle();
                     participant.teleToLocation(new Location(x, y, this._startLocation._z), true);
                     participant.stopAllEffectsExceptThoseThatLastThroughDeath();
                     Item bloodOfferings = participant.getInventory().getItemByItemId(5901);
                     if (bloodOfferings != null) {
                        participant.destroyItem(ItemProcessType.FEE, bloodOfferings, null, true);
                     }
                  }
               }
            } catch (NullPointerException var8) {
            }
         }

         try {
            Spawn npcSpawn = new Spawn(this._witchSpawn._npcId);
            npcSpawn.setXYZ(this._witchSpawn._x, this._witchSpawn._y, this._witchSpawn._z);
            npcSpawn.setHeading(this._witchSpawn._heading);
            npcSpawn.setAmount(1);
            npcSpawn.setRespawnDelay(1);
            npcSpawn.startRespawn();
            SpawnTable.getInstance().addSpawn(npcSpawn);
            this._witchInst = npcSpawn.doSpawn();
         } catch (Exception var7) {
            SevenSignsFestival.LOGGER
               .log(
                  Level.WARNING,
                  "SevenSignsFestival: Error while spawning Festival Witch ID " + this._witchSpawn._npcId + ": " + var7.getMessage(),
                  (Throwable)var7
               );
         }

         MagicSkillUse msu = new MagicSkillUse(this._witchInst, this._witchInst, 2003, 1, 1, 0);
         this._witchInst.broadcastPacket(msu);
         msu = new MagicSkillUse(this._witchInst, this._witchInst, 2133, 1, 1, 0);
         this._witchInst.broadcastPacket(msu);
         this.sendMessageToParticipants("The main event will start in 2 minutes. Please register now.");
      }

      protected void festivalStart() {
         this.spawnFestivalMonsters(60, 0);
      }

      protected void moveMonstersToCenter() {
         for (FestivalMonster festivalMob : this._npcInsts) {
            if (!festivalMob.isDead()) {
               Intention currIntention = festivalMob.getAI().getIntention();
               if (currIntention == Intention.IDLE || currIntention == Intention.ACTIVE) {
                  int x = this._startLocation._x;
                  int y = this._startLocation._y;
                  if (Rnd.nextBoolean()) {
                     x += Rnd.get(230);
                     y += Rnd.get(230);
                  } else {
                     x -= Rnd.get(230);
                     y -= Rnd.get(230);
                  }

                  festivalMob.setRunning();
                  festivalMob.getAI().setIntentionMoveTo(new Location(x, y, this._startLocation._z, Rnd.get(65536)));
               }
            }
         }
      }

      protected void spawnFestivalMonsters(int respawnDelay, int spawnType) {
         int[][] npcSpawns = null;
         switch (spawnType) {
            case 0:
            case 1:
               npcSpawns = this._cabal == 2
                  ? SevenSignsFestival.FESTIVAL_DAWN_PRIMARY_SPAWNS[this._levelRange]
                  : SevenSignsFestival.FESTIVAL_DUSK_PRIMARY_SPAWNS[this._levelRange];
               break;
            case 2:
               npcSpawns = this._cabal == 2
                  ? SevenSignsFestival.FESTIVAL_DAWN_SECONDARY_SPAWNS[this._levelRange]
                  : SevenSignsFestival.FESTIVAL_DUSK_SECONDARY_SPAWNS[this._levelRange];
               break;
            case 3:
               npcSpawns = this._cabal == 2
                  ? SevenSignsFestival.FESTIVAL_DAWN_CHEST_SPAWNS[this._levelRange]
                  : SevenSignsFestival.FESTIVAL_DUSK_CHEST_SPAWNS[this._levelRange];
               break;
            default:
               return;
         }

         for (int[] spawn : npcSpawns) {
            SevenSignsFestival.FestivalSpawn currSpawn = new SevenSignsFestival.FestivalSpawn(spawn);
            if (spawnType != 1 || !SevenSignsFestival.isFestivalArcher(currSpawn._npcId)) {
               try {
                  Spawn npcSpawn = new Spawn(currSpawn._npcId);
                  npcSpawn.setXYZ(currSpawn._x, currSpawn._y, currSpawn._z);
                  npcSpawn.setHeading(Rnd.get(65536));
                  npcSpawn.setAmount(1);
                  npcSpawn.setRespawnDelay(respawnDelay);
                  npcSpawn.startRespawn();
                  SpawnTable.getInstance().addSpawn(npcSpawn);
                  FestivalMonster festivalMob = (FestivalMonster)npcSpawn.doSpawn();
                  if (spawnType == 1) {
                     festivalMob.setOfferingBonus(2);
                  } else if (spawnType == 3) {
                     festivalMob.setOfferingBonus(5);
                  }

                  this._npcInsts.add(festivalMob);
               } catch (Exception var11) {
                  SevenSignsFestival.LOGGER
                     .log(Level.WARNING, "SevenSignsFestival: Error while spawning NPC ID " + currSpawn._npcId + ": " + var11.getMessage(), (Throwable)var11);
               }
            }
         }
      }

      protected boolean increaseChallenge() {
         if (this._challengeIncreased) {
            return false;
         } else {
            this._challengeIncreased = true;
            this.spawnFestivalMonsters(60, 1);
            return true;
         }
      }

      public void sendMessageToParticipants(String text) {
         if (this._participants != null && !this._participants.isEmpty()) {
            this._witchInst.broadcastPacket(new CreatureSay(this._witchInst, ChatType.NPC_GENERAL, text));
         }
      }

      protected void festivalEnd() {
         if (this._participants != null && !this._participants.isEmpty()) {
            for (int participantObjId : this._participants) {
               try {
                  Player participant = World.getPlayer(participantObjId);
                  if (participant != null) {
                     this.relocatePlayer(participant, false);
                     participant.sendMessage("The festival has ended. Your party leader must now register your score before the next festival takes place.");
                  }
               } catch (Exception var4) {
               }
            }

            if (this._cabal == 2) {
               SevenSignsFestival.this._dawnPreviousParticipants.put(this._levelRange, this._participants);
            } else {
               SevenSignsFestival.this._duskPreviousParticipants.put(this._levelRange, this._participants);
            }
         }

         this._participants = null;
         this.unspawnMobs();
      }

      protected void unspawnMobs() {
         if (this._witchInst != null) {
            this._witchInst.getSpawn().stopRespawn();
            this._witchInst.deleteMe();
            SpawnTable.getInstance().removeSpawn(this._witchInst.getSpawn());
         }

         for (FestivalMonster monsterInst : this._npcInsts) {
            if (monsterInst != null) {
               monsterInst.getSpawn().stopRespawn();
               monsterInst.deleteMe();
               SpawnTable.getInstance().removeSpawn(monsterInst.getSpawn());
            }
         }
      }

      public void relocatePlayer(Player participant, boolean isRemoving) {
         try {
            SevenSignsFestival.FestivalSpawn origPosition = this._originalLocations.get(participant.getObjectId());
            if (isRemoving) {
               this._originalLocations.remove(participant.getObjectId());
            }

            participant.getAI().setIntentionIdle();
            participant.teleToLocation(new Location(origPosition._x, origPosition._y, origPosition._z), true);
            participant.sendMessage("You have been removed from the festival arena.");
         } catch (Exception var6) {
            try {
               participant.teleToLocation(TeleportWhereType.TOWN);
               participant.sendMessage("You have been removed from the festival arena.");
            } catch (Exception var5) {
            }
         }
      }
   }

   private class FestivalManager implements Runnable {
      protected Map<Integer, SevenSignsFestival.DarknessFestival> _festivalInstances;

      public FestivalManager() {
         Objects.requireNonNull(SevenSignsFestival.this);
         super();
         this._festivalInstances = new HashMap<>();
         SevenSignsFestival.this._festivalCycle++;
         SevenSignsFestival.this.setNextCycleStart();
         SevenSignsFestival.this.setNextFestivalStart(FeatureConfig.ALT_FESTIVAL_CYCLE_LENGTH - SevenSignsFestival.FESTIVAL_SIGNUP_TIME);
      }

      @Override
      public synchronized void run() {
         try {
            if (SevenSigns.getInstance().isSealValidationPeriod()) {
               return;
            }

            if (SevenSigns.getInstance().getMilliToPeriodChange() < FeatureConfig.ALT_FESTIVAL_CYCLE_LENGTH) {
               return;
            }

            if (SevenSignsFestival.this.getMinsToNextFestival() == 2) {
               SevenSignsFestival.this.sendMessageToAll("The main event will start in 2 minutes. Please register now.");
            }

            try {
               this.wait(SevenSignsFestival.FESTIVAL_SIGNUP_TIME);
            } catch (Exception var13) {
            }

            SevenSignsFestival.this._dawnPreviousParticipants.clear();
            SevenSignsFestival.this._duskPreviousParticipants.clear();

            for (SevenSignsFestival.DarknessFestival festivalInst : this._festivalInstances.values()) {
               festivalInst.unspawnMobs();
            }

            SevenSignsFestival.this._noPartyRegister = true;

            while (SevenSignsFestival.this._noPartyRegister) {
               if (SevenSignsFestival.this._duskFestivalParticipants.isEmpty() && SevenSignsFestival.this._dawnFestivalParticipants.isEmpty()) {
                  try {
                     SevenSignsFestival.this.setNextCycleStart();
                     SevenSignsFestival.this.setNextFestivalStart(FeatureConfig.ALT_FESTIVAL_CYCLE_LENGTH - SevenSignsFestival.FESTIVAL_SIGNUP_TIME);
                     this.wait(FeatureConfig.ALT_FESTIVAL_CYCLE_LENGTH - SevenSignsFestival.FESTIVAL_SIGNUP_TIME);

                     for (SevenSignsFestival.DarknessFestival festivalInst : this._festivalInstances.values()) {
                        if (!festivalInst._npcInsts.isEmpty()) {
                           festivalInst.unspawnMobs();
                        }
                     }
                  } catch (Exception var14) {
                  }
               } else {
                  SevenSignsFestival.this._noPartyRegister = false;
               }
            }

            long elapsedTime = 0L;

            for (int i = 0; i < 5; i++) {
               if (SevenSignsFestival.this._duskFestivalParticipants.get(i) != null) {
                  this._festivalInstances.put(10 + i, SevenSignsFestival.this.new DarknessFestival(1, i));
               }

               if (SevenSignsFestival.this._dawnFestivalParticipants.get(i) != null) {
                  this._festivalInstances.put(20 + i, SevenSignsFestival.this.new DarknessFestival(2, i));
               }
            }

            SevenSignsFestival.this._festivalInitialized = true;
            SevenSignsFestival.this.setNextFestivalStart(FeatureConfig.ALT_FESTIVAL_CYCLE_LENGTH);
            SevenSignsFestival.this.sendMessageToAll("The main event is now starting.");

            try {
               this.wait(FeatureConfig.ALT_FESTIVAL_FIRST_SPAWN);
            } catch (Exception var12) {
            }

            elapsedTime = FeatureConfig.ALT_FESTIVAL_FIRST_SPAWN;
            SevenSignsFestival.this._festivalInProgress = true;

            for (SevenSignsFestival.DarknessFestival festivalInstx : this._festivalInstances.values()) {
               festivalInstx.festivalStart();
               festivalInstx.sendMessageToParticipants("The main event is now starting.");
            }

            try {
               this.wait(FeatureConfig.ALT_FESTIVAL_FIRST_SWARM - FeatureConfig.ALT_FESTIVAL_FIRST_SPAWN);
            } catch (Exception var11) {
            }

            elapsedTime += FeatureConfig.ALT_FESTIVAL_FIRST_SWARM - FeatureConfig.ALT_FESTIVAL_FIRST_SPAWN;

            for (SevenSignsFestival.DarknessFestival festivalInstx : this._festivalInstances.values()) {
               festivalInstx.moveMonstersToCenter();
            }

            try {
               this.wait(FeatureConfig.ALT_FESTIVAL_SECOND_SPAWN - FeatureConfig.ALT_FESTIVAL_FIRST_SWARM);
            } catch (Exception var10) {
            }

            for (SevenSignsFestival.DarknessFestival festivalInstx : this._festivalInstances.values()) {
               festivalInstx.spawnFestivalMonsters(30, 2);
               long end = (FeatureConfig.ALT_FESTIVAL_LENGTH - FeatureConfig.ALT_FESTIVAL_SECOND_SPAWN) / 60000L;
               if (end == 2L) {
                  festivalInstx.sendMessageToParticipants("The Festival of Darkness will end in two minutes.");
               } else {
                  festivalInstx.sendMessageToParticipants("The Festival of Darkness will end in " + end + " minute(s).");
               }
            }

            elapsedTime += FeatureConfig.ALT_FESTIVAL_SECOND_SPAWN - FeatureConfig.ALT_FESTIVAL_FIRST_SWARM;

            try {
               this.wait(FeatureConfig.ALT_FESTIVAL_SECOND_SWARM - FeatureConfig.ALT_FESTIVAL_SECOND_SPAWN);
            } catch (Exception var9) {
            }

            for (SevenSignsFestival.DarknessFestival festivalInstxx : this._festivalInstances.values()) {
               festivalInstxx.moveMonstersToCenter();
            }

            elapsedTime += FeatureConfig.ALT_FESTIVAL_SECOND_SWARM - FeatureConfig.ALT_FESTIVAL_SECOND_SPAWN;

            try {
               this.wait(FeatureConfig.ALT_FESTIVAL_CHEST_SPAWN - FeatureConfig.ALT_FESTIVAL_SECOND_SWARM);
            } catch (Exception var8) {
            }

            for (SevenSignsFestival.DarknessFestival festivalInstxx : this._festivalInstances.values()) {
               festivalInstxx.spawnFestivalMonsters(60, 3);
               festivalInstxx.sendMessageToParticipants("The chests have spawned! Be quick, the festival will end soon.");
            }

            elapsedTime += FeatureConfig.ALT_FESTIVAL_CHEST_SPAWN - FeatureConfig.ALT_FESTIVAL_SECOND_SWARM;

            try {
               this.wait(FeatureConfig.ALT_FESTIVAL_LENGTH - elapsedTime);
            } catch (Exception var7) {
            }

            SevenSignsFestival.this._festivalInProgress = false;

            for (SevenSignsFestival.DarknessFestival festivalInstxx : this._festivalInstances.values()) {
               festivalInstxx.festivalEnd();
            }

            SevenSignsFestival.this._dawnFestivalParticipants.clear();
            SevenSignsFestival.this._duskFestivalParticipants.clear();
            SevenSignsFestival.this._festivalInitialized = false;
            SevenSignsFestival.this.sendMessageToAll("That will do! I'll move you to the outside soon.");
         } catch (Exception var15) {
            SevenSignsFestival.LOGGER.warning(var15.getMessage());
         }
      }

      public SevenSignsFestival.DarknessFestival getFestivalInstance(int oracle, int festivalIdValue) {
         if (!SevenSignsFestival.this._festivalInitialized) {
            return null;
         } else {
            int festivalId = festivalIdValue + (oracle == 1 ? 10 : 20);
            return this._festivalInstances.get(festivalId);
         }
      }
   }

   private static class FestivalSpawn {
      protected final int _x;
      protected final int _y;
      protected final int _z;
      protected final int _heading;
      protected final int _npcId;

      protected FestivalSpawn(int x, int y, int z, int heading) {
         this._x = x;
         this._y = y;
         this._z = z;
         this._heading = heading < 0 ? Rnd.get(65536) : heading;
         this._npcId = -1;
      }

      protected FestivalSpawn(int[] spawnData) {
         this._x = spawnData[0];
         this._y = spawnData[1];
         this._z = spawnData[2];
         this._heading = spawnData[3] < 0 ? Rnd.get(65536) : spawnData[3];
         if (spawnData.length > 4) {
            this._npcId = spawnData[4];
         } else {
            this._npcId = -1;
         }
      }
   }

   private static class SingletonHolder {
      protected static final SevenSignsFestival INSTANCE = new SevenSignsFestival();
   }
}
