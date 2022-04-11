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
	
	private double utility = 0;
	
	private Map<Integer, Agent> allAgents;
	
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
    		Agent agent = new Agent(a.getID(), a.getXPosition(), a.getYPosition(), a.getTemplateView().getRange(), a.getTemplateView().getBasicAttack(), a.getTemplateView().getBaseHealth());
    		agent.setPlayer(a.getTemplateView().getName().equals("Footman") ? 0 : 1);
    		map[a.getXPosition()][a.getYPosition()] = agent;
    		agents.put(agent.getId(), agent);
    		
    		System.out.print(a.getTemplateView().getName() + " (Player " + agent.getPlayer() + "), ID:" + agent.getId());
    		System.out.print(" at (" + agent.getX() + ", " + agent.getY() + ") with HP:" + agent.getHp());
    		System.out.println();
    	});
    	
    	
    	allAgents = agents;
    	
    	for(Agent aa : allAgents.values()) {
    		for(int aaa : canAttack(aa)) {
    			System.out.print(aaa);
    		}
    		System.out.println();
    	}
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
    	
    	//which player controls it?
    	private int player;
    	
    	public Agent(int id, int x, int y, int atkDmg, int atkRan, int hp) {
    		super(id, x, y);
    		
    		this.atkDmg = atkDmg;
    		this.atkRan = atkRan;
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
    	//start with most basic: distance from  archer
    	//this.utility += 
    	
        return this.utility;
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
    	
    	
        return null;
    }

    /**
     * to make life easier and since all agents will need to have actions evaluated,
     * method to determine agent actions
     * all agents have 5 choices: move in one of cardinal directions, or attack
     * @param agent
     * @return
     */
    private List<Action> agentAct(Agent agent) {
    	
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
    	for(int id : canAttack(agent)) {
    		actions.add(Action.createPrimitiveAttack(agent.getId(), id));
    	}
    	
    	return actions;
    }
    
    private List<Integer> canAttack(Agent agent) {
    	List<Integer> agents = new ArrayList<Integer>();
    	
    	for (Agent enemy : allAgents.values()) {
    		//need to check that
    		//1. enemy is actually an enemy; not same type (footman vs archer)
    		//2. they are not the same agent
    		//3. enemy is in agent's range
    		if(enemy.getPlayer() != agent.getPlayer() && 
    		   enemy.getId() != agent.getId() &&
    		   //this is too complicated to just write here
    		   Math.floor(distance(agent, enemy)) < agent.atkRan) {
    			agents.add(enemy.getId());
    		}
    	}
    	
    	return agents;
    }
    
    private double distance(Cell agent, Cell enemy) {
    	return Math.sqrt(Math.pow(agent.getX() - enemy.getX(), 2.0) + Math.pow(agent.getY() - enemy.getY(), 2.0));
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
