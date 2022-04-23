package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.Comparator;

public class MinimaxAlphaBeta extends Agent {

    private final int numPlys;

    public MinimaxAlphaBeta(int playernum, String[] args)
    {
        super(playernum);

        if(args.length < 1)
        {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }

        numPlys = Integer.parseInt(args[0]);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this.
     *
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     *
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
     *
     * @param node The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta)
    {
    	System.out.println("alpha beta is happening with depth = " + depth);
    	//at a particular state we have gamestate.getutility() for utility
    	//we also need to know which state we are at, aka max player vs min enemy
    	
    	double val = maximize(node, depth, alpha, beta);
    	
    	//find node that corresponds to our max
    	//System.out.println("chosen node with val " + val);
    	
    	for(GameStateChild child : node.state.getChildren()) {
    		if(child.state.getUtility() == val) return child;
    	}
    	
    	//sometimes you just dont succeed so lets just stay in place until something works
    	return node;
    }

    //methods to circumvent the need for player tracking variable
    private double maximize(GameStateChild node, int depth, double alpha, double beta) {
    	
    	if(depth == 0) return node.state.getUtility();
    	
    	double max = Double.NEGATIVE_INFINITY; //init to min
    	
    	for(GameStateChild child : orderChildrenWithHeuristics(node.state.getChildren())) {
    		
    		max = Math.max(max, minimize(child, depth - 1, alpha, beta));
    		
    		if(max >= beta) return max; //prune
    		
    		alpha = Math.max(max, alpha);
    	}
    	return max;
    }
    
    private double minimize(GameStateChild node, int depth, double alpha, double beta) {

    	if(depth == 0) return node.state.getUtility();
    	
    	double min = Double.POSITIVE_INFINITY; //you know the drill
    	
    	for(GameStateChild child : orderChildrenWithHeuristics(node.state.getChildren())) {
    		
    		min = Math.min(min, maximize(child, depth - 1, alpha, beta));
    		
    		if(min <= alpha) return min;
    		
    		beta = Math.min(min, beta);
    	}
    	return min;
    }
    
    /**
     * You will implement this.
     *
     * Given a list of children you will order them according to heuristics you make up.
     * See the assignment description for suggestions on heuristics to use when sorting.
     *
     * Use this function inside of your alphaBetaSearch method.
     *
     * Include a good comment about what your heuristics are and why you chose them.
     *
     * @param children
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children)
    {
    	//goal is to kill archer
    	//attack is highest priority, so we want to get as many as possible
    	//so simple hierarchy is
    	//n attacks > 1 attack > no attacks
    	
    	List<GameStateChild> orderedChildren = new ArrayList<GameStateChild>();
    	List<GameStateChild> orderedMoves = new ArrayList<GameStateChild>();
    	
    	//System.out.println("node expansion size: " + children.size());
    	
    	for(GameStateChild child : children) {
    		int atks = 0;
    		
    		for(Action action : child.action.values()) {
    			if(action.getType() == ActionType.PRIMITIVEATTACK) {
    				atks++;
    			}
    		}
    		
    		if(atks == child.action.size()) {
    			orderedChildren.add(0, child);   		
    		} else if (atks > 0) {
    			if(orderedChildren.isEmpty()) orderedChildren.add(0, child);
    			else orderedChildren.add(1, child);
    		} else {
    			orderedMoves.add(child);
    		}
    	}
    	
    	orderedMoves.sort(new Comparator<GameStateChild>() {
    		@Override
    		public int compare(GameStateChild o1, GameStateChild o2) {
    	    	if(o1.state.getUtility() > o2.state.getUtility()) return 1;
    	    	else if (o1.state.getUtility() < o2.state.getUtility()) return -1;
    	    	else return 0;
    	    }
    	});
    	
    	orderedChildren.addAll(orderedMoves); //append sorted movement options
    	
        return orderedChildren;
    }
    
    
    
    
    
}
