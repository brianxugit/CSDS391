package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.util.Direction;

public class Harvest implements StripsAction {
	
	int resourceId;
	
	Position bobPos;
	Position resPos;
	
	boolean holdingResource;
	boolean empty;

	public Harvest(Position bobPos, Position resPos, int id, boolean holdingResource, boolean empty) {
		this.bobPos = bobPos;
		this.resPos = resPos;
		this.resourceId = id;
		this.holdingResource = holdingResource;
		this.empty = empty;
	}
	
	// resource is not empty & peasant does not have it
	@Override
	public boolean preconditionsMet(GameState state) {
		return bobPos.equals(resPos) && !holdingResource && !empty;
	}

	@Override
	public GameState apply(GameState state) {
		GameState newState = new GameState(state);
		
		newState.harvest(resourceId);
		
		//System.out.println("applied harvest at " + resPos.x + ", " + resPos.y);
		
		return newState;
	}

	@Override
	public Action createSepia(int id, Direction dir) {
		System.out.println(dir);
		return Action.createPrimitiveGather(id, dir);
	}

	@Override
	public Position targetPos() {
		return resPos;
	}
	
	@Override
	public boolean directed() {
		return true;
	}
}
