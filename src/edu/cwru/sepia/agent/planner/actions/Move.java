package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.util.Direction;

public class Move implements StripsAction {

	Position bob;
	Position pos;
	
	public Move(Position bob, Position pos) {
		this.bob = bob;
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
		
		System.out.println("applied move from " + bob.x + ", " + bob.y + " to " + pos.x + ", " + pos.y + " w/cost: " + getCost());
		
		return newState;
	}
	
	@Override
	public double getCost() {
		return bob.chebyshevDistance(pos) - 1;
	}

	@Override
	public Action createSepia(int id, Direction dir) {
		return Action.createCompoundMove(id, pos.x, pos.y);
	}
}
