package org.l2jmobius.gameserver.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.actor.instance.Merchant;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class MerchantPriceConfigTable {
   private static final Logger LOGGER = Logger.getLogger(MerchantPriceConfigTable.class.getName());
   private static final String MPCS_FILE = "MerchantPriceConfig.xml";
   private final Map<Integer, MerchantPriceConfigTable.MerchantPriceConfig> _mpcs = new HashMap<>();
   private MerchantPriceConfigTable.MerchantPriceConfig _defaultMpc;

   public static MerchantPriceConfigTable getInstance() {
      return MerchantPriceConfigTable.SingletonHolder.INSTANCE;
   }

   public MerchantPriceConfigTable.MerchantPriceConfig getMerchantPriceConfig(Merchant npc) {
      for (MerchantPriceConfigTable.MerchantPriceConfig mpc : this._mpcs.values()) {
         if (ZoneManager.getInstance().getRegion(npc).getZones().containsKey(mpc.getZoneId())) {
            return mpc;
         }
      }

      return this._defaultMpc;
   }

   public MerchantPriceConfigTable.MerchantPriceConfig getMerchantPriceConfig(int id) {
      return this._mpcs.get(id);
   }

   public void loadXML() throws SAXException, IOException, ParserConfigurationException {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);
      factory.setIgnoringComments(true);
      File file = new File(ServerConfig.DATAPACK_ROOT + "/data/MerchantPriceConfig.xml");
      if (file.exists()) {
         Document document = factory.newDocumentBuilder().parse(file);
         Node n = document.getDocumentElement();
         Node dpcNode = n.getAttributes().getNamedItem("defaultPriceConfig");
         if (dpcNode == null) {
            throw new IllegalStateException("merchantPriceConfig must define an 'defaultPriceConfig'");
         }

         int defaultPriceConfigId = Integer.parseInt(dpcNode.getNodeValue());

         for (Node var9 = n.getFirstChild(); var9 != null; var9 = var9.getNextSibling()) {
            MerchantPriceConfigTable.MerchantPriceConfig mpc = this.parseMerchantPriceConfig(var9);
            if (mpc != null) {
               this._mpcs.put(mpc.getId(), mpc);
            }
         }

         MerchantPriceConfigTable.MerchantPriceConfig defaultMpc = this.getMerchantPriceConfig(defaultPriceConfigId);
         if (defaultMpc == null) {
            throw new IllegalStateException("'defaultPriceConfig' points to an non-loaded priceConfig");
         }

         this._defaultMpc = defaultMpc;
      }
   }

   private MerchantPriceConfigTable.MerchantPriceConfig parseMerchantPriceConfig(Node n) {
      if (n.getNodeName().equals("priceConfig")) {
         int castleId = -1;
         int zoneId = -1;
         Node node = n.getAttributes().getNamedItem("id");
         if (node == null) {
            throw new IllegalStateException("Must define the priceConfig 'id'");
         } else {
            int id = Integer.parseInt(node.getNodeValue());
            node = n.getAttributes().getNamedItem("name");
            if (node == null) {
               throw new IllegalStateException("Must define the priceConfig 'name'");
            } else {
               String name = node.getNodeValue();
               node = n.getAttributes().getNamedItem("baseTax");
               if (node == null) {
                  throw new IllegalStateException("Must define the priceConfig 'baseTax'");
               } else {
                  int baseTax = Integer.parseInt(node.getNodeValue());
                  node = n.getAttributes().getNamedItem("castleId");
                  if (node != null) {
                     castleId = Integer.parseInt(node.getNodeValue());
                  }

                  node = n.getAttributes().getNamedItem("zoneId");
                  if (node != null) {
                     zoneId = Integer.parseInt(node.getNodeValue());
                  }

                  return new MerchantPriceConfigTable.MerchantPriceConfig(id, name, baseTax, castleId, zoneId);
               }
            }
         }
      } else {
         return null;
      }
   }

   public void loadInstances() {
      try {
         this.loadXML();
         LOGGER.info(this.getClass().getSimpleName() + ": Loaded " + this._mpcs.size() + " merchant price configs.");
      } catch (Exception var2) {
         LOGGER.log(Level.SEVERE, this.getClass().getSimpleName() + ": Failed loading MerchantPriceConfigTable. Reason: " + var2.getMessage(), (Throwable)var2);
      }
   }

   public void updateReferences() {
      for (MerchantPriceConfigTable.MerchantPriceConfig mpc : this._mpcs.values()) {
         mpc.updateReferences();
      }
   }

   public static class MerchantPriceConfig {
      private final int _id;
      private final String _name;
      private final int _baseTax;
      private final int _castleId;
      private Castle _castle;
      private final int _zoneId;

      public MerchantPriceConfig(int id, String name, int baseTax, int castleId, int zoneId) {
         this._id = id;
         this._name = name;
         this._baseTax = baseTax;
         this._castleId = castleId;
         this._zoneId = zoneId;
      }

      public int getId() {
         return this._id;
      }

      public String getName() {
         return this._name;
      }

      public int getBaseTax() {
         return this._baseTax;
      }

      public double getBaseTaxRate() {
         return this._baseTax / 100.0;
      }

      public Castle getCastle() {
         return this._castle;
      }

      public int getZoneId() {
         return this._zoneId;
      }

      public boolean hasCastle() {
         return this._castle != null;
      }

      public double getCastleTaxRate() {
         return this.hasCastle() ? this._castle.getTaxRate() : 0.0;
      }

      public int getTotalTax() {
         return this.hasCastle() ? this._castle.getTaxPercent() + this._baseTax : this._baseTax;
      }

      public double getTotalTaxRate() {
         return this.getTotalTax() / 100.0;
      }

      public void updateReferences() {
         if (this._castleId > 0) {
            this._castle = CastleManager.getInstance().getCastleById(this._castleId);
         }
      }
   }

   private static class SingletonHolder {
      protected static final MerchantPriceConfigTable INSTANCE = new MerchantPriceConfigTable();
   }
}
