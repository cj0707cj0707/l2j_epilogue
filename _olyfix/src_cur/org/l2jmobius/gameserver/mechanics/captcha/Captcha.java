package org.l2jmobius.gameserver.mechanics.captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.CaptchaManager;
import org.l2jmobius.gameserver.network.serverpackets.PledgeCrest;
import org.l2jmobius.gameserver.util.DDSConverter;

public class Captcha {
   private static final char[] CAPTCHA_TEXT_POSSIBILITIES = new char[]{
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'K', 'L', 'M', 'P', 'R', 'S', 'T', 'U', 'W', 'X', 'Y', 'Z'
   };
   private static final int CAPTCHA_WORD_LENGTH = 5;
   private static final int CAPTCHA_MIN_ID = 19000;
   private static final int CAPTCHA_MAX_ID = 25000;
   char[] captchaText;
   int captchaId;

   protected Captcha() {
   }

   public void generateCaptcha(CaptchaManager.PlayerData data, Player target) {
      int cId = generateRandomCaptchaId();
      char[] cText = generateCaptchaText();
      BufferedImage image = generateCaptcha(cText);
      byte[] imageData = DDSConverter.convertToDDS(image).array();
      data.image = image;
      PledgeCrest packet = new PledgeCrest(cId, imageData);
      target.sendPacket(packet);
      data.captchaId = cId;
      data.captchaText = String.valueOf(cText);
   }

   public synchronized Map<Integer, ImageData> createImageList() {
      Map<Integer, ImageData> imageMap = new ConcurrentHashMap<>();

      for (int i = 0; i < 1000; i++) {
         do {
            this.captchaId = generateRandomCaptchaId();
            this.captchaText = generateCaptchaText();
         } while (
            !imageMap.isEmpty() && imageMap.values().stream().anyMatch(txt -> txt.captchaText.equals(String.valueOf(this.captchaText)))
               || imageMap.values().stream().anyMatch(s -> s.captchaID == this.captchaId)
         );

         ImageData dt = new ImageData();
         dt.captchaID = this.captchaId;
         dt.captchaText = String.valueOf(this.captchaText);
         dt.image = generateCaptcha(this.captchaText);
         imageMap.put(i, dt);
      }

      return imageMap;
   }

   private static char[] generateCaptchaText() {
      char[] text = new char[5];

      for (int i = 0; i < 5; i++) {
         text[i] = CAPTCHA_TEXT_POSSIBILITIES[Rnd.get(CAPTCHA_TEXT_POSSIBILITIES.length)];
      }

      return text;
   }

   private static int generateRandomCaptchaId() {
      return Rnd.get(19000, 25000);
   }

   public static BufferedImage generateCaptcha(char[] text) {
      Color textColor = new Color(38, 213, 30);
      Color circleColor = new Color(73, 100, 151);
      Font textFont = new Font("comic sans ms", 1, 24);
      int charsToPrint = 5;
      int width = 256;
      int height = 64;
      int circlesToDraw = 8;
      float horizMargin = 20.0F;
      double rotationRange = 0.7;
      BufferedImage bufferedImage = new BufferedImage(256, 64, 1);
      Graphics2D g = (Graphics2D)bufferedImage.getGraphics();
      g.setColor(new Color(30, 31, 31));
      g.fillRect(0, 0, 256, 64);
      g.setColor(circleColor);

      for (int i = 0; i < 8; i++) {
         int circleRadius = (int)(Math.random() * 64.0 / 2.0);
         int circleX = (int)(Math.random() * 256.0 - circleRadius);
         int circleY = (int)(Math.random() * 64.0 - circleRadius);
         g.drawOval(circleX, circleY, circleRadius * 2, circleRadius * 2);
      }

      g.setColor(textColor);
      g.setFont(textFont);
      FontMetrics fontMetrics = g.getFontMetrics();
      int maxAdvance = fontMetrics.getMaxAdvance();
      int fontHeight = fontMetrics.getHeight();
      float spaceForLetters = 216.0F;
      float spacePerChar = 54.0F;

      for (int i = 0; i < 5; i++) {
         char characterToShow = text[i];
         int charWidth = fontMetrics.charWidth(characterToShow);
         int charDim = Math.max(maxAdvance, fontHeight);
         int halfCharDim = charDim / 2;
         BufferedImage charImage = new BufferedImage(charDim, charDim, 2);
         Graphics2D charGraphics = charImage.createGraphics();
         charGraphics.translate(halfCharDim, halfCharDim);
         double angle = (Math.random() - 0.5) * 0.7;
         charGraphics.transform(AffineTransform.getRotateInstance(angle));
         charGraphics.translate(-halfCharDim, -halfCharDim);
         charGraphics.setColor(textColor);
         charGraphics.setFont(textFont);
         int charX = (int)(0.5 * charDim - 0.5 * charWidth);
         charGraphics.drawString(String.valueOf(characterToShow), charX, (charDim - fontMetrics.getAscent()) / 2 + fontMetrics.getAscent());
         float x = 20.0F + 54.0F * i - charDim / 2.0F;
         int y = (64 - charDim) / 2;
         g.drawImage(charImage, (int)x, y, charDim, charDim, null, null);
         charGraphics.dispose();
      }

      g.dispose();
      return bufferedImage;
   }

   public static Captcha getInstance() {
      return Captcha.SingletonHolder.INSTANCE;
   }

   private static class SingletonHolder {
      protected static final Captcha INSTANCE = new Captcha();
   }
}
