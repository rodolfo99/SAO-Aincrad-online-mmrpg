package dev.aincrad;

import java.util.*;

/** Persistent, individually crafted objects. Recipes are controlled by root, never by clients. */
public final class Crafting {
 public record Profession(String id,String name,String stationRole,String description){}
 public record Material(String id,String name,int price,Integer sellPrice){public Material(String id,String name,int price){this(id,name,price,null);}}
 public record Product(String id,String name,String kind,String targetId,String slot,int price,boolean enabled){}
 public record Recipe(String id,String name,String professionId,String kind,String targetId,String slot,int minProfessionLevel,int colCost,Map<String,Integer> materials,int experience,int maxUpgrade,int upgradeColCost,Map<String,Integer> upgradeMaterials,Map<String,Double> bonusAttributes,Map<String,Double> upgradeAttributes){}
 public static class Rules {
  public boolean enabled=true;public int maxInventory=100,maxProfessionLevel=50,xpPerLevel=100,upgradeExperience=10,potionPrice=12;public double stationRange=4,saleMultiplier=.3;
  public List<Profession> professions;public List<Material> materials;public List<Recipe> recipes;public List<Product> shopProducts;public Map<String,Integer> initialMaterials;public Map<String,Map<String,Integer>> monsterDrops;
 }
 public static class Item {
  public List<String> specializationIds=new ArrayList<>();public String id,recipeId,kind,name,classId,gender;public int minLevel,upgrade,investedCol;public Weapons.Item weapon;public WorldData.Outfit armor;
  Item copy(){var i=new Item();i.specializationIds=new ArrayList<>(specializationIds);i.id=id;i.recipeId=recipeId;i.kind=kind;i.name=name;i.classId=classId;i.gender=gender;i.minLevel=minLevel;i.upgrade=upgrade;i.investedCol=investedCol;i.weapon=weapon;i.armor=armor;return i;}
 }
 public static class State {
  public long lastGatherEvent,gatherReadyAt;public Map<String,Integer> professionXp=new LinkedHashMap<>(),materials=new LinkedHashMap<>();public Map<String,Item> items=new LinkedHashMap<>();public Map<String,String> equipped=new LinkedHashMap<>();
  State copy(){var c=new State();c.lastGatherEvent=lastGatherEvent;c.gatherReadyAt=gatherReadyAt;c.professionXp.putAll(professionXp);c.materials.putAll(materials);items.forEach((id,item)->c.items.put(id,item.copy()));c.equipped.putAll(equipped);return c;}
 }
 static boolean id(String id){return id!=null&&id.matches("[A-Za-z0-9_-]{1,64}");}
 static boolean name(String s){return s!=null&&!s.isBlank()&&s.length()<=160;}
 public static State initial(Rules r){var s=new State();s.materials.putAll(r.initialMaterials);for(var p:r.professions)s.professionXp.put(p.id(),0);return s;}
 public static int level(Rules r,State s,String profession){return Math.min(r.maxProfessionLevel,1+s.professionXp.getOrDefault(profession,0)/r.xpPerLevel);}
 public static Recipe recipe(Rules r,String id){return r.recipes.stream().filter(v->v.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Receta inexistente"));}
 public static void validate(Rules r,WorldData.CharacterOptions c){
  if(r==null||r.maxInventory<1||r.maxInventory>200||r.maxProfessionLevel<1||r.maxProfessionLevel>100||r.xpPerLevel<1||r.xpPerLevel>10000||r.upgradeExperience<0||r.upgradeExperience>1000||r.potionPrice<1||r.potionPrice>100000||!Double.isFinite(r.stationRange)||r.stationRange<1||r.stationRange>8||!Double.isFinite(r.saleMultiplier)||r.saleMultiplier<0||r.saleMultiplier>1||r.professions==null||r.professions.isEmpty()||r.professions.size()>20||r.materials==null||r.materials.isEmpty()||r.materials.size()>50||r.recipes==null||r.recipes.size()>500||r.monsterDrops==null||r.monsterDrops.size()>50)throw new IllegalArgumentException("Configuración de oficios inválida");
  Set<String> jobs=new HashSet<>(),materials=new HashSet<>(),recipes=new HashSet<>();
  for(var p:r.professions)if(p==null||!id(p.id())||!jobs.add(p.id())||!name(p.name())||p.description()==null||p.description().length()>600||!Set.of("smith","tailor","merchant","mine","forest").contains(p.stationRole()))throw new IllegalArgumentException("Oficio inválido");
  for(var m:r.materials)if(m==null||!id(m.id())||!materials.add(m.id())||!name(m.name())||m.price()<1||m.price()>100000||m.sellPrice()!=null&&(m.sellPrice()<0||m.sellPrice()>m.price()))throw new IllegalArgumentException("Material inválido: la venta debe estar entre 0 y su precio de compra");
  costs(r.initialMaterials,materials);
  for(var e:r.monsterDrops.entrySet()){if(!id(e.getKey()))throw new IllegalArgumentException("Criatura de materiales inválida");costs(e.getValue(),materials);}
  for(var rec:r.recipes){
   if(rec==null||!id(rec.id())||!recipes.add(rec.id())||!name(rec.name())||!jobs.contains(rec.professionId())||rec.minProfessionLevel()<1||rec.minProfessionLevel()>r.maxProfessionLevel||rec.colCost()<0||rec.colCost()>1000000||rec.experience()<0||rec.experience()>10000||rec.maxUpgrade()<0||rec.maxUpgrade()>20||rec.upgradeColCost()<1||rec.upgradeColCost()>1000000)throw new IllegalArgumentException("Receta inválida");
   if("weapon".equals(rec.kind())){var set=c.weaponSets.stream().filter(s->s.id().equals(rec.targetId())).findFirst().orElseThrow(()->new IllegalArgumentException("Conjunto de receta inexistente"));if(!Set.of("mainHand","offHand").contains(rec.slot())||rec.slot().equals("offHand")&&set.offHand()==null)throw new IllegalArgumentException("Ranura de receta inválida");}
   else if(!"armor".equals(rec.kind())||!"armor".equals(rec.slot())||c.equipmentSets.stream().noneMatch(a->a.id().equals(rec.targetId())))throw new IllegalArgumentException("Armadura de receta inexistente");
   if(r.professions.stream().anyMatch(j->j.id().equals(rec.professionId())&&Set.of("mine","forest").contains(j.stationRole())))throw new IllegalArgumentException("Los oficios de recolección usan nodos, no recetas");
   costs(rec.materials(),materials);costs(rec.upgradeMaterials(),materials);attributes(rec.bonusAttributes(),c);attributes(rec.upgradeAttributes(),c);
  }
  if(r.shopProducts==null)r.shopProducts=basicProducts(c);
  if(r.shopProducts.size()>500)throw new IllegalArgumentException("Máximo 500 productos de mercado");
  Set<String> products=new HashSet<>();
  for(var product:r.shopProducts){
   if(product==null||!id(product.id())||!products.add(product.id())||!name(product.name())||product.slot()==null||product.targetId()==null||product.price()<1||product.price()>1000000)throw new IllegalArgumentException("Producto de mercado inválido");
   if("weapon".equals(product.kind())){var set=c.weaponSets.stream().filter(w->w.id().equals(product.targetId())).findFirst().orElseThrow(()->new IllegalArgumentException("Armas del producto inexistentes"));if(!Set.of("mainHand","offHand").contains(product.slot())||product.slot().equals("offHand")&&set.offHand()==null)throw new IllegalArgumentException("Ranura del producto inexistente");}
   else if(!"armor".equals(product.kind())||!"armor".equals(product.slot())||c.equipmentSets.stream().noneMatch(a->a.id().equals(product.targetId())))throw new IllegalArgumentException("Ropa o armadura del producto inexistente");
  }
 }
 public static List<Product> basicProducts(WorldData.CharacterOptions c){
  var products=new ArrayList<Product>();
  for(var w:c.weaponSets)if(w.minLevel()==1){products.add(new Product("market-"+w.id()+"-main",w.mainHand().name(),"weapon",w.id(),"mainHand",w.mainHand().hands()==2?80:55,true));if(w.offHand()!=null)products.add(new Product("market-"+w.id()+"-off",w.offHand().name(),"weapon",w.id(),"offHand",35,true));}
  for(var a:c.equipmentSets)if(a.minLevel()==1)products.add(new Product("market-"+a.id(),a.name(),"armor",a.id(),"armor",65,true));
  return products;
 }
 static Item buyProduct(Rules rules,WorldData.CharacterOptions c,Product product){
  var template=new Recipe("",product.name(),"",product.kind(),product.targetId(),product.slot(),1,0,Map.of(),0,0,1,Map.of(),Map.of(),Map.of());
  Item item=make(template,c);item.investedCol=product.price();
  item.recipeId=rules.recipes.stream().filter(r->r.kind().equals(product.kind())&&r.targetId().equals(product.targetId())&&r.slot().equals(product.slot())).map(Recipe::id).findFirst().orElse("");
  return item;
 }
 static void costs(Map<String,Integer> costs,Set<String> known){if(costs==null||costs.size()>50)throw new IllegalArgumentException("Materiales inválidos");for(var e:costs.entrySet())if(!known.contains(e.getKey())||e.getValue()==null||e.getValue()<1||e.getValue()>100000)throw new IllegalArgumentException("Cantidad o material inválido");}
 static void attributes(Map<String,Double> attrs,WorldData.CharacterOptions c){if(attrs==null||attrs.size()>100)throw new IllegalArgumentException("Atributos de receta inválidos");for(var e:attrs.entrySet())if(e.getValue()==null||!Double.isFinite(e.getValue())||Math.abs(e.getValue())>100||c.attributeDefinitions.stream().noneMatch(d->d.id().equals(e.getKey())))throw new IllegalArgumentException("Atributo de receta no válido");}
 static Map<String,Double> add(Map<String,Double> base,Map<String,Double> extra,WorldData.CharacterOptions c){var a=new LinkedHashMap<String,Double>();for(var d:c.attributeDefinitions){double value=base.getOrDefault(d.id(),0.0)+extra.getOrDefault(d.id(),0.0);if(base.containsKey(d.id())||extra.containsKey(d.id()))a.put(d.id(),Math.max(d.min(),Math.min(d.max(),value)));}return a;}
 static Weapons.Item add(Weapons.Item w,Map<String,Double> extra,WorldData.CharacterOptions c){return new Weapons.Item(w.name(),w.kind(),w.hands(),add(w.attributes(),extra,c),w.primary(),w.secondary(),w.glow());}
 static WorldData.Outfit add(WorldData.Outfit a,Map<String,Double> extra,WorldData.CharacterOptions c){return new WorldData.Outfit(a.id(),a.name(),a.classId(),a.gender(),a.minLevel(),add(a.attributes(),extra,c),a.style(),a.primary(),a.metal(),a.accent(),a.helmet(),a.cape(),a.specializationIds());}
 static Item make(Recipe r,WorldData.CharacterOptions c){var item=new Item();item.id=UUID.randomUUID().toString();item.recipeId=r.id();item.kind=r.kind();item.name=r.name();
  if(r.kind().equals("weapon")){var w=c.weaponSets.stream().filter(s->s.id().equals(r.targetId())).findFirst().orElseThrow();item.weapon=add(r.slot().equals("mainHand")?w.mainHand():w.offHand(),r.bonusAttributes(),c);item.specializationIds=w.specializationIds()==null?List.of():w.specializationIds();item.classId=w.classId();item.gender="";item.minLevel=w.minLevel();}
  else{var a=c.equipmentSets.stream().filter(s->s.id().equals(r.targetId())).findFirst().orElseThrow();item.armor=add(a,r.bonusAttributes(),c);item.specializationIds=a.specializationIds()==null?List.of():a.specializationIds();item.classId=a.classId();item.gender=a.gender();item.minLevel=a.minLevel();}return item;
 }
 static void pay(GameWorld.Player p,int col,Map<String,Integer> materials,int factor){long price=(long)col*factor;if(p.col<price)throw new IllegalArgumentException("Col insuficiente");for(var e:materials.entrySet())if(p.crafting.materials.getOrDefault(e.getKey(),0)<(long)e.getValue()*factor)throw new IllegalArgumentException("Material insuficiente: "+e.getKey());p.col-=(int)price;materials.forEach((id,n)->p.crafting.materials.compute(id,(key,old)->old-n*factor));}
 public static int materialSalePrice(Rules rules,Material material){return material.sellPrice()!=null?material.sellPrice():rules.saleMultiplier==0?0:Math.max(1,(int)Math.floor(material.price()*rules.saleMultiplier));}
 static void gain(GameWorld.Player p,Rules r,String job,int xp){p.crafting.professionXp.compute(job,(id,old)->Math.min(r.xpPerLevel*(r.maxProfessionLevel-1),Objects.requireNonNullElse(old,0)+xp));}
 static boolean compatible(Item i,GameWorld.Player p){return i!=null&&i.classId.equals(p.appearance.classId())&&i.minLevel<=p.level()&&Specializations.permits(i.specializationIds,p.appearance.specializationId())&&(i.weapon==null?p.specialty().armorStyles().contains(i.armor.style()):p.specialty().weaponKinds().contains(i.weapon.kind()))&&(i.gender.isEmpty()||i.gender.equals(p.appearance.gender()));}
 public static Item equipped(GameWorld.Player p,String slot){var item=p.crafting.items.get(p.crafting.equipped.get(slot));return compatible(item,p)?item:null;}
 public static Map<String,Object> view(Rules r,State s){Map<String,Integer> levels=new LinkedHashMap<>();for(var p:r.professions)levels.put(p.id(),level(r,s,p.id()));return Map.of("professionXp",s.professionXp,"levels",levels,"materials",s.materials,"items",s.items.values(),"equipped",s.equipped,"capacity",r.maxInventory);}
}
