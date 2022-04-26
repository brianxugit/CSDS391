package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.util.Direction;

public class Deposit implements StripsAction {

	Position bobPos;
	Position townhallPos;
	boolean hasSome;
	
	public Deposit(Position bobPos, Position pos, boolean loaded) {
		this.bobPos = bobPos;
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

	@Override
	public Action createSepia() {
		// TODO Auto-generated method stub
		Direction direction = bobPos.getDirection(townhallPos);
		
		return Action.createPrimitiveDeposit(0, direction);

	}

}
