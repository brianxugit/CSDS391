package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;

public class Harvest implements StripsAction {
	
	int id;
	
	Position bobPos;
	Position resPos;
	
	boolean hasSome;

	public Harvest(GameState state) {
		
	}
	
	// resource is not empty & peasant does not have it
	@Override
	public boolean preconditionsMet(GameState state) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public GameState apply(GameState state) {
		// TODO Auto-generated method stub
		return null;
	}

}
