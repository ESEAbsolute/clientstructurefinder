package icu.eseabs0.clientstructurefinder.structure;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.earthcomputer.clientcommands.command.arguments.ClientBlockPredicateArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.Vec3i;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StructurePatternArgument implements ArgumentType<StructurePatternArgument.StructurePatternResult> {

    private final CommandBuildContext context;

    public StructurePatternArgument(CommandBuildContext context) {
        this.context = context;
    }

    public static StructurePatternArgument structurePattern(CommandBuildContext context) {
        return new StructurePatternArgument(context);
    }

    @Override
    public StructurePatternResult parse(StringReader reader) {
        String raw = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        return new StructurePatternResult(raw, context);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        int lastSpace = remaining.lastIndexOf(' ');
        int startOffset = lastSpace + 1;
        String lastToken = remaining.substring(startOffset);

        SuggestionsBuilder newBuilder = builder.createOffset(builder.getStart() + startOffset);

        String mode;
        try {
            mode = context.getArgument("mode", String.class);
        } catch (IllegalArgumentException e) {
            mode = "non";
        }
        boolean directional = "dir".equals(mode);

        // Suggest a DIRECTION if the previous token is a BLOCK
        boolean suggestBlocks = true;
        String textBeforeCursor = remaining.substring(0, startOffset).trim();
        if (!textBeforeCursor.isEmpty()) {
            String[] previousTokens = textBeforeCursor.split("\\s+");
            if (previousTokens.length > 0) {
                String prevToken = previousTokens[previousTokens.length - 1];
                if (!isDirectionToken(prevToken, directional)) {
                    suggestBlocks = false;
                }
            }
        }

        SuggestionsBuilder dirBuilder = newBuilder.createOffset(newBuilder.getStart());
        List<String> directions = new ArrayList<>();
        if (directional) {
            directions.addAll(List.of("U", "D", "N", "W", "S", "E", "Z"));
        } else {
            directions.addAll(List.of("U", "D", "L", "R", "F", "B", "Z"));
        }
        for (String dir : directions) {
            if (dir.startsWith(lastToken.toUpperCase())) {
                dirBuilder.suggest(dir);
            }
        }

        if (".".startsWith(lastToken)) dirBuilder.suggest(".");
        if ("..".startsWith(lastToken)) dirBuilder.suggest("..");
        
        Suggestions dirSuggestions = dirBuilder.build();

        CompletableFuture<Suggestions> blockSuggestionsFuture;
        if (suggestBlocks) {
            try {
                ClientBlockPredicateArgument blockArg = ClientBlockPredicateArgument.blockPredicate(this.context)
                        .disallowTags();

                blockSuggestionsFuture = blockArg.listSuggestions(context, newBuilder);
                
            } catch (Exception e) {
                blockSuggestionsFuture = CompletableFuture.completedFuture(Suggestions.empty().join());
            }
        } else {
            blockSuggestionsFuture = CompletableFuture.completedFuture(Suggestions.empty().join());
        }

        return blockSuggestionsFuture.thenApply(blockSuggestions -> {
            if (dirSuggestions.isEmpty()) return blockSuggestions;
            if (blockSuggestions.isEmpty()) return dirSuggestions;
            
            return Suggestions.merge(newBuilder.getInput(), List.of(dirSuggestions, blockSuggestions));
        });
    }

    public static class StructurePatternResult {
        private final String raw;
        private final CommandBuildContext context;

        public StructurePatternResult(String raw, CommandBuildContext context) {
            this.raw = raw;
            this.context = context;
        }

        public String getRaw() {
            return raw;
        }

        public StructurePattern toStructurePattern(CommandContext<FabricClientCommandSource> ctx) throws CommandSyntaxException {
            boolean directional = ctx.getArgument("mode", String.class).equals("dir");
            String[] tokens = raw.split(" ");
            return StructurePatternArgument.parseTokens(tokens, directional, context);
        }
    }

    public static StructurePattern parseTokens(String[] tokens, boolean directional, CommandBuildContext context) throws CommandSyntaxException {
        List<SingleBlock> blocks = new ArrayList<>();
        Vec3i offset = Vec3i.ZERO;
        ClientBlockPredicateArgument.ClientBlockPredicate firstPred = null;
        ClientBlockPredicateArgument.ClientBlockPredicate lastPred = null;

        int i = 0;
        while (i < tokens.length) {
            String token = tokens[i];

            if (isBlockToken(token, directional)) {
                ClientBlockPredicateArgument.ClientBlockPredicate pred;
                if (token.equals("..")) {
                    pred = firstPred;
                } else if (token.equals(".")) {
                    pred = lastPred;
                } else {
                    pred = getBlockPredicateFromName(context, token);
                }

                if (pred == null) {
                    throw new com.mojang.brigadier.exceptions.DynamicCommandExceptionType(s -> net.minecraft.network.chat.Component.literal("Invalid block reference: " + s))
                            .create(token);
                }

                blocks.add(new SingleBlock(pred, offset));
                if (firstPred == null) firstPred = pred;
                lastPred = pred;
                i++;
                continue;
            }

            if (isDirectionToken(token, directional)) {
                offset = applyDirection(offset, token);
                i++;
                continue;
            }

            i++;
        }

        return new StructurePattern(blocks, directional);
    }

    private static boolean isBlockToken(String token, boolean directional) {
        if (token.equals(".") || token.equals("..")) return true;
        return !isDirectionToken(token, directional);
    }

    private static Vec3i applyDirection(Vec3i current, String token) {
        if (token.equals("Z")) {
            return Vec3i.ZERO;
        }
        int count = 1;
        String numberPart = token.substring(1);
        if (!numberPart.isEmpty()) count = Integer.parseInt(numberPart);

        char dir = token.charAt(0);
        Vec3i delta = switch (dir) {
            case 'U' -> new Vec3i(0, count, 0);
            case 'D' -> new Vec3i(0, -count, 0);
            case 'N' -> new Vec3i(0, 0, -count);
            case 'S' -> new Vec3i(0, 0, count);
            case 'W' -> new Vec3i(-count, 0, 0);
            case 'E' -> new Vec3i(count, 0, 0);
            case 'L' -> new Vec3i(-count, 0, 0);
            case 'R' -> new Vec3i(count, 0, 0);
            case 'F' -> new Vec3i(0, 0, -count);
            case 'B' -> new Vec3i(0, 0, count);
            default -> Vec3i.ZERO;
        };
        return current.offset(delta);
    }

    private static boolean isDirectionToken(String token, boolean directional) {
        if (token.equals("Z")) return true;
        if (directional) {
            return token.matches("[UDNWSE]\\d*");
        } else {
            return token.matches("[UDLFBR]\\d*");
        }
    }

    public static ClientBlockPredicateArgument.ClientBlockPredicate getBlockPredicateFromName(CommandBuildContext context, String name) throws CommandSyntaxException {
        var parseResult = ClientBlockPredicateArgument.blockPredicate(context)
                .parse(new com.mojang.brigadier.StringReader(name));
        return ClientBlockPredicateArgument.getBlockPredicate(parseResult);
    }
}
