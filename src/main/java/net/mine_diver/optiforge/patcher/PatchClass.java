package net.mine_diver.optiforge.patcher;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@RequiredArgsConstructor
public class PatchClass {

    public final String name;
    public final Set<PatchField> fields = Collections.newSetFromMap(new IdentityHashMap<>());
    public final Set<PatchMethod> methods = Collections.newSetFromMap(new IdentityHashMap<>());
}
