package org.ships.movement;

import org.core.CorePlugin;
import org.core.entity.LiveEntity;
import org.core.entity.living.human.player.Player;
import org.core.vector.Vector3;
import org.core.world.boss.ServerBossBar;
import org.core.world.position.impl.sync.SyncBlockPosition;
import org.ships.event.vessel.move.ResultEvent;
import org.ships.plugin.ShipsPlugin;
import org.ships.vessel.common.flag.SuccessfulMoveFlag;
import org.ships.vessel.common.types.Vessel;
import org.ships.vessel.sign.LicenceSign;
import org.ships.vessel.structure.PositionableShipsStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class Result extends ArrayList<Result.Run> {

    public static final Result DEFAULT_RESULT = new Result(
            Run.COMMON_TELEPORT_ENTITIES,
            Run.COMMON_RESET_GRAVITY,
            Run.COMMON_SET_POSITION_OF_LICENCE_SIGN,
            Run.COMMON_SET_NEW_POSITIONS,
            Run.COMMON_SPAWN_ENTITIES,
            Run.COMMON_SET_SUCCESSFUL,
            Run.COMMON_SAVE,
            Run.REMOVE_BAR);

    public interface Run {

        Run COMMON_SET_SUCCESSFUL = (v, c) -> {
            SuccessfulMoveFlag flag = v.get(SuccessfulMoveFlag.class).orElse(new SuccessfulMoveFlag());
            flag.setValue(true);
            v.set(flag);
        };

        Run REMOVE_BAR = (v, c) -> c.getBar().ifPresent(ServerBossBar::deregisterPlayers);

        Run COMMON_TELEPORT_ENTITIES = (v, c) -> c.getEntities().forEach((entity, value) -> {
            double pitch = entity.getPitch();
            double yaw = entity.getYaw();
            double roll = entity.getRoll();
            Optional<SyncBlockPosition> opBefore = value.getBeforePosition();
            Optional<SyncBlockPosition> opAfter = value.getAfterPosition();
            if(opBefore.isPresent() && opAfter.isPresent()){
                Vector3<Double> position = entity.getPosition().getPosition().minus(opBefore.get().toExactPosition().getPosition());
                Vector3<Double> position2 = opAfter.get().toExactPosition().getPosition();
                position = position2.add(position);
                entity.setPosition(position);
            }
            entity.setYaw(yaw);
            entity.setRoll(roll);
            entity.setPitch(pitch);
        });

        Run COMMON_RESET_GRAVITY = (v, c) -> c.getEntities().keySet().forEach(e -> e.setGravity(true));

        Run COMMON_SET_NEW_POSITIONS = (v, c) -> {
            PositionableShipsStructure pss = v.getStructure();
            pss.clear();
            c.getMovingStructure().forEach((mb) -> mb.getAfterPosition().ifPresent(pss::addPosition));
        };

        Run COMMON_SET_POSITION_OF_LICENCE_SIGN = (v, c) -> {
            Optional<MovingBlock> opSign = c.getMovingStructure().get(ShipsPlugin.getPlugin().get(LicenceSign.class).get());
            opSign.flatMap(MovingBlock::getAfterPosition).ifPresent(after -> v.getStructure().setPosition(after));
        };

        Run COMMON_SPAWN_ENTITIES = (v, c) -> c.getEntities().keySet().forEach(e -> {
            if(!(e instanceof Player)) {
                e.getCreatedFrom().ifPresent(LiveEntity::remove);
            }
            try {
                e.spawnEntity();
            }catch (IllegalStateException ignored){
            }
        });

        Run COMMON_SAVE = (v, c) -> v.save();

        void run(Vessel vessel, MovementContext context);

    }

    public Result(){
        super();
    }

    public Result(Run... run){
        super(Arrays.asList(run));
    }

    public Result(Collection<Run> collection){
        super(collection);
    }

    public void run(Vessel vessel, MovementContext context){
        this.forEach(e -> {
            ResultEvent.PreRun event = new ResultEvent.PreRun(vessel, this, e, context);
            CorePlugin.getEventManager().callEvent(event);
            if(event.isCancelled()){
                return;
            }
            e.run(vessel, context);
        });
    }
}
