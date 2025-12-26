package icu.eseabs0.clientstructurefinder.structure;

import net.earthcomputer.clientcommands.command.arguments.ClientBlockPredicateArgument;
import net.minecraft.core.Vec3i;

public record SingleBlock (
        ClientBlockPredicateArgument.ClientBlockPredicate predicate,
        Vec3i offset
) {}