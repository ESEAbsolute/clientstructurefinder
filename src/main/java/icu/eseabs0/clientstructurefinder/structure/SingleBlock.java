package icu.eseabs0.clientstructurefinder.structure;

import icu.eseabs0.clientstructurefinder.clientcommandadapted.ClientBlockPredicateArgument;
import net.minecraft.core.Vec3i;

public record SingleBlock (
        ClientBlockPredicateArgument.ClientBlockPredicate predicate,
        Vec3i offset
) {}