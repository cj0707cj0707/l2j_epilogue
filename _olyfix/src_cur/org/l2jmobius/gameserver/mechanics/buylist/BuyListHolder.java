package org.l2jmobius.gameserver.mechanics.buylist;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class BuyListHolder {
   private final int _listId;
   private final Map<Integer, Product> _products = new LinkedHashMap<>();
   private Set<Integer> _allowedNpcs = null;

   public BuyListHolder(int listId) {
      this._listId = listId;
   }

   public int getListId() {
      return this._listId;
   }

   public Collection<Product> getProducts() {
      return this._products.values();
   }

   public Product getProductByItemId(int itemId) {
      return this._products.get(itemId);
   }

   public void addProduct(Product product) {
      this._products.put(product.getItemId(), product);
   }

   public void addAllowedNpc(int npcId) {
      if (this._allowedNpcs == null) {
         this._allowedNpcs = new HashSet<>();
      }

      this._allowedNpcs.add(npcId);
   }

   public boolean isNpcAllowed(int npcId) {
      return this._allowedNpcs != null && this._allowedNpcs.contains(npcId);
   }

   public Set<Integer> getNpcsAllowed() {
      return this._allowedNpcs;
   }
}
