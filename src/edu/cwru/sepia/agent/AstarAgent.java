package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class AstarAgent extends Agent {

    class MapLocation implements Comparable<MapLocation>
    {
        public int x, y;
        public MapLocation cameFrom;
        
        public float cost; //f = g + h
        public float dist; //distance to goal

        public boolean passable;

        public MapLocation(int x, int y, MapLocation cameFrom, float cost)
        {
            this.x = x;
            this.y = y;
            this.cameFrom = cameFrom;
            this.cost = cost;
        }
        
        @Override
        public int compareTo(MapLocation anotherLocation)
        {
        	return Float.compare(cost, anotherLocation.cost); //-1 if this < another, 0 if equal, 1 if this > another
        }
    }

    Stack<MapLocation> path;
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;

    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs

    public AstarAgent(int playernum)
    {
        super(playernum);

        System.out.println("Constructed AstarAgent");
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        // get the footman location
        List<Integer> unitIDs = newstate.getUnitIds(playernum);

        if(unitIDs.size() == 0)
        {
            System.err.println("No units found!");
            return null;
        }

        footmanID = unitIDs.get(0);

        // double check that this is a footman
        if(!newstate.getUnit(footmanID).getTemplateView().getName().equals("Footman"))
        {
            System.err.println("Footman unit not found");
            return null;
        }

        // find the enemy playernum
        Integer[] playerNums = newstate.getPlayerNumbers();
        int enemyPlayerNum = -1;
        for(Integer playerNum : playerNums)
        {
            if(playerNum != playernum) {
                enemyPlayerNum = playerNum;
                break;
            }
        }

        if(enemyPlayerNum == -1)
        {
            System.err.println("Failed to get enemy playernumber");
            return null;
        }

        // find the townhall ID
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

        if(enemyUnitIDs.size() == 0)
        {
            System.err.println("Failed to find enemy units");
            return null;
        }

        townhallID = -1;
        enemyFootmanID = -1;
        for(Integer unitID : enemyUnitIDs)
        {
            Unit.UnitView tempUnit = newstate.getUnit(unitID);
            String unitType = tempUnit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall"))
            {
                townhallID = unitID;
            }
            else if(unitType.equals("footman"))
            {
                enemyFootmanID = unitID;
            }
            else
            {
                System.err.println("Unknown unit type");
            }
        }

        if(townhallID == -1) {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }

        long startTime = System.nanoTime();
        path = findPath(newstate);
        totalPlanTime += System.nanoTime() - startTime;

        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        long startTime = System.nanoTime();
        long planTime = 0;

        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        if(shouldReplanPath(newstate, statehistory, path)) {
            long planStartTime = System.nanoTime();
            path = findPath(newstate);
            planTime = System.nanoTime() - planStartTime;
            totalPlanTime += planTime;
        }

        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);

        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();

        if(!path.empty() && (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {

            // start moving to the next step in the path
            nextLoc = path.pop();

            System.out.println("Moving to (" + nextLoc.x + ", " + nextLoc.y + ")");
        }

        if(nextLoc != null && (footmanX != nextLoc.x || footmanY != nextLoc.y))
        {
            int xDiff = nextLoc.x - footmanX;
            int yDiff = nextLoc.y - footmanY;

            // figure out the direction the footman needs to move in
            Direction nextDirection = getNextDirection(xDiff, yDiff);

            actions.put(footmanID, Action.createPrimitiveMove(footmanID, nextDirection));
        } else {
            Unit.UnitView townhallUnit = newstate.getUnit(townhallID);

            // if townhall was destroyed on the last turn
            if(townhallUnit == null) {
                terminalStep(newstate, statehistory);
                return actions;
            }

            if(Math.abs(footmanX - townhallUnit.getXPosition()) > 1 ||
                    Math.abs(footmanY - townhallUnit.getYPosition()) > 1)
            {
                System.err.println("Invalid plan. Cannot attack townhall");
                totalExecutionTime += System.nanoTime() - startTime - planTime;
                return actions;
            }
            else {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(footmanID, Action.createPrimitiveAttack(footmanID, townhallID));
            }
        }

        totalExecutionTime += System.nanoTime() - startTime - planTime;
        return actions;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
        System.out.println("Total turns: " + newstate.getTurnNumber());
        System.out.println("Total planning time: " + totalPlanTime/1e9);
        System.out.println("Total execution time: " + totalExecutionTime/1e9);
        System.out.println("Total time: " + (totalExecutionTime + totalPlanTime)/1e9);
    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this method.
     *
     * This method should return true when the path needs to be replanned
     * and false otherwise. This will be necessary on the dynamic map where the
     * footman will move to block your unit.
     * 
     * You can check the position of the enemy footman with the following code:
     * state.getUnit(enemyFootmanID).getXPosition() or .getYPosition().
     * 
     * There are more examples of getting the positions of objects in SEPIA in the findPath method.
     *
     * @param state
     * @param history
     * @param currentPath
     * @return
     */
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath)
    {
    	if(state.getUnit(enemyFootmanID) == null) return false;
    	
    	int enemyX = state.getUnit(enemyFootmanID).getXPosition();
    	int enemyY = state.getUnit(enemyFootmanID).getYPosition();
    	
    	//naive check of into next 5 steps of path to see if footman is blocking
    	
    	//make a copy of path for viewing
    	Stack<MapLocation> currPath = (Stack<MapLocation>) currentPath.clone();
    	int n = currPath.size();
    	for(int i = 0; i < n; i++) {
    		MapLocation curr = currPath.pop();
    		if(curr.x == enemyX && curr.y == enemyY) {
    			//System.out.println("true");
    			return true;
    		}
    	}
    	//System.out.println("false");
        return false;
    }

    /**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state)
    {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);

        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);

        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);

        MapLocation footmanLoc = null;
        if(enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }

        // get resource locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for(Integer resourceID : resourceIDs)
        {
            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);

            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }

        return AstarSearch(startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
    }
    /**
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     *
     * Therefore your you need to find some possible adjacent steps which are in range 
     * and are not trees or the enemy footman.
     * Hint: Set<MapLocation> resourceLocations contains the locations of trees
     *
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     *
     * As an example consider the following simple map
     *
     * F - - - -
     * x x x - x
     * H - - - -
     *
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     *
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     *
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     *
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     *
     * The path would be
     *
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     *
     * Notice how the initial footman position and the townhall position are not included in the path stack
     *
     * @param start Starting position of the footman
     * @param goal MapLocation of the townhall
     * @param xExtent Width of the map
     * @param yExtent Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @return Stack of positions with top of stack being first move in plan
     */    
    private Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations)
    {
    	//initialize cell array
    	System.out.println("Size of maze: " + xExtent + " x " + yExtent);
    	System.out.println("Start location: " + start.x + ", " + start.y);
    	System.out.println("Goal: " + goal.x + ", " + goal.y);
    	MapLocation[][] Map = new MapLocation[xExtent][yExtent];
    	
    	for(int x = 0; x < xExtent; x++) {
    		for(int y = 0; y < yExtent; y++) {
    			Map[x][y] = new MapLocation(x, y, null, 0);
    			Map[x][y].passable = true;
    		}
    	}
    	
    	//relevant cells
    	Map[start.x][start.y] = start;
    	Map[goal.x][goal.y] = goal;
    	goal.passable = true;
    	for(MapLocation obs : resourceLocations) {
    		System.out.println("Resource at: " + obs.x + ", " + obs.y);
    		Map[obs.x][obs.y].passable = false;
    	}
    	if(enemyFootmanLoc != null) Map[enemyFootmanLoc.x][enemyFootmanLoc.y].passable = false;
    	
    	    	
    	PriorityQueue<MapLocation> openSet = new PriorityQueue<>();
    	HashSet<MapLocation> closedSet = new HashSet<>();

    	Stack<MapLocation>path = new Stack<MapLocation>();
    	    	
    	//start.cameFrom = start;
    	start.dist = 0;
    	openSet.add(start);
    	
    	while(!openSet.isEmpty())
    	{
    		MapLocation n = openSet.poll(); //with an admissible heuristic, a we only open a node once
    		closedSet.add(n);
    		if(n == goal) break;
    		
    		for(MapLocation q : getNeighbors(Map, n)) {
    			
    			//ignore closed or impassable cells
    			if(!q.passable || closedSet.contains(q)) continue;
    			 
    			float distTo = n.dist + calcDist(n, q);
    			
    			//evaluate new cells or update shorter paths to existing cells
    			if(!openSet.contains(q) || distTo < q.dist) {
    				q.dist = distTo;
    				q.cost = calcHeuristic(q, goal);
    				q.cameFrom = n;
    				 
    				if(!openSet.contains(q)) openSet.add(q);
    			}
    		}
    	}
    	
    	//exit for no available path
    	if(!closedSet.contains(goal)) {
    		System.out.println("No available path");
    		System.exit(0);
    	}
    	
    	//reconstruct path from goal
    	MapLocation curr = goal.cameFrom; 	//do not include the goal in path
    	while(curr != start) {				//do not include the start in path
    		path.add(curr);
    		curr = curr.cameFrom;
    	}
    	
        return path;
    }
    
    /**
     * Returns the heuristic between two MapLocations
     * 
     * @param curr The current MapLocation
     * @param goal The destination MapLocation
     * @return a float representing the Chebyshev distance from curr to goal
     */
    private float calcHeuristic(MapLocation curr, MapLocation goal) 
    {
    	if(curr == null || goal == null) {
    		System.err.println("No MapLocation(s) specified");
    		return Float.MAX_VALUE;
    	}
    	return Math.max(Math.abs(goal.y - curr.y), Math.abs(goal.x - curr.x)) - 1;
    }
    
    /**
     * Returns the Euclidean distance between two MapLocations
     * 
     * @param curr The current MapLocation
     * @param next The target MapLocation
     * @return A float representing distance between MapLocations
     */
    private float calcDist(MapLocation curr, MapLocation next)
    {
    	return (float) Math.sqrt(Math.pow(next.x - curr.x, 2) + Math.pow(next.y - curr.y, 2));
    }
    
    /**
     * Looks at a given MapLocation in the map 
     * generates cells if they don't exist
     * and returns a set of in-bounds neighboring MapLocations
     * 
     * @param Map Array containing MapLocations
     * @param n The current MapLocation
     * @return A HashSet containing valid neighboring cells
     */
    private Set<MapLocation> getNeighbors(MapLocation[][] map, MapLocation n) {
    	Set<MapLocation> neighbors = new HashSet<MapLocation>();
    	    	
    	System.out.println("Checking neighbors of : (" + n.x + ", " + n.y + ") for map size : " + map.length + " x " + map[0].length);
    	
    	for(int x = -1; x <= 1; x++) {
    		for(int y = -1; y <= 1; y++) {
    			if (x == 0 && y == 0) continue; //ignore self
    			
    			int nextX = n.x + x; //offset from initial cell
    			int nextY = n.y + y;
    			
    			System.out.println();
    			
    			System.out.print("(" + nextX + ", " + nextY + ")");
    			
    			if(nextX < map.length && nextX >= 0 && nextY < map[0].length && nextY >= 0) {
    				
    				System.out.print(" Attempt to add neighbor: " + nextX + ", " + nextY);
    				
    				//contingency that should never happen
    				if (map[nextX][nextY] == null) map[nextX][nextY] = new MapLocation(nextX, nextY, null, 0);
    				
    				neighbors.add(map[nextX][nextY]);
    			}
    		}
    	}
    	
    	System.out.println();
    	return neighbors;
    }

    /**
     * Primitive actions take a direction (e.g. Direction.NORTH, Direction.NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
    private Direction getNextDirection(int xDiff, int yDiff) {

        // figure out the direction the footman needs to move in
        if(xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }

        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
}
