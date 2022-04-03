package biz.princeps.landlordmap.dynmap.worldguard;

import org.bukkit.Bukkit;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.flag.IWrappedFlag;

import java.util.Optional;

public final class LLDM_Worldguard {

    private LLDM_Worldguard() {
        throw new UnsupportedOperationException("This is a utility class.");
    }


    public static IWrappedFlag<Boolean> LLDM_COLOR_FLAG;

    public static void initFlags() {
        WorldGuardWrapper ins = WorldGuardWrapper.getInstance();
        Optional<IWrappedFlag<Boolean>> opt = ins.registerFlag("lldm-color", Boolean.class);
        if (opt.isPresent()) {
            LLDM_COLOR_FLAG = opt.get();
        } else {
            Bukkit.getLogger().warning("Failed to register lldm-color-flag!!!");
        }
    }
}
