package org.ships.algorthum.blockfinder;

import org.core.world.direction.Direction;
import org.core.world.direction.FourFacingDirection;
import org.core.world.position.BlockPosition;
import org.ships.config.blocks.BlockInstruction;
import org.ships.config.blocks.BlockList;
import org.ships.plugin.ShipsPlugin;
import org.ships.vessel.common.types.ShipsVessel;
import org.ships.vessel.structure.AbstractPosititionableShipsStructure;
import org.ships.vessel.structure.PositionableShipsStructure;

import java.util.Optional;

public class Ships5BlockFinder implements BasicBlockFinder {

    private int blockLimit = -1;
    private int blockCount = 0;
    private PositionableShipsStructure shipsStructure;
    private ShipsVessel vessel;
    private BlockList list;

    private void getNextBlock(BlockPosition position, Direction... directions){
        if(this.blockLimit != -1 && this.blockCount >= this.blockLimit){
            return;
        }
        this.blockCount++;
        for(Direction direction : directions){
            BlockPosition block = position.getRelative(direction);
            if(list.getBlockInstruction(block.getBlockType()).getCollideType().equals(BlockInstruction.CollideType.MATERIAL)){
                shipsStructure.addPosition(block);
                getNextBlock(block, directions);
            }
        }
    }

    @Override
    public PositionableShipsStructure getConnectedBlocks(BlockPosition position) {
        this.blockCount = 0;
        this.shipsStructure = new AbstractPosititionableShipsStructure(position);
        Direction[] directions = FourFacingDirection.getFourFacingDirections();
        getNextBlock(position, directions);
        return this.shipsStructure;
    }

    @Override
    public void getConnectedBlocksOvertime(BlockPosition position, OvertimeBlockFinderUpdate runAfterFullSearch) {
        runAfterFullSearch.onShipsStructureUpdated(getConnectedBlocks(position));
    }

    @Override
    public int getBlockLimit() {
        return this.blockLimit;
    }

    @Override
    public BasicBlockFinder setBlockLimit(int limit) {
        this.blockLimit = limit;
        return this;
    }

    @Override
    public Optional<ShipsVessel> getConnectedVessel() {
        return Optional.ofNullable(this.vessel);
    }

    @Override
    public BasicBlockFinder setConnectedVessel(ShipsVessel vessel) {
        this.vessel = vessel;
        if(this.vessel == null){
            this.list = ShipsPlugin.getPlugin().getBlockList();
        }else{
            this.list = this.vessel.getBlockList();
        }
        return this;
    }

    @Override
    public String getId() {
        return "ships:blockfinder_ships_five";
    }

    @Override
    public String getName() {
        return "Ships 5 BlockFinder";
    }
}