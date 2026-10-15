package org.l2jmobius.commons.ui;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

public class LineLimitListener implements DocumentListener {
   private final boolean _removeFromStart;
   private final int _maxLines;

   public LineLimitListener(int maxLines) {
      this(maxLines, true);
   }

   public LineLimitListener(int maxLines, boolean removeFromStart) {
      this._removeFromStart = removeFromStart;
      this._maxLines = maxLines;
   }

   public int getLimitLines() {
      return this._maxLines;
   }

   private void removeLines(DocumentEvent event) {
      Document document = event.getDocument();
      Element root = document.getDefaultRootElement();

      while (root.getElementCount() > this._maxLines) {
         if (this._removeFromStart) {
            this.removeFromStart(document, root);
         } else {
            this.removeFromEnd(document, root);
         }
      }
   }

   private void removeFromStart(Document document, Element root) {
      Element line = root.getElement(0);
      int end = line.getEndOffset();

      try {
         document.remove(0, end);
      } catch (BadLocationException var6) {
         System.out.println(var6);
      }
   }

   private void removeFromEnd(Document document, Element root) {
      Element line = root.getElement(root.getElementCount() - 1);
      int start = line.getStartOffset();
      int end = line.getEndOffset();

      try {
         document.remove(start - 1, end - start);
      } catch (BadLocationException var7) {
         System.out.println(var7);
      }
   }

   @Override
   public void insertUpdate(DocumentEvent event) {
      SwingUtilities.invokeLater(() -> this.removeLines(event));
   }

   @Override
   public void removeUpdate(DocumentEvent event) {
   }

   @Override
   public void changedUpdate(DocumentEvent event) {
   }
}
