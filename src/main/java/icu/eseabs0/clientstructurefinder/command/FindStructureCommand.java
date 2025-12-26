package icu.eseabs0.clientstructurefinder.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import icu.eseabs0.clientstructurefinder.structure.SingleBlock;
import icu.eseabs0.clientstructurefinder.structure.StructurePattern;
import icu.eseabs0.clientstructurefinder.structure.StructurePatternArgument;
import net.earthcomputer.clientcommands.command.Flag;
import net.earthcomputer.clientcommands.task.RenderDistanceScanTask;
import net.earthcomputer.clientcommands.task.TaskManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.earthcomputer.clientcommands.command.ClientCommandHelper.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class FindStructureCommand {
    public static final Flag<Boolean> FLAG_KEEP_SEARCHING = Flag.ofFlag("keep-searching").build();

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext context) {
//        var cfindblock = dispatcher.register(literal("cfindblock")
//                .then(argument("block", withString(blockPredicate(context)))
//                        .executes(ctx -> {
//                            var blockWithString = getWithString(ctx, "block", ClientBlockPredicateArgument.ParseResult.class);
//                            return findBlock(ctx, Component.translatable("commands.cfindblock.starting", blockWithString.string()), getBlockPredicate(blockWithString.value()));
//                        })));
        var cfindstructure = dispatcher.register(literal("cfindstructure")
                .then(argument("mode", string()).suggests((ctx, builder) -> {
                            builder.suggest("non");
                            builder.suggest("dir");
                            return builder.buildFuture();
                        })
                        .then(argument("pattern", StructurePatternArgument.structurePattern(context))
                                .executes(ctx -> {
                                    StructurePatternArgument.StructurePatternResult res = ctx.getArgument("pattern", StructurePatternArgument.StructurePatternResult.class);
                                    StructurePattern pattern = res.toStructurePattern(ctx);
                                    String[] tokens = res.getRaw().split(" ");
                                    return findStructure(ctx, Component.translatable(
                                            "commands.cfindblock.starting",
                                            "%s (%s structure matching)".formatted(
                                                    tokens[0], (getString(ctx, "mode").equals("dir") ?
                                                            "directional" : "non-directional")
                                            )), pattern);
                                }))));
        FLAG_KEEP_SEARCHING.addToCommand(dispatcher, cfindstructure, ctx -> true);
    }

    public static int findStructure(CommandContext<FabricClientCommandSource> ctx, Component startingMessage, StructurePattern pattern) throws CommandSyntaxException {
        boolean keepSearching = getFlag(ctx, FLAG_KEEP_SEARCHING);
        sendFeedback(startingMessage);
        TaskManager.addTask("cfindstructure", new FindStructureTask(pattern, keepSearching));
        return Command.SINGLE_SUCCESS;
    }

    private static final class FindStructureTask extends RenderDistanceScanTask {
        private final StructurePattern pattern;

        @Nullable
        private BlockPos closestStructureCore;

        FindStructureTask(StructurePattern pattern, boolean keepSearching) {
            super(keepSearching);
            this.pattern = pattern;
        }

        @Override
        protected void scanBlock(Entity cameraEntity, BlockPos pos) throws CommandSyntaxException {
            ClientLevel level = Minecraft.getInstance().level;
            assert level != null;
            Vec3 cameraPos = cameraEntity.getEyePosition(0);
            SingleBlock coreBlock = pattern.getBlocks().getFirst();
            if (!coreBlock.predicate().test(level.registryAccess(), level, pos)) return;
            if (!validateStructure(level, pos, pattern)) return;
            if ((closestStructureCore == null
                    || pos.distToCenterSqr(cameraPos) < closestStructureCore.distToCenterSqr(cameraPos))) {
                closestStructureCore = pos.immutable();
                keepSearching = false;
            }
        }

        private boolean validateStructure(ClientLevel level, BlockPos corePos, StructurePattern pattern) throws CommandSyntaxException {
            if (pattern.isDirectional()) {
                for (int i = 1; i < pattern.getBlocks().size(); i++) {
                    SingleBlock sb = pattern.getBlocks().get(i);
                    BlockPos checkPos = corePos.offset(sb.offset());
                    if (!sb.predicate().test(level.registryAccess(), level, checkPos)) {
                        return false;
                    }
                }
                return true;
            } else {
                for (int r = 0; r < 4; r++) {
                    boolean ok = true;
                    for (int i = 1; i < pattern.getBlocks().size(); i++) {
                        SingleBlock sb = pattern.getBlocks().get(i);
                        Vec3i offset = rotateY(sb.offset(), r);
                        BlockPos checkPos = corePos.offset(offset);
                        if (!sb.predicate().test(level.registryAccess(), level, checkPos)) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) return true;
                }
                return false;
            }
        }

        private Vec3i rotateY(Vec3i v, int times) {
            int t = ((times % 4) + 4) % 4;
            int x = v.getX();
            int y = v.getY();
            int z = v.getZ();
            return switch (t) {
                case 0 -> new Vec3i(x, y, z);
                case 1 -> new Vec3i(-z, y, x);
                case 2 -> new Vec3i(-x, y, -z);
                default -> new Vec3i(z, y, -x);
            };
        }

        @Override
        protected boolean canScanChunk(Entity cameraEntity, ChunkPos pos) {
            return (closestStructureCore == null || hasAnyBlockCloserThan(cameraEntity, pos, closestStructureCore.distToCenterSqr(cameraEntity.getEyePosition(0))))
                    && super.canScanChunk(cameraEntity, pos);
        }

        @Override
        protected boolean canScanChunkSection(Entity cameraEntity, SectionPos pos) {
            SingleBlock coreBlock = pattern.getBlocks().getFirst();
            return hasBlockState(pos, coreBlock.predicate()::canEverMatch) && super.canScanChunkSection(cameraEntity, pos);
        }

        @Override
        public void onCompleted() {
            super.onCompleted();
            if (closestStructureCore == null) {
                sendError(Component.translatable("commands.cfindblock.notFound"));
            } else {
                Entity cameraEntity = Objects.requireNonNullElse(Minecraft.getInstance().cameraEntity, Minecraft.getInstance().player);

                String foundRadius = "%.2f".formatted(Math.sqrt(closestStructureCore.distToCenterSqr(cameraEntity.getEyePosition(0))));
                sendFeedback(
                        Component.translatable(
                                "commands.cfindblock.success",
                                Component.empty()
                                        .append(getLookCoordsTextComponent(closestStructureCore))
                                        .append(" ")
                                        .append(getGlowButtonTextComponent(closestStructureCore)),
                                foundRadius
                        )
                );
            }
        }
    }
}
