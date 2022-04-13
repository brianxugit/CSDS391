package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.util.*;
import java.util.stream.Collectors;

import com.sun.source.tree.Tree;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {

	public static double max = Double.POSITIVE_INFINITY;
	public static double min = Double.NEGATIVE_INFINITY;
	
	private Cell[][] map;
	private int xDim;
	private int yDim;
	//private map of agents
	//private map of obstacles
	
	private boolean playerTurn; //true for footmen, false for archers
	
	private double utility = 0;
	
	//absolute state of confusion of what scope this should be
	//allagents: id, agent
	private Map<Integer, Agent> allAgents;
	private ArrayList<Agent> allFootmen;
	private ArrayList<Agent> allArchers;
	
	private State.StateView stateBandAid; //sweet jesus is this a terrible idea
	
	//need a variable to check if this gamestate is enemy turn or our turn...
	//bool something
	
    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns the IDs of all of the obstacles in the map
     * state.getResourceNode(int resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     * 
     * You can get a list of all the units belonging to a player with the following command:
     * state.getUnitIds(int playerNum): gives a list of all unit IDs beloning to the player.
     * You control player 0, the enemy controls player 1.
     * 
     * In order to see information about a specific unit, you must first get the UnitView
     * corresponding to that unit.
     * state.getUnit(int id): gives the UnitView for a specific unit
     * 
     * With a UnitView you can find information about a given unit
     * unitView.getXPosition() and unitView.getYPosition(): get the current location of this unit
     * unitView.getHP(): get the current health of this unit
     * 
     * SEPIA stores information about unit types inside TemplateView objects.
     * For a given unit type you will need to find statistics from its Template View.
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit type deals
     * unitView.getTemplateView().getBaseHealth(): The initial amount of health of this unit type
     *
     * @param state Current state of the episode
     */
    public GameState(State.StateView state) {
    	
    	stateBandAid = state;
    	
    	//band-aid data structure for now
    	allFootmen = new ArrayList<Agent>();
    	allArchers = new ArrayList<Agent>(); 
    	
		this.xDim = state.getXExtent();
		this.yDim = state.getYExtent();
		
    	this.map = new Cell[xDim][yDim];
    	
    	Map<Integer, Tree> trees = new HashMap<Integer, Tree>();
    	Map<Integer, Agent> agents = new HashMap<Integer, Agent>();
    	
    	state.getAllResourceNodes().stream().forEach( (t) -> {
    		Tree tree = new Tree(t.getID(), t.getXPosition(), t.getYPosition());
    		map[t.getXPosition()][t.getYPosition()] = tree;
    		trees.put(tree.getId(), tree);
    		
    		//System.out.println(tree.getX() + " " + tree.getY());
    	}
    	);
    	
    	state.getAllUnits().stream().forEach( (a) -> {
    		Agent agent = new Agent(a.getID(), a.getXPosition(), a.getYPosition(), a.getTemplateView().getRange(), a.getTemplateView().getBasicAttack(), state.getUnit(a.getID()).getHP());
    		agent.setPlayer(a.getTemplateView().getName().equals("Footman") ? 0 : 1);
    		map[a.getXPosition()][a.getYPosition()] = agent;
    		agents.put(agent.getId(), agent);
    		
    		if(agent.getPlayer() == 0) {
    			allFootmen.add(agent);
    		}
    		else {
    			allArchers.add(agent);
    		}
    		
    		System.out.print(a.getTemplateView().getName() + " (Player " + agent.getPlayer() + "), ID:" + agent.getId());
    		System.out.print(" at (" + agent.getX() + ", " + agent.getY() + ") with HP:" + agent.getHp());
    		System.out.println();
    	});
    	
    	//temporary band-aid before i figure out how i want to structure all the data
    	allAgents = agents;
    	
    	/*
    	for(Agent aa : allAgents.values()) {
    		System.out.println(aa.getId() + " " + aa.atkRan + " " + aa.atkDmg);
    		for(int aaa : canAttack(aa)) {
    			System.out.print(aaa);
    		}
    		System.out.println();
    	}
    	*/
    	
    	System.out.println();
    }
    
    //secondary constructor...
    //
    public GameState(GameState gameState) {
    	
    }

    private class Tree extends Cell {
    	
    	private int id;
    	private int x;
    	private int y;
    	
    	public Tree(int id, int x, int y) {
    		super(id, x, y);
    	}
    }

    private class Agent extends Cell{
    	
    	private int id;
    	private int x;
    	private int y;
    	
    	private int atkDmg;
    	private int atkRan;
    	private int hp;
    	private int potentialHp;
    	
    	//which player controls it?
    	private int player;
    	
    	public Agent(int id, int x, int y, int atkRan, int atkDmg, int hp) {
    		super(id, x, y);
    		
    		this.atkRan = atkRan;
    		this.atkDmg = atkDmg; 		
    		this.hp = hp;
    	}
    	
    	public int getAtkDmg() {
    		return atkDmg;
    	}
    	
    	public int getAtkRan() {
    		return atkRan;
    	}
    	
    	public void setHp(int hp) {
    		this.hp = hp;
    	}
    	
    	public int getHp() {
    		return hp;
    	}
    	
    	public int getPlayer() {
    		return player;
    	}
    	
    	public void setPlayer(int i) {
    		this.player = i;
    	}
    	
    	public void setPhp(int php) {
    		this.potentialHp = php; //why do people dislike php? ive havent gotten to that part of cs yet
    	}
    	
    	public int getPhp() { //i just think its funny
    		return potentialHp;
    	}
    	
    	public boolean alive() {
    		return this.hp > 0;
    	}
    }
    
    /**
     * abstract representation of a single cell on the game map
     * 
     * @author Brian
     */
    private abstract class Cell {
    	
    	private int id;
    	private int x;
    	private int y;
    	
    	public Cell(int id, int x, int y) {
    		this.id = id;;
    		this.x = x;
    		this.y = y;;
    	}
    	
    	public int getId() {
    		return id;
    	}
    	
    	public int getX() {
    		return x;
    	}
    	
    	public int getY() {
    		return y;
    	}
    	
    	public void setX(int x) {
    		this.x = x;
    	}
    	
    	public void setY(int y) {
    		this.y = y;
    	}
    }
    
    /**
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {
    	
    	//weighted linear combination
    	//a* would be nice but i somehow feel that would be too complicated
    	//but how else should the footman find its path?
    	//start with most basic: distance from  archer
    	//this would immediately cause an issue given how the map is designed but let's just see how it interacts
    	//the plan afterwards is to implement a* and use path length as a heuristic
    	this.utility += enemyDistanceUtility();
    	
    	//a useful utility will be the expected hp of any given agent
    	//that is, if they are in attackable range, consider the potential hp loss
    	
        return this.utility;
    }
    
    private double enemyDistanceUtility() {
    	double utility = 0.0;
    	
    	for(Agent footman : getLivingFootmen()) {
    		for(Agent archer : getLivingArchers()) {
    			utility = Math.min(distance(footman, archer), utility);
    		}
    	}
    	
    	return utility;
    }

    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     * 
     * It may be useful to be able to create a SEPIA Action. In this assignment you will
     * deal with movement and attacking actions. There are static methods inside the Action
     * class that allow you to create basic actions:
     * Action.createPrimitiveAttack(int attackerID, int targetID): returns an Action where
     * the attacker unit attacks the target unit.
     * Action.createPrimitiveMove(int unitID, Direction dir): returns an Action where the unit
     * moves one space in the specified direction.
     *
     * You may find it useful to iterate over all the different directions in SEPIA. This can
     * be done with the following loop:
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     * 
     * If you wish to explicitly use a Direction you can use the Direction enum, for example
     * Direction.NORTH or Direction.NORTHEAST.
     * 
     * You can check many of the properties of an Action directly:
     * action.getType(): returns the ActionType of the action
     * action.getUnitID(): returns the ID of the unit performing the Action
     * 
     * ActionType is an enum containing different types of actions. The methods given above
     * create actions of type ActionType.PRIMITIVEATTACK and ActionType.PRIMITIVEMOVE.
     * 
     * For attack actions, you can check the unit that is being attacked. To do this, you
     * must cast the Action as a TargetedAction:
     * ((TargetedAction)action).getTargetID(): returns the ID of the unit being attacked
     * 
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren() {
    	
    	//ok well i made the alpha beta system but theres no way to get children yet so its always null
    	//here we go i guess
    	
    	//so like
    	//well there's exactly up to 5 children for each agent, 4 move states and 1 atk state
    	//we need to choose between the footmen and archers for computing gamestate moves given a turn
    	//naively i hoped playernum would be useful but not for this...
    	
    	//choose between footmen and archers
    	Collection<Agent> agents = playerTurn ? this.getLivingFootmen() : this.getLivingArchers();
    	
    	//collect actions per agent into a list
    	//given an agent, agentActions() returns a list of actions that agent can do
    	List<List<Action>> agentActions = agents.stream().map(a -> agentActions(a)).collect(Collectors.toList());
    	//we only have 2 agents so the result is the list agentActions.get(0) and agentActions.get(1) for each one
    	
    	//now we want to combine these actions to form up to 25 pairs of actions
    	//make action combos for gamestates
    	//mercy on my names
    	List<Map<Integer, Action>> actionListList = new ArrayList<Map<Integer, Action>>();
    	
    	for(Action act1 : agentActions.get(0)) {
    		if(agentActions.size() == 1) {
    			Map<Integer, Action> actionList = new HashMap<Integer, Action>();
    			actionList.put(act1.getUnitId(), act1);
    			actionListList.add(actionList);
    		} else {
    			for(Action act2 : agentActions.get(1)) {
    				Map<Integer, Action> actionList = new HashMap<Integer, Action>();
    				actionList.put(act1.getUnitId(), act1);
    				actionList.put(act2.getUnitId(), act2);
    				actionListList.add(actionList);
    			}
    		}
    	}
    	
    	//now the tricky part... create gamestates associated with each action outcome
    	//the document explicitly says not to use sepia's state cloning feature...
    	//so my initial hope of making several copies of the gamestate,
    	//then applying all relevant actions is completely useless...
    	//the current gamestate constructor uses some stateview state so let's try to use that
    	List<GameStateChild> children = new ArrayList<GameStateChild>(25);
    	for(Map<Integer, Action> actionList : actionListList) {
    		GameState child = new GameState(stateBandAid); //holy cow is this a bad idea
    		for(Action action : actionList.values()) {
    			child.applyAction(action);
    		}
    	}
    	
    	//the amount of band-aids on this code is extremely alarming
    	//and i havent even done heuristic data collection and calcualtion yet
    	
        return children;
    }
    
    private void applyAction(Action action) {
    	if(action.getType() == ActionType.PRIMITIVEATTACK) {
    		TargetedAction tarAction = (TargetedAction) action;
    		
    		Agent atker = this.allAgents.get(tarAction.getUnitId());
    		Agent atked = this.allAgents.get(tarAction.getTargetId());
    		
    		this.attack(atker, atked);
    		
    	} else { //actiontype.primitivemove
    		DirectedAction dirAction = (DirectedAction) action;
    		
    		this.move(this.allAgents.get(dirAction.getUnitId()), 
    				  dirAction.getDirection().xComponent(),
    				  dirAction.getDirection().yComponent());
    	}
    }
    
    private void move(Agent agent, int x, int y) {
    	int nextX = agent.getX() + x;
    	int nextY = agent.getY() + y;
    	
    	this.map[agent.getX()][agent.getY()] = null;
    	
    	agent.setX(nextX);
    	agent.setY(nextY);
    	
    	this.map[nextX][nextY] = agent;
    }
    
    private void attack(Agent atker, Agent atked) {
    	atked.setHp(atked.getHp() - atker.getAtkDmg());
    }

    /**
     * to make life easier and since all agents will need to have actions evaluated,
     * method to determine agent actions
     * all agents have 5 choices: move in one of cardinal directions, or attack--no diagonals
     * @param agent
     * @return
     */
    private List<Action> agentActions(Agent agent) {
    	
    	List<Action> actions = new ArrayList<Action>();
    	
    	for(Direction direction : Direction.values()) {
    		switch(direction) {
    		case NORTH: case SOUTH: case EAST: case WEST:
    			int x = agent.getX() + direction.xComponent();
    			int y = agent.getY() + direction.yComponent();
    			
    			if (x >= 0 && y >= 0 && x < xDim && y < yDim && map[x][y] == null) {
    				actions.add(Action.createPrimitiveMove(agent.getId(), direction));
    			}
    			break;
    		}
    	}
    	//check if agent can attack
    	//from observing the base config,
    	//when the archer and footman occupy opposite corners of 7x7 square,
    	//i expect distance to be that of sqrt(36 + 36), floored to 8, while archer range is 8
    	//in the sim they attack, so my can attack function should be able to account for this
    	//it also seems that the archer dmg value is randomized in its dmg range
    	for(int id : canAttack(agent)) {
    		actions.add(Action.createPrimitiveAttack(agent.getId(), id));
    	}
    	
    	return actions;
    }
    
    //who can i attack?
    private List<Integer> canAttack(Agent agent) {
    	List<Integer> agents = new ArrayList<Integer>();
    	
    	for (Agent enemy : allAgents.values()) {
    		//need to check that
    		//1. enemy is actually an enemy; not same type (footman vs archer)
    		//2. they are not the same agent
    		//3. enemy is in agent's range
    		if(enemy.getPlayer() != agent.getPlayer() && 
    		   enemy.getId() != agent.getId() &&
    		   //this is too complicated so i need a distance function
    		   Math.floor(distance(agent, enemy)) <= agent.atkRan) {
    			agents.add(enemy.getId());
    		}
    	}
    	
    	return agents;
    }
    
    private double distance(Cell agent, Cell enemy) {
    	return Math.sqrt(Math.pow(agent.getX() - enemy.getX(), 2.0) + Math.pow(agent.getY() - enemy.getY(), 2.0));
    }
    
    //it turns out that constantly checking allFootmen.size() > 0 is annoying...
    public Collection<Agent> getLivingFootmen() {
    	return allFootmen.stream().filter(a -> (a.alive())).collect(Collectors.toList());
    }
    
    public Collection<Agent> getLivingArchers() {
    	return allArchers.stream().filter(a -> (a.alive())).collect(Collectors.toList());
    }
    
    /*
     * utility function for confirming map data
     */
    private void printMap(Cell[][] map) {
    	for (Cell[] cell : map) {
    		for (Cell sell : cell) {
    			if (sell != null) {
    				System.out.println(sell.getId() + " at " + sell.getX() + " " + sell.getY());
    			}
    		}
    	}
    }
}
