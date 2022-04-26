package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.Harvest;
import edu.cwru.sepia.agent.planner.actions.Move;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.State;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to represent the state of the game after applying one of the avaiable actions. It will also
 * track the A* specific information such as the parent pointer and the cost and heuristic function. Remember that
 * unlike the path planning A* from the first assignment the cost of an action may be more than 1. Specifically the cost
 * of executing a compound action such as move can be more than 1. You will need to account for this in your heuristic
 * and your cost function.
 *
 * The first instance is constructed from the StateView object (like in PA2). Implement the methods provided and
 * add any other methods and member variables you need.
 *
 * Some useful API calls for the state view are
 *
 * state.getXExtent() and state.getYExtent() to get the map size
 *
  * Note that SEPIA saves the townhall as a unit. Therefore when you create a GameState instance,
 * you must be able to distinguish the townhall from a peasant. This can be done by getting
 * the name of the unit type from that unit's TemplateView:
 * state.getUnit(id).getTemplateView().getName().toLowerCase(): returns "townhall" or "peasant"
 * 
 * You will also need to distinguish between gold mines and trees.
 * state.getResourceNode(id).getType(): returns the type of the given resource
 * 
 * You can compare these types to values in the ResourceNode.Type enum:
 * ResourceNode.Type.GOLD_MINE and ResourceNode.Type.TREE
 * 
 * You can check how much of a resource is remaining with the following:
 * state.getResourceNode(id).getAmountRemaining()
 *
 * I recommend storing the actions that generated the instance of the GameState in this class using whatever
 * class/structure you use to represent actions.
 */
public class GameState implements Comparable<GameState> {
	
	private static int requiredGold;
	private static int requiredWood;
	
	private static Position townhallPos;
	private static int townhallId;
	
	private static Peasant bob;
	
	private static Set<Position> resourcePos = new HashSet<Position>();
	
	private int gold;
	private int wood;
	
	private double cost;
	
	private Map<Integer, Resource> resources = new HashMap<Integer, Resource>();
	
	private List<StripsAction> plan = new ArrayList<StripsAction>();

    /**
     * Construct a GameState from a stateview object. This is used to construct the initial search node. All other
     * nodes should be constructed from the another constructor you create or by factory functions that you create.
     *
     * @param state The current stateview at the time the plan is being created
     * @param playernum The player number of agent that is planning
     * @param requiredGold The goal amount of gold (e.g. 200 for the small scenario)
     * @param requiredWood The goal amount of wood (e.g. 200 for the small scenario)
     * @param buildPeasants True if the BuildPeasant action should be considered
     */
    public GameState(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants) {
        // TODO: Implement me!
    	// in PA2, it was advantageous to build a second constructor which took the previous state as an input to simplify making children
    	// so i guess i should do this again
    	// alright let's do this entire thing again woohoo
    	
    	GameState.requiredGold = requiredGold;
    	GameState.requiredWood = requiredWood;
    	
    	state.getAllResourceNodes().stream().forEach((r) -> {
    		Position pos = new Position(r.getXPosition(), r.getYPosition());
    		GameState.resourcePos.add(pos);
    		
    		if(r.getType().name().toLowerCase().equals("gold_mine")) {
    			resources.put(r.getID(), new Gold(r.getID(), pos, r.getAmountRemaining()));
    		}
    		else //name == "wood" 
    		{
    			resources.put(r.getID(), new Wood(r.getID(), pos, r.getAmountRemaining()));
    		}
    	});
    	
    	state.getAllUnits().stream().forEach((u) -> {
    		Position pos = new Position(u.getXPosition(), u.getYPosition());
    		
    		if(u.getTemplateView().getName().toLowerCase().equals("townhall")) {
    			GameState.townhallPos = pos;
    			GameState.townhallId = u.getID();
    		}
    		else //name == "peasant"
    		{
    			bob = new Peasant(u.getID(), pos);
    		}
    	});
    }
    
    public GameState(GameState state) {
    	this.gold = state.gold;
    	this.wood = state.wood;
    	this.bob = state.bob;
    }
    
    private class Gold extends Resource {
    	Gold(int id, Position pos, int amount) {
    		this.id = id;
    		this.pos = pos;
    		this.amount = amount;
    	}
    	
    	@Override
    	public boolean isGold() { return true; }
    	
    	@Override
    	public boolean isWood() { return false; }
    }
    
    private class Wood extends Resource {
    	Wood(int id, Position pos, int amount) {
    		this.id = id;
    		this.pos = pos;
    		this.amount = amount;
    	}
    	
    	@Override
    	public boolean isGold() { return false; }
    	
    	@Override
    	public boolean isWood() { return true; }
    }

    private abstract class Resource {
    	protected int id;
    	protected Position pos;
    	
    	protected int amount;
    	
    	public abstract boolean isGold();
    	public abstract boolean isWood();
    	
    	public int getId() { return this.id; }
    	public Position getPos() { return this.pos; }
    	
    	public int getAmount() { return this.amount; }
    	
    	public void setId(int id) { this.id = id; }
    	public void setPos(Position pos) { this.pos = pos; }
    	
    	public void setAmount(int id) { this.amount = amount; }
    	
    	public boolean empty() { return amount == 0; }
    }
    
    private class Peasant {
    	private int id;
    	private Position pos;
    	private int gold = 0;
    	private int wood = 0;
    	
    	public Peasant(int id, Position pos) {
    		this.id = id;
    		this.pos = pos;
    	}
    	
    	public int getId() { return id; }
    	public Position getPos() { return pos; }
    	
    	public void setId(int id) { this.id = id; }
    	public void setPos(Position pos) { this.pos = pos; }
    	
    	public int getGold() { return gold; }
    	public int getWood() { return wood; }
    	
    	public void setGold(int gold) { this.gold = gold; }
    	public void setWood(int wood) { this.wood = wood; }
    	
    	public boolean hasGold() { return gold > 0; }
    	public boolean hasWood() { return wood > 0; }
    	public boolean hasSome() { return hasGold() || hasWood(); }
    }
    
    /**
     * Unlike in the first A* assignment there are many possible goal states. As long as the wood and gold requirements
     * are met the peasants can be at any location and the capacities of the resource locations can be anything. Use
     * this function to check if the goal conditions are met and return true if they are.
     *
     * @return true if the goal conditions are met in this instance of game state.
     */
    public boolean isGoal() {
        // TODO: Implement me!
        return gold >= requiredGold && wood >= requiredWood;
    }

    /**
     * The branching factor of this search graph are much higher than the planning. Generate all of the possible
     * successor states and their associated actions in this method.
     *
     * @return A list of the possible successor states and their associated actions
     */
    public List<GameState> generateChildren() {
        // TODO: Implement me!
    	
    	List<GameState> children = new ArrayList<GameState>();
    	
    	GameState child = new GameState(this);
    	
    	//if bob has stuff
    	if(bob.hasSome()) {
    		
    		if(bob.getPos().equals(townhallPos)) {
    			//deposit
    		}
    		else {
    			
    			Move action = new Move(townhallPos);
    			if(action.preconditionsMet(child)) {
    				action.apply(child);
    				update(action);
    			}
    		}
    	}
    	
    	//if bob can get some stuff
    	//aka if bob is at a resource
    	else if(GameState.resourcePos.contains(bob.getPos()) && canHarvest()) {
    		
    		for(Resource resource : this.resources.values()) {
    			
    			Harvest action = new Harvest(resource.getId());
    			
    			if(action.preconditionsMet(child)) {
    				action.apply(child);
    				update(action);
    			}
    		}
    	}
    	//move around
    	else {
    		
    		for(Resource resource : this.resources.values()) {
    			
    			GameState grandChild = new GameState(child);
    			
    			Move action = new Move(resource.getPos());
    			
    			if(action.preconditionsMet(grandChild)) {
    				action.apply(grandChild);
    				update(action);
    			}
    			
    			children.add(grandChild);
    		}
    	}
    	children.add(child);
    	
        return children;
    }
    
    private boolean canHarvest() {
    	return !this.resources.values().stream().filter((r) -> r.getPos().equals(bob.getPos())).findFirst().get().empty();
    }

    /**
     * Write your heuristic function here. Remember this must be admissible for the properties of A* to hold. If you
     * can come up with an easy way of computing a consistent heuristic that is even better, but not strictly necessary.
     *
     * Add a description here in your submission explaining your heuristic.
     *
     * @return The value estimated remaining cost to reach a goal state from this state.
     */
    public double heuristic() {
        // TODO: Implement me!
    	int heuristic = 0;
    	
    	if(gold <= requiredGold) heuristic += (requiredGold - gold);
    	else heuristic += (gold - requiredGold);
    	
    	if(wood <= requiredWood) heuristic += (requiredWood - wood);
    	else heuristic += (wood - requiredWood);
    	
    	if(bob.hasSome()) heuristic -= bob.getGold() + bob.getWood();
    	else {
    		if(canHarvest()) heuristic -= 50;
    		else heuristic += 100;
    	}
    	
        return heuristic;
    }

    /**
     *
     * Write the function that computes the current cost to get to this node. This is combined with your heuristic to
     * determine which actions/states are better to explore.
     *
     * @return The current cost to reach this goal
     */
    public double getCost() {
        // TODO: Implement me!
        return this.cost;
    }
    
    public void update(StripsAction action) {
    	plan.add(action);
    	this.cost++;
    }
    
    public void move(Position pos) {
    	bob.setPos(pos);
    }
    
    public void harvest(int id) {
    	Resource resource = this.resources.get(id);
    	if(resource.isGold()) {
    		bob.setGold(Math.min(100, resource.getAmount()));
    		resource.setAmount(Math.min(0, resource.getAmount() - 100));
    	}
    	else //harvest wood
    	{
    		bob.setWood(Math.min(100, resource.getAmount()));
    		resource.setAmount(Math.min(0, resource.getAmount() - 100));
    	}
    }
    
    public void deposit() {
    	if(bob.hasGold()) {
    		this.gold += bob.getGold();
    		bob.setGold(0);
    	}
    	else //deposit wood
    	{
    		this.wood += bob.getWood();
    		bob.setWood(0);
    	}
    }
    
    public Position bobPos() {
    	return bob.getPos();
    }

    /**
     * This is necessary to use your state in the Java priority queue. See the official priority queue and Comparable
     * interface documentation to learn how this function should work.
     *
     * @param o The other game state to compare
     * @return 1 if this state costs more than the other, 0 if equal, -1 otherwise
     */
    @Override
    public int compareTo(GameState o) {
        if(this.heuristic() > o.heuristic()) return 1;
        else if(this.heuristic() < o.heuristic()) return -1;
        return 0;
    }

    /**
     * This will be necessary to use the GameState as a key in a Set or Map.
     *
     * @param o The game state to compare
     * @return True if this state equals the other state, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        // TODO: Implement me!
        return false;
    }

    /**
     * This is necessary to use the GameState as a key in a HashSet or HashMap. Remember that if two objects are
     * equal they should hash to the same value.
     *
     * @return An integer hashcode that is equal for equal states.
     */
    @Override
    public int hashCode() {
        // TODO: Implement me!
        return 0;
    }
}
