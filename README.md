# Command Syntax

## Structure

```text
/cfindstructure [--keep-searching] <mode> <block0> <dir> <...> <block1> <dir> <...> ...
```

## Params Explanation

- `<mode>`: Match mode, supports:
  - `non` — non-directional match  
  - `dir` — directional match  

- `<block>`: The block to match, supports:
  - Block name (e.g., `waxed_copper_bulb`) with block entity tags
  - `.` — follow the previous block  
  - `..` — follow the first block  

- `<dir>`: Direction to match **relative to the previous block**:
  - Non-directional mode (`non`):
    - `U` — Up  
    - `D` — Down  
    - `L` — Left  
    - `R` — Right  
    - `F` — Front  
    - `B` — Back  
    - `Z` — Center (return to center)  

  - Directional mode (`dir`):
    - `U` — Up  
    - `D` — Down  
    - `N` — North  
    - `S` — South  
    - `E` — East  
    - `W` — West  
    - `Z` — Center (return to center)

> **Notes:**  
> - Directions (except `Z`) can be followed by a number to indicate multiple blocks.  
>   - Example: `U3` = 3 blocks up  
>   - Consecutive directions can be combined: `U U U` = `U3`

## Examples

```text
/cfindstructure --keep-searching non minecraft:waxed_copper_bulb U minecraft:lightning_rod L minecraft:lightning_rod Z R minecraft:lightning_rod Z F minecraft:lightning_rod
```
The command above matches a structure that:
- Starts with a waxed copper bulb (call it CENTER)
- with a lightning rod one block above the CENTER (U indicates the block above the previous)
- with a lightning rod one block above AND one block to the left of the CENTER (L indicates the block to the left of the **previous** block, which means that to the left of the block above the CENTER)
- with a lightning rod one block to the right of the CENTER (note that Z returns the matching position to the center)
- with a lightning rod one space in front of the CENTER

Which is same as the following commands:
```text
/cfindstructure --keep-searching non minecraft:waxed_copper_bulb U minecraft:lightning_rod L minecraft:lightning_rod Z R minecraft:lightning_rod Z F minecraft:lightning_rod
/cfindstructure --keep-searching non minecraft:waxed_copper_bulb U minecraft:lightning_rod Z L U minecraft:lightning_rod Z R minecraft:lightning_rod Z F minecraft:lightning_rod
/cfindstructure --keep-searching non minecraft:waxed_copper_bulb U minecraft:lightning_rod L minecraft:lightning_rod D R2 minecraft:lightning_rod L F minecraft:lightning_rod
/cfindstructure --keep-searching non minecraft:waxed_copper_bulb U minecraft:lightning_rod L . Z R . Z F .
```




