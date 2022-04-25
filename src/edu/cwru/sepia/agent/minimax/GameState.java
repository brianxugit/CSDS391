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
	
	//im tempted to store a* path information as a static list
	//the only time a path between 2 cells could possibly change is if somehow an agent is on one of those tiles
	//while that's very possible it's also very unlikely to have a pronounced outcome on the path
	//therefore a map of < start cell + goal cell , path info > would be a solution
	//to avoid constant recalculations of a*
	//thing is, we aren't doing true a*
	//that is, we simply want to compute the path length and use that to inform our main heuristic
	
	private static Map<Integer, Integer> paths = new HashMap<Integer, Integer>();
	//here's the idea
	//given a starting cell, 01, 01, and goal cell, 11, 11
	//key is concatenation 01011111, path info is simply computed path length
	//01011111 returns equivalent to 11110101 so i guess i'll just write to the map twice when computing a path
	//also i have no idea if this is the proper way to instantiate the static map but worst case it's just as if i didnt have it
	
	private World world;
	
	//private Cell[][] map;
	//private int xDim;
	//private int yDim;

	
	private boolean playerTurn; //true for footmen, false for archers
	//alright so now this thing needs to alternate every time
	//aka when gamestate constructor happens it needs to flip...
	private boolean calcUtility; //calc utility at player turn, dont for enemy turn
	
	private double utility = 0;
	
	private int totalFootmanHp;
	private int totalArcherHp;
	
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
    	this.world = new World(state.getXExtent(), state.getYExtent());

    	state.getAllResourceNodes().stream().forEach( (t) -> {
    		this.world.addTree(t.getID(), t.getXPosition(), t.getYPosition());
    	});
    	
    	state.getAllUnits().stream().forEach( (a) -> {
    		//jesus
    		this.world.addAgent(a.getID(), a.getXPosition(), a.getYPosition(), a.getTemplateView().getRange(), a.getTemplateView().getBasicAttack(), state.getUnit(a.getID()).getHP(), state.getUnit(a.getID()).getHP(), a.getTemplateView().getName().equals("Footman") ? 0 : 1);
    		if(a.getTemplateView().getName().equals("Footman")) totalFootmanHp += a.getHP(); //2 * 160 = 320
    		else totalArcherHp += a.getHP(); //etc
    		
    	});
    	
    	playerTurn = true;
    }
    
    //secondary constructor...
    //
    public GameState(GameState gameState) {
    	//System.out.println("new gamestate");
    	//i give up im making a secondary gamestate constructor
    	//the first one, which takes in a pure state, needs playerTurn = true
    	//all subsequent ones need playerTurn = !playerTurn
    	//which means adding new conditions to the existing constructor or just making another
    	//but now it's too unwieldy to copy all the initialization code for the map
    	//so i need to give that an independent method
    	//and now everything needs to be refactored
    	//ahfuosdfuoahuoga
    	
    	//with this it might be time to set up a proper structure of data
    	//the highest is the sepia state, which gamestate takes as parameter
    	//the most important piece of data in a gamestate is the map
    	//on the map holds all the agents and resources
    	//so let's have a map class
    	//no that's a terrible idea, map is already defined in java
    	
    	//that means a TON of methods have to be moved into the World class
    	
    	this.world = new World(gameState.world.xDim, gameState.world.yDim); //i have no idea why this is legal
    	
    	gameState.world.trees.values().stream().forEach( (t) -> {
    		this.world.addTree(t.getId(), t.getX(), t.getY());
    	});
    	
    	gameState.world.agents.values().stream().forEach( (a) -> {
    		this.world.addAgent(a.getId(), a.getX(), a.getY(), a.getAtkRan(), a.getAtkDmg(), a.getHp(), a.getPhp(), a.getPlayer());
    	});
    	
    	this.playerTurn = !gameState.playerTurn;
    	this.calcUtility =  gameState.calcUtility;
    	this.utility = gameState.utility;
    	
    }
    
    private class World {
    	private int xDim;
    	private int yDim;
    	//i just want to call you map, why does map have to be a data structure?
    	private Cell[][] map;
    	
    	private ArrayList<Agent> allFootmen = new ArrayList<Agent>();
    	private ArrayList<Agent> allArchers = new ArrayList<Agent>();
    	
    	private Map<Integer, Tree> trees = new HashMap<Integer, Tree>();
    	private Map<Integer, Agent> agents = new HashMap<Integer, Agent>();
    	
    	public World(int x, int y) {
    		this.xDim = x;
    		this.yDim = y;
    		
        	this.map = new Cell[xDim][yDim];
        	     	
        	/*
        	for(Agent aa : allAgents.values()) {
        		System.out.println(aa.getId() + " " + aa.atkRan + " " + aa.atkDmg);
        		for(int aaa : canAttack(aa)) {
        			System.out.print(aaa);
        		}
        		System.out.println();
        	}
        	*/
    	}
        
    	public void addTree(int id, int x, int y) {
    		Tree tree = new Tree(id, x, y);
    		map[x][y] = tree;
    		trees.put(tree.getId(), tree);
    	}
    	
    	public void addAgent(int id, int x, int y, int range, int dmg, int hp, int php, int player) {
    		Agent agent = new Agent(id, x, y, range, dmg, hp); 
    		agent.setPhp(php);
    		agent.setPlayer(player); //ew
    		map[x][y] = agent;
    		agents.put(agent.getId(), agent);
    		if(agent.getPlayer() == 0) {
    			allFootmen.add(agent);
    		}
    		else {
    			allArchers.add(agent);
    		}
    		/*
    		System.out.print("(Player " + agent.getPlayer() + "), ID:" + agent.getId());
    		System.out.print(" at (" + agent.getX() + ", " + agent.getY() + ") with HP:" + agent.getHp());
    		System.out.println();
    		*/
    	}
    	
        public boolean validCell(int x, int y) {
        	return x >= 0 && y >= 0 && x < xDim && y < yDim;
        }
        
        private double distance(Cell agent, Cell enemy) {
        	return Math.sqrt(Math.pow(agent.getX() - enemy.getX(), 2.0) + Math.pow(agent.getY() - enemy.getY(), 2.0));
        }
        
        public Collection<Agent> getLivingFootmen() {
        	return allFootmen.stream().filter(a -> (a.alive())).collect(Collectors.toList());
        }
        
        public Collection<Agent> getLivingArchers() {
        	return allArchers.stream().filter(a -> (a.alive())).collect(Collectors.toList());
        }
        
        public Cell[][] getMap() {
        	return map;
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
    	
    	if(this.calcUtility) return this.utility;
    	
    	this.utility += enemyDistanceUtility();
    	//this.utility += enemyHealthUtility();
    	//this.utility += canAttackUtility();
    	//this.utility += myHealthUtility();
    	
    	//System.out.println("health util: " + myHealthUtility());
    	//System.out.println("utility: " + this.utility);
    	
    	//you wouldn't believe how many hours were wasted by not including this
    	if(this.world.getLivingArchers().size() == 0) this.utility = Double.MAX_VALUE;
    	if(this.world.getLivingFootmen().size() == 0) this.utility = Double.MIN_VALUE;
    	
    	calcUtility = true;
    	
        return this.utility;
    }
    
    //a ton of unused utilities because, in the end, simple pathing and a gentle nudge telling it to attack was enough for all test obstacles
    private double myHealthUtility() {
    	double utility = 0.0;
    	
    	Map<Integer, Integer> footmenHp = new HashMap<Integer, Integer>();
    	
    	for(Agent archer : this.world.getLivingArchers()) {
    		for(int footmanId : canAttack(archer)) {
    			Agent footman = this.world.agents.get(footmanId);
    			footman.setPhp(footman.getHp() - archer.getAtkDmg());
    		}
    	}
    	
    	for(Agent footman : this.world.getLivingFootmen()) {
    		utility += (double)footman.getPhp() / (double)footman.getHp();
    	}
    	
    	return utility;
    }
    
    private double enemyHealthUtility() {
    	double utility = 0.0;
    	
    	for(Agent footman : this.world.getLivingFootmen()) {
    		for(int archerId : canAttack(footman)) {
    			Agent archer = this.world.agents.get(archerId);
    			archer.setPhp(archer.getHp() - footman.getAtkDmg());
    		}
    	}
    	
    	for(Agent archer : this.world.getLivingArchers()) {
    		utility += (double)archer.getHp() / (double)archer.getPhp();
    	}
    	utility = utility / (double)this.world.getLivingArchers().size();
    	
    	return utility;
    }
    
    private double canAttackUtility() {
    	double utility = 0.0;
    	for(Agent agent : this.world.getLivingFootmen()) {
    		utility += canAttack(agent).size();
    	}
    	System.out.println("attack util: " + utility);
    	return this.utility;
    }
    
    private double enemyDistanceUtility() {
    	
    	double utility = 0.0;
    	double temp = 1;
    	
    	//System.out.println(this.world.getLivingFootmen().size() + " footmen are alive");
    	//System.out.println(this.world.getLivingArchers().size() + " archers are alive");
    	
    	//find each footman's best path(s) to archer(s)
    	for(Agent footman : this.world.getLivingFootmen()) {
    		
    		int fX = footman.getX();
    		int fY = footman.getY();
	
    		//stores the shortest path a particular footman has access to
    		int shortestPath = Integer.MAX_VALUE; 

    		for(Agent archer : this.world.getLivingArchers()) {
    			int aX = archer.getX();
    			int aY = archer.getY();
    			
    			//the path from this footman to a particular archer
    			int pathLengthToArcher = AstarPathLength(footman, this.world.map[fX][fY], this.world.map[aX][aY]);
    			
    			//System.out.println("footman ID: " + footman.getId() + " at " + fX + ",  " + fY + " found path length " + pathLengthToArcher);
    			
    			shortestPath = Math.min(pathLengthToArcher, shortestPath);
    		}
    		//System.out.println("shortest path for " + footman.getId() + " = " + shortestPath);
    		
    		//dividing by zero is bad
    		utility += (shortestPath != 0) ? temp * (1/(double)shortestPath) : temp + 1; //band-aid to resolve issue where 0 path length was somehow less ideal
    	}
    	return utility;
    }
    
    //i would like each instance of astar to have its own, clean copy of the map
    //so instead i will build a new map for each one
    //populated by special cells for astar
    //holy hell is this a bad idea
    private class AstarCell extends Cell implements Comparable<AstarCell> {
    	public int x, y;
    	public AstarCell cameFrom;
    	public float cost;
    	public float dist;
    	public boolean passable;
    	
    	public AstarCell(int x, int y, AstarCell cameFrom, float cost) {
    		super(0, x, y);
    		this.cameFrom = cameFrom;
    		this.cost = cost;
    	}
    	
    	@Override
    	public int compareTo(AstarCell anotherCell) {
    		return Float.compare(cost,  anotherCell.cost);
    	}
    }
    
    private int AstarPathLength(Agent agent, Cell start, Cell goal) {
    	
    	Cell[][] cellMap = this.world.getMap();
    	
    	AstarCell[][] map = new AstarCell[this.world.xDim][this.world.yDim];
    	
    	for(int x = 0; x < this.world.xDim; x++) {
    		for(int y = 0; y < this.world.yDim; y++) {
    			map[x][y] = new AstarCell(x, y, null, 0);
    			map[x][y].passable = !this.world.trees.containsValue(cellMap[x][y]);
    		}
    	}
    	
    	for(Agent a : (agent.getPlayer() == 0) ? this.world.getLivingFootmen() : this.world.getLivingArchers()) {
    		map[a.getX()][a.getY()].passable = false;
    	}
    	
    	PriorityQueue<AstarCell> openSet = new PriorityQueue<>();
    	HashSet<AstarCell> closedSet = new HashSet<>();
    	
    	AstarCell s = map[start.getX()][start.getY()];
    	AstarCell g = map[goal.getX()][goal.getY()];
    	
    	s.dist = 0f;
    	openSet.add(s);
    	
    	while(!openSet.isEmpty())
    	{
    		AstarCell n = openSet.poll();
    		closedSet.add(n);
    		if(n == g) break;
    		
    		for(AstarCell q : getNeighbors(map, n)) {
    			if(closedSet.contains(q) || !q.passable) continue;
    			
    			float distTo = n.dist + euclidean(n, q);
    			
    			if(!openSet.contains(q) || distTo < q.dist) {
    				q.dist = distTo;
    				q.cost = manhattan(q, g);
    				q.cameFrom = n;
    				
    				if(!openSet.contains(q)) openSet.add(q);
    			}
    		}
    	}
    	
    	AstarCell curr = g.cameFrom;
    	int length = 0;
    	while(curr != s) {
    		//System.out.println(curr.getX() + ", " + curr.getY());
    		length++;
    		curr = curr.cameFrom;
    	}
    	
    	//System.out.println("unit at (" + s.getX() + ", " + s.getY() + ") is " + length + " moves from (" + g.getX() + ", " + g.getY() + ")");
    	
    	return length;
    }
    
    //manhattan distance
    private float manhattan(AstarCell curr, AstarCell goal) {
    	if(curr == null || goal == null) {
    		return Float.MAX_VALUE;
    	}
    	return Math.abs(goal.getY() - curr.getY()) + Math.abs(goal.getX() - curr.getX()) - 1; //make it admissible
    }
    
    private float euclidean(AstarCell curr, AstarCell goal) {
    	return (float) Math.sqrt(Math.pow(goal.getX() - curr.getX(), 2) + Math.pow(goal.getY() - curr.getY(), 2));
    }
    
    private Set<AstarCell> getNeighbors(AstarCell[][] map, AstarCell n) {
    	Set<AstarCell> neighbors = new HashSet<AstarCell>();
    	
    	for(int x = -1; x <= 1; x++) {
    		for(int y = -1; y <= 1; y++) {
    			if (x == 0 && y == 0) continue; //ignore self
    			
    			int nextX = n.getX() + x; //offset from initial cell
    			int nextY = n.getY() + y;
    			
    			if(nextX < map.length && nextX >= 0 && nextY < map[0].length && nextY >= 0) {
    				
    				neighbors.add(map[nextX][nextY]);
    			}
    		}
    	}
    	return neighbors;
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
    	Collection<Agent> agents = playerTurn ? this.world.getLivingFootmen() : this.world.getLivingArchers();
    	
    	//collect actions per agent into a list
    	//given an agent, agentActions() returns a list of actions that agent can do
    	
    	List<List<Action>> agentActions = agents.stream().map(a -> agentActions(a)).collect(Collectors.toList());
    	
    	//we only have 2 agents so the result is the list agentActions.get(0) and agentActions.get(1) for each one
    	
    	//now we want to combine these actions to form up to 25 pairs of actions
    	//make action combos for gamestates
    	//mercy on my names
    	
    	List<Map<Integer, Action>> actionListList = new ArrayList<Map<Integer, Action>>();
    	
    	if(agentActions.size() == 0) return null;
    	
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
    		GameState child = new GameState(this); //ive removed the band-aid and now its not terrible
    		for(Action action : actionList.values()) {
    			child.applyAction(action);
    		}
    		children.add(new GameStateChild(actionList, child));
    	}
    	
    	//System.out.println("getChildren found " + children.size() + " children");
    	
        return children;
    }
    
    /* to make life easier and since all agents will need to have actions evaluated,
     * method to determine agent actions
     * all agents have 5 choices: move in one of cardinal directions, or attack--no diagonals
     * @param agent
     * @return
     */
    private List<Action> agentActions(Agent agent) {
    	
    	List<Action> actions = new ArrayList<Action>();
    	
    	for(int id : canAttack(agent)) {
    		actions.add(Action.createPrimitiveAttack(agent.getId(), id));
    	}
    	
    	for(Direction direction : Direction.values()) {
    		switch(direction) {
    		case NORTH: case SOUTH: case EAST: case WEST:
    			int x = agent.getX() + direction.xComponent();
    			int y = agent.getY() + direction.yComponent();
    			
    			if (this.world.validCell(x, y) && this.world.map[x][y] == null) {
    				actions.add(Action.createPrimitiveMove(agent.getId(), direction));
    			}
    			break;
    		}
    	}

    	
    	return actions;
    }
    
  //who can i attack?
    private List<Integer> canAttack(Agent agent) {
    	List<Integer> agents = new ArrayList<Integer>();
    	
    	for (Agent enemy : this.world.agents.values()) {
    		//need to check that
    		//1. enemy is actually an enemy; not same type (footman vs archer)
    		//2. they are not the same agent
    		//3. enemy is in agent's range
    		if(enemy.getPlayer() != agent.getPlayer() && 
    		   enemy.getId() != agent.getId() &&
    		   //this is too complicated so i need a distance function
    		   inRange(agent.getX(), agent.getY(), enemy.getX(), enemy.getY(), agent.getAtkRan())) {
    			agents.add(enemy.getId());
    		}
    	}
    	
    	return agents;
    }
    
    private boolean inRange(int x, int y, int enemyX, int enemyY, int range) {
    	return Math.abs(enemyY - y) <= range && Math.abs(enemyX - x) <= range;
    }
    
    private void applyAction(Action action) {
    	if(action.getType() == ActionType.PRIMITIVEATTACK) {
    		//System.out.println("applied an atk action");
    		TargetedAction tarAction = (TargetedAction) action;
    		
    		Agent atker = this.world.agents.get(tarAction.getUnitId());
    		Agent atked = this.world.agents.get(tarAction.getTargetId());
    		
    		this.attack(atker, atked);
    		
    	} else { //actiontype.primitivemove
    		DirectedAction dirAction = (DirectedAction) action;
    		
    		this.move(this.world.agents.get(dirAction.getUnitId()), 
    				  dirAction.getDirection().xComponent(),
    				  dirAction.getDirection().yComponent());
    	}
    }
    
    private void move(Agent agent, int x, int y) {
    	int nextX = agent.getX() + x;
    	int nextY = agent.getY() + y;
    	
    	this.world.map[agent.getX()][agent.getY()] = null;
    	
    	agent.setX(nextX);
    	agent.setY(nextY);
    	
    	this.world.map[nextX][nextY] = agent;
    	
    	//System.out.println(agent.getId() + " moved!");
    }
    
    private void attack(Agent atker, Agent atked) {
    	atked.setHp(atked.getHp() - atker.getAtkDmg());
    	
    }

    /**
    
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
