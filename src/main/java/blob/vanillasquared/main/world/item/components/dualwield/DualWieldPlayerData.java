package blob.vanillasquared.main.world.item.components.dualwield;

public interface DualWieldPlayerData {
    int vsq$getDualWieldCritCharges();
    void vsq$setDualWieldCritCharges(int charges);
    boolean vsq$consumeDualWieldCritCharge();
}
