package icu.eseabs0.clientstructurefinder.structure;

import java.util.List;

public class StructurePattern {
    private final List<SingleBlock> blocks;
    private final boolean directional; // dir / non

    public StructurePattern(List<SingleBlock> blocks, boolean directional) {
        this.blocks = blocks;
        this.directional = directional;
    }

    public List<SingleBlock> getBlocks() { return blocks; }

    public boolean isDirectional() { return directional; }
}
