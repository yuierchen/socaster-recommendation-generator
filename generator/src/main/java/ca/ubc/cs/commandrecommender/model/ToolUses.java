package ca.ubc.cs.commandrecommender.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;
import org.apache.commons.collections4.set.ListOrderedSet;


public class ToolUses extends ArrayList<ToolUse>{

    public final int userId;

    public ToolUses(){
        this(-1);
    }

    public ToolUses(int userId){
        super();
        this.userId = userId;
    }

    public ToolUses(int userId, Collection<? extends ToolUse> c) {
        super(c);
        this.userId = userId;
    }

    public ToolUses(int userId, int initialCapacity) {
        super(initialCapacity);
        this.userId = userId;
    }

    public boolean userIsIdentified(){
        return userId!=-1;
    }

    public boolean userIdIs(int id){
        return userId==id;
    }

    private static final long serialVersionUID = 8820630451728423087L;

    public ListOrderedSet<Integer> toolsUsedInOrder(){
        ListOrderedSet<Integer> set = new ListOrderedSet<Integer>();
        for(ToolUse u : this)
            set.add(u.tool);
        return set;
    }
    public String firstTimeUsed(int toolId) {
        for(ToolUse tu : this){
            if(tu.tool==toolId)
                return tu.time.toString();
        }
        return "<unknown>";
    }

    public Bag<Integer> toolsUsedBag(){
        Bag<Integer> bag = new HashBag<Integer>();
        for(ToolUse u : this)
            bag.add(u.tool);
        return bag;
    }

    /*
     * A convenience method for sorting the uses by timestamp
     */
    public void sort() {
        Collections.sort(this, new Comparator<ToolUse>() {

            @Override
            public int compare(ToolUse o1, ToolUse o2) {
                return o1.time.compareTo(o2.time);
            }
        });
    }

}