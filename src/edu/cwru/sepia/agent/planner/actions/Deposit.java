package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;

public class Deposit implements StripsAction {

	Position townhallPos;
	boolean hasSome;
	
	public Deposit(Position pos, boolean loaded) {
		this.townhallPos = pos;
		this.hasSome = loaded;
	}
	
	@Override
	public boolean preconditionsMet(GameState state) {
		return state.bobPos().equals(townhallPos) && hasSome;
	}

	@Override
	public GameState apply(GameState state) {
		GameState newState = new GameState(state);
		
		newState.deposit();
		
		return newState;
	}

}
