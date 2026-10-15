package org.l2jmobius.gameserver.data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.options.Augmentation;
import org.l2jmobius.gameserver.mechanics.options.Options;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class AugmentationData {
   private static final Logger LOGGER = Logger.getLogger(AugmentationData.class.getName());
   private static final int STAT_BLOCKSIZE = 3640;
   private static final int STAT_SUBBLOCKSIZE = 91;
   public static final int MIN_SKILL_ID = 14560;
   private static final int BLUE_START = 14561;
   private static final int SKILLS_BLOCKSIZE = 178;
   private static final int BASESTAT_STR = 16341;
   private static final int BASESTAT_MEN = 16344;
   private static final int ACC_START = 16669;
   private static final int ACC_BLOCKS_NUM = 10;
   private static final int ACC_STAT_SUBBLOCKSIZE = 21;
   private static final int ACC_RING_START = 16669;
   private static final int ACC_RING_SKILLS = 18;
   private static final int ACC_RING_BLOCKSIZE = 102;
   private static final int ACC_RING_END = 17688;
   private static final int ACC_EAR_START = 17689;
   private static final int ACC_EAR_SKILLS = 18;
   private static final int ACC_EAR_BLOCKSIZE = 102;
   private static final int ACC_EAR_END = 18708;
   private static final int ACC_NECK_START = 18709;
   private static final int ACC_NECK_SKILLS = 24;
   private static final int ACC_NECK_BLOCKSIZE = 108;
   private final List<List<Integer>> _blueSkills = new ArrayList<>();
   private final List<List<Integer>> _purpleSkills = new ArrayList<>();
   private final List<List<Integer>> _redSkills = new ArrayList<>();
   private final List<AugmentationData.AugmentationChance> _augmentationChances = new ArrayList<>();
   private final List<AugmentationData.augmentationChanceAcc> _augmentationChancesAcc = new ArrayList<>();
   private final Map<Integer, SkillHolder> _allSkills = new HashMap<>();

   protected AugmentationData() {
      for (int i = 0; i < 10; i++) {
         this._blueSkills.add(new ArrayList<>());
         this._purpleSkills.add(new ArrayList<>());
         this._redSkills.add(new ArrayList<>());
      }

      this.load();
      if (!PlayerConfig.RETAIL_LIKE_AUGMENTATION) {
         for (int i = 0; i < 10; i++) {
            LOGGER.info(
               this.getClass().getSimpleName()
                  + ": Loaded "
                  + this._blueSkills.get(i).size()
                  + " blue, "
                  + this._purpleSkills.get(i).size()
                  + " purple and "
                  + this._redSkills.get(i).size()
                  + " red skills for lifeStoneLevel "
                  + i
            );
         }
      } else {
         LOGGER.log(Level.INFO, this.getClass().getSimpleName() + ": Loaded " + this._augmentationChances.size() + " augmentations.");
         LOGGER.log(Level.INFO, this.getClass().getSimpleName() + ": Loaded " + this._augmentationChancesAcc.size() + " accessory augmentations.");
      }
   }

   private void load() {
      if (!PlayerConfig.RETAIL_LIKE_AUGMENTATION) {
         try {
            int badAugmantData = 0;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            File file = new File(ServerConfig.DATAPACK_ROOT + "/data/stats/augmentation/augmentation_skillmap.xml");
            if (!file.exists()) {
               LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": ERROR The augmentation skillmap file is missing.");
               return;
            }

            Document document = factory.newDocumentBuilder().parse(file);

            for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling()) {
               if ("list".equalsIgnoreCase(n.getNodeName())) {
                  for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling()) {
                     if ("augmentation".equalsIgnoreCase(d.getNodeName())) {
                        NamedNodeMap attrs = d.getAttributes();
                        int skillId = 0;
                        int augmentationId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
                        int skillLevel = 0;
                        String type = "blue";

                        for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling()) {
                           if ("skillId".equalsIgnoreCase(cd.getNodeName())) {
                              attrs = cd.getAttributes();
                              skillId = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
                           } else if ("skillLevel".equalsIgnoreCase(cd.getNodeName())) {
                              attrs = cd.getAttributes();
                              skillLevel = Integer.parseInt(attrs.getNamedItem("val").getNodeValue());
                           } else if ("type".equalsIgnoreCase(cd.getNodeName())) {
                              attrs = cd.getAttributes();
                              type = attrs.getNamedItem("val").getNodeValue();
                           }
                        }

                        if (skillId == 0) {
                           badAugmantData++;
                        } else if (skillLevel == 0) {
                           badAugmantData++;
                        } else {
                           int k = (augmentationId - 14561) / 178;
                           if (type.equalsIgnoreCase("blue")) {
                              this._blueSkills.get(k).add(augmentationId);
                           } else if (type.equalsIgnoreCase("purple")) {
                              this._purpleSkills.get(k).add(augmentationId);
                           } else {
                              this._redSkills.get(k).add(augmentationId);
                           }

                           this._allSkills.put(augmentationId, new SkillHolder(skillId, skillLevel));
                        }
                     }
                  }
               }
            }

            if (badAugmantData != 0) {
               LOGGER.info(this.getClass().getSimpleName() + ": " + badAugmantData + " negative effect skill(s) were skipped.");
            }
         } catch (Exception var19) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": ERROR parsing augmentation_skillmap.xml.", (Throwable)var19);
            return;
         }
      } else {
         DocumentBuilderFactory factoryx = DocumentBuilderFactory.newInstance();
         factoryx.setValidating(false);
         factoryx.setIgnoringComments(true);
         File filex = new File(ServerConfig.DATAPACK_ROOT + "/data/stats/augmentation/retailchances.xml");
         if (!filex.exists()) {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": ERROR The retailchances.xml data file is missing.");
            return;
         }

         Document document = null;

         try {
            document = factoryx.newDocumentBuilder().parse(filex);
         } catch (Exception var18) {
            LOGGER.warning("Problem with AugmentationData: " + var18.getMessage());
            return;
         }

         String weaponType = null;
         int stoneId = 0;
         int variationId = 0;
         int categoryChance = 0;
         int augmentId = 0;
         float augmentChance = 0.0F;

         for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeName().equals("list")) {
               NamedNodeMap nodeAttributes = null;

               for (Node nx = node.getFirstChild(); nx != null; nx = nx.getNextSibling()) {
                  if (nx.getNodeName().equals("weapon")) {
                     nodeAttributes = nx.getAttributes();
                     weaponType = nodeAttributes.getNamedItem("type").getNodeValue();

                     for (Node c = nx.getFirstChild(); c != null; c = c.getNextSibling()) {
                        if (c.getNodeName().equals("stone")) {
                           nodeAttributes = c.getAttributes();
                           stoneId = Integer.parseInt(nodeAttributes.getNamedItem("id").getNodeValue());

                           for (Node v = c.getFirstChild(); v != null; v = v.getNextSibling()) {
                              if (v.getNodeName().equals("variation")) {
                                 nodeAttributes = v.getAttributes();
                                 variationId = Integer.parseInt(nodeAttributes.getNamedItem("id").getNodeValue());

                                 for (Node j = v.getFirstChild(); j != null; j = j.getNextSibling()) {
                                    if (j.getNodeName().equals("category")) {
                                       nodeAttributes = j.getAttributes();
                                       categoryChance = Integer.parseInt(nodeAttributes.getNamedItem("probability").getNodeValue());

                                       for (Node e = j.getFirstChild(); e != null; e = e.getNextSibling()) {
                                          if (e.getNodeName().equals("augment")) {
                                             nodeAttributes = e.getAttributes();
                                             augmentId = Integer.parseInt(nodeAttributes.getNamedItem("id").getNodeValue());
                                             augmentChance = Float.parseFloat(nodeAttributes.getNamedItem("chance").getNodeValue());
                                             this._augmentationChances
                                                .add(
                                                   new AugmentationData.AugmentationChance(
                                                      weaponType, stoneId, variationId, categoryChance, augmentId, augmentChance
                                                   )
                                                );
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (PlayerConfig.RETAIL_LIKE_AUGMENTATION_ACCESSORY) {
         DocumentBuilderFactory factory3 = DocumentBuilderFactory.newInstance();
         factory3.setValidating(false);
         factory3.setIgnoringComments(true);
         File aFile3 = new File(ServerConfig.DATAPACK_ROOT + "/data/stats/augmentation/retailchances_accessory.xml");
         if (aFile3.exists()) {
            Document aDoc = null;

            try {
               aDoc = factory3.newDocumentBuilder().parse(aFile3);
            } catch (Exception var17) {
               LOGGER.warning("Problem with AugmentationData: " + var17.getMessage());
               return;
            }

            String aWeaponType = null;
            int aStoneId = 0;
            int aVariationId = 0;
            int aCategoryChance = 0;
            int aAugmentId = 0;
            float aAugmentChance = 0.0F;

            for (Node l = aDoc.getFirstChild(); l != null; l = l.getNextSibling()) {
               if (l.getNodeName().equals("list")) {
                  NamedNodeMap aNodeAttributes = null;

                  for (Node nxx = l.getFirstChild(); nxx != null; nxx = nxx.getNextSibling()) {
                     if (nxx.getNodeName().equals("weapon")) {
                        aNodeAttributes = nxx.getAttributes();
                        aWeaponType = aNodeAttributes.getNamedItem("type").getNodeValue();

                        for (Node cx = nxx.getFirstChild(); cx != null; cx = cx.getNextSibling()) {
                           if (cx.getNodeName().equals("stone")) {
                              aNodeAttributes = cx.getAttributes();
                              aStoneId = Integer.parseInt(aNodeAttributes.getNamedItem("id").getNodeValue());

                              for (Node vx = cx.getFirstChild(); vx != null; vx = vx.getNextSibling()) {
                                 if (vx.getNodeName().equals("variation")) {
                                    aNodeAttributes = vx.getAttributes();
                                    aVariationId = Integer.parseInt(aNodeAttributes.getNamedItem("id").getNodeValue());

                                    for (Node jx = vx.getFirstChild(); jx != null; jx = jx.getNextSibling()) {
                                       if (jx.getNodeName().equals("category")) {
                                          aNodeAttributes = jx.getAttributes();
                                          aCategoryChance = Integer.parseInt(aNodeAttributes.getNamedItem("probability").getNodeValue());

                                          for (Node ex = jx.getFirstChild(); ex != null; ex = ex.getNextSibling()) {
                                             if (ex.getNodeName().equals("augment")) {
                                                aNodeAttributes = ex.getAttributes();
                                                aAugmentId = Integer.parseInt(aNodeAttributes.getNamedItem("id").getNodeValue());
                                                aAugmentChance = Float.parseFloat(aNodeAttributes.getNamedItem("chance").getNodeValue());
                                                this._augmentationChancesAcc
                                                   .add(
                                                      new AugmentationData.augmentationChanceAcc(
                                                         aWeaponType, aStoneId, aVariationId, aCategoryChance, aAugmentId, aAugmentChance
                                                      )
                                                   );
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         } else {
            LOGGER.log(Level.WARNING, this.getClass().getSimpleName() + ": ERROR The retailchances_accessory.xml data file is missing.");
         }
      }
   }

   public Augmentation generateRandomAugmentation(int lifeStoneLevel, int lifeStoneGrade, BodyPart bodyPart, int lifeStoneId, Item targetItem) {
      switch (bodyPart) {
         case LR_FINGER:
         case LR_EAR:
         case NECK:
            return this.generateRandomAccessoryAugmentation(lifeStoneLevel, bodyPart, lifeStoneId);
         default:
            return this.generateRandomWeaponAugmentation(lifeStoneLevel, lifeStoneGrade, lifeStoneId, targetItem);
      }
   }

   private Augmentation generateRandomWeaponAugmentation(int level, int lifeStoneGrade, int lifeStoneId, Item item) {
      int stat12 = 0;
      int stat34 = 0;
      if (PlayerConfig.RETAIL_LIKE_AUGMENTATION) {
         if (item.getTemplate().isMagicWeapon()) {
            List<AugmentationData.AugmentationChance> selectedChances12 = new ArrayList<>();
            List<AugmentationData.AugmentationChance> selectedChances34 = new ArrayList<>();

            for (AugmentationData.AugmentationChance ac : this._augmentationChances) {
               if (ac.getWeaponType().equals("mage") && ac.getStoneId() == lifeStoneId) {
                  if (ac.getVariationId() == 1) {
                     selectedChances12.add(ac);
                  } else {
                     selectedChances34.add(ac);
                  }
               }
            }

            int r = Rnd.get(10000);
            float s = 10000.0F;

            for (AugmentationData.AugmentationChance acx : selectedChances12) {
               if (s > r) {
                  s -= acx.getAugmentChance() * 100.0F;
                  stat12 = acx.getAugmentId();
               }
            }

            int[] gradeChance = null;

            gradeChance = switch (lifeStoneGrade) {
               case 0 -> PlayerConfig.RETAIL_LIKE_AUGMENTATION_NG_CHANCE;
               case 1 -> PlayerConfig.RETAIL_LIKE_AUGMENTATION_MID_CHANCE;
               case 2 -> PlayerConfig.RETAIL_LIKE_AUGMENTATION_HIGH_CHANCE;
               case 3 -> PlayerConfig.RETAIL_LIKE_AUGMENTATION_TOP_CHANCE;
               default -> PlayerConfig.RETAIL_LIKE_AUGMENTATION_NG_CHANCE;
            };
            int c = Rnd.get(100);
            byte var43;
            if (c < gradeChance[0]) {
               var43 = 55;
            } else if (c < gradeChance[0] + gradeChance[1]) {
               var43 = 35;
            } else if (c < gradeChance[0] + gradeChance[1] + gradeChance[2]) {
               var43 = 7;
            } else {
               var43 = 3;
            }

            List<AugmentationData.AugmentationChance> selectedChances34final = new ArrayList<>();

            for (AugmentationData.AugmentationChance acxx : selectedChances34) {
               if (acxx.getCategoryChance() == var43) {
                  selectedChances34final.add(acxx);
               }
            }

            r = Rnd.get(10000);
            s = 10000.0F;

            for (AugmentationData.AugmentationChance acxxx : selectedChances34final) {
               if (s > r) {
                  s -= acxxx.getAugmentChance() * 100.0F;
                  stat34 = acxxx.getAugmentId();
               }
            }
         } else {
            List<AugmentationData.AugmentationChance> selectedChances12 = new ArrayList<>();
            List<AugmentationData.AugmentationChance> selectedChances34 = new ArrayList<>();

            for (AugmentationData.AugmentationChance acxxxx : this._augmentationChances) {
               if (acxxxx.getWeaponType().equals("warrior") && acxxxx.getStoneId() == lifeStoneId) {
                  if (acxxxx.getVariationId() == 1) {
                     selectedChances12.add(acxxxx);
                  } else {
                     selectedChances34.add(acxxxx);
                  }
               }
            }

            int r = Rnd.get(10000);
            float s = 10000.0F;

            for (AugmentationData.AugmentationChance acxxxxx : selectedChances12) {
               if (s > r) {
                  s -= acxxxxx.getAugmentChance() * 100.0F;
                  stat12 = acxxxxx.getAugmentId();
               }
            }

            int[] gradeChance = null;

            gradeChance = switch (lifeStoneGrade) {
               case 0 -> PlayerConfig.RETAIL_LIKE_AUGMENTATION_NG_CHANCE;
               case 1 -> PlayerConfig.RETAIL_LIKE_AUGMENTATION_MID_CHANCE;
               case 2 -> PlayerConfig.RETAIL_LIKE_AUGMENTATION_HIGH_CHANCE;
               case 3 -> PlayerConfig.RETAIL_LIKE_AUGMENTATION_TOP_CHANCE;
               default -> PlayerConfig.RETAIL_LIKE_AUGMENTATION_NG_CHANCE;
            };
            int c = Rnd.get(100);
            byte var46;
            if (c < gradeChance[0]) {
               var46 = 55;
            } else if (c < gradeChance[0] + gradeChance[1]) {
               var46 = 35;
            } else if (c < gradeChance[0] + gradeChance[1] + gradeChance[2]) {
               var46 = 7;
            } else {
               var46 = 3;
            }

            List<AugmentationData.AugmentationChance> selectedChances34final = new ArrayList<>();

            for (AugmentationData.AugmentationChance acxxxxxx : selectedChances34) {
               if (acxxxxxx.getCategoryChance() == var46) {
                  selectedChances34final.add(acxxxxxx);
               }
            }

            r = Rnd.get(10000);
            s = 10000.0F;

            for (AugmentationData.AugmentationChance acxxxxxxx : selectedChances34final) {
               if (s > r) {
                  s -= acxxxxxxx.getAugmentChance() * 100.0F;
                  stat34 = acxxxxxxx.getAugmentId();
               }
            }
         }

         return new Augmentation((stat34 << 16) + stat12);
      } else {
         boolean generateSkill = false;
         boolean generateGlow = false;
         int lifeStoneLevel = Math.min(level, 9);
         switch (lifeStoneGrade) {
            case 0:
               if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_NG_SKILL_CHANCE) {
                  generateSkill = true;
               }

               if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_NG_GLOW_CHANCE) {
                  generateGlow = true;
               }
               break;
            case 1:
               if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_MID_SKILL_CHANCE) {
                  generateSkill = true;
               }

               if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_MID_GLOW_CHANCE) {
                  generateGlow = true;
               }
               break;
            case 2:
               if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_HIGH_SKILL_CHANCE) {
                  generateSkill = true;
               }

               if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_HIGH_GLOW_CHANCE) {
                  generateGlow = true;
               }
               break;
            case 3:
               if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_TOP_SKILL_CHANCE) {
                  generateSkill = true;
               }

               if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_TOP_GLOW_CHANCE) {
                  generateGlow = true;
               }
               break;
            case 4:
               if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_ACC_SKILL_CHANCE) {
                  generateSkill = true;
               }
         }

         if (!generateSkill && Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_BASESTAT_CHANCE) {
            stat34 = Rnd.get(16341, 16344);
         }

         int resultColor = Rnd.get(0, 100);
         byte var27;
         if (stat34 == 0 && !generateSkill) {
            if (resultColor <= 15 * lifeStoneGrade + 40) {
               var27 = 1;
            } else {
               var27 = 0;
            }
         } else if (resultColor > 10 * lifeStoneGrade + 5 && stat34 == 0) {
            if (resultColor <= 10 * lifeStoneGrade + 10) {
               var27 = 1;
            } else {
               var27 = 2;
            }
         } else {
            var27 = 3;
         }

         if (generateSkill) {
            switch (var27) {
               case 1:
                  stat34 = this._blueSkills.get(lifeStoneLevel).get(Rnd.get(0, this._blueSkills.get(lifeStoneLevel).size() - 1));
                  break;
               case 2:
                  stat34 = this._purpleSkills.get(lifeStoneLevel).get(Rnd.get(0, this._purpleSkills.get(lifeStoneLevel).size() - 1));
                  break;
               case 3:
                  stat34 = this._redSkills.get(lifeStoneLevel).get(Rnd.get(0, this._redSkills.get(lifeStoneLevel).size() - 1));
            }
         }

         int offset;
         if (stat34 == 0) {
            int temp = Rnd.get(2, 3);
            int colorOffset = var27 * 910 + temp * 3640 + 1;
            offset = lifeStoneLevel * 91 + colorOffset;
            stat34 = Rnd.get(offset, offset + 91 - 1);
            if (generateGlow && lifeStoneGrade >= 2) {
               offset = lifeStoneLevel * 91 + (temp - 2) * 3640 + lifeStoneGrade * 910 + 1;
            } else {
               offset = lifeStoneLevel * 91 + (temp - 2) * 3640 + Rnd.get(0, 1) * 910 + 1;
            }
         } else if (!generateGlow) {
            offset = lifeStoneLevel * 91 + Rnd.get(0, 1) * 3640 + 1;
         } else {
            offset = lifeStoneLevel * 91 + Rnd.get(0, 1) * 3640 + (lifeStoneGrade + var27) / 2 * 910 + 1;
         }

         stat12 = Rnd.get(offset, offset + 91 - 1);
         return new Augmentation((stat34 << 16) + stat12);
      }
   }

   private Augmentation generateRandomAccessoryAugmentation(int level, BodyPart bodyPart, int lifeStoneId) {
      int stat12 = 0;
      int stat34 = 0;
      if (PlayerConfig.RETAIL_LIKE_AUGMENTATION_ACCESSORY) {
         List<AugmentationData.augmentationChanceAcc> selectedChances12 = new ArrayList<>();
         List<AugmentationData.augmentationChanceAcc> selectedChances34 = new ArrayList<>();

         for (AugmentationData.augmentationChanceAcc ac : this._augmentationChancesAcc) {
            if (ac.getWeaponType().equals("warrior") && ac.getStoneId() == lifeStoneId) {
               if (ac.getVariationId() == 1) {
                  selectedChances12.add(ac);
               } else {
                  selectedChances34.add(ac);
               }
            }
         }

         int r = Rnd.get(10000);
         float s = 10000.0F;

         for (AugmentationData.augmentationChanceAcc acx : selectedChances12) {
            if (s > r) {
               s -= acx.getAugmentChance() * 100.0F;
               stat12 = acx.getAugmentId();
            }
         }

         int c = Rnd.get(100);
         byte var29;
         if (c < 55) {
            var29 = 55;
         } else if (c < 90) {
            var29 = 35;
         } else if (c < 99) {
            var29 = 9;
         } else {
            var29 = 1;
         }

         List<AugmentationData.augmentationChanceAcc> selectedChances34final = new ArrayList<>();

         for (AugmentationData.augmentationChanceAcc acxx : selectedChances34) {
            if (acxx.getCategoryChance() == var29) {
               selectedChances34final.add(acxx);
            }
         }

         r = Rnd.get(10000);
         s = 10000.0F;

         for (AugmentationData.augmentationChanceAcc acxxx : selectedChances34final) {
            if (s > r) {
               s -= acxxx.getAugmentChance() * 100.0F;
               stat34 = acxxx.getAugmentId();
            }
         }

         return new Augmentation((stat34 << 16) + stat12);
      } else {
         int lifeStoneLevel = Math.min(level, 9);
         int base = 0;
         int skillsLength = 0;
         byte var20;
         switch (bodyPart) {
            case LR_FINGER:
               base = 16669 + 102 * lifeStoneLevel;
               var20 = 18;
               break;
            case LR_EAR:
               base = 17689 + 102 * lifeStoneLevel;
               var20 = 18;
               break;
            case NECK:
               base = 18709 + 108 * lifeStoneLevel;
               var20 = 24;
               break;
            default:
               return null;
         }

         int resultColor = Rnd.get(0, 3);
         stat12 = Rnd.get(21);
         Options op = null;
         if (Rnd.get(1, 100) <= PlayerConfig.AUGMENTATION_ACC_SKILL_CHANCE) {
            stat34 = base + Rnd.get((int)var20);
            op = OptionData.getInstance().getOptions(stat34);
         }

         if (op == null || !op.hasActiveSkill() && !op.hasPassiveSkill() && !op.hasActivationSkills()) {
            stat34 = (stat12 + 1 + Rnd.get(20)) % 21;
            stat34 = base + var20 + 21 * resultColor + stat34;
         }

         stat12 = base + var20 + 21 * resultColor + stat12;
         return new Augmentation((stat34 << 16) + stat12);
      }
   }

   public static AugmentationData getInstance() {
      return AugmentationData.SingletonHolder.INSTANCE;
   }

   public class AugmentationChance {
      private final String _weaponType;
      private final int _stoneId;
      private final int _variationId;
      private final int _categoryChance;
      private final int _augmentId;
      private final float _augmentChance;

      public AugmentationChance(String weaponType, int stoneId, int variationId, int categoryChance, int augmentId, float augmentChance) {
         Objects.requireNonNull(AugmentationData.this);
         super();
         this._weaponType = weaponType;
         this._stoneId = stoneId;
         this._variationId = variationId;
         this._categoryChance = categoryChance;
         this._augmentId = augmentId;
         this._augmentChance = augmentChance;
      }

      public String getWeaponType() {
         return this._weaponType;
      }

      public int getStoneId() {
         return this._stoneId;
      }

      public int getVariationId() {
         return this._variationId;
      }

      public int getCategoryChance() {
         return this._categoryChance;
      }

      public int getAugmentId() {
         return this._augmentId;
      }

      public float getAugmentChance() {
         return this._augmentChance;
      }
   }

   private static class SingletonHolder {
      protected static final AugmentationData INSTANCE = new AugmentationData();
   }

   public class augmentationChanceAcc {
      private final String _weaponType;
      private final int _stoneId;
      private final int _variationId;
      private final int _categoryChance;
      private final int _augmentId;
      private final float _augmentChance;

      public augmentationChanceAcc(String weaponType, int stoneId, int variationId, int categoryChance, int augmentId, float augmentChance) {
         Objects.requireNonNull(AugmentationData.this);
         super();
         this._weaponType = weaponType;
         this._stoneId = stoneId;
         this._variationId = variationId;
         this._categoryChance = categoryChance;
         this._augmentId = augmentId;
         this._augmentChance = augmentChance;
      }

      public String getWeaponType() {
         return this._weaponType;
      }

      public int getStoneId() {
         return this._stoneId;
      }

      public int getVariationId() {
         return this._variationId;
      }

      public int getCategoryChance() {
         return this._categoryChance;
      }

      public int getAugmentId() {
         return this._augmentId;
      }

      public float getAugmentChance() {
         return this._augmentChance;
      }
   }
}
