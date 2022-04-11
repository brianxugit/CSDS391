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
	
	private Map map;
	private double xDim;
	private double yDim;
	//private map of agents
	//private map of obstacles
	
	
	private double utility = 0;
	
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
    	Map<Integer, Tree> trees = new HashMap<Integer, Tree>();
    	Map<Integer, Agent> agents = new HashMap<Integer, Agent>();
    	
    	state.getAllResourceNodes().stream().forEach( (e) -> {
    		Tree tree = new Tree(e.getID(), e.getXPosition(), e.getYPosition());
    		//System.out.println(tree.getX() + " " + tree.getY());
    	}
    	);
    	
    	state.getAllUnits().stream().forEach( (a) -> {
    		Agent agent = new Agent(a.getID(), a.getXPosition(), a.getYPosition(), a.getTemplateView().getRange(), a.getTemplateView().getBasicAttack(), a.getTemplateView().getBaseHealth());
    		System.out.println(agent.getX() + " " + agent.getY() + " HP:" + agent.getHp());
    	});
    	
    }
    
    private class Tree {
    	
    	private int id;
    	private int x;
    	private int y;
    	
    	public Tree(int id, int x, int y) {
    		this.id = id;
    		this.x = x;
    		this.y = y;
    	}
    	
    	public int getX() {
    		return x;
    	}
    	
    	public int getY() {
    		return y;
    	}
    }

    private class Agent {
    	
    	private int id;
    	private int x;
    	private int y;
    	
    	private int atkDmg;
    	private int atkRan;
    	private int hp;
    	
    	public Agent(int id, int x, int y, int atkDmg, int atkRan, int hp) {
    		this.id = id;
    		this.x = x;
    		this.y = y;
    		
    		this.atkDmg = atkDmg;
    		this.atkRan = atkRan;
    		this.hp = hp;
    	}
    	
    	public int getX() {
    		return x;
    	}
    	
    	public int getY() {
    		return y;
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
        return 0.0;
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
}
