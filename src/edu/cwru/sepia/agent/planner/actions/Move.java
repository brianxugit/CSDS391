package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;

public class Move implements StripsAction {

	Position pos;
	
	public Move(Position pos) {
		this.pos = pos;
	}
	
	//precond: bob not at target position
	@Override
	public boolean preconditionsMet(GameState state) {
		return !state.bobPos().equals(pos);
	}

	//effect: move bob to target position
	@Override
	public GameState apply(GameState state) {
		//make fresh copy
		GameState newState = new GameState(state);
		
		newState.move(pos);
		
		return newState;
	}

}
