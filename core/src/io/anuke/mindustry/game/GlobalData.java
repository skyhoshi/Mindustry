package io.anuke.mindustry.game;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.ObjectIntMap;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.EventType.UnlockEvent;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;

/**Stores player unlocks. Clientside only.*/
public class GlobalData{
    private ObjectMap<ContentType, ObjectSet<String>> unlocked = new ObjectMap<>();
    private ObjectIntMap<Item> items = new ObjectIntMap<>();
    private boolean modified;

    public GlobalData(){
        Core.settings.setSerializer(ContentType.class, (stream, t) -> stream.writeInt(t.ordinal()), stream -> ContentType.values()[stream.readInt()]);
    }

    public void addItem(Item item, int amount){
        modified = true;
        items.getAndIncrement(item, 0, amount);
    }

    public boolean hasItems(ItemStack[] stacks){
        for(ItemStack stack : stacks){
            if(items.get(stack.item, 0) < stack.amount){
                return false;
            }
        }
        return true;
    }

    public void removeItems(ItemStack[] stacks){
        for(ItemStack stack : stacks){
            items.getAndIncrement(stack.item, 0, -stack.amount);
        }
        modified = true;
    }

    public ObjectIntMap<Item> items(){
        return items;
    }

    /** Returns whether or not this piece of content is unlocked yet.*/
    public boolean isUnlocked(UnlockableContent content){
        return true;
        //return content.alwaysUnlocked() || unlocked.getOr(content.getContentType(), ObjectSet::new).contains(content.getContentName());
    }

    /**
     * Makes this piece of content 'unlocked', if possible.
     * If this piece of content is already unlocked or cannot be unlocked due to dependencies, nothing changes.
     * Results are not saved until you call {@link #save()}.
     *
     * @return whether or not this content was newly unlocked.
     */
    public boolean unlockContent(UnlockableContent content){
        if(!content.canBeUnlocked() || content.alwaysUnlocked()) return false;

        boolean ret = unlocked.getOr(content.getContentType(), ObjectSet::new).add(content.getContentName());

        //fire unlock event so other classes can use it
        if(ret){
            modified = true;
            content.onUnlock();
            Events.fire(new UnlockEvent(content));
            save();
        }

        return ret;
    }

    /** Clears all unlocked content. Automatically saves.*/
    public void reset(){
        save();
    }

    public void checkSave(){
        if(modified){
            save();
            modified = false;
        }
    }

    @SuppressWarnings("unchecked")
    public void load(){
        unlocked = Core.settings.getObject("unlocks", ObjectMap.class, ObjectMap::new);
        for(Item item : Vars.content.items()){
            items.put(item, Core.settings.getInt("item-" + item.name, 0));
        }
    }

    public void save(){
        Core.settings.putObject("unlocks", unlocked);
        for(Item item : Vars.content.items()){
            Core.settings.put("item-" + item.name, items.get(item, 0));
        }
        Core.settings.save();
    }

}
