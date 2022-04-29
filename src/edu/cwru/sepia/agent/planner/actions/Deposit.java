package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.util.Direction;

public class Deposit implements StripsAction {

	Position bobPos;
	Position townhallPos = GameState.townhallPos;
	boolean holdingResource;
	
	public Deposit(Position bobPos, boolean loaded) {
		this.bobPos = bobPos;
		this.holdingResource = loaded;
	}
	
	@Override
	public boolean preconditionsMet(GameState state) {
		return state.bobPos().equals(townhallPos) && holdingResource;
	}

	@Override
	public GameState apply(GameState state) {
		GameState newState = new GameState(state);
		
		newState.deposit();
		
		//System.out.println("applied deposit");
		
		return newState;
	}

	@Override
	public Action createSepia(int id, Direction dir) {
		return Action.createPrimitiveDeposit(id, dir);

	}

	@Override
	public Position targetPos() {
		return townhallPos;
	}
	
	@Override
	public boolean directed() {
		return true;
	}
}
