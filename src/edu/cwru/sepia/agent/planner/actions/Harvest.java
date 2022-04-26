package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;

public class Harvest implements StripsAction {
	
	int resourceId;
	
	Position bobPos;
	Position resPos;
	
	boolean hasSome;

	public Harvest(int id) {
		resourceId = id;
	}
	
	// resource is not empty & peasant does not have it
	@Override
	public boolean preconditionsMet(GameState state) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public GameState apply(GameState state) {
		GameState newState = new GameState(state);
		
		newState.harvest(resourceId);
		
		return newState;
	}

}
