package com.agrieconomy.defense;

import com.agrieconomy.farmland.FarmlandManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * 방어 시설 블록 공통 부모.
 *
 * <p>설치 규칙:
 * <ul>
 *   <li>등록된 농지 청크 내에만 설치 가능</li>
 *   <li>청크당 1개 제한</li>
 *   <li>재해 발생 시 자동 소모 (1회용)</li>
 * </ul>
 * </p>
 */
public abstract class AbstractDefenseBlock extends BaseEntityBlock {

    // 블록 모양: 1×1 바닥 패드 형태 (높이 0.5)
    private static final VoxelShape SHAPE =
            net.minecraft.world.phys.shapes.Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);

    protected AbstractDefenseBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    /** 서브 클래스에서 자신의 DefenseType을 반환한다. */
    public abstract DefenseType getDefenseType();

    // ── 블록 배치 ─────────────────────────────────────────────────────

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
                        BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;

        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;

        // 농지가 아니면 즉시 제거
        if (!FarmlandManager.get(serverLevel).isFarmland(cx, cz)) {
            level.destroyBlock(pos, true);
            return;
        }

        DefenseManager defMgr = DefenseManager.get(serverLevel);

        // 청크당 1개 제한: 이미 설치된 시설이 있으면 제거
        if (defMgr.getInstallation(cx, cz) != null) {
            level.destroyBlock(pos, true);
            notifyNearbyPlayers(serverLevel, pos, "§c이 청크에는 이미 방어 시설이 설치되어 있습니다.");
            return;
        }

        // BlockEntity에 타입 저장
        if (level.getBlockEntity(pos) instanceof DefenseBlockEntity be) {
            be.setDefenseType(getDefenseType());
        }

        // DefenseManager에 등록 (블록 위치 포함)
        defMgr.install(cx, cz, getDefenseType(), pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            // 자연 제거(재해 소모) 시에는 DefenseManager에서 이미 처리하므로 중복 방지
            DefenseManager.get(serverLevel).removeIfPos(cx, cz, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ── 우클릭: 설치 상태 확인 ───────────────────────────────────────

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        DefenseType type = getDefenseType();

        player.sendSystemMessage(Component.literal(
                "§a[방어 시설] §f" + getFacilityName() +
                " (청크 [" + cx + ", " + cz + "]) — " +
                "§7" + type.getCounteredDisaster().name() + " 재해 방어 대기 중"));

        return InteractionResult.CONSUME;
    }

    // ── BlockEntity ───────────────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DefenseBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                CollisionContext context) {
        return SHAPE;
    }

    // ── 유틸 ─────────────────────────────────────────────────────────

    protected abstract String getFacilityName();

    private void notifyNearbyPlayers(ServerLevel level, BlockPos pos, String message) {
        level.players().stream()
                .filter(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 64)
                .forEach(p -> p.sendSystemMessage(Component.literal(message)));
    }
}
