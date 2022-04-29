package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.Deposit;
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
import java.util.Stack;

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
	
	public static Position townhallPos;
	public static int townhallId;
	
	private Peasant bob;
	
	private static Set<Position> resourcePos = new HashSet<Position>();
	
	private int gold = 0;
	private int wood = 0;
	
	private double cost = 0;
	private double heuristic = 0;
	
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
    	System.out.println("Peasant Bob with ID: " + bob.getId());
    }
    /**
     * Secondary constructor to ease generating children, effectively a clone function
     * 
     * @param state The GameState to copy
     */
    public GameState(GameState state) {
    	this.gold = state.gold;
    	this.wood = state.wood;
    	this.bob = new Peasant(state.bob);
    	this.cost = state.cost;
    	
    	for(Resource resource : state.resources.values()) {
    		if(resource.isGold()) this.resources.put(resource.getId(), new Gold(resource));
    		else this.resources.put(resource.getId(), new Wood(resource));
    	}
    	
    	state.plan.stream().forEach((p) -> plan.add(p));
    }
    
    private class Gold extends Resource {
    	Gold(int id, Position pos, int amount) {
    		this.id = id;
    		this.pos = pos;
    		this.amount = amount;
    	}
    	
    	Gold(Resource resource) {
    		this.id = resource.getId();
    		this.pos = resource.getPos();
    		this.amount = resource.getAmount();
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
    	
    	Wood(Resource resource) {
    		this.id = resource.getId();
    		this.pos = resource.getPos();
    		this.amount = resource.getAmount();
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
    	
    	public void setAmount(int amount) { this.amount = amount; }
    	
    	public boolean empty() { return amount <= 0; }
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
    	
    	public Peasant(Peasant p) {
    		this.id = p.id;
    		this.pos = p.pos;
    		this.gold = p.gold;
    		this.wood = p.wood;
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
    	public boolean holdingResource() { return hasGold() || hasWood(); }
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
    	
    	//System.out.println("initial state gen can harvest = " + canHarvest());
    	
    	//if bob has stuff attempt to deposit
    	if(bob.holdingResource()) {
    		
    		if(bob.getPos().equals(townhallPos)) {

    			Deposit action = new Deposit(bob.getPos(), bob.holdingResource());
    			
    			if(action.preconditionsMet(child)) {
    				child = new GameState(action.apply(child));
    				child.update(child, action);
    			}
    		}
    		else {
    			
    			Move action = new Move(bob.getPos(), townhallPos);
    			
    			if(action.preconditionsMet(child)) {
    				child = new GameState(action.apply(child));
    				child.update(child, action);
    			}
    		}
    	}
    	//if bob dont have stuff attempt to harvest
    	//by checking if bob can harvest
    	else if(canHarvest()) {

    		for(Resource resource : this.resources.values()) {

    			Harvest action = new Harvest(bob.getPos(), resource.getPos(), resource.getId(), bob.holdingResource(), resource.empty());
    			
    			if(action.preconditionsMet(child)) {
    				child = new GameState(action.apply(child));
    				child.update(child, action);
    			}
    		}
    	}
    	//move around
    	else {
    		
    		for(Resource resource : this.resources.values()) {
    			if(resource.empty()) continue;
    			
    			GameState grandChild = new GameState(child);
    			
    			Move action = new Move(bob.getPos(), resource.getPos());
    			
    			if(action.preconditionsMet(grandChild)) {
    				grandChild = new GameState(action.apply(grandChild));
    				grandChild.update(grandChild, action);
    			}
    			
    			children.add(grandChild);
    		}
    	}
    	
    	children.add(child);
    	
    	GameState grandChild = new GameState(this);
    	
    	Deposit depAction = new Deposit(bob.getPos(), bob.holdingResource());
    	
    	if(depAction.preconditionsMet(grandChild)) {
    		grandChild = depAction.apply(grandChild);
    		grandChild.update(grandChild, depAction);
    	}
    	
    	for(Resource resource : this.resources.values()) {
    		GameState grandGrandChild = new GameState(grandChild);
    		
    		StripsAction action = null;
    		
    		if(canHarvest()) {
    			action = new Harvest(bob.getPos(), resource.getPos(), resource.getId(), bob.holdingResource(), resource.empty());
    		}
    		else
    		{
    			action = new Move(bob.getPos(), resource.getPos());
    		}
    		
    		if(action.preconditionsMet(grandGrandChild)) {
    			grandGrandChild = new GameState(action.apply(grandGrandChild));
    			grandGrandChild.update(grandGrandChild, action);
    		}
    		
    		children.add(new GameState(grandGrandChild));
    	}
    	
    	Move movAction = new Move(bob.getPos(), townhallPos);
    	
    	if(movAction.preconditionsMet(grandChild)) {
    		grandChild = movAction.apply(grandChild);
    		grandChild.update(grandChild, movAction);
    	}
    	
        return children;
    }
    
    private boolean canHarvest() {
    	for(Resource resource : this.resources.values()) {
    		if(resource.getPos().equals(bob.getPos())) {
    			return !resource.empty();
    		}
    	}
    	return false;
    	//return !this.resources.values().stream().filter((r) -> r.getPos().equals(bob.getPos())).findFirst().get().empty();
    }

    /**
     * Heuristic is a linear sum of quantities that need to be fulfilled
     * As a result, the heuristic is greater for a less favorable state and lesser for a favorable state
     *
     * @return The value estimated remaining cost to reach a goal state from this state.
     */
    public double heuristic() {

    	if(this.heuristic != 0) return heuristic;
    	
    	if(gold <= requiredGold) heuristic += (requiredGold - gold);
    	else heuristic += (gold - requiredGold);
    	
    	if(wood <= requiredWood) heuristic += (requiredWood - wood);
    	else heuristic += (wood - requiredWood);
    	
    	if(bob.holdingResource()) heuristic -= bob.getGold() + bob.getWood();
    	else {
    		if(canHarvest()) heuristic -= 50;
    		else heuristic += 100;
    	}
    	
        return heuristic;
    }

    /**
     * @return The current cost to reach this goal
     */
    public double getCost() {
        return this.cost;
    }
    
    public void update(GameState state, StripsAction action) {
    	state.plan.add(action);
    	state.heuristic = state.heuristic();
    	state.cost += action.getCost();
    }
    
    public Stack<StripsAction> getPlan() {
    	
    	Stack<StripsAction> plan = new Stack<StripsAction>();
    	
    	for(int i = this.plan.size() - 1; i > -1; i--) {
    		plan.push(this.plan.get(i));
    	}
    	
    	return plan;
    }
    
    public void move(Position pos) {
    	bob.setPos(pos);
    }
    
    public void harvest(int id) {
    	Resource resource = this.resources.get(id);
    	if(resource.isGold()) {
    		bob.setGold(Math.min(100, resource.getAmount()));
    		resource.setAmount(Math.max(0, resource.getAmount() - 100));
    	}
    	else //harvest wood
    	{
    		bob.setWood(Math.min(100, resource.getAmount()));
    		resource.setAmount(Math.max(0, resource.getAmount() - 100));
    	}

    }
    
    public void deposit() {
    	if(this.bob.hasGold()) {
    		this.gold += this.bob.getGold();
    		this.bob.setGold(0);
    	}
    	else //deposit wood
    	{
    		this.wood += this.bob.getWood();
    		this.bob.setWood(0);
    	}
    }
    
    public Position bobPos() {
    	return bob.getPos();
    }
    
    public Peasant getBob() {
    	return bob;
    }

    /**
     * Compares based on GameState heuristic
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
     * Defines GameState equality
     *
     * @param o The game state to compare
     * @return True if this state equals the other state, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        
        if(this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        GameState state = (GameState) o;
        
        if(gold != state.gold) return false;
        if(wood != state.wood) return false;
        if(this.bob.getPos() != state.getBob().getPos()) return false;
        
        return true;
    }

    /**
     * Calculates GameState hash
     *
     * @return An integer hashcode that is equal for equal states.
     */
    @Override
    public int hashCode() {
    	final int prime = 31;
    	int hash = 1;

    	hash = prime * hash + gold;
    	hash = prime * hash + wood;
    	hash = prime * hash + bob.getGold();
    	hash = prime * hash + bob.getWood();
    	hash = prime * hash + bob.getPos().hashCode();
    	//hash = prime * hash + plan.size();
    	
        return hash;
    }
}
