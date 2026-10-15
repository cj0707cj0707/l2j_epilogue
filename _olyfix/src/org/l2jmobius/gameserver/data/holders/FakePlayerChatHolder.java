package org.l2jmobius.gameserver.data.holders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FakePlayerChatHolder {
   private final String _fpcName;
   private final String _searchMethod;
   private final List<String> _searchText;
   private final List<String> _answers;

   public FakePlayerChatHolder(String fpcName, String searchMethod, String searchText, String answers) {
      this._fpcName = fpcName;
      this._searchMethod = searchMethod;
      this._searchText = new ArrayList<>(Arrays.asList(searchText.split(";")));
      this._answers = new ArrayList<>(Arrays.asList(answers.split(";")));
   }

   public String getFpcName() {
      return this._fpcName;
   }

   public String getSearchMethod() {
      return this._searchMethod;
   }

   public List<String> getSearchText() {
      return this._searchText;
   }

   public List<String> getAnswers() {
      return this._answers;
   }
}
