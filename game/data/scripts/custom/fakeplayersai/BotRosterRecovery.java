package custom.fakeplayersai;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.World;

/** Recover only offline Cardinal/Bishop bot-account owners lost from registry.
 * Never adopts a character merely because its name matches. No database writes. */
final class BotRosterRecovery
{
    private static final Logger LOGGER = Logger.getLogger(BotRosterRecovery.class.getName());
    record Candidate(int objectId, String name) { }
    private BotRosterRecovery() { }

    static List<Candidate> findCardinal(BotClassPreset preset)
    {
        if (preset == null || preset.getClassId() != 97) return List.of();
        try (Connection con = DatabaseFactory.getConnection())
        {
            return readCandidates(con, BotManager.getSettings().getAccountPrefix(), id -> World.getPlayer(id) != null);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "FakePlayersAI: Cardinal orphan lookup failed; no foreign character adopted", e);
            return List.of();
        }
    }

    static boolean generatedBotAccount(String account, String prefix)
    {
        if (account == null || prefix == null || prefix.isBlank() || !account.startsWith(prefix) || account.length() != prefix.length() + 6) return false;
        final String suffix = account.substring(prefix.length());
        return suffix.charAt(0) >= '1' && suffix.charAt(0) <= '9' && suffix.chars().allMatch(c -> c >= '0' && c <= '9');
    }

    static List<Candidate> readCandidates(Connection con, String prefix, IntPredicate loaded) throws Exception
    {
        final List<Candidate> result = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT c.charId,c.char_name,c.account_name,c.classid,c.base_class,c.online,c.deletetime,a.login AS registered_account FROM characters c LEFT JOIN accounts a ON a.login=c.account_name WHERE c.char_name IN (?,?) AND c.classid=97 AND c.base_class=97 AND c.online=0 AND c.deletetime=0 AND a.login IS NULL ORDER BY c.charId"))
        {
            ps.setString(1, "Cardinal"); ps.setString(2, "Bishop");
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    final int id = rs.getInt("charId");
                    final String name = rs.getString("char_name");
                    if (id <= 0 || name == null || !(name.equalsIgnoreCase("Cardinal") || name.equalsIgnoreCase("Bishop"))
                        || rs.getInt("classid") != 97 || rs.getInt("base_class") != 97 || rs.getInt("online") != 0 || rs.getLong("deletetime") != 0
                        || rs.getString("registered_account") != null || !generatedBotAccount(rs.getString("account_name"), prefix) || loaded.test(id)) continue;
                    result.add(new Candidate(id, name));
                }
            }
        }
        return result;
    }
}
